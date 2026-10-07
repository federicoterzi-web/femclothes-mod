"""Genera los modelos GeckoLib de los apliques y su atlas.

A pedido (2026-10-01, Mesa de estilado: "agregarles modelos 3d anclados en la
geometria de la prenda para agregar moños mariposas flores... unos 3 modelos con
3 colores configurables... si para que tengan fisicas o conecten a la pollera
tienen que ser geckolib hacelos asi").

Convenciones (las usa render/ApliqueRenderer):
  - El modelo mira hacia -Z (north): la espalda del aplique va en z = 0, apoyada
    en la tela, y crece hacia z negativo (hacia afuera). Centrado en x = 0, y = 0.
  - Atlas de 96x32: tres columnas de 32x32, una por ZONA de color (1, 2, 3). Cada
    cara de cada cubo apunta a la columna de su zona; el render tiñe cada columna
    con el color de esa zona (y la fase 4 pinta ahí los patrones de Tintes).
  - Huesos con nombre para lo que se mueve (tela blanda, render/ApliqueRenderer lee el
    prefijo): "cola*" = cadena de tramos anidados (cada uno pivota en su borde de arriba),
    "ala*" = aleteo, "petalo*" y "hojas" = resorte firme; "nudo", "cuerpo" y "centro" no se mueven.

Uso: python tools/generar_apliques.py
"""
import json
import random
from pathlib import Path

from PIL import Image

RAIZ = Path(__file__).resolve().parent.parent / "src/main/resources/assets/modamod"
GEO = RAIZ / "geo"
ATLAS = RAIZ / "textures/entity/aplique_atlas.png"
TEX_W, TEX_H, ZONA = 96, 32, 32


def caras(size, zona):
    """UV por cara apuntando a la columna de la zona, con el tamaño real de la cara."""
    sx, sy, sz = (max(0.1, v) for v in size)
    u0, v0 = (zona - 1) * ZONA + 1, 1
    def f(w, h):
        return {"uv": [u0, v0], "uv_size": [round(w, 3), round(h, 3)]}
    return {"north": f(sx, sy), "south": f(sx, sy), "east": f(sz, sy), "west": f(sz, sy),
            "up": f(sx, sz), "down": f(sx, sz)}


def cubo(origen, tam, zona, rot=None, pivote=None):
    c = {"origin": [round(v, 3) for v in origen], "size": [round(v, 3) for v in tam], "uv": caras(tam, zona)}
    if rot:
        c["rotation"] = rot
        c["pivot"] = pivote
    return c


def hueso(nombre, pivote, cubos, padre="root", rot=None):
    h = {"name": nombre, "parent": padre, "pivot": pivote, "cubes": cubos}
    if rot:
        h["rotation"] = rot
    return h


def modelo(nombre, huesos):
    return {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {"identifier": f"geometry.aplique_{nombre}", "texture_width": TEX_W,
                            "texture_height": TEX_H, "visible_bounds_width": 1, "visible_bounds_height": 1,
                            "visible_bounds_offset": [0, 0, 0]},
            "bones": [{"name": "root", "pivot": [0, 0, 0]}] + huesos,
        }],
    }


def cadena(nombre, pivote, x0, ancho, y_arriba, largo, z0, prof, zona, rot, tramos=3):
    """Una cola o cinta en TRAMOS tramos anidados (2026-10-04, tela blanda: "tela blanda
    afectada por el movimiento"): cada tramo es hijo del anterior y pivota en su borde de
    arriba, así `render/ApliqueRenderer` les da un giro a cada uno y la cinta se curva en
    vez de girar entera. Nombres: <nombre>, <nombre>_2, <nombre>_3... (el render los
    reconoce por el prefijo "cola"/"cinta")."""
    huesos = []
    alto = largo / tramos
    for k in range(tramos):
        top = y_arriba - k * alto
        cubos = [cubo([x0, top - alto, z0], [ancho, alto, prof], zona)]
        nom = nombre if k == 0 else f"{nombre}_{k + 1}"
        padre = "root" if k == 0 else (nombre if k == 1 else f"{nombre}_{k}")
        piv = pivote if k == 0 else [pivote[0], top, pivote[2]]
        huesos.append(hueso(nom, piv, cubos, padre=padre, rot=rot if k == 0 else None))
    return huesos


