#!/usr/bin/env bash
# Make a separate Hadoop configuration for one Ubuntu user. Run once, before
# formatting HDFS. The original Hadoop distribution/configuration is untouched.
set -euo pipefail

LAB_ROOT="$HOME/giraph-single-user"
HADOOP_DIR="$LAB_ROOT/hadoop-2.7.7"
PROJECT_DIR="$LAB_ROOT/project"
CONFIG_DIR="$LAB_ROOT/config"
COMPAT_DIR="$LAB_ROOT/compat-bin"

if [[ ! -d "$HADOOP_DIR/etc/hadoop" || ! -f "$PROJECT_DIR/setup/compat/kill" ]]; then
  echo 'STOP: expected Hadoop and project files are missing.' >&2
  exit 1
fi
if [[ -e "$CONFIG_DIR" || -e "$COMPAT_DIR" ]]; then
  echo 'STOP: configuration or compatibility directory already exists; inspect it first.' >&2
  exit 1
fi
if [[ "$LAB_ROOT" == *'&'* || "$LAB_ROOT" == *'<'* || "$LAB_ROOT" == *'>'* ]]; then
  echo 'STOP: home-directory path contains a character unsafe in XML.' >&2
  exit 1
fi

mkdir "$CONFIG_DIR" "$COMPAT_DIR"
cp -a "$HADOOP_DIR/etc/hadoop/." "$CONFIG_DIR/"
install -m 755 "$PROJECT_DIR/setup/compat/kill" "$COMPAT_DIR/kill"
bash -n "$COMPAT_DIR/kill"

cat > "$CONFIG_DIR/core-site.xml" <<EOF
<?xml version="1.0"?>
<configuration>
  <property><name>fs.defaultFS</name><value>hdfs://localhost:19000</value></property>
  <property><name>hadoop.tmp.dir</name><value>$LAB_ROOT/hadoop_tmp</value></property>
</configuration>
EOF

cat > "$CONFIG_DIR/hdfs-site.xml" <<EOF
<?xml version="1.0"?>
<configuration>
  <property><name>dfs.replication</name><value>1</value></property>
  <property><name>dfs.namenode.name.dir</name><value>file:$LAB_ROOT/hadoop_tmp/dfs/name</value></property>
  <property><name>dfs.datanode.data.dir</name><value>file:$LAB_ROOT/hadoop_tmp/dfs/data</value></property>
  <property><name>dfs.namenode.http-address</name><value>localhost:15070</value></property>
  <property><name>dfs.datanode.address</name><value>localhost:15010</value></property>
  <property><name>dfs.datanode.http.address</name><value>localhost:15075</value></property>
  <property><name>dfs.datanode.ipc.address</name><value>localhost:15020</value></property>
</configuration>
EOF

cat > "$CONFIG_DIR/mapred-site.xml" <<'EOF'
<?xml version="1.0"?>
<configuration>
  <property><name>mapreduce.framework.name</name><value>yarn</value></property>
</configuration>
EOF

cat > "$CONFIG_DIR/yarn-site.xml" <<EOF
<?xml version="1.0"?>
<configuration>
  <property><name>yarn.nodemanager.aux-services</name><value>mapreduce_shuffle</value></property>
  <property><name>yarn.nodemanager.aux-services.mapreduce.shuffle.class</name><value>org.apache.hadoop.mapred.ShuffleHandler</value></property>
  <property><name>yarn.resourcemanager.hostname</name><value>localhost</value></property>
  <property><name>yarn.resourcemanager.address</name><value>localhost:18032</value></property>
  <property><name>yarn.resourcemanager.scheduler.address</name><value>localhost:18030</value></property>
  <property><name>yarn.resourcemanager.resource-tracker.address</name><value>localhost:18031</value></property>
  <property><name>yarn.resourcemanager.admin.address</name><value>localhost:18033</value></property>
  <property><name>yarn.resourcemanager.webapp.address</name><value>localhost:18088</value></property>
  <property><name>yarn.nodemanager.webapp.address</name><value>localhost:18042</value></property>
  <property><name>yarn.nodemanager.localizer.address</name><value>localhost:18040</value></property>
  <property><name>yarn.nodemanager.address</name><value>localhost:18041</value></property>
  <property><name>yarn.nodemanager.local-dirs</name><value>$LAB_ROOT/hadoop_tmp/nm-local</value></property>
  <property><name>yarn.nodemanager.log-dirs</name><value>$LAB_ROOT/hadoop_tmp/nm-logs</value></property>
  <property><name>yarn.nodemanager.resource.memory-mb</name><value>8192</value></property>
  <property><name>yarn.scheduler.maximum-allocation-mb</name><value>8192</value></property>
</configuration>
EOF

# These are private copies of the distribution's environment files. The
# compatibility directory is visible only to daemons started with this config.
printf '\nexport JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64\n' >> "$CONFIG_DIR/hadoop-env.sh"
printf '\nexport PATH="%s/compat-bin:$PATH"\n' "$LAB_ROOT" >> "$CONFIG_DIR/yarn-env.sh"

mkdir -p "$LAB_ROOT/hadoop_tmp"
echo "Created isolated configuration: $CONFIG_DIR"
echo 'No NameNode was formatted and no Hadoop services were started.'
