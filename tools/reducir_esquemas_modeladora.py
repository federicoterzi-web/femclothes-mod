"""Saca de los esquemas de la Modeladora los marcos sobrantes de trama (2026-10-08, "podemos sacar dos customizaciones
total lo unico que entra ahi hasta ahora es la trama" + "sacalos en todas"): en cada prenda queda UN lugar de trama.
Pantalón: sin MAT2/MAT3; medias y calientabrazos: sin los lugares 2 y 3 de cada lado; pollera y capa: sin MAT2/MAT3.
La remera/Top se rearma aparte (tools/generar_esquemas_top.py). Idempotente sobre los PNG originales guardados en
assets_viejos/esquemas_modeladora_2026-10-08/ (la primera corrida los copia).

Uso: python tools/reducir_esquemas_modeladora.py
"""
import os
import shutil
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(__file__))
from quitar_marcos_esquema import quitar  # noqa: E402

RAIZ = os.path.join(os.path.dirname(__file__), "..")
GUI = os.path.join(RAIZ, "src", "main", "resources", "assets", "modamod", "textures", "gui", "container")
COPIA = os.path.join(RAIZ, "assets_viejos", "esquemas_modeladora_2026-10-08")
ESQUEMA_Y = 36

# prenda -> posiciones PIN_POS (x, y) de los marcos a sacar. Pantalón: se queda el del medio (el de la entrepierna);
# los otros dos están uno sobre la bragueta y otro entre las piernas.
QUITAR = {
    "pantalon": [(112, 79), (112, 122)],
    "medias": [(28, 94), (28, 113), (196, 94), (196, 113)],
    "calientabrazos": [(28, 96), (28, 115), (196, 96), (196, 115)],
    "pollera": [(181, 91), (181, 118)],
    "capa": [(190, 132), (190, 76)],
}


# prenda -> marcos que se quedan (PIN_POS)
QUEDAN = {
    "pantalon": [(112, 46), (112, 100), (47, 95), (49, 143), (175, 144)],
    "medias": [(44, 52), (180, 52), (43, 142), (184, 142), (112, 142), (28, 76), (196, 76)],
    "calientabrazos": [(38, 55), (185, 55), (37, 134), (187, 135), (112, 144), (28, 77), (196, 77)],
    "pollera": [(53, 55), (39, 134), (39, 95), (181, 66), (184, 144), (17, 69), (74, 144)],
    "capa": [(190, 100), (48, 128), (175, 54), (48, 56), (48, 89)],
}


def centro(x, y):
    return ((x + 8) * 4, (y - ESQUEMA_Y + 8) * 4)


def main():
    os.makedirs(COPIA, exist_ok=True)
    for prenda, pines in QUITAR.items():
        origen = os.path.join(COPIA, f"esquema_{prenda}.png")
        destino = os.path.join(GUI, f"esquema_{prenda}.png")
        if not os.path.exists(origen):
            shutil.copyfile(destino, origen)
        im = Image.open(origen)
        hecho = quitar(im, [centro(x, y) for x, y in pines], [centro(x, y) for x, y in QUEDAN[prenda]])
        hecho.quantize(colors=256, method=Image.MEDIANCUT).save(destino, optimize=True)
        print("esquema_%s.png: %d marcos menos" % (prenda, len(pines)))


if __name__ == "__main__":
    main()
