(ns behaviors.t-line-number-reporting
  (:use [midje.sweet])
  (:use [clojure.test])
  (:use [midje.test-util]))


(defn f [n] n)

(def position-1 9)

(after-silently 
 (fact (+ 1 1) => 3)
 (fact
   @reported => (just
		 (contains
		  {:type :mock-expected-result-failure
		   :position ["t_line_number_reporting.clj" (+ position-1 3)]}))))

(after-silently 
 (fact (+ 1 1) => 3)  ; same line as fact
 (fact
   @reported => (just
		 (contains
		  {:type :mock-expected-result-failure
		   :position ["t_line_number_reporting.clj" (+ position-1 11)]}))))

(after-silently 
 (fact "odd positioning"
   
   (+ 1 1) =>   

   3)
 (fact
   @reported => (just
		 (contains
		  {:type :mock-expected-result-failure
		   :position ["t_line_number_reporting.clj" (+ position-1 21)]}))))

(defn g [n] n)
(def position-2 40)

(after-silently 
 (fact (g 1) => 1
   (provided (f 2) => 2))
 (fact
   @reported => (just [ (contains {:type :mock-incorrect-call-count
				                           :failures (contains (contains {:position ["t_line_number_reporting.clj" (+ position-2 4)]})) })
			                  pass])))

  (after-silently 
   (fact (g 1) => 1

     ;; quite the gap here.

         (provided
            (f 2) => 2))
   (fact
     @reported => (just [ (contains {:type :mock-incorrect-call-count
                                     :failures (contains (contains {:position ["t_line_number_reporting.clj" (+ position-2 16)]})) })
			  pass])))

(unfinished favorite-animal)
(defn favorite-animal-name [] (name (favorite-animal)))
(defn favorite-animal-empty [] )
(defn favorite-animal-only-animal [] (favorite-animal))
(defn favorite-animal-only-name [] (name "fred"))
(defn favorite-animal-one-call [] (name (favorite-animal 1)))

(deftest unfolding-fakes-examples ;; Use a deftest to check that line numbers work inside.
  (after-silently
    (fact
      (favorite-animal-name) => "betsy"
      (provided
        (name (favorite-animal)) => "betsy"))
   (fact @reported => (just pass)))

  (def line-number 77)
  (after-silently
   (fact
     (favorite-animal-empty) => "betsy"
     (provided
       (name (favorite-animal)) => "betsy"))
   (fact
     @reported => (just [ (contains {:type :mock-incorrect-call-count
				                             :failures (just [(contains {:position ["t_line_number_reporting.clj" (+ line-number 5)]
				                                                        :expected-call "(name ...favorite-animal-value-1...)" })   
                                                      (contains {:expected-call "(favorite-animal)"
                                                                :position ["t_line_number_reporting.clj" (+ line-number 5)]})]
                                                )})
                          (contains {:type :mock-expected-result-failure
				                             :position ["t_line_number_reporting.clj" (+ line-number 3)]})])))

  (def line-number 93)
  (after-silently
   (fact
     (favorite-animal-only-animal) => "betsy"
     (provided
       (name (favorite-animal)) => "betsy"))
   (fact
     @reported => (just [ (contains {:type :mock-incorrect-call-count
				                              :failures (contains (contains {:position ["t_line_number_reporting.clj" (+ line-number 5)]})) })
			                     (contains {:type :mock-expected-result-failure
			                    	          :position ["t_line_number_reporting.clj" (+ line-number 3)]})])))

  (def line-number 105)
  (after-silently
   (fact
     (favorite-animal-only-name) => "betsy"
     (provided
       (name (favorite-animal)) => "betsy"))
   (fact
     @reported => (just [
              ;; This used to produce a :mock-argument-match-failure because of
              ;; (name "fred"). Since the name function actually exists, it's
              ;; used.
			  (contains {:type :mock-incorrect-call-count
                   :failures (just [(contains {:position ["t_line_number_reporting.clj" (+ line-number 5)]
				                                       :expected-call "(name ...favorite-animal-value-1...)"})
                                    (contains {:position ["t_line_number_reporting.clj" (+ line-number 5)]
                                               :expected-call "(favorite-animal)"})]  )})
			  (contains {:type :mock-expected-result-failure
				     :position ["t_line_number_reporting.clj" (+ line-number 3)]})])))


  (def line-number 125)
  (after-silently
   (fact
     (favorite-animal-one-call) => "betsy"
     (provided
       (name (favorite-animal 1)) => "betsy"
       (name (favorite-animal 2)) => "jake")) ;; a folded prerequisite can have two errors.
   (fact
     @reported => (just [(contains {:type :mock-incorrect-call-count
                                    :failures (just [(contains {:position ["t_line_number_reporting.clj" (+ line-number 6)]
				                                                       :expected-call "(name ...favorite-animal-value-2...)"})
                                                     (contains {:position ["t_line_number_reporting.clj" (+ line-number 6)]
                                                                :expected-call "(favorite-animal 2)"})  ])})
                         pass]))))


  (def line-number-separate 141)
