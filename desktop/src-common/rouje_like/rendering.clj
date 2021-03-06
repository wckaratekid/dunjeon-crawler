(ns rouje-like.rendering
  (:import [com.badlogic.gdx.graphics.g2d TextureRegion]
           [com.badlogic.gdx.graphics Texture Pixmap Color]
           [com.badlogic.gdx.graphics.g2d SpriteBatch TextureRegion]
           [clojure.lang Keyword Atom]
           [com.badlogic.gdx.graphics Texture Pixmap Color]
           [com.badlogic.gdx.files FileHandle])

  (:require [play-clj.core :refer :all]
            [play-clj.ui :refer :all]
            [play-clj.g2d :refer [texture texture!]]

            [clojure.math.numeric-tower :as math]

            [rouje-like.components
             :refer [render ->2DPoint ->3DPoint]]

            [rouje-like.utils :as rj.u :refer [?]]
            [rouje-like.equipment :as rj.eq]
            [rouje-like.config :as rj.cfg]
            [rouje-like.entity-wrapper     :as rj.e]))

#_(in-ns 'rouje-like.rendering)
#_(use 'rouje-like.rendering :reload)

(defn process-one-game-tick
  [system delta-time]
  (let [renderable-entities (rj.e/all-e-with-c system :renderable)]
    (doseq [e-renderable renderable-entities]
      (let [c-renderable (rj.e/get-c-on-e system e-renderable :renderable)
            args (assoc (:args c-renderable)
                        :delta-time delta-time)]
        (render c-renderable e-renderable args system))))
  system)

(defn render-messages
  [_ e-this _ system]
  (let [c-relay (rj.e/get-c-on-e system e-this :relay)
        {backgrounds :background
         immediates :immediate} c-relay

        e-counter (first (rj.e/all-e-with-c system :counter))
        {current-turn :turn} (rj.e/get-c-on-e system e-counter :counter)

        msgs (concat
               (filter #(= (:turn %)
                           (dec current-turn))
                       backgrounds)
               immediates)
        msgs (mapcat #(str (:message %) ". \n")
                     msgs)

        renderer (new SpriteBatch)]
    (.begin renderer)
    (label! (label (apply str (into [] msgs))
                   (color :green)
                   :set-y (float 0))
            :draw renderer 1.0)
    (.end renderer)))

(defn render-player-stats
  [_ e-this {:keys [view-port-sizes]} system]
  (let [[_ vheight] view-port-sizes

        c-player (rj.e/get-c-on-e system e-this :player)
        player-name (:name c-player)

        c-race (rj.e/get-c-on-e system e-this :race)
        race (:race c-race)

        c-class (rj.e/get-c-on-e system e-this :class)
        class (:class c-class)

        c-wallet (rj.e/get-c-on-e system e-this :wallet)
        gold (:gold c-wallet)

        c-experience (rj.e/get-c-on-e system e-this :experience)
        experience (:experience c-experience)
        level (:level c-experience)

        c-position (rj.e/get-c-on-e system e-this :position)
        x (:x c-position)
        y (:y c-position)
        z (:z c-position)

        {:keys [hp max-hp def status-effects]} (rj.e/get-c-on-e system e-this :destructible)
        status-effects (into []
                             (map (fn [{:keys [type value duration]}]
                                    {:type type
                                     :value value
                                     :duration duration})
                                  status-effects))

        c-inv (rj.e/get-c-on-e system e-this :inventory)
        junk (->> (:junk c-inv)
                  (map rj.eq/equipment-value)
                  (reduce + 0))
        slot (rj.eq/equipment-name (or (:weapon (:slot c-inv))
                                       (:armor (:slot c-inv))))
        hp-potions (:hp-potion c-inv)
        mp-potions (:mp-potion c-inv)

        c-equip (rj.e/get-c-on-e system e-this :equipment)
        armor (rj.eq/equipment-name (:armor c-equip))
        weapon (rj.eq/equipment-name (:weapon c-equip))

        c-attacker (rj.e/get-c-on-e system e-this :attacker)
        attack (:atk c-attacker)

        c-energy (rj.e/get-c-on-e system e-this :energy)
        energy (:energy c-energy)

        c-magic (rj.e/get-c-on-e system e-this :magic)
        mp (:mp c-magic)
        max-mp (:max-mp c-magic)

        renderer (new SpriteBatch)]
    (.begin renderer)
    (label! (label (str "{"
                        "Name: " player-name ", "
                        "Gold: " gold ", "
                        "Position: [" x "," y "," z "], "
                        "HP: " hp  "/" max-hp ", "
                        "MP: " mp "/" max-mp
                        "}\n{"
                        "Attack: " attack ", "
                        "Defense: " def ", "
                        "Race: " race ", "
                        "Class: " class
                        "}\n{"
                        "Experience: " experience ", "
                        "Level: " level ", "
                        "cli: " @rj.u/cmdl-buffer
                        "}\n{"
                        "Status: " status-effects
                        "}\n{"
                        "Energy: " energy ", "
                        "Junk: " junk ", "
                        "Slot: [" slot "], "
                        "Armor: [" armor "], "
                        "Weapon: [" weapon "], "
                        "HP-Potions: " hp-potions ", "
                        "MP-Potions: " mp-potions
                        "}")

                   (color :green)
                   :set-y (float (* (+ vheight
                                       (- (+ (:top rj.cfg/padding-sizes)
                                             (:btm rj.cfg/padding-sizes))
                                          3))
                                    (rj.cfg/block-size))))
            :draw renderer 1.0)
    (.end renderer)))

