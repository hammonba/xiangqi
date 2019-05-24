(ns client.effects
  (:require [client.router :as router]
            [citrus.core :as citrus]))

(defn navigate [reconciler controller [[route args] & _]]
  (router/navigate! route args))

(defn websocket-post [reconciler controller event]
  (citrus/dispatch! reconciler :websocket-controller (:action event) event))



(defn controller
  "event is an effect name args are the event args"
  [event args state cofx]
  (when (not= :init event)
    {event args}))