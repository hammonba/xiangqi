(ns xiangqi.pedestal.server
  (:require [io.pedestal.http :as server]
            [io.pedestal.http.route :as route]
            [xiangqi.pedestal.service :as xiangqi-service]))

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
           ::server/allowed-origins {:creds true :allowed-origins (constantly true)})
    server/default-interceptors
    server/dev-interceptors
    server/create-server
    server/start))

(defn -main
  [& args]
  (println "\nCreating server...")
  (server/start runnable-service))

