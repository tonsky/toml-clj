(defproject io.github.tonsky/toml-clj "0.0.0"
  :description "Fast TOML parser"
  :license     {:name "MIT" :url "https://github.com/tonsky/toml-clj/blob/master/LICENSE"}
  :url         "https://github.com/tonsky/toml-clj"
  :dependencies
  [[org.clojure/clojure "1.11.1"]]
  :deploy-repositories
  {"clojars"
   {:url "https://clojars.org/repo"
    :username "tonsky"
    :password :env/clojars_token
    :sign-releases false}})