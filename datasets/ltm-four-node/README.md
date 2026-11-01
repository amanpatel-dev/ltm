# Four-node weighted LTM sample

Source: `/home/amani/ltm/graph.txt` on the WSL development PC.

```text
1 2:0.6 3:0.5
2 1:0.6 4:0.5
3 1:0.5 4:0.4
4 2:0.5 3:0.4
```

Undirected-looking mutual edges with influence weights in `[0.4, 0.6]`. Used to exercise `LinearThresholdComputation` with global threshold `0.5` and seed vertex `1`.

Expected activation behaviour must be hand-checked and recorded under `results/` once the job command is finalized.
