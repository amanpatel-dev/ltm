# Giraph laptop setup — Ubuntu 22.04, from scratch

This is the **lab-compatible** setup for a new Ubuntu laptop. We use Ubuntu 22.04 LTS, Java 8, Hadoop 2.7.7 and a pinned Giraph revision because that combination was used for our verified jobs. A newer Ubuntu/Hadoop/Java combination is a separate porting project, not a harmless upgrade. These old releases should be used for teaching on a trusted local machine, not exposed as a public production server.

Work through one section at a time. Commands labelled **administrator**, **hduser** and **mca2025** must run in those accounts. If a check finds an existing Hadoop installation or HDFS data, **stop**: this fresh-install path must not overwrite it. Nothing in this guide requires deleting an installation or formatting an existing NameNode. The two account names match our scripts and HDFS examples; they are service/development roles, not the professor's login.

Validation status (23 September 2026): we built the pinned Giraph source and ran a three-file Hadoop MapReduce job and a 30-vertex Giraph BFS job on a second Ubuntu 22.04 computer. Its existing Hadoop 2.9.2 installation and data were left untouched; our Hadoop 2.7.7 test ran in an isolated directory. The exact two-account, administrator-assisted installation below has **not** yet been trialled from start to finish on a blank laptop. Check each stop point rather than assuming the commands fit an existing machine.

## 0. Preflight — administrator's Ubuntu terminal

```bash
cat /etc/os-release | grep -E '^(NAME|VERSION_ID)='
uname -m
free -h
df -h / /usr/local /home
sudo -v
command -v java || true
command -v javac || true
command -v hadoop || true
command -v git || true
command -v mvn || true
getent passwd hduser || true
getent passwd mca2025 || true
ls -ld /usr/local/hadoop /home/hduser/hadoop_tmp 2>/dev/null || true
ls -ld /opt/hadoop /opt/hadoop/* /home/"$USER"/hadoop_data 2>/dev/null || true
```

Continue only for **Ubuntu 22.04**, **x86_64**, working `sudo`, approximately **16 GB RAM and 40 GB free disk**. 8 GB RAM might be possible with a smaller YARN configuration but is *not* this tested profile. If `hadoop` exists **or** Hadoop appears under `/opt/hadoop`, `/usr/local/hadoop`, or an account's data directory, inspect it first; a missing `hadoop` command does **not** prove that Hadoop is absent. Do not follow the Hadoop extraction/configuration/format steps below on an existing installation. If a different Java or `javac` is the system default, that is fine: we explicitly select Java 8 for these two accounts. Do not change the professor's global Java default.

Check network access to GitHub and the Apache archive. If TLS verification fails, fix the laptop/network trust configuration; do **not** use `curl -k` or disable certificate verification.

## 1. Install only missing base tools — administrator

```bash
sudo apt update
for tool in git mvn ssh wget curl tar unzip; do command -v "$tool" || echo "MISSING: $tool"; done
```

For a missing tool, install its package (`mvn` = `maven`, `ssh` server = `openssh-server`). Running `apt install` for an already installed package is safe and normally skips it:

```bash
sudo apt install git maven openssh-server wget curl tar unzip ca-certificates
```

Check the **Java 8 JDK**, not merely any `java`:

```bash
test -x /usr/lib/jvm/java-8-openjdk-amd64/bin/javac && /usr/lib/jvm/java-8-openjdk-amd64/bin/java -version
apt-cache policy openjdk-8-jdk
```

If the first check reports Java 8 (`1.8`), skip installation. Otherwise, if `apt-cache policy` shows a candidate, run `sudo apt install openjdk-8-jdk`. If it shows `Candidate: (none)`, enable Ubuntu's official `universe` component and retry:

```bash
sudo apt install software-properties-common
sudo add-apt-repository universe
sudo apt update
apt-cache policy openjdk-8-jdk
```

If there is still no candidate, **stop**; do not substitute Java 17/21 or add an unreviewed PPA. Verify `.../bin/java -version` and `.../bin/javac -version` both show `1.8`. Note the actual JDK path if it differs from `/usr/lib/jvm/java-8-openjdk-amd64` and adjust all later `JAVA_HOME` references consistently.

## 2. Obtain this project and prepare two accounts — administrator

If the repository is absent, clone it. If present, verify that it is the intended repository and use its current files; do not clone over it.

```bash
test -d /home/mca2025/giraph-learning-lab/.git && echo 'Project checkout already exists; verify its remote later' || echo 'Project checkout not present yet'
getent group hadoop || sudo addgroup hadoop
id hduser 2>/dev/null || sudo adduser hduser
id mca2025 2>/dev/null || sudo adduser mca2025
id hduser
id mca2025
```