(unfinished outermost middlemost innermost)
(in-separate-namespace
(background (outermost) => 2)
(against-background [ (middlemost) => 33]
   (after-silently
    (fact
      (against-background (innermost) => 8)
      (+ (middlemost)
	 (outermost)
	 (innermost)) => 44
	 (+ 1 (middlemost)) => 2)
    (fact
      @reported => (just [ (contains { :position ["t_line_number_reporting.clj" (+ line-number-separate 8)]})
			   (contains { :position ["t_line_number_reporting.clj" (+ line-number-separate 11)]}) ])))))


;; future facts
(after-silently
 (future-fact "text")
 (fact @reported => (just (contains {:position '("t_line_number_reporting.clj" 160)
			                               :description ["text"] }))))

(after-silently
 (pending-fact (+ 1 1) => 2)
 (fact @reported => (just (contains {:position '("t_line_number_reporting.clj" 165)
		                               	:description [nil] }))))


;; Improved error handling for pathological cases

(def line-number-pathological 172)
;; statements without lists guess 1+ most recent"
(after-silently
 (fact 
   1 => even?)
 (fact @reported => (just (contains {:position
                                     ["t_line_number_reporting.clj"
                                      (+ line-number-pathological 4)]}))))

;; That can cause mistakes
(after-silently
 (fact
   
   1 => even?)
 (fact @reported => (just (contains {:position
                                     ["t_line_number_reporting.clj"
                                      (+ line-number-pathological 12)]}))))

;; Facts that have detectable line numbers update the best-guess.
(after-silently
 (fact
   ;; Here, the line numbering goes astray.
   (+ 1 2) => odd?
   1 => even?)
 (fact @reported => (just [pass
                           (contains {:position ["t_line_number_reporting.clj"
                                                 (+ line-number-pathological 23)]})])))


(def facts-position 201)
(after-silently
 (facts "... also use fallback line number"
   1 => even?  


   5 => even?
   (+ 1 2) => odd?
   3 => even?)
 (fact @reported => (just (contains {:position ["t_line_number_reporting.clj"
                                                (+ facts-position 3)]})
                          (contains {:position ["t_line_number_reporting.clj"
                                                (+ facts-position 4)]})
                          pass
                          (contains {:position ["t_line_number_reporting.clj"
                                                (+ facts-position 8)]}))))


;; Line number reporting for variant expect arrows

(def variant-position 221)
(after-silently 
 (fact
   (+ 1 1) =deny=> 2
   (+ 1 1) =not=> 2
   (+ 1 "1") =future=> "2")
 (fact @reported => (just (contains {:position ["t_line_number_reporting.clj"
                                                (+ variant-position 3)]})
                          (contains {:position ["t_line_number_reporting.clj"
                                                (+ variant-position 4)]})
                          (contains {:position ["t_line_number_reporting.clj"
                                                (+ variant-position 5)]}))))


(def tabular-position 235)
(after-silently
 (tabular
  (fact (inc ?n) => ?n)
  ?n  ?comment
  1   "1"
  2   "2")
 (fact "The line number is the line number of the fact, not the substitutions."
   @reported => (just (contains {:position ["t_line_number_reporting.clj"
                                            (+ tabular-position 3)]
                                 :binding-note "[?n 1\n                           ?comment \"1\"]"})
                      (contains {:position ["t_line_number_reporting.clj"
                                            (+ tabular-position 3)]
                                 :binding-note "[?n 2\n                           ?comment \"2\"]"}))))
