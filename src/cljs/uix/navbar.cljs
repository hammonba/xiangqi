(ns uix.navbar
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
            ["@material-ui/core/Dialog" :as Dialog]
            ["@material-ui/core/DialogActions" :as DialogActions]
            ["@material-ui/core/DialogContent" :as DialogContent]
            ["@material-ui/core/DialogContentText" :as DialogContentText]
            ["@material-ui/core/DialogTitle" :as DialogTitle]
            [uix.core.alpha :as uix]
            [uix.compiler.aot :as compiler.aot]
            [uix.compiler.alpha :as compiler]))

(def generate-styles
  "returns a thunk which must be called from  within react component
   andwill generate-and-return style names"
  (styles/makeStyles
    (fn [theme]
      (clj->js {:root {:flexGrow 1}
                :menuButton {:marginRight (.spacing theme 2)}
                :title {:flexGrow 1}}))
    #js{:name "muix"}))

(defn link-to-calc []
  [:> router/Link {:to "/calc"}])



(defn navbar
  [{:keys [uistate* toggle-createdialog]}]
  (let [styles (generate-styles)
        navstate* (uix/state {:auth false :anchorEl nil :menuOpened nil :createDialogOpened false})
        auth* (uix/cursor-in navstate* [:auth])
        anchorEl* (uix/cursor-in navstate* [:anchorEl])
        menuOpened* (uix/cursor-in navstate* [:menuOpened])
        dialogCreateOpened* (uix/cursor-in uistate* [:dialog-create :visible])
        ]
    (.log js/console "navbar: uistate* is " @uistate*)
    (.log js/console "navbar: dialogCreateOpened* is " @dialogCreateOpened*)

    [:div {:className (.-root styles)}
     [:> form-group/default
      [:> form-controllabel/default
       {:control (uix/as-element
                   [:> switch/default
                    {:checked @auth* #_(:auth @navstate*)
                     :onChange #(swap! auth* not)
                     :aria-label "login switch"}])
        :label (if @auth* "Logout" "Login")}]]
     [:> appbar/default {:position "static"}
      [:> toolbar/default
       [:> icon-button/default {:edge "start"
                                :className js/styles.menuButton
                                :onClick (fn [evt]
                                           (reset! anchorEl* js/evt.currentTarget)
                                           (reset! menuOpened* :action)
                                           )
                                :color "inherit"
                                :aria-label "menu"}
        [:> menu-icon/default]]
       [:> menu/default
        {:id "menu-appbar-action"
         :anchorEl @anchorEl*
         :anchorOrigin #js {:vertical "top"
                            :horizontal "left"}
         :keepMounted true
         :transformOrigin #js {:vertical "top"
                               :horizontal "left"}
         :open (= :action @menuOpened*)
         :onClose #(reset! menuOpened* nil)}
        [:> menu-item/default {:onClick (fn [evt]
                                          (reset! anchorEl* nil)
                                          (reset! menuOpened* nil)
                                          (swap! dialogCreateOpened* not)
                                          #_(toggle-createdialog)
                                          #_(swap! uistate* assoc-in [:dialog-create :visible] true)
                                          (.log js/console "createDialogOpened*:" @dialogCreateOpened*)
                                          (.log js/console "createDialogOpened*:" (get-in @uistate* [:dialog-creat :visible])))}
         "New Game"
         #_[:> router/Link {:to "/calc"} "New Game"]]
        [:> menu-item/default {:onClick (fn [evt]
                                          (reset! anchorEl* nil)
                                          (reset! menuOpened* nil))}
         [:> router/Link {:to "/greeting"} "Existing Games"]]
        ]
       [:> typography/default {:variant "h6"
                               :className js/styles.title
                               :color "inherit"}
        "Elephant Chess"
        ]
       (when @auth*
         [:div
          [:> icon-button/default {:aria-label "account of current user"
                                   :aria-controls "menu-appbar"
                                   :aria-haspopup "true"
                                   :onClick (fn [evt]
                                              (reset! anchorEl* js/evt.currentTarget)
                                              (reset! menuOpened* :user))
                                   :color "inherit"}
           [:> account-circle/default]]
          [:> menu/default
           {:id "menu-appbar"
            :anchorEl @anchorEl*
            :anchorOrigin {:vertical "top"
                           :horizontal "right"}
            :keepMounted true
            :transformOrigin {:vertical "top"
                              :horizontal "right"}
            :open (= :user @menuOpened*)
            :onClose #(reset! menuOpened* nil)}
           [:> menu-item/default {:onClick (fn [evt] (reset! anchorEl* nil))}
            "Profile"]
           [:> menu-item/default {:onClick (fn [evt] (reset! anchorEl* nil))}
            "My Account"]
           ]])
       ]]
     #_[newgame-dialog createDialogOpened*]
     ]))
