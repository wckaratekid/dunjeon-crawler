(ns rouje-like.skeleton
  (:require [brute.entity :as br.e]

            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.utils :as rj.u :refer [?]]
            [rouje-like.components :as rj.c :refer [can-move? move
                                                    can-attack? attack]]
            [rouje-like.mobile :as rj.m]
            [rouje-like.destructible :as rj.d]
            [rouje-like.attacker :as rj.atk]
            [rouje-like.config :as rj.cfg]))

(declare process-input-tick)

(defn add-skeleton
  ([{:keys [system z]}]
   (let [e-world (first (rj.e/all-e-with-c system :world))
         c-world (rj.e/get-c-on-e system e-world :world)
         levels (:levels c-world)
         world (nth levels z)

         get-rand-tile (fn [world]
                         (get-in world [(rand-int (count world))
                                        (rand-int (count (first world)))]))]
     (loop [target-tile (get-rand-tile world)]
       (if (#{:floor} (:type (rj.u/tile->top-entity target-tile)))
         (add-skeleton system target-tile)
         (recur (get-rand-tile world))))))
  ([system target-tile]
   (let [e-world (first (rj.e/all-e-with-c system :world))
         e-skeleton (br.e/create-entity)]
     {:system (-> system
                  (rj.u/update-in-world e-world [(:z target-tile) (:x target-tile) (:y target-tile)]
                                         (fn [entities]
                                           (vec
                                             (conj
                                               (remove #(#{:wall} (:type %)) entities)
                                               (rj.c/map->Entity {:id   e-skeleton
                                                                  :type :skeleton})))))
                  (rj.e/add-c e-skeleton (rj.c/map->Skeleton {}))
                  (rj.e/add-c e-skeleton (rj.c/map->Position {:x    (:x target-tile)
                                                              :y    (:y target-tile)
                                                              :z    (:z target-tile)
                                                              :type :skeleton}))
                  (rj.e/add-c e-skeleton (rj.c/map->Mobile {:can-move?-fn rj.m/can-move?
                                                            :move-fn      rj.m/move}))
                  (rj.e/add-c e-skeleton (rj.c/map->Sight {:distance 4}))
                  (rj.e/add-c e-skeleton (rj.c/map->Attacker {:atk              (:atk rj.cfg/skeleton-stats)
                                                              :can-attack?-fn   rj.atk/can-attack?
                                                              :attack-fn        rj.atk/attack
                                                              :is-valid-target? (fn [type]
                                                                                  (#{:player} type))}))
                  (rj.e/add-c e-skeleton (rj.c/map->Destructible {:hp             (:hp  rj.cfg/skeleton-stats)
                                                                  :defense        (:def rj.cfg/skeleton-stats)
                                                                  :can-retaliate? false
                                                                  :take-damage-fn rj.d/take-damage}))
                  (rj.e/add-c e-skeleton (rj.c/map->Killable {:experience 1}))
                  (rj.e/add-c e-skeleton (rj.c/map->Tickable {:tick-fn process-input-tick
                                                              :pri 0}))
                  (rj.e/add-c e-skeleton (rj.c/map->Broadcaster {:msg-fn (constantly "the skeleton")})))
      :z (:z target-tile)})))

(defn get-closest-tile-to
  [level this-pos target-tile]
  (let [target-pos [(:x target-tile) (:y target-tile)]
        dist-from-target (rj.u/taxicab-dist this-pos target-pos)

        _ (? target-pos)
        this-pos+dir-offset (fn [this-pos dir]
                                 (rj.u/coords+offset this-pos (rj.u/direction->offset dir)))
        shuffled-directions (shuffle [:up :down :left :right])
        offset-shuffled-directions (map #(this-pos+dir-offset this-pos %)
                                        shuffled-directions)

        is-valid-target-tile? #{:floor :torch :gold :player}

        nth->offset-pos (fn [index]
                            (nth offset-shuffled-directions index))
        isa-closer-tile? (fn [target-pos+offset]
                           (and (< (rj.u/taxicab-dist target-pos+offset target-pos)
                                   dist-from-target)
                                (is-valid-target-tile?
                                  (:type (rj.u/tile->top-entity (get-in level target-pos+offset))))))]
    (cond
      (isa-closer-tile? (nth->offset-pos 0))
      (get-in level (nth->offset-pos 0))

      (isa-closer-tile? (nth->offset-pos 1))
      (get-in level (nth->offset-pos 1))

      (isa-closer-tile? (nth->offset-pos 2))
      (get-in level (nth->offset-pos 2))

      (isa-closer-tile? (nth->offset-pos 3))
      (get-in level (nth->offset-pos 3))

      :else nil)))

(defn process-input-tick
  [_ e-this system]
  (let [c-position (rj.e/get-c-on-e system e-this :position)
        this-pos [(:x c-position) (:y c-position)]
        c-mobile (rj.e/get-c-on-e system e-this :mobile)

        e-player (first (rj.e/all-e-with-c system :player))
        c-player-pos (rj.e/get-c-on-e system e-player :position)
        player-pos [(:x c-player-pos) (:y c-player-pos)]

        e-world (first (rj.e/all-e-with-c system :world))
        c-world (rj.e/get-c-on-e system e-world :world)
        levels (:levels c-world)
        level (nth levels (:z c-position))

        neighbor-tiles (rj.u/get-neighbors level [(:x c-position) (:y c-position)])

        c-sight (rj.e/get-c-on-e system e-this :sight)
        is-player-within-range? (seq (rj.u/get-neighbors-of-type-within level this-pos [:player]
                                                                        #(<= %  (:distance c-sight))))
        
        c-attacker (rj.e/get-c-on-e system e-this :attacker)

        target-tile (if (and (rj.u/can-see? level (:distance c-sight) this-pos player-pos)
                              is-player-within-range?)
                      (get-closest-tile-to level this-pos (first is-player-within-range?))
                      (if (seq neighbor-tiles)
                        (rand-nth (conj neighbor-tiles nil))
                        nil))
        e-target (:id (rj.u/tile->top-entity target-tile))]
    (if (not (nil? target-tile))
      (cond
        (and (< (rand-int 100) 80)
             (can-move? c-mobile e-this target-tile system))
        (move c-mobile e-this target-tile system)

        (can-attack? c-attacker e-this e-target system)
        (attack c-attacker e-this e-target system)

        :else system)
      system)))

