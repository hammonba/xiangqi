(ns uix.past-moves
  (:require ["@material-ui/core/Box" :as box]))

(defn box
  []
  [:<>
   [:h6 "Moves so far go in here"]
   [:ol
    [:li "first move"]
    [:li "second move"]]])
