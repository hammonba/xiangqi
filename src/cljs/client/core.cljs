(ns ^:figwheel-hooks client.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [goog.dom :as gdom]
   [rum.core :as rum]
   [cljs.core.async :as a :refer [<! >!]]
   [haslett.client :as ws]
   [haslett.format :as fmt]
   [client.board-view :as board-view]
   [client.effects :as effects]
   [client.router :as router]
   [client.board-controller :as board-controller]
   [client.websocket-controller :as websocket-controller]
   [citrus.core :as citrus]))



(enable-console-print!)

(def initial-state 0)


(defmulti control (fn [event] event))


;; define your app data so that it doesn't get over-written on reload
(defn multiply [a b] (* a b))

(defonce app-state (atom {:text "Hello world!"}))

(defn get-app-element []
  (gdom/getElement "app"))
;; initialize controllers
(defonce reconciler
  (citrus/reconciler
    {:state (atom {})
     :controllers {:board-controller board-controller/control-board
                   :websocket-controller websocket-controller/control-ws
                   :effects effects/controller}
     :effect-handlers {:navigate effects/navigate
                       :websocket-post effects/websocket-post
                       :websocket-readloop websocket-controller/websocket-readloop}}))

(defonce init-ctrl
  (citrus/broadcast-sync! reconciler :init))


(rum/defc app-component []
  [:div
   (board-view/board-disposition
     reconciler)
   ])

;; conditionally start your application based on the presence of an "app" element
;; this is particularly helpful for testing this ns without launching the app

(defn mount-app-element []
  (when-let [el (get-app-element)]
    (rum/mount (app-component) el)))

(defn main []
  (router/start! reconciler)
  (mount-app-element))

(defonce init (main))

;; specify reload hook with ^;after-load metadata
(defn ^:after-load on-reload []
  (mount-app-element)
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )
