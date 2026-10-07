"""Modelo del Telar automático (2026-10-07, "subi una nueva tapa de maquina nueva hay q componerla con la parte de abajo de
la mesa de estilado" + "es el telar automatico"): el cuerpo de abajo de la Mesa de estilado (styling_table, cortado donde
empieza la tapa, y=10,3) con la tapa del telar (auto_loom: bastidor, plegador de urdimbre, dos lizos, batán, lanzadera,
plegador de tela y volante) montada encima.

Entradas: geo/styling_table.geo.json + su atlas (del mod) y assets_nuevos_prueba/telar/ (la tapa, del zip "telar automatico").
Salidas: geo/telar.geo.json, animations/telar.animation.json y textures/block/telar_atlas.png (128x256: la mesa arriba,
la tapa abajo; los UV de la tapa bajan 128).
Uso: python tools/generar_modelo_telar.py
"""
import copy
import json
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(__file__))
import generar_modelo_estilista as est   # recortar_cubo / bajar_uv / CORTE / DESPLAZAR_V

RAIZ = os.path.join(os.path.dirname(__file__), "..")
RES = os.path.join(RAIZ, "src", "main", "resources", "assets", "femclothes")
NUEVO = os.path.join(RAIZ, "assets_nuevos_prueba", "telar")


def geo(ruta):
    return json.load(open(ruta, encoding="utf-8"))


def main():
    M = geo(os.path.join(RES, "geo", "styling_table.geo.json"))
    L = geo(os.path.join(NUEVO, "auto_loom.geo.json"))
    gm, gl = M["minecraft:geometry"][0], L["minecraft:geometry"][0]

    huesos = []
    for b in gm["bones"]:
        b = copy.deepcopy(b)
        if b["name"] == "base":
            b["cubes"] = [c for c in (est.recortar_cubo(c) for c in b.get("cubes", [])) if c]
        huesos.append(b)
    base = next(b for b in huesos if b["name"] == "base")
    for b in gl["bones"]:
        if b["name"] == "root":
            continue
        b = copy.deepcopy(b)
        b["cubes"] = [est.bajar_uv(c) for c in b.get("cubes", [])]
        if b["name"] == "base":
            base["cubes"].extend(b["cubes"])          # el bastidor del telar pasa a ser parte de la base
            continue
        b["parent"] = b.get("parent") or "root"
        huesos.append(b)

    out = copy.deepcopy(M)
    g = out["minecraft:geometry"][0]
    g["description"]["identifier"] = "geometry.telar"
    g["description"]["texture_height"] = 256
    g["bones"] = huesos
    json.dump(out, open(os.path.join(RES, "geo", "telar.geo.json"), "w", encoding="utf-8"), indent=2)

    # Animaciones del telar con los keyframes ordenados por tiempo (GeckoLib no mueve los que vienen desordenados).
    A = geo(os.path.join(NUEVO, "auto_loom.animation.json"))["animations"]
    animaciones = {}
    for nombre, anim in A.items():
        anim = copy.deepcopy(anim)
        for hueso in anim.get("bones", {}).values():
            for canal, claves in list(hueso.items()):
                if isinstance(claves, dict):
                    hueso[canal] = dict(sorted(claves.items(), key=lambda kv: float(kv[0])))
        animaciones[nombre.replace("auto_loom", "telar")] = anim
    json.dump({"format_version": "1.8.0", "animations": animaciones},
              open(os.path.join(RES, "animations", "telar.animation.json"), "w", encoding="utf-8"), indent=2)

    # Atlas: la mesa de estilado arriba, la tapa del telar abajo.
    a = Image.open(os.path.join(RES, "textures", "block", "styling_table_atlas.png")).convert("RGBA")
    b = Image.open(os.path.join(NUEVO, "auto_loom_atlas.png")).convert("RGBA")
    atlas = Image.new("RGBA", (128, 256), (0, 0, 0, 0))
    atlas.paste(a, (0, 0))
    atlas.paste(b, (0, est.DESPLAZAR_V))
    atlas.save(os.path.join(RES, "textures", "block", "telar_atlas.png"))
    print("huesos:", [x["name"] for x in huesos])
    print("cubos de la base:", len(base["cubes"]), "| animaciones:", list(animaciones))


if __name__ == "__main__":
    main()
