(ns uix.list-games
  (:require [xframe.core.alpha :as xf]))

#_(xf/reg-sub ::all
  (fn []
    (xf/<- [:uix/control-games])))

(defn games-list
  []
  (into [:ul]
    (map (fn [[_ g]] [:li (str g)]))
    (xf/<sub [:uix/control-games])))
