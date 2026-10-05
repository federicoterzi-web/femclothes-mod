"""Cinta transportadora (2026-10-04, "geometria de cinta transportadora... hace vos los assets").

Genera los modelos de bloque (recta, curva izquierda y curva derecha), el
blockstate, el modelo de ítem y las texturas (marco + banda animada) de la
cinta. Uso: python tools/generar_cinta.py

Convenciones:
  * Los modelos miran al NORTE (la prenda sale por -Z); el blockstate los gira.
  * La banda va a 1 px por tick (frametime 1, 4 frames de período 4 px), igual
    que la velocidad del ítem en CintaBlockEntity (16 ticks por bloque recto).
  * Curva izquierda = la prenda entra por el lado IZQUIERDO (oeste si mira al
    norte) y sale por el frente; pivote en la esquina noroeste. La derecha es
    el espejo.
"""
import json
import math
import os
import random

from PIL import Image

RAIZ = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "femclothes")
TEX = os.path.join(RAIZ, "textures", "block")
MOD = os.path.join(RAIZ, "models", "block")
ITEM = os.path.join(RAIZ, "models", "item")
ESTADOS = os.path.join(RAIZ, "blockstates")

CUERO_CLARO = (74, 66, 61, 255)
CUERO_OSCURO = (52, 46, 43, 255)
COSTURA = (118, 104, 92, 255)

PERIODO = 4          # px de una nervadura (2 claras + 2 oscuras)
FRAMES = 4           # un frame por px de avance
RADIO_ANILLO = (2.0, 14.0)
PASO_ARCO = PERIODO / 8.0   # rad por frame: a radio 8 es 1 px de arco por tick


