(ns lexers.lexer1 
   "A simple lexical analyser for arithmetic expressions"
   (:use san.lexer.core :reload))

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
                        {:name :assign :type :oprator :regex #"="}
                        {:name :number :type :constant :regex #"\d*?\.?\d+" :action :string-to-number}
                        {:type :variable :regex #":namechars+?(?::namechars|[0-9])*"}
                        {:type :whitespace :regex #":spaces|:newline" :action :ignore}
                     ]
                  }
     ))

(take-while #(not= % :eof) (map #(% 1) (tokenize-text expr-lexer "a1 + 56")))
(take-while #(not= % :eof) (map #(% 1) (tokenize-text expr-lexer "_xx=345+yy")))