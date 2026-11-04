# Four-node LTM sample (paper-style input)

Same topology and weights as `datasets/ltm-four-node/`, encoded in Sir’s LTM record format for `V2LtmTextInputFormat` / `V2LinearThresholdComputation`.

## Line format

```text
nodeId<TAB>B;credence nb1:w1 nb2:w2 ...;beliefChanged;accWeight
```

## This graph

| Node | Role | θ | Notes |
|---|---|---|---|
| 1 | seed | 0.5 | `B=1`, `beliefChanged=true`, `activateRound` starts at 0 |
| 2 | inactive | 0.5 | receives 0.6 from 1 in round 1 |
| 3 | inactive | 0.5 | receives 0.5 from 1 in round 1 |
| 4 | inactive | 0.5 | receives 0.5+0.4 from 2 and 3 in round 2 |

Expected activation rounds (hand check): seed `1` at 0; `2` and `3` at superstep 1; `4` at superstep 2.
