(ns client.board-composite
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

(defn fourboards
  [r]
  [:div {:id "board-disp"}
   [:svg {:viewBox "0 -1.3 19 22"}
    [:g {:transform "translate(0.0)"}
     (board-view/board-disposition r)]
    [:g {:transform "translate(9.5,0)"}
     (board-view/board-disposition r)]
    [:g {:transform "translate(0,10.5)"}
     (board-view/board-disposition r)]
    [:g {:transform "translate(9.5,10.5)"}
     (board-view/board-disposition r)]]])

(defn autogridboards
  [r]
  [:div {:class "set_width"}
   [:div {:class "on_top" :id "autogridboards"}
    (ui/grid-list {:cols 4 :spacing 0}
      (ui/grid-tile {:xs 4 :sm 4 :rows 4}
        [:svg {:width "100" :viewBox "0 0 10 10"}
         (board-view/board-disposition r)])
      (ui/grid-tile {:xs 1 :sm 1 :rows 1}
        [:svg {:viewBox "0 0 10 10" :transform "rotate(180)"}
         (board-view/board-disposition r)])
      (ui/grid-tile {:xs 1 :rows 1}
        [:svg {:viewBox "0 0 10 10"}
         (board-view/board-disposition r)])
      (ui/grid-tile {:xs 1 :rows 1}
        [:svg {:viewBox "0 0 10 10"}
         (board-view/board-disposition r)])
      (ui/grid-tile {:xs 1 :rows 1}
        [:svg {:viewBox "0 0 10 10"}
         (board-view/board-disposition r)]))]
   [:div {:class "set_height"}]
   ])

(defn autogridboards2
  [r]
  (ui/grid-list {}
    (ui/grid-listtile {}
      [:span {:class "container"}
       [:div {:class "relative_container"}
        [:svg {:viewBox "0 0 10 10" :xmlns "http://www.w3.org/2000/svg" :version "1.1"}
         (board-view/board-disposition r)]]])
    (ui/grid-listtile {:xs 1 :sm 1 :rows 1}
      [:svg {:viewBox "0 0 10 10" :transform "rotate(180)"}
       (board-view/board-disposition r)])
    (ui/grid-listtile {:xs 1 :rows 1}
      [:svg {:viewBox "0 0 10 10"}
       (board-view/board-disposition r)])
    (ui/grid-listtile {:xs 1 :rows 1}
      [:svg {:viewBox "0 0 10 10"}
       (board-view/board-disposition r)])
    (ui/grid-listtile {:xs 1 :rows 1}
      [:svg {:viewBox "0 0 10 10"}
       (board-view/board-disposition r)])))

(defn autogridboards3
  [r]
  (ui/grid {:spacing 1 :container true}
    (ui/grid{:item true :sm 11}
      (ui/paper
        [:span {:class "container"}
         [:div {:class "relative_container"}
          [:svg {:viewBox "0 0 10 10" :xmlns "http://www.w3.org/2000/svg" :version "1.1"}
           (board-view/board-disposition r)]]]))
    (ui/grid{:item true :sm 1}
      [:span {:class "container"}
       [:div {:class "relative_container"}
        [:svg {:viewBox "0 0 10 10" :xmlns "http://www.w3.org/2000/svg" :version "1.1"}
         (board-view/board-disposition r)]]])
    #_(ui/grid{:item true}
        [:svg {:viewBox "0 0 10 10" :transform "rotate(180)"}
         (board-view/board-disposition r)])
    #_(ui/grid{:item true}
        [:svg {:viewBox "0 0 10 10"}
         (board-view/board-disposition r)])
    #_(ui/grid{:item true}
        [:svg {:viewBox "0 0 10 10"}
         (board-view/board-disposition r)])
    #_(ui/grid{:item true}
        [:svg {:viewBox "0 0 10 10"}
         (board-view/board-disposition r)])))

