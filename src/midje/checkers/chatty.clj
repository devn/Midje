(ns ^{:doc "Checkers that explain more about a failure."}
  midje.checkers.chatty
  (:use [midje.checkers.util :only [named-as-call]]
        [midje.checkers.defining :only [as-checker]]
        [midje.util.form-utils :only [pairs quoted? single-destructuring-arg->form+name]]))

;; Note: checkers need to be exported in ../checkers.clj

;; TODO: It might make sense to split the notion of extended-falsehood out of
;; that of chatty checkers, since there are now other kinds of checkers that
;; generate chatty falsehoods.

(defn as-chatty-falsehood [value]
  (with-meta value {:midje/chatty-checker-falsehood true}))

(defn chattily-false? [value]
  (or (not value)
      (:midje/chatty-checker-falsehood (meta value))))

(defn as-chatty-checker [function]
  (as-checker (vary-meta function assoc :midje/chatty-checker true)))

(defn chatty-falsehood-to-map [value]
  (with-meta value {}))

(defn chatty-checker-falsehood? [value]
  (:midje/chatty-checker-falsehood (meta value)))

(defn add-actual [actual result]
  (if (chatty-checker-falsehood? result)
    (assoc result :actual actual)
    result))
  
(defn chatty-checker? [fn]
  (:midje/chatty-checker (meta fn)))

(defn chatty-worth-reporting-on? [arg]
  (and (or (list? arg) (seq? arg)) ; what started as a list (fn x y) might now be a seq.
       (pos? (count arg))
       (not (quoted? arg))))

(defn chatty-untease [result-symbol arglist]
  (reduce (fn [[complex-forms substituted-args] current-arg]
              (if (chatty-worth-reporting-on? current-arg)
                [ (conj complex-forms current-arg), 
                  (conj substituted-args `(~result-symbol ~(count complex-forms))) ]
                [complex-forms, (conj substituted-args current-arg)]))
      [[] []]
      arglist))

(defmacro chatty-checker
  "Create a function that returns either true or a detailed description of a failure."
  [ [actual-arg] [f & args] ]
  (let [result-symbol (gensym "chatty-intermediate-results-")
        [complex-forms substituted-args] (chatty-untease result-symbol args)
        [arg-form arg-name] (single-destructuring-arg->form+name actual-arg)]
    `(as-chatty-checker
      (fn [~arg-form]
        (let [~result-symbol (vec ~complex-forms)]
          (if (chattily-false? (~f ~@substituted-args))
            (let [pairs# (pairs '~complex-forms ~result-symbol)]
              (as-chatty-falsehood {:actual ~arg-name
                                    :intermediate-results pairs#}))
            true))))))
