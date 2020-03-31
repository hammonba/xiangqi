(ns uix.chat
  (:require ["@material-ui/core/Box" :as box]))

(defn box
  []
  [:<>
   [:h6 "Chats go in here"]
   [:ol
    [:li "blah"]
    [:li "bleugh"]]
   [:input {:value "new chat"}]
   ])

