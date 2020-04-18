(ns xiangqi.user-api
  (:require [ring.util.response :as ring-resp]
            [clojure.string :as string]
            [integrant.core :as ig]
            [io.pedestal.interceptor :refer [interceptor]]
            [medley.core :as medley]
            [io.pedestal.http.route :as route]
            [clojure.tools.logging :as log]
            [datomic.client.api :as client]
            [xiangqi.utils :as utils]
            [io.pedestal.interceptor :as interceptor]
            [io.pedestal.interceptor.chain :as chain]
            [io.pedestal.interceptor.chain :as interceptor.chain])
  (:import [com.auth0.jwt JWT JWTVerifier JWTCreator$Builder]
           [com.auth0.jwt.algorithms Algorithm]
           [java.util Date UUID]
           [com.auth0 AuthenticationController IdentityVerificationException]
           [com.auth0.jwk JwkProviderBuilder]
           [com.auth0.jwt.interfaces Claim DecodedJWT Verification]
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
      (.withAudience "elephantchess.me")
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



(defmethod ig/init-key :user-api/jwt-algorithm
  [_ config]
  (assoc config :jwt/algorithm (Algorithm/HMAC256 "secret")))

(defn strarr
  [str-or-coll]
  (into-array String (if (coll? str-or-coll)
                       str-or-coll
                       [str-or-coll])))

(def jwt-create-fns
  {:iss #(.withIssuer ^JWTCreator$Builder %1 %3)
   :sub #(.withSubject ^JWTCreator$Builder %1 %3)
   :aud #(.withAudience ^JWTCreator$Builder %1 (strarr %3))
   :exp #(.withExpiresAt ^JWTCreator$Builder %1 %3)
   :nbf #(.withNotBefore ^JWTCreator$Builder %1 %3)
   :iat #(.withIssuedAt ^JWTCreator$Builder %1 %3)
   :jti #(.withJWTId ^JWTCreator$Builder %1 %3)})

(defn with-claim
  [^JWTCreator$Builder jcb name val]
  (.withClaim jcb name ^String val))

(defmulti create-algorithm :type)
(defmethod create-algorithm :hmac256 [{:keys [secret]}] (Algorithm/HMAC256 secret))
(defmethod create-algorithm :hmac512 [{:keys [secret]}] (Algorithm/HMAC512 secret))

(defn jwt-create-and-sign
  [payload]
  (let [^JWTCreator$Builder jcb
        (reduce-kv
          (fn [jcb k v]
              (let [f (get jwt-create-fns k with-claim)]
                (f jcb k v)))
          (JWT/create)
          (dissoc payload :algo)
          )]
    (.sign jcb (:algo payload))))


(def cookie-keys
  "keys that belong to outer level of cookie"
  [:domain :expires :http-only :max-age :path :same-site :secure])

(defn cookie-creator
  [payload]
  (let [jwt (jwt-create-and-sign (apply dissoc payload cookie-keys))]
    (-> payload
      (select-keys cookie-keys)
      (utils/assoc-someabsent :Expires (:exp payload))
      (assoc :value jwt)))
  )

(defn build-defaults-cookiecreator
  "close over defaults, ensure they are added to future payloads"
  [defaults]
  (comp cookie-creator #(merge defaults %)))

(def jwt-verify-fn
  {:iss #(.withIssuer ^Verification %1 (strarr %3))
   :sub #(.withSubject ^Verification %1 %3)
   :aud #(.withAudience ^Verification %1 (strarr %3))
   :leeway #(.acceptLeeway ^Verification %1 (long %3))
   :expiresAt #(.acceptExpiresAt ^Verification %1 (long %3))
   :notBefore #(.acceptNotBefore ^Verification %1 (long %3))
   :issuedAt #(.acceptIssuedAt ^Verification %1 (long %3))
   :jti #(.withJWTId ^Verification %1 %3)
   })

(defn jwt-verify-withclaim
  [^Verification verif name v]
  (.withClaim verif name ^String v))

(defn build-jwt-verifier
  [{:keys [algo] :as claims}]
  (let [verifier
        (reduce-kv
          (fn [ver k v]
              (let [f (get jwt-verify-fn k jwt-verify-withclaim)]
                (f ver k v)))
          (JWT/require (create-algorithm algo))
          (dissoc claims :algo))]
    (.build verifier)))

(defn build-jwtcreator
  [{:keys [iss aud algo]}]

  (fn [sub]
      (-> (JWT/create)
        (.withSubject sub)
        (.withIssuedAt (Date.))
        (cond->
          iss (.withIssuer iss)
          aud (.withAudience aud))
        (.sign algo))))

(defn build-jwtverifier
  [{:keys [iss aud algo]}]
  (cond-> (JWT/require algo)
    iss (.withIssuer (strarr iss))
    aud (.withAudience (strarr aud))
    :always .build))

(defn build-jwtverifier-fn
  [m]
  (let [^JWTVerifier v (build-jwtverifier m)]
    (fn [^String s] (when s
                      (.verify v s)))))

(defmethod ig/init-key :user-api/cookie-processor
  [_ config]
  (-> config
    (update :algo create-algorithm)
    (utils/invoke-and-accumulate
      :create-fn build-defaults-cookiecreator
      :verify-fn build-jwtverifier-fn)))

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

(defn extract-claims-from-decoded
  "put interesting idtoken information into a clojure may"
  [^DecodedJWT jwt]
  (when jwt
    (into {}
      (map (fn [[k ^Claim v]]
               [(keyword k) (or
                              (.asBoolean v)
                              (.asDate v)
                              (.asDouble v)
                              (.asString v))])
        (.getClaims jwt)))))

(defn ^DecodedJWT verify-cookie
  [algo s]
  (.verify ^JWTVerifier (.build (JWT/require algo)) s))

(defn extract-claims
  "put interesting idtoken information into a clojure may"
  [idToken]
  (when idToken
    (extract-claims-from-decoded (JWT/decode idToken))))

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
  "insert or update user info."
  [{:keys [conn]} userinfo]
  (client/transact conn {:tx-data [(transform-userinfo userinfo)]}))

(defn extract-uid-from-cookie
  [ck]
  (when-let [s (:sub ck)]
    (UUID/fromString s)))

(def uia (atom nil))

(defn user-lookupref
  [uuid]
  [:user/uuid uuid])

(defn build-intercept-uid-from-cookie
  "interceptor that picks up uid cookie and reads it
  TODO what happens when verify fails?"
  [{:keys [verify-fn create-fn]}]
  (let [vfresh-uid (volatile! nil)]
    (interceptor/interceptor
      {:name ::uid-from-cookie
       :enter
       (fn [ctx]
           (let [uid (or
                       (-> (get-in ctx [:request :cookies "uid" :value])
                         verify-fn
                         extract-claims-from-decoded
                         extract-uid-from-cookie)
                       (vreset! vfresh-uid (UUID/randomUUID)))]
             (update ctx :request assoc
               :user/uuid uid
               :user/lookup-ref (user-lookupref uid))))

       :leave
       (fn [ctx]
           (if @vfresh-uid
             (let [ctx (update ctx :response assoc-in [:cookies "uid"] (create-fn {:sub (str @vfresh-uid)}))
                   ]
               (reset! reqa ctx)
               ctx)
             ctx))})))


(defmethod ig/init-key :user-api/uid-interceptor
  [_ {:keys [cookie-fns]}]
  (build-intercept-uid-from-cookie cookie-fns))

(defn do-logged-in
  [{:auth/keys [^AuthenticationController controller]
    :keys [cookies servlet-request servlet-response] :as request}]
  (try
    (let [toks (.handle controller servlet-request servlet-response)
          ;accessToken (.getAccessToken toks)
          idToken (.getIdToken toks)
          userinfo (extract-claims idToken)
          userinfo (medley/assoc-some userinfo :uuid (:user/uuid request))]
      (transact-userinfo request userinfo)
      (ring-resp/redirect main-page))
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
  [{:keys [db] :as req}]
  (ring-resp/response
    (-> (client/pull db '[*] (:user/lookup-ref req))
      (utils/update-some :user/picture str))))
