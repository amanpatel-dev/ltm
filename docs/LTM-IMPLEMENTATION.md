# LTM implementation notes (current WSL code)

## Model reminder

In the **Linear Threshold Model**:

- Each vertex is inactive or active.
- Each inactive vertex `v` has a threshold `θ(v)` (our draft uses a global `0.5`).
- Each edge `u → v` carries an influence weight `w(u,v)`.
- Vertex `v` activates when the **sum of weights from currently active in-neighbours** is at least `θ(v)`.
- Newly active vertices may influence neighbours in later Giraph supersteps.

This is different from Graph Burning (scheduled fire sources) and from Independent Cascade (one probabilistic attempt per new activation).

## File locations

| Role | Path in WSL | Path in this repo |
|---|---|---|
| LTM computation | `~/giraph/giraph-examples/.../LinearThresholdComputation.java` | `src/LinearThresholdComputation.java` |
| Custom SSSP | `~/giraph/.../MySSSPComputation.java` | `src/MySSSPComputation.java` |
| Text input format (id / value / weighted edges) | Giraph examples + repo copy | `src/LongDoubleFloatTextInputFormat.java` |
| Weighted sample | `~/ltm/graph.txt` | `datasets/ltm-four-node/graph.txt` |

## Current draft behaviour

`LinearThresholdComputation` (as synced from WSL on 2026-11-01):

1. **Superstep 0:** vertex `SOURCE_ID = 1` becomes active (`value = 1.0`); others start at `0.0`. The seed sends its outgoing edge weights as text messages.
2. **Later supersteps:** inactive vertices sum incoming influence messages. If `sum >= THRESHOLD` (`0.5`), they activate and forward their own outgoing edge weights.
3. Already-active vertices halt without resending.

### Limits of this draft (known)

- Threshold and seed are **hard-coded**, not `-ca` options.
- Vertex value stores only active/inactive (`1.0` / `0.0`), not activation round.
- Influence is taken from **outgoing** edge weights on the sender; input graph orientation must match the intended influence direction.
- No aggregator yet for cascade size / rounds.
- Not yet compared against a local Python oracle on the four-node sample.

These are the natural next implementation tasks.

## Four-node sample graph

`datasets/ltm-four-node/graph.txt` uses a weighted adjacency text form:

```text
1 2:0.6 3:0.5
2 1:0.6 4:0.5
3 1:0.5 4:0.4
4 2:0.5 3:0.4
```

Meaning: vertex id, then neighbour:weight pairs. Exact Giraph input-format wiring for this file must match whatever reader we bind in the job command (see `scripts/run_ltm.sh` as it evolves).

## Related foundational class

`MySSSPComputation` is our custom shortest-path computation (edge weights as distances). It is kept here as part of the shared-algorithm learning path; LTM work does not replace it.
