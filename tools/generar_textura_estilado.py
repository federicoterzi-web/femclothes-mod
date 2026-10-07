"""Genera el fondo de GUI de la Mesa de estilado (2026-10-01, "armar la gui
para probar" los apliques): pergamino con herrajes lila, visor grande a la
izquierda para la vista 3D (click = poner aplique) y, a la derecha, los 3
slots (prenda, molde, retazo) + inventario. Mismas coordenadas que
EstiladoScreenHandler / EstiladoScreen; botones los dibuja Java.

Uso: python tools/generar_textura_estilado.py
"""
from pathlib import Path

from PIL import Image

import generar_textura_tinturas as base

ANCHO, ALTO = 384, 256
X_DERECHA, Y_SLOTS, Y_INVENTARIO = 214, 30, 172

GUI = Path(__file__).resolve().parent.parent / "src/main/resources/assets/modamod/textures/gui/container"

# Mismos valores que EstiloPergamino.Tema.LILA en Java.
base.TEMAS["lila"] = dict(pergamino=[224, 206, 182], claro=[214, 170, 232], medio=[150, 90, 184], oscuro=[70, 34, 96])


def fondo():
    base.aplicar_tema("lila")
    img = base.pergamino(ANCHO, ALTO)
    img = base.marco_madera(img, 5)
    img = base.esquina(img, 0, 0, 1, 1)
    img = base.esquina(img, ANCHO - 1, 0, -1, 1)
    img = base.esquina(img, 0, ALTO - 1, 1, -1)
    img = base.esquina(img, ANCHO - 1, ALTO - 1, -1, -1)

    img = base.marco_visor(img, 8, 18, 204, 250)

    for i in range(3):
        img = base.slot(img, X_DERECHA + i * 26, Y_SLOTS)
    for i in range(3):
        for j in range(9):
            img = base.slot(img, X_DERECHA + j * 18, Y_INVENTARIO + i * 18)
    for j in range(9):
        img = base.slot(img, X_DERECHA + j * 18, Y_INVENTARIO + 58)

    img = base.costura_vertical(img, X_DERECHA - 5, 14, ALTO - 14)
    return img


def main():
    GUI.mkdir(parents=True, exist_ok=True)
    Image.fromarray(base.rgb(fondo())).save(GUI / "estilado.png")
    print("ok", GUI / "estilado.png")


if __name__ == "__main__":
    main()
