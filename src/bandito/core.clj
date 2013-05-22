(ns bandito.core)

; Ephemeral result store
(def report (atom {}))

; Configuration map
(def config (atom {:epsilon 0.1}))

; Counters

(defn- inc-in-report [test-key option-key counter-key]
  (assert (keyword? test-key))
  (assert (keyword? option-key))
  (assert (keyword? counter-key))
  (fn [report]
    (let [v (-> report test-key option-key counter-key)
          v (inc (or v 0))]
      (assoc-in report [test-key option-key counter-key] v))))

(defn- inc-conversions! [test-key option-key]
  (swap! report (inc-in-report test-key option-key :conversions)))

(defn- inc-views! [test-key option-key]
  (swap! report (inc-in-report test-key option-key :views)))

; Pickers

(defn- calc-ratio [[k resultmap]]
  (let [conversions (or (:conversions resultmap) 0)
        views (or (:views resultmap) 0)
        ratio (double (if (= views 0) 0 (/ conversions views)))]
    [k ratio])
  )

(defn- best-contender [leader contender]
  (if (> (last contender) (last leader)) contender leader))

(defn- pick-random-view
  "Return a random key from viewmap"
  [k viewmap]
  (rand-nth (keys viewmap)))

(defn- pick-best-view
  "Return the key in viewmap that has the best conversion ratio"
  [k viewmap]
  (let [ratios (map calc-ratio (-> @report :banditotest))]
    (if (< (count ratios) (count viewmap)) ; Give everyone a chance
      (pick-random-view k viewmap)
      (first (reduce best-contender ratios)))))

(defn pick-view
  [k viewmap]
  (if (< (rand) (get @config :epsilon))
    (pick-random-view k viewmap)
    (pick-best-view k viewmap)))

; Exposed api

(defn runtest!
  "Define a test to be run. Accepts a keyword uniquely identifying the test,
  and a hash-map of views identified by unique keywords."
  [k viewmap]

  (assert (keyword? k))
  (assert (map? viewmap))

  ; Return a modified view function
  (fn [req]
    (let [viewkey (or (-> req :session :bandito-state k)
                      (pick-view k viewmap))
          resp ((viewmap viewkey) req)]
      (inc-views! k viewkey)
      (if (string? resp)
        {:status 200
         :body resp
         :session (assoc-in (:session req) [:bandito-state k] viewkey)}
        (assoc-in resp [:session :bandito-state k] viewkey)))))

(defn convert!
  "Record a conversion"
  [k req]
  (assert (keyword? k))
  (let [choice-shown (-> req :session :bandito-state k)]
    (if (not (nil? choice-shown))
      (inc-conversions! k choice-shown))))

(defn persist!
  "Accept a function that will persist the conversion report"
  [f]
  (f @report))

(defn load!
  "Load the given conversion report."
  [data]
  (reset! report data))

(defn as-map
  "Output the report (or one test) as a clojure map"
  ([] @report)
  ([k] (get @report k)))
