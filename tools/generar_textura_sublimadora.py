"""Genera el fondo de GUI de la Sublimadora en estilo pergamino / madera /
latón (2026-09-28, "cambiemos la gui de la sublimadora para hacerla
sintonizar con sus bloques hermanos"): mismo panel de 560x408 y mismas
columnas que la Estación de Tintes — reusa sus funciones de dibujo
(generar_textura_tinturas.py). Mismas coordenadas exactas que
SublimadoraScreenHandler.java para los Slot reales.

La columna del medio lleva un cuadro con marco de madera donde va el
dibujo de la prenda (Frente | Espalda): la prenda la dibuja Java (es el
ítem real, con su color), acá solo el cuadro y los dos marcos de foto.

Uso: python3 tools/generar_textura_sublimadora.py (desde la raíz del repo).
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(__file__))
import generar_textura_tinturas as base  # noqa: E402
from generar_textura_tinturas import (  # noqa: E402
    ALTO, ANCHO, M_DERECHA, M_MEDIO, M_MEDIO_ANCHO, Image, costura_vertical, esquina, flecha,
    marco_madera, marco_visor, pergamino, rgb, slot, slot_grande)

ESQUEMA_Y, ESQUEMA_ALTO = 36, 136
# SublimadoraScreenHandler.FOTO_POS (x relativa a la columna del medio).
FOTOS = [(52, 104), (172, 104)]


def cuadro_esquema(img):
    """Cuadro de madera fina con pergamino apenas más oscuro adentro, del tamaño de los esquemas de Tintes."""
    x0, y0 = M_MEDIO, ESQUEMA_Y
    sub = img[y0:y0 + ESQUEMA_ALTO, x0:x0 + M_MEDIO_ANCHO].copy()
    sub *= 0.94
    sub = marco_madera(sub, 4)
    for (ex, ey, dx, dy) in ((0, 0, 1, 1), (M_MEDIO_ANCHO - 1, 0, -1, 1),
                             (0, ESQUEMA_ALTO - 1, 1, -1), (M_MEDIO_ANCHO - 1, ESQUEMA_ALTO - 1, -1, -1)):
        sub = esquina(sub, ex, ey, dx, dy, lado=12)
    img[y0:y0 + ESQUEMA_ALTO, x0:x0 + M_MEDIO_ANCHO] = sub
    # Separador cosido entre Frente y Espalda.
    return costura_vertical(img, M_MEDIO + M_MEDIO_ANCHO // 2, ESQUEMA_Y + 8, ESQUEMA_Y + ESQUEMA_ALTO - 8)


def main():
    base.aplicar_tema("oro")   # dorado (2026-09-29), ver TEMAS en generar_textura_tinturas.py
    img = pergamino(ANCHO, ALTO)
    img = marco_madera(img, 5)
    img = esquina(img, 0, 0, 1, 1)
    img = esquina(img, ANCHO - 1, 0, -1, 1)
    img = esquina(img, 0, ALTO - 1, 1, -1)
    img = esquina(img, ANCHO - 1, ALTO - 1, -1, -1)

    # Visor 3D, columna izquierda — igual que Tintes.
    img = marco_visor(img, 8, 18, 94, 214)

    # Columna del medio: cuadro de la prenda con los dos slots de foto,
    # cinturón Entrada -> Salida, inventario y hotbar.
    img = cuadro_esquema(img)
    for (fx, fy) in FOTOS:
        img = slot(img, M_MEDIO + fx, fy)
    img = slot_grande(img, M_MEDIO + 40, 176)
    img = slot_grande(img, M_MEDIO + 168, 176)
    img = flecha(img, M_MEDIO + 96, 184, 48, 16)
    for fila in range(3):
        for col in range(9):
            img = slot(img, M_MEDIO + col * 18, 324 + fila * 18)
    for col in range(9):
        img = slot(img, M_MEDIO + col * 18, 382)

    # Columna derecha: almacén de fotos, 3 filas de 9 (2026-09-28, "quiero
    # mas espacios de almacenamiento"). Los tanques de tinta y papel los
    # dibuja Java.
    for i in range(27):
        img = slot(img, M_DERECHA + (i % 9) * 18, 200 + (i // 9) * 18)

    img = costura_vertical(img, M_MEDIO - 9, 16, ALTO - 16)
    img = costura_vertical(img, M_DERECHA - 9, 16, ALTO - 16)

    out = "src/main/resources/assets/modamod/textures/gui/container/sublimadora.png"
    Image.fromarray(rgb(img)).save(out)
    print("guardado:", out, ANCHO, "x", ALTO)


if __name__ == "__main__":
    np.seterr(all="ignore")
    main()
