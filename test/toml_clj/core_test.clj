(ns toml-clj.core-test
  (:require
    [clojure.string :as str]
    [clojure.pprint :refer [pprint]]
    [clojure.test :refer [deftest is are testing] :as test]
    [lambdaisland.deep-diff2 :as ddiff]
    [toml-clj.core :as core])
  (:import
    [clojure.lang IPersistentMap IPersistentVector]
    [java.time OffsetDateTime]))

(defn reindent ^String [s indent]
  (let [lines    (str/split-lines s)
        butfirst (->> lines
                   next
                   (remove str/blank?))]
    (if (seq butfirst)
      (let [prefix (->> butfirst
                     (map #(count (second (re-matches #"( *).*" %))))
                     (reduce min))]
        (str/join "\n"
          (cons
            (str indent (first lines))
            (map #(if (str/blank? %) "" (str indent (subs % prefix))) (next lines)))))
      s)))

(defn ml [s]
  (assert (string? s))
  (reindent s ""))

(deftest test-core
  ; (alter-var-root #'clojure.pprint/*print-right-margin* (constantly 180))
  (is (= {"key" "value"
          "key2" "value2"}
        (core/read-string
          #ml "key = \"value\"
               key2 = \"value2\"")))
  
  (is (= {"table" {"key" "value"}
          "table2" {"key2" "value2"}}
        (core/read-string
          #ml "[table]
               key = \"value\"
               [table2]
               key2 = \"value2\"")))
  
  (is (= {"talk" [{"key" "value"
                   "version" [{"key2" "value2"}
                              {"key3" "value3"}]}
                  {"key4" "value4"
                   "version" [{"key5" "value5"}
                              {"key6" "value6"}]}]}
        (core/read-string
          #ml "[[talk]]
               key = \"value\"
               [[talk.version]]
               key2 = \"value2\"
               [[talk.version]]
               key3 = \"value3\"
               [[talk]]
               key4 = \"value4\"
               [[talk.version]]
               key5 = \"value5\"
               [[talk.version]]
               key6 = \"value6\""))))

(deftest test-keyword
  (is (= {:root-key 0
          :table {:key1 1
                  :inner {:key2 2}}
          :array [{:key3 3}]}
        (core/read-string
          #ml "root-key = 0
               [table]
               key1 = 1
               [table.inner]
               key2 = 2
               [[array]]
               key3 = 3" {:key-fn keyword}))))

(deftest test-types
  (is (instance? IPersistentVector (-> (core/read-string "x = [1, 2]" {:key-fn keyword}) :x)))
  (is (instance? IPersistentVector (-> (core/read-string "x = [[1, 2], [3, 4]]" {:key-fn keyword}) :x)))
  (is (instance? IPersistentVector (-> (core/read-string "x = [[1, 2], [3, 4]]" {:key-fn keyword}) :x first)))
  (is (instance? IPersistentMap (core/read-string "x = 1")))
  (is (instance? IPersistentMap (core/read-string "[table]\nx = 1")))
  (is (instance? IPersistentMap (-> (core/read-string "[table]\nx = 1" {:key-fn keyword}) :table)))
  (is (instance? IPersistentVector (-> (core/read-string "[[array]]\nx = 1" {:key-fn keyword}) :array)))
  (is (instance? IPersistentMap (-> (core/read-string "[[array]]\nx = 1" {:key-fn keyword}) :array first))))

(deftest test-v0.5.0
  (is (= {"x" {"y" {"z" 123}}}
        (core/read-string "x.y.z = 123")))
  (is (= {"x" 16, "y" 8, "z" 2}
        (core/read-string "x = 0x10
                            y = 0o10
                            z = 0b10")))
  (is (= {"dt" (OffsetDateTime/parse "1979-05-27T07:32:00-08:00")}
        (core/read-string "dt = 1979-05-27 07:32:00-08:00")))
  (is (= {"s" "first><second"}
        (core/read-string "s = \"\"\"first>\\  \n<second\"\"\""))))

(def color-printer
  (ddiff/printer
    {:color-scheme
     {:delimiter nil ; [:bold :red]
      :tag       nil ; [:red]
      :nil       nil ; [:bold :black]
      :boolean   nil ; [:green]
      :number    nil ; [:cyan]
      :string    nil ; [:bold :magenta]
      :character nil ; [:bold :magenta]
      :keyword   nil ; [:bold :yellow]
      :symbol    nil
      :function-symbol nil ; [:bold :blue]
      :class-delimiter nil ; [:blue]
      :class-name      nil ; [:bold :blue]

      :lambdaisland.deep-diff2.printer-impl/insertion [:green]
      :lambdaisland.deep-diff2.printer-impl/deletion  [:red]
      :lambdaisland.deep-diff2.printer-impl/other     [:yellow]}}))

(defmethod test/report :fail [m]
  (test/with-test-out
    (test/inc-report-counter :fail)
    (println "\nFAIL in" (test/testing-vars-str m))
    (when (seq test/*testing-contexts*) (println (test/testing-contexts-str)))
    (when-some [message (:message m)]
      (println message))
    (if (= '= (first (:expected m)))
      (let [[_ [_ expected actual]] (:actual m)]
        (-> (ddiff/diff expected actual)
          ; (ddiff/minimize)
          (ddiff/pretty-print color-printer)))
      (do
        (println "expected:" (pr-str (:expected m)))
        (println "  actual:" (pr-str (:actual m)))))))

