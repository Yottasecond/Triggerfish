(ns triggerfish.server.midi-control-adapters)

;;midi-val -> real-val 
(defn dial [{:keys [params]} val]
  (let [min (:min params)
        max (:max params)
        rng (- max min)
        ratio (/ rng 127)]
    (+ min (* ratio val))))

(defn toggle [ctl val]

  (let [old-val (:val ctl)]
    (if (> val 0) ;;when the midi ctl is non-zero, swap toggle value (ignore note-offs)
      (if (= old-val -1)
        1
        -1)
      old-val)))

(def adapters
  {:dial dial
   :toggle toggle})
