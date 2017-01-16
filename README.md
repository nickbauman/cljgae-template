# cljgae-template

A Leiningen template for creating useful and productive GAE apps in Clojure 
using the GAE Java SDK.

## Release 0.3.0-SNAPSHOT

Leiningen Clojars dependency:

```clojure
[org.clojars.nicolaus/cljgae-template "0.3.0-SNAPSHOT"]
```

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
* User services with user state examples

With unit tests for each. All examples also run on the dev appserver.

## Datastore query language

A Clojure DSL has been developed inspired by the Python NDB library (with an 
emphasis on Clojure's more functional idiom.) Queries return a lazy sequence.

### Examples

```clojure
(defentity BasicEntity [content saved-time])

(defentity AnotherEntity [content saved-time int-value])

(let [entity (save! (create-AnotherEntity "Some content woo" (t/date-time 1980 3 5) 6))
      entity2 (save! (create-AnotherEntity "Other content" (t/date-time 1984 10 12) 91))
      entity3 (save! (create-AnotherEntity "More interesting content" (t/date-time 1984 10 12) 17))
      root-entity (save! (create-BasicEntity "basic entity content" (t/date-time 2015 6 8)))
      child-entity1 (save! (create-AnotherEntity "child one content" (t/date-time 2016 12 10) 33) (gae-key root-entity))
      child-entity2 (save! (create-AnotherEntity "child two content" (t/date-time 2016 12 10) 44) (gae-key root-entity))]   
                                        ; query all
  (query-AnotherEntity [])
                                        ; equality
  (query-AnotherEntity [:content = "Some content woo"])
  (query-AnotherEntity [:content = "Blearg not found"])
                                        ; not equal
  (query-AnotherEntity [:content != "Not found"])
  (query-AnotherEntity [:content != "Other content"])
                                        ; greater-than and less-than
  (query-AnotherEntity [:int-value < 7])
  (query-AnotherEntity [:int-value < 5])
                                        ; time: before and after
  (query-AnotherEntity [:saved-time > (.toDate (t/date-time 1979 3 5))])
  (query-AnotherEntity [:saved-time < (.toDate (t/date-time 1979 3 5))])
                                        ; "and" compound queries
  (query-AnotherEntity [:and [:content = "Some content woo"] [:int-value > 5]])
  (query-AnotherEntity [:and [:int-value > 5] [:int-value <= 17]])
                                        ; "or" compound queries
  (query-AnotherEntity [:or [:content = "Some content woo"] [:int-value < 5]])
  (query-AnotherEntity [:or [:content = "Some content woo"] [:int-value > 5]])
                                        ; compound queries with nested compound predicates
  (query-AnotherEntity [:or [:content = "Other content"] 
                        [:and [:saved-time < (.toDate (t/date-time 1983 3 5))] [:int-value = 6]]])
                                        ; keys-only support
  (query-AnotherEntity [:int-value < 7] [:keys-only true])
                                        ; order-by support
  (query-AnotherEntity [:int-value > 0] [:order-by :int-value :desc])
                                        ; keys only and order-by support together 
  (query-AnotherEntity [:int-value > 0] [:keys-only true :order-by :int-value :desc])
                                        ; support multiple sort orders (with keys-only, too)
  (query-AnotherEntity [:saved-time > 0] [:order-by :saved-time :desc :int-value :asc :keys-only true])
                                        ; parents can find their children
  (query-AnotherEntity [] [:ancestor-key (gae-key root-entity)])
                                        ; ancestors that work with predicates
  (query-AnotherEntity [:int-value > 33] [:ancestor-key (gae-key root-entity)])
                                        ; ancestors that work with keys-only support
  (query-AnotherEntity [] [:keys-only true :ancestor-key (gae-key root-entity)])
  (query-AnotherEntity [] [:ancestor-key (gae-key root-entity) :keys-only true])
                                        ; ancestors that work with order-by
  (query-AnotherEntity [] [:ancestor-key (gae-key root-entity) :order-by :int-value :desc])
                                        ; transactions
  (with-transaction
     (save! (create-AnotherEntity "Content information" (t/date-time 1984 10 12) 201)))
                                        ; Cross-group transactions
  (with-xg-transaction
     (save! (create-AnotherEntity "Content information" (t/date-time 1984 10 12) 6001))
     (save! (create-AnotherEntity "More content information" (t/date-time 1984 10 12) 6002))))
```

## Future directions

* Add support for projections
* More comprehensive examples of task queues
* More comprehensive use of cloud storage
* Examples of other commonly used GAE apis

## License

Copyright © 2016, 2017 Peter Schwarz and Nick Bauman

Distributed under the Eclipse Public License either version 1.0 or (at your 
option) any later version.
