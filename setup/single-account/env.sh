#!/usr/bin/env bash
# Source this file in every new terminal used for the one-account lab.
# It changes only the current shell; it does not change system-wide defaults.
export GIRAPH_LAB_ROOT="$HOME/giraph-single-user"
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
export HADOOP_HOME="$GIRAPH_LAB_ROOT/hadoop-2.7.7"
export HADOOP_CONF_DIR="$GIRAPH_LAB_ROOT/config"
export PATH="$JAVA_HOME/bin:$HADOOP_HOME/bin:$HADOOP_HOME/sbin:$PATH"
