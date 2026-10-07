"""Ícono del sombrero de bruja (2026-10-04, "modelemos y agreguemos un sombrero de bruja"): formas simples, así que
se dibuja por código. Escribe `textures/item/sombrero_bruja.png` (64x64, tinta marrón del "taller de sastrería") y el
ícono gris del slot `textures/gui/slot/sombrero.png` (16x16, como los demás slots de Trinkets).

Uso: python tools/generar_icono_sombrero.py
"""
from pathlib import Path

from PIL import Image, ImageDraw

RAIZ = Path(__file__).resolve().parent.parent / "src/main/resources/assets/modamod/textures"
TINTA = (59, 36, 16, 255)
CONO = (84, 58, 112, 255)
CONO_LUZ = (112, 82, 146, 255)
ALA = (66, 46, 90, 255)
CINTA = (170, 96, 196, 255)
SS = 8  # supermuestreo


def silueta(d, escala, relleno, borde=None, grosor=0):
    """Dibuja ala, cono y punta doblada en un lienzo con la escala dada (coordenadas pensadas para 64)."""
    k = escala
    ala = [(4 * k, 46 * k, 60 * k, 56 * k)]
    cono = [(18 * k, 46 * k), (46 * k, 46 * k), (40 * k, 30 * k), (35 * k, 18 * k), (24 * k, 10 * k),
            (16 * k, 8 * k), (21 * k, 16 * k), (26 * k, 28 * k)]
    return ala, cono


def item():
    k = SS
    im = Image.new("RGBA", (64 * k, 64 * k), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    ala, cono = silueta(d, k, None)
    g = 2 * k
    # contorno: las mismas formas engordadas
    d.ellipse((ala[0][0] - g, ala[0][1] - g, ala[0][2] + g, ala[0][3] + g), fill=TINTA)
    d.polygon(cono, fill=TINTA, outline=TINTA, width=g)
    d.ellipse(ala[0], fill=ALA)
    d.polygon(cono, fill=CONO)
    # luz del lado izquierdo del cono
    d.polygon([(20 * k, 45 * k), (27 * k, 45 * k), (27 * k, 28 * k), (23 * k, 16 * k), (19 * k, 12 * k), (22 * k, 18 * k)],
              fill=CONO_LUZ)
    # cinta
    d.polygon([(17 * k, 40 * k), (47 * k, 40 * k), (46 * k, 46 * k), (18 * k, 46 * k)], fill=CINTA)
    d.line([(17 * k, 40 * k), (47 * k, 40 * k)], fill=TINTA, width=k)
    return im.resize((64, 64), Image.LANCZOS)


def slot():
    k = 16
    im = Image.new("L", (16 * k, 16 * k), 0)
    d = ImageDraw.Draw(im)
    d.ellipse((1 * k, 11 * k, 15 * k, 14.6 * k), fill=255)
    d.polygon([(4.4 * k, 12 * k), (11.6 * k, 12 * k), (10 * k, 7.5 * k), (8.6 * k, 4.6 * k), (6 * k, 2.4 * k),
               (4.2 * k, 2 * k), (5.4 * k, 4.2 * k), (6.6 * k, 7.5 * k)], fill=255)
    m = im.resize((16, 16), Image.LANCZOS).point(lambda v: 255 if v > 110 else 0)
    out = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px, mp = out.load(), m.load()
    for y in range(16):
        for x in range(16):
            if not mp[x, y]:
                continue
            borde = any(not (0 <= x + dx < 16 and 0 <= y + dy < 16 and mp[x + dx, y + dy])
                        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            px[x, y] = (40, 40, 44, 190) if borde else (58, 58, 62, 120)
    return out


if __name__ == "__main__":
    item().save(RAIZ / "item/sombrero_bruja.png")
    slot().save(RAIZ / "gui/slot/sombrero.png")
