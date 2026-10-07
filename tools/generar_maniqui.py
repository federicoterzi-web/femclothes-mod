"""Genera la skin del Maniquí: textures/entity/maniqui.png (64x64, brazos finos).

A pedido (2026-09-30, "llegue al limite con claude design lo haces vos"): una
skin de maniquí de sastrería, madera clara/lino beige, sin cara, con las
juntas marcadas (cuello, hombros, codos, muñecas, cadera, rodillas, tobillos)
y costuras de tela en el torso. Solo la capa BASE: la zona de la segunda capa
queda transparente (ManiquiRenderer la apaga igual).

Layout de caja de Minecraft para una caja en (u, v) de w x h x d:
    arriba (u+d, v, w, d)      abajo (u+d+w, v, w, d)
    der.   (u, v+d, d, h)      frente (u+d, v+d, w, h)
    izq.   (u+d+w, v+d, d, h)  atrás (u+2d+w, v+d, w, h)

Uso: python tools/generar_maniqui.py
"""
import random
from pathlib import Path

from PIL import Image

SALIDA = Path(__file__).resolve().parent.parent / "src/main/resources/assets/modamod/textures/entity/maniqui.png"

BASE = (214, 188, 150)      # #D6BC96 lino/madera clara
SOMBRA = (184, 154, 112)    # #B89A70
COSTURA = (140, 112, 80)    # #8C7050
LUZ = (228, 206, 172)

random.seed(7)
img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
px = img.load()


def mezclar(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def pintar(x, y, color):
    # Veta suave: un poco de ruido por píxel, para que no se vea plástico.
    r = random.uniform(-0.035, 0.035)
    c = tuple(max(0, min(255, round(v * (1 + r)))) for v in color)
    px[x, y] = c + (255,)


def caras(u, v, w, h, d):
    """Rectángulos (x, y, ancho, alto) de las 6 caras, por nombre."""
    return {
        "arriba": (u + d, v, w, d),
        "abajo": (u + d + w, v, w, d),
        "der": (u, v + d, d, h),
        "frente": (u + d, v + d, w, h),
        "izq": (u + d + w, v + d, d, h),
        "atras": (u + 2 * d + w, v + d, w, h),
    }


# Cuánto se oscurece cada cara: luz de arriba, costados en sombra suave.
TONO = {"arriba": 0.0, "abajo": 0.55, "frente": 0.12, "atras": 0.3, "der": 0.4, "izq": 0.4}


def caja(u, v, w, h, d, juntas=(), costuras_v=(), linea_cintura=None):
    """Pinta una caja. juntas: filas (0..h-1) con una junta (línea oscura en
    las 4 caras laterales). costuras_v: columnas del FRENTE y de ATRÁS con
    costura vertical. linea_cintura: fila con costura horizontal."""
    for nombre, (x0, y0, cw, ch) in caras(u, v, w, h, d).items():
        base = mezclar(LUZ, SOMBRA, TONO[nombre])
        for y in range(ch):
            for x in range(cw):
                color = base
                lateral = nombre in ("der", "frente", "izq", "atras")
                if lateral and y in juntas:
                    color = COSTURA
                elif lateral and linea_cintura is not None and y == linea_cintura:
                    color = mezclar(base, COSTURA, 0.6)
                elif nombre in ("frente", "atras") and x in costuras_v:
                    color = mezclar(base, COSTURA, 0.55)
                elif lateral and (x == 0 or x == cw - 1):
                    # Canto de la caja apenas más oscuro: se lee el volumen.
                    color = mezclar(base, SOMBRA, 0.35)
                pintar(x0 + x, y0 + y, color)


# Cabeza (0,0) 8x8x8: lisa, sin cara, con una costura vertical al medio
# adelante y atrás; el cuello (última fila) marcado.
caja(0, 0, 8, 8, 8, juntas=(7,), costuras_v=(3, 4))
# Torso (16,16) 8x12x4: costura central y dos "princesa", cintura en la fila 8,
# junta de cuello arriba y de cadera abajo.
caja(16, 16, 8, 12, 4, juntas=(0, 11), costuras_v=(1, 3, 4, 6), linea_cintura=8)
# Brazos finos (3 de ancho): hombro, codo (fila 5) y muñeca (fila 10).
caja(40, 16, 3, 12, 4, juntas=(0, 5, 10))
caja(32, 48, 3, 12, 4, juntas=(0, 5, 10))
# Piernas: cadera, rodilla (fila 5) y tobillo (fila 10).
caja(0, 16, 4, 12, 4, juntas=(0, 5, 10))
caja(16, 48, 4, 12, 4, juntas=(0, 5, 10))

SALIDA.parent.mkdir(parents=True, exist_ok=True)
img.save(SALIDA)
print("ok", SALIDA)
