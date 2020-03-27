(ns uix.control-board
  "manages a single board view state"
  (:require [uix.view-board]
            [uix.websocket :as ws]
            [xframe.core.alpha :as xf]))

(def dbk :uix/control-board)

(xf/reg-event-db :uix.control-board/db-init
  (fn [db _]
    #_(.log js/console (str ::db-init))
    (if (:uix/control-board db)
      db
      (assoc db :uix/control-board
                {:board-description :test-thingy #_uix.view-board/initial-state
                 :opened-move nil
                 :updatetime (.getTime (js/Date.))}))))

(defonce init-db
  (xf/dispatch [:uix.control-board/db-init]))

(xf/reg-event-fx ::fetch-board
  (fn [db [_ board-ident]]
    {:db
     (update db :uix/control-board assoc
       :board-ident board-ident
       :fetching true
       :updatetime (.getTime (js/Date.)))

     ::ws/dispatch-or-queue
       [::ws/send {:msg {:action :get-board :board-ident board-ident}
                   :on-ok ::fetch-board-ok
                   :on-failed ::fetch-board-failed}]}))

(xf/reg-event-db ::fetch-board-ok
  (fn [db [_ {:keys [resp]}]]
    #_(.log js/console (str ::fetch-board-ok) resp)
    (update db :uix/control-board assoc
      :board-description resp
      :fetching false
      :updatetime (.getTime (js/Date.)))))

(xf/reg-sub :db/control-board
  (fn []
    #_(.log js/console (str :db/control-board)
      (:uix/control-board (xf/<- [::xf/db])))
    (:uix/control-board (xf/<- [::xf/db]))))

(xf/reg-sub :uix.control-board/board-description
  (fn []
    #_(.log js/console (str :uix.control-board/board-description)
      (:board-description (xf/<- [:db/control-board])))
    (:board-description (xf/<- [:db/control-board]))
    #_(get-in (xf/<- [::xf/db])
      [:uix/control-board :board-description])))
