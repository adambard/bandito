(ns bandito.core)

(defn generate-random-key []
  (keyword (str (java.util.UUID/randomUUID))))

; Configuration map
(def default-config {:epsilon 0.1})

(defprotocol BanditoReport
  (as-map [this]))

(defrecord Experiment [k report config view-map]
  BanditoReport
  (as-map [this]
    (deref (:report this))))


; Counters

(defn- inc-in-report [option-key counter-key]
  (assert (keyword? option-key))
  (assert (keyword? counter-key))
  (fn [report]
    (let [v (-> report option-key counter-key)
          v (inc (or v 0))]
      (assoc-in report [option-key counter-key] v))))

(defn- inc-conversions! [{report :report :as experiment}
                         option-key]
  (swap! report (inc-in-report option-key :conversions)))

(defn- inc-views! [{report :report :as experiment}
                   option-key]
  (swap! report (inc-in-report option-key :views)))


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
  [viewmap]
  (rand-nth (keys viewmap)))

(defn- pick-best-view
  "Return the key in viewmap that has the best conversion ratio"
  [{report :report  :as experiment} viewmap]
  (let [ratios (map calc-ratio @report)]
    (if (< (count ratios) (count viewmap)) ; Give everyone a chance
      (pick-random-view viewmap)
      (first (reduce best-contender ratios)))))

(defn pick-view
  [{config :config :as experiment} viewmap]
  (if (< (rand) (get config :epsilon))
    (pick-random-view viewmap)
    (pick-best-view experiment viewmap)))

; Exposed api

(defn init-experiment
  ([viewmap] (init-experiment {} viewmap))
  ([config viewmap] (->Experiment
                        (generate-random-key)
                        (atom {})
                        (merge default-config config)
                        viewmap)))

(defn experiment-handler
  "Define a test to be run. Accepts a keyword uniquely identifying the test,
  and a hash-map of views identified by unique keywords."
  [{k :k viewmap :view-map :as experiment}]

  (assert (keyword? k))
  (assert (map? viewmap))

  ; Return a modified view function
  (fn [req]
    (let [viewkey (or (-> req :session :bandito-state k)
                      (pick-view experiment viewmap))
          resp ((get viewmap viewkey) req)]
      (inc-views! experiment viewkey)
      (if (string? resp)
        {:status 200
         :body resp
         :session (assoc-in (:session req) [:bandito-state k] viewkey)}
        (assoc-in resp [:session :bandito-state k] viewkey)))))

(defn convert!
  "Record a conversion"
  [{k :k :as experiment} req]
  (let [choice-shown (-> req :session :bandito-state k)]
    (if (not (nil? choice-shown))
      (inc-conversions! experiment choice-shown))))
