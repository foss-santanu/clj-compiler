(ns san.lexer.re-lexer
   "Package to build a lexer from a lexer specification.
    Inspired by the tools like flex, JLex, etc."
   (:require [clojure.string :as string]
             [clojure.set :as set] :reload))

;;; A good example of using Clojure Regex to build a Lexer
;;; http://scottmking.ca/2013/01/13/simple-lexical-analysis-in-clojure/

(def ^:private newline-regex #"\r?\n|\r|[\u0085\u2028\u2029]")

(def ^:private standard-actions {
        :ignore nil
        :string-to-number (fn [lexer lexem]
                              (if (re-matches #"\d+|\d+\.?\d{1,}" lexem)
                                  [lexer (read-string lexem)]))
     })

;;; An "action" is a function that takes the lexer and lexem as parameters
;;; and returns a value for the token being matched. This "action" may
;;; change the "context" of subsequent lexem matching and the "state" of lexer.
;;; Hence "action" returns a pair of modified lexer and value for the token.
(defrecord ^:private lexem-matcher
   [name type regex action states])

(defn- make-matcher
   "An utility function to instantiate lexem-matcher"
   [{:keys [name type regex action states] :or {states #{:initial}}}]
   (lexem-matcher. name type regex action states))

(defrecord ^:private lexer-t
   [declarations lexem-specs state context])

(defn build-lexer
   "Function to build a lexer from its specifications"
   [{{:keys [directives regexes states] :as declarations} :declarations
     lex-rules :lex-rules :as lex-spec}]
   (let [ expand-regex (fn [regex [reg-name reg-val]]
                           (re-pattern (string/replace (str regex)
                                                       (str reg-name)
                                                       (str reg-val))
                           ))
          start-state (let [ init (:start-state directives) ]
                           (if (and init (contains? (set states) init)) init :initial))
          lexer-states (if (= start-state :initial)
                           (set/union (set states) #{:initial})
                           (set states))
          lex-matcher (fn [{:keys [states name type regex action] :as lex-rule}]
                          (let [ states-valid (or (nil? states)
                                                  (set/subset? (set states) lexer-states))
                                 exp-regex (reduce expand-regex regex (seq regexes))
                               ]
                               (when states-valid
                                     (make-matcher {:name name
                                                    :type type
                                                    :regex exp-regex
                                                    :action action
                                                    :states (if states (set states) #{:initial})}))
                          ))
          setup-context (fn [context {:keys [name init-value]}]
                            (assoc context name init-value))
        ]
        (lexer-t. (assoc declarations :states lexer-states)
                  (map lex-matcher lex-rules)
                  start-state
                  (reduce setup-context {:char-count 0 :line-count 0 :errors []}
                                        (:global-vars directives)))
   ))

(defn- string-splitter
   "Takes a list of lexem-matchers as parameter.
    Returns a regex to split a string into a sequence of lexems."
   [lexem-matchers]
   (let [ regexs (map :regex lexem-matchers)
          re-string (string/join "|" (map str regexs))
        ]
        (re-pattern re-string)))

(defn- lexem-seq
   "Splits a string into a sequence of lexems"
   [lexer str-to-parse]
   (re-seq (string-splitter (:lexem-specs lexer)) str-to-parse))

;;; Small uttility function to escape a string
;;; so that it can be used as a regular expression.
;;; Ref: http://stackoverflow.com/questions/28342570/how-to-split-a-string-in-clojure-not-in-regular-expression-mode
(defn- escape-char
   [c]
   (if ((set "<([{\\^-=$!|]})?*+.>") c)
       (str "\\" c)
       c))
(defn- escape-re
   [re-str]
   (apply str (map escape-char (seq re-str))))

(defn- check-for-error
   "Checks if unmatched string exists between the start of
    input string and the first index of lexem in the string."
   [lexer lexem input-string]
   (let [ [error-str rest-input] (if (and lexem input-string)
                                     (subvec (re-find (re-pattern (str "(?s)(.*?)"
                                                                       (escape-re lexem) "(.*)"))
                                                      input-string) 1)
                                     (when input-string [input-string nil]))
          char-count (get-in lexer [:context :char-count])
          line-count (get-in lexer [:context :line-count])
          updated-lexer (if (> (count error-str) 0)
                            (-> lexer
                                (update-in [:context :errors]
                                           conj {:str-not-matched error-str
                                                 :at-char char-count :at-line line-count})
                                (update-in [:context :char-count] + (count error-str))
                                (update-in [:context :line-count] + (count (re-seq newline-regex
                                                                                   error-str))))
                            lexer)
        ]
        [updated-lexer (if (> (count error-str) 0) rest-input input-string)]))

(defn- update-char-line-count
   [lexer lexem input-string]
   (let [ [error-str rest-input] (if (and lexem input-string)
                                     (subvec (re-find (re-pattern (str "(?s)(.*?)"
                                                                       (escape-re lexem) "(.*)"))
                                                      input-string) 1)
                                     (when input-string [input-string nil]))
          updated-lexer (if (> (count error-str) 0)
                            (throw (Exception. (str "Unexpected input string " input-string
                                                      " for lexem " lexem)))
                            (if lexem
                                (-> lexer
                                    (update-in [:context :char-count] + (count lexem))
                                    (update-in [:context :line-count] + (count (re-seq newline-regex
                                                                                       lexem)))
                                )
                                lexer))
        ]
        [updated-lexer (if lexem rest-input input-string)]))

(defn- mk-token
   "Creates a token for currently matched lexem.
    A token is a map with three keys: name, type and value"
   [[lexer lexems string-to-parse]]
   (let [ lex-rules (:lexem-specs lexer)
          lexem (first lexems)
          rest-of-lexems (next lexems)
          [updt-lexer input-string] (check-for-error lexer lexem string-to-parse)
          match? (fn [re-str]
                     (re-matches (re-pattern (str re-str)) lexem))
          tk-builder (fn [{:keys [name type regex action states]}]
                         (let [ valid-state (contains? states (:state updt-lexer))
                                fn-action (if (nil? action)
                                              (fn [x] [updt-lexer x])
                                              (if (keyword? action)
                                                  (let [ stnd-action (action standard-actions) ]
                                                       (when stnd-action
                                                             (partial stnd-action updt-lexer)))
                                                  (partial action updt-lexer)))
                              ]
                              (when (and valid-state (match? regex))
                                    (let [ char-count (get-in updt-lexer [:context :char-count]) 
                                           line-count (get-in updt-lexer [:context :line-count])
                                           show-char-indx (get-in updt-lexer
                                                          [:declarations :directives :show-char-index])
                                           show-line-indx (get-in updt-lexer
                                                          [:declarations :directives :show-line-index])
                                           [lexer tk-value] (if fn-action 
                                                                (fn-action lexem)
                                                                [updt-lexer :ignored])
                                           [mod-lexer mod-input-str] (update-char-line-count 
                                                                             lexer lexem input-string)
                                         ]                                      
                                         (if (= tk-value :ignored)
                                             [mod-lexer rest-of-lexems mod-input-str nil]
                                             (let [ token (cond
                                                                 (and show-char-indx show-line-indx)
                                                                 {:name (if name name lexem) :type type
                                                                  :value tk-value :char-indx char-count
                                                                  :line-indx line-count}
                                                                 show-char-indx
                                                                 {:name (if name name lexem) :type type
                                                                  :value tk-value :char-indx char-count}
                                                                 show-line-indx
                                                                 {:name (if name name lexem) :type type
                                                                  :value tk-value :line-indx line-count}
                                                                 :else
                                                                 {:name (if name name lexem) :type type
                                                                  :value tk-value})
                                                  ]
                                                  [mod-lexer rest-of-lexems mod-input-str token]))
                                    ))
                         ))
        ]
        (if (and (nil? lexem) (nil? rest-of-lexems))
            [updt-lexer rest-of-lexems input-string :eof]
            (some tk-builder lex-rules))
        ))

(defn- tokenize-str
   "Returns a lazy sequence of tokens for a parse string"
   [lexer string-to-parse]
   (let [ lexems (lexem-seq lexer string-to-parse)
          tokenizer (filter #(not (nil? (% 3)))
                            (iterate mk-token [lexer lexems string-to-parse nil]))
        ]
        (map (fn [x] [(x 0) (x 3)]) tokenizer))
   )

(defprotocol lexer
   "Defines an API to access the lexical analyser"
   (tokenize-text [this text-to-parse]
                  "Reads in a text and transforms it into a sequence of tokens")
   (tokenize-file [this file-to-parse]
                  "Reads text from a file and builds the sequence of tokens")
   (lexer-errors [this] "Returns a list of lexical errors")
   (count-of-chars [this] "Returns count of characters read so far")
   (count-of-lines [this] "Returns the number of lines read so far"))

(extend-type lexer-t
   lexer
        (tokenize-text
           [this text-to-parse]
           (tokenize-str this text-to-parse))
        (tokenize-file
           [this file-to-parse]
           (tokenize-str this (slurp file-to-parse)))
        (lexer-errors
           [this]
           (-> this
               (get-in [:context :errors])
               rseq
               vec))
        (count-of-chars
           [this]
           (get-in this [:context :char-count]))
        (count-of-lines
           [this]
           (get-in this [:context :line-count]))
   )
