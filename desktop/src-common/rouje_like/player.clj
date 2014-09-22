(ns rouje-like.player
  (:import [com.badlogic.gdx.graphics.g2d SpriteBatch BitmapFont]
           [com.badlogic.gdx.scenes.scene2d.ui Label Skin])
  (:require [play-clj.core :refer :all]
            [play-clj.ui :refer :all]

            [rouje-like.components :as rj.c]
            [rouje-like.entity :as rj.e]))

(defn can-attack?
  [_ _ target]
  (#{:lichen} (-> target
                  (:entities)
                  (rj.c/sort-by-pri)
                  (first)
                  (:type))))

(defn attack!
  [system this target]
  (let [damage (:attack (rj.e/get-c-on-e system this :attacker))

        e-enemy (:id (first (filter #(#{:lichen} (:type %))
                                    (:entities target))))

        take-damage! (:take-damage! (rj.e/get-c-on-e system e-enemy :destructible))

        _ (do (println "attack!: " (brute.entity/get-all-components-on-entity system e-enemy)))]
    (take-damage! system e-enemy damage)))

(defn can-dig?
  [_ _ target]
  (#{:wall} (-> target
                (:entities)
                (rj.c/sort-by-pri)
                (first)
                (:type))))

(defn dig!
  [system this target]
  (let [e-world (first (rj.e/all-e-with-c system :world))

        wall->floor (fn [world ks]
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
    (-> system
        (rj.e/upd-c e-world :world
                    (fn [c-world]
                      (update-in c-world [:world]
                                 wall->floor [(:x target) (:y target)])))
        (rj.e/upd-c this :moves-left
                    (fn [c-moves-left]
                      (update-in c-moves-left [:moves-left]
                                 dec))))))

(defn can-move?
  [_ _ target]
  (#{:floor :gold :torch} (-> target
                              (:entities)
                              (rj.c/sort-by-pri)
                              (first)
                              (:type))))

(defn move!
  [system this target]
  (let [c-sight (rj.e/get-c-on-e system this :sight)
        sight-decline-rate (:decline-rate c-sight)
        sight-lower-bound (:lower-bound c-sight)
        sight-upper-bound (:upper-bound c-sight)
        dec-sight (fn [prev] (if (> prev (inc sight-lower-bound))
                               (- prev sight-decline-rate)
                               prev))
        inc-sight (fn [prev] (if (<= prev (- sight-upper-bound 2))
                               (+ prev 2)
                               prev))

        c-position (rj.e/get-c-on-e system this :position)

        e-world (first (rj.e/all-e-with-c system :world))]
    (-> system
        (rj.e/upd-c this :moves-left
                    (fn [c-moves-left]
                      (update-in c-moves-left [:moves-left] dec)))

        (as-> system (case (-> target (:entities)
                               (rj.c/sort-by-pri)
                               (first) (:type))
                       :gold  (-> system
                                  (rj.e/upd-c this :gold
                                              (fn [c-gold]
                                                (update-in c-gold [:gold] inc)))
                                  (rj.e/upd-c this :sight
                                              (fn [c-sight]
                                                (update-in c-sight [:distance] dec-sight))))
                       :torch (-> system
                                  (rj.e/upd-c this :sight
                                              (fn [c-sight]
                                                (update-in c-sight [:distance] inc-sight))))
                       :floor (-> system
                                  (rj.e/upd-c this :sight
                                              (fn [c-sight]
                                                (update-in c-sight [:distance] dec-sight))))
                       system))

        (rj.e/upd-c e-world :world
                    (fn [c-world]
                      (update-in c-world [:world]
                                 (fn [world]
                                   (let [player-pos [(:x c-position) (:y c-position)]
                                         target-pos [(:x target) (:y target)]]
                                     (-> world
                                         (update-in target-pos
                                                    (fn [tile]
                                                      (update-in tile [:entities]
                                                                 (fn [entities]
                                                                   (vec (conj
                                                                          (remove #(#{:gold :torch} (:type %))
                                                                                  entities)
                                                                          (rj.c/map->Entity {:type :player
                                                                                             :id   this})))))))
                                         (update-in player-pos
                                                    (fn [tile]
                                                      (update-in tile [:entities]
                                                                 (fn [entities]
                                                                   (vec (remove #(#{:player} (:type %))
                                                                                entities))))))))))))

        (rj.e/upd-c this :position
                    (fn [c-position]
                      (-> c-position
                          (assoc-in [:x] (:x target))
                          (assoc-in [:y] (:y target))))))))

(defn process-input-tick!
  [system direction]
  (let [this (first (rj.e/all-e-with-c system :player))
        c-moves-left (rj.e/get-c-on-e system this :moves-left)
        moves-left (:moves-left c-moves-left)

        c-position (rj.e/get-c-on-e system this :position)
        x-pos (:x c-position)
        y-pos (:y c-position)

        e-world (first (rj.e/all-e-with-c system :world))
        c-world (rj.e/get-c-on-e system e-world :world)
        world (:world c-world)
        target-coords (case direction
                        :up    [     x-pos (inc y-pos)]
                        :down  [     x-pos (dec y-pos)]
                        :left  [(dec x-pos)     y-pos]
                        :right [(inc x-pos)     y-pos])
        target (get-in world target-coords nil)]
    (if (and (pos? moves-left)
             (not (nil? target)))
      (let [c-mobile (rj.e/get-c-on-e system this :mobile)
            c-digger (rj.e/get-c-on-e system this :digger)
            c-attacker (rj.e/get-c-on-e system this :attacker)]
        (if (not (nil? target))
          (cond
            ((:can-move? c-mobile) system this target)
            ((:move! c-mobile) system this target)

            ((:can-dig? c-digger) system this target)
            ((:dig! c-digger) system this target)

            ((:can-attack? c-attacker) system this target)
            ((:attack! c-attacker) system this target))
          system))
      system)))

;;RENDERING FUNCTIONS
(defn render-player-stats
  [system this {:keys [view-port-sizes]}]
  (let [[_ vheight] view-port-sizes

        c-gold (rj.e/get-c-on-e system this :gold)
        gold (:gold c-gold)

        c-moves-left (rj.e/get-c-on-e system this :moves-left)
        moves-left (:moves-left c-moves-left)

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
