# Swami Sir paper — LTM input design (study note)

**Paper:** Umar Bin Mushtaq, Damerla Anand, and Anjeneya Swami Kare.  
*A MapReduce Framework for Information Diffusion Models and Component-Aware Influence Maximization in Social Networks.*  
SCIS, University of Hyderabad.

**Local scan:** `c:\Users\amani\Desktop\project\image to png\Swami sir research paper.pdf`  
(Scanned PDF; text extracted via page images for this note.)

This note records only what Group B needs for **Linear Threshold Model (LTM)** simulation input and round logic. ICM/SIR are mentioned only for contrast.

---

## 1. What the paper is doing

- Implement three classical diffusion simulators on **Hadoop MapReduce**: LTM, ICM, SIR.
- One **diffusion round = one MapReduce job**.
- A driver chains jobs, keeps **full node state on HDFS**, stops using **Hadoop counters**.
- Evaluated on **LiveJournal** (after prep ≈ 3.99M nodes, 34.7M undirected edges).
- Important LTM finding: **edge-weight distribution affects spread more than seed count**.

Our project keeps the **same LTM semantics**, but runs them on **Apache Giraph** (vertex-centric, in-memory messages) instead of MapReduce disk rounds. The paper itself points at Pregel/Giraph as a natural fit for iterative graph work.

---

## 2. LTM semantics (from the paper)

- Every node has a personal threshold **θ** (`credence` in their record).
- Every edge has an influence weight **w**.
- A node becomes active when the **accumulated weight from already-active neighbors ≥ θ**.
- Influence is **deterministic** once weights and thresholds are fixed.
- **Only newly activated** nodes send influence in the next round (avoids double-counting).
- Accumulated weight **persists across rounds** until activation.

---

## 3. LTM input / record design (critical)

Section 3.1 defines **one HDFS record per node**, tab-separated:

```text
<nodeId>\tB;credence nb1:w1 nb2:w2 ...;beliefChanged;accWeight
```

| Field | Meaning |
|---|---|
| `nodeId` | Key |
| `B` | Activation state: `-1` inactive, `1` active |
| `credence` | Node threshold θ |
| `nb1:w1 nb2:w2 ...` | Out-neighbours with edge weights |
| `beliefChanged` | Whether this node became active in the **previous** round |
| `accWeight` | Accumulated influence so far (kept across rounds) |

**Same format is written as output** so the next round can read it again.

### Preprocessing (Section 4.2)

From SNAP edge-list pairs, a Python prep script:

1. Build adjacency list; undirected edges stored **both ways**; drop self-loops.
2. Choose seeds (strategy-dependent).
3. Emit one record per node with state, θ, weights.
4. Verify seed vs non-seed line counts.

Experimental LTM defaults used in the paper:

- Global threshold **θ = 0.5** (same for all nodes in those runs).
- Edge weights either **fixed w = 0.3** or **random w ~ U[0.1, 0.8]**.
- Max iterations for LTM: **20**.
- PRNG seed for sampling: **42**.

With `w = 0.3` and `θ = 0.5`, a node needs **at least two** active neighbours to activate.

---

## 4. One LTM round (Map / Reduce → Giraph)

| MapReduce piece | Behaviour | Giraph mapping |
|---|---|---|
| Mapper | Re-emit node record; if `beliefChanged`, send `DIFFUSION` + edge weight to each neighbour | Active/newly-active vertex `sendMessage` with weight |
| Reducer | Add incoming weights into `accWeight`; if inactive and `accWeight ≥ θ`, activate, set `beliefChanged`, bump counter | In `compute()`, sum messages into stored `accWeight`, then threshold check |
| Driver stop | Stop when belief-change counter is 0 (or max iters) | Aggregator counting new activations; halt when 0 |

Table 1 (paper): LTM mapper sends weight; reducer **sums** weights; state kept across rounds; deterministic; counter = belief changes.

---

## 5. Gap vs v1 draft (still kept)

`LinearThresholdComputation` (v1) remains unchanged:

- Hard-coded seed `1` and threshold `0.5`.
- Vertex value is only `0.0 / 1.0`.
- Sample `datasets/ltm-four-node/graph.txt` is only `id nb:w ...`.

## 6. V2 Giraph classes (paper-aligned)

Implemented in this repository (and synced into WSL `giraph-examples`):

| Class | Role |
|---|---|
| `V2LtmVertexValue` | Writable: `B`, `credence`, `beliefChanged`, `accWeight`, `activateRound` |
| `V2LtmTextInputFormat` | Parses the paper tab/semicolon line |
| `V2LinearThresholdComputation` | LTM round logic; only `beliefChanged` vertices send |

Sample input: `datasets/ltm-four-node-paper/graph.txt`  
Runner: `scripts/run_ltm_v2.sh`  
Details: `docs/LTM-IMPLEMENTATION.md`

## 7. Still ahead

1. Run v2 on HDFS and save `results/` output.
2. Add a small Python round preview / oracle for the four-node graph.
3. Later LiveJournal-scale SNAP prep (paper §4.2 style).
