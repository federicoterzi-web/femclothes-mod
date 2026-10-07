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
from collections import deque
import shutil
import sys

import numpy as np
from PIL import Image

RAIZ = os.path.join(os.path.dirname(__file__), "..")
ORIGEN = os.path.join(RAIZ, "assets_nuevos_prueba", "medias_pantalon")
ITEMS = os.path.join(RAIZ, "src", "main", "resources", "assets", "modamod", "textures", "item")
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


# Papel doblado de los íconos de corte de cuello (2026-10-05, "el estilo con respecto al fondo y las cuatro areas
# iluminadas que lo hacen parecer un papel doblado"): cuatro cuadrantes planos con distinta luz y dos pliegues en cruz.
CUADRANTES = {(0, 0): (236, 210, 178), (1, 0): (243, 221, 195), (0, 1): (217, 184, 146), (1, 1): (240, 212, 186)}
PLIEGUE = np.array([204, 170, 130])


def cuadrante(x, y):
    return CUADRANTES[(1 if x >= 32 else 0, 1 if y >= 33 else 0)]


def papel_doblado(img, ref=None):
    """Cambia el papel del fondo por los cuatro cuadrantes (y entona la tela con la misma luz)."""
    a = np.array(img.convert("RGBA"))
    rgb = a[..., :3].astype(int)
    lum = rgb.sum(axis=2) / 3
    # la clasificación mira el recorte original (antes de bajar la saturación): papel = tostado, tela = casi blanca
    o = np.array((ref or img).convert("RGB")).astype(int)
    nar = (o[..., 0] - o[..., 2] > 150) & (o[..., 1] < 160)
    oscuro = (o.sum(axis=2) / 3) < 120
    barrera = oscuro | nar
    visto = (o[..., 0] - o[..., 2] > 62) & ~barrera
    # el marco: franja de 3 px alrededor; ahí no se toca
    marco = np.ones((64, 64), bool)
    marco[3:61, 3:61] = False
    out = a.copy()
    for y in range(64):
        for x in range(64):
            if marco[y, x] or a[y, x, 3] < 8 or barrera[y, x]:
                continue
            base = np.array(cuadrante(x, y), float)
            if visto[y, x]:
                out[y, x, :3] = base
            else:                                            # tela: conserva su sombreado sobre el color del cuadrante
                out[y, x, :3] = np.clip(base * min(1.0, lum[y, x] / 236) * 1.0, 0, 255)
    for x in range(3, 61):                                   # pliegues en cruz, sin pisar contornos ni naranja
        for (px, py) in ((x, 33), (32, x)):
            if not barrera[py, px] and not marco[py, px] and a[py, px, 3] >= 8:
                out[py, px, :3] = (out[py, px, :3] * 0.55 + PLIEGUE * 0.45)
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
    def listo(ruta):
        original = Image.open(ruta)
        return papel_doblado(entonar(original), original)
    rangos = [listo(os.path.join(ORIGEN, f"rango_medias_{i}.png")) for i in range(6)]
    rangos.append(maximo_desde(rangos[5], 5))
    calces = [listo(os.path.join(ORIGEN, f"calce_pantalon_{i}.png")) for i in range(5)]
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
