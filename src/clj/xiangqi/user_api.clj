(ns xiangqi.user-api
  (:require [ring.util.response :as ring-resp]
            [clojure.string :as string]
            [integrant.core :as ig]
            [io.pedestal.interceptor :refer [interceptor]]
            [medley.core :as medley]
            [io.pedestal.http.route :as route]
            [clojure.tools.logging :as log]
            [datomic.client.api :as client]
            [xiangqi.utils :as utils])
  (:import [com.auth0.jwt JWT]
           [com.auth0.jwt.algorithms Algorithm]
           [java.util Date UUID]
           [com.auth0 AuthenticationController IdentityVerificationException]
           [com.auth0.jwk JwkProviderBuilder]
           [com.auth0.jwt.interfaces Claim DecodedJWT]
           [org.eclipse.jetty.server.session SessionHandler]
           [org.eclipse.jetty.servlet ServletContextHandler]
           [java.net URI]))


(def main-page "/index.html")
(def login-fail "/login-fail.html")

(defn ^AuthenticationController build-auth0-controller
  [{:keys [^String domain clientId clientSecret]}]
  {:pre [(not-any? string/blank? [domain clientId clientSecret])]}
  (let [jwk (.build (JwkProviderBuilder. domain))]
    (->
      (AuthenticationController/newBuilder domain clientId clientSecret)
      (.withJwkProvider jwk)
      (.build))))

(defn build-auth-cookie
  "an authorisation cookie of our very own"
  [user-uid]
  (let [algorithm (Algorithm/HMAC256 "secret")]
    (-> (JWT/create)
      (.withIssuer "elephantchess.me")
      (.withSubject (str user-uid))
      (.withIssuedAt (Date.))
      (.sign algorithm))))

(defn create-cookie
  "create a user id cookie"
  ([]
   (create-cookie
     (ring-resp/status nil 204)
     (UUID/randomUUID)))
  ([resp uid]
   (ring-resp/set-cookie
     resp
     "uid"
     (build-auth-cookie uid))))

