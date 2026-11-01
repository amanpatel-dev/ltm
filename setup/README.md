# Setup

Group B uses the **same installation style** as the shared cohort guides (Java 8, Hadoop 2.7.7-class single-node layout, Giraph 1.4.0-SNAPSHOT), but on a **personal Windows PC with WSL2 Ubuntu** instead of the lab shared accounts.

## Choose a guide

- On **this PC**, day-to-day work follows [docs/WSL-WORKFLOW.md](../docs/WSL-WORKFLOW.md) and [docs/DAILY-STARTUP.md](../docs/DAILY-STARTUP.md). Hadoop is already under `~/hadoop` and Giraph under `~/giraph` for user `amani`.
- For a **fresh Ubuntu machine** using one login account, use [UBUNTU-22.04-SINGLE-ACCOUNT.md](UBUNTU-22.04-SINGLE-ACCOUNT.md) (shared cohort procedure; clone **this** LTM repo instead of Group A’s Graph Burning repo).
- For a **two-account shared server** (`hduser` + developer), use [UBUNTU-22.04-FROM-SCRATCH.md](UBUNTU-22.04-FROM-SCRATCH.md).

## Included helpers

- `config/` — sanitized Hadoop XML (includes copies taken from this WSL PC for documentation)
- `patches/` — Guava shading patch used by the pinned Giraph revision
- `maven-settings-https.xml` — HTTPS Maven Central mirror for archived Giraph POMs
- `compat/kill` — NodeManager workaround for older Hadoop on newer Ubuntu
- `single-account/` — one-user environment helper scripts

Do not store passwords, SSH keys, live HDFS data, or machine-specific secrets here.
