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
# Destaques (2026-10-04, "mas brillante e iconica a la chincheta y a la tijera. Y a la linea de corte"): cobre vivo.
VIVO = np.array([255, 138, 58])
VIVO_LUZ = np.array([255, 214, 160])
VIVO_SOMBRA = np.array([176, 72, 28])
TINTA = np.array([59, 36, 16])
ACERO = np.array([206, 215, 226])
ACERO_LUZ = np.array([248, 250, 253])
ACERO_SOMBRA = np.array([128, 138, 154])
XP, X_PIVOTE, X_PUNTA = 17, 45, 37      # centro de la chincheta, eje y puntas de la tijera


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

    # Piezas viejas = lo naranja/gris dentro de las ventanas de la chincheta y la tijera.
    yc = (y0 + y1) // 2
    ventana = np.zeros_like(naranja)
    ventana[max(0, yc - 10):yc + 11, 10:27] = True
    ventana[max(0, yc - 10):yc + 11, 38:64] = True
    piezas = (naranja | gris) & ventana
    tramo = np.zeros_like(naranja)
    tramo[max(0, y0 - 1):y1 + 2, :] = True
    vieja_linea = naranja & ~ventana & tramo
    barrera = dilatar(piezas, 1)
    dentro = interior(oscuro, barrera | vieja_linea, (32, 40))

    # puntadas de adentro: mediana (5x5, dos pasadas) solo dentro del contorno y lejos de las piezas y la línea
    suave = Image.fromarray(a).filter(ImageFilter.MedianFilter(5)).filter(ImageFilter.MedianFilter(5))
    suave = np.array(suave)
    zona = dentro & ~barrera & ~tramo
    b = a.copy()
    b[zona] = suave[zona]

    # sacar lo viejo (piezas y línea): promedio de los vecinos que no son piezas
    sacar = dilatar(piezas, 1) | vieja_linea   # +1 px: se lleva también el contorno de tinta de lo viejo
    valido = ~sacar
    for _ in range(4):
        nuevo_b = b.copy()
        for y, x in zip(*np.where(sacar)):
            ys, xs = slice(max(0, y - 2), y + 3), slice(max(0, x - 2), x + 3)
            v = valido[ys, xs]
            if v.any():
                nuevo_b[y, x] = b[ys, xs][v].mean(axis=0)
                valido[y, x] = True
        b = nuevo_b
        sacar = sacar & ~valido

    # Los destaques se dibujan en una capa aparte y se les pone contorno de tinta (más icónicos).
    capa = np.zeros((64, 64, 4), dtype=np.uint8)

    def px(x, y, c):
        if 0 <= x < 64 and 0 <= y < 64:
            capa[y, x, :3] = c
            capa[y, x, 3] = 255

    def disco(cx, cy, r, c):
        for y in range(cy - r, cy + r + 1):
            for x in range(cx - r, cx + r + 1):
                if (x - cx) ** 2 + (y - cy) ** 2 <= r * r + r * 0.6:
                    px(x, y, c)

    # línea de corte: 3 px, trazos de 4 con 2 de hueco, entre la chincheta y la tijera
    xa, xb = XP + 5, X_PUNTA - 1
    for x in range(xa, xb + 1):
        if ((x - xa) % 6) < 4:
            px(x, yc - 1, VIVO_LUZ)
            px(x, yc, VIVO)
            px(x, yc + 1, VIVO_SOMBRA)

    # chincheta: cabeza redonda con luz y sombra, y la aguja que asoma abajo
    for k in range(5):
        px(XP - 1 - k // 2 - 1, yc + 4 + k, ACERO if k < 4 else ACERO_SOMBRA)
    disco(XP, yc, 5, VIVO_SOMBRA)
    disco(XP - 1, yc - 1, 4, VIVO)
    disco(XP - 2, yc - 2, 2, VIVO_LUZ)
    px(XP - 3, yc - 3, ACERO_LUZ)
    px(XP - 2, yc - 3, ACERO_LUZ)

    # tijera: dos hojas gruesas cruzadas en el eje, entreabiertas hacia la línea, y dos anillos con agujero
    def hoja(x0, y0_, x1, y1_, claro, medio, sombra):
        n = max(abs(x1 - x0), abs(y1 - y0_))
        for k in range(n + 1):
            t = k / n
            x = round(x0 + (x1 - x0) * t)
            y = round(y0_ + (y1 - y0_) * t)
            alto = 3 if t < 0.55 else 2        # se afina hacia la punta
            px(x, y - 1, claro)
            px(x, y, medio)
            if alto == 3:
                px(x, y + 1, sombra)
    # la hoja que sale del anillo de arriba termina abajo, y al revés (así se cruzan en el eje)
    hoja(X_PIVOTE + 7, yc - 4, X_PUNTA, yc + 3, ACERO_LUZ, ACERO, ACERO_SOMBRA)
    hoja(X_PIVOTE + 7, yc + 4, X_PUNTA, yc - 3, ACERO_LUZ, ACERO, ACERO_SOMBRA)
    for sg in (-1, 1):
        cy = yc + sg * 5
        disco(X_PIVOTE + 10, cy, 3, VIVO_SOMBRA)
        disco(X_PIVOTE + 10, cy, 2, VIVO)
        px(X_PIVOTE + 9, cy - 2, VIVO_LUZ)
        px(X_PIVOTE + 10, cy - 2, VIVO_LUZ)
        for ddx in (-1, 0, 1):               # el agujero del anillo, del color del papel
            px(X_PIVOTE + 10 + ddx, cy, np.array([232, 202, 160]))
        px(X_PIVOTE + 10, cy - 1, np.array([232, 202, 160]))
        px(X_PIVOTE + 10, cy + 1, np.array([232, 202, 160]))
    disco(X_PIVOTE + 3, yc, 1, VIVO_SOMBRA)        # el tornillo del eje
    px(X_PIVOTE + 3, yc, VIVO_LUZ)

    # contorno de tinta de 1 px alrededor de todo lo dibujado
    mascara = capa[..., 3] > 0
    borde = dilatar(mascara, 1) & ~mascara
    for y, x in zip(*np.where(borde)):
        b[y, x] = TINTA
    for y, x in zip(*np.where(mascara)):
        b[y, x] = capa[y, x, :3]
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
