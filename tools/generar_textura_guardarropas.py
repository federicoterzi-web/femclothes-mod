"""Genera los fondos de GUI del Guardarropas y del Maniquí en el estilo del mod
(pergamino / madera / metal), con el metal de los acentos de cada mueble —
2026-09-30, "armame la gui de el guardarropas y el maniqui con el estilo del
mod y los colores de sus acentos metalicos":

  - Guardarropas: acero plateado (las bisagras, manijas y la percha del
    modelo wardrobe, tiles grises del atlas).
  - Maniquí: azul esmaltado (las esquinas, la barra del frente y el parante
    del modelo mannequin, tiles azules del atlas).

Reusa las funciones de dibujo de generar_textura_tinturas.py (importadas: leen
la paleta del módulo, que aplicar_tema reasigna). Mismas coordenadas que
GuardarropasScreenHandler / ManiquiScreenHandler para los Slot reales:
grilla de 5 categorías x 4 capas en (M_MEDIO + c*20, 20 + r*20), columna de
armadura en X_ARMADURA, inventario en (M_MEDIO + j*18, 180 + i*18) y hotbar
en y=238. Los botones y sliders los dibuja Java (EstiloPergamino).

Uso: python tools/generar_textura_guardarropas.py
"""
from pathlib import Path

from PIL import Image

import generar_textura_tinturas as base

ANCHO, ALTO = 482, 264
M_MEDIO = 19 + 100
CATEGORIAS, CAPAS, ARMADURAS = 5, 4, 4
X_ARMADURA = M_MEDIO + CATEGORIAS * 20 + 6
X_SLIDERS = M_MEDIO + 170          # ManiquiScreen: sliders de pose a la derecha

GUI = Path(__file__).resolve().parent.parent / "src/main/resources/assets/femclothes/textures/gui/container"

# Mismos valores que EstiloPergamino.Tema.PLATA / AZUL en Java.
base.TEMAS["plata"] = dict(pergamino=[214, 200, 170], claro=[230, 234, 240], medio=[150, 157, 168], oscuro=[66, 72, 84])
base.TEMAS["azul"] = dict(pergamino=[214, 198, 162], claro=[150, 184, 230], medio=[76, 111, 159], oscuro=[30, 52, 90])


def fondo(tema, sliders):
    base.aplicar_tema(tema)
    img = base.pergamino(ANCHO, ALTO)
    img = base.marco_madera(img, 5)
    img = base.esquina(img, 0, 0, 1, 1)
    img = base.esquina(img, ANCHO - 1, 0, -1, 1)
    img = base.esquina(img, 0, ALTO - 1, 1, -1)
    img = base.esquina(img, ANCHO - 1, ALTO - 1, -1, -1)

    # Vista previa del jugador (PREVIEW 8,18 .. 94,164) — el botón de vista va abajo, en Java.
    img = base.marco_visor(img, 8, 18, 94, 164)

    for c in range(CATEGORIAS):
        for r in range(CAPAS):
            img = base.slot(img, M_MEDIO + c * 20, 20 + r * 20)
    for i in range(ARMADURAS):
        img = base.slot(img, X_ARMADURA, 20 + i * 20)

    for i in range(3):
        for j in range(9):
            img = base.slot(img, M_MEDIO + j * 18, 180 + i * 18)
    for j in range(9):
        img = base.slot(img, M_MEDIO + j * 18, 238)

    img = base.costura_vertical(img, M_MEDIO - 12, 14, ALTO - 14)
    if sliders:
        img = base.costura_vertical(img, X_SLIDERS - 6, 14, 172)
    return img


def main():
    GUI.mkdir(parents=True, exist_ok=True)
    Image.fromarray(base.rgb(fondo("plata", False))).save(GUI / "guardarropas.png")
    Image.fromarray(base.rgb(fondo("azul", True))).save(GUI / "maniqui.png")
    print("ok", GUI / "guardarropas.png", GUI / "maniqui.png")


if __name__ == "__main__":
    main()