(defn render-player
  [_ e-this args system]
  (render-player-stats _ e-this args system))

(def ^:private type->tile-info
  (let [grim-tile-sheet "grim_12x12.png"
        darkond-tile-sheet "DarkondDigsDeeper_16x16.png"
        bisasam-tile-sheet "Bisasam_20x20.png"]
    {:arrow-trap                {:x 3 :y 2
                                 :width 12 :height 12
                                 :color {:r 218 :g 165 :b 32 :a 255}
                                 :tile-sheet grim-tile-sheet}
     :bat                       {:x 14 :y 5
                                 :width 12 :height 12
                                 :color {:r 255 :g 255 :b 255 :a 128}
                                 :tile-sheet grim-tile-sheet}
     :colossal-amoeba           {:x 7 :y 12
                                 :width 16 :height 16
                                 :color {:r 111 :g 246 :b 255 :a 125}
                                 :tile-sheet darkond-tile-sheet}
     :door                      {:x 11 :y 2
                                 :width 12 :height 12
                                 :color {:r 255 :g 255 :b 255 :a 255}
                                 :tile-sheet grim-tile-sheet}
     :drake                     {:x 13 :y 0
                                 :width 16 :height 16
                                 :color {:r 222 :g 5 :b 48 :a 255}
                                 :tile-sheet darkond-tile-sheet}
     :dune                      {:x 14 :y 2
                                 :width 12 :height 12
                                 :color {:r 255 :g 140 :b 0 :a 255}
                                 :tile-sheet grim-tile-sheet}
     :equipment                 {:x 2 :y 9
                                 :width 12 :height 12
                                 :color {:r 255 :g 255 :b 255 :a 255}
                                 :tile-sheet grim-tile-sheet}
     :floor                     {:x 14 :y 2
                                 :width 12 :height 12
                                 :color {:r 255 :g 255 :b 255 :a 64}
                                 :tile-sheet grim-tile-sheet}
     :forest-floor              {:x 14 :y 2
                                 :width 12 :height 12
                                 :color {:r 103 :g 133 :b 81 :a 64}
                                 :tile-sheet grim-tile-sheet}
     :giant-amoeba              {:x 7 :y 12
                                 :width 16 :height 16
                                 :color {:r 111 :g 246 :b 255 :a 125}
                                 :tile-sheet darkond-tile-sheet}
     :gold                      {:x 1 :y 9
                                 :width 12 :height 12
                                 :color {:r 255 :g 255 :b 0 :a 255}
                                 :tile-sheet grim-tile-sheet}
     :health-potion             {:x 13 :y 10
                                 :width 12 :height 12
                                 :color {:r 255 :g 0 :b 0 :a 255}
                                 :tile-sheet grim-tile-sheet}
     :mimic                     {:x 1 :y 9
                                 :width 12 :height 12
                                 :color {:r 128 :g 255 :b 1 :a 255}
                                 :tile-sheet grim-tile-sheet}
     :hidden-spike-trap         {:x 14 :y 2
                                 :width 12 :height 12
                                 :color {:r 255 :g 140 :b 0 :a 255}
                                 :tile-sheet grim-tile-sheet}
     :hydra-head                {:x 6 :y 2
                                 :width 16 :height 16
                                 :color {:r 40 :g 156 :b 23 :a 255}
                                 :tile-sheet darkond-tile-sheet}
     :hydra-neck                {:x 12 :y 1
                                 :width 16 :height 16
                                 :color {:r 40 :g 156 :b 23 :a 255}
                                 :tile-sheet darkond-tile-sheet}
     :hydra-rear                {:x 12 :y 1
                                 :width 16 :height 16
                                 :color {:r 40 :g 156 :b 23 :a 255}
                                 :tile-sheet darkond-tile-sheet}
     :hydra-tail                {:x 12 :y 1
                                 :width 16 :height 16
                                 :color {:r 40 :g 156 :b 23 :a 255}
                                 :tile-sheet darkond-tile-sheet}
     :large-amoeba              {:x 7 :y 12
                                 :width 16 :height 16
                                 :color {:r 175 :g 251 :b 255 :a 125}
                                 :tile-sheet darkond-tile-sheet}
     :lichen                    {:x 15 :y 0
                                 :width 12 :height 12
                                 :color {:r 0 :g 255 :b 0 :a 255}
                                 :tile-sheet grim-tile-sheet}
     :m-portal                  {:x 4 :y 9
                                 :width 12 :height 12
                                 :color {:r 0 :g 0 :b 255 :a 255}
                                 :tile-sheet grim-tile-sheet}
     :magic-potion              {:x 13 :y 10
                                 :width 12 :height 12
                                 :color {:r 0 :g 0 :b 255 :a 255}
                                 :tile-sheet grim-tile-sheet}
     :maze-wall                 {:x 8 :y 5
                                 :width 12 :height 12
                                 :color {:r 0 :g 82 :b 3 :a 255}
                                 :tile-sheet grim-tile-sheet}
     :merchant                  {:x 13 :y 4
                                 :width 12 :height 12
                                 :color {:r 0 :g 0 :b 255 :a 255}
                                 :tile-sheet grim-tile-sheet}
     :visible-mimic             {:x 15 :y 8
                                 :width 12 :height 12
                                 :color {:r 255 :g 241 :b 36 :a 255}
                                 :tile-sheet grim-tile-sheet}
     :necromancer               {:x 10 :y 14
                                 :width 12 :height 12
                                 :color {:r 116 :g 84 :b 141 :a 255}
                                 :tile-sheet grim-tile-sheet}
     :open-door                 {:x 12 :y 5
                                 :width 12 :height 12
                                 :color {:r 255 :g 255 :b 255 :a 128}
                                 :tile-sheet grim-tile-sheet}
     :portal                    {:x 4 :y 9
                                 :width 12 :height 12
                                 :color {:r 102 :g 0 :b 102 :a 255}
                                 :tile-sheet grim-tile-sheet}
     :purchasable               {:x 2 :y 9
                                 :width 12 :height 12
                                 :color {:r 255 :g 255 :b 0 :a 255}
                                 :tile-sheet grim-tile-sheet}
     :skeleton                  {:x 3 :y 5
                                 :width 16 :height 16
                                 :color {:r 255 :g 255 :b 255 :a 255}
                                 :tile-sheet darkond-tile-sheet}
     :slime                     {:x 7 :y 15
                                 :width 16 :height 16
                                 :color {:r 72 :g 223 :b 7 :a 125}
                                 :tile-sheet darkond-tile-sheet}
     :snake                     {:x 3 :y 7
                                 :width 16 :height 16
                                 :color {:r 1 :g 255 :b 1 :a 255}
                                 :tile-sheet darkond-tile-sheet}
     :spider                    {:x 14 :y 9
                                 :width 16 :height 16
                                 :color {:r 183 :g 21 :b 3 :a 255}
                                 :tile-sheet darkond-tile-sheet}
     :spike-trap                {:x 4 :y 14
                                 :width 16 :height 16
                                 :color {:r 218 :g 165 :b 32 :a 255}
                                 :tile-sheet darkond-tile-sheet}
     :temple-wall               {:x 3 :y 2
                                 :width 12 :height 12
                                 :color {:r 218 :g 165 :b 32 :a 128}
                                 :tile-sheet grim-tile-sheet}
     :torch                     {:x 1 :y 2
                                 :width 12 :height 12
                                 :color {:r 255 :g 0 :b 0 :a 255}
                                 :tile-sheet grim-tile-sheet}
     :tree                      {:x 5 :y 0
                                 :width 20 :height 20
                                 :color {:r 21 :g 54 :b 21 :a 255}
                                 :tile-sheet bisasam-tile-sheet}
     :troll                     {:x 4 :y 5
                                 :width 12 :height 12
                                 :color {:r 255 :g 140 :b 1 :a 255}
                                 :tile-sheet grim-tile-sheet}
     :wall                      {:x 3 :y 2
                                 :width 12 :height 12
                                 :color {:r 255 :g 255 :b 255 :a 128}
                                 :tile-sheet grim-tile-sheet}
     :willow-wisp               {:x 10 :y 2
                                 :width 20 :height 20
                                 :color {:r 210 :g 138 :b 181 :a 125}
                                 :tile-sheet bisasam-tile-sheet}
     :player                    {:x 0 :y 4
                                 :width 12 :height 12
                                 :color {:r 255 :g 255 :b 255 :a 255}
                                 :tile-sheet grim-tile-sheet}}))

