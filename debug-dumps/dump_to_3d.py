#!/usr/bin/env python3
"""
Convert a chunks-*.bin.gz dump into a compact JSON for the Three.js viewer.

    python3 dump_to_3d.py chunks-1777733558031.bin.gz [--out dome_blocks.json]

Filters to a ~140-block horizontal box around the Hyperspace Mountain dome
center (-270, 80, 167), Y=30..120 — the show building plus a small margin.
Output is grouped by block type; positions stored as flat [x0,y0,z0,x1,...]
arrays (smaller than nested arrays in JSON).
"""

import argparse
import gzip
import json
import struct
import sys
from collections import defaultdict
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from parse_dump import parse  # noqa: E402

# Hand-curated approximate top-face colors. Falls back to the default for
# anything not listed — no need to be exhaustive.
COLOR_MAP = {
    "minecraft:air": None,  # never emitted
    "minecraft:cave_air": None,
    "minecraft:void_air": None,
    "minecraft:light": None,  # invisible "light" block
    "minecraft:barrier": "#ff5555",  # normally invisible; show as red so we can see them
    # Concretes
    "minecraft:black_concrete": "#0a0d11",
    "minecraft:white_concrete": "#cfd5d6",
    "minecraft:gray_concrete": "#3b3d42",
    "minecraft:light_gray_concrete": "#7d7d73",
    "minecraft:blue_concrete": "#2c2e8f",
    "minecraft:cyan_concrete": "#157788",
    "minecraft:red_concrete": "#8e2121",
    "minecraft:orange_concrete": "#e06100",
    "minecraft:yellow_concrete": "#e0c93b",
    "minecraft:green_concrete": "#5e7c16",
    "minecraft:purple_concrete": "#641f9c",
    "minecraft:pink_concrete": "#d56791",
    "minecraft:lime_concrete": "#7cbd1d",
    "minecraft:magenta_concrete": "#a93dab",
    "minecraft:brown_concrete": "#603d23",
    # Concrete powders (similar but slightly muted)
    "minecraft:black_concrete_powder": "#1a1c1f",
    # Terracottas / glazed
    "minecraft:cyan_terracotta": "#566c75",
    "minecraft:black_terracotta": "#251710",
    "minecraft:gray_terracotta": "#3a2a23",
    "minecraft:green_terracotta": "#4c5e2f",
    "minecraft:white_terracotta": "#d1b1a1",
    "minecraft:red_terracotta": "#8f3e2d",
    "minecraft:orange_terracotta": "#a45129",
    "minecraft:brown_terracotta": "#4d3324",
    # Wools
    "minecraft:black_wool": "#15151a",
    "minecraft:white_wool": "#e9ecec",
    "minecraft:cyan_wool": "#157788",
    "minecraft:gray_wool": "#3e3e3e",
    # Stone family
    "minecraft:stone": "#7a7a7a",
    "minecraft:cobblestone": "#7a7a7a",
    "minecraft:smooth_stone": "#9d9d9d",
    "minecraft:smooth_stone_slab": "#9d9d9d",
    "minecraft:stone_slab": "#7a7a7a",
    "minecraft:stone_brick_stairs": "#787876",
    "minecraft:polished_andesite": "#828585",
    "minecraft:andesite": "#7d7d7d",
    "minecraft:granite": "#9b6757",
    # Quartz (the white pillar/dome material)
    "minecraft:quartz_block": "#ece6dd",
    "minecraft:quartz_slab": "#ece6dd",
    "minecraft:quartz_pillar": "#e8e2d8",
    "minecraft:quartz_stairs": "#ece6dd",
    "minecraft:smooth_quartz": "#ece6dd",
    "minecraft:smooth_quartz_slab": "#ece6dd",
    # Nether brick (the dark accents)
    "minecraft:nether_bricks": "#2c1717",
    "minecraft:nether_brick_stairs": "#2c1717",
    "minecraft:nether_brick_slab": "#2c1717",
    "minecraft:nether_brick_fence": "#2c1717",
    # Misc
    "minecraft:clay": "#a4a8b8",
    "minecraft:iron_block": "#d8d8d8",
    "minecraft:gold_block": "#fae44d",
    "minecraft:redstone_block": "#a51918",
    "minecraft:obsidian": "#0c0a16",
    "minecraft:bedrock": "#444444",
    "minecraft:dirt": "#866043",
    "minecraft:grass_block": "#5b8b39",
    "minecraft:gravel": "#7d7d7d",
    "minecraft:sea_lantern": "#b8c8c8",
    "minecraft:glowstone": "#f8c870",
    "minecraft:lantern": "#f0bd5e",
    "minecraft:redstone_lamp": "#a55322",
    # Wood / leaves
    "minecraft:oak_leaves": "#4f6c2c",
    "minecraft:oak_log": "#6a5234",
    "minecraft:oak_planks": "#a17a48",
    "minecraft:dark_oak_planks": "#3f2613",
    "minecraft:spruce_planks": "#735032",
    "minecraft:mushroom_stem": "#c5b791",
    "minecraft:red_mushroom_block": "#a3221c",
    "minecraft:brown_mushroom_block": "#956d4f",
    # Glass — render slightly transparent in viewer
    "minecraft:glass": "#cfdbe0",
    "minecraft:white_stained_glass": "#dadada",
    "minecraft:black_stained_glass": "#181818",
    # Magma / lava
    "minecraft:magma_block": "#9c4416",
    "minecraft:lava": "#d96c1f",
    "minecraft:water": "#3f76e4",
}
DEFAULT_COLOR = "#bb55ee"  # magenta-ish so unknown blocks stand out


