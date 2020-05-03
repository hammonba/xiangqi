(ns uix.control-games
  "manages game states"
  (:require [uix.websocket :as ws]
            [xframe.core.alpha :as xf]))

(def ^:const dbk :uix/control-games)

(xf/reg-event-db ::db-init
  (fn [db _]
    (assoc db dbk {})))

(defonce init-db
  (xf/dispatch [::db-init]))


(xf/reg-sub :uix/control-games
  (fn []
    (xf/<- [::xf/db] :uix/control-games)))

(xf/reg-event-db ::game-update
  (fn [db [_ {:keys [resp]}]]
    (let [{:game/keys [ident]
           :board/keys [ident-string]
           :as game} resp]
      (assoc-in db [dbk ident] (dissoc game :correlation-id)))))

(xf/reg-event-fx ::create-game
  (fn [db [_]]
    {::ws/send {:msg {:action :create-game}
                :on-ok ::game-update
                :on-failed ::game-create-failed}}))

(defn create-game
  [args]
  (xf/dispatch [::ws/send {:msg (assoc args :action :create-game)
                           :on-ok ::game-update
                           :on-failed ::game-create-failed}]))
