# 16-node LTM sample (paper-style input)

Teaching/demo graph for `V2LtmTextInputFormat` / `V2LinearThresholdComputation`.

| Setting | Value |
|---|---|
| Nodes | 16 |
| Seeds | **1** (node `1`) |
| Threshold θ | `0.5` for every node |
| Weights | **mixed** (about 0.2–0.7); seed→{2,3,4} are 0.6 / 0.55 / 0.5 so the cascade can ignite |
| Topology | undirected (each link stored both ways): ring + chords + a few long-range edges |

## Files

| File | Purpose |
|---|---|
| `graph.txt` | Giraph / HDFS input (paper records) |
| `expected-activate-round.tsv` | Local Python oracle matching Giraph send→next-superstep receive timing |

## Line format

```text
nodeId<TAB>B;credence nb1:w1 nb2:w2 ...;beliefChanged;accWeight
```

## Expected activation rounds (oracle)

Giraph delivers messages one superstep later, and newly activated nodes send on the **following** superstep (`beliefChanged`). So new activations appear on odd supersteps here:

| activateRound | Vertices |
|---|---|
| 0 | 1 (seed) |
| 1 | 2, 3, 4, 16 |
| 3 | 5, 6, 9, 11, 12, 15 |
| 5 | 7, 8, 10, 13, 14 |

All **16** vertices become active. Full table: `expected-activate-round.tsv`.

## Run

```bash
hdfs dfs -mkdir -p /user/amani/ltm/v2/input16
hdfs dfs -put -f /mnt/d/LTM/datasets/ltm-16-node-paper/graph.txt /user/amani/ltm/v2/input16/
bash /mnt/d/LTM/scripts/run_ltm_v2.sh \
  /user/amani/ltm/v2/input16 \
  /user/amani/ltm/v2/output16
hdfs dfs -cat /user/amani/ltm/v2/output16/*
```

Compare `activateRound` column to `expected-activate-round.tsv`.
