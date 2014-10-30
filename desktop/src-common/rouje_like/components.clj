(ns rouje-like.components
  (:import [com.badlogic.gdx.graphics.g2d TextureRegion]
           [clojure.lang Atom Fn Keyword PersistentVector]))

;;TODO: PUT IN config.clj
(def block-size 18)
(def world-sizes {:width  60
                  :height 60})
(def view-port-sizes [20 20])
(def padding-sizes {:top   1
                    :btm   1
                    :left  1
                    :right 1})

(defrecord Bat [])

(defrecord Broadcaster [msg-fn])

(defrecord Counter [turn])

(defrecord Class- [class])

(defrecord Digger [^Fn can-dig?-fn
                   ^Fn dig-fn])

(defrecord Entity [^Keyword id
                   ^Keyword type])

(defrecord Experience [experience])

(defrecord Gold [value])

(defrecord Item [pickup-fn])

(defrecord Killable [experience])

(defrecord Lichen [grow-chance%
                   max-blob-size])

(defrecord MovesLeft [moves-left])

(defrecord Player [show-world?])

(defrecord PlayerSight [distance
                        decline-rate
                        lower-bound
                        upper-bound
                        torch-power])

(defrecord Position [x y
                     ^Keyword type])

(defrecord Race [race])

(defrecord Receiver [])

(defrecord Relay [static
                  blocking])

(defrecord Sight [distance])

(defrecord Skeleton [])

(defrecord Snake [])

(defrecord Tile [^Number x ^Number y
                 ^PersistentVector entities])

(defrecord Torch [brightness])

(defrecord Wallet [^Number gold])

(defrecord World [world])

(defprotocol IAttacker
  (can-attack? [this e-this e-target system])
  (attack      [this e-this e-target system]))
(defrecord Attacker [^Number atk
                     ^Fn attack-fn
                     ^Fn can-attack?-fn
                     is-valid-target?]
  IAttacker
  (can-attack?     [this e-this e-target system]
    (can-attack?-fn this e-this e-target system))
  (attack     [this e-this e-target system]
    (attack-fn this e-this e-target system)))

(defprotocol IDestructible
  (take-damage [this e-this damage from system]))
(defrecord Destructible [^Number hp
                         ^Number defense
                         can-retaliate?
                         ^Fn take-damage-fn]
  IDestructible
  (take-damage     [this e-this damage from system]
    (take-damage-fn this e-this damage from system)))

(defprotocol IMobile
  (can-move? [this e-this target-tile system])
  (move      [this e-this target-tile system]))
(defrecord Mobile [^Fn can-move?-fn
                   ^Fn move-fn]
  IMobile
  (can-move?     [this e-this target-tile system]
    (can-move?-fn this e-this target-tile system))
  (move     [this e-this target-tile system]
    (move-fn this e-this target-tile system)))

(defprotocol IRenderable
  (render [this e-this args system]))
(defrecord Renderable [^Fn render-fn
                       args]
  IRenderable
  (render     [this e-this argz system]
    (render-fn this e-this argz system)))

(defprotocol ITickable
  (tick [this e-this system]))
(defrecord Tickable [^Fn tick-fn pri]
  ITickable
  (tick     [this e-this system]
    (tick-fn this e-this system)))

(def ^{:doc "Workaround for not being able to get record's type 'statically'"}
  get-type {:attacker     (type (->Attacker nil nil nil nil))
            :bat          (type (->Bat))
            :broadcaster  (type (->Broadcaster nil))
            :counter      (type (->Counter nil))
            :destructible (type (->Destructible nil nil nil nil))
            :digger       (type (->Digger nil nil))
            :entity       (type (->Entity nil nil))
            :gold         (type (->Gold nil))
            :item         (type (->Item nil))
            :lichen       (type (->Lichen nil nil))
            :mobile       (type (->Mobile nil nil))
            :moves-left   (type (->MovesLeft nil))
            :player       (type (->Player nil))
            :playersight  (type (->PlayerSight nil nil nil nil nil))
            :position     (type (->Position nil nil nil))
            :receiver     (type (->Receiver))
            :relay        (type (->Relay nil nil))
            :renderable   (type (->Renderable nil nil))
            :sight        (type (->Sight nil))
            :skeleton     (type (->Skeleton))
            :snake        (type (->Snake))
            :tickable     (type (->Tickable nil nil))
            :tile         (type (->Tile nil nil nil))
            :torch        (type (->Torch nil))
            :wallet       (type (->Wallet nil))
            :world        (type (->World nil))})
