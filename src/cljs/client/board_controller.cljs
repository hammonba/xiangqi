(ns client.board-controller
  (:require [citrus.core :as citrus]))

(defmulti control-board (fn [event] (:action event event)))

(defmethod control-board :init
  [_]
  {:state {}})

(defmethod control-board :get-board
  [evt [args & _] state cofx]
  {:websocket-post (assoc args :action :get-board)})

(defmethod control-board :received-board
  [evt [msg & _] state cofx]
  {:state (merge state msg)})