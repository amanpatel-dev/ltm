# Apache Giraph on Ubuntu 22.04 — one-account setup

Use this guide when **one person uses one Ubuntu laptop** for Hadoop and Giraph. The same Ubuntu login owns the files, starts Hadoop, builds Giraph, and runs jobs. No `hduser` or `mca2025` account is created. If several students need separate access to a shared server, use the [two-account guide](UBUNTU-22.04-FROM-SCRATCH.md) instead.

This follows the isolated setup tested on a second Ubuntu 22.04 PC on 23 September 2026: Java 8, Hadoop 2.7.7, Maven 3.6.3, pinned Giraph 1.4.0-SNAPSHOT source, a three-file MapReduce job, and a 30-vertex Giraph BFS job. That PC already had a different Hadoop installation, which we **did not change**. The commands below create a separate directory in the current user's home. The configuration helper reproduces the tested settings and passed a shell syntax check, but the exact helper has not yet been rerun as a whole on a blank laptop. We have not tested every possible Ubuntu laptop or network.

Read a whole step before running it, and keep using the same terminal unless a step says otherwise. Stop if a check differs from the expected result; do not format an existing HDFS directory or overwrite someone else's Hadoop configuration. The old software is for a trusted teaching laptop, not a public production server.

## 1. Check the laptop and current account

Open an Ubuntu terminal as the account that will own the project. Keep using this same account throughout this guide. Check:

```bash
whoami
echo "$HOME"
cat /etc/os-release | grep -E '^(NAME|VERSION_ID)='
uname -m
free -h
df -h "$HOME"
command -v java || true
command -v javac || true
command -v mvn || true
command -v git || true
command -v hadoop || true
command -v setsid || true
ls -ld "$HOME/giraph-single-user" /opt/hadoop /usr/local/hadoop 2>/dev/null || true
```

Use **Ubuntu 22.04 x86_64**, about **16 GB RAM**, and at least **40 GB free**. A different system needs its own review. An existing Hadoop installation is not automatically a problem: this guide uses different directories and ports, but do not confuse the two installations. If `$HOME/giraph-single-user` already exists, **stop** and inspect it; this is not an upgrade or overwrite procedure.

You need administrator rights **only to install missing system packages**. Hadoop and Giraph themselves stay in your home directory. If your laptop has a different default Java version, leave it alone; we explicitly select Java 8 for this project.

## 2. Install missing prerequisites

Check whether the Java **8 JDK** exists (both `java` and `javac` must report 1.8):

```bash
/usr/lib/jvm/java-8-openjdk-amd64/bin/java -version
/usr/lib/jvm/java-8-openjdk-amd64/bin/javac -version
mvn -version
git --version
```

If any required tool is missing, use Ubuntu packages. Already-installed packages are normally left unchanged:

```bash
sudo apt update
sudo apt install git maven curl tar unzip ca-certificates openjdk-8-jdk
```

If Ubuntu reports that `openjdk-8-jdk` has no installation candidate, check `apt-cache policy openjdk-8-jdk`, enable Ubuntu's official `universe` component if needed, and retry. If it is still unavailable, **stop**; do not silently replace Java 8 with Java 17/21 or add an unreviewed package source. Maven 3.6.3 was used in our validation. If your Maven version differs significantly, note it before building.

Do not bypass TLS errors with `curl -k`. Fix the laptop or network certificate configuration first.

## 3. Clone the project and download Hadoop

Set up the one private workspace. Do not continue if the directory already exists:

```bash
export GIRAPH_LAB_ROOT="$HOME/giraph-single-user"
test ! -e "$GIRAPH_LAB_ROOT" || { echo 'STOP: lab directory already exists'; exit 1; }
mkdir "$GIRAPH_LAB_ROOT"
git clone https://github.com/25mcmc34-kumarshivam/APACHE-GIRAPH-PROJECT-.git "$GIRAPH_LAB_ROOT/project"
mkdir "$GIRAPH_LAB_ROOT/downloads"
```

Download Hadoop 2.7.7. The Apache archive can be slow; the alternative mirror below is the one used in our trial. **In either case, verify the same official Apache SHA-256 before extraction**:

```bash
curl -fL --retry 3 \
  -o "$GIRAPH_LAB_ROOT/downloads/hadoop-2.7.7.tar.gz" \
  https://archive.apache.org/dist/hadoop/common/hadoop-2.7.7/hadoop-2.7.7.tar.gz
```

If that download fails or is too slow, download from the tested mirror instead (do not run both when the first succeeded):

```bash
curl -fL --retry 3 \
  -o "$GIRAPH_LAB_ROOT/downloads/hadoop-2.7.7.tar.gz" \
  https://repo.huaweicloud.com/apache/hadoop/common/hadoop-2.7.7/hadoop-2.7.7.tar.gz
```

Then verify and extract:

