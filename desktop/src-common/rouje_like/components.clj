(ns rouje-like.components
  (:import [com.badlogic.gdx.graphics.g2d TextureRegion]
           [clojure.lang Atom Fn Keyword PersistentVector]))

(def block-size 27)


(defrecord World [^Atom world])

(defrecord Tile [^Number x ^Number y
                 ^PersistentVector entities])

(defrecord Entity [^Keyword id
                   ^Keyword type])

(defrecord Player [^Atom show-world?])

(defrecord Digger [^Fn can-dig?
                   ^Fn dig!])

(defrecord MovesLeft [^Atom moves-left])

(defrecord Gold [^Atom gold])

(defrecord Sight [^Atom distance
                  ^Atom decline-rate
                  ^Atom lower-bound
                  ^Atom upper-bound])

(defrecord Position [^Atom world
                     ^Atom x
                     ^Atom y])

(defrecord Mobile [^Fn can-move?
                   ^Fn move!])

(defrecord Attacker [^Atom attack
                     ^Fn attack!
                     ^Fn can-attack?])

(defrecord Destructible [^Atom hp
                         ^Atom defense
                         ^Fn take-damage!])

(defrecord Tickable [^Fn tick-fn
                     args])

(defrecord Lichen [^Atom grow-chance%])

(defrecord Renderable [^Number pri
                       ^Fn render-fn
                       args #_(args-type=map)])

;; Workaround for not being able to get record's type "statically"
(def get-type {:world        (type (->World nil))
               :tile         (type (->Tile nil nil nil))
               :entity       (type (->Entity nil nil))
               :player       (type (->Player nil))
               :digger       (type (->Digger nil nil))
               :position     (type (->Position nil nil nil))
               :mobile       (type (->Mobile nil nil))
               :moves-left   (type (->MovesLeft nil))
               :gold         (type (->Gold nil))
               :sight        (type (->Sight nil nil nil nil))
               :renderable   (type (->Renderable nil nil nil))
               :attacker     (type (->Attacker nil nil nil))
               :destructible (type (->Destructible nil nil nil))
               :tickable     (type (->Tickable nil nil))
               :lichen       (type (->Lichen nil))})

(def get-pri {:floor 1
              :torch 2
              :gold 2
              :lichen 3
              :wall 3
              :player 10})

(defn sort-by-pri [coll]
  (sort (fn [arg1 arg2]
          (let [t1 (:type arg1)
                t2 (:type arg2)]
            (if (= t1 t2)
              0
              (- (get-pri t2) (get-pri t1)))))
        coll))
