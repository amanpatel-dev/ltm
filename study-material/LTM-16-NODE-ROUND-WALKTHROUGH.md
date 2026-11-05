# 16-node LTM — full round-by-round walkthrough

This note explains **every Giraph superstep** for the demo graph  
[`datasets/ltm-16-node-paper/graph.txt`](../datasets/ltm-16-node-paper/graph.txt).

Matching pictures (with edge weights and θ on nodes):  
[`results/ltm-16-node-paper/round-preview/`](../results/ltm-16-node-paper/round-preview/)

## Setup (before any superstep)

| Setting | Value |
|---|---|
| Nodes | 1 … 16 |
| Seed | **1** (`B=1`, `beliefChanged=true`, `activateRound=0`) |
| Threshold θ (`credence`) | **0.5 for every node** |
| Starting `accWeight` | **0.0** for all |
| Rule | Activate when `accWeight ≥ 0.5`. Only `beliefChanged` nodes send. Messages sent in superstep **S** are received in **S+1**. |

Undirected edges are stored both ways with the same weight.

## Legend for pictures

| Colour | Meaning |
|---|---|
| Grey | Inactive |
| Orange | Newly activated this superstep |
| Red | Already active |
| Node text | `id` and `θ=0.5` |
| Edge text | influence weight `w` |

---

## Superstep 0 — seed sends (no new activations)

**Active:** `{1}`  
**Senders:** `{1}`  
**Picture:** `round-00.png`

Seed 1 sends these weights (arrive in SS1):

| To | Weight | Crosses θ=0.5 alone? |
|---|---:|---|
| 2 | 0.60 | yes |
| 3 | 0.55 | yes |
| 4 | 0.50 | yes (exact) |
| 5 | 0.40 | no |
| 6 | 0.45 | no |
| 15 | 0.45 | no |
| 16 | 0.55 | yes |

After sending, seed clears `beliefChanged`. Nobody receives yet in SS0, so **newly active = {}**.

---

## Superstep 1 — first activation wave

**Senders:** none (nobody still has `beliefChanged` at the start)  
**Receive:** messages from SS0  
**Picture:** `round-01.png`

| Node | Incoming this step | New `accWeight` | vs θ=0.5 | Result |
|---|---:|---:|---|---|
| 2 | 0.60 from 1 | 0.60 | ≥ | **activate** |
| 3 | 0.55 from 1 | 0.55 | ≥ | **activate** |
| 4 | 0.50 from 1 | 0.50 | ≥ | **activate** |
| 16 | 0.55 from 1 | 0.55 | ≥ | **activate** |
| 5 | 0.40 from 1 | 0.40 | < | stay inactive |
| 6 | 0.45 from 1 | 0.45 | < | stay inactive |
| 15 | 0.45 from 1 | 0.45 | < | stay inactive |

**Newly active:** `{2, 3, 4, 16}` with `activateRound=1`, `beliefChanged=true` (they will send in SS2).  
**Active now:** `{1, 2, 3, 4, 16}`

---

## Superstep 2 — send only (no new activations)

**Senders:** `{2, 3, 4, 16}`  
**Receive mailbox:** empty (nobody sent in SS1)  
**Picture:** `round-02.png`

These nodes push influence (selected outs that matter for later activations):

| From | Important outs |
|---|---|
| 2 | 8:0.35, 16:0.50, … |
| 3 | **5:0.70**, **9:0.60**, … |
| 4 | **5:0.65**, **6:0.65**, 10:0.25, … |
| 16 | **11:0.55**, **12:0.55**, 14:0.35, **15:0.30**, … |

No inactive node receives anything **in this same superstep**, so colours match end of SS1 except the title says send-only. Messages sit in the mailbox for SS3.

---

## Superstep 3 — second activation wave

**Senders:** none at start  
**Receive:** SS2 messages  
**Picture:** `round-03.png`

