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
            ["@material-ui/core/FormLabel" :as form-label]
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
            [xframe.core.alpha :as xf]
            [clojure.string :as string]))

(def ^:const dbk :uix/newgame-dialog)

(def ^:const dbk2 *ns*)

(xf/reg-event-db ::db-init
  (fn [db _] db))

(defonce init-db
  (xf/dispatch [::db-init]))

(xf/reg-sub ::visible
  (fn [] (xf/<- [[::xf/db] dbk])))

(def player-config
  {true {:domain :player/red
         :label "RED"}
   false {:domain :player/black
          :label "BLACK"}
   :default-key true})

(defn new-switchstate
  [f v]
  (.log js/console "v is " v)
  (.log js/console "fv is " (f v))
  (let [m2 (f v)
        bb (boolean v)
        m3 (assoc m2 :checked bb)]
    m3))

(defn default-switchstate
  [f]
  (->> (f :default-key)
    (new-switchstate f)))

(defn switchgroup
  [{:keys [title vals-fn output]}]
  (let [mystate* (uix/state (default-switchstate vals-fn))
        change-fn (fn [evt]
                    (let [chk (boolean js/evt.target.checked)]
                      (let [newstate (new-switchstate vals-fn chk)]
                        (reset! output (:domain newstate))
                        (reset! mystate* newstate))))]
    [:> form-group/default {:row true}
     (when-not (string/blank? title)
       [:> form-label/default {:component "legend"} title])
     [:> form-controllabel/default
      {:label (:label @mystate*)
       :control
       (uix/as-element
         [:> switch/default
          {:checked (:checked @mystate*)
           :onChange change-fn}])}]
     ]))

(def invitation-config
  {false {:label "Anyone"
          :domain :opponent/anyone}
   true {:label "Invite Only"
         :domain :opponent/invite-only}
   :default-key false})

(defn newgame-dialog
  [{app-state* :state*}]
  (let [visible* (uix/cursor-in app-state* [:visible])
        mystate* (uix/state {:player (:domain (default-switchstate player-config))
                             :invitation (:domain (default-switchstate invitation-config))})
        close-fn (fn [evt] (reset! visible* false))]
    [:> dialog/default {:open @visible* :onClose close-fn}
     [:> dialog-title/default {:id :newgame-dialog-title} "Create New Game"]
     [:> form-group/default {:row false}]
     [switchgroup {:title "Play As:"
                   :vals-fn player-config
                   :output (uix/cursor-in mystate* [:player])}]
     [switchgroup {:title "Invite Opponent:"
                   :vals-fn invitation-config
                   :output (uix/cursor-in mystate* [:invitation])}]

     [:> dialog-content/default nil
      [:> dialog-contenttext/default nil
       "this is a dialog content text"]]
     [:> dialog-actions/default nil
      [:> button/default {:color "primary" :onClick close-fn}
       "Create"]
      [:> button/default {:color "primary" :onClick close-fn}
       "Cancel"]]
     ]))
