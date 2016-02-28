(ns bandito.bandito_spec
  (:use speclj.core)
  (:require [bandito.core :as bandito]))

(defn view1 [req] "View #1")
(defn view2 [req] "View #2")
(defn view3 [req] "View #3")
(def viewmap {:view1 view1 :view2 view2 :view3 view3})

(defn reset-report [experiment views conversions]
  (reset!
    (:report experiment)
    
     {:view1 {:views (nth views 0)
              :conversions (nth conversions 0)}
      :view2 {:views (nth views 1)
              :conversions (nth conversions 1)}
      :view3 {:views (nth views 2)
              :conversions (nth conversions 2)}}))

(defn mock-req [{k :k :as experiment} choice]
  {:session {:bandito-state {k choice}}})

(describe "Bandito multi-armed bandit testing"

  (it "Saves the choice in the session and uses it if provided"
    (let [experiment (bandito/init-experiment {:epsilon 0} viewmap)
          req (mock-req experiment :view1)]
      ; 10 times
      ((bandito/experiment-handler experiment) req)
      ((bandito/experiment-handler experiment) req)
      ((bandito/experiment-handler experiment) req)
      ((bandito/experiment-handler experiment) req)
      ((bandito/experiment-handler experiment) req)
      ((bandito/experiment-handler experiment) req)
      ((bandito/experiment-handler experiment) req)
      ((bandito/experiment-handler experiment) req)
      ((bandito/experiment-handler experiment) req)
      ((bandito/experiment-handler experiment) req)
      (should= 10 (-> experiment :report (deref) :view1 :views))
      (should= :view1 (-> ((bandito/experiment-handler experiment) req)
                          :session :bandito-state (get (:k experiment))))))


  (it "Does not register a conversion if no session or data are present"
    (let [experiment (bandito/init-experiment {:epsilon 0} viewmap)]
      (bandito/convert! experiment {})
      (bandito/convert! experiment {:session {:bandito-state {:badkey :view1}}})
      (should= @(:report experiment) {})
      (reset! (:report experiment) {:view1 {:views 1}})
      (bandito/convert! experiment {:session {:bandito-state {(:k experiment) :view1}}})
      (should= @(:report experiment) {:view1 {:views 1 :conversions 1}} ))
      )

  (it "Chooses the best choice"
    (let [experiment (bandito/init-experiment {:epsilon 0} viewmap)]
      (reset-report experiment [1 1 1000] [0 0 1])
      (dotimes [n 100]
        (should= "View #3"(:body ((bandito/experiment-handler experiment) {}))))))

  (it "Records a conversion"
    (let [experiment (bandito/init-experiment {:epsilon 0} viewmap)]
      (reset-report experiment [1 1 1] [0 0 0])
      (should= 0 (-> @(:report experiment) :view2 :conversions))
      (bandito/convert! experiment (mock-req experiment :view2))
      (should= 1 (-> @(:report experiment) :view2 :conversions))))
)