If an account already exists, confirm it is the intended account and its home is `/home/hduser` or `/home/mca2025`. A same-named account with another home or purpose is a **stop point**. Add missing group memberships only after checking `id`:

```bash
id -nG hduser | grep -qw hadoop || sudo usermod -aG hadoop hduser
id -nG mca2025 | grep -qw hadoop || sudo usermod -aG hadoop mca2025
```

New group membership applies after a new login. Verify with `su - hduser -c 'id'` and `su - mca2025 -c 'id'`. Install this repo as `mca2025`:

```bash
sudo -iu mca2025
cd "$HOME"
git clone https://github.com/25mcmc34-kumarshivam/APACHE-GIRAPH-PROJECT-.git giraph-learning-lab
git -C "$HOME/giraph-learning-lab" remote -v
exit
```

If `giraph-learning-lab` already exists, **skip the clone**, check `git -C /home/mca2025/giraph-learning-lab remote -v` and `git status --short`; do not pull over uncommitted work.

## 3. Local SSH for Hadoop startup — administrator, then hduser

Hadoop's single-node startup scripts SSH to `localhost`, even though all daemons run on this laptop.

```bash
sudo systemctl start ssh
sudo -iu hduser
mkdir -p "$HOME/.ssh"
chmod 700 "$HOME/.ssh"
test -f "$HOME/.ssh/id_rsa" || ssh-keygen -t rsa -b 3072 -N '' -f "$HOME/.ssh/id_rsa"
touch "$HOME/.ssh/authorized_keys"
grep -qxF "$(cat "$HOME/.ssh/id_rsa.pub")" "$HOME/.ssh/authorized_keys" || cat "$HOME/.ssh/id_rsa.pub" >> "$HOME/.ssh/authorized_keys"
chmod 600 "$HOME/.ssh/authorized_keys"
ssh localhost 'echo Local SSH works'
ssh -o BatchMode=yes localhost 'echo Passwordless local SSH works'
exit
```

The first `ssh localhost` may ask you to confirm the host key. Inspect it and answer once. This starts SSH for the current boot only; it does not automatically expose an SSH service after every reboot. Start it again before Hadoop if needed. Never upload the private key, `.ssh`, passwords or Hadoop data to GitHub.

## 4. Hadoop 2.7.7 — administrator, only on a fresh machine

If the preflight found a Hadoop installation or HDFS data, **skip this entire section and seek a migration review**. Do not mix existing Hadoop versions/configurations with these files.

```bash
cd /tmp
wget https://archive.apache.org/dist/hadoop/common/hadoop-2.7.7/hadoop-2.7.7.tar.gz
echo 'd129d08a2c9dafec32855a376cbd2ab90c6a42790898cabbac6be4d29f9c2026  hadoop-2.7.7.tar.gz' | sha256sum -c -
test ! -e /usr/local/hadoop && test ! -e /usr/local/hadoop-2.7.7
sudo tar -xzf hadoop-2.7.7.tar.gz -C /usr/local
sudo mv /usr/local/hadoop-2.7.7 /usr/local/hadoop
sudo chown -R hduser:hadoop /usr/local/hadoop
/usr/local/hadoop/bin/hadoop version | head -n 1
```

