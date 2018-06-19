#!/usr/bin/env bash

# Modifies RUNPATH of ejb-app executable to make it able to locate java_binding.so and Rust std lib .so
# EJB App executable must be in the current directory, dynamic libraries in the `lib` subdirectory.

chrpath -v || { echo "No chrpath installed. Aborting"; exit 1; }

# Convert RPATH to RUNPATH
chrpath -c ejb-app

# Modify RUNPATH
chrpath ejb-app -r \$ORIGIN/lib