Focus on nodes that cross θ now (prior `accWeight` from SS1 included):

| Node | Prior acc | Incoming this step (main sources) | New acc | Result |
|---|---:|---|---:|---|
| 5 | 0.40 | 0.70 (from 3) + 0.65 (from 4) = **1.35** | 1.75 | **activate** |
| 6 | 0.45 | 0.65 (from 4) | 1.10 | **activate** |
| 9 | 0.00 | 0.60 (from 3) | 0.60 | **activate** |
| 11 | 0.00 | 0.55 (from 16) | 0.55 | **activate** |
| 12 | 0.00 | 0.55 (from 16) | 0.55 | **activate** |
| 15 | 0.45 | 0.30 (from 16) | 0.75 | **activate** |
| 8 | 0.00 | 0.35 (from 2) | 0.35 | still inactive |
| 10 | 0.00 | 0.25 (from 4) | 0.25 | still inactive |
| 14 | 0.00 | 0.35 (from 16) | 0.35 | still inactive |

**Newly active:** `{5, 6, 9, 11, 12, 15}` (`activateRound=3`)  
**Active now:** 11 nodes

---

## Superstep 4 — send only again

**Senders:** `{5, 6, 9, 11, 12, 15}`  
**Receive:** empty  
**Picture:** `round-04.png`

Important outs toward the last inactive set `{7, 8, 10, 13, 14}`:

| From | Toward last wave |
|---|---|
| 5 | **7:0.50** |
| 6 | **7:0.55**, **8:0.70** |
| 9 | **7:0.55**, 8:0.25, 10:0.35, **14:0.70** |
| 11 | 10:0.45, **13:0.60** |
| 12 | **10:0.60**, **13:0.50**, **14:0.60** |
| 15 | **10:0.60**, 13:0.30, **14:0.50** |

No new activations this superstep; messages wait for SS5.

---

## Superstep 5 — final activation wave

**Receive:** SS4 messages  
**Picture:** `round-05.png`

| Node | Prior acc | Incoming this step | New acc | Result |
|---|---:|---:|---:|---|
| 7 | 0.00 | 0.50+0.55+0.55 = **1.60** (from 5,6,9) | 1.60 | **activate** |
| 8 | 0.35 | **0.95** (0.70 from 6 + 0.25 from 9) | 1.30 | **activate** |
| 10 | 0.25 | **2.00** (0.35+0.45+0.60+0.60 from 9,11,12,15) | 2.25 | **activate** |
| 13 | 0.00 | **1.40** (0.60+0.50+0.30 from 11,12,15) | 1.40 | **activate** |
| 14 | 0.35 | **1.80** (0.70+0.60+0.50 from 9,12,15) | 2.15 | **activate** |

**Newly active:** `{7, 8, 10, 13, 14}` (`activateRound=5`)  
**Active now:** **all 16**

Later supersteps (6–7) only add more weight among already-active nodes; the cascade is finished for activation.

---

## Final activation rounds

| activateRound | Vertices |
|---|---|
| 0 | 1 (seed) |
| 1 | 2, 3, 4, 16 |
| 3 | 5, 6, 9, 11, 12, 15 |
| 5 | 7, 8, 10, 13, 14 |

Full numeric table: [`datasets/ltm-16-node-paper/expected-activate-round.tsv`](../datasets/ltm-16-node-paper/expected-activate-round.tsv)

## Why there are “empty” activation rounds (2 and 4)

Giraph timing + `beliefChanged`:

1. Activate and mark “send next”  
2. **Next** superstep: send weights  
3. **Following** superstep: neighbours receive and maybe activate  

So waves of **new orange nodes** appear on SS1, SS3, SS5; SS2 and SS4 are the **send** halves of those waves.

## Regenerate pictures

```bash
python scripts/show_ltm_rounds.py datasets/ltm-16-node-paper/graph.txt \
  --out results/ltm-16-node-paper/round-preview --png
```
