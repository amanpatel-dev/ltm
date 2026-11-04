#!/usr/bin/env bash
# Run paper-style V2 LTM on this WSL PC.
set -euo pipefail

export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-8-openjdk-amd64}"
export HADOOP_HOME="${HADOOP_HOME:-$HOME/hadoop}"
export PATH="$JAVA_HOME/bin:$HADOOP_HOME/bin:$PATH"

GIRAPH_HOME="${GIRAPH_HOME:-$HOME/giraph}"
EXAMPLES_JAR="$GIRAPH_HOME/giraph-examples/target/giraph-examples-1.4.0-SNAPSHOT-for-hadoop-2.5.1-jar-with-dependencies.jar"

INPUT_HDFS="${1:-/user/amani/ltm/v2/input}"
OUTPUT_HDFS="${2:-/user/amani/ltm/v2/output}"

if [[ ! -f "$EXAMPLES_JAR" ]]; then
  echo "Missing examples JAR: $EXAMPLES_JAR" >&2
  echo "Build with: cd ~/giraph && mvn -pl giraph-examples -am -DskipTests package" >&2
  exit 1
fi

hdfs dfs -test -d "$INPUT_HDFS" || {
  echo "HDFS input missing: $INPUT_HDFS" >&2
  echo "Upload datasets/ltm-four-node-paper/graph.txt first." >&2
  exit 1
}

hdfs dfs -rm -r -f "$OUTPUT_HDFS"

hadoop jar "$EXAMPLES_JAR" org.apache.giraph.GiraphRunner \
  org.apache.giraph.examples.V2LinearThresholdComputation \
  -vif org.apache.giraph.examples.V2LtmTextInputFormat \
  -vof org.apache.giraph.io.formats.IdWithValueTextOutputFormat \
  -vip "$INPUT_HDFS" \
  -op "$OUTPUT_HDFS" \
  -w 1

echo "Done. Fetch with: hdfs dfs -cat $OUTPUT_HDFS/*"
echo "Value columns: B activateRound credence accWeight beliefChanged"
