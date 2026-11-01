# Linear Threshold Model on Apache Giraph

Group B repository for the MCA project **Implementation of Distributed Graph Algorithms for Social Network Problems Using Apache Giraph Framework** at the University of Hyderabad.

Our implementation focus is the **Linear Threshold Model (LTM)** for influence diffusion. Group A works on Graph Burning in a separate repository; the shared foundation (Hadoop, Giraph, PageRank, degree, BFS/SSSP, lab workflow) is the same idea, but the research algorithm and experiments differ.

| Start here | Purpose |
|---|---|
| [Project overview](docs/PROJECT-OVERVIEW.md) | Team, faculty, title, evaluation context |
| [WSL workflow](docs/WSL-WORKFLOW.md) | How we use Ubuntu WSL on this Windows PC |
| [LTM implementation notes](docs/LTM-IMPLEMENTATION.md) | Current Giraph LTM code and design |
| [Daily startup](docs/DAILY-STARTUP.md) | Start/stop Hadoop and run jobs |
| [Setup](setup/) | Fresh-machine Hadoop/Giraph installation guides |
| [Datasets](datasets/) / [results](results/) | Inputs and saved outputs |
| [Weekly reports](weekly-reports/) | Chronology of work — update whenever a task finishes |

## Team (Group B)

| Enrolment | Name |
|---|---|
| 25MCMC37 | Aman Choudhary |
| 25MCMC33 | Suyash Singh |

## Faculty supervisor

**Dr. Anjeneya Swami Kare**  
Assistant Professor, SCIS, University of Hyderabad  
https://scis.uohyd.ac.in/~askcs/

## Verified local environment (this PC)

- Windows host + **WSL2 Ubuntu 24.04** (`amani` @ `HISPAT1999`)
- OpenJDK **8** used for Giraph jobs (`1.8.0_492`)
- Apache Hadoop under `~/hadoop` (pseudo-distributed HDFS + YARN)
- Apache Giraph **1.4.0-SNAPSHOT** source under `~/giraph` (pinned Apache history)
- Custom LTM code lives in  
  `~/giraph/giraph-examples/src/main/java/org/apache/giraph/examples/LinearThresholdComputation.java`
- This GitHub folder (`D:\LTM`) is the clean project mirror; WSL is where we build and run

## What is already implemented in WSL

- `LinearThresholdComputation` — first LTM draft (fixed seed `1`, fixed threshold `0.5`)
- `MySSSPComputation` — custom single-source shortest paths
- Weighted four-node LTM sample graph in `datasets/ltm-four-node/`
- Built examples JAR present under `~/giraph/giraph-examples/target/`

## Repository layout

- `src/` — project Giraph computation classes (synced from WSL)
- `datasets/` — graph inputs
- `scripts/` — environment checks and job helpers
- `setup/` — installation helpers (same style as the shared lab setup)
- `docs/` — project and operating guides
- `results/`, `weekly-reports/`, `study-material/` — evidence and notes

## Important

This repository does **not** store passwords, SSH keys, live HDFS data, Maven `target/` trees, or generated JARs. Build and run inside WSL.

GitHub remote: https://github.com/amanpatel-dev/ltm
