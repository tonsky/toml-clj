(ns toml-clj.core
  (:refer-clojure :exclude [read read-string])
  (:import
    [java.io StringReader]
    [me.tonsky.toml_clj TomlParser]))

(defn read
  "Reads TOML file from java.io.Reader.
   
   Possible opts:
   
   :key-fn - function to convert keys from strings, e.g. `keyword`"
  ([]
   (read *in* {}))
  ([stream]
   (read stream {}))
  ([stream opts]
   (let [parser (TomlParser.)]
     (when-some [key-fn (:key-fn opts)]
       (set! (.-keyFn parser) key-fn))
     (.parse parser stream))))

(defn read-string
  "Reads TOML file from String.
   
   Possible opts:
   
   :key-fn - function to convert keys from strings, e.g. `keyword`"
  ([s]
   (read (StringReader. s) {}))
  ([s opts]
   (read (StringReader. s) opts)))
