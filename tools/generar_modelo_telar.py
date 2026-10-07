"""Modelo del Telar automático (2026-10-07, "subi una nueva tapa de maquina nueva hay q componerla con la parte de abajo de
la mesa de estilado" + "la base era de la estiladora automatica. No de ese bloque. La idea es q produzca prendas basicas a
base de lana e hilo"): el cuerpo de abajo de la Estilista (el de la Modeladora, garment_shaper, cortado donde empieza la
tapa, y=10,3) con la tapa del telar (auto_loom: bastidor, plegador de urdimbre, dos lizos, batán, lanzadera,
plegador de tela y volante) montada encima.

Entradas: geo/garment_shaper.geo.json + su atlas (del mod) y assets_nuevos_prueba/telar/ (la tapa, del zip "telar automatico").
Salidas: geo/telar.geo.json, animations/telar.animation.json y textures/block/telar_atlas.png (128x256: la base arriba,
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
# Texels propios y libres del atlas (2026-10-07, "el telar automatico tiene q tener la barra de progreso y dos barras de
# insumos como la autoestiladora"): barra de progreso granate y las de lana (blanca) e hilo (dorado).
TEXEL_ACENTO, TEXEL_LANA, TEXEL_HILO = (100, 60), (101, 60), (102, 60)


def geo(ruta):
    return json.load(open(ruta, encoding="utf-8"))


def main():
    M = geo(os.path.join(RES, "geo", "garment_shaper.geo.json"))
    L = geo(os.path.join(NUEVO, "auto_loom.geo.json"))
    gm, gl = M["minecraft:geometry"][0], L["minecraft:geometry"][0]

    huesos = []
    for b in gm["bones"]:
        if b["name"] in ("guillotine", "needle", "REMERA", "PANTALON", "CALIENTABRAZOS", "MEDIAS", "tijera"):
            continue                                      # lo de arriba de la Modeladora lo reemplaza la tapa
        b = copy.deepcopy(b)
        if b["name"] == "base":
            b["cubes"] = [c for c in (est.recortar_cubo(c) for c in b.get("cubes", [])) if c]
        huesos.append(b)
    base = next(b for b in huesos if b["name"] == "base")
    # como en la Estilista: sin la tira de abajo de la pantalla
    base["cubes"] = [c for c in base["cubes"] if not (abs(c["origin"][0] + 6) < 0.01 and abs(c["origin"][1] - 4.69) < 0.01 and abs(c["size"][1] - 0.32) < 0.01)]
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

    for b in huesos:
        if b["name"] == "progress":
            for c in b["cubes"]:
                for cara in c["uv"].values():
                    cara["uv"] = [TEXEL_ACENTO[0], TEXEL_ACENTO[1]]
    # Dos indicadores verticales de insumos en el hueco del panel (como la Estilista); el cliente los escala en Y.
    for nombre, x0, texel in (("lana", 1.55, TEXEL_LANA), ("hilo", 3.55, TEXEL_HILO)):
        base["cubes"].append({
            "origin": [x0 - 0.12, 5.18, -8.86], "size": [1.34, 3.34, 0.1],
            "uv": {cara: {"uv": [126, 1], "uv_size": [0.3, 0.3]} for cara in ("north", "south", "east", "west", "up", "down")},
        })
        huesos.append({
            "name": nombre, "parent": "root", "pivot": [x0 + 0.55, 5.3, -8.78],
            "cubes": [{
                "origin": [x0, 5.3, -8.9], "size": [1.1, 3.1, 0.24],
                "uv": {cara: {"uv": [texel[0], texel[1]], "uv_size": [0.3, 0.3]} for cara in ("north", "south", "east", "west", "up", "down")},
            }],
        })

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
    a = Image.open(os.path.join(RES, "textures", "block", "garment_shaper_atlas.png")).convert("RGBA")
    b = Image.open(os.path.join(NUEVO, "auto_loom_atlas.png")).convert("RGBA")
    atlas = Image.new("RGBA", (128, 256), (0, 0, 0, 0))
    atlas.paste(a, (0, 0))
    atlas.paste(b, (0, est.DESPLAZAR_V))
    atlas.putpixel(TEXEL_ACENTO, (0xA3, 0x26, 0x3A, 255))
    atlas.putpixel(TEXEL_LANA, (0xF2, 0xF2, 0xF2, 255))
    atlas.putpixel(TEXEL_HILO, (0xD8, 0xA8, 0x5A, 255))
    # La pista gris de las barras (texel 126,1) la usa también la Estilista: se copia a este atlas si falta.
    est_atlas = Image.open(os.path.join(RES, "textures", "block", "estilista_atlas.png")).convert("RGBA")
    atlas.putpixel((126, 1), est_atlas.getpixel((126, 1)))
    atlas.save(os.path.join(RES, "textures", "block", "telar_atlas.png"))
    print("huesos:", [x["name"] for x in huesos])
    print("cubos de la base:", len(base["cubes"]), "| animaciones:", list(animaciones))


if __name__ == "__main__":
    main()
