"""Íconos nuevos de rango (medias) y calce (pantalón) del usuario (2026-10-05, "nuevos iconos para calce y rango...
que se vean mas como los de remera que son mis favoritos" → "reemplacen, los de remera que me gustan son los de corte de cuello").
Parte de los recortes de 64x64 de assets_nuevos_prueba/medias_pantalon/ (rango_medias_0..5, calce_pantalon_0..4),
les baja la saturación del papel y suaviza el naranja hacia los tonos de los íconos de corte de cuello, y arma el séptimo
rango (máximo: línea de corte abajo de todo) desde el sexto.
Uso: python tools/ajustar_iconos_medias_pantalon.py            # vista previa en tools/vista_medias_pantalon.png
     python tools/ajustar_iconos_medias_pantalon.py --aplicar  # los copia a textures/item (respalda los actuales)
"""
import colorsys
import os
import shutil
import sys

import numpy as np
from PIL import Image

RAIZ = os.path.join(os.path.dirname(__file__), "..")
ORIGEN = os.path.join(RAIZ, "assets_nuevos_prueba", "medias_pantalon")
ITEMS = os.path.join(RAIZ, "src", "main", "resources", "assets", "femclothes", "textures", "item")
RESPALDO = os.path.join(RAIZ, "assets_viejos", "iconos_medias_pantalon_2026-10-05")
RANGOS = ["cero", "minimo", "corto", "medio", "mediolargo", "largo", "maximo"]
CALCES = ["pegado", "ajustado", "normal", "suelto", "oversize"]
NARANJA_CUELLO = np.array([226, 98, 40])


def entonar(img):
    """Papel menos saturado y naranja más quemado, como los íconos de corte de cuello."""
    a = np.array(img.convert("RGBA")).astype(float)
    out = a.copy()
    for y in range(64):
        for x in range(64):
            r, g, b, al = a[y, x]
            if al < 8:
                continue
            naranja = r - b > 170 and g < 140 and r > 200
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            if naranja:
                mezcla = 0.4
                out[y, x, :3] = (1 - mezcla) * np.array([r, g, b]) + mezcla * NARANJA_CUELLO
            elif v > 0.45:                                   # papel, tela y marco claro; el contorno oscuro no se toca
                s = s * 0.52
                v = min(1.0, v * 1.0 + 0.015)
                out[y, x, :3] = [c * 255 for c in colorsys.hsv_to_rgb(h, s, v)]
    return Image.fromarray(out.clip(0, 255).astype(np.uint8), "RGBA")


def fila_linea(img):
    a = np.array(img.convert("RGB")).astype(int)
    nar = (a[..., 0] - a[..., 2] > 170) & (a[..., 1] < 140) & (a[..., 0] > 200)
    nar[:, :16] = False
    nar[:, 48:] = False                                     # solo el tramo del medio: la línea, no la chincheta ni la tijera
    return int(np.argmax(nar.sum(axis=1)))


def maximo_desde(sexto, baja):
    """Séptimo nivel: baja la línea, la chincheta y la tijera `baja` px (la zona vieja se rellena con el papel de abajo)."""
    a = np.array(sexto).copy()
    yl = fila_linea(sexto)
    y0, y1 = yl - 8, yl + 7                                  # banda con chincheta, línea y tijera
    banda = a[y0:y1].copy()
    # relleno de lo que queda libre: filas de arriba de la banda repetidas (papel y piernas son casi verticales)
    for y in range(y0, y1):
        a[y] = a[y0 - 1]
    # la banda se pega más abajo, sin pisar el contorno inferior del ícono
    a[y0 + baja:y1 + baja] = banda
    a[55:] = np.array(sexto)[55:]                             # el marco de abajo no se toca
    return Image.fromarray(a, "RGBA")


def main():
    rangos = [entonar(Image.open(os.path.join(ORIGEN, f"rango_medias_{i}.png"))) for i in range(6)]
    rangos.append(maximo_desde(rangos[5], 5))
    calces = [entonar(Image.open(os.path.join(ORIGEN, f"calce_pantalon_{i}.png"))) for i in range(5)]
    E = 4
    todos = rangos + calces
    hoja = Image.new("RGB", (len(todos) * (64 * E + 8) + 8, 64 * E + 16), (60, 52, 46))
    for i, im in enumerate(todos):
        fondo = Image.new("RGB", (64, 64), (60, 52, 46))
        fondo.paste(im, (0, 0), im)
        hoja.paste(fondo.resize((64 * E, 64 * E), Image.NEAREST), (8 + i * (64 * E + 8), 8))
    hoja.save(os.path.join(os.path.dirname(__file__), "vista_medias_pantalon.png"))
    if "--aplicar" in sys.argv:
        os.makedirs(RESPALDO, exist_ok=True)
        pares = [(f"molde_rango_{n}.png", im) for n, im in zip(RANGOS, rangos)] + \
                [(f"molde_calce_{n}.png", im) for n, im in zip(CALCES, calces)]
        for nombre, im in pares:
            ruta = os.path.join(ITEMS, nombre)
            dst = os.path.join(RESPALDO, nombre)
            if os.path.exists(ruta) and not os.path.exists(dst):
                shutil.copy(ruta, dst)
            im.save(ruta)


if __name__ == "__main__":
    main()