def _norm(name: str) -> str:
    return name.split("[", 1)[0]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("dump", help="path to chunks-*.bin.gz")
    ap.add_argument(
        "--out",
        default="dome_blocks.json",
        help="output JSON path (default: dome_blocks.json)",
    )
    ap.add_argument("--cx", type=float, default=-270.0)
    ap.add_argument("--cy", type=float, default=80.0)
    ap.add_argument("--cz", type=float, default=167.0)
    ap.add_argument(
        "--horiz",
        type=int,
        default=80,
        help="horizontal half-extent in blocks (default 80)",
    )
    ap.add_argument("--y-min", type=int, default=30)
    ap.add_argument("--y-max", type=int, default=120)
    args = ap.parse_args()

    print(f"[1/3] parsing {args.dump} ...", flush=True)
    d = parse(args.dump)

    cx, cy, cz = args.cx, args.cy, args.cz
    H = args.horiz
    YMIN, YMAX = args.y_min, args.y_max
    px, py, pz, _, _ = d["player"]

    print(
        f"[2/3] filtering: center=({cx:.0f},{cy:.0f},{cz:.0f})  "
        f"horiz=±{H}  y=[{YMIN},{YMAX}] ...",
        flush=True,
    )

    grouped = defaultdict(list)  # name -> flat [x,y,z,x,y,z,...]
    counts = defaultdict(int)
    bbox = [1 << 30, 1 << 30, 1 << 30, -(1 << 30), -(1 << 30), -(1 << 30)]

    for ch in d["chunks"]:
        base_x = ch["cx"] * 16
        base_z = ch["cz"] * 16
        # Skip whole chunks outside the window
        if base_x + 15 < cx - H or base_x > cx + H:
            continue
        if base_z + 15 < cz - H or base_z > cz + H:
            continue
        for sec in ch["sections"]:
            base_y = sec["sy"] * 16
            if base_y > YMAX or base_y + 15 < YMIN:
                continue
            palette = sec["palette"]
            indices = sec["indices"]
            for i, p_idx in enumerate(indices):
                state = palette[p_idx]
                name = _norm(state)
                color = COLOR_MAP.get(name, DEFAULT_COLOR)
                if color is None:
                    continue  # air / invisible
                lx = i & 0xF
                lz = (i >> 4) & 0xF
                ly = (i >> 8) & 0xF
                wx = base_x + lx
                wy = base_y + ly
                wz = base_z + lz
                if wy < YMIN or wy > YMAX:
                    continue
                if abs(wx - cx) > H or abs(wz - cz) > H:
                    continue
                arr = grouped[name]
                arr.append(wx)
                arr.append(wy)
                arr.append(wz)
                counts[name] += 1
                if wx < bbox[0]:
                    bbox[0] = wx
                if wy < bbox[1]:
                    bbox[1] = wy
                if wz < bbox[2]:
                    bbox[2] = wz
                if wx > bbox[3]:
                    bbox[3] = wx
                if wy > bbox[4]:
                    bbox[4] = wy
                if wz > bbox[5]:
                    bbox[5] = wz

    blocks_payload = []
    for name in sorted(counts, key=lambda n: -counts[n]):
        blocks_payload.append(
            {
                "name": name,
                "color": COLOR_MAP.get(name, DEFAULT_COLOR),
                "positions": grouped[name],
            }
        )

    payload = {
        "meta": {
            "dimension": d["dim"],
            "player": [px, py, pz],
            "center": [cx, cy, cz],
            "horiz": H,
            "y_range": [YMIN, YMAX],
            "bbox": bbox,
            "hole_bbox": {"min": [-283, 74, 215], "max": [-223, 83, 220]},
            "default_color": DEFAULT_COLOR,
        },
        "blocks": blocks_payload,
    }

    out_path = Path(args.out)
    print(
        f"[3/3] writing {out_path}  ({sum(counts.values()):,} blocks, "
        f"{len(counts)} types) ...",
        flush=True,
    )
    out_path.write_text(json.dumps(payload, separators=(",", ":")))
    size_kb = out_path.stat().st_size / 1024
    print(f"done — {size_kb:.0f} KB")
    print()
    print("top types:")
    for name, c in sorted(counts.items(), key=lambda kv: -kv[1])[:10]:
        print(f"  {c:>8,}  {name}")


if __name__ == "__main__":
    main()
