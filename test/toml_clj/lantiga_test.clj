(ns toml-clj.lantiga-test
  (:require
    [clojure.string :as str]
    [clojure.test :refer [deftest is are testing] :as test]
    [toml-clj.core :as core])
  (:import
    [me.tonsky.toml_clj ParsingException]
    [java.time LocalTime LocalDate LocalDateTime OffsetDateTime]))

;; Adapted from https://github.com/lantiga/clj-toml

(deftest comment-test
  (testing "Comments"
    (is (= (core/read-string
             #ml "#just a comment line
                  foo = \"bar\" # and one more comment
                  [will] # really
                  work = 1")
          {"foo" "bar" "will" {"work" 1}}))))

(deftest keyvalue-pairs-test
  (testing "Key/value pairs"
    (is (= (core/read-string
             #ml "key = 1
                  bare_key = 2
                  bare-key = 3
                  BARE-KEY = 4")
          {"key" 1 "bare_key" 2 "bare-key" 3 "BARE-KEY" 4}))
    (is (thrown? ParsingException
          (core/read-string #ml "\"\" = 1
                                  '' = 2")))))

(deftest string-test
  (testing "Strings (standard)"
    (is (= (core/read-string "str = \"I'm a string. Name Jos\u00E9 Location SF.\"")
          {"str" "I'm a string. Name Jos\u00E9 Location SF."})))
  (testing "Strings (literal)"
    (is (= (core/read-string "str = 'Comes$as\\is<:>'")
          {"str" "Comes$as\\is<:>"})))
  (testing "Strings (multiline)")
  (testing "Strings (multiline literal)"))

(deftest integer-test
  (testing "Integer numbers"
    (is (= (core/read-string "integer = 3")
          {"integer" 3}))
    (is (= (core/read-string "integer = +3_0")
          {"integer" 30}))
    (is (= (core/read-string "integer = -3_0")
          {"integer" -30})))
  (testing "Integer hexadecimal numbers"
    (is (= (core/read-string "hex = 0x01")
          {"hex" 1}))
    (is (= (core/read-string "hex = 0xFFFFFF")
          {"hex" 16777215}))
    (is (= (core/read-string "hex = 0xDEADBEEF")
          {"hex" 3735928559}))
    (is (= (core/read-string "hex = 0xdead_beef")
          {"hex" 3735928559})))
  (testing "Integer octal numbers"
    (is (= (core/read-string "octal = 0o755")
          {"octal" 493})))
  (testing "Integer binary numbers"
    (is (= (core/read-string "bin = 0b101010")
          {"bin" 42}))))

(deftest float-test
  (testing "Float point numbers"
    (is (= (core/read-string "float = 3.0")
          {"float" 3.0}))
    (is (= (core/read-string "float = -3.0")
          {"float" -3.0}))
    (is (= (core/read-string "float = 3_0.0")
          {"float" 30.0}))
    (is (= (core/read-string "float = 1e1_0")
          {"float" 1e10}))
    (is (Double/isNaN ((core/read-string "float = -nan") "float")))
    (is (Double/isNaN ((core/read-string "float = +nan") "float")))
    (is (Double/isNaN ((core/read-string "float = nan") "float")))
    (is (= (core/read-string "float = +inf")
          {"float" Double/POSITIVE_INFINITY}))
    (is (= (core/read-string "float = -inf")
          {"float" Double/NEGATIVE_INFINITY}))
    (is (= (core/read-string "float = inf")
          {"float" Double/POSITIVE_INFINITY}))))

(deftest bool-test
  (testing "Booleans"
    (is (= (core/read-string
             #ml "truthy = true
                  falsy = false")
          {"truthy" true
           "falsy" false}))))

(deftest datetime-test
  (testing "Offset Date-Time"
    (is (= (core/read-string "odt = 1979-05-27T07:32:00Z")
          {"odt" (OffsetDateTime/parse "1979-05-27T07:32:00Z")}))
    (is (= (core/read-string "odt = 1979-05-27 07:32:00Z")
          {"odt" (OffsetDateTime/parse "1979-05-27T07:32:00Z")})))
  (testing "Local Date-Time"
    (is (= (core/read-string "ldt = 1979-05-27T07:32:00")
          {"ldt" (LocalDateTime/parse "1979-05-27T07:32:00")})))
  (testing "Local Date"
    (is (= (core/read-string "ld = 1979-05-27")
          {"ld" (LocalDate/parse "1979-05-27")})))
  (testing "Local Time"
    (is (= (core/read-string "lt = 00:32:00")
          {"lt" (LocalTime/parse "00:32:00")}))))

(deftest array-test
  (testing "Arrays"
    (is (= (core/read-string 
             #ml "inline = [1, 2, 3] # comment can be here
                  multiline = [4,
                               5, # also comment on arbitrary item is allowed
                               -6]
                  nested = [[7, 8, -9],
                            [\"seven\",
                             \"eight\",
                             \"negative nine\"]]
                  empty = [] # empty array are allowed")
          {"inline" [1 2 3]
           "multiline" [4 5 -6]
           "nested" [[7 8 -9] ["seven" "eight" "negative nine"]]
           "empty" []}))))

(deftest standard-table-test
  (testing "Standard table"
    (is (= (core/read-string
             #ml "[table-1] # comment allowed for table name
                  a = 1
                  [table-2]
                  a = 1")
          {"table-1" {"a" 1}
           "table-2" {"a" 1}})))
  (testing "Standard table (empty keygroups)"
    (is (= (core/read-string
             #ml "[table-1]
                  [table-2]
                  [table-2.sub]
                  [table-3.sub]")
          {"table-1" {}
           "table-2" {"sub" {}}
           "table-3" {"sub" {}}})))
  (testing "Standard table (nested keygroups)"
    (is (= (core/read-string
             #ml "[table-1]
                  a = 1
                  [table-1.sub]
                  b = 1
                  [table-2]
                  a = 1
                  [table-2 .sub  . sub]
                  c = 1")
          {"table-1" {"a" 1 "sub" {"b" 1}}
           "table-2" {"a" 1 "sub" {"sub" {"c" 1}}}}))))

(deftest inline-table-test
  (testing "Inline table"
    (is (= (core/read-string "name = {first = \"Tom\", last = \"Preston-Werner\"}")
          {"name" {"first" "Tom" "last" "Preston-Werner"}}))
    (is (= (core/read-string "point = {x = 1, y = 1, color = {r = 0, g = 0, b = 0}}")
          {"point" {"x" 1 "y" 1 "color" {"r" 0 "g" 0 "b" 0}}}))))

(deftest array-of-tables-test
  (testing "Array of Tables"
    (is (= (core/read-string
             #ml "[[fruit]]
                  name = \"apple\"

                  [fruit.physical]
                  color = \"red\"

                  [[fruit.variety]]
                  name = \"red delicious\"

                  [[fruit.variety]]
                  name = \"granny smith\"

                  [[fruit]]
                  name = \"banana\"

                  [[fruit.variety]]
                  name = \"plantain\"")
          {"fruit" [{"name"     "apple"
                     "physical" {"color" "red"}
                     "variety"  [{"name" "red delicious"}
                                 {"name" "granny smith"}]}
                    {"name"    "banana"
                     "variety" [{"name" "plantain"}]}]}))))

;; TODO: add tests showing that clj-toml is a superset of TOML

(deftest example-test
  (testing "TOML example"
    (is (= (core/read-string (slurp "test/examples/example.toml"))
          {"title" "TOML Example"
           "owner"
           {"name" "Tom Preston-Werner"
            "organization" "GitHub"
            "bio" "GitHub Cofounder & CEO\nLikes tater tots and beer."
            "dob" (OffsetDateTime/parse "1979-05-27T07:32:00Z")}
           "database"
           {"enabled" true,
            "connection_max" 5000,
            "server" "192.168.1.1",
            "ports" [8001 8001 8002]}
           "servers"
           {"alpha"
            {"ip" "10.0.0.1"
             "dc" "eqdc10"}
            "beta"
            {"ip" "10.0.0.2"
             "dc" "eqdc10"
             "country" "中国"}}
           "clients"
           {"data" [["gamma" "delta"] [1 2]]
            "hosts" ["alpha" "omega"]}
           "products"
           [{"name" "Hammer"
             "sku" 738594937}
            {"name" "Nail"
             "sku" 284758393
             "color" "gray"}]}))))

(deftest hard-example-test
  (testing "TOML hard example"
    (is (= (core/read-string (slurp "test/examples/hard_example.toml"))
          {"the"
           {"hard"
            {"another_test_string" " Same thing, but with a string #",
             "test_array2"
             ["Test #11 ]proved that" "Experiment #9 was a success"],
             "test_array" ["] " " # "],
             "bit#"
             {"what?" "You don't think some user won't do that?",
              "multi_line_array" ["]"]},
             "harder_test_string"
             " And when \"'s are in the string, along with # \""},
            "test_string" "You'll hate me after this - #"}}))))

(deftest example-v0.4.0-test
  (testing "TOML 0.4.0 example"
    (is (= {"table"
            {"key" "value"
             "subtable" {"key" "another value"}
             "inline" {"name" {"first" "Tom", "last" "Preston-Werner"}
                       "point" {"x" 1, "y" 2}}}
            "x" {"y" {"z" {"w" {}}}}
            "string"
            {"basic"
             {"basic" "I'm a string. \"You can quote me\". Name\tJos\u00E9\nLocation\tSF."}
             "multiline"
             {"key1" "One\nTwo"
              "key2" "One\nTwo"
              "key3" "One\nTwo"
              "continued"
              {"key1" "The quick brown fox jumps over the lazy dog."
               "key2" "The quick brown fox jumps over the lazy dog."
               "key3" "The quick brown fox jumps over the lazy dog."}}
             "literal"
             {"winpath" "C:\\Users\\nodejs\\templates"
              "winpath2" "\\\\ServerX\\admin$\\system32\\"
              "quoted" "Tom \"Dubs\" Preston-Werner"
              "regex" "<\\i\\c*\\s*>"
              "multiline"
              {"regex2" "I [dw]on't need \\d{2} apples"
               "lines" "The first newline is\ntrimmed in raw strings.\n   All other whitespace\n   is preserved.\n"}}}
            "integer"
            {"key1" 99
             "key2" 42
             "key3" 0
             "key4" -17
             "underscores"
             {"key1" 1000
              "key2" 5349221
              "key3" 12345}}
            "float"
            {"fractional"
             {"key1" 1.0
              "key2" 3.1415
              "key3" -0.01}
             "exponent"
             {"key1" 5e+22
              "key2" 1e6
              "key3" -2E-2}
             "both"
             {"key" 6.626e-34}
             "underscores"
             {"key1" 9224617.445991228313
              "key2" 1e1000}}
            "boolean"
            {"True" true
             "False" false}
            "datetime"
            {"key1" (OffsetDateTime/parse "1979-05-27T07:32:00Z")
             "key2" (OffsetDateTime/parse "1979-05-27T00:32:00-07:00")
             "key3" (OffsetDateTime/parse "1979-05-27T00:32:00.999999-07:00")}
            "array"
            {"key1" [1 2 3]
             "key2" ["red" "yellow" "green"]
             "key3" [[1 2] [3 4 5]]
             "key4" [[1 2] ["a" "b" "c"]]
             "key5" [1 2 3]
             "key6" [1 2]}
            "products"
            [{"name" "Hammer" "sku" 738594937}
             {}
             {"name" "Neil" "sku" 284758393 "color" "gray"}]
            "fruit"
            [{"name" "apple"
              "physical" {"color" "red" "shape" "round"}
              "variety" [{"name" "red delicious"}
                         {"name" "granny smith"}]}
             {"name" "banana"
              "variety" [{"name" "plantain"}]}]}
          (core/read-string (slurp "test/examples/example-v0.4.0.toml"))))))
