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

(def board-msg
  (pr-str {:msg-type :board
           :disposition [{:x "0", :y "6", :piecename "red-soldier" :moves [{:x "0" :y "6"}]}
                         {:x "2", :y "6", :piecename "red-soldier"}
                         {:x "4", :y "6", :piecename "red-soldier"}
                         {:x "6", :y "6", :piecename "red-soldier"}
                         {:x "8", :y "6", :piecename "red-soldier"}
                         {:x 1, :y 7, :piecename "red-cannon" :moves [{:x 1 :y 8} {:x 1 :y 6} {:x 1 :y 5} {:x 1 :y 4} {:x 1 :y 3} {:x 1 :y 0}]}
                         {:x "7", :y "7", :piecename "red-cannon"}
                         {:x "0", :y "9", :piecename "red-chariot"}
                         {:x "8", :y "9", :piecename "red-chariot"}
                         {:x "1", :y "9", :piecename "red-horse"}
                         {:x "7", :y "9", :piecename "red-horse"}

                         {:x "2", :y "9", :piecename "red-minister"}
                         {:x "6", :y "9", :piecename "red-minister"}
                         {:x "3", :y "9", :piecename "red-guard"}
                         {:x "5", :y "9", :piecename "red-guard"}
                         {:x "4", :y "9", :piecename "red-marshal"}

                         {:x "0", :y "3", :piecename "black-private"}
                         {:x "2", :y "3", :piecename "black-private"}
                         {:x "4", :y "3", :piecename "black-private"}
                         {:x "6", :y "3", :piecename "black-private"}
                         {:x "8", :y "3", :piecename "black-private"}

                         {:x "1", :y "2", :piecename "black-catapult"}
                         {:x "7", :y "2", :piecename "black-catapult"}
                         {:x "0", :y "0", :piecename "black-chariot"}
                         {:x "8", :y "0", :piecename "black-chariot"}

                         {:x "1", :y "0", :piecename "black-horse"}
                         {:x "7", :y "0", :piecename "black-horse"}

                         {:x "2", :y "0", :piecename "black-elephant"}
                         {:x "6", :y "0", :piecename "black-elephant"}

                         {:x "3", :y "0", :piecename "black-guard"}
                         {:x "5", :y "0", :piecename "black-guard"}
                         {:x "4", :y "0", :piecename "black-general"}]}))

(defn new-ws-client
  [ws-session send-ch]
  (async/put! send-ch board-msg)
  (swap! ws-clients assoc ws-session send-ch))

(defn send-and-close! []
  (let [[ws-session send-ch] (first @ws-clients)]
    (async/put! send-ch "A message from the server")
    (async/close! send-ch)
    (swap! ws-clients dissoc ws-session)))

(defn send-message-to-all!
  [message]
  (doseq [[^Session session channel] @ws-clients]
    (when (.isOpen session)
      (async/put! channel message))))

#_(defn ws-paths
  [ws-sessions]
  {"/ws" {:on-connect (ws/start-ws-connection (websock-server/on-connect ws-sessions))
          :on-text (websock-server/on-text ws-sessions)
          :on-binary (websock-server/on-binary ws-sessions)
          :on-error (websock-server/on-error ws-sessions)
          :on-close (websock-server/on-close ws-sessions)}})

#_(defn ws-paths
  [ws-sessions]
  {"/ws" {:on-connect (ws/start-ws-connection (websock-server/on-connect ws-sessions))
          :on-text (websock-server/on-text ws-sessions)
          :on-binary (websock-server/on-binary ws-sessions)
          :on-error (websock-server/on-error ws-sessions)
          :on-close (websock-server/on-close ws-sessions)}})

(def all-sessions (atom #{}))

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