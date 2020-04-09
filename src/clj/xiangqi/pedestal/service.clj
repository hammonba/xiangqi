(ns xiangqi.pedestal.service
  (:require [io.pedestal.http :as http]
            [io.pedestal.log :as log]
            [io.pedestal.http.route :as route]
            [ring.util.response :as ring-resp]
            [ring.middleware.session.cookie :as cookie]
            [clojure.core.async :as async]
            [io.pedestal.http.jetty.websockets :as ws]
            [io.pedestal.http.body-params :as body-params]
            [xiangqi.pedestal.auth-controller :as auth-controller]
            [xiangqi.pedestal.board]
            [xiangqi.pedestal.websockets :as websock-server]
            [io.pedestal.http.ring-middlewares :as middlewares])
  (:import [org.eclipse.jetty.websocket.api Session WebSocketAdapter]
           [org.eclipse.jetty.websocket.servlet ServletUpgradeRequest ServletUpgradeResponse]
           [org.eclipse.jetty.servlet ServletContextHandler]
           [org.eclipse.jetty.server.session SessionHandler]
           [java.util UUID]))

(defn about-page
  [request]
  (ring-resp/response (format "Clojure %s - served from %s"
                        (clojure-version)
                        (route/url-for ::about-page))))

(defn home-page
  [request]
  (ring-resp/response "Hello World!"))

(def common-interceptors [(body-params/body-params)
                          (middlewares/session {:store (cookie/cookie-store)})])

(let [auth0 (auth-controller/build-auth0-controller
              {:domain "dev-e0x9wgap.eu.auth0.com"
               :clientId "ITQvmR3b13hmc4eL4Ct60Gipr3OUdIyr"
               :clientSecret "wkrtUvUfhJYbPEnm64vhNrP0hBgdSHGzlpFQPLDT9SPpPUEhQS5lfRj-XS10Pc06"
               })]
  (def routes #{
                ["/do-login" :get #(@#'auth-controller/do-login-redirect auth0 %) :route-name :do-login]
                ["/logged-in" :get (conj common-interceptors #(@#'auth-controller/do-logged-in auth0 %)) :route-name :logged-in-get]
                ["/logged-in" :post (conj common-interceptors #(@#'auth-controller/do-logged-in auth0 %)) :route-name :logged-in-post]
                ["/login-failed" :post #'auth-controller/do-login-failed :route-name :login-failed]
                ["/do-logout" :post #'auth-controller/do-logout :route-name :do-logout]
                ["/logged-out" :post #'auth-controller/logged-out :route-name :logged-out]
                ["/srv/about" :get (conj common-interceptors `about-page)]
                ["/srv/board" :get
                 (conj common-interceptors #'xiangqi.pedestal.board/board-interceptor)
                 :route-name ::board-default]
                ["/srv/board/:board-ident" :get
                 (conj common-interceptors #'xiangqi.pedestal.board/board-interceptor)
                 :route-name ::board]
                ["/hello" :get (conj common-interceptors `home-page)]
                #_["/*" :get (conj common-interceptors (middlewares/resource "/resources/public"))]}))

(def ws-clients (atom {}))

(defn ws-paths
  [sesh-atom]
  {"/ws" (fn [uuid]
             (.printStackTrace (ex-info "ws thunk just called!!!" {:uuid uuid}))
             (let [sendchp (promise)]
               {:on-connect (ws/start-ws-connection
                              (fn on-connect* [^Session ws-session send-ch]
                                  (.printStackTrace (ex-info "this is a test stacktrace" {}))
                                  #_(.getCookies (.getUpgradeRequest ws-session))
                                  (deliver sendchp send-ch)
                                  (swap! sesh-atom assoc uuid {:channel send-ch
                                                               :session ws-session})))
                :on-text #(websock-server/on-text @sendchp %1)
                :on-binary #(websock-server/on-binary @sendchp %1 %2 %3)
                :on-error #(websock-server/on-error @sendchp %1)
                :on-close #(websock-server/on-close @sendchp %1 %2)}))})

(defn ws-listenerX
  "Hindol Adyha on pedestala Slack Channel"
  [_request _response ws-map]
  (proxy [WebSocketAdapter] []
    (onWebSocketConnect [^Session ws-session]
      (proxy-super onWebSocketConnect ws-session)
      (when-let [f (:on-connect ws-map)]
        (f ws-session)))
    (onWebSocketClose [status-code reason]
      (when-let [f (:on-close ws-map)]
        (f (.getSession this) status-code reason)))
    (onWebSocketError [^Throwable e]
      (when-let [f (:on-error ws-map)]
        (f (.getSession this) e)))
    (onWebSocketText [^String message]
      (when-let [f (:on-text ws-map)]
        (f (.getSession this) message)))
    (onWebSocketBinary [^bytes payload offset length]
      (when-let [f (:on-binary ws-map)]
        (f (.getSession this) payload offset length)))))

(defn ws-stateful-listener
  [^ServletUpgradeRequest req
   ^ServletUpgradeResponse resp
   wsmap-genfn]
  (ws/make-ws-listener (wsmap-genfn (UUID/randomUUID))))

(def service
  (let [ws-sessions (atom {})]
    {:env :prod
     ::ws-sessions ws-sessions
     ::http/routes routes
     ::http/resource-path "/resources/public"
     ::http/type :jetty
     ::http/container-options
     {:context-configurator
      (fn [^ServletContextHandler sch]
          (.insertHandler sch (SessionHandler.))
          ;(set! (.-_options sch) ServletContextHandler/SESSIONS)
          (ws/add-ws-endpoints
            sch
            (ws-paths ws-sessions)
            {:listener-fn ws-stateful-listener}))}
     ::http/port 8080}))
