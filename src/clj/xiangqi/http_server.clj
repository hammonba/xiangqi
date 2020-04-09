(ns xiangqi.http-server
  "abstraction of pedestal endpoint that can have vase apis loaded in to i"
  (:require [clojure.tools.logging :as clog]
            [com.cognitect.vase.api :as vase.api]
            [io.pedestal.http :as pedestal.http]
            [io.pedestal.http.route :as http.route]

            [integrant.core :as ig]
            [io.pedestal.http :as http]
            [io.pedestal.log :as log]
            [xiangqi.utils :as utils])
  (:import [org.eclipse.jetty.server Server]))

;;; Response helpers

(defn ok
  [s]
  {:status 200
   :body s})

(defn bad-request
  [s]
  {:status 400
   :body s})

(defn with-headers
  [r h]
  (assoc r :headers h))


(def our-routes
  #{["/healthcheck" :get `healthcheck]})

(defn healthcheck
  "Pedestal response handler: simple liveness check."
  [_]
  (ok "OK"))

(defn reinstate-routes
  "our original routes have been lost"
  [svcmap original other-routes]
  (-> svcmap
    (update ::pedestal.http/routes into (::pedestal.http/routes original))
    (update ::pedestal.http/routes into other-routes)))

(defn mixin-apis
  [{:keys [service api]} extra-routes]
  (if-some [apises ({} not-empty (mapcat :apis api))]
    (reinstate-routes
      (vase.api/service-map {:apis apises} service)
      service
      extra-routes)
    (update service ::pedestal.http/routes utils/into-set extra-routes)))

(defn process-addins
  "addins are functions that need to be composed onto the
  service map"
  [service all-addins]
  (reduce
    (fn [svc {:keys [add-ins]}]
        (update svc merge-with comp add-ins))
    service
    all-addins))

(defmethod ig/init-key :component/endpoint
  [_ {:keys [service addins] :as this}]
  (->
    (mixin-apis this our-routes)
    (process-addins addins)
    http/default-interceptors
    (cond-> (::http/dev? service) http/dev-interceptors)
    (vase.api/start-service)))

(defmethod ig/halt-key! :component/endpoint
  [_ http-service]
  (log/info :message "Stopping HTTP endpoint" :port (::http/port http-service))
  (try (http/stop http-service)
       (catch Throwable t
         (log/error :message "Error in Pedestal stop" :exception t))))

(defn ^Server server-obj
  [{::http/keys [server]}]
  server)

(defn port
  [component]
  (some->
    (server-obj component)
    .getConnectors
    (aget 0)
    .getLocalPort))

(defn join
  [component]
  (.join (server-obj component)))
