# Results — 16-node paper LTM preview

Local Python round preview for [`datasets/ltm-16-node-paper/`](../../datasets/ltm-16-node-paper/).

Full SS0–SS5 weight/credence narrative:  
[`study-material/LTM-16-NODE-ROUND-WALKTHROUGH.md`](../../study-material/LTM-16-NODE-ROUND-WALKTHROUGH.md)

## Round pictures

See [`round-preview/`](round-preview/):

| File | Meaning |
|---|---|
| `round-00.png` | Seed 1 sends (send-only) |
| `round-01.png` | Newly: 2, 3, 4, 16 |
| `round-02.png` | Send-only from {2, 3, 4, 16} |
| `round-03.png` | Newly: 5, 6, 9, 11, 12, 15 |
| `round-04.png` | Send-only from {5, 6, 9, 11, 12, 15} |
| `round-05.png` | Newly: 7, 8, 10, 13, 14 (all 16 active) |
| `expected.tsv` | Per-vertex activation round |
| `summary.txt` | Text summary of each frame |

**Labels:** node = `id` + `θ`; edge = influence weight.  
**Colours:** grey = inactive, orange = newly active, red = already active.

Regenerate:

```bash
python scripts/show_ltm_rounds.py datasets/ltm-16-node-paper/graph.txt \
  --out results/ltm-16-node-paper/round-preview --png
```

Giraph HDFS output for this dataset is not stored here yet.
