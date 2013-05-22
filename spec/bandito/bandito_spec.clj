(ns bandito.bandito_spec
  (:use speclj.core)
  (:require [bandito.core :as bandito]))

(defn view1 [req] "View #1")
(defn view2 [req] "View #2")
(defn view3 [req] "View #3")
(def viewmap {:view1 view1 :view2 view2 :view3 view3})

(defn reset-report [views conversions]
  (reset!
    bandito/report
    {:banditotest
     {:view1 {:views (nth views 0)
              :conversions (nth conversions 0)}
      :view2 {:views (nth views 1)
              :conversions (nth conversions 1)}
      :view3 {:views (nth views 2)
              :conversions (nth conversions 2)}}}))

(defn mock-req [choice]
  {:session {:bandito-state {:banditotest choice}}})

(describe "Bandito multi-armed bandit testing"

  (before
    (reset! bandito/config {:epsilon 0}); We're not testing rand-nth here
    (reset! bandito/report {}))

  (it "Saves the choice in the session and uses it if provided"
    (let [req (mock-req :view1)]
      ; 10 times
      ((bandito/runtest! :banditotest viewmap) req)
      ((bandito/runtest! :banditotest viewmap) req)
      ((bandito/runtest! :banditotest viewmap) req)
      ((bandito/runtest! :banditotest viewmap) req)
      ((bandito/runtest! :banditotest viewmap) req)
      ((bandito/runtest! :banditotest viewmap) req)
      ((bandito/runtest! :banditotest viewmap) req)
      ((bandito/runtest! :banditotest viewmap) req)
      ((bandito/runtest! :banditotest viewmap) req)
      ((bandito/runtest! :banditotest viewmap) req)
      (should= 10 (-> @bandito/report :banditotest :view1 :views))
      (should= :view1 (-> ((bandito/runtest! :banditotest viewmap) req) :session :bandito-state :banditotest))
      ))

  (it "Does not register a conversion if no session or data are present"
      (reset! bandito/report {})
      (bandito/convert! :banditotest {})
      (bandito/convert! :banditotest {:session {:bandito-state {:badkey :view1}}})
      (should= @bandito/report {})
      (reset! bandito/report {:banditotest {:view1 {:views 1}}})
      (bandito/convert! :banditotest {:session {:bandito-state {:banditotest :view1}}})
      (should= @bandito/report {:banditotest {:view1 {:views 1 :conversions 1}}})
      )

  (it "Chooses the best choice"
    (reset-report [1 1 1000] [0 0 1])
    (dotimes [n 100]
      (should= "View #3"(:body ((bandito/runtest! :banditotest viewmap) {})))))

  (it "Records a conversion"
      (reset-report [1 1 1] [0 0 0])
      (should= 0 (-> @bandito/report :banditotest :view2 :conversions))
      (bandito/convert! :banditotest (mock-req :view2))
      (should= 1 (-> @bandito/report :banditotest :view2 :conversions)))

  (it "Can be persisted"
      (reset-report [1 2 3] [0 1 0])
      (let [store (atom {})]
        (bandito/persist! (partial reset! store))
        (should= @store @bandito/report)))
)


