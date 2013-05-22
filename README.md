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
[bandito "1.0.0"]
```

## Usage

The current version is *1.0.0*

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

Use `runtest!` to mash together your variants into a single view function
that can be used as normal. `runtest!` takes a (unique) keyword identifying
the experiment, and a hash-map of view functions identified by keywords:

```clojure
(require '[bandito.core :as bandito])

;; Define your experiment views. You can add new views on the end, but don't
;; remove them while the experiment is running!
(def viewmap
  {:view1 view1
   :view2 view2
   :view3 view3})

(def experiment-view (bandito/runtest! :banditotest viewmap))
```

You can use `experiment-view` here as you would normally use `view1`, `view2`, or `view3`

### Logging conversions

In the view representing your conversion, include a call to `convert!` with the experiment
keyword and the request map (from which bandito will pull the view that was shown):

```clojure
(defn conversion-view [req]
  (bandito/convert! :banditotest req)
  ; ... the rest of your view
)
```

### Fetching and loading results

Bandito doesn't know or care how you store the results. You can grab the current
results report at any time using `as-map`:

```clojure
(bandito/as-map)

;; How your test might look after running for a bit

; {:banditotest
;   {:view1 {:views 2 :conversions 0}
;    :view2 {:views 7 :conversions 1}
;    :view3 {:views 17 :conversions 3}}}
```

You can also give a function that will accept the above map to `persist!`, with
the expectation that said function will write the data somewhere. The inverse of
`persist!` is `load!`, which will initialize bandito with the provided data,
which must be in the format as returned by `as-map`

## License

Copyright © 2013 Adam Bard (adambard.com)

Distributed under the Eclipse Public License, the same as Clojure.
