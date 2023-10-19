#!/bin/bash
set -o errexit -o nounset -o pipefail
cd "`dirname $0`/.."

python3 script/build.py
clojure -A:dev -X user/-test