# Fast TOML parser for Clojure

toml-clj is fast TOML parser for JVM Clojure that supports latest TOML spec (1.0.0).

## Using

Add this to deps.edn:

```
io.github.tonsky/toml-clj {:mvn/version "0.1.0"}
```

Require:

```
(require '[toml-clj.core :as toml])
```

Parse strings:

```
(toml/read-string "x = 1")
; => {"x" 1}
```

Convert keys to keywords:

```
(toml/read-string "[section]\nx = 1" {:key-fn keyword})
; => {:section {:x 1}}
```

Parse streams:

```
(with-open [rdr (clojure.java.io/reader "test/test.toml")]
  (toml/read rdr))
; => {"table" {"key" ...
```

## Performance

Quick testing on 25 Kb TOML file on Macbook Pro M1:

| Library                 | Execution time mean |
| ----------------------- | ------------------- |
| toml-clj (this library) | 0.274 ms            |
| [lantiga/clj-toml](https://github.com/lantiga/clj-toml) v0.3.1 | 603.536 ms |
| [vmfhrmfoaj/clj-toml](https://github.com/vmfhrmfoaj/clj-toml) | 594.234 ms |
| [tomlj](https://github.com/tomlj/tomlj) (Java) | 5.6 ms |
| clojure.edn (EDN) | 0.9 ms |
| [cheshire](https://github.com/dakrone/cheshire) (JSON) | 0.057 ms |

## Credits

Parser code is adopted from https://github.com/TheElectronWill/Night-Config and changed to make it work with Clojure.

Tests are adopted from https://github.com/lantiga/clj-toml.

## License

Copyright © 2023 Nikita Prokopov

Licensed under [LGPL version 3](LICENSE).
