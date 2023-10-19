#! /usr/bin/env python3
import argparse, build_utils, functools, os, re

basedir = os.path.abspath(os.path.dirname(__file__) + '/..')
version = build_utils.get_arg("version") or build_utils.parse_ref() or build_utils.parse_sha() or "0.0.0-SNAPSHOT"
clojars = "https://repo.clojars.org"

@functools.lru_cache(maxsize=1)
def deps():
  deps = [
    build_utils.fetch_maven("org.clojure", "clojure", "1.11.1"),
    build_utils.fetch_maven("org.clojure", "core.specs.alpha", "0.2.62"),
    build_utils.fetch_maven("org.clojure", "spec.alpha", "0.3.218"),
  ]

  return deps
