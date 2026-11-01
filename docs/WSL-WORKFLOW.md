# How we use WSL on this system

This document describes **Group B’s actual machine**, not the lab shared server.

## Machine map

| Layer | Detail |
|---|---|
| Host OS | Windows 10/11 |
| Distro | WSL2 **Ubuntu 24.04** (Noble), default distro name `Ubuntu` |
| Linux user | `amani` |
| Hostname | `HISPAT1999` |
| Windows project mirror | `D:\LTM` ↔ GitHub `amanpatel-dev/ltm` |
| Giraph source tree (build/run) | `/home/amani/giraph` |
| Custom algorithms path | `/home/amani/giraph/giraph-examples/src/main/java/org/apache/giraph/examples/` |
| Hadoop install | `/home/amani/hadoop` |
| HDFS local dirs | `/home/amani/hadoopdata/hdfs/{namenode,datanode}` |
| Small LTM graph (also in repo) | `/home/amani/ltm/graph.txt` |

Cursor / the coding agent reaches the same files via:

- `wsl -d Ubuntu -e bash -lc "..."` from Windows PowerShell
- UNC path `\\wsl$\Ubuntu\home\amani\...` when the distro is running

If `wsl` returns `Wsl/Service/E_UNEXPECTED`, run `wsl --shutdown`, wait a few seconds, then open Ubuntu again.

## What lives where

```text
Windows D:\LTM          → clean GitHub project (docs, src copies, datasets, scripts)
WSL ~/giraph            → full Apache Giraph source + Maven build + shaded JAR
WSL ~/hadoop            → Hadoop 2.x pseudo-distributed cluster
WSL ~/ltm               → quick local scratch graphs during experiments
```

**Rule:** implement and compile inside WSL under `~/giraph`. After a change works (or even as a draft), copy the Java/dataset/script into `D:\LTM` and commit so GitHub stays the academic record.

## Open a usable shell

From Windows Terminal / PowerShell:

```powershell
wsl -d Ubuntu
```

Or one-shot:

```powershell
wsl -d Ubuntu -e bash -lc "your-command-here"
```

Inside Ubuntu, load the Hadoop environment (your `~/.bashrc` already exports `HADOOP_HOME`). Prefer **Java 8** for Giraph:

```bash
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
export PATH="$JAVA_HOME/bin:$HADOOP_HOME/bin:$HADOOP_HOME/sbin:$PATH"
java -version    # expect 1.8.x
```

Note: `~/.bashrc` currently also mentions Java 11. For Giraph/Hadoop jobs, always force Java 8 as above until that line is cleaned up.

## Daily service check

```bash
jps
# Expect when healthy: NameNode, DataNode, ResourceManager, NodeManager
# (SecondaryNameNode may also appear)
```

If nothing Hadoop-related appears, follow [DAILY-STARTUP.md](DAILY-STARTUP.md).

## Edit → build → sync loop

1. Edit LTM Java in WSL:

```bash
nano ~/giraph/giraph-examples/src/main/java/org/apache/giraph/examples/LinearThresholdComputation.java
```

2. Rebuild examples (from Giraph source root):

```bash
cd ~/giraph
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
mvn -pl giraph-examples -am -DskipTests package
```

3. Sync the source into the GitHub mirror:

```bash
cp ~/giraph/giraph-examples/src/main/java/org/apache/giraph/examples/LinearThresholdComputation.java /mnt/d/LTM/src/
cp ~/ltm/graph.txt /mnt/d/LTM/datasets/ltm-four-node/graph.txt
```

4. On Windows (or from WSL), commit and push from `D:\LTM`.

## Credentials / keys

No GitHub password is stored in this repo. Push uses the logged-in `gh` / git credential helper on Windows (`amanpatel-dev`).

We do **not** need sudo passwords for normal Giraph job runs. Sudo is only needed if apt packages are missing.

If a future step needs:

- GitHub auth failure → run `gh auth login` on Windows
- Hadoop web UI / SSH to another machine → tell the agent; do **not** paste passwords into the repository

## Agent working agreement

When the coding agent helps:

1. Prefer `wsl -d Ubuntu -e bash -lc "..."` for build/run checks
2. Keep secrets out of commits
3. Update `D:\LTM` files that belong in GitHub
4. Leave Graph Burning experiments in Group A’s repo; keep this repo on LTM + shared foundations
