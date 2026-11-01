#!/usr/bin/env bash
set -u

export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-8-openjdk-amd64}"
export HADOOP_HOME="${HADOOP_HOME:-/usr/local/hadoop}"
export HADOOP_CONF_DIR="${HADOOP_CONF_DIR:-$HADOOP_HOME/etc/hadoop}"
export PATH="$JAVA_HOME/bin:$HADOOP_HOME/bin:$HADOOP_HOME/sbin:$PATH"

failures=0

check_command() {
  if command -v "$1" >/dev/null 2>&1; then
    printf '[OK] %s: %s\n' "$1" "$(command -v "$1")"
  else
    printf '[MISSING] command: %s\n' "$1"
    failures=$((failures + 1))
  fi
}

echo '=== Commands ==='
for command_name in java javac git mvn ssh hadoop hdfs yarn jps; do
  check_command "$command_name"
done

echo
echo '=== Versions ==='
java -version 2>&1 | head -n 1 || true
hadoop version 2>/dev/null | head -n 1 || true
mvn -version 2>/dev/null | head -n 1 || true
git --version 2>/dev/null || true

echo
echo '=== Hadoop processes ==='
jps 2>/dev/null || true

echo
echo '=== HDFS ==='
if hdfs dfsadmin -report >/dev/null 2>&1; then
  echo '[OK] NameNode is reachable.'
else
  echo '[NOT READY] NameNode is not reachable.'
  failures=$((failures + 1))
fi

echo
echo '=== YARN ==='
yarn_nodes="$(yarn node -list 2>&1 || true)"
printf '%s\n' "$yarn_nodes"
if printf '%s\n' "$yarn_nodes" | grep -q 'RUNNING'; then
  echo '[OK] A RUNNING NodeManager is registered.'
else
  echo '[NOT READY] No RUNNING NodeManager is registered.'
  failures=$((failures + 1))
fi

echo
if [ "$failures" -eq 0 ]; then
  echo 'Environment check passed.'
else
  echo "Environment check found $failures problem(s). Read docs/INSTALLATION.md."
  exit 1
fi
