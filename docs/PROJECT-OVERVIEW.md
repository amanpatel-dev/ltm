# Project Overview — Group B (LTM)

## Academic context

- **Programme:** Master of Computer Applications (MCA)
- **Institution:** University of Hyderabad
- **School:** School of Computer and Information Sciences (SCIS)
- **Semester focus:** Semester III project work (continuous through May 2028)
- **Total project credits:** 12 (6 internal + 6 external, as recorded for the cohort)
- **Internal evaluation window:** late November / early December 2026 (official notice remains authoritative)
- **External evaluation:** May 2028 (official notice remains authoritative)

## Faculty supervisor

**Dr. Anjeneya Swami Kare**  
Assistant Professor  
School of Computer and Information Sciences  
University of Hyderabad  

Faculty page: https://scis.uohyd.ac.in/~askcs/

Research interests relevant to this project include graph algorithms and social-network analysis.

## Team structure

All four students share the broad project title. Work is organized into two implementation groups.

### Group A (Graph Burning)

| Enrolment | Name |
|---|---|
| 25MCMC34 | Kumar Shivam |
| 25MCMC13 | Archana Kumari |

Reference repository: https://github.com/25mcmc34-kumarshivam/APACHE-GIRAPH-PROJECT-

### Group B (Linear Threshold Model) — this repository

| Enrolment | Name |
|---|---|
| 25MCMC37 | Aman Choudhary |
| 25MCMC33 | Suyash Singh |

This repository: https://github.com/amanpatel-dev/ltm

## Project title and problem statement

- **Approved title:** Implementation of Distributed Graph Algorithms for Social Network Problems Using Apache Giraph Framework
- **Group B focus:** Linear Threshold Model (LTM) influence diffusion on Giraph
- **Shared foundation with Group A:** Linux/WSL, Java 8, Hadoop HDFS/YARN, Giraph build, basic graph algorithms, documentation discipline
- **Divergence:** Group A simulates Graph Burning sequences; Group B implements and evaluates LTM activation under edge influence weights and thresholds

**Problem statement:** Implement algorithms for social-network problems using the Apache Giraph framework, which follows a vertex-centric, massively scalable approach to graph processing.

## Weekly supervision meeting

- **Day:** Thursday
- **Time:** 9:00 AM
- **Location:** Supervisor's chamber
- **Supervisor:** Dr. Anjeneya Swami Kare

## Current status (Group B)

| Area | Status |
|---|---|
| Personal PC WSL Ubuntu + Hadoop + Giraph | Installed and reachable |
| Basic algorithms (PageRank/degree/SSSP examples) | Available in Giraph examples; custom `MySSSPComputation` present |
| LTM first Giraph draft | `LinearThresholdComputation` present (fixed seed/threshold) |
| Project GitHub mirror | This repository |
| Configurable seeds/thresholds, round tracking, validation scripts | Next implementation work |
| Experiment datasets and recorded results | Starting from the four-node weighted sample |

## Evidence expected for evaluation

- Git commit history on this repository
- Weekly meeting reports
- Installation / WSL workflow notes
- Source code and scripts
- Dataset descriptions
- Expected vs actual LTM outputs on small graphs
- Algorithm explanation and literature notes
- Limitations and future work
