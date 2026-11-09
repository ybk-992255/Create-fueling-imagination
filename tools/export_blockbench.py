"""Export the supplied Blockbench cubes to Forge OBJ without changing source art.

Run from the project root with Python 3. The supplied food .bbmodel files,
including all six cookie counts, are inputs; generated OBJ and MTL files
belong to src/main/resources.
"""

from __future__ import annotations

import base64
import json
import math
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/creategourmetfood/models/block"
MODELS = (
    (ROOT / "模型素材/齿轮曲奇/齿轮曲奇.bbmodel", "gear_cookie"),
    (ROOT / "模型素材/精密构件派/精密构件.bbmodel", "precision_mechanism_pie"),
    *((ROOT / f"模型素材/齿轮曲奇/齿轮曲奇{count}.bbmodel", f"gear_cookie_{count}")
      for count in range(1, 7)),
)


def rotate(point, origin, degrees):
    x, y, z = (point[i] - origin[i] for i in range(3))
    ax, ay, az = (math.radians(a) for a in degrees)
    if ax:
        y, z = y * math.cos(ax) - z * math.sin(ax), y * math.sin(ax) + z * math.cos(ax)
    if ay:
        x, z = x * math.cos(ay) + z * math.sin(ay), -x * math.sin(ay) + z * math.cos(ay)
    if az:
        x, y = x * math.cos(az) - y * math.sin(az), x * math.sin(az) + y * math.cos(az)
    return x + origin[0], y + origin[1], z + origin[2]


def cube_faces(element):
    x1, y1, z1 = element["from"]
    x2, y2, z2 = element["to"]
    return {
        "north": [(x2, y1, z1), (x1, y1, z1), (x1, y2, z1), (x2, y2, z1)],
        "south": [(x1, y1, z2), (x2, y1, z2), (x2, y2, z2), (x1, y2, z2)],
        "east": [(x2, y1, z2), (x2, y1, z1), (x2, y2, z1), (x2, y2, z2)],
        "west": [(x1, y1, z1), (x1, y1, z2), (x1, y2, z2), (x1, y2, z1)],
        "up": [(x1, y2, z2), (x2, y2, z2), (x2, y2, z1), (x1, y2, z1)],
        "down": [(x1, y1, z1), (x2, y1, z1), (x2, y1, z2), (x1, y1, z2)],
    }


def collect(model):
    elements = {item["uuid"]: item for item in model["elements"] if item.get("type") == "cube"}
    faces = []

    def visit(node, transforms):
        if isinstance(node, str):
            element = elements.get(node)
            if element is None:
                return
            rotations = transforms + [(element.get("origin", [0, 0, 0]), element.get("rotation", [0, 0, 0]))]
            for direction, vertices in cube_faces(element).items():
                face = element.get("faces", {}).get(direction)
                if not face or face.get("texture") is None:
                    continue
                # Blockbench's nested group rotations are applied from leaf to root.
                transformed = []
                for vertex in vertices:
                    for origin, degrees in reversed(rotations):
                        vertex = rotate(vertex, origin, degrees)
                    transformed.append(vertex)
                faces.append((transformed, face))
            return
        if node.get("export", True) is False or node.get("visibility", True) is False:
            return
        group_rotations = transforms + [(node.get("origin", [0, 0, 0]), node.get("rotation", [0, 0, 0]))]
        for child in node.get("children", []):
            visit(child, group_rotations)

    for root in model["outliner"]:
        visit(root, [])
    return faces


def export(source, name, offset=None):
    model = json.loads(source.read_text(encoding="utf-8"))
    faces = collect(model)
    if not faces:
        raise ValueError(f"No visible faces in {source}")
    coordinates = [vertex for vertices, _ in faces for vertex in vertices]
    minimum = [min(vertex[axis] for vertex in coordinates) for axis in range(3)]
    maximum = [max(vertex[axis] for vertex in coordinates) for axis in range(3)]
    spans = [maximum[axis] - minimum[axis] for axis in range(3)]
    scale = min(14 / max(spans[0], spans[2]), 6 / spans[1])
    center_x = (minimum[0] + maximum[0]) / 2
    center_z = (minimum[2] + maximum[2]) / 2
    floor_y = minimum[1]
    if offset is not None:
        # Machine models retain their authored block scale; only align their different origins.
        scale = 1.0
        center_x, floor_y, center_z = 8 - offset[0], -offset[1], 8 - offset[2]
    width = model["resolution"]["width"]
    height = model["resolution"]["height"]

    lines = [f"# Exported from {source.name}; original art is unchanged.",
             f"mtllib {name}.mtl", "g food", "usemtl texture0"]
    vertex_index = 1
    texcoord_index = 1
    normal_index = 1
    for vertices, face in faces:
        for x, y, z in vertices:
            lines.append(f"v {(8 + (x - center_x) * scale) / 16:.7f} "
                         f"{((y - floor_y) * scale) / 16:.7f} "
                         f"{(8 + (z - center_z) * scale) / 16:.7f}")
        u1, v1, u2, v2 = face["uv"]
        uv = [(u1, v2), (u2, v2), (u2, v1), (u1, v1)]
        turns = (int(face.get("rotation", 0)) // 90) % 4
        uv = uv[turns:] + uv[:turns]
        # Forge's OBJ loader uses V as the texture's top-origin coordinate when
        # the model JSON has flip_v=false. Blockbench UVs are top-origin too.
        for u, v in uv:
            lines.append(f"vt {u / width:.7f} {v / height:.7f}")
        a = [vertices[1][i] - vertices[0][i] for i in range(3)]
        b = [vertices[2][i] - vertices[0][i] for i in range(3)]
        normal = (a[1] * b[2] - a[2] * b[1],
                  a[2] * b[0] - a[0] * b[2],
                  a[0] * b[1] - a[1] * b[0])
        norm = math.sqrt(sum(component * component for component in normal))
        lines.append("vn " + " ".join(f"{component / norm:.7f}" for component in normal))
        lines.append("f " + " ".join(
            f"{vertex_index + i}/{texcoord_index + i}/{normal_index}" for i in range(4)))
        vertex_index += 4
        texcoord_index += 4
        normal_index += 1
    ASSETS.mkdir(parents=True, exist_ok=True)
    (ASSETS / f"{name}.obj").write_text("\n".join(lines) + "\n", encoding="utf-8")
    (ASSETS / f"{name}.mtl").write_text("newmtl texture0\nKd 1 1 1\nmap_Kd #texture0\n", encoding="utf-8")
    if offset is not None:
        # The two authored workstation states embed different atlases; external texture.png differs too.
        encoded = model["textures"][0]["source"].split(",", 1)[1]
        texture_path = ASSETS.parent.parent / "textures/block" / f"{name}.png"
        texture_path.write_bytes(base64.b64decode(encoded))
    print(f"{name}: {len(faces)} faces, source spans {spans}, scale {scale:.3f}")


if __name__ == "__main__":
    for source, name in MODELS:
        export(source, name)
    export(ROOT / "模型素材/动力工作站/model.bbmodel", "powered_artisan_table", (0, 0, 0))
    export(ROOT / "模型素材/动力工作站/model - Converted.bbmodel", "powered_artisan_table_active", (8, 0, 8))
