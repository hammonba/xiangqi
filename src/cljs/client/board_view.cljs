(ns client.board-view
  (:require [citrus.core :as citrus]
            [client.router :as router :refer [board]]
            [rum.core :as rum]
            [xiangqi.board-layout :as board-layout]))

(defn complete-move
  [reconciler svg opened-move {:move/keys [next-board]}]
  (update svg 1 assoc
    :class
    (board-layout/opened-move-classes (:player @opened-move))

    :onClick
    (fn []
      (reset! opened-move nil)
      (citrus/dispatch! reconciler :effects :navigate [router/board {:board-ident next-board}]))))

(defn did-piece-open-move?
  [opened-move piece]
  (when-let [{:keys [opened-x opened-y]} @opened-move]
    (and
      (= (:x piece) opened-x)
      (= (:y piece) opened-y))))

(defn open-move
  [opened-move svg piece]
  (if (empty? (:piece/moves piece))
    svg
    (update svg 1 assoc
      :onClick
      (fn [] (swap! opened-move #(when-not (= piece %) piece))))))

(defn place-pieces
  [reconciler disposition opened-move]
  (board-layout/layout-pieces
    {:openmove-fn  #(open-move opened-move %1 %2)
     :completemove-fn #(complete-move reconciler %1 %2 %3)
     :did-piece-open-move?-fn #(did-piece-open-move? opened-move %1)}
    disposition
    opened-move
    (:piece/moves @opened-move)))

(rum/defcs board-disposition < rum/reactive (rum/local {} ::opened-move) rum/static
  [{opened-move ::opened-move :as state} reconciler & args]
  (let [{:keys [disposition] :as board-state}
        (rum/react (citrus/subscription reconciler [:board-controller]))]
    #_[:svg {:xmlns "http://www.w3.org/2000/svg" :version "1.1"
           :viewBox "0 0 10 10"}]
    (board-layout/board-hiccup
      board-state
      (place-pieces reconciler disposition opened-move))))