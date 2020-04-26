(ns uix.newgame-dialog
  (:require [shadow.markup.react :as html :refer ($ defstyled)]
            [shadow.dom :as dom]
            ["react" :as react]
            ["react-dom" :refer (render)]
            ["react-router-dom" :as router]
            ["@material-ui/core/styles" :as styles]
            ["@material-ui/core/AppBar" :as appbar]
            ["@material-ui/core/Button" :as button]
            ["@material-ui/core/FormControlLabel" :as form-controllabel]
            ["@material-ui/core/FormGroup" :as form-group]
            ["@material-ui/core/IconButton" :as icon-button]
            ["@material-ui/core/MenuItem" :as menu-item]
            ["@material-ui/core/Menu" :as menu]
            ["@material-ui/core/Switch" :as switch]
            ["@material-ui/core/Toolbar" :as toolbar]
            ["@material-ui/core/Typography" :as typography]
            ["@material-ui/icons/AccountCircle" :as account-circle]
            ["@material-ui/icons/Menu" :as menu-icon]
            ["@material-ui/core/Dialog" :as dialog]
            ["@material-ui/core/DialogActions" :as dialog-actions]
            ["@material-ui/core/DialogContent" :as dialog-content]
            ["@material-ui/core/DialogContentText" :as dialog-contenttext]
            ["@material-ui/core/DialogTitle" :as dialog-title]
            [uix.core.alpha :as uix]
            [uix.compiler.aot :as compiler.aot]
            [uix.compiler.alpha :as compiler]
            [xframe.core.alpha :as xf]))

(def ^:const dbk :uix/newgame-dialog)

(def ^:const dbk2 *ns*)

(xf/reg-event-db ::db-init
  (fn [db _] db))

(defonce init-db
  (xf/dispatch [::db-init]))

(xf/reg-sub ::visible
  (fn [] (xf/<- [[::xf/db] dbk])))

(def playerswitch-config
  {true {:player :player/red
         :label "RED"}
   false {:player :player/black
          :label "BLACK"}})

(defn switchgroup
  [{:keys [state* vals-fn]}]
  (.log js/console "switchgroup: state* is " @state*)
  (let [label-val* (uix/cursor-in state* [:label])
        checked-val* (uix/cursor-in state* [:checked])
        domain-val* (uix/cursor-in state* [:domain])
        change-fn (fn [evt]
                    (.log js/console "switchgroup-changefn" evt)
                    (.log js/console "switchgroup-changefn evt-taraget-chkesked" js/evt.target.checked)
                    (.log js/console "switchgroup-change to" (vals-fn js/evt.target.checked))

                    (let [{:keys [label domain]}
                          (vals-fn js/evt.target.checked)]
                      (reset! label-val* label)
                      (reset! checked-val* js/evt.target.checked)
                      (reset! domain-val* domain)
                      (.log js/console "changefn: state* AFTER is " @state*)))]
    (uix/effect!
      (fn [] (when (nil? @state*)
               (.log js/console "switchgroup - setting default!")
               (reset! state* (merge {:checked false} (vals-fn :default)))))
      [])

    #_(when (nil? @label-val*)
        (reset! label-val* (:label (vals-fn :default))))
    #_(when (nil? @checked-val*)
      (reset! checked-val* (:checked (vals-fn :default))))
    [:> form-group/default {:row true}
     [:> form-controllabel/default
      {:label @label-val*
       :control
       (uix/as-element
         [:> switch/default
          {:checked @checked-val*
           :onChange change-fn}])}]
     ]))

(def inviteonly-statevals
  {false {:label "Oppenent coube be anyone"
          :domain :opponent/anyone}
   true {:label "Opponent must be invited"
         :domain :opponent/invite-only}
   :default {:label "Oppenent coube be anyone"
             :domain :opponent/anyone}})

(defn defaulting-cursor-in
  [ref path default]
  (let [r (uix/cursor-in ref path)]
    (when (nil? @r)
      (reset! r default))
    r))

(defn newgame-dialog
  [{:keys [state* toggle-createdialog]}]
  (let [visible* (uix/cursor-in state* [:visible])
        player* (uix/cursor-in state* [:player])
        playerlabel* (uix/cursor-in state* [:player-label])
        openclose* (uix/cursor-in state* [:openclose])
        name (uix/cursor-in state* [:name])
        close-fn (fn [evt] (reset! visible* false))
        play-switchval* (uix/state true)
        inviteonly-state* (defaulting-cursor-in state* [:invite-only] {})]
    (.log js/console "newgame-dialog state is " @state*)
    (.log js/console "visible* is " @visible*)
    (.log js/console "invite-only state is " @inviteonly-state*)
    [:> dialog/default {:open @visible* :onClose close-fn}
     [:> dialog-title/default {:id :newgame-dialog-title} "Create New Game"]
     [:> form-group/default {:row true}
      [:> form-controllabel/default
       {:label @playerlabel*
        :control (uix/as-element
                   [:> switch/default
                    {:checked @play-switchval*
                     :name "Player"
                     :onChange (fn [evt]
                                 (let [{:keys [player label]}
                                       (get playerswitch-config js/evt.target.checked)]
                                   (reset! player* player)
                                   (reset! playerlabel* label)
                                   (reset! play-switchval* js/evt.target.checked)))}])}]]
     [switchgroup {:state* inviteonly-state* :vals-fn inviteonly-statevals}]
     #_[:> form-controllabel/default
      {:label "Open"
       :control (uix/as-element [:> switch/default {:checked true}])}]

     [:> dialog-content/default nil
      [:> dialog-contenttext/default nil
       "this is a dialog content text"]]
     [:> dialog-actions/default nil
      [:> button/default {:color "primary" :onClick close-fn}
       "Create"]
      [:> button/default {:color "primary" :onClick close-fn}
       "Cancel"]]
     ]))
