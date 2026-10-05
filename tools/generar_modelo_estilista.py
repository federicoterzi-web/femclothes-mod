"""Modelo de la Estilista automática (2026-10-05, "copia la base de la modeladora ponele la tapa de la autostyler
arriba y vamos a migrar toda la funcionalidad de la mesa estilizadora a esta nueva maquina"): el cuerpo de abajo de la
Modeladora (pantalla, LED, barra de progreso, ventilador y bandeja de salida) con TODO lo de arriba del auto_styler
(la tapa con el pórtico y los huesos garment / placed_1..4 / held) montado encima.

Entradas: garment_shaper (geo + atlas de la Modeladora) y assets_nuevos_prueba/estilista/ (el zip "tapa de estilado").
Salidas: geo/estilista.geo.json, animations/estilista.animation.json y textures/block/estilista_atlas.png (128x256:
la Modeladora arriba, la tapa abajo; los UV de la tapa bajan 128).
Uso: python tools/generar_modelo_estilista.py
"""
import copy
import json
import os

from PIL import Image

RAIZ = os.path.join(os.path.dirname(__file__), "..")
TEXEL_LILA = (100, 60)          # libre en la franja de la pantalla; color de Tema.LILA
RES = os.path.join(RAIZ, "src", "main", "resources", "assets", "femclothes")
NUEVO = os.path.join(RAIZ, "assets_nuevos_prueba", "estilista")
CORTE = 10.3          # donde termina el cuerpo de la Modeladora y empieza la tapa (la base de la tapa está en 10.3)
DESPLAZAR_V = 128     # la tapa usa la mitad de abajo del atlas


def geo(ruta):
    return json.load(open(ruta, encoding="utf-8"))


def recortar_cubo(c):
    """Corta un cubo de la Modeladora en CORTE (solo la altura; los UV se dejan: el borde de arriba queda tapado)."""
    y0, h = c["origin"][1], c["size"][1]
    if y0 >= CORTE - 1e-6:
        return None
    if y0 + h > CORTE:
        c = copy.deepcopy(c)
        c["size"][1] = round(CORTE - y0, 4)
    return c


def bajar_uv(cubo):
    c = copy.deepcopy(cubo)
    uv = c.get("uv")
    if isinstance(uv, dict):
        for cara in uv.values():
            cara["uv"][1] += DESPLAZAR_V
    elif isinstance(uv, list):
        c["uv"][1] += DESPLAZAR_V
    return c


# Giro de la tapa (2026-10-05, "que la parte de arriba el espacio mas vacio quede del lado de la pantallita y la barra
# de progreso" → "pared al fondo"): 90° alrededor de Y, (x, z) → (-z, x): lo que estaba a +x (la pared maciza, del lado de
# la bandeja) pasa a +z, el fondo; el pórtico, que corría de adelante hacia atrás, corre de lado a lado.
CARA_GIRADA = {"south": "east", "west": "south", "north": "west", "east": "north"}   # cara nueva ← cara vieja


def girar_cubo(c):
    c = copy.deepcopy(c)
    o, t = c["origin"], c["size"]
    c["origin"] = [round(-(o[2] + t[2]), 4), o[1], o[0]]
    c["size"] = [t[2], t[1], t[0]]
    uv = c.get("uv")
    if isinstance(uv, dict):
        c["uv"] = {nueva: uv[vieja] for nueva, vieja in CARA_GIRADA.items() if vieja in uv}
        for cara in ("up", "down"):
            if cara in uv:
                c["uv"][cara] = uv[cara]
    return c


def girar_punto(p):
    return [round(-p[2], 4), p[1], p[0]]


def girar_vector(v, escala=False):
    if isinstance(v, list) and len(v) == 3 and all(isinstance(n, (int, float)) for n in v):
        return [v[2], v[1], v[0]] if escala else [-v[2], v[1], v[0]]
    if isinstance(v, dict):
        return {k: girar_vector(x, escala) for k, x in v.items()}
    return v


