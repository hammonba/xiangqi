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
  (:import [org.eclipse.jetty.websocket.api Session]
           [org.eclipse.jetty.websocket.servlet ServletUpgradeRequest ServletUpgradeResponse]
           [org.eclipse.jetty.servlet ServletContextHandler]
           [org.eclipse.jetty.server.session SessionHandler]))

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
  {"/ws" (fn []
             (let [sendchp (promise)]
               {:on-connect (ws/start-ws-connection
                              (fn on-connect* [^Session ws-session send-ch]
                                  #_(.getCookies (.getUpgradeRequest ws-session))
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