```bash
cd "$GIRAPH_LAB_ROOT/downloads"
echo 'd129d08a2c9dafec32855a376cbd2ab90c6a42790898cabbac6be4d29f9c2026  hadoop-2.7.7.tar.gz' | sha256sum -c -
test ! -e "$GIRAPH_LAB_ROOT/hadoop-2.7.7" || { echo 'STOP: Hadoop target exists'; exit 1; }
tar -xzf hadoop-2.7.7.tar.gz -C "$GIRAPH_LAB_ROOT"
```

The hash is published in the [Apache archive checksum file](https://archive.apache.org/dist/hadoop/common/hadoop-2.7.7/hadoop-2.7.7.tar.gz.mds). If it does not match, do not use the archive.

## 4. Create an isolated Hadoop configuration

The project helper makes a **new** config directory, ports and local data paths under `$GIRAPH_LAB_ROOT`. It does **not** format HDFS or start services. Read it if you want to see every setting: `setup/single-account/configure.sh`.

First check that the chosen ports are not already listening:

```bash
ss -ltn | grep -E ':(19000|15070|15010|15075|15020|18032|18030|18031|18033|18088|18042|18040|18041)\b' || true
```

If any line appears, stop: another service may be using one of the ports. Otherwise:

```bash
bash "$GIRAPH_LAB_ROOT/project/setup/single-account/configure.sh"
source "$GIRAPH_LAB_ROOT/project/setup/single-account/env.sh"
java -version
hadoop version | head -n 1
hdfs getconf -confKey fs.defaultFS
hdfs getconf -confKey mapreduce.framework.name
```

Expected: Java `1.8`, Hadoop `2.7.7`, `hdfs://localhost:19000`, and `yarn`. The helper also installs a project-local compatibility wrapper for Hadoop 2.7.7's container cleanup on Ubuntu 22.04. Only the YARN daemons launched with this config use it; the system `kill` command is unchanged. This fix was necessary to keep the NodeManager alive after jobs on the test PC.

## 5. Format **this new HDFS only once**, then start services

Check the new data paths before formatting:

```bash
ls -ld "$GIRAPH_LAB_ROOT/hadoop_tmp/dfs/name" "$GIRAPH_LAB_ROOT/hadoop_tmp/dfs/data" 2>/dev/null || true
```

Only if **both do not exist**, run:

```bash
hdfs namenode -format
```

Never repeat the format after HDFS has data. A repeated format can make that data inaccessible. If either path already exists, stop and inspect it.

Start the four services directly. This avoids `start-dfs.sh`/`start-yarn.sh` using SSH to localhost; a passwordless SSH setup is **not** needed for this one-account method:

```bash
setsid -f "$HADOOP_HOME/sbin/hadoop-daemon.sh" start namenode
setsid -f "$HADOOP_HOME/sbin/hadoop-daemon.sh" start datanode
setsid -f "$HADOOP_HOME/sbin/yarn-daemon.sh" start resourcemanager
setsid -f "$HADOOP_HOME/sbin/yarn-daemon.sh" start nodemanager
jps
hdfs dfsadmin -report
yarn node -list
```

Expect `NameNode`, `DataNode`, `ResourceManager`, `NodeManager`, **one live DataNode**, and **one RUNNING YARN node**. Wait a few seconds and repeat the checks if daemons are still starting. If a daemon is already running, do not start another copy. The one-account trial did not start a `SecondaryNameNode`; plan proper HDFS checkpoints if keeping data long-term.

Create only this new HDFS workspace:

```bash
hdfs dfs -mkdir -p /tmp /tmp/hadoop-yarn/staging "/user/$USER/giraph_learning"
hdfs dfs -chmod 1777 /tmp /tmp/hadoop-yarn /tmp/hadoop-yarn/staging
hdfs dfs -ls -d /tmp /tmp/hadoop-yarn /tmp/hadoop-yarn/staging "/user/$USER/giraph_learning"
```

## 6. Build Giraph in the same account

Clone Apache Giraph and pin the exact revision used in the successful trial:

```bash
test ! -e "$GIRAPH_LAB_ROOT/giraph" || { echo 'STOP: Giraph directory already exists'; exit 1; }
git clone https://github.com/apache/giraph.git "$GIRAPH_LAB_ROOT/giraph"
cd "$GIRAPH_LAB_ROOT/giraph"
git checkout 14a74297378dc1584efbb698054f0e8bff4f90bc
git status --short
```

If the checkout cannot find the revision, run `git fetch origin trunk` and retry; do not pick an arbitrary newer commit. Apply this project's shading patch. Keep Giraph's upstream Guava **21.0** for compilation; the patch relocates its Guava classes inside the final JAR so they do not conflict with Hadoop's older Guava:

```bash
git apply --check "$GIRAPH_LAB_ROOT/project/setup/patches/giraph-hadoop-2.7.7.patch"
git apply "$GIRAPH_LAB_ROOT/project/setup/patches/giraph-hadoop-2.7.7.patch"
for source_file in "$GIRAPH_LAB_ROOT"/project/src/Learning*Computation.java; do
  target_file="$GIRAPH_LAB_ROOT/giraph/giraph-examples/src/main/java/org/apache/giraph/examples/$(basename "$source_file")"
  test ! -e "$target_file" || { echo "STOP: existing source: $target_file"; exit 1; }
  cp "$source_file" "$target_file"
done
```

Do not copy `src/LongDoubleFloatTextInputFormat.java`: that input reader already exists in the pinned Giraph source. Build with the project-specific HTTPS Maven settings (the archived upstream POM still refers to HTTP repositories):

```bash
cd "$GIRAPH_LAB_ROOT/giraph"
mvn -s "$GIRAPH_LAB_ROOT/project/setup/maven-settings-https.xml" -B clean -Phadoop_2 -DskipTests -Dgiraph.maven.duplicate.finder.skip=true package
test -s giraph-examples/target/giraph-examples-1.4.0-SNAPSHOT-hadoop-guava-shaded.jar
jar tf giraph-examples/target/giraph-examples-1.4.0-SNAPSHOT-hadoop-guava-shaded.jar | grep -m1 '^org/apache/giraph/shaded/com/google/common/base/Preconditions.class$'
```

Expect `BUILD SUCCESS` and the shaded class. If Maven fails, read its **first actual error**, not just the final reactor summary. The HTTPS settings apply only to this Maven command; do not make them a global mirror for unrelated projects.

## 7. Prove that Hadoop and Giraph work

The following is a **test**, not a step to repeat before every job. Upload the included 30-vertex graph as **three separate text files**:

```bash
HDFS_BASE="/user/$USER/giraph_learning"
hdfs dfs -mkdir -p "$HDFS_BASE/install_smoke_input"
hdfs dfs -put "$GIRAPH_LAB_ROOT"/project/datasets/thirty-node-text/part-*.txt "$HDFS_BASE/install_smoke_input/"
hdfs dfs -ls "$HDFS_BASE/install_smoke_input"
```

Expect three `part-*.txt` files. If they already exist, stop and inspect them instead of uploading duplicates. First run a MapReduce job with a reducer; this checks that the NodeManager survives container cleanup:

```bash
STAMP=$(date +%Y%m%d_%H%M%S)
MAPREDUCE_OUTPUT="$HDFS_BASE/install_smoke_wordcount_$STAMP"
hadoop jar "$HADOOP_HOME/share/hadoop/mapreduce/hadoop-mapreduce-examples-2.7.7.jar" wordcount "$HDFS_BASE/install_smoke_input" "$MAPREDUCE_OUTPUT"
hdfs dfs -cat "$MAPREDUCE_OUTPUT/part-r-00000" | head -n 5
```

It must say the job completed successfully. Now run Giraph BFS from vertex 1:

```bash
GIRAPH_JAR="$GIRAPH_LAB_ROOT/giraph/giraph-examples/target/giraph-examples-1.4.0-SNAPSHOT-hadoop-guava-shaded.jar"
BFS_OUTPUT="$HDFS_BASE/install_smoke_bfs_$STAMP"
hadoop jar "$GIRAPH_JAR" org.apache.giraph.GiraphRunner \
  org.apache.giraph.examples.LearningBfsComputation \
  -vif org.apache.giraph.examples.LongDoubleFloatTextInputFormat \
  -vip "$HDFS_BASE/install_smoke_input" \
  -vof org.apache.giraph.io.formats.IdWithValueTextOutputFormat \
  -op "$BFS_OUTPUT" -w 1 \
  -ca mapred.job.tracker=yarn \
  -ca LearningBfsComputation.sourceId=1
hdfs dfs -cat "$BFS_OUTPUT"/part-m-\* | sort -n | head -n 10
jps
yarn node -list
```

Giraph should report **30 vertices, 79 directed edges**, and `completed successfully`. The first distances should be `1 0.0`, `2 1.0`, `3 2.0`, `4 3.0` (spacing may differ). The NodeManager must still be present after the jobs. An application stuck in `ACCEPTED` is not finished; check the process as well as `yarn node -list`.

## Daily use, shutdown, and limits

After a reboot, open a terminal in the **same account** and run `source "$HOME/giraph-single-user/project/setup/single-account/env.sh"`. Check `jps`; start **only the missing** Hadoop services with the commands in step 5. Do not format HDFS again and do not rebuild Giraph for each job. Use a **new HDFS output path** for each run.

Before stopping services, verify that `yarn application -list -appStates ACCEPTED,RUNNING` shows no active work. Then, if these are your isolated services:

```bash
yarn-daemon.sh stop nodemanager
yarn-daemon.sh stop resourcemanager
hadoop-daemon.sh stop datanode
hadoop-daemon.sh stop namenode
```

This one-user setup makes local learning simpler but does **not** provide separate student permissions or a multi-machine Hadoop cluster. The three input files are processed as multiple input splits on **one computer**; that demonstrates the input model, not multi-computer distribution. Keep the two-account/shared-server guide for a team server. Do not publish private keys, passwords, raw HDFS storage, or machine-specific files to GitHub.
