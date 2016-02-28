# Bandito: Easy conversion optimization for Ring apps.

Bandito is a small clojure library designed to help you easily run
epsilon-greedy multi-armed bandit tests. It's designed to be used with [Ring](), and optionally a routing
library like [Compojure]()

Bandito will take care of the following for you:

* Selecting your best-converting variant based on an [epsilon-greedy](http://en.wikipedia.org/wiki/Multi-armed_bandit#Semi-uniform_strategies) algorithm.
* Making sure a given user sees the same page consistently.
* Storing the results in memory. Persisting your results to disk somehow is up to you.

*Bandito requires that you use ring's **session middleware**.*

## Installation

Bandito is available from clojars, so you just need to put the following in
your project `:dependencies`

```
[bandito "1.1.0"]
```

## Usage

The current version is *1.1.0*

To start doing tests, you need to make different variants. Bandito
assumes that your variants can be expressed as different view functions.
The view functions (i.e. handler functions) you provide must take a single argument representing
a Ring request map, and respond with either a string or a Ring response map

Here's some test views we'll use for examples.

```clojure
(defn view1 [req] "View #1")
(defn view2 [req] "View #2")
(defn view3 [req] "View #3")
```

### Running experiments

Use `init-experiment` to create an experiment -- that is, a container
for your results. You can define this in your top level or give it a lifecycle
with component or mount, whatever you like. `init-experiment` an optional configuration
map, and a hash-map of view functions identified by keywords. Use
`experiment-handler` to create a ring handler from the experiment that will
track views and conversions.

```clojure
(require '[bandito.core :as bandito])

;; Define your experiment views. You can add new views on the end, but don't
;; remove them while the experiment is running!
(def viewmap
  {:view1 view1
   :view2 view2
   :view3 view3})

(def experiment (bandito/init-experiment viewmap))
(def experiment-view (bandito/experiment-handler experiment))
```

You can use `experiment-view` here as you would normally use `view1`, `view2`, or `view3`

An `experiment` is a clojure record with 4 properties:

* `:report`, an atom containing a map representing the results of your experiment,
* `:config`, a hash-map containing configuration options
* `:view-map`, the view map described above

### Logging conversions

In the view representing your conversion, include a call to `convert!` with the experiment
and the request map (from which bandito will pull the view that was shown):

```clojure
(defn conversion-view [req]
  (bandito/convert! experiment req)
  ; ... the rest of your view
)
```

### Fetching and loading results

Bandito doesn't know or care how you store the results. You can grab the current
results report at any time using `as-map`:

```clojure
(bandito/as-map experiment)

;; How your test might look after running for a bit

;   {:view1 {:views 2 :conversions 0}
;    :view2 {:views 7 :conversions 1}
;    :view3 {:views 17 :conversions 3}}
```

To load persisted results, just update the `:report` atom within the experiment
with a map of a form similar to the above:

```clojure
(reset! (:report experiment) my-report-map)
```


## License

Copyright © 2013-2016 Adam Bard (adambard.com)

Distributed under the Eclipse Public License, the same as Clojure.
