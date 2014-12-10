(ns rouje-like.t-utils
  (:use midje.sweet)
  (:require [rouje-like.core :as rj.core]
            [brute.entity :as br.e]
            [rouje-like.utils :refer :all]
            [rouje-like.components :as rj.c :refer [->3DPoint]]
            [rouje-like.world :as rj.w]))

(def level (rj.w/generate-random-level
             {:width 3 :height 3} 1 :merchant))
(def wall-e (rj.c/map->Entity {:type :wall}))
(def level+wall (update-in level [0 1 :entities]
                           conj wall-e))

(fact "taxicab-dist"
      (taxicab-dist [0 0] [1 1]) => 2)

(fact "tile->top-entity"
      (let [entities [(rj.c/map->Entity {:type :player})
                      (rj.c/map->Entity {:type :floor})]
            tile (rj.c/map->Tile {:entities entities})]
        (tile->top-entity tile)) => {:id nil,
                                     :type :player})

(fact "points->line"
      (points->line [0 0] [2 3]) => [[0 0] [0 1]
                                     [1 1]
                                     [1 2] [2 3]])

(fact "can-see?"
      (can-see? level+wall 3 [0 0] [2 1]) => true
      (can-see? level+wall 3 [0 0] [4 0]) => false
      (can-see? level+wall 3 [0 0] [1 2]) => false)

(fact "coords+offset"
      (coords+offset [0 0] [1 3]) => [1 3])

(fact "get-neighbors-coords"
      (get-neighbors-coords [1 1]) => [[1 0] [1 2] [2 1] [0 1]])

(facts "get-neighbors"
       (get-neighbors level [1 1])
       => (just [(contains {:x 1 :y 0 :z 1})
                 (contains {:x 1 :y 2 :z 1})
                 (contains {:x 2 :y 1 :z 1})
                 (contains {:x 0 :y 1 :z 1})]))

(fact "get-neighbors-of-type"
      (map :entities
           (get-neighbors-of-type level+wall [0 0] [:wall]))
      => (contains
           (contains {:id nil
                      :type :wall})))

(fact "radial-distance"
      (radial-distance [0 0] [2 2]) => 2
      (radial-distance [4 4] [5 5]) => 1)

(fact "get-entities-radially"
      (get-entities-radially level+wall [0 0]
                             #(<= % 1))
      => (just [(contains {:x 0 :y 0})
                (contains {:x 0 :y 1})
                (contains {:x 1 :y 0})
                (contains {:x 1 :y 1})]))

(fact "get-neighbors-of-type-within"
      (:entities
        (first
          (get-neighbors-of-type-within level+wall [0 0]
                                        [:wall] #(<= % 1))))
      => (contains {:type :wall
                    :id nil}))

(fact "not-any-radially-of-type"
      (not-any-radially-of-type level [0 0]
                                #(<= % 1) [:wall])
      => true)

(fact "ring-coords"
      (ring-coords [0 0] 3) => (just
                                 [-3 -3] [-3 -2] [-3 -1] [-3 0]
                                 [-3 1]  [-3 2]  [-3 3]  [-2 -3]
                                 [-2 3]  [-1 -3] [-1 3]  [0 -3]
                                 [0 3]   [1 -3]  [1 3]   [2 -3]
                                 [2 3]   [3 -3]  [3 -2]  [3 -1]
                                 [3 0]   [3 1]   [3 2]   [3 3]))

(fact "get-ring-around"
      (get-ring-around level [0 0] 2)
      => (every-checker
           #(= 16 (count %))
           #(= 5 (count
                   (filter (fn [tile]
                             (= :dune (:type (tile->top-entity tile))))
                           %)))))

(fact "rand-rng"
      (take 100 (repeatedly #(rand-rng 1 10)))
      => (has every? (roughly 5 5)))

(defn get-system []
  (with-open [w (clojure.java.io/writer "/dev/null")]
    (binding [*out* w]
      (-> (br.e/create-system)
          (rj.core/init-entities {})))))

(fact "update-in-world"
      (let [system (get-system)
            e-world (first (rj.e/all-e-with-c system :world))
            e-player (first (rj.e/all-e-with-c system :player))]
        (update-in-world system e-world [1 3 3];[z x y]
                         (fn [es]
                           [(rj.c/map->Entity
                              {:id nil
                               :type :fact})])))
      => (fn [system]
           (let [es (entities-at-pos system [1 3 3])]
             (= (:type (first es)) :fact))))

(fact "change-type"
      (let [system (get-system)
            e-player (first (rj.e/all-e-with-c system :player))]
        (change-type system e-player :player :reyalp))
      => (fn [system]
           (let [e-player (first (rj.e/all-e-with-c system :player))
                 c-position (rj.e/get-c-on-e system e-player :position)
                 this-pos (->3DPoint c-position)]
             (and (#{:reyalp} (:type c-position))
                 (seq
                   (filter #(= :reyalp (:type %))
                           (entities-at-pos system this-pos)))))))