def main():
    S = geo(os.path.join(RES, "geo", "garment_shaper.geo.json"))
    A = geo(os.path.join(NUEVO, "auto_styler.geo.json"))
    gs, ga = S["minecraft:geometry"][0], A["minecraft:geometry"][0]

    huesos = []
    for b in gs["bones"]:
        if b["name"] in ("guillotine", "needle", "REMERA", "PANTALON", "CALIENTABRAZOS", "MEDIAS"):
            continue                                      # lo de arriba de la Modeladora lo reemplaza la tapa
        b = copy.deepcopy(b)
        if b["name"] == "base":
            b["cubes"] = [c for c in (recortar_cubo(c) for c in b.get("cubes", [])) if c]
        huesos.append(b)
    base = next(b for b in huesos if b["name"] == "base")
    # 2026-10-05, "sacarle una barrita abajo de la pantalla": la tira de la Modeladora bajo la pantalla se va.
    base["cubes"] = [c for c in base["cubes"] if not (abs(c["origin"][0] + 6) < 0.01 and abs(c["origin"][1] - 4.69) < 0.01 and abs(c["size"][1] - 0.32) < 0.01)]
    # 2026-10-05, "tintar la barra de progreso de lila": un texel propio (libre) en vez del naranja compartido.
    for b in huesos:
        if b["name"] == "progress":
            for c in b["cubes"]:
                for cara in c["uv"].values():
                    cara["uv"] = [TEXEL_LILA[0], TEXEL_LILA[1]]
    for b in ga["bones"]:
        if b["name"] == "root":
            continue
        b = copy.deepcopy(b)
        b["cubes"] = [girar_cubo(bajar_uv(c)) for c in b.get("cubes", [])]
        if "pivot" in b:
            b["pivot"] = girar_punto(b["pivot"])
        if b["name"] == "base":
            base["cubes"].extend(b["cubes"])              # la tapa pasa a ser parte de la base
            continue
        b["parent"] = b.get("parent") or "root"
        huesos.append(b)

    out = copy.deepcopy(S)
    g = out["minecraft:geometry"][0]
    g["description"]["identifier"] = "geometry.estilista"
    g["description"]["texture_height"] = 256
    g["description"]["visible_bounds_height"] = 3
    g["description"]["visible_bounds_offset"] = [0, 1.25, 0]
    g["bones"] = huesos
    ruta = os.path.join(RES, "geo", "estilista.geo.json")
    json.dump(out, open(ruta, "w", encoding="utf-8"), indent=2)

    # Animaciones: el trabajo del pórtico (13 s) con la bandeja de salida que se desliza al final, el quieto de la
    # tapa y el ventilador en marcha de la Modeladora.
    AS = geo(os.path.join(RES, "animations", "garment_shaper.animation.json"))["animations"]
    AA = geo(os.path.join(NUEVO, "auto_styler.animation.json"))["animations"]
    for anim in AA.values():                               # la tapa está girada: los recorridos también
        for hueso in anim["bones"].values():
            for canal, claves in hueso.items():
                for t, v in list(claves.items()):
                    claves[t] = girar_vector(v, escala=(canal == "scale"))
    for anim in AA.values():                               # GeckoLib espera los keyframes ordenados por tiempo
        for hueso in anim["bones"].values():
            for canal, claves in list(hueso.items()):
                hueso[canal] = dict(sorted(claves.items(), key=lambda kv: float(kv[0])))
    trabajo = copy.deepcopy(AA["animation.auto_styler.trabajo"])
    largo = trabajo["animation_length"]
    trabajo["bones"]["cargo"] = {"position": {"0": [0, 0, 0], str(round(largo * 0.85, 2)): [0, 0, 0], str(largo): [4.4, 0, 0]}}
    animaciones = {
        "animation.estilista.trabajo": trabajo,
        "animation.estilista.quieto": AA["animation.auto_styler.quieto"],
        "animation.estilista.en_marcha": AS["animation.garment_shaper.en_marcha"],
    }
    salida = {"format_version": "1.8.0", "animations": animaciones}
    json.dump(salida, open(os.path.join(RES, "animations", "estilista.animation.json"), "w", encoding="utf-8"), indent=2)

    # Atlas: Modeladora arriba, tapa abajo.
    a = Image.open(os.path.join(RES, "textures", "block", "garment_shaper_atlas.png")).convert("RGBA")
    b = Image.open(os.path.join(NUEVO, "auto_styler_atlas.png")).convert("RGBA")
    atlas = Image.new("RGBA", (128, 256), (0, 0, 0, 0))
    atlas.paste(a, (0, 0))
    atlas.paste(b, (0, DESPLAZAR_V))
    atlas.putpixel(TEXEL_LILA, (0xD6, 0xAA, 0xE8, 255))
    atlas.save(os.path.join(RES, "textures", "block", "estilista_atlas.png"))
    print("huesos:", [x["name"] for x in huesos])
    print("cubos de la base:", len(base["cubes"]))


if __name__ == "__main__":
    main()
