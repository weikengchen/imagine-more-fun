#!/usr/bin/env python3
"""
Parser for /imf dumpchunks output.

Format produced by ChunkDumpCommand.java (big-endian, gzipped). Run:

    python3 parse_dump.py chunks-<timestamp>.bin.gz

By default prints a summary: bounding box of non-air blocks, light-emitter
count, top-N most common block types. Modify `analyze()` to taste — the dict
returned by `parse()` gives you everything in raw form.
"""

import collections
import gzip
import struct
import sys
from typing import Dict, List, Tuple


def _read_utf(buf: bytes, offset: int) -> Tuple[str, int]:
    """DataInput.readUTF: u16 byte length, then modified-UTF-8 bytes."""
    n = struct.unpack_from(">H", buf, offset)[0]
    s = buf[offset + 2 : offset + 2 + n].decode("utf-8")
    return s, offset + 2 + n


def _unpack(fmt: str, buf: bytes, offset: int):
    sz = struct.calcsize(fmt)
    return struct.unpack_from(fmt, buf, offset), offset + sz


def parse(path: str) -> dict:
    with gzip.open(path, "rb") as f:
        buf = f.read()
    o = 0
    magic = buf[o : o + 4]
    if magic != b"IMFC":
        raise ValueError(f"bad magic: {magic!r}")
    o += 4
    (version,), o = _unpack(">B", buf, o)
    dim, o = _read_utf(buf, o)
    (px, py, pz), o = _unpack(">ddd", buf, o)
    (yaw, pitch), o = _unpack(">ff", buf, o)
    (radius,), o = _unpack(">i", buf, o)
    (min_cx, max_cx, min_cz, max_cz), o = _unpack(">iiii", buf, o)
    (min_light_sy, max_light_sy), o = _unpack(">ii", buf, o)
    (chunk_count,), o = _unpack(">i", buf, o)

    chunks: List[dict] = []
    for _ in range(chunk_count):
        (cx, cz, sec_count), o = _unpack(">iii", buf, o)
        sections: List[dict] = []
        for _ in range(sec_count):
            (sy,), o = _unpack(">i", buf, o)
            (palette_size,), o = _unpack(">H", buf, o)
            palette: List[str] = []
            for _ in range(palette_size):
                s, o = _read_utf(buf, o)
                palette.append(s)
            (idx_bytes,), o = _unpack(">B", buf, o)
            if idx_bytes == 1:
                indices = list(buf[o : o + 4096])
                o += 4096
            else:
                indices = list(struct.unpack_from(">4096H", buf, o))
                o += 4096 * 2
            (blk_flag,), o = _unpack(">B", buf, o)
            block_light = None
            if blk_flag == 2:
                block_light = buf[o : o + 2048]
                o += 2048
            (sky_flag,), o = _unpack(">B", buf, o)
            sky_light = None
            if sky_flag == 2:
                sky_light = buf[o : o + 2048]
                o += 2048
            sections.append(
                dict(
                    sy=sy,
                    palette=palette,
                    indices=indices,
                    block_light=block_light,
                    block_light_flag=blk_flag,
                    sky_light=sky_light,
                    sky_light_flag=sky_flag,
                )
            )
        chunks.append(dict(cx=cx, cz=cz, sections=sections))

    return dict(
        version=version,
        dim=dim,
        player=(px, py, pz, yaw, pitch),
        radius=radius,
        chunk_bounds=(min_cx, max_cx, min_cz, max_cz),
        light_section_y=(min_light_sy, max_light_sy),
        chunks=chunks,
    )


def nibble(arr: bytes, idx: int) -> int:
    """Read 4-bit value at index `idx` from a 2048-byte DataLayer array."""
    b = arr[idx >> 1]
    return (b >> ((idx & 1) << 2)) & 0xF


def analyze(d: dict) -> None:
    print(f"dim:    {d['dim']}")
    print(f"player: x={d['player'][0]:.1f} y={d['player'][1]:.1f} z={d['player'][2]:.1f}")
    print(f"radius: {d['radius']} chunks  bounds={d['chunk_bounds']}")
    print(f"chunks: {len(d['chunks'])} loaded")

    block_counts: collections.Counter = collections.Counter()
    emitter_counts: collections.Counter = collections.Counter()
    min_x = min_y = min_z = 1 << 30
    max_x = max_y = max_z = -(1 << 30)
    sections_with_blocks = 0
    sections_with_block_light = 0

    for ch in d["chunks"]:
        for sec in ch["sections"]:
            base_x = ch["cx"] * 16
            base_y = sec["sy"] * 16
            base_z = ch["cz"] * 16
            indices = sec["indices"]
            palette = sec["palette"]
            non_air_in_sec = False
            for i, p_idx in enumerate(indices):
                state = palette[p_idx]
                if state.startswith("minecraft:air"):
                    continue
                non_air_in_sec = True
                lx = i & 0xF
                lz = (i >> 4) & 0xF
                ly = (i >> 8) & 0xF
                wx = base_x + lx
                wy = base_y + ly
                wz = base_z + lz
                if wx < min_x:
                    min_x = wx
                if wy < min_y:
                    min_y = wy
                if wz < min_z:
                    min_z = wz
                if wx > max_x:
                    max_x = wx
                if wy > max_y:
                    max_y = wy
                if wz > max_z:
                    max_z = wz
                # Strip blockstate properties for the per-block tally
                base = state.split("[", 1)[0]
                block_counts[base] += 1
            if non_air_in_sec:
                sections_with_blocks += 1
            if sec["block_light_flag"] == 2:
                sections_with_block_light += 1
                bl = sec["block_light"]
                # Count cells with light >= 13 — those are roughly at-source values
                bright_cells = sum(1 for i in range(4096) if nibble(bl, i) >= 13)
                if bright_cells:
                    emitter_counts[(ch["cx"], sec["sy"], ch["cz"])] = bright_cells

    print()
    print(
        f"non-air bbox: x=[{min_x},{max_x}] y=[{min_y},{max_y}] z=[{min_z},{max_z}]"
    )
    print(
        f"  size: {max_x - min_x + 1} x {max_y - min_y + 1} x {max_z - min_z + 1} blocks"
    )
    cx = (min_x + max_x) // 2
    cy = (min_y + max_y) // 2
    cz = (min_z + max_z) // 2
    print(f"  center: ({cx}, {cy}, {cz})")

    print()
    print("top 12 block types (by non-air count):")
    for name, count in block_counts.most_common(12):
        print(f"  {count:>10,}  {name}")

    print()
    print(
        f"sections with blocks: {sections_with_blocks}, "
        f"sections with block-light data: {sections_with_block_light}"
    )
    print()
    print("top 6 sections by 'bright' (BL>=13) cell count — likely emitters:")
    for (cx, sy, cz), n in emitter_counts.most_common(6):
        print(f"  cx={cx} sy={sy} cz={cz}  bright_cells={n}  (block bbox y={sy*16}..{sy*16+15})")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("usage: parse_dump.py <chunks-*.bin.gz>", file=sys.stderr)
        sys.exit(2)
    analyze(parse(sys.argv[1]))
