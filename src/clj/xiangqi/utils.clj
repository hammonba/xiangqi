(ns xiangqi.utils
  (:require [medley.core :as medley]))

(defn strip-keyword-ns
  [v]
  (if (keyword? v)
    (keyword (name v))
    v))

(defn strip-all-namespaces
  [mz]
  (into {}
    (map (fn [[k v]] [(strip-keyword-ns k) (strip-keyword-ns v)]))
    mz))

(def conj-vec (fnil conj []))
(def into-set (fnil into #{}))

(defn zip-by
  ([keyfn coll]
   (zip-by keyfn identity coll))
  ([keyfn valfn coll]
   (zipmap (map keyfn coll) (map valfn coll))))

(defn conj-some
  [coll & args]
  (apply conj coll (filter some? args)))

(defn nilsafe
  [f]
  (fn [v] (when v (f v))))

(def nname (nilsafe name))

(defn threading-assocsome
  "shim to conveniently put assoc-some into a threading macro"
  [m k f & args]
  (medley/assoc-some m k (apply f m args)))

(defn select-and-rename-keys
  "equivalent to (comp set/rename-keys select-keys)"
  [m kmap]
  (into {}
    (keep (fn [me]
              (when-let [k2 (get kmap (key me))]
                [k2 (val me)])))
    m))


(defn update-some
  "variant of update that only alters map when
  m contains non-nil key"
  ([m k f & args]
   (if-some [v (get m k)]
     (assoc m k (apply f v args))
     m)))
