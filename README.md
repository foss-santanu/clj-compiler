# clj-compiler

# Regular Expression-Based Lexer for Clojure

A **customizable lexer engine** implemented in Clojure, inspired by classic tools like `flex`, `JLex`, and `ANTLR`. It uses **regular expressions and user-defined actions** to tokenize input strings or files into structured tokens.

This lexer is part of the [`clj-compiler`](https://github.com/foss-santanu/clj-compiler) project and is intended for building lexers in a functional, extensible way.

---

## ✨ Highlights

- ⚙️ Written in idiomatic Clojure using records, protocols, and higher-order functions.
- 📜 Declarative lexer specification using data structures.
- 🔎 Uses regular expressions with symbolic expansion (macro-like regexes).
- 🧪 Fully tested with examples and assertions.
- 🧩 Easily extendable to support language-specific lexing or REPL-based parsing.

---

## ✨ Features

- Define lexical grammar declaratively using Clojure maps
- Supports macros for reusable regex fragments
- Named tokens with types, actions, and contextual metadata
- Line and character tracking
- Token transformation hooks
- Skipping/ignoring whitespace or irrelevant tokens
- Lazy sequence of tokens from string or file

---

## 📁 Project Structure

```
clj-compiler/
├── src/
│   └── main/
│       └── clojure/
│           └── san/
│               └── lexer/
│                   └── re_lexer.clj      ; Core lexer implementation
├── examples/
│   └── lexers/
│       └── lexer1.clj                    ; Example lexer spec and usage
├── test/                                 ; (Optional) Add tests here
├── README.md                             ; Project documentation
├── deps.edn or project.clj               ; Dependency and build configuration
```

---

## 🌐 Namespace

```clojure
(ns san.lexer.re-lexer)
```

---

## 📑 Defining a Lexer

Use the `build-lexer` function to create a lexer from a spec:

```clojure
(def expr-lexer
  (build-lexer
    {:declarations {
      :directives {
        :show-char-index true
        :show-line-index true
      }
      :regexes {
        :spaces     #"[ \t\x0B\f]+"
        :newline    #"\r?\n|\r|[\u0085\u2028\u2029]"
        :namechars  #"[a-zA-Z_]"
      }}
     :lex-rules [
       {:name :plus     :type :operator :regex #\+"}
       {:name :minus    :type :operator :regex #"-"}
       {:name :multiply :type :operator :regex #"\*"}
       {:name :divide   :type :operator :regex #"/"}
       {:name :assign   :type :operator :regex #"="}
       {:name :number   :type :constant :regex #"\d*?\.?\d+" :action :string-to-number}
       {:type :variable :regex #:namechars+?(?::namechars|[0-9])*}
       {:type :whitespace :regex #:spaces|:newline :action :ignore}
     ]}))
```

---

## ⚖️ Tokenizing Input

```clojure
(tokenize-text expr-lexer "a1 + 56")
;; => ({:name "a1", :type :variable, :value "a1", :char-indx 0, :line-indx 0}
;;     {:name :plus, :type :operator, :value "+", :char-indx 3, :line-indx 0}
;;     {:name :number, :type :constant, :value 56, :char-indx 5, :line-indx 0})
```

### Available Functions (via protocol)

| Function            | Description                             |
|---------------------|-----------------------------------------|
| `tokenize-text`     | Tokenizes a string                      |
| `tokenize-file`     | Tokenizes file contents                 |
| `lexer-errors`      | Reports unmatched fragments             |
| `count-of-chars`    | Tracks character count                  |
| `count-of-lines`    | Tracks line count                       |

---

## 🔁 Token Processing Flow

```text
  +---------------------------+
  |        Input Text        |
  +-----------+--------------+
              |
              v
    [ Combined Regex Pattern ]  <= built from all lexem rules
              |
              v
     re-seq splits input into lexems
              |
              v
    +------------------------------+
    |      For each Lexem:         |
    |   1. Match with rule         |
    |   2. Check lexer state       |
    |   3. Apply action (if any)   |
    |   4. Create token            |
    +------------------------------+
              |
              v
       Collect tokens and metadata
```

---

## 📊 Strengths

- **Composable**: Regex macros promote reuse
- **Action Hooks**: Supports runtime token transformation
- **Context-aware**: Tracks char/line index for precise error reporting
- **Extensible**: State machine and multiple modes possible
- **Tested**: Comes with assertions in `lexer1.clj` showing usage

---

## ⚙️ Future Extensions

- Support **stateful lexing** (mode switching like `YYSTATE` in Flex)
- Add **token position range** (`:start-pos`, `:end-pos`)
- Emit **diagnostics** with rich error categories
- Add **parser integration hooks** (for LL(1), PEG etc.)
- **Multilingual lexer DSLs** using YAML/EDN input
- Performance enhancements (e.g., regex grouping optimization)

---

## ⚖️ License

This project is distributed under the GPL-2.0 License.

---

## 🎓 Author

Developed by [Santanu Chakrabarti](https://github.com/foss-santanu) as part of a broader Clojure-based compiler toolkit.