def marco():
    """Madera clara con un filete de latón en el borde."""
    rnd = random.Random(7)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            v = 118 + rnd.randint(-6, 6) + (8 if (y // 4) % 2 else 0)
            px[x, y] = (v, int(v * 0.74), int(v * 0.48), 255)
    laton = (196, 156, 72, 255)
    for i in range(16):
        px[i, 0] = laton
        px[i, 15] = (150, 114, 52, 255)
        px[0, i] = laton
        px[15, i] = (150, 114, 52, 255)
    img.save(os.path.join(TEX, "cinta_marco.png"))


def banda_recta():
    img = Image.new("RGBA", (16, 16 * FRAMES), (0, 0, 0, 0))
    px = img.load()
    for k in range(FRAMES):
        for y in range(16):
            for x in range(2, 14):
                banda = ((y + k) % PERIODO) < PERIODO // 2
                c = CUERO_CLARO if banda else CUERO_OSCURO
                if x in (2, 13):
                    c = COSTURA if (y + k) % 4 == 0 else c
                px[x, 16 * k + y] = c
    img.save(os.path.join(TEX, "cinta_banda.png"))


def banda_curva(nombre, espejo):
    img = Image.new("RGBA", (16, 16 * FRAMES), (0, 0, 0, 0))
    px = img.load()
    for k in range(FRAMES):
        for y in range(16):
            for x in range(16):
                cx = (15 - x if espejo else x) + 0.5
                cz = y + 0.5
                r = math.hypot(cx, cz)
                if not (RADIO_ANILLO[0] <= r <= RADIO_ANILLO[1]):
                    continue
                phi = math.atan2(cx, cz)
                banda = ((phi - k * PASO_ARCO) / (PERIODO / 8.0)) % 1.0 < 0.5
                c = CUERO_CLARO if banda else CUERO_OSCURO
                if r < RADIO_ANILLO[0] + 1 or r > RADIO_ANILLO[1] - 1:
                    c = COSTURA if int((phi - k * PASO_ARCO) / (PERIODO / 8.0)) % 2 == 0 else c
                px[x, 16 * k + y] = c
    img.save(os.path.join(TEX, nombre))


def quietas():
    """Las bandas SIN animación (2026-10-05, "el conveyor belt y las maquinas dejan de circular items cuando reciben
    senal de redstone"): el primer cuadro de cada banda, para el estado powered (la cinta se ve parada)."""
    for nombre in ("cinta_banda", "cinta_curva_izq", "cinta_curva_der"):
        img = Image.open(os.path.join(TEX, nombre + ".png")).convert("RGBA")
        img.crop((0, 0, 16, 16)).save(os.path.join(TEX, nombre + "_quieta.png"))


def mcmeta(nombre):
    with open(os.path.join(TEX, nombre + ".mcmeta"), "w") as f:
        json.dump({"animation": {"frametime": 1, "interpolate": False}}, f, indent=2)


def caja(desde, hasta, cara="#marco"):
    """Caja con la textura de {cara} en las 6 caras. Si sobresale del bloque (la rampa llega a 22 px de alto, 2026-10-05,
    "fijate bien el borde que se construye por encima del bloque... tiene planos transparentes"), los UV por defecto
    de Minecraft salen de las coordenadas y se pasan de 16: leen afuera de la textura, o sea transparente. Ahí se
    ponen UV explícitos (los mismos del modelo por defecto) corriendo el tramo de arriba para que termine en 16."""
    x1, y1, z1 = desde
    x2, y2, z2 = hasta
    if max(desde + hasta) <= 16 and min(desde + hasta) >= 0:
        return {
            "from": desde, "to": hasta,
            "faces": {lado: {"texture": cara} for lado in ("north", "south", "east", "west", "up", "down")},
        }
    if y2 > 16:
        y1, y2 = max(0.0, y1 - (y2 - 16)), 16.0
    r = lambda v: round(v, 3)
    uv = {
        "north": [16 - x2, 16 - y2, 16 - x1, 16 - y1],
        "south": [x1, 16 - y2, x2, 16 - y1],
        "west": [z1, 16 - y2, z2, 16 - y1],
        "east": [16 - z2, 16 - y2, 16 - z1, 16 - y1],
        "up": [x1, z1, x2, z2],
        "down": [x1, 16 - z2, x2, 16 - z1],
    }
    return {
        "from": desde, "to": hasta,
        "faces": {lado: {"texture": cara, "uv": [r(v) for v in uv[lado]]} for lado in uv},
    }


def plano_banda(textura):
    return {
        "from": [0, 4.3, 0], "to": [16, 4.3, 16],
        "faces": {"up": {"uv": [0, 0, 16, 16], "texture": textura}},
    }


def modelo_recta(q=""):
    return {
        "textures": {"marco": "femclothes:block/cinta_marco", "banda": "femclothes:block/cinta_banda" + q,
                     "particle": "femclothes:block/cinta_marco"},
        "elements": [
            caja([0, 0, 0], [16, 4, 16]),
            caja([0, 4, 0], [2, 6, 16]),
            caja([14, 4, 0], [16, 6, 16]),
            plano_banda("#banda"),
        ],
    }


def modelo_curva(espejo, q=""):
    nombre = "cinta_curva_der" if espejo else "cinta_curva_izq"
    elementos = [caja([0, 0, 0], [16, 4, 16])]

    def x_(a, b):
        return [16 - b, 16 - a] if espejo else [a, b]

    # Poste interior (esquina del pivote).
    xs = x_(0, 2)
    elementos.append(caja([xs[0], 4, 0], [xs[1], 6, 2]))
    # Pared exterior: columnas de 1 px entre r=14 y r=16 alrededor del pivote.
    for c in range(16):
        xc = c + 0.5
        z_in = math.sqrt(max(0.0, RADIO_ANILLO[1] ** 2 - xc ** 2)) if xc < RADIO_ANILLO[1] else 0.0
        z_out = min(16.0, math.sqrt(max(0.0, 16.0 ** 2 - xc ** 2)))
        if z_out - z_in < 0.2:
            continue
        xs = x_(c, c + 1)
        elementos.append(caja([xs[0], 4, round(z_in, 2)], [xs[1], 6, round(z_out, 2)]))
    return nombre + q, {
        "textures": {"marco": "femclothes:block/cinta_marco", "banda": f"femclothes:block/{nombre}{q}",
                     "particle": "femclothes:block/cinta_marco"},
        "elements": elementos + [plano_banda("#banda")],
    }


def plano_rampa(textura, y_centro, angulo):
    """Plano de la banda inclinado 45° alrededor de su centro (como los rieles en subida de vanilla): con
    rescale cubre 16 px de recorrido en z y sube/baja 16 px."""
    return {
        "from": [0, y_centro, 0], "to": [16, y_centro, 16],
        "rotation": {"origin": [8, y_centro, 8], "axis": "x", "angle": angulo, "rescale": True},
        "faces": {"up": {"uv": [0, 0, 16, 16], "texture": textura}},
    }


def modelo_rampa(sube, q=""):
    """Rampa de un bloque. Mira al norte: entra por el sur y sale por el norte, al nivel de la banda del bloque
    siguiente. Sube: de 4,05 px a 20,05 px. Baja (2026-10-04, "se superpone la rampa sobre el bloque de abajo"): va
    en el nivel de abajo, de 20,05 px (atrás, a la altura de la cinta que le entrega) a 4,05 px: nunca se mete
    en el bloque de abajo."""
    elementos = []
    signo = 1 if sube else -1

    def y_en(z):
        # z=16 (atrás) .. z=0 (adelante)
        return 4.05 + (16 - z) if sube else 4.05 + z

    for z0 in range(16):
        y_min = min(y_en(z0), y_en(z0 + 1))
        y_max = max(y_en(z0), y_en(z0 + 1))
        techo = round(y_min - 0.02, 2)
        elementos.append(caja([0, 0, z0], [16, techo, z0 + 1]))
        alto = round(y_max + 2, 2)
        for x0, x1 in ((0, 2), (14, 16)):
            elementos.append(caja([x0, techo, z0], [x1, alto, z0 + 1]))
    elementos.append(plano_rampa("#banda", 12.05, 45 * signo))
    return {
        "textures": {"marco": "femclothes:block/cinta_marco", "banda": "femclothes:block/cinta_banda" + q,
                     "particle": "femclothes:block/cinta_marco"},
        "elements": elementos,
    }


def modelo_empalme(q=""):
    """Empalme (2026-10-05, "un empalme que una hasta tres entradas laterales... una superior de tolva y una salida"):
    una losa con la banda arriba y cuatro postes en las esquinas, abierta por los costados y por atrás (entradas) y por
    el frente (salida, -Z). La tolva carga por arriba."""
    elementos = [caja([0, 0, 0], [16, 4, 16])]
    for x0 in (0, 14):
        for z0 in (0, 14):
            elementos.append(caja([x0, 4, z0], [x0 + 2, 5.5, z0 + 2]))   # postes bajitos: "bajale un poquito al bordecito" (2026-10-05)
    elementos.append(plano_banda("#banda"))
    return {
        "textures": {"marco": "femclothes:block/cinta_marco", "banda": "femclothes:block/cinta_banda" + q,
                     "particle": "femclothes:block/cinta_marco"},
        "elements": elementos,
    }


def escribir(ruta, datos):
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    with open(ruta, "w", encoding="utf-8") as f:
        json.dump(datos, f, indent=2)


def main():
    os.makedirs(TEX, exist_ok=True)
    marco()
    banda_recta()
    mcmeta("cinta_banda.png")
    for nombre, espejo in (("cinta_curva_izq.png", False), ("cinta_curva_der.png", True)):
        banda_curva(nombre, espejo)
        mcmeta(nombre)
    quietas()

    for q in ("", "_quieta"):
        escribir(os.path.join(MOD, "cinta_recta" + q + ".json"), modelo_recta(q))
        for espejo in (False, True):
            nombre, datos = modelo_curva(espejo, q)
            escribir(os.path.join(MOD, nombre + ".json"), datos)
        escribir(os.path.join(MOD, "cinta_rampa_sube" + q + ".json"), modelo_rampa(True, q))
        escribir(os.path.join(MOD, "cinta_rampa_baja" + q + ".json"), modelo_rampa(False, q))
        escribir(os.path.join(MOD, "empalme" + q + ".json"), modelo_empalme(q))

    giros = {"north": 0, "east": 90, "south": 180, "west": 270}
    variantes = {}
    for forma, modelo in (("recta", "cinta_recta"), ("curva_izq", "cinta_curva_izq"), ("curva_der", "cinta_curva_der"),
                           ("rampa_sube", "cinta_rampa_sube"), ("rampa_baja", "cinta_rampa_baja")):
        for lado, y in giros.items():
            for powered, q in (("false", ""), ("true", "_quieta")):
                v = {"model": f"femclothes:block/{modelo}{q}"}
                if y:
                    v["y"] = y
                variantes[f"facing={lado},forma={forma},powered={powered}"] = v
    escribir(os.path.join(ESTADOS, "cinta.json"), {"variants": variantes})
    escribir(os.path.join(ITEM, "cinta.json"), {"parent": "femclothes:block/cinta_recta"})

    # El empalme: la salida es el frente (-Z en el modelo), así que se gira igual que la cinta.
    variantes = {}
    for lado, y in giros.items():
        for powered, q in (("false", ""), ("true", "_quieta")):
            v = {"model": f"femclothes:block/empalme{q}"}
            if y:
                v["y"] = y
            variantes[f"facing={lado},powered={powered}"] = v
    escribir(os.path.join(ESTADOS, "empalme.json"), {"variants": variantes})
    escribir(os.path.join(ITEM, "empalme.json"), {"parent": "femclothes:block/empalme"})


if __name__ == "__main__":
    main()
