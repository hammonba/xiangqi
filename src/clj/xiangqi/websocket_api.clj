(ns xiangqi.websocket-api
  (:require [integrant.core :as ig]
            [io.pedestal.http.jetty.websockets :as jetty.websockets]
            [io.pedestal.log :as log])
  (:import [org.eclipse.jetty.servlet ServletContextHandler$Context ServletContextHandler]
           [org.eclipse.jetty.websocket.api Session]
           [org.eclipse.jetty.websocket.servlet ServletUpgradeResponse ServletUpgradeRequest]))

(defrecord Conn [key ^Session ws-session send-chs])

(defn on-connect
  [key ^Session ws-session send-ch]
  (->Conn key ws-session send-ch))

(defn on-close
  [^Conn conn status-code reason]
  (log/info :fn :on-close :key (:key conn) :status-code status-code :reason reason)
  nil)

(defn on-error
  [^Conn conn cause]
  (log/warn :fn :on-error :key (:key conn) :exception cause)
  conn)

(defn on-text
  [^Conn conn msg]
  (log/info :fn :on-text :key (:key conn) :msg msg)
  conn)

(defn on-binary
  [^Conn conn payload offset length]
  (log/info :fn :on-binary :key (:key conn) :offset offset :length length)
  conn)

(defn websocket-routes
  [ws-sessions]
  {"/ws"
   (fn []
       (let [key (Object.)]
         {:on-connect (jetty.websockets/start-ws-connection
                        (fn [ws-session ch]
                            (swap! ws-sessions assoc key (on-connect key ws-session ch))))
          :on-text (fn [msg]
                       (swap! ws-sessions update key on-text msg))
          :on-binary (fn [payload offset length]
                         (swap! ws-sessions update key on-binary payload offset length))
          :on-error (fn [cause]
                        (swap! ws-sessions update key on-error cause))
          :on-close (fn [status-code reason]
                        (on-close (get @ws-sessions key) status-code reason)
                        (swap! ws-sessions dissoc key))}))})

(defn ws-stateful-listener
  [^ServletUpgradeRequest req
   ^ServletUpgradeResponse resp
   wsmap-genfn]
  (jetty.websockets/make-ws-listener (wsmap-genfn)))

(defn build-context-configurator-addin
  "takes an atom with which to store websocket state updates
   an a ServletContextHandler to add ourselves in to "
  [ws-sessions ^ServletContextHandler sch]
  (jetty.websockets/add-ws-endpoints
    sch
    (websocket-routes ws-sessions)
    {:listener-fn ws-stateful-listener}))

(defmethod ig/init-key :component/websocket
  [_ config]
  (let [ws-sessions (atom {})]
    (assoc-in config
      [:add-ins
       :io.pedestal.http/container-options
       :context-configurator]
      #(build-context-configurator-addin ws-sessions %)))
  )
