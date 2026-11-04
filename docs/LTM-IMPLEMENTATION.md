# LTM implementation notes

## Model reminder

In the **Linear Threshold Model**:

- Each vertex is inactive or active.
- Each inactive vertex `v` has a threshold `θ(v)` (`credence` in the paper record).
- Each edge `u → v` carries an influence weight `w(u,v)`.
- Vertex `v` activates when the **accumulated weight from active neighbours ≥ θ(v)**.
- Only **newly activated** vertices send influence (`beliefChanged`).
- `accWeight` persists across rounds until activation.

## Two implementations in this repo

| | v1 draft | v2 paper-style |
|---|---|---|
| Computation | `LinearThresholdComputation` | `V2LinearThresholdComputation` |
| Input format | `LongDoubleFloatTextInputFormat` | `V2LtmTextInputFormat` |
| Vertex value | `DoubleWritable` (0/1) | `V2LtmVertexValue` |
| Seed / θ | Hard-coded in Java | In the input file |
| Activation round | Not stored | `activateRound` field |
| Sample data | `datasets/ltm-four-node/` | `datasets/ltm-four-node-paper/` |
| Run script | `scripts/run_ltm.sh` | `scripts/run_ltm_v2.sh` |

**Do not modify v1.** It remains the early teaching draft.

## File locations

| Role | Repo path | WSL path (after sync) |
|---|---|---|
| v1 LTM | `src/LinearThresholdComputation.java` | `~/giraph/giraph-examples/.../LinearThresholdComputation.java` |
| v2 LTM | `src/V2LinearThresholdComputation.java` | same `examples/` package |
| v2 vertex value | `src/V2LtmVertexValue.java` | same |
| v2 input format | `src/V2LtmTextInputFormat.java` | same |
| Custom SSSP | `src/MySSSPComputation.java` | same |

## v2 paper input line

```text
nodeId<TAB>B;credence nb1:w1 nb2:w2 ...;beliefChanged;accWeight
```

Example seed:

```text
1	1;0.5 2:0.6 3:0.5;true;0.0
```

| Field | Meaning |
|---|---|
| `B` | `1` active / `-1` inactive |
| `credence` | threshold θ |
| `nb:w` | outgoing influence edges |
| `beliefChanged` | send influence this superstep if true |
| `accWeight` | influence collected so far |

`activateRound` is **not** in the input file. The computation sets it: seeds `0`, others the activating superstep, never activated `-1`.

## v2 compute behaviour

1. If `beliefChanged`, send each outgoing edge weight as a `DoubleWritable` message, then clear the flag.
2. Add all incoming message weights into `accWeight`.
3. If inactive and `accWeight >= credence`, set `B=1`, `beliefChanged=true`, `activateRound=currentSuperstep`.
4. Halt only when `beliefChanged` is false (so a newly activated vertex runs again next superstep to send).

## v2 output columns

With `IdWithValueTextOutputFormat`, each line is:

```text
vertexId	B activateRound credence accWeight beliefChanged
```

## Hand-checked expectation (four-node paper sample)

| Vertex | activateRound |
|---|---|
| 1 | 0 (seed) |
| 2 | 1 |
| 3 | 1 |
| 4 | 2 |

## Rebuild after editing v2 Java in WSL

```bash
cd ~/giraph
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
mvn -pl giraph-examples -am -DskipTests package
```