(defn svg-grid
  [r]
  [:svg {:viewBox "0 0 12 12" :xmlns "http://www.w3.org/2000/svg" :version "1.1"}
   [:g
    (board-view/board-disposition r)]
   [:g {:transform "translate(9,0) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(9,2) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(9,4) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(9,6) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(9,8) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(9,10) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(0,10) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(1.8,10) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(3.6,10) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(5.4,10) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(7.2,10) scale(0.2,0.2)"}
    (board-view/board-disposition r)]])

(defn svg-grid-medium
  [r]
  [:svg {:viewBox "0 0 10 12" :xmlns "http://www.w3.org/2000/svg" :version "1.1"}
   [:g {:transform "scale(0.8,0.8)"}
    (board-view/board-disposition r)]

   [:g {:transform "translate(7.2,0) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(7.2,2) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(7.2,4) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(7.2,6) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(7.2,8) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(7.2,10) scale(0.2,0.2)"}
    (board-view/board-disposition r)]

   [:g {:transform "translate(9,0) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(9,2) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(9,4) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(9,6) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(9,8) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(9,10) scale(0.2,0.2)"}
    (board-view/board-disposition r)]

   [:g {:transform "translate(0,8) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(1.8,8) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(3.6,8) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(5.4,8) scale(0.2,0.2)"}
    (board-view/board-disposition r)]


   [:g {:transform "translate(0,10) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(1.8,10) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(3.6,10) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(5.4,10) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(7.2,10) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   ])

(defn svg-compositeXX
  [r]
  [:svg {:viewBox "0 0 10 12" :xmlns "http://www.w3.org/2000/svg" :version "1.1"}
   (for [x (range 5) y (range 5)]
     [:g {:transform (str "translate(" (* x 1.9) "," (* y 2.1) ") scale(" 0.2 ", " 0.2 ")")}
      (board-view/board-disposition r)])])

(defn svg-composite
  [r]
  (let [sx 1
        sy 1]
    [:svg {:viewBox "0 0 10 12" :xmlns "http://www.w3.org/2000/svg" :version "1.1"}
     (for [x (range 5) y (range 5)]
       [:g {:transform (str "translate(" (* x 1.9) "," (* y 2.1) ") scale(" 0.2 ", " 0.2 ")")}
        (board-view/board-disposition r)])]))

(defn svg-compositeX
  [r]
  (let [n 3
        step 0.2
        mainscale (- 1.2 (* n step))]
    (into [:svg {:viewBox "0 0 10 12" :xmlns "http://www.w3.org/2000/svg" :version "1.1"}
           [:g {:transform (str "scale(" mainscale ", " mainscale ")")}
            (board-view/board-disposition r)]]
      (for [n2 (range n) i (range (- 6 n2))]
        [:g {:transform (str "translate("n2 "," i ") scale(" mainscale ", " mainscale ")")}
         (board-view/board-disposition r)]))
    ))

(defn svg-grid-medium-plus
  [r]
  [:svg {:viewBox "0 0 10 12" :xmlns "http://www.w3.org/2000/svg" :version "1.1"}
   [:g {:transform "scale(0.6,0.6)"}
    (board-view/board-disposition r)]

   [:g {:transform "translate(5.4,0) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(5.4,2) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(5.4,4) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(5.4,6) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(5.4,8) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(5.4,10) scale(0.2,0.2)"}
    (board-view/board-disposition r)]

   [:g {:transform "translate(7.2,0) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(7.2,2) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(7.2,4) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(7.2,6) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(7.2,8) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(7.2,10) scale(0.2,0.2)"}
    (board-view/board-disposition r)]

   [:g {:transform "translate(9,0) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(9,2) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(9,4) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(9,6) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(9,8) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(9,10) scale(0.2,0.2)"}
    (board-view/board-disposition r)]


   [:g {:transform "translate(0,6) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(1.8,6) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(3.6,6) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(5.4,6) scale(0.2,0.2)"}
    (board-view/board-disposition r)]


   [:g {:transform "translate(0,8) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(1.8,8) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(3.6,8) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(5.4,8) scale(0.2,0.2)"}
    (board-view/board-disposition r)]


   [:g {:transform "translate(0,10) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(1.8,10) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(3.6,10) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(5.4,10) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(7.2,10) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   ])

(defn svg-grid-bigger
  [r]
  [:svg {:viewBox "0 0 14 14" :xmlns "http://www.w3.org/2000/svg" :version "1.1"}
   [:g {:transform "scale(0.8,0.8)"}
    (board-view/board-disposition r)]

   [:g {:transform "translate(7.2,0) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(7.2,2) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(7.2,4) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(7.2,6) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(7.2,8) scale(0.2,0.2)"}
    (board-view/board-disposition r)]


   [:g {:transform "translate(9,0) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(9,2) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(9,4) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(9,6) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(9,8) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(9,10) scale(0.2,0.2)"}
    (board-view/board-disposition r)]

   [:g {:transform "translate(10.8,0) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(10.8,2) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(10.8,4) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(10.8,6) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(10.8,8) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(10.8,10) scale(0.2,0.2)"}
    (board-view/board-disposition r)]

   [:g {:transform "translate(0,6) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(1.8,6) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(3.6,6) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(5.4,6) scale(0.2,0.2)"}
    (board-view/board-disposition r)]

   [:g {:transform "translate(0,10) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(1.8,10) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(3.6,10) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(5.4,10) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(7.2,10) scale(0.2,0.2)"}
    (board-view/board-disposition r)]

   [:g {:transform "translate(0,12) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(1.8,12) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(3.6,12) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(5.4,12) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(7.2,12) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(9.0,12) scale(0.2,0.2)"}
    (board-view/board-disposition r)]
   [:g {:transform "translate(10.8,12) scale(0.2,0.2)"}
    (board-view/board-disposition r)]])

(defn otherlayouts
  [r]
  [[:span {:class "container"}
    [:span {:class "relative_container"}
     [:svg {:xmlns "http://www.w3.org/2000/svg" :version "1.1" :viewBox "0 0 10 10"}
      (board-view/board-disposition r)]]
    [:div {:class "container"}
     [:span {:class "relative_container"}
      [:svg {:xmlns "http://www.w3.org/2000/svg" :version "1.1" :viewBox "0 0 10 10"}
       (board-view/board-disposition r)]]
     [:span {:class "relative_container"}
      [:svg {:xmlns "http://www.w3.org/2000/svg" :version "1.1" :viewBox "0 0 10 10"}
       (board-view/board-disposition r)]]]]
   [:div {:class "container"}
    [:span
     [:span {:class "relative_container"}
      [:svg {:xmlns "http://www.w3.org/2000/svg" :version "1.1" :viewBox "0 0 10 10"}
       (board-view/board-disposition r)]]
     [:span {:class "relative_container"}
      [:svg {:xmlns "http://www.w3.org/2000/svg" :version "1.1" :viewBox "0 0 10 10"}
       (board-view/board-disposition r)]]]]]
  #_[:svg {:viewBox "0 0 10 10" :transform "rotate(180)"}
     (board-view/board-disposition r)]
  #_[:svg {:viewBox "0 0 10 10"}
     (board-view/board-disposition r)]
  #_[:svg {:viewBox "0 0 10 10"}
     (board-view/board-disposition r)]
  #_[:svg {:viewBox "0 0 10 10"}
     (board-view/board-disposition r)])


(rum/defc root < rum/reactive
  [r]
  #_(board-view/board-disposition r)
  (ui/mui-theme-provider {:theme (get-mui-theme {})}
    [:div #_(nav-bar)
     [:div {:id "board-disp"}
      #_[:h1 "this is an haitch one"]
      #_(otherlayouts r)
      #_(autogridboards2 r)
      #_(autogridboards3 r)
      (svg-composite r)]])
  #_(ui/mui-theme-provider
      {:mui-theme (get-mui-theme {})}
      [:div #_(nav-bar)
       [:div {:id "board-disp"}
        (autogridboards2 r)]])
  )

