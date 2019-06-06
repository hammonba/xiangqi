(ns xiangqi.pedestal.service
  (:require [io.pedestal.http :as http]
            [io.pedestal.log :as log]
            [io.pedestal.http.route :as route]
            [ring.util.response :as ring-resp]
            [clojure.core.async :as async]
            [io.pedestal.http.jetty.websockets :as ws]
            [io.pedestal.http.body-params :as body-params]
            [xiangqi.pedestal.board]
            [xiangqi.pedestal.websockets :as websock-server])
  (:import [org.eclipse.jetty.websocket.api Session]
           [org.eclipse.jetty.websocket.servlet ServletUpgradeRequest ServletUpgradeResponse]))

(defn about-page
  [request]
  (ring-resp/response (format "Clojure %s - served from %s"
                        (clojure-version)
                        (route/url-for ::about-page))))

(defn home-page
  [request]
  (ring-resp/response "Hello World!"))

(def common-interceptors [(body-params/body-params)])

(def routes #{["/" :get (conj common-interceptors `home-page)]
              ["/about" :get (conj common-interceptors `about-page)]
              ["/board" :get
               (conj common-interceptors #'xiangqi.pedestal.board/board-interceptor)
               :route-name ::board-default]
              ["/board/:board-ident" :get
               (conj common-interceptors #'xiangqi.pedestal.board/board-interceptor)
               :route-name ::board]})

(def ws-clients (atom {}))

(defn ws-paths
  [sesh-atom]
  {"/ws" (fn []
             (let [sendchp (promise)]
               {:on-connect (ws/start-ws-connection
                              (fn on-connect* [ws-session send-ch]
                                  (deliver sendchp send-ch)
                                  (swap! sesh-atom assoc send-ch ws-session)))
                :on-text #(websock-server/on-text @sendchp %1)
                :on-binary #(websock-server/on-binary @sendchp %1 %2 %3)
                :on-error #(websock-server/on-error @sendchp %1)
                :on-close #(websock-server/on-close @sendchp %1 %2)}))})

(defn ws-stateful-listener
  [^ServletUpgradeRequest req ^ServletUpgradeResponse resp wsmap-genfn]
  (ws/make-ws-listener (wsmap-genfn)))

(def service
  (let [ws-sessions (atom {})]
    {:env :prod
     ::ws-sessions ws-sessions
     ::http/routes routes
     ::http/resource-path "/public"
     ::http/type :jetty
     ::http/container-options {:context-configurator
                               #(ws/add-ws-endpoints
                                  %
                                  (ws-paths ws-sessions)
                                  {:listener-fn ws-stateful-listener})}
     ::http/port 8080}))