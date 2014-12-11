(ns rouje-like.t-inventory
  (:use midje.sweet)
  (:require [rouje-like.utils :refer [? update-gold]]
            [rouje-like.entity-wrapper :as rj.e]
            [rouje-like.equipment :as rj.eq]
            [rouje-like.status-effects :as rj.stef]
            [rouje-like.core :as rj.core]
            [brute.entity :as br.e]
            [rouje-like.inventory :as rj.inv]
            [rouje-like.utils :as rj.u :refer [?]]))

(defn get-system []
  (with-open [w (clojure.java.io/writer "NUL")]
    (binding [*out* w]
      (-> (br.e/create-system)
          (rj.core/init-entities {})))))

(let [system (get-system)
      e-player (first (rj.e/all-e-with-c system :player))
      c-inv (rj.e/get-c-on-e system e-player :inventory)
      item (rj.eq/generate-random-equipment)
      item2 (rj.eq/generate-random-equipment)
      junk (:junk c-inv)]
  (fact "update-junk"
        (rj.inv/update-junk system e-player _)=future=> truthy)
  (fact "add-junk"
        (as-> system system
              (rj.inv/add-junk system e-player item)
              (:junk (rj.e/get-c-on-e system e-player :inventory)))
        => (conj junk item))
  (fact "junk-value"
        (as-> system system
              (rj.inv/add-junk system e-player item)
              (rj.inv/junk-value system e-player))
        => (rj.eq/equipment-value item))
  (fact "sell-junk"
        (as-> system system
              (rj.inv/add-junk system e-player item)
              (rj.inv/sell-junk system e-player)
              (:gold (rj.e/get-c-on-e system e-player :wallet)))
        => (rj.eq/equipment-value item))
  (fact "switch-slot-item"
        (as-> system system
              (rj.inv/switch-slot-item system e-player item)
              (:slot (rj.e/get-c-on-e system e-player :inventory)))
        => item)
  (fact "pickup-slot-item"
        (as-> system system
              (rj.inv/switch-slot-item system e-player item)
              (rj.inv/pickup-slot-item system e-player item2)
              (? (first (:junk (rj.e/get-c-on-e system e-player :inventory))))
              [(:slot (rj.e/get-c-on-e system e-player :inventory))
               (first (:junk (rj.e/get-c-on-e system e-player :inventory)))])
        =future=> [item2 item]))