def mono():
    # Zona 1: alas · Zona 2: nudo · Zona 3: colas.
    return modelo("mono", [
        hueso("nudo", [0, 0, 0], [cubo([-0.6, -0.6, -1.0], [1.2, 1.2, 1.0], 2)]),
        hueso("ala_izq", [-0.6, 0, -0.5], [
            cubo([-3.0, -1.1, -0.85], [2.4, 2.2, 0.7], 1),
            cubo([-3.2, -0.6, -0.75], [0.3, 1.2, 0.5], 1),
        ]),
        hueso("ala_der", [0.6, 0, -0.5], [
            cubo([0.6, -1.1, -0.85], [2.4, 2.2, 0.7], 1),
            cubo([2.9, -0.6, -0.75], [0.3, 1.2, 0.5], 1),
        ]),
        *cadena("cola_izq", [-0.3, -0.5, -0.5], -0.9, 0.6, -0.5, 2.6, -0.6, 0.25, 3, [0, 0, -18]),
        *cadena("cola_der", [0.3, -0.5, -0.5], 0.3, 0.6, -0.5, 2.6, -0.6, 0.25, 3, [0, 0, 18]),
    ])


def mariposa():
    # Zona 1: alas de arriba · Zona 2: alas de abajo y lunares · Zona 3: cuerpo y antenas.
    return modelo("mariposa", [
        hueso("cuerpo", [0, 0, 0], [
            cubo([-0.3, -1.6, -0.9], [0.6, 3.2, 0.6], 3),
            cubo([-0.75, 1.5, -0.7], [0.15, 1.1, 0.15], 3, [0, 0, 22], [-0.3, 1.6, -0.6]),
            cubo([0.6, 1.5, -0.7], [0.15, 1.1, 0.15], 3, [0, 0, -22], [0.3, 1.6, -0.6]),
        ]),
        hueso("ala_izq", [-0.3, 0, -0.6], [
            cubo([-3.0, -0.2, -0.65], [2.7, 2.4, 0.15], 1),
            cubo([-2.3, -2.0, -0.65], [2.0, 1.8, 0.15], 2),
            cubo([-2.3, 0.6, -0.72], [0.8, 0.8, 0.08], 2),
        ]),
        hueso("ala_der", [0.3, 0, -0.6], [
            cubo([0.3, -0.2, -0.65], [2.7, 2.4, 0.15], 1),
            cubo([0.3, -2.0, -0.65], [2.0, 1.8, 0.15], 2),
            cubo([1.5, 0.6, -0.72], [0.8, 0.8, 0.08], 2),
        ]),
    ])


def flor():
    # Zona 1: pétalos · Zona 2: centro · Zona 3: hojas.
    petalos = [cubo([-0.65, 0.45, -0.75], [1.3, 1.9, 0.35], 1, [0, 0, k * 72], [0, 0, -0.55]) for k in range(5)]
    return modelo("flor", [
        hueso("centro", [0, 0, 0], [cubo([-0.7, -0.7, -1.15], [1.4, 1.4, 0.7], 2)]),
        hueso("petalos", [0, 0, -0.55], petalos),
        hueso("hojas", [0, -1.0, -0.3], [
            cubo([-0.4, -3.4, -0.45], [0.8, 2.3, 0.2], 3, [0, 0, 38], [0, -1.1, -0.35]),
            cubo([-0.4, -3.4, -0.45], [0.8, 2.3, 0.2], 3, [0, 0, -38], [0, -1.1, -0.35]),
        ]),
    ])


def atlas():
    """Tres columnas de tela gris clara (se tiñen en runtime), con un tramado suave."""
    random.seed(11)
    img = Image.new("RGBA", (TEX_W, TEX_H), (0, 0, 0, 0))
    px = img.load()
    for z in range(3):
        for y in range(TEX_H):
            for x in range(ZONA):
                base = 226 + random.randint(-7, 7)
                if (x + y) % 4 == 0:
                    base -= 10   # trama de la tela
                px[z * ZONA + x, y] = (base, base, base, 255)
    return img


def main():
    GEO.mkdir(parents=True, exist_ok=True)
    for nombre, m in (("mono", mono()), ("mariposa", mariposa()), ("flor", flor())):
        (GEO / f"aplique_{nombre}.geo.json").write_text(json.dumps(m, indent=2) + "\n")
    ATLAS.parent.mkdir(parents=True, exist_ok=True)
    atlas().save(ATLAS)
    print("ok", GEO, ATLAS)


if __name__ == "__main__":
    main()
