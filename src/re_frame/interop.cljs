(ns re-frame.interop
  (:require ["google-closure-library"]
            [reagent.core]
            [reagent.ratom]))

(js/goog.require "goog.async.nextTick")
(js/goog.require "goog.events")

(defn on-load
      [listener]
      ;; events/listen throws an exception in react-native environments because addEventListener is not available.
      (try
        (js/goog.events.listen js/self "load" listener)
        (catch :default _)))

(def next-tick js/goog.async.nextTick)

;; #queue literal

(def empty-queue (PersistentQueue. nil 0 nil []))

(def after-render next-tick)

;; Make sure the Google Closure compiler sees this as a boolean constant,
;; otherwise Dead Code Elimination won't happen in `:advanced` builds.
;; Type hints have been liberally sprinkled.
;; https://developers.google.com/closure/compiler/docs/js-for-compiler
(def ^boolean debug-enabled? ^boolean js/goog.DEBUG)

(defn ratom [x]
  (reagent.core/atom x))

;; NOTE: ratom? not implemented.
;; (defn ratom? [x]
;;   ;; ^:js suppresses externs inference warnings by forcing the compiler to
;;   ;; generate proper externs. Although not strictly required as
;;   ;; reagent.ratom/IReactiveAtom is not JS interop it appears to be harmless.
;;   ;; See https://shadow-cljs.github.io/docs/UsersGuide.html#infer-externs
;;   (satisfies? reagent.ratom/IReactiveAtom ^js x))

(defn deref? [x]
  (satisfies? IDeref x))


(defn make-reaction [f]
  (reagent.ratom/make-reaction f))

(defonce ^:private on-dispose-callbacks (atom {}))

(defn add-on-dispose!
  [a-ratom f]
  (do (swap! on-dispose-callbacks update a-ratom (fnil conj []) f)
      nil))

(defn dispose!
  [a-ratom]
  ;; Try to replicate reagent's behavior, releasing resources first then
  ;; invoking callbacks
  (let [callbacks (get @on-dispose-callbacks a-ratom)]
    (swap! on-dispose-callbacks dissoc a-ratom)
    (doseq [f callbacks] (f))))

(defn set-timeout! [f ms]
  (js/setTimeout f ms))

(defn now []
  (js/performance.now))

#_(defn reagent-id
  "Produces an id for reactive Reagent values
  e.g. reactions, ratoms, cursors."
  [reactive-val]
  ;; ^:js suppresses externs inference warnings by forcing the compiler to
  ;; generate proper externs. Although not strictly required as
  ;; reagent.ratom/IReactiveAtom is not JS interop it appears to be harmless.
  ;; See https://shadow-cljs.github.io/docs/UsersGuide.html#infer-externs
  (when (implements? reagent.ratom/IReactiveAtom ^js reactive-val)
    (str (condp instance? reactive-val
           reagent.ratom/RAtom "ra"
           reagent.ratom/RCursor "rc"
           reagent.ratom/Reaction "rx"
           reagent.ratom/Track "tr"
           "other")
         (hash reactive-val))))

(defn reagent-id
  "Produces an id for reactive Reagent values
  e.g. reactions, ratoms, cursors."
  [reactive-val]
  ;; ^:js suppresses externs inference warnings by forcing the compiler to
  ;; generate proper externs. Although not strictly required as
  ;; reagent.ratom/IReactiveAtom is not JS interop it appears to be harmless.
  ;; See https://shadow-cljs.github.io/docs/UsersGuide.html#infer-externs
  (hash reactive-val))
