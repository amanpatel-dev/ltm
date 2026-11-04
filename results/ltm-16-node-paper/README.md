# Results — 16-node paper LTM preview

Local Python round preview for [`datasets/ltm-16-node-paper/`](../../datasets/ltm-16-node-paper/).

## Round pictures

See [`round-preview/`](round-preview/):

| File | Meaning |
|---|---|
| `round-00.png` | Seed 1 active |
| `round-01.png` | Newly: 2, 3, 4, 16 |
| `round-03.png` | Newly: 5, 6, 9, 11, 12, 15 |
| `round-05.png` | Newly: 7, 8, 10, 13, 14 (all 16 active) |
| `expected.tsv` | Per-vertex activation round |
| `summary.txt` | Text summary of each frame |

Colours: grey = inactive, orange = newly active, red = already active.

Regenerate:

```bash
python scripts/show_ltm_rounds.py datasets/ltm-16-node-paper/graph.txt \
  --out results/ltm-16-node-paper/round-preview --png
```

Giraph HDFS output for this dataset is not stored here yet.
