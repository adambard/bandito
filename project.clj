(defproject bandito "1.0.1"
  :description "Easy episilon-greedy multi-armed banditry for clojure"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/data.json "0.2.2"]
                 ]
  :profiles  {:dev  {:dependencies  [[speclj "2.5.0"]]}}
  :plugins  [[speclj "2.5.0"] ]
  :test-paths  ["spec" "test"]
  )
