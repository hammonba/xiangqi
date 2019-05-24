(ns client.router
  (:require [bide.core :as bide]
            [citrus.core :as citrus]))

(def board ::board)

(def routes
  (bide/router [["/board/:board-ident" board]]))

(defn navigate!
  [& args]
  (apply bide/navigate! routes args))

(defn on-navigate [reconciler route-name route-params route-query]
  (let [p (condp = route-name
            board (assoc route-params :action :get-board))]
    (citrus/dispatch-sync! reconciler :board-controller :get-board p)))

(defn start! [reconciler]
  (bide/start! routes
    {:default board
     :on-navigate #(on-navigate reconciler %1 %2 %3)}))
