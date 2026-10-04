"""Ajuste de los íconos de rango dibujados por el usuario (2026-10-04, "vamos con la de la remera... A"):
parte de los 7 PNG de 64x64 de assets_nuevos_prueba/ (recortados de su imagen) y
  1) pasa el naranja de la línea, la chincheta y la tijera al cobre de la Modeladora,
  2) engruesa la línea de corte (3 px, a trazos, con brillo y sombra),
  3) borra las puntadas grises de adentro de la remera (un filtro de mediana solo dentro del contorno).
Uso: python tools/ajustar_iconos_rango.py            # deja los resultados en assets_nuevos_prueba/ajustados/
     python tools/ajustar_iconos_rango.py --aplicar  # los copia a textures/item (respalda los actuales)
"""
import os
import shutil
import sys
from collections import deque

import numpy as np
from PIL import Image, ImageFilter

RAIZ = os.path.join(os.path.dirname(__file__), "..")
ORIGEN = os.path.join(RAIZ, "assets_nuevos_prueba")
SALIDA = os.path.join(ORIGEN, "ajustados")
ITEMS = os.path.join(RAIZ, "src", "main", "resources", "assets", "femclothes", "textures", "item")
RESPALDO = os.path.join(RAIZ, "assets_viejos", "molde_rango_2026-10-04")
NOMBRES = ["cero", "minimo", "corto", "medio", "mediolargo", "largo", "maximo"]

COBRE_CLARO = np.array([242, 170, 120])
COBRE_MEDIO = np.array([184, 102, 56])
COBRE_OSCURO = np.array([96, 44, 22])


def mascaras(a):
    r, g, b = a[..., 0].astype(int), a[..., 1].astype(int), a[..., 2].astype(int)
    lum = (r + g + b) / 3
    naranja = (r > 170) & (r - b > 95) & (g > 60) & (g < 200)
    gris = (np.abs(r - g) < 22) & (np.abs(g - b) < 26) & (lum > 110) & (lum < 215)
    oscuro = lum < 105
    return naranja, gris, oscuro


def interior(oscuro, barrera, inicio):
    """Relleno por inundación desde `inicio` sin cruzar el contorno oscuro ni las piezas (chincheta/tijera)."""
    h, w = oscuro.shape
    ocupado = oscuro | barrera
    visto = np.zeros_like(ocupado)
    cola = deque([inicio])
    visto[inicio[1], inicio[0]] = True
    while cola:
        x, y = cola.popleft()
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, ny = x + dx, y + dy
            if 0 <= nx < w and 0 <= ny < h and not visto[ny, nx] and not ocupado[ny, nx]:
                visto[ny, nx] = True
                cola.append((nx, ny))
    return visto


def dilatar(m, n=1):
    out = m.copy()
    for _ in range(n):
        p = out.copy()
        p[1:, :] |= out[:-1, :]
        p[:-1, :] |= out[1:, :]
        p[:, 1:] |= out[:, :-1]
        p[:, :-1] |= out[:, 1:]
        out = p
    return out


