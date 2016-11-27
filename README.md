# cljgae-template

A Leiningen template for creating useful and productive GAE apps in Clojure 
using the GAE Java SDK.

## Usage

    lein new cljgae-template <project name>

Creates a new appengine project on disk under dir <project name> that should run
on the latest GAE. It has a few routes with corresponding tests which show the 
usage of a few appengine APIs such as 

* Google Cloud Storage (via file upload test example)
* The datastore, including a Clojure DSL for querying the datastore (see 
  `db.clj` test for examples)
* The App Identity Service API (via "/" route) 
* Asyncronous task queues / AKA appengine"push queues" (via a JSON request of 
  a "large" list of data points)

With unit tests for each. All examples also run on the dev appserver.

## Datastore query language

A Clojure DSL has been developed inspired by the Python NDB library (with an 
emphasis on Clojure's more functional idiom.) Queries return a lazy sequence.

### Examples

```clojure
(defentity AnotherEntity [content saved-time int-value])

(def entity (save! (create-AnotherEntity "Some content woo" (t/date-time 1980 3 5) 6)))
(def entity2 (save! (create-AnotherEntity "Other content" (t/date-time 1984 10 12) 17)))

; query all
(query-AnotherEntity [])
; equality
(query-AnotherEntity [:content = "Some content woo"])
; not equal
(query-AnotherEntity [:content != "Other content"])
; greater-than and less-than
(query-AnotherEntity [:int-value < 7])
; time: before and after
(query-AnotherEntity [:saved-time > (.toDate (t/date-time 1979 3 5))])
; "and" & "or" queries
(query-AnotherEntity [:and [:content = "Some content woo"] [:int-value > 5]])
(query-AnotherEntity [:or [:content = "Some content woo"] [:int-value < 5]])
; compound queries with nested compound predicates
(query-AnotherEntity [:or [:content = "Other content"] [:and [:saved-time < (.toDate (t/date-time 1983 3 5))] [:int-value = 6]]])
; keys-only support
(query-AnotherEntity  [:int-value < 7] [:keys-only true])
; order-by support
(query-AnotherEntity  [:or [:content = "Some content woo"] [:int-value > 5]] [:order-by :int-value :desc])
; keys only and order-by support together, of course
(query-AnotherEntity  [:saved-time > (.toDate (t/date-time 1979 3 5))] [:keys-only true :order-by :int-value :desc])
```

## Future directions

* Add support for projections and ancestor queries
* More comprehensive examples of task queues
* More comprehensive use of cloud storage
* Examples of other commonly used GAE apis

## License

Copyright © 2016 Peter Schwarz and Nick Bauman

Distributed under the Eclipse Public License either version 1.0 or (at your 
option) any later version.
