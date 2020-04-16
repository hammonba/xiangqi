(ns xiangqi.pedestal.auth-controller
  (:require [ring.util.response :as ring-resp]
            [io.pedestal.http.route :as route]
            [clojure.string :as string]
            [io.pedestal.log :as log]
            [fipp.edn])
  (:import
   [com.auth0.jwk JwkProviderBuilder]
   [com.auth0 AuthenticationController IdentityVerificationException]
   [com.auth0.client.auth AuthAPI AuthorizeUrlBuilder]
   [com.auth0.json.auth UserInfo]
   [com.auth0.jwt JWT JWTCreator JWTCreator$Builder]
   [com.auth0.jwt.interfaces DecodedJWT Claim]
   [com.auth0.jwt.impl JsonNodeClaim]
   [com.auth0.jwt.algorithms Algorithm]))

(def main-page "/index.html")
(def login-fail "/login-fail.html")

(defn ^AuthenticationController build-auth0-controller
  [{:keys [domain clientId clientSecret]}]
  {:pre [(not-any? string/blank? [domain clientId clientSecret])]}
  (let [jwk (.build (JwkProviderBuilder. domain))]
    (->
      (AuthenticationController/newBuilder domain clientId clientSecret)
      (.withJwkProvider jwk)
      (.build))))

(defn build-login-redirect-uri
  [{:keys [scheme server-name server-port]}]
  (format "%s://%s:%d%s"
    (name scheme)
    server-name
    server-port
    (route/url-for :logged-in-get {})))

(defn build-auth0-login-uri
  [^AuthenticationController control {:keys [servlet-request servlet-response redirect-uri] :as request}]
  (->
    (.buildAuthorizeUrl control servlet-request servlet-response redirect-uri)
    (.withScope "openid profile email")
    (.build)))

;(defn build-auth0-login-uri2
;  [^AuthAPI auth {:keys [servlet-request servlet-response redirect-uri] :as request}]
;  (let [^AuthorizeUrlBuilder builder (doto (.authorizeUrl auth redirect-uri)
;                                       (.withScope "openid")
;                                       (.withResponseType "code")
;                                       (.withState))])
;  * AuthAPI auth = new AuthAPI("me.auth0.com", "B3c6RYhk1v9SbIJcRIOwu62gIUGsnze", "2679NfkaBn62e6w5E8zNEzjr-yWfkaBne");
;  * String url = auth.authorizeUrl("https://me.auth0.com/callback")
;  *      .withConnection("facebook")
;  *      .withAudience("https://api.me.auth0.com/users")
;  *      .withScope("openid contacts")
;  *      .withState("my-custom-state")
;  *      .build();)

(defn do-login-redirect
  "generate a 302 redirect for auth0"
  [^AuthenticationController control request]
  (->>
    (build-login-redirect-uri request)
    (assoc request :redirect-uri)
    (build-auth0-login-uri control)
    (ring-resp/redirect)))

(defn extract-claims
  "put interesting idtoken information into a clojure may"
  [idToken]
  (let [^DecodedJWT jwt (JWT/decode idToken)]
    (into {}
      (map (fn [[k ^Claim v]]
               [(keyword k) (or
                              (.asBoolean v)
                              (.asDate v)
                              (.asDouble v)
                              (.asString v))])
        (.getClaims jwt)))))

(defn build-auth-cookie
  "an authorisation cookie of our very own"
  []
  (let [algorithm (Algorithm/HMAC256 "secret")]
    (-> (JWT/create)
      (.withIssuer "mememe")
      (.sign algorithm))))

(defn do-logged-in
  [^AuthenticationController control {:keys [servlet-request servlet-response]}]
  (try
    (let [toks (.handle control servlet-request servlet-response)
          accessToken (.getAccessToken toks)
          idToken (.getIdToken toks)
          userinfo (extract-claims idToken)
          my-auth-cookie nil]

      ;; TODO retrieve user details from auth0
      ;; determine xiangqi user Id
      ;; drop a login coookie for it
      (-> (ring-resp/redirect main-page)
        (ring-resp/set-cookie "user" (build-auth-cookie))))
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