def ajustar(img):
    a = np.array(img.convert("RGB"))
    naranja, gris, oscuro = mascaras(a)
    # La línea de corte: la fila con más naranja.
    cuenta = naranja.sum(axis=1)
    yl = int(np.argmax(cuenta))
    banda = [y for y in range(max(0, yl - 3), min(64, yl + 4)) if cuenta[y] >= 0.5 * cuenta[yl]]
    y0, y1 = (min(banda), max(banda)) if banda else (yl, yl)

    # Extremos: la chincheta y la tijera sobresalen de la banda, así que se miden en las filas de arriba y abajo.
    fuera = np.zeros_like(naranja)
    fuera[max(0, y0 - 7):max(0, y0 - 1), :] = True
    fuera[min(64, y1 + 2):min(64, y1 + 8), :] = True
    pieza_px = (naranja | gris) & fuera
    cols = np.where(pieza_px.any(axis=0))[0]
    mitad = (int(np.where(naranja[y0:y1 + 1].any(axis=0))[0].min()) + int(np.where(naranja[y0:y1 + 1].any(axis=0))[0].max())) // 2
    izq = cols[cols < mitad]
    der = cols[cols >= mitad]
    x_pin = int(izq.max()) if len(izq) else int(np.where(naranja[y0:y1 + 1].any(axis=0))[0].min()) + 6
    x_tij = int(der.min()) if len(der) else int(np.where(naranja[y0:y1 + 1].any(axis=0))[0].max()) - 8
    xa, xb = x_pin + 2, x_tij - 2           # tramo libre de la línea

    # Piezas = todo lo naranja/gris que NO es el tramo libre de la línea.
    tramo = np.zeros_like(naranja)
    tramo[max(0, y0 - 1):y1 + 2, xa:xb + 1] = True
    piezas = (naranja | gris) & ~tramo
    barrera = dilatar(piezas, 1)
    dentro = interior(oscuro, barrera | (tramo & naranja), (32, 40))

    # 3) puntadas de adentro: mediana (5x5, dos pasadas) solo dentro del contorno y lejos de línea y piezas
    suave = Image.fromarray(a).filter(ImageFilter.MedianFilter(5)).filter(ImageFilter.MedianFilter(5))
    suave = np.array(suave)
    zona = dentro & ~barrera
    zona[max(0, y0 - 1):y1 + 2, xa:xb + 1] = False
    b = a.copy()
    b[zona] = suave[zona]

    # borrar la línea vieja: mezcla vertical entre la fila de arriba y la de abajo de la banda
    ya, yb = max(0, y0 - 3), min(63, y1 + 3)
    for x in range(xa, xb + 1):
        for y in range(y0 - 1, y1 + 2):
            if naranja[y, x]:
                t = (y - ya) / max(1, (yb - ya))
                b[y, x] = ((1 - t) * b[ya, x] + t * b[yb, x]).astype(np.uint8)

    # 1) recolorear el naranja de las piezas → cobre según el brillo
    for (y, x) in zip(*np.where(naranja & piezas)):
        v = int(a[y, x, 1])      # el verde separa luz, cuerpo y sombra del naranja
        b[y, x] = COBRE_CLARO if v > 140 else COBRE_MEDIO if v > 95 else COBRE_OSCURO

    # nueva línea de corte: 3 px, trazos de 4 con 2 de hueco, brillo arriba y sombra abajo
    yc = (y0 + y1) // 2
    for x in range(xa, xb + 1):
        if ((x - xa) % 6) < 4:
            b[yc - 1, x] = COBRE_CLARO
            b[yc, x] = COBRE_MEDIO
            b[yc + 1, x] = COBRE_OSCURO
    return Image.fromarray(b)


def main():
    os.makedirs(SALIDA, exist_ok=True)
    res = []
    for n in NOMBRES:
        im = ajustar(Image.open(os.path.join(ORIGEN, f"molde_rango_{n}.png")))
        im.save(os.path.join(SALIDA, f"molde_rango_{n}.png"))
        res.append(im)
    E = 4
    hoja = Image.new("RGB", (7 * (64 * E + 10) + 10, 2 * 64 * E + 30 + 64), (60, 52, 46))
    for i, im in enumerate(res):
        orig = Image.open(os.path.join(ORIGEN, f"molde_rango_{NOMBRES[i]}.png")).convert("RGB")
        hoja.paste(orig.resize((64 * E, 64 * E), Image.NEAREST), (10 + i * (64 * E + 10), 10))
        hoja.paste(im.resize((64 * E, 64 * E), Image.NEAREST), (10 + i * (64 * E + 10), 64 * E + 20))
        hoja.paste(im, (10 + i * (64 * E + 10), 2 * 64 * E + 30))
    hoja.save(os.path.join(os.path.dirname(__file__), "vista_rango_ajustado.png"))
    if "--aplicar" in sys.argv:
        os.makedirs(RESPALDO, exist_ok=True)
        for n, im in zip(NOMBRES, res):
            ruta = os.path.join(ITEMS, f"molde_rango_{n}.png")
            dst = os.path.join(RESPALDO, os.path.basename(ruta))
            if os.path.exists(ruta) and not os.path.exists(dst):
                shutil.copy(ruta, dst)
            im.convert("RGBA").save(ruta)


if __name__ == "__main__":
    main()
