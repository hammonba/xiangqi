(ns uix.app
  (:require
   [clojure.core.async :as async]
   ["@material-ui/core/styles" :as styles]
   [uix.control-auth]
   [uix.control-board]
   [uix.navbar :refer [navbar]]
   [uix.view-board :as board]
   [uix.boiling :as boiling]
   [uix.chat :as chat]
   [uix.past-moves :as past-moves]

   [uix.dom.alpha :as uix.dom]
   [uix.core.alpha :as uix :refer [defui]]
   [uix.websocket :as websocket]

   [xframe.core.alpha :as xf]

   ["@material-ui/core/Box" :as box]
   ["@material-ui/core/Container" :as container]
   ["@material-ui/core/styles" :as styles]
   ["react" :as react]
   ["react-dom" :refer (render)]
   ["react-router-dom" :as router]
   ["react-router" :as react-router]
   ))

(def theme (styles/createMuiTheme #js {:background "linear-gradient(45deg, #FE6B8B 30%, #FF8E53 90%)"}))

(defn with-route
  [props child]
  [:> router/Route
   (assoc props :render (fn [m] (uix/as-element [child m])))])

(defn board-from-id
  []
  (let [m (router/useParams)]
    (when js/m.id
      (xf/dispatch [:uix.control-board/fetch-board js/m.id]))
    [board/draw-board]))

(defn app []
  [:> styles/ThemeProvider {:theme theme}
   [:> router/HashRouter
    [:div
     [navbar nil nil]
     [:> router/Switch
      [:> router/Route {:path "/board/:id"}
       [board-from-id]]
      [:> router/Route {:path "/calc"}
       [boiling/calculator]]
      [:> router/Route {:path "/"}
       [:> box/default {:display "flex" :flex-direction "column"}
        [board/draw-board]

        [:> box/default {:display "flex" :flex-direction "row"}
         [:> box/default {:flex-grow "1"}
          [chat/box]]
         [:> box/default {:flex-grow "1"}
          [past-moves/box]]]]
       ]]
     ]]])


(defn ^:export main
  []
  (uix.dom/render [app] (.getElementById js/document "app"))
  ;document.getElementById("essveegee").height.baseVal.newValueSpecifiedUnits(SVGLength.SVG_LENGTHTYPE_PX, document.documentElement.clientHeight);
  ;;TODO resize board here!!!
  #_(let [eltApp (.getElementById js/document "app")
        w (.-clientWidth eltApp)
        h (.-clientHeight eltApp)
        d (min w (* h 0.9))
        eltSvg (.getElementById js/document "outer-svg")]
    (.log js/console "d is " d)
    (set! (.-value (.-width eltSvg)) d)))

(websocket/create-websocket)