(prefer-method ig/init-key :component/vase-api :auth/controller)
(defmethod ig/init-key :auth/controller
  [_ config]
  (let [auth-controller (build-auth0-controller config)]
    (assoc config
      :auth-controller auth-controller
      :mixin-authcontroller
      (interceptor
        {:name :mixin-authcontroller
         :enter #(assoc-in % [:request :auth/controller] auth-controller)}))))

;;;combine key with value for the benefit of fern
;(defmethod ig/resolve-key :auth/controller
;  [k v]
;  v)

(prefer-method ig/halt-key! :component/vase-api :auth/controller)
(defmethod ig/halt-key! :auth/controller
  [_ this]
  (dissoc this :auth-controller))

(defn build-auth0-login-uri
   [{:auth/keys [^AuthenticationController controller]
     :keys [servlet-request servlet-response redirect-uri] :as request}]
  (->
    (.buildAuthorizeUrl controller servlet-request servlet-response redirect-uri)
    (.withScope "openid profile email")
    (.build)))

(defmethod ig/init-key :jetty-addin/session-handler
  [_ config]
  (assoc-in config
    [:add-ins :io.pedestal.http/container-options :context-configurator]
    (fn [^ServletContextHandler sch]
        (.insertHandler sch (SessionHandler.))
        sch)))

(def reqa (atom nil))

(defn do-login-redirect
  "generate a 302 redirect for auth0"
  [request]
  (reset! reqa request)
  (->>
    (route/url-for :user.v1/logged-in :absolute? true)
    (assoc request :redirect-uri)
    build-auth0-login-uri
    ring-resp/redirect))

(defn extract-claims
  "put interesting idtoken information into a clojure may"
  [idToken]
  (when idToken
    (let [^DecodedJWT jwt (JWT/decode idToken)]
      (into {}
        (map (fn [[k ^Claim v]]
                 [(keyword k) (or
                                (.asBoolean v)
                                (.asDate v)
                                (.asDouble v)
                                (.asString v))])
          (.getClaims jwt))))))

(def uia (atom nil))

(def my-userinfo
  {:given_name "Ben",
   :email "hammonba@gmail.com",
   :aud "ITQvmR3b13hmc4eL4Ct60Gipr3OUdIyr",
   :locale "en",
   :sub "google-oauth2|100995705928489273496",
   :iss "https://dev-e0x9wgap.eu.auth0.com/",
   :name "Ben Hammond",
   :nickname "hammonba",
   :exp #inst"2020-04-16T18:56:21.000-00:00",
   :email_verified true,
   :family_name "Hammond",
   :updated_at "2020-04-16T08:56:21.260Z",
   :picture "https://lh3.googleusercontent.com/a-/AOh14GhOut5cWX-ipl7MFERmR-Sxdnz3et9u251OnvhN8Q",
   :iat #inst"2020-04-16T08:56:21.000-00:00"})

(def userinfo-keymap
  {:email :user/email
   :locale :user/locale
   :sub :user/ident
   :name :user/name
   :picture :user/picture
   :uuid :user/uuid})

(def user-tempid "user-tempid")

(defn transform-userinfo
  "transform userinfo to make it suitable for datomic tx"
  [userinfo]
  (-> userinfo
    (utils/select-and-rename-keys userinfo-keymap)
    (assoc :db/id user-tempid)
    (utils/update-some :user/picture #(URI. %))))

(defn existing-uuid
  "if the :user/uuid is missing then create it
   return the :user/uuid value"
  [db lkr]
  (first (:user/uuid (client/pull db '[:user/uuid] lkr))))

(defn ensure-uuid
  "if the :user/uuid is missing then create it
   return the :user/uuid value"
  [conn db lkr]
  (or (existing-uuid db lkr)
    (let [uuid (UUID/randomUUID)]
      (client/transact conn
        {:tx-data [[:db/add lkr :user/uuid uuid]]})
      uuid)))

(defn transact-userinfo
  "insert or update user info.
   return our own user/uuid for this record"
  [{:keys [conn db]} userinfo]
  (let [{:keys [tempids]}
        (client/transact conn {:tx-data [(transform-userinfo userinfo)]})
        user-eid (get tempids user-tempid)]
    (ensure-uuid conn db user-eid)))

(defn extract-uid-from-cookie
  [ck]
  "(UUID/fromString (:sub (uapi/extract-claims (get-in (:cookies @uapi/reqa) [\"uid\" :value]))))"
  (:uid (extract-claims ck)))

(defn do-logged-in
  [{:auth/keys [^AuthenticationController controller]
    :keys [cookies servlet-request servlet-response] :as request}]
  (try
    (let [toks (.handle controller servlet-request servlet-response)
          ;accessToken (.getAccessToken toks)
          idToken (.getIdToken toks)
          userinfo (extract-claims idToken)
          existing-uid (extract-uid-from-cookie (:uid cookies))
          userinfo (medley/assoc-some userinfo :uuid existing-uid)
          uid (transact-userinfo request userinfo)]
      (reset! uia toks)

      (-> (ring-resp/redirect main-page)
        (create-cookie uid)))
    (catch IdentityVerificationException idex
      (log/error :exception idex))
    ))

(defn do-login-failed
  [req]
  (log/warn :fn :do-login-failed
    :req req)
  (ring-resp/response (with-out-str (fipp.edn/pprint req))))

(defn do-logout
  [req]
  {:status  501
   :headers {}
   :body    "TODO logout"})

(defn logged-out
  [req]
  {:status 501
   :headers {}
   :body "TODO logged out"})

(defn details
  [{:keys [db cookies] :as req}]
  (reset! reqa req)
  (let [lkr [:user/uuid
             (extract-uid-from-cookie (get cookies "uid"))]]
    (client/pull db '[*] lkr)))
