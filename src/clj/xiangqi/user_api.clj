(ns xiangqi.user-api
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [datomic.client.api :as client]
            [integrant.core :as ig]
            [io.pedestal.interceptor :refer [interceptor]]
            [io.pedestal.http.route :as route]
            [ring.util.response :as ring-resp]
            [xiangqi.cookie :as cookie]
            [xiangqi.utils :as utils])
  (:import [com.auth0 AuthenticationController IdentityVerificationException]
           [com.auth0.jwk JwkProviderBuilder]
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

(defmethod ig/init-key :auth/controller
  [_ config]
  (let [auth-controller (build-auth0-controller config)]
    (assoc config
      :auth-controller auth-controller
      :mixin-authcontroller
      (interceptor
        {:name :mixin-authcontroller
         :enter #(assoc-in % [:request :auth/controller] auth-controller)}))))

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

;; auth0 requires the presencee of a org.eclipse.jetty.server.session.SessionHandler
(defmethod ig/init-key :jetty-addin/session-handler
  [_ config]
  (assoc-in config
    [:add-ins :io.pedestal.http/container-options :context-configurator]
    (fn [^ServletContextHandler sch]
        (.insertHandler sch (SessionHandler.))
        sch)))

(defn do-login-redirect
  "generate a 302 redirect for auth0"
  [request]
  (->>
    (route/url-for :user.v1/logged-in :absolute? true)
    (assoc request :redirect-uri)
    build-auth0-login-uri
    ring-resp/redirect))

(def userinfo-keymap
  "maps auth0 claims to datomic attributes"
  {:email :user/email
   :locale :user/locale
   :sub :user/ident
   :name :user/name
   :picture :user/picture
   :uuid :user/uuid})

(def user-tempid "tempid/user")
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

(defn transact-userinfo
  "insert or update user info."
  [{:keys [conn]} userinfo]
  (client/transact conn {:tx-data [(transform-userinfo userinfo)]}))

(defn do-logged-in
  [{:auth/keys [^AuthenticationController controller]
    :user/keys [uuid]
    :keys [servlet-request servlet-response] :as request}]
  (try
    (let [toks (.handle controller servlet-request servlet-response)
          ;accessToken (.getAccessToken toks)
          idToken (.getIdToken toks)
          userinfo (cookie/extract-claims {:uuid uuid} idToken)]
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

(defn all-uids-for-user
  [db uid]
  (sequence
    (mapcat #(mapcat :user/uuid %))
    (datomic.client.api/q
      {:query '[:find (pull ?e [:user/uuid])
                :in $ ?uid-in
                :where
                [?e :user/uuid ?uid-in]]
       :args [db uid]}))
  )

(defn pull-games-for-user
  [db all-uids]
  (client/q
    {:query '[:find (pull ?e [:board/ident-string
                              :game/title
                              :game/creator
                              :game/ident
                              :game/red-player
                              :game/black-player])
              :in $ [?uid ...]
              :where
              (or
                [?e :game/black-player ?uid]
                [?e :game/red-player ?uid])]
     :args [db all-uids]}))

(defn allgames-for-user
  [db-user db-game uid]
  (->>
    (all-uids-for-user db-user uid)
    (pull-games-for-user db-game)
    (mapcat identity)))

(defn pull-user-details
  [db lkr]
  (->
    (client/pull db '[*] lkr)
    (utils/update-some :user/picture str)))

(defn userdetails-plus-games
  [db-user db-game uid]
  (-> (pull-user-details db-user (xiangqi.cookie/user-lookupref uid))
    (assoc :games (allgames-for-user db-user db-game uid))))

(defn user-details
  "response for websocket"
  [{:keys [conn]} uid]
  (pull-user-details
    (client/db conn)
    (cookie/user-lookupref uid)))

(defn details
  "response for webservice"
  [{:keys [db] :as req}]
  (ring-resp/response
    (pull-user-details db (:user/lookup-ref req))))
