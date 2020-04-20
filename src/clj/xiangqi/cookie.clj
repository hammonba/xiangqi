(ns xiangqi.cookie
  "cookie and jwt processing"
  (:require [xiangqi.utils :as utils]
            [integrant.core :as ig]
            [io.pedestal.interceptor :as interceptor]
            [java-time]
            [clojure.tools.logging :as clog])
  (:import [com.auth0.jwt JWTCreator$Builder JWTVerifier JWT]
           [com.auth0.jwt.interfaces Verification DecodedJWT Claim]
           [java.util UUID]
           [com.auth0.jwt.algorithms Algorithm]
           [java.time ZoneId]))

(def uid-cookiename
  "the userid cookie identifier"
  "uid")

(def jwt-create-fns
  {:iss #(.withIssuer ^JWTCreator$Builder %1 %3)
   :sub #(.withSubject ^JWTCreator$Builder %1 %3)
   :aud #(.withAudience ^JWTCreator$Builder %1 (utils/strarr %3))
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
  [:domain :expires :http-only
   :max-age :path :same-site :secure])

(defn cookie-creator
  [payload]
  (let [jwt (-> (apply dissoc payload cookie-keys)
              jwt-create-and-sign)]
    (-> payload
      (select-keys cookie-keys)
      (utils/assoc-someabsent :expires (str (:exp payload)))
      (assoc :value jwt))))

(def utc-timezoneid (ZoneId/of "UTC"))
(defn utc-javadate
  [jt]
  (-> jt
    (java-time/instant utc-timezoneid)
    java-time/to-java-date))

(defn uid-cookiefields
  [uuid]
  (let [t (java-time/zoned-date-time utc-timezoneid)]
    {:sub (str uuid)
     :iat (utc-javadate t)
     :exp (-> t
            (java-time/plus (java-time/years 1))
            (utc-javadate))}))

(defn build-defaults-cookiecreator
  "close over defaults, ensure they are added to future payloads"
  [defaults]
  (comp cookie-creator
    #(merge defaults (uid-cookiefields %))))

(def jwt-verify-fn
  {:iss #(.withIssuer ^Verification %1 (utils/strarr %3))
   :sub #(.withSubject ^Verification %1 %3)
   :aud #(.withAudience ^Verification %1 (utils/strarr %3))
   :leeway #(.acceptLeeway ^Verification %1 (long %3))
   :expiresAt #(.acceptExpiresAt ^Verification %1 (long %3))
   :notBefore #(.acceptNotBefore ^Verification %1 (long %3))
   :issuedAt #(.acceptIssuedAt ^Verification %1 (long %3))
   :jti #(.withJWTId ^Verification %1 %3)
   })

(defn jwt-verify-withclaim
  [^Verification verif ^String name ^String v]
  (.withClaim verif name v))

(defn build-jwt-verifier
  [{:keys [algo] :as config}]
  (let [verifier
        (reduce-kv
          (fn [ver k v]
              (let [f (get jwt-verify-fn k jwt-verify-withclaim)]
                (f ver k v)))
          (JWT/require algo)
          (apply dissoc config
            :create-fn :algo cookie-keys))]
    (.build verifier)))

(defn build-jwtverifier-fn
  [m]
  (let [^JWTVerifier v (build-jwt-verifier m)]
    (fn [^String s] (when s
                      (.verify v s)))))

(defn extract-claims-from-decoded
  "put interesting idtoken information into a clojure may"
  ([^DecodedJWT jwt]
   (extract-claims-from-decoded {} jwt))
  ([m ^DecodedJWT jwt]
   (when jwt
     (into m
       (map (fn [[k ^Claim v]]
                [(keyword k) (or
                               (.asBoolean v)
                               (.asDate v)
                               (.asDouble v)
                               (.asString v))])
         (.getClaims jwt))))))

(defn extract-claims
  "put interesting idtoken information into a clojure may"
  [m idToken]
  (when idToken
    (extract-claims-from-decoded m (JWT/decode idToken))))

(defn extract-uid-from-cookie
  [ck]
  (when-let [s (:sub ck)]
    (UUID/fromString s)))

(defmethod ig/init-key ::processor
  [_ config]
  (-> config
    (update :algo create-algorithm)
    (utils/invoke-and-accumulate
      :create-fn build-defaults-cookiecreator
      :verify-fn build-jwtverifier-fn)))

(defn user-lookupref
  [uuid]
  [:user/uuid uuid])

(defn verified-uid-from-cookie
  [{:keys [verify-fn]} s]
  (-> s
    verify-fn
    extract-claims-from-decoded
    extract-uid-from-cookie))

(defn build-intercept-uid-from-cookie
  "interceptor that picks up uid cookie and reads it
   also creates missing cookies"
  [{:keys [verify-fn create-fn]}]
  (let [vfresh-uid (volatile! nil)]
    (interceptor/interceptor
      {:name ::uid-from-cookie
       :enter
       (fn [ctx]
           (let [uid (or
                       (-> (get-in ctx [:request :cookies uid-cookiename :value])
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
             (update ctx :response
               assoc-in [:cookies uid-cookiename]
               (create-fn @vfresh-uid))
             ctx))})))


(defmethod ig/init-key ::uid-interceptor
  [_ {:keys [cookie-fns]}]
  (build-intercept-uid-from-cookie cookie-fns))

(defn extract-named-cookie
  "return the first cookie of the specified name
   this is usually (but not always) sorted out for us
   by ring.middleware.cookies/parse-cookie-header"
  [name cookie-strings]
  (transduce
    (comp
      (mapcat #(.split #";" %))
      (map #(.split #"=" %)))
    (completing
      (fn [_ [^String k v]]
          (when (= name (.trim k))
            (reduced v))))
    nil
    cookie-strings))

(defn extract-uid-cookie
  [s]
  (extract-named-cookie uid-cookiename s))
