# Round preview frames

| Colour / label | Meaning |
|---|---|
| Grey | Inactive |
| Orange | Newly activated this superstep |
| Red | Already active |
| Node text | `id` and threshold `θ` |
| Edge text | Influence weight |

Open frames in order. Even-numbered frames after the seed are **send-only** (messages leave; activations happen next step):

| File | Kind |
|---|---|
| `round-00.png` | Seed sends |
| `round-01.png` | Activate |
| `round-02.png` | Send-only |
| `round-03.png` | Activate |
| `round-04.png` | Send-only |
| `round-05.png` | Activate (cascade complete) |

Weight-by-weight explanation:  
[`study-material/LTM-16-NODE-ROUND-WALKTHROUGH.md`](../../../study-material/LTM-16-NODE-ROUND-WALKTHROUGH.md)
