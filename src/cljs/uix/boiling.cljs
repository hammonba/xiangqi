(ns uix.boiling
  "https://reactjs.org/docs/lifting-state-up.html"
  (:require [uix.core.alpha :as uix :refer [defui]]
            [xframe.core.alpha :as xf :refer [<sub]]
            ["@material-ui/core/Switch" :as switch]
            ))

(def scaleNames {:c "Celsius"
                 :f "Fahrenheit"})

(defn toCelsius
  [fahrenheit]
  (-> fahrenheit
    (- 32)
    (* 5)
    (/ 9)))

(defn toFahrenheit
  [celsius]
  (-> celsius
    (* 9)
    (/ 5)
    (+ 32)))

(xf/reg-sub :boiling/state
  (fn []
    (:boiling (xf/<- [::xf/db]))))

(xf/reg-sub :boiling/celsius
  (fn []
    (:celsius (xf/<- [:boiling/state]))))

(xf/reg-sub :boiling/fahrenheit
  (fn []
    (:fahrenheit (xf/<- [:boiling/state]))))

(xf/reg-sub :boiling/verdict
  (fn []
    (if (>= (xf/<- [:boiling/celsius]) 100)
      [:p "The water would boil"]
      [:p "The water would not boil"])))

(xf/reg-event-db ::db-init
  (fn [db _]
    (assoc db :boiling {:celsius "" :fahrenheit ""})))

(xf/reg-event-db :set-celsius
  (fn [db [_ value]]
    (let [c (js/parseFloat value)
          f (if-not (js/Number.isNaN c) (toFahrenheit c) "")]
      (-> db
        (assoc-in [:boiling :fahrenheit] f)
        (assoc-in [:boiling :celsius] c)))))

(xf/reg-event-db :set-fahrenheit
  (fn [db [_ value]]
    (let [f (js/parseFloat value)
          c (when-not (js/Number.isNaN f) (toCelsius f))]
      (-> db
        (assoc-in [:boiling :fahrenheit] f)
        (assoc-in [:boiling :celsius] c)))))

(defui temperatureInput
  [{:keys [onTemperatureChange temperature scale] :as props}]
  [:fieldset
   [:legend (str "Enter temperature in " (scaleNames scale) ": ")]
   [:input {:value (<sub [temperature])
            :onChange onTemperatureChange}]])

(defn calculator
  []
  [:div
   [^js/React.Component temperatureInput {:scale :f
                                          :temperature :boiling/fahrenheit
                                          :onTemperatureChange (fn [evt]
                                                                 (xf/dispatch [:set-fahrenheit (.. evt -target -value)]))}]
   [^js/React.Component temperatureInput {:scale :c
                                          :temperature :boiling/celsius
                                          :onTemperatureChange (fn [evt]
                                                                 (xf/dispatch [:set-celsius (.. evt -target -value)]))}]
   (<sub [:boiling/verdict])
   ])

(defonce init-db
  (xf/dispatch [::db-init]))
