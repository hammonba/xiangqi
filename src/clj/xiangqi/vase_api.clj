(ns xiangqi.vase-api
  "load distinct vase api, make avaible to component.endpoint via :apis key"
  (:require [com.cognitect.vase.fern :as vase.fern]
            [integrant.core :as ig]
            [fern]
            [fern.easy]
            [clojure.tools.logging :as clog]
            [io.pedestal.interceptor :as interceptor]
            [medley.core :as medley]
            [clojure.tools.logging :as log])
  (:import [clojure.lang MultiFn]))

(defmethod fern/literal 'fern/load-env
  [_ {:keys [path enclosing-env exclude-keys plugin-sym] :as argz}]
  (merge (fern.easy/load-environment path (or plugin-sym 'vase/plugins))
    (apply dissoc enclosing-env (or exclude-keys ['integrant/system]))
    (vase.fern/stock-interceptors)))

;; t comes through as a (var ...) sexp
;; which we need to evaluate before interception
(defmethod fern/literal 'vase/reval-interceptor
  [_ t]
  (interceptor/interceptor (eval t)))

(defmethod fern/literal 'vase/subkey
  [_ m k]
  (get m k))

(def igr (atom nil))

(defn build-initkey-map
  "match every key in the composite with its defmethod"
  [kk]
  (into {}
    (keep
      (fn [k]
          (when-let [f (.getMethod ^MultiFn ig/init-key k)]
            [k f]))
      kk)))

(defn initkey-composites
  "apply initkey function to every element of composite key excluding kignore.
   k is expected to have its own submap in the config
   return as map"
  [kk config & kignore]
  (when (vector? kk)
    (medley/map-kv-vals
      (fn [k f] (f k (get config k)))
      (apply dissoc (build-initkey-map kk) kignore))))

(defmethod ig/init-key :component/vase-api
  [kk {:keys [specs api-syms integrant-refs] :as config}]
  (let [
        ;subkeys (initkey-composites kk config :component/vase-api)
        ;subsymbols (medley/map-keys symbol subkeys)
        env (into specs integrant-refs)]
    #_(reset! igr {:subkeys subkeys
                 :subsymbols subsymbols
                 :env env})
    (assoc config
      :apis (mapv #(fern/evaluate env %) api-syms))))

(defmethod ig/halt-key! :component/vase-api
  [_ this]
  (dissoc this :routes))
