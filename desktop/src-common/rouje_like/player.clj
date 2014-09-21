(ns rouje-like.player
  (:import [com.badlogic.gdx.graphics.g2d SpriteBatch BitmapFont]
           [com.badlogic.gdx.scenes.scene2d.ui Label Skin])
  (:require [play-clj.core :refer :all]
            [play-clj.ui :refer :all]

            [rouje-like.components :as rj.c]
            [rouje-like.entity :as rj.e]))

(defn can-attack?
  [system this target]
  (let [c-moves-left (rj.e/get-c-on-e system this :moves-left)
        moves-left @(:moves-left c-moves-left)]
    (and (pos? moves-left)
         ;; TODO: Refactor to check for Destructible component
         (some #(= (:type %) :lichen) (:entities target)))))

(defn attack!
  [system this target]
  (let [damage @(:attack (rj.e/get-c-on-e system this :attacker))

        e-enemy (:id (first (filter #(#{:lichen} (:type %))
                                    (:entities target))))
        take-damage! (:take-damage! (rj.e/get-c-on-e system e-enemy :destructible))]
    (take-damage! system e-enemy damage)))

(defn can-dig?
  [system this target]
  (let [c-moves-left (rj.e/get-c-on-e system this :moves-left)
        moves-left @(:moves-left c-moves-left)]
    (and (pos? moves-left)
         ;; TODO: Refactor to filter for only the :wall
         (#{:wall} (-> target (:entities) (first) (:type))))))

(defn dig!
  [system this target]
  (let [c-position (rj.e/get-c-on-e system this :position)
        world (:world c-position)#_(ATOM)

        c-moves-left (rj.e/get-c-on-e system this :moves-left)
        moves-left (:moves-left c-moves-left)#_(ATOM)

        wall->floor-at (fn [world ks]
                      (update-in world ks
                                 (fn [tile]
                                   (update-in tile [:entities]
                                              (fn [entities]
                                                (map (fn [entity]
                                                       (if (#{:wall}
                                                            (:type entity))
                                                         (rj.c/map->Entity {:type :floor})
                                                         entity))
                                                     entities))))))]
    (do
      (swap! world wall->floor-at [(:x target) (:y target)])
      (swap! moves-left dec))))

(defn can-move?
  [system this target]
  (let [c-moves-left (rj.e/get-c-on-e system this :moves-left)
        moves-left @(:moves-left c-moves-left)]
    (and (pos? moves-left)
         ;; TODO: Refactor to filter for only the :floor/:gold/:torch
         (#{:floor :gold :torch} (-> target (:entities) (first) (:type))))))

(defn move!
  [system this target]
  (let [c-gold (rj.e/get-c-on-e system this :gold)
        gold (:gold c-gold)#_(ATOM)

        c-moves-left (rj.e/get-c-on-e system this :moves-left)
        moves-left (:moves-left c-moves-left)#_(ATOM)

        c-sight (rj.e/get-c-on-e system this :sight)
        sight-distance (:distance c-sight)#_(ATOM)
        sight-decline-rate @(:decline-rate c-sight)
        sight-lower-bound @(:lower-bound c-sight)
        sight-upper-bound @(:upper-bound c-sight)
        dec-sight (fn [prev] (if (> prev (inc sight-lower-bound))
                               (- prev sight-decline-rate)
                               prev))
        inc-sight (fn [prev] (if (<= prev (- sight-upper-bound 2))
                               (+ prev 2)
                               prev))

        c-position (rj.e/get-c-on-e system this :position)
        world (:world c-position)#_(ATOM)
        x-pos (:x c-position)#_(ATOM)
        y-pos (:y c-position)#_(ATOM)

        player<->tile-at (fn [world player-loc target-loc]
                         (-> world
                             (update-in player-loc
                                        (fn [tile]
                                          (update-in tile [:entities]
                                                     (fn [entities]
                                                       (map (fn [entity]
                                                              (if (#{:player}
                                                                   (:type entity))
                                                                (rj.c/map->Entity {:type :floor})
                                                                entity))
                                                            entities)))))
                             (update-in target-loc
                                        (fn [tile]
                                          (update-in tile [:entities]
                                                     (fn [entities]
                                                       (map (fn [entity]
                                                              (if (#{:floor :gold :torch}
                                                                   (:type entity))
                                                                (rj.c/map->Entity {:type :player})
                                                                entity))
                                                            entities)))))))]
    (swap! moves-left dec)
    (case (:type (first (:entities target)))
      :gold  (do
               (swap! gold inc)
               (swap! sight-distance dec-sight))
      :torch (do
               (swap! sight-distance inc-sight))
      :floor (do
               (swap! sight-distance dec-sight))
      nil)
    (swap! world player<->tile-at [@x-pos @y-pos] [(:x target) (:y target)])
    (reset! x-pos (:x target))
    (reset! y-pos (:y target))))

(defn process-input-tick!
  [system direction]
  (let [this (first (rj.e/all-e-with-c system :player))

        c-position (rj.e/get-c-on-e system this :position)
        world @(:world c-position)
        x-pos @(:x c-position)
        y-pos @(:y c-position)

        c-mobile (rj.e/get-c-on-e system this :mobile)
        c-attacker (rj.e/get-c-on-e system this :attacker)

        target-coords (case direction
                        :up    [     x-pos (inc y-pos)]
                        :down  [     x-pos (dec y-pos)]
                        :left  [(dec x-pos)     y-pos]
                        :right [(inc x-pos)     y-pos])
        target (get-in world target-coords nil)

        c-digger (rj.e/get-c-on-e system this :digger)]
    (if (not (nil? target))
      (cond
        ((:can-move? c-mobile) system this target)
        ((:move! c-mobile) system this target)

        ((:can-dig? c-digger) system this target)
        ((:dig! c-digger) system this target)

        ((:can-attack? c-attacker) system this target)
        ((:attack! c-attacker) system this target)))))

;;RENDERING FUNCTIONS
(defn render-player-stats
  [system this {:keys [view-port-sizes]}]
  (let [[_ vheight] view-port-sizes

        c-gold (rj.e/get-c-on-e system this :gold)
        gold @(:gold c-gold)

        c-moves-left (rj.e/get-c-on-e system this :moves-left)
        moves-left @(:moves-left c-moves-left)

        renderer (new SpriteBatch)]
    (.begin renderer)
    (label! (label (str "Gold: [" gold "]" " - " "MovesLeft: [" moves-left "]")
                   (color :white)
                   :set-y (float (* (+ vheight 2) rj.c/block-size)))
            :draw renderer 1.0)
    (.end renderer)))

(defn render-player
  [system this args]
  (render-player-stats system this args))
