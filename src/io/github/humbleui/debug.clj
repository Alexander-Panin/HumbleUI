(ns io.github.humbleui.debug
  (:require
    [io.github.humbleui.canvas :as canvas]
    [io.github.humbleui.core :as core]
    [io.github.humbleui.font :as font]
    [io.github.humbleui.paint :as paint]))

(defonce *enabled?
  (atom false))

(def width 100)

(def height 50)

(def max-time-ms 50)

(def data
  {:paint {:times (make-array Long/TYPE width)
           :idx   (volatile! 0)
           :t0    (volatile! (System/nanoTime))}
   :event {:times (make-array Long/TYPE width)
           :idx   (volatile! 0)
           :t0    (volatile! (System/nanoTime))}})

(defn on-start-impl [tag]
  (vreset! (-> data tag :t0) (System/nanoTime)))

(defmacro on-start [tag]
  `(when *enabled?
     (on-start-impl ~tag)))

(defn on-end-impl [tag]
  (let [dt (- (System/nanoTime) @(-> data tag :t0))]
    (aset (-> data tag :times) @(-> data tag :idx) dt)
    (vswap! (-> data tag :idx) #(-> % inc (mod width)))))

(defmacro on-end [tag]
  `(when *enabled?
     (on-end-impl ~tag)))

(defn draw-impl [canvas tag]
  (with-open [bg   (paint/fill 0x4033CC33)
              fg   (paint/fill 0xFF33CC33)
              font (font/make-with-size nil 11)]
    (let [ms->y #(- height (-> % (* height) (/ max-time-ms)))
          i0    @(-> data tag :idx)
          last  (-> data tag :times (aget (mod (+ i0 (- width 1)) width)) (/ 1000000.0))]
      (canvas/draw-rect canvas (core/irect-ltrb 0 0 width height) bg)
      (canvas/draw-rect canvas (core/irect-ltrb 0 (ms->y 16) width (+ (ms->y 16) 1)) bg)
      (canvas/draw-string canvas (str (name tag) " " (format "%.2f ms" last)) 4 12 font fg)
      (doseq [i (range width)
              :let [idx (-> i (+ i0) (mod width))
                    ms  (/ (aget (-> data tag :times) idx) 1000000.0)]]
        (canvas/draw-rect canvas (core/irect-ltrb i (ms->y ms) (+ i 1) height) fg)))))

(defmacro draw [canvas tag]
  `(when *enabled?
     (draw-impl ~canvas ~tag)))
