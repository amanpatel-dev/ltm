# Daily startup on this WSL PC

Use this after opening Ubuntu. Prefer Java 8 for every Hadoop/Giraph command.

```bash
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
export HADOOP_HOME=$HOME/hadoop
export PATH="$JAVA_HOME/bin:$HADOOP_HOME/bin:$HADOOP_HOME/sbin:$PATH"
```

## Start services

```bash
start-dfs.sh
start-yarn.sh
jps
```

Healthy `jps` typically shows NameNode, DataNode, ResourceManager, NodeManager.

Quick HDFS check:

```bash
hdfs dfs -ls /
```

## Stop services

```bash
stop-yarn.sh
stop-dfs.sh
```

## Rebuild Giraph examples after code edits

```bash
cd ~/giraph
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
mvn -pl giraph-examples -am -DskipTests package
```

The fat examples JAR used for jobs is typically:

```text
~/giraph/giraph-examples/target/giraph-examples-1.4.0-SNAPSHOT-for-hadoop-2.5.1-jar-with-dependencies.jar
```

## Run checklist before an LTM experiment

1. Services up (`jps`)
2. Fresh examples JAR built if Java changed
3. Input uploaded to HDFS
4. Output path removed if it already exists
5. Save stdout / HDFS output under `results/` in this repo after the run
6. Sync updated Java into `D:\LTM\src` and commit

Sanitized Hadoop XML from this machine is stored under `setup/config/` for documentation. Do not commit live `hadoopdata/` contents.
