"""[Desde el 2026-10-04 las texturas cuerpo_* de abajo ya NO se generan: el recorte lo hace el juego en runtime, ver
sublimadora/CuelloRecorte.java (mismas formas). Este script solo genera los íconos corte_* y sus modelos.]

Genera las texturas de los cuellos CUADRADO y CORAZON de la remera.

A pedido (2026-10-04, "esos son los moldes para cuello redondo en v polera cuadrado y corazon... haceme el cuello
cuadrado y en forma de corazon" + "el molde corazon tiene que acompañar la silueta de los pechos"):

  * textures/entity/cuerpo_<largo>_<manga>_<cuello>.png (512x512, 8x la skin): se parte de la textura del cuello
    REDONDO del mismo corte y se le recorta (alfa 0) el escote nuevo. El torso de frente ocupa x 160..223, y 160..255
    (8 texeles de ancho); la tapa de arriba, y 128..159. El agujero de la tapa se copia del cuello en V (2 texeles de
    ancho en las 4 filas) y el del frente es la forma nueva, en coordenadas de texel (0..8, y hacia abajo).
  * textures/item/corte_<largo>_<manga>_<cuello>.png (16x16): lo mismo, a escala de ítem.
  * models/item/corte_*.json: los mismos modelos que los cuellos de siempre.

Formas (en texeles del frente del torso, x de 0 a 8):
  cuadrado: x 2..6, hasta 1.75 de hondo.
  corazon:  dos lóbulos que son el arco de arriba de cada pecho (centros (2, 5.0) y (6, 5.0), radio 2: puntos altos de la tela en y 3.0) y se juntan
            en una punta al centro, x 1..7. Hasta 5.0 de hondo (bajado el 2026-10-04: la tela arranca justo arriba del medio del pecho).

Uso: python tools/generar_cuellos.py
"""
import json
import math
from pathlib import Path

from PIL import Image

RAIZ = Path(__file__).resolve().parent.parent / "src/main/resources/assets/femclothes"
ENT = RAIZ / "textures/entity"
ITEM = RAIZ / "textures/item"
MODELOS = RAIZ / "models/item"

K = 8                      # escala de las texturas de cuerpo (512 / 64)
X0, Y0 = 160, 160          # esquina del frente del torso
CAPA_X, CAPA_Y0, CAPA_Y1 = (160, 224), 128, 160


# Dónde queda el pico del medio del corazón (2026-10-04, "el corte de cuello corazon lo bajaria para que empiecen a
# cubrir justo encima del medio de la teta"): antes 2.7; así los puntos más altos de la tela quedan en 3.0.
CORAZON_BAJA = 5.0
# Ancho (en texeles, de 0 a 8) del agujero de la tapa de arriba: el mismo que el corte del frente.
ANCHO_TAPA = {"cuadrado": (2.0, 6.0), "corazon": (1.0, 7.0)}


def agujero(cuello, x, y):
    """x, y en texeles del frente: True si ese punto queda recortado."""
    if cuello == "cuadrado":
        return 2.0 <= x <= 6.0 and y < 1.75
    if cuello == "corazon":
        if not 1.0 <= x <= 7.0:
            return False
        cx = 2.0 if x <= 4.0 else 6.0
        borde = CORAZON_BAJA - math.sqrt(max(0.0, 4.0 - (x - cx) ** 2))
        return y < borde
    return False


def cuerpo(largo_manga):
    base = Image.open(ENT / f"cuerpo_{largo_manga}_redondo.png").convert("RGBA")
    en_v = Image.open(ENT / f"cuerpo_{largo_manga}_v.png").convert("RGBA")
    for cuello in ("cuadrado", "corazon"):
        im = base.copy()
        px, pv = im.load(), en_v.load()
        # La tapa de arriba acompaña el ancho del corte del frente (2026-10-04, "la tapa de arriba de la ropa deberia
        # acompañar el ancho del corte del cuello de frente"): antes copiaba el agujero de la V (2 texeles) y el
        # cuadrado (4) y el corazón (6) quedaban con la tapa cerrada. Las 4 filas de profundidad.
        ancho = ANCHO_TAPA[cuello]
        for y in range(CAPA_Y0, CAPA_Y1):
            for x in range(*CAPA_X):
                if ancho[0] <= (x - CAPA_X[0] + 0.5) / K <= ancho[1]:
                    px[x, y] = (0, 0, 0, 0)
        # El frente: la forma nueva, a resolución 8x.
        for y in range(Y0, Y0 + 5 * K + 8):
            for x in range(X0, X0 + 8 * K):
                if agujero(cuello, (x - X0 + 0.5) / K, (y - Y0 + 0.5) / K):
                    px[x, y] = (0, 0, 0, 0)
        im.save(ENT / f"cuerpo_{largo_manga}_{cuello}.png")


def icono(largo_manga):
    """El ícono de 16x16: un escote de pocos píxeles sobre el del cuello redondo."""
    ruta = ITEM / f"corte_{largo_manga}_redondo.png"
    if not ruta.exists():
        return
    base = Image.open(ruta).convert("RGBA")
    quitar = {
        "cuadrado": [(6, 2), (7, 2), (8, 2), (9, 2), (6, 3), (7, 3), (8, 3), (9, 3)],
        "corazon": [(6, 2), (7, 2), (8, 2), (9, 2), (5, 3), (6, 3), (7, 3), (8, 3), (9, 3), (10, 3),
                    (7, 4), (8, 4), (7, 5), (8, 5)],
    }
    for cuello, pixeles in quitar.items():
        im = base.copy()
        for p in pixeles:
            im.putpixel(p, (0, 0, 0, 0))
        im.save(ITEM / f"corte_{largo_manga}_{cuello}.png")


def modelos():
    for ruta in sorted(MODELOS.glob("corte_*_redondo.json")):
        corte = ruta.name[len("corte_"):-len("_redondo.json")]
        for cuello in ("cuadrado", "corazon"):
            datos = json.loads(ruta.read_text(encoding="utf-8"))
            datos["textures"]["layer0"] = f"femclothes:item/corte_{corte}_{cuello}"
            (MODELOS / f"corte_{corte}_{cuello}.json").write_text(json.dumps(datos, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    cortes = sorted(p.name[len("cuerpo_"):-len("_redondo.png")] for p in ENT.glob("cuerpo_*_redondo.png"))
    # Las texturas cuerpo_* ya no se generan: cuadrado y corazón se recortan en runtime
    # (sublimadora/CuelloRecorte.java, 2026-10-04); cuerpo() queda de referencia de las formas.
    for ruta in ITEM.glob("corte_*_redondo.png"):
        icono(ruta.name[len("corte_"):-len("_redondo.png")])
    modelos()
    print("modelos:", len(list(MODELOS.glob("corte_*_cuadrado.json"))) * 2)