The hash comes from the [Apache archive checksum file](https://archive.apache.org/dist/hadoop/common/hadoop-2.7.7/hadoop-2.7.7.tar.gz.mds). If the check fails, do not extract the download. `chown -R` here applies **only to the newly extracted `/usr/local/hadoop`**, never to an existing installation or `/usr/local` itself.

The reference configurations live in this repo's `setup/config/`. Back up the newly extracted configuration as `hduser` before replacing anything:

```bash
sudo -iu hduser
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
export HADOOP_HOME=/usr/local/hadoop
export HADOOP_CONF_DIR="$HADOOP_HOME/etc/hadoop"
export PATH="$JAVA_HOME/bin:$HADOOP_HOME/bin:$HADOOP_HOME/sbin:$PATH"
test ! -e "$HOME/hadoop-conf-original" || { echo 'STOP: original config backup already exists'; exit 1; }
cp -a "$HADOOP_CONF_DIR" "$HOME/hadoop-conf-original"
exit
```

Back as the **administrator**, copy only on this fresh install. `sudo` can read the project files even when `/home/mca2025` is private; do not open that home directory to everyone just for this step:

```bash
sudo cp /home/mca2025/giraph-learning-lab/setup/config/{core,hdfs,mapred,yarn}-site.xml /usr/local/hadoop/etc/hadoop/
sudo chown hduser:hadoop /usr/local/hadoop/etc/hadoop/{core,hdfs,mapred,yarn}-site.xml
sudo -iu hduser
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
export HADOOP_HOME=/usr/local/hadoop
export HADOOP_CONF_DIR="$HADOOP_HOME/etc/hadoop"
export PATH="$JAVA_HOME/bin:$HADOOP_HOME/bin:$HADOOP_HOME/sbin:$PATH"
```

If `hadoop-conf-original` exists, stop and choose a different backup name; never overwrite a backup. In `$HADOOP_CONF_DIR/hadoop-env.sh`, set the existing `export JAVA_HOME=` line to `/usr/lib/jvm/java-8-openjdk-amd64` (use an editor; avoid adding two conflicting lines). Verify with `grep -n JAVA_HOME "$HADOOP_CONF_DIR/hadoop-env.sh"`.

### Ubuntu 22.04 / Hadoop 2.7.7 NodeManager compatibility

This is required for the tested version combination. Hadoop 2.7.7 invokes `kill -15 -PROCESS_GROUP` when it cleans up a container. On our Ubuntu 22.04 test PC, `/usr/bin/kill` interpreted that form as `kill(0, SIGTERM)`, terminating the NodeManager's own group. We reproduced it with `strace` and confirmed that adding `--` sends the signal to the intended container group. The project wrapper changes **only** this two-argument process-group form and is used **only** by Hadoop daemons through `yarn-env.sh`; it does not replace the system's `kill` command.

First type `exit` to leave the `hduser` shell and return to the **administrator** shell. Then install the reviewed wrapper for `hduser` on this new setup:

```bash
exit
sudo install -d -m 755 -o hduser -g hadoop /home/hduser/giraph-compat
sudo install -m 755 -o hduser -g hadoop /home/mca2025/giraph-learning-lab/setup/compat/kill /home/hduser/giraph-compat/kill
sudo -u hduser bash -n /home/hduser/giraph-compat/kill
sudo -iu hduser
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
export HADOOP_HOME=/usr/local/hadoop
export HADOOP_CONF_DIR="$HADOOP_HOME/etc/hadoop"
export PATH="$JAVA_HOME/bin:$HADOOP_HOME/bin:$HADOOP_HOME/sbin:$PATH"
```

As **hduser**, open `/usr/local/hadoop/etc/hadoop/yarn-env.sh` and add this line once at the end:

```bash
export PATH="/home/hduser/giraph-compat:$PATH"
```

Check it with `grep -n 'giraph-compat' "$HADOOP_CONF_DIR/yarn-env.sh"`. Do not add this directory to every user's global `PATH`. If your Hadoop release or OS differs, investigate first; do not assume this wrapper is needed there.

Persist the environment **once** in `/home/hduser/.bashrc` (use `nano`; do not add duplicate/conflicting exports):

```bash
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
export HADOOP_HOME=/usr/local/hadoop
export HADOOP_CONF_DIR="$HADOOP_HOME/etc/hadoop"
export PATH="$JAVA_HOME/bin:$HADOOP_HOME/bin:$HADOOP_HOME/sbin:$PATH"
```

Create the local temporary base, **not** `dfs/name` or `dfs/data` yourself:

```bash
mkdir -p "$HOME/hadoop_tmp"
grep -n -E 'JAVA_HOME|HADOOP_HOME|HADOOP_CONF_DIR' "$HOME/.bashrc"
hdfs getconf -confKey fs.defaultFS
hdfs getconf -confKey mapreduce.framework.name
```

Expected: `hdfs://localhost:9000` and `yarn`. Check the NameNode metadata before the **one-time format**:

```bash
ls -ld "$HOME/hadoop_tmp/dfs/name" "$HOME/hadoop_tmp/dfs/data" 2>/dev/null || true
```

Only if **both paths do not exist**, this is a new laptop, and no previous HDFS was configured, run `hdfs namenode -format` **once**. If either path exists, **do not format**. Stop and inspect why. Formatting an existing NameNode can make existing HDFS files inaccessible.

## 5. Start Hadoop and create HDFS workspace — hduser

```bash
start-dfs.sh
start-yarn.sh
jps
hdfs dfsadmin -report
yarn node -list
```

Expect NameNode, DataNode, SecondaryNameNode, ResourceManager and NodeManager, one live DataNode, and **exactly one RUNNING YARN node**. If a service is already running, do not start a duplicate; check the process and YARN registration first. If YARN stays at zero nodes, read `docs/DAILY-LAB-STARTUP.md` and NodeManager logs rather than submitting a Giraph job.

For this new HDFS only, create shared scratch and the development home:

```bash
hdfs dfs -mkdir -p /tmp /tmp/hadoop-yarn/staging /user/mca2025
hdfs dfs -chmod 1777 /tmp /tmp/hadoop-yarn /tmp/hadoop-yarn/staging
hdfs dfs -chown mca2025:hadoop /user/mca2025
hdfs dfs -ls -d /tmp /tmp/hadoop-yarn /tmp/hadoop-yarn/staging /user/mca2025
exit
```

Do not apply these `chmod`/`chown` commands to an existing/shared HDFS without examining its ownership and policies.

## 6. Giraph source, exact repair and build — mca2025

```bash
sudo -iu mca2025
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
export HADOOP_HOME=/usr/local/hadoop
export HADOOP_CONF_DIR="$HADOOP_HOME/etc/hadoop"
export PATH="$JAVA_HOME/bin:$HADOOP_HOME/bin:$HADOOP_HOME/sbin:$PATH"
cd "$HOME"
test -e giraph && echo 'Giraph directory exists: inspect before cloning' || git clone https://github.com/apache/giraph.git giraph
cd "$HOME/giraph"
git status --short
git rev-parse HEAD
```

For a fresh clone with an empty `git status --short`, pin the exact source revision used in our lab. If the directory existed before this session, pause and inspect any local changes before running the next command:

```bash
git checkout 14a74297378dc1584efbb698054f0e8bff4f90bc
git rev-parse HEAD
```

If `giraph` existed before this guide, **do not change its checkout** until you have inspected its status and revision. If a fresh clone cannot find the commit, run `git fetch origin trunk` and retry. The source revision is from [Apache Giraph's repository](https://github.com/apache/giraph).

Apply our reviewed shading patch once:

```bash
PATCH_FILE="$HOME/giraph-learning-lab/setup/patches/giraph-hadoop-2.7.7.patch"
if git apply --reverse --check "$PATCH_FILE"; then
  echo 'Compatibility patch is already present; skip'
elif git apply --check "$PATCH_FILE"; then
  git apply "$PATCH_FILE"
else
  echo 'STOP: source differs from the pinned revision or has partial edits'
fi
git diff --stat
```

If the `STOP` branch prints, **do not build yet**. Keep the pinned Giraph source's Guava **21.0** dependency for compilation: changing it to Hadoop's 11.0.2 caused missing `MoreObjects` symbols in a clean build. The patch adds a shaded examples JAR that relocates Giraph's Guava classes so Hadoop can still use its own older Guava at runtime.

Copy this project's **four new `Learning*Computation.java` classes** before building. The `LongDoubleFloatTextInputFormat.java` in this study repository is an annotated copy of a class already present upstream; **do not overwrite the upstream file**. If a destination already exists and differs, inspect it instead of overwriting it:

```bash
for source_file in "$HOME"/giraph-learning-lab/src/Learning*Computation.java; do
  target_file="$HOME/giraph/giraph-examples/src/main/java/org/apache/giraph/examples/$(basename "$source_file")"
  if test -e "$target_file"; then cmp -s "$source_file" "$target_file" || { echo "STOP: different existing file: $target_file"; exit 1; }; else cp "$source_file" "$target_file"; fi
done
```

If any `STOP` printed, resolve it before building. Our study repository contains custom example classes; it is not a replacement for upstream Giraph. Build from `$HOME/giraph`:

```bash
mvn -s "$HOME/giraph-learning-lab/setup/maven-settings-https.xml" -B clean -Phadoop_2 -DskipTests -Dgiraph.maven.duplicate.finder.skip=true package
test -s giraph-examples/target/giraph-examples-1.4.0-SNAPSHOT-hadoop-guava-shaded.jar
jar tf giraph-examples/target/giraph-examples-1.4.0-SNAPSHOT-hadoop-guava-shaded.jar | grep -m1 '^org/apache/giraph/shaded/com/google/common/base/Preconditions.class$'
if jar tf giraph-examples/target/giraph-examples-1.4.0-SNAPSHOT-hadoop-guava-shaded.jar | grep -q '^com/google/common/'; then echo 'STOP: unrelocated Guava remains'; fi
jar tf giraph-examples/target/giraph-examples-1.4.0-SNAPSHOT-hadoop-guava-shaded.jar | grep 'LearningBfsComputation.class'
```

Expected: `BUILD SUCCESS`, a nonempty shaded JAR, a relocated Guava class, **no** original `com/google/common/` classes, and our learning class. The pinned upstream POM contains an obsolete HTTP Maven repository; the supplied settings file routes downloads through HTTPS Maven Central. Its `mirrorOf=*` applies to this command only; do not install it globally for unrelated projects. Maven downloads dependencies during the first build, so this step needs network access and can take longer than later rebuilds. On the second PC, this exact shaded build completed in about 85 seconds after dependencies were available.

Persist the same four environment exports in `/home/mca2025/.bashrc` once. Open a new login and check `java -version`, `hadoop version`, `mvn -version`, `command -v yarn`. `jps` run as `mca2025` may show only that user's processes; check daemon health with `yarn node -list` and `hdfs dfsadmin -report`, or use `jps` as `hduser`.

## 7. End-to-end smoke test — mca2025

Run only when HDFS and YARN checks pass. The output directory must be new:

```bash
cd "$HOME/giraph-learning-lab"
chmod +x scripts/check_environment.sh scripts/run_text_graph_algorithm.sh
./scripts/check_environment.sh
hdfs dfs -mkdir -p /user/mca2025/giraph_learning/install_smoke_input
if hdfs dfs -test -e /user/mca2025/giraph_learning/install_smoke_input/part-01.txt; then
  echo 'Input is already present; inspect all three parts before reusing it'
else
  hdfs dfs -put datasets/thirty-node-text/part-*.txt /user/mca2025/giraph_learning/install_smoke_input/
fi
hdfs dfs -ls /user/mca2025/giraph_learning/install_smoke_input
MAPREDUCE_OUTPUT="/user/mca2025/giraph_learning/install_smoke_wordcount_$(date +%Y%m%d_%H%M%S)"
hadoop jar "$HADOOP_HOME/share/hadoop/mapreduce/hadoop-mapreduce-examples-2.7.7.jar" wordcount /user/mca2025/giraph_learning/install_smoke_input "$MAPREDUCE_OUTPUT"
hdfs dfs -cat "$MAPREDUCE_OUTPUT"/part-r-00000 | head -n 5
SMOKE_OUTPUT="/user/mca2025/giraph_learning/install_smoke_bfs_$(date +%Y%m%d_%H%M%S)"
./scripts/run_text_graph_algorithm.sh bfs /user/mca2025/giraph_learning/install_smoke_input "$SMOKE_OUTPUT" 1
hdfs dfs -cat "$SMOKE_OUTPUT"/part-m-\* | sort -n | head -n 10
ps -ef | grep '[N]odeManager'
yarn node -list
```

The input directory should contain **three** text parts. Wordcount must finish successfully, including its reducer; this tests container cleanup before Giraph. BFS from vertex 1 should begin `1 0.0`, `2 1.0`, `3 2.0`, `4 3.0` (tabs/spaces may differ). The Giraph job should report 30 vertices and 79 edges. Compare the full result with `results/` only after a successful job. The NodeManager must still be alive after both jobs. If the job waits for resources, check both `yarn node -list` **and the live NodeManager process as `hduser`**; a stale node listing alone is not proof the process survived. `ACCEPTED` with no live node is not a computation in progress.

## Daily use and important stop points

After a reboot, start local SSH as administrator if it is not running (`sudo systemctl start ssh`). Log in as `hduser`, run `start-dfs.sh` and `start-yarn.sh` **only for services not already running**, then verify `jps`, `hdfs dfsadmin -report`, and `yarn node -list`. Log in as `mca2025` to submit jobs. Do **not** format the NameNode again or rebuild Giraph for every job. Use [daily lab startup](../docs/DAILY-LAB-STARTUP.md) for normal work and recovery.

Stop rather than improvise if: OS differs from Ubuntu 22.04; laptop lacks memory; an existing Hadoop/data directory is present; package signatures/TLS checks fail; Giraph patch does not apply cleanly; YARN reports zero or duplicate nodes; or HDFS reports no live DataNode. Record the exact command and error before changing configuration.

Primary references: [Apache Hadoop 2.7.7 archive](https://archive.apache.org/dist/hadoop/common/hadoop-2.7.7/), [Apache Hadoop single-node documentation](https://hadoop.apache.org/docs/r2.7.7/hadoop-project-dist/hadoop-common/SingleNodeSetup.html), [Apache Giraph source](https://github.com/apache/giraph).
