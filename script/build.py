#! /usr/bin/env python3
import argparse, build_utils, common, os, platform, subprocess, sys

def main():
  os.chdir(common.basedir)
  sources = build_utils.files("src-java/**/*.java", )
  build_utils.javac(sources, "target/classes", classpath=common.deps())

if __name__ == '__main__':
  sys.exit(main())
