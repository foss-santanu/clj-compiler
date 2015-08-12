(ns lexers.lexer1 
   "A simple lexical analyser for arithmetic expressions"
   (:use san.lexer.re-lexer :reload))

(def expr-lexer 
     (build-lexer { 
                     :declarations { 
                        :directives { 
                           :show-char-index true
                           :show-line-index true
                        }
                        :regexes { 
                           :spaces #"[ \t\x0B\f]+"
                           :newline #"\r?\n|\r|[\u0085\u2028\u2029]"
                           :namechars #"[a-zA-Z_]"
                        }
                     }
                     
                     :lex-rules [ 
                        {:name :plus :type :operator :regex #"\+"}
                        {:name :minus :type :operator :regex #"-"}
                        {:name :multiply :type :operator :regex #"\*"}
                        {:name :divide :type :operator :regex #"/"}
                        {:name :assign :type :operator :regex #"="}
                        {:name :number :type :constant :regex #"\d*?\.?\d+" :action :string-to-number}
                        {:type :variable :regex #":namechars+?(?::namechars|[0-9])*"}
                        {:type :whitespace :regex #":spaces|:newline" :action :ignore}
                     ]
                  }
     ))

;;; 1st test case
(let [ tokens (take-while #(not= % :eof) 
                          (map #(% 1) (tokenize-text expr-lexer "a1 + 56")))
       no-of-tokens (count tokens)
       token-1st (nth tokens 0)
       token-2nd (nth tokens 1)
       token-3rd (nth tokens 2) 
     ]
     (assert (= no-of-tokens 3) "Expression 'a1 + 56' has three valid tokens.")
     (assert (= (:name token-1st) "a1") "For 'a1 + 56' first token is 'a1'.")
     (assert (= (:type token-1st) :variable) "For 'a1 + 56' first token is a variable.")
     (assert (= (:char-indx token-1st) 0) "For 'a1 + 56' character index of first token is 0.")
     (assert (= (:line-indx token-1st) 0) "For 'a1 + 56' line index of first token is 0.")
     (assert (= (:name token-2nd) :plus) "For 'a1 + 56' second token is :plus.")
     (assert (= (:type token-2nd) :operator) "For 'a1 + 56' second token is an operator.")
     (assert (= (:char-indx token-2nd) 3) "For 'a1 + 56' character index of second token is 3.")
     (assert (= (:line-indx token-2nd) 0) "For 'a1 + 56' line index of second token is 0.")
     (assert (= (:name token-3rd) :number) "For 'a1 + 56' third token is a number.")
     (assert (= (:type token-3rd) :constant) "For 'a1 + 56' third token is a constant.")
     (assert (= (:value token-3rd) 56) "For 'a1 + 56' third token has a value 56.")
     (assert (= (:char-indx token-3rd) 5) "For 'a1 + 56' character index of third token is 5.")
     (assert (= (:line-indx token-3rd) 0) "For 'a1 + 56' line index of third token is 0."))

;;; 2nd test case
(let [ tokens (take-while #(not= % :eof) 
              (map #(% 1) (tokenize-text expr-lexer "_xx=345+yy")))
       no-of-tokens (count tokens)
       token-1st (nth tokens 0)
       token-2nd (nth tokens 1)
       token-3rd (nth tokens 2)
       token-4th (nth tokens 3)
       token-5th (nth tokens 4)
     ]
     (assert (= no-of-tokens 5) "Expression '_xx=345+yy' has five valid tokens.")
     (assert (= (:name token-1st) "_xx") "For '_xx=345+yy' first token is '_xx'.")
     (assert (= (:type token-1st) :variable) "For '_xx=345+yy' first token is a variable.")
     (assert (= (:char-indx token-1st) 0) "For '_xx=345+yy' character index of first token is 0.")
     (assert (= (:line-indx token-1st) 0) "For '_xx=345+yy' line index of first token is 0.")
     (assert (= (:name token-2nd) :assign) "For '_xx=345+yy' second token is :assign.")
     (assert (= (:type token-2nd) :operator) "For '_xx=345+yy' second token is an operator.")
     (assert (= (:char-indx token-2nd) 3) "For '_xx=345+yy' character index of second token is 3.")
     (assert (= (:line-indx token-2nd) 0) "For '_xx=345+yy' line index of second token is 0.")
     (assert (= (:name token-3rd) :number) "For '_xx=345+yy' third token is a number.")
     (assert (= (:type token-3rd) :constant) "For '_xx=345+yy' third token is a constant.")
     (assert (= (:value token-3rd) 345) "For '_xx=345+yy' third token has a value 345.")
     (assert (= (:char-indx token-3rd) 4) "For '_xx=345+yy' character index of third token is 4.")
     (assert (= (:line-indx token-3rd) 0) "For 'a1 + 56' line index of third token is 0.")
     (assert (= (:name token-4th) :plus) "For '_xx=345+yy' fourth token is :plus.")
     (assert (= (:type token-4th) :operator) "For '_xx=345+yy' fourth token is an operator.")
     (assert (= (:char-indx token-4th) 7) "For '_xx=345+yy' character index of fourth token is 7.")
     (assert (= (:line-indx token-4th) 0) "For '_xx=345+yy' line index of fourth token is 0.")
     (assert (= (:name token-5th) "yy") "For '_xx=345+yy' fifth token is 'yy'.")
     (assert (= (:type token-5th) :variable) "For '_xx=345+yy' fifth token is a variable.")
     (assert (= (:char-indx token-5th) 8) "For '_xx=345+yy' character index of fifth token is 8.")
     (assert (= (:line-indx token-5th) 0) "For '_xx=345+yy' line index of fifth token is 0."))

;;; 3rd test case
(let [ tokens (take-while #(not= % :eof) 
              (map #(% 1) (tokenize-text expr-lexer "_xx=345\n\nyy= _xx * de4"))) 
     ]
     tokens)