(def ^:private type->texture
  (memoize
    (fn [^Keyword type]
      (let [{:keys [x y width height
                    color tile-sheet]}
            (type->tile-info type)
            x (* x width)
            y (* y height)]
        (assoc (texture tile-sheet
                        :set-region x y width height)
               :color color)))))

(defn render-world
  [_ e-this {:keys [view-port-sizes]} system]
  (let [e-player (first (rj.e/all-e-with-c system :player))

        {:keys [x y]
         :as c-player-pos} (rj.e/get-c-on-e system e-player :position)
        player-pos (->2DPoint c-player-pos)
        fog-of-war? (:fog-of-war? (rj.e/get-c-on-e system e-player :player))

        c-sight (rj.e/get-c-on-e system e-player :playersight)
        sight (math/ceil (:distance c-sight))

        c-world (rj.e/get-c-on-e system e-this :world)
        levels (:levels c-world)
        world (nth levels (:z c-player-pos))

        [vp-size-x vp-size-y] view-port-sizes

        start-x (max 0 (- x (int (/ vp-size-x 2))))
        start-y (max 0 (- y (int (/ vp-size-y 2))))

        end-x (+ start-x vp-size-x)
        end-x (min end-x (count world))

        end-y (+ start-y vp-size-y)
        end-y (min end-y (count (first world)))

        start-x (- end-x vp-size-x)
        start-y (- end-y vp-size-y)

        renderer (new SpriteBatch)]
    (.begin renderer)
    (doseq [x (range start-x end-x)
            y (range start-y end-y)
            :let [tile (get-in levels [(:z c-player-pos) x y])]]
      (when (or (not fog-of-war?)
                (rj.u/can-see? world sight player-pos [x y]))
        (let [top-entity (rj.u/tile->top-entity tile)
              texture-entity (-> top-entity
                                 (:type) (type->texture))]
          (let [color-values (:color texture-entity)
                color-values (update-in color-values [:a]
                                        (fn [alpha]
                                          (if-let [c-destr (rj.e/get-c-on-e system (:id top-entity)
                                                                            :destructible)]
                                            (let [hp (:hp c-destr)
                                                  max-hp (:max-hp c-destr)]
                                              (max 75 (* (/ hp max-hp) alpha)))
                                            alpha)))]
            (.setColor renderer
                       (Color. (float (/ (:r color-values) 255))
                               (float (/ (:g color-values) 255))
                               (float (/ (:b color-values) 255))
                               (float (/ (:a color-values) 255)))))
          (.draw renderer
                 (:object texture-entity)
                 (float (* (+ (- x start-x)
                              (:left rj.cfg/padding-sizes))
                           (rj.cfg/block-size)))
                 (float (* (+ (- y start-y)
                              (:btm rj.cfg/padding-sizes))
                           (rj.cfg/block-size)))
                 (float (rj.cfg/block-size)) (float (rj.cfg/block-size))))))
    (.end renderer)))
