(ns xiangqi.pedestal.server
  (:require [io.pedestal.http :as server]
            [io.pedestal.http.route :as route]
            [xiangqi.pedestal.service :as xiangqi-service]
            [clojure.tools.logging :as log]))

(defonce runnable-service (server/create-server xiangqi-service/service))

(defn run-dev
  [& args]
  (println "\nCreating your [DEV] server...")
  (->
    xiangqi-service/service
    (assoc :env :dev
           ::server/join? false
           ::server/resource-path "public"
           ::server/routes (fn [] (route/expand-routes (deref #'xiangqi-service/routes)))
           ::server/allowed-origins {:creds true :allowed-origins (constantly true)}
           ::server/secure-headers
           {:content-security-policy-settings
            {:object-src "'none'"
             :script-src "'self' 'unsafe-inline' 'unsafe-eval'"
             :default-src "'self'"
             :img-src "http: https: data:"
             :style-src "* 'unsafe-inline'"
             :connect-src "ws: http: https:"
             :frame-src "https://dev-e0x9wgap.eu.auth0.com"}})
    server/default-interceptors
    server/dev-interceptors
    server/create-server
    server/start))


(defn -main
  [& args]
  (println "\nCreating server...")
  (server/start runnable-service))

