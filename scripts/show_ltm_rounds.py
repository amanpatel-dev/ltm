#!/usr/bin/env python3
"""Preview Linear Threshold Model rounds for paper-style Giraph input.

Reads the same line format as V2LtmTextInputFormat:

    nodeId<TAB>B;credence nb1:w1 nb2:w2 ...;beliefChanged;accWeight

Simulation matches V2LinearThresholdComputation message timing:
messages sent in superstep S are received in S+1; a vertex that becomes
active sets beliefChanged and sends on its next compute call.

Text outputs need only the standard library. PNG frames need::

    python -m pip install networkx matplotlib
"""

from __future__ import annotations

import argparse
from collections import defaultdict
from pathlib import Path


def parse_paper_ltm(path: Path):
    """Return adj, state, credence, belief, acc from a paper-style graph file."""
    adj = defaultdict(dict)
    state = {}
    credence = {}
    belief = {}
    acc = {}

    with path.open(encoding="utf-8") as handle:
        for raw in handle:
            line = raw.strip()
            if not line or line.startswith("#"):
                continue
            if "\t" in line:
                vid_s, payload = line.split("\t", 1)
            else:
                parts = line.split(None, 1)
                if len(parts) != 2:
                    raise ValueError("bad LTM line: " + line)
                vid_s, payload = parts

            vid = int(vid_s)
            fields = payload.split(";")
            if len(fields) != 4:
                raise ValueError(
                    "expected B;credence[ edges];beliefChanged;accWeight: " + line
                )

            state[vid] = int(fields[0].strip())
            mid = fields[1].strip().split()
            if not mid:
                raise ValueError("missing credence in line: " + line)
            credence[vid] = float(mid[0])
            for token in mid[1:]:
                neighbour_s, weight_s = token.split(":", 1)
                adj[vid][int(neighbour_s)] = float(weight_s)
            belief[vid] = fields[2].strip().lower() == "true"
            acc[vid] = float(fields[3].strip())

    for vid in state:
        adj.setdefault(vid, {})
    return adj, state, credence, belief, acc


def simulate_giraph_ltm(adj, state0, credence, belief0, acc0):
    """Return (frames, activate_round, final_acc).

    Each frame is (superstep, newly_active_list, active_set_after).
    Includes a seed frame at superstep 0, then every superstep with new
    activations.
    """
    vertices = sorted(state0)
    state = dict(state0)
    belief = dict(belief0)
    acc = dict(acc0)
    activate_round = {v: (0 if state[v] == 1 else -1) for v in vertices}

    mailbox = defaultdict(float)
    frames = []
    active0 = {v for v in vertices if state[v] == 1}
    frames.append((0, [], set(active0)))

    for ss in range(0, 100):
        outgoing = defaultdict(float)
        for u in vertices:
            if belief[u]:
                for v, weight in adj[u].items():
                    outgoing[v] += weight
                belief[u] = False

        newly = []
        for v in vertices:
            if mailbox[v] > 0:
                acc[v] += mailbox[v]
            if state[v] == -1 and acc[v] >= credence[v]:
                state[v] = 1
                belief[v] = True
                activate_round[v] = ss
                newly.append(v)

        if newly:
            active = {v for v in vertices if state[v] == 1}
            if ss == 0:
                frames[0] = (0, newly, active)
            else:
                frames.append((ss, newly, active))

        mailbox = outgoing
        if not any(belief.values()) and not any(mailbox.values()):
            break

    return frames, activate_round, acc


def write_text_outputs(out_dir: Path, activate_round, acc):
    out_dir.mkdir(parents=True, exist_ok=True)
    expected = out_dir / "expected.tsv"
    lines = ["vertexId\tactivateRound\tfinalAccWeight\tfinalState\n"]
    for vid in sorted(activate_round):
        final_state = 1 if activate_round[vid] >= 0 else -1
        lines.append(
            "{}\t{}\t{:.4f}\t{}\n".format(
                vid, activate_round[vid], acc[vid], final_state
            )
        )
    expected.write_text("".join(lines), encoding="utf-8")
    return expected


def write_summary(out_dir: Path, frames):
    summary_lines = []
    for ss, newly, active in frames:
        summary_lines.append(
            "superstep {}: newly={} active_count={}\n".format(
                ss, newly, len(active)
            )
        )
    path = out_dir / "summary.txt"
    path.write_text("".join(summary_lines), encoding="utf-8")
    return path


def draw_pngs(out_dir: Path, adj, frames):
    try:
        import matplotlib

        matplotlib.use("Agg")
        import matplotlib.pyplot as plt
        import networkx as nx
    except ImportError as exc:
        raise SystemExit(
            "PNG output needs networkx and matplotlib.\n"
            "Install with: python -m pip install networkx matplotlib\n"
            "Original error: {}".format(exc)
        )

    graph = nx.Graph()
    for u, neighbours in adj.items():
        graph.add_node(u)
        for v in neighbours:
            if u <= v:
                graph.add_edge(u, v)

    pos = nx.spring_layout(graph, seed=42, k=0.9)

    for ss, newly, active in frames:
        newly_set = set(newly)
        earlier = active - newly_set
        colour_map = []
        for node in graph.nodes:
            if node in newly_set:
                colour_map.append("#f09000")
            elif node in earlier:
                colour_map.append("#c02828")
            else:
                colour_map.append("#b0b0b0")

        fig, ax = plt.subplots(figsize=(8, 8))
        nx.draw_networkx_edges(graph, pos, ax=ax, alpha=0.35, width=1.2)
        nx.draw_networkx_nodes(
            graph, pos, ax=ax, node_color=colour_map, node_size=700
        )
        nx.draw_networkx_labels(
            graph, pos, ax=ax, font_size=9, font_color="white"
        )
        if ss == 0 and not newly:
            title = "LTM superstep 0 — seed already active: {}".format(
                sorted(active)
            )
        else:
            title = "LTM superstep {} — newly active: {}".format(ss, newly)
        ax.set_title(title)
        ax.set_axis_off()
        fig.tight_layout()
        out_path = out_dir / "round-{:02d}.png".format(ss)
        fig.savefig(out_path, dpi=140)
        plt.close(fig)
        print("wrote", out_path)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("input", type=Path, help="paper-style LTM graph.txt")
    parser.add_argument(
        "--out",
        type=Path,
        required=True,
        help="output directory for preview files",
    )
    parser.add_argument(
        "--png",
        action="store_true",
        help="also write NetworkX/Matplotlib PNG frames",
    )
    args = parser.parse_args()

    adj, state, credence, belief, acc = parse_paper_ltm(args.input)
    frames, activate_round, final_acc = simulate_giraph_ltm(
        adj, state, credence, belief, acc
    )
    expected = write_text_outputs(args.out, activate_round, final_acc)
    summary = write_summary(args.out, frames)
    print("wrote", expected)
    print("wrote", summary)
    for ss, newly, active in frames:
        print(
            "superstep {}: newly={} (|active|={})".format(
                ss, newly, len(active)
            )
        )

    if args.png:
        draw_pngs(args.out, adj, frames)


if __name__ == "__main__":
    main()
