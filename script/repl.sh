#!/bin/bash
set -o errexit -o nounset -o pipefail
cd "`dirname $0`/.."

python3 script/build.py
clj -M:dev -m user