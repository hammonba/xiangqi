(ns uix.control-board
  "manages a single board view state"
  (:require [uix.view-board]
            [uix.websocket :as ws]
            [xframe.core.alpha :as xf]))

(def dbk :uix/control-board)


(xf/reg-event-db ::db-init
  (fn [db _]
    (.log js/console (str ::db-init))
    (assoc db dbk
              {:board-description uix.view-board/initial-state
               :opened-move nil})))

(defonce init-db
  (xf/dispatch [::db-init]))

(xf/reg-event-fx ::fetch-board
  (fn [db [_ board-ident]]
    {:db (update db dbk assoc
           :board-ident board-ident
           :fetching true)
     ::ws/dispatch-or-queue
     [::ws/send {:msg {:action :get-board :board-ident board-ident}
                 :on-ok ::fetch-board-ok
                 :on-failed ::fetch-board-failed}]}))

(xf/reg-event-db ::fetch-board-ok
  (fn [db [_ {:keys [resp]}]]
    (.log js/console (str ::fetch-board-ok) resp)
    (update db dbk
      (fn [dbk] (assoc dbk :board-description resp
                           ::fetching false)))))

(xf/reg-sub ::board-description
  (fn []
    (.log js/console (str :board-description) (xf/<- [::xf/db]))
    (get-in (xf/<- [::xf/db])
      [dbk :board-description])))
