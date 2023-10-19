(ns toml-clj.bench
  (:require
    [cheshire.core :as json]
    [clojure.edn :as edn]
    [criterium.core :as criterium]
    [toml-clj.core :as core]
    [clj-toml.core :as clj-toml])
  (:import
    [org.tomlj Toml]))

(defn -main [& {:as args}]
  (let [file (get args "--file" "bench/examples/talks.toml")
        s    (slurp file)]
    (println "[ --- toml-clj --- ]")
    (when (not= 53 (-> (core/read-string s) (get "talk") count))
      (throw (ex-info "Parse error" (core/read-string s))))
    (criterium/quick-bench
      (core/read-string s))
    
    (println "\n[ --- clj-toml --- ]")
    (when (not= 53 (-> (clj-toml/parse-string s) (get "talk") count))
      (throw (ex-info "Parse error" (clj-toml/parse-string s))))
    (criterium/quick-bench
      (clj-toml/parse-string s))
    
    (println "\n[ --- tomlj --- ]")
    (criterium/quick-bench
      (Toml/parse ^String s))
    
    (println "\n[ --- EDN --- ]")
    (let [s (slurp "bench/examples/talks.edn")]
      (when (not= 53 (-> (edn/read-string s) (get "talk") count))
        (throw (ex-info "Parse error" (edn/read-string s))))
      (criterium/quick-bench
        (edn/read-string s)))
    
    (println "\n[ --- JSON --- ]")
    (let [s (slurp "bench/examples/talks.json")]
      (when (not= 53 (-> (json/parse-string s) (get "talk") count))
        (throw (ex-info "Parse error" (json/parse-string s))))
      (criterium/quick-bench
        (json/parse-string s)))
    ))

(comment
  (def s (slurp "bench/examples/talks.toml"))
  
  ;; generate edn
  (defmethod print-method java.time.LocalDate [c, ^java.io.Writer w]
    (.write w "#inst \"")
    (.write w (.format (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd") c))
    (.write w "\""))

  (spit "bench/examples/talks.edn" (pr-str (core/read-string s)))
  (edn/read-string (slurp "bench/examples/talks.edn"))
  
  ;; generate json
  (cheshire.generate/add-encoder java.time.LocalDate
   (fn [c jsonGenerator]
     (.writeString jsonGenerator (.format (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd") c))))
  
  (spit "bench/examples/talks.json" (json/generate-string (core/read-string s)))
  (json/parse-string (slurp "bench/examples/talks.json")))
