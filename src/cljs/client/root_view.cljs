(ns client.root-view
  (:require [rum.core :as rum]
            [client.board-view :as board-view]
            [cljsjs.material-ui :as mui]
            [cljs-react-material-ui.core :refer [color get-mui-theme]]
            [cljs-react-material-ui.rum :as ui]
            [cljs-react-material-ui.icons :as icons]))

(rum/defc nav-bar < rum/static
  []
  (ui/app-bar {:title "xiangqi"}
    (ui/raised-button {:label "new-game"})))

(defn svg-single
  [r]
  [:svg {:viewBox "0 0 10 12" :xmlns "http://www.w3.org/2000/svg" :version "1.1"}
   (board-view/board-disposition r)])

(rum/defc root < rum/reactive
  [r]
  #_(board-view/board-disposition r)
  (ui/mui-theme-provider {:theme (get-mui-theme {})}
    [:div (#_nav-bar)
     [:div {:id "board-disp"}
      #_[:h1 "this is an haitch one"]
      #_(otherlayouts r)
      #_(autogridboards2 r)
      #_(autogridboards3 r)
      (svg-single r)]])
  #_(ui/mui-theme-provider
    {:mui-theme (get-mui-theme {})}
    [:div #_(nav-bar)
     [:div {:id "board-disp"}
      (autogridboards2 r)]])
  )
