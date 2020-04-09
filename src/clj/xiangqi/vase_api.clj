(ns xiangqi.vase-api
  "load distinct vase api, make avaible to component.endpoint via :apis key"
  (:require [com.cognitect.vase.fern :as vase.fern]
            [integrant.core :as ig]
            [fern]
            [fern.easy]
            [clojure.tools.logging :as clog]))

(defmethod fern/literal 'fern/load-env
  [_ {:keys [path enclosing-env exclude-keys plugin-sym] :as argz}]
  (merge (fern.easy/load-environment path plugin-sym)
    (apply dissoc enclosing-env (or exclude-keys ['integrant/system]))
    (vase.fern/stock-interceptors)))

(defmethod ig/init-key :component/vase-api
  [_ {:keys [specs api-syms] :as config}]
  (let [env specs]
    (assoc config :apis (mapv #(fern/evaluate env %) api-syms))))

(defmethod ig/halt-key! :component/vase-api
  [_ this]
  (dissoc this :routes))
