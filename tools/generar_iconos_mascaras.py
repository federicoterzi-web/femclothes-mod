"""Íconos provisorios de los moldes de máscara de la Sublimadora (2026-10-02).

Hoja de papel kraft (los colores de los moldes de "Taller de sastrería") con
la forma calada al medio y la chinche de bronce de los otros moldes.
Salida: src/main/resources/assets/femclothes/textures/item/molde_mascara_<forma>.png (64x64).
"""
import math
import os
from PIL import Image

RAIZ = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "femclothes", "textures", "item")
BASE = Image.open(os.path.join(RAIZ, "molde_cuello_v.png")).convert("RGBA")

# Colores tomados del molde de cuello (relleno, borde oscuro, sombra).
def mas_comun(img, filtro):
    cuenta = {}
    for c in img.getdata():
        if c[3] == 255 and filtro(c):
            cuenta[c] = cuenta.get(c, 0) + 1
    return max(cuenta, key=cuenta.get)

KRAFT = mas_comun(BASE, lambda c: c[0] > 150 and c[0] > c[2] + 40)
BORDE = mas_comun(BASE, lambda c: c[0] < 110 and c[0] > c[2])
HUECO = (60, 38, 22, 255)


def dentro(forma, u, v):
    if not (-0.5 <= u <= 0.5 and -0.5 <= v <= 0.5):
        return False
    if forma == "cuadrado":
        return True
    if forma == "franja":
        return abs(v) <= 0.13
    if forma == "circulo":
        return u * u + v * v <= 0.25
    if forma == "triangulo":
        return abs(u) <= (v + 0.5) * 0.5
    if forma == "estrella":
        r = math.hypot(u, v)
        a = math.atan2(u, -v)
        s = math.pi * 2 / 5
        f = abs(((a % s) + s) % s - s / 2) / (s / 2)
        return r <= 0.2 + 0.3 * f
    return False


def icono(forma):
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    x0, y0, x1, y1 = 8, 10, 56, 56
    for y in range(y0, y1):
        for x in range(x0, x1):
            borde = x in (x0, x1 - 1) or y in (y0, y1 - 1)
            img.putpixel((x, y), BORDE if borde else KRAFT)
    # La forma calada, de 30 px, con un borde oscuro.
    cx, cy, lado = 32, 34, 30
    calado = set()
    for y in range(64):
        for x in range(64):
            u, v = (x + 0.5 - cx) / lado, (y + 0.5 - cy) / lado
            if dentro(forma, u, v):
                calado.add((x, y))
    for (x, y) in calado:
        vecinos = [(x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)]
        img.putpixel((x, y), BORDE if any(n not in calado for n in vecinos) else HUECO)
    # Chinche de bronce del molde de cuello (esquina de arriba a la izquierda).
    for y in range(14, 26):
        for x in range(10, 22):
            c = BASE.getpixel((x, y))
            if c[3] == 255 and c[1] > 120 and c[2] < 90 and c[0] > 160:
                img.putpixel((x, y), c)
    return img


for forma in ["cuadrado", "franja", "circulo", "estrella", "triangulo"]:
    icono(forma).save(os.path.join(RAIZ, f"molde_mascara_{forma}.png"))
    print("ok", forma)
