(ns uix.control-board
  "manages a single board view state"
  (:require [uix.view-board]
            [uix.websocket :as ws]
            [xframe.core.alpha :as xf]))

(def ^:const dbk :uix/control-board)

(xf/reg-event-db :uix.control-board/db-init
  (fn [db _]
    (assoc db dbk
              {:board-description nil
               :opened-move nil
               :updatetime (.getTime (js/Date.))})))

(defonce init-db
  (xf/dispatch [:uix.control-board/db-init]))

(xf/reg-event-fx ::fetch-board
  (fn [db [_ board-ident]]
    {:db
     (update db dbk assoc
       :board-ident board-ident
       :fetching true
       :updatetime (.getTime (js/Date.)))

     ::ws/dispatch-or-queue
       [::ws/send {:msg {:action :get-board :board-ident board-ident}
                   :on-ok ::fetch-board-ok
                   :on-failed ::fetch-board-failed}]}))

(xf/reg-event-db ::fetch-board-ok
  (fn [db [_ {:keys [resp]}]]
    (update db dbk assoc
      :board-description resp
      :fetching false
      :updatetime (.getTime (js/Date.)))))

(xf/reg-sub dbk
  (fn []
    (get (xf/<- [::xf/db]) dbk)))

(xf/reg-sub :uix.control-board/board-description
  (fn []
    (:board-description (xf/<- [dbk]))))
