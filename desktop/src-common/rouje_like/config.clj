(ns rouje-like.config
  (:require [clojure.set :refer [union]]))

;; WORLD CONFIG
(def block-size 36)
(def padding-sizes {:top   3
                    :btm   2
                    :left  1
                    :right 1})
(def view-port-sizes [20 20])
(def world-sizes {:width  20
                  :height 20})

;; TILE TYPES
(def <floors>
  #{:dune :floor :forest-floor})

(def <walls>
  #{:wall :tree :maze-wall})

(def <items>
  #{:torch :gold :health-potion :equipment :purchasable})

(def <empty>
  (union <floors> <items>))

(def <sight-blockers>
  (union <walls> #{:lichen}))

(def <valid-move-targets>
  (union <empty> #{:portal :m-portal}))

(def <valid-mob-targets>
  (union <empty> #{:player}))

(def wall->stats
  {:wall      {:hp 2}
   :tree      {:hp 1}
   :maze-wall {:hp 100}})

;; PLAYER CONFIG
(def player-stats
  {:max-hp 100
   :atk 4
   :def 1})

(def class->stats {:rogue   {}
                   :warrior {}
                   :mage    {}})

(def race->stats {:human {:max-hp 10  :atk 1}
                  :orc   {:max-hp 20  :atk 2}
                  :elf   {:max-hp -5  :atk 0}})

(def stat->comp {:max-hp :destructible
                 :atk :attacker
                 :def :destructible})

(def stat->pointinc {:max-hp 5
                     :atk 1
                     :def 1})

(def level-exp
  {:exp 1})

(def player-init-pos
  (let [x (/ (:width  world-sizes) 2)
        y (/ (:height world-sizes) 2)]
    [1 x y]))

(def player-sight
  {:distance 5.0
   :decline-rate (/ 1 4)
   :lower-bound 4         ;; Inclusive
   :upper-bound 11        ;; Exclusive
   :torch-multiplier 1.})

;; EQUIPMENT CONFIG
(def weapons
  (->> (flatten
         [(repeat 1 [:sword  {:atk 3}])
          (repeat 1 [:mace   {:atk 2}])
          (repeat 1 [:axe    {:atk 3}])
          (repeat 1 [:flail  {:atk 2}])
          (repeat 1 [:dagger {:atk 1}])])
       (partition 2)))

(def weapon-qualities
  (->> (flatten
         [(repeat 1 [:quick  {:atk  1}])
          (repeat 1 [:giant  {:atk  2}])
          (repeat 1 [:great  {:atk  2}])
          (repeat 1 [:tiny   {:atk  1}])
          (repeat 1 [:dull   {:atk -1}])
          (repeat 1 [:dented {:atk -2}])])
       (partition 2)))

(def weapon-effects
  (->> (flatten
         [(repeat 1 [:bloodletting {:atk 1}])
          (repeat 1 [:pain {:atk 1}])
          (repeat 1 [:poison])
          (repeat 1 [:paralysis])
          (repeat 1 [:power {:atk 2}])
          (repeat 1 [:death {:atk 2}])
          (repeat 1 [:fire])
          (repeat 1 [nil])])
       (partition 2)))

(def armors
  (->> (flatten
         [(repeat 1 [:chestplate {:max-hp 1 :def 1}])
          (repeat 1 [:chainmail  {:max-hp 3}])
          (repeat 1 [:tunic      {:max-hp 1}])])
       (partition 2)))

(def status-effects
  {:paralysis {:type     :paralysis
               :duration 2
               :value    1}
   :poison    {:type     :poison
               :duration 2
               :value    2}
   :fire      {:type     :fire
               :duration 2
               :value    2}})

;; CREATURE CONFIG
(def bat-stats
  {:hp  2
   :def 0})

(def lichen-stats
  {:hp  4
   :atk 0
   :def 1})

(def skeleton-stats
  {:hp  8
   :def 1
   :atk 3
   :exp 1})

(def potion-stats
  {:health 5})

;; WORLD CONFIG
(def init-wall% 45)
(def init-torch% 2)
(def init-gold% 5)
(def init-health-potion% 2)
(def init-lichen% 1)
(def init-bat% 1)
(def init-skeleton% 1)
(def init-equip% 1)

;; MERCHANT CONFIG
(def merchant-pos
  {:x 5
   :y 7})

(def merchant-portal-pos
  {:x 5
   :y 9})

(def merchant-player-pos
  {:x 5
   :y 3})

(def merchant-level-size
  {:width 10
   :height 10})

(def merchant-item-pos
  [{:x 3 :y 5}
   {:x 5 :y 5}
   {:x 7 :y 5}])

(def inspectables
  [:purchasable :merchant])
