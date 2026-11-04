# Faculty brief — Group B Linear Threshold Model (LTM) on Apache Giraph

Use this note when explaining the implementation to **Dr. Anjeneya Swami Kare**. It maps theory → code → demo files.

**Repository:** https://github.com/amanpatel-dev/ltm  
**Local mirror:** `D:\LTM`

## 1. Project context

| Item | Detail |
|---|---|
| Title | Implementation of Distributed Graph Algorithms for Social Network Problems Using Apache Giraph Framework |
| Group B | Aman Choudhary (25MCMC37), Suyash Singh (25MCMC33) |
| Focus | **Linear Threshold Model (LTM)** influence diffusion |
| Group A (separate repo) | Graph Burning |

Shared foundations (Hadoop, Giraph, basic graph jobs) are the same idea as Group A; the research algorithm and experiments differ.

## 2. What LTM means in our work

- Each node is inactive (`B = -1`) or active (`B = 1`).
- Each node has a threshold **θ** stored as **`credence`**.
- Each edge has an influence **weight** `w`.
- An inactive node becomes active when **`accWeight` (sum of influence received so far) ≥ credence**.
- Only **newly activated** nodes send influence next (`beliefChanged = true`). This matches Sir’s MapReduce LTM design and avoids re-adding the same neighbour weight every round.

Related theory note: [swami-sir-paper-ltm-input-design.md](swami-sir-paper-ltm-input-design.md).

## 3. What we implemented (file map)

### Kept as teaching draft (v1) — unchanged

| File | Role |
|---|---|
| [src/LinearThresholdComputation.java](../src/LinearThresholdComputation.java) | Early LTM: hard-coded seed `1` and threshold `0.5` |
| [datasets/ltm-four-node/graph.txt](../datasets/ltm-four-node/graph.txt) | Simple `id nb:w` lines (not paper records) |
| [scripts/run_ltm.sh](../scripts/run_ltm.sh) | Runs v1 |

### Paper-style implementation (v2) — current demo target

| File | Role |
|---|---|
| [src/V2LtmVertexValue.java](../src/V2LtmVertexValue.java) | Stores `B`, `credence`, `beliefChanged`, `accWeight`, `activateRound` |
| [src/V2LtmTextInputFormat.java](../src/V2LtmTextInputFormat.java) | Parses Sir-style input lines into Giraph vertices |
| [src/V2LinearThresholdComputation.java](../src/V2LinearThresholdComputation.java) | Superstep LTM logic on Giraph |
| [datasets/ltm-four-node-paper/](../datasets/ltm-four-node-paper/) | Tiny 4-node paper input |
| [datasets/ltm-16-node-paper/](../datasets/ltm-16-node-paper/) | 16-node demo (1 seed, mixed weights) |
| [scripts/run_ltm_v2.sh](../scripts/run_ltm_v2.sh) | Submits v2 Giraph job |
| [scripts/show_ltm_rounds.py](../scripts/show_ltm_rounds.py) | Local round simulator + PNG preview |

v1 was **not** deleted so we can show progression: hard-coded draft → paper-aligned input.

## 4. Paper-style input (where the threshold is)

One line per node (tab after the id):

```text
nodeId<TAB>B;credence nb1:w1 nb2:w2 ...;beliefChanged;accWeight
```

Example seed line from the 16-node dataset:

```text
1	1;0.5 2:0.6 3:0.55 4:0.5 ...;true;0.0
```

| Piece | Meaning |
|---|---|
| `1` (id) | Vertex id |
| `B = 1` | Already active (seed) |
| `credence = 0.5` | **Threshold θ** |
| `2:0.6 3:0.55 ...` | Out-neighbours and influence weights |
| `beliefChanged = true` | This node sends influence in the first send phase |
| `accWeight = 0.0` | No pressure collected yet |

Inactive nodes use `B = -1` and `beliefChanged = false`. In the 16-node demo, **every** node has θ = `0.5`; edge weights are mixed.

## 5. How v2 runs in Giraph (short walkthrough)

```text
Superstep S
  1. If beliefChanged: send each outgoing edge weight as a message; then clear the flag
  2. Add all incoming messages into accWeight (messages sent in S-1 arrive in S)
  3. If inactive and accWeight >= credence: set B=1, beliefChanged=true, activateRound=S
  4. Halt only if beliefChanged is false (so a newly activated node runs again to send)
```

**Output value columns** (after `IdWithValueTextOutputFormat`):

```text
B  activateRound  credence  accWeight  beliefChanged
```

`activateRound` is **not** in the input file; the computation records it (seed starts at `0`).

## 6. Ordered demo for Sir (16-node)

1. **Input file**  
   [datasets/ltm-16-node-paper/graph.txt](../datasets/ltm-16-node-paper/graph.txt)  
   Point out seed line, `0.5` threshold, mixed weights.

2. **Round pictures** (local Python oracle / visualization)  
   Folder: [results/ltm-16-node-paper/round-preview/](../results/ltm-16-node-paper/round-preview/)

   | File | What to say |
   |---|---|
   | `round-00.png` | Only seed **1** active |
   | `round-01.png` | Newly active: **2, 3, 4, 16** |
   | `round-03.png` | Newly: **5, 6, 9, 11, 12, 15** |
   | `round-05.png` | Newly: **7, 8, 10, 13, 14** — all 16 active |

   Colours: **grey** inactive, **orange** newly active this superstep, **red** already active.

3. **Expected activation table**  
   [datasets/ltm-16-node-paper/expected-activate-round.tsv](../datasets/ltm-16-node-paper/expected-activate-round.tsv)  
   Same numbers as `round-preview/expected.tsv` (local check matched).

4. **Code**  
   Open `V2LtmTextInputFormat` (input), `V2LinearThresholdComputation` (rules), `V2LtmVertexValue` (state + output).

5. **Optional live Giraph** (when Hadoop is up)  
   Rebuild examples JAR, upload `graph.txt` to HDFS, run `scripts/run_ltm_v2.sh`, compare `activateRound` to the expected table.  
   Status: **v2 code and local preview are ready; a saved HDFS TSV for the 16-node job may still be pending.**

## 7. Honest limits

- v1 is a simplified draft (seed/θ in Java), not the paper record format.
- Round PNGs are produced by **local Python** with the same timing rules as our Giraph v2 design; they are **not** screenshots from YARN.
- We do **not** claim LiveJournal-scale results or influence-maximization optimality yet—only correct LTM **simulation** on teaching graphs, with a path to Sir’s input design.

## 8. One-sentence summary for the meeting

Group B implemented a Giraph LTM that reads Sir’s per-node record (state, threshold, weights, belief flag, accumulated weight), records each vertex’s activation round, and demonstrates a 16-node cascade with input file, expected table, and round-by-round pictures.
