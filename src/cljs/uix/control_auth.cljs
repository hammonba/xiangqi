(ns uix.control-auth
  (:require ["@auth0/auth0-spa-js" :as auth0]
            [xframe.core.alpha :as xf]))

(def ^:const dbk :uix/control-auth)

(def auth0-config
  #_{:domain "dev-e0x9wgap.eu.auth0.com"
      :clientID "nJk1iq3jzwOStNKx7zHnhHqhDeQ5TrUO"
      :redirectUri (str js/window.location.protocol "//" js/window.location.host)
      :audience "https://elephantchess.me/user"
      :responseType "token id_token"
      :scope "openid profile email"}
  #js {:domain "dev-e0x9wgap.eu.auth0.com"
   :client_id "nJk1iq3jzwOStNKx7zHnhHqhDeQ5TrUO"
   :redirect_uri (str js/window.location.protocol "//" js/window.location.host "/index.html")
   :scope "openid profile email"
   :response_type "token id_token"                          ;=code&
   ;:response_mode=query&
   ;:state=Qlpkb01HVnB6dkllNEpHc0owSUYwTTZNa0VwLTl%2BYTR1emR4WTZsMzVMZQ%3D%3D&
   ;:nonce=TVf293OBsU ~C1q0oxcL6kcF.ir8e ~E5ORzA ~cERe3Lk&
   ;:code_challenge=ARzbh4WErMEtjpVX32au9DCRDY1ciWU2jNxgxkBbCxg&
   ;:code_challenge_method=S256&
   ;:auth0Client=eyJuYW1lIjoiYXV0aDAtc3BhLWpzIiwidmVyc2lvbiI6IjEuNi41In0%3D
   })

(xf/reg-event-db ::store-auth0client
  (fn [db [_ c]]
    (let [lr (.loginWithRedirect c)]
      (.log js/console "loginWithRedirect: " lr))
    (assoc-in db [dbk ::client] c)))

;(xf/reg-event-db ::db-init
;  (fn [db _]
;    (let [p (->
;              (auth0 uth0-config)
;              (.then (fn [c]
;                       (.log js/console "auth-client constructed: " c)
;                       (xf/dispatch [::store-auth0client c])))
;              (.catch (fn [e]
;                        (.error js/console "auth-client caught: " e)))
;              (.finally (fn [c]
;                          (.log js/console "auth-client finally: " c)
;                          c)))]
;      (assoc db dbk {:auth0-promise p}))))
;
;(defonce init-db
;  (xf/dispatch [::db-init]))

(defn create-websocket
  []
  (xf/dispatch [::c-websocket]))
