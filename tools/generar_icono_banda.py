"""Íconos de la banda (2026-10-05, "correas y cintos"): formas simples, dibujadas por código. Escribe
`textures/item/banda.png` (64x64, tinta marrón del "taller de sastrería": un cinto con hebilla) y los íconos grises de
los slots `textures/gui/slot/cinto.png` y `choker.png` (16x16, como los demás slots de Trinkets).

Uso: python tools/generar_icono_banda.py
"""
from pathlib import Path

from PIL import Image, ImageDraw

RAIZ = Path(__file__).resolve().parent.parent / "src/main/resources/assets/femclothes/textures"
TINTA = (59, 36, 16, 255)
CUERO = (110, 72, 44, 255)
CUERO_LUZ = (146, 100, 64, 255)
ORO = (214, 170, 72, 255)
ORO_LUZ = (244, 214, 130, 255)
SS = 8


def item():
    k = SS
    im = Image.new("RGBA", (64 * k, 64 * k), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    g = 2 * k
    # cinto en diagonal suave: dos tiras unidas por la hebilla
    banda = [(4 * k, 26 * k), (60 * k, 26 * k), (60 * k, 40 * k), (4 * k, 40 * k)]
    d.polygon([(banda[0][0] - g, banda[0][1] - g), (banda[1][0] + g, banda[1][1] - g),
               (banda[2][0] + g, banda[2][1] + g), (banda[3][0] - g, banda[3][1] + g)], fill=TINTA)
    d.polygon(banda, fill=CUERO)
    d.rectangle((4 * k, 26 * k, 60 * k, 29 * k), fill=CUERO_LUZ)
    for x in range(40, 58, 5):                      # agujeritos
        d.ellipse((x * k, 31.5 * k, (x + 2) * k, 33.5 * k), fill=TINTA)
    # hebilla: aro dorado
    d.rectangle((16 * k - g, 21 * k - g, 34 * k + g, 45 * k + g), fill=TINTA)
    d.rectangle((16 * k, 21 * k, 34 * k, 45 * k), fill=ORO)
    d.rectangle((20 * k, 25 * k, 30 * k, 41 * k), fill=CUERO)
    d.rectangle((16 * k, 21 * k, 34 * k, 23 * k), fill=ORO_LUZ)
    d.rectangle((24 * k, 31 * k, 40 * k, 34 * k), fill=ORO)       # la púa
    return im.resize((64, 64), Image.LANCZOS)


def _slot(dibujo):
    k = 16
    im = Image.new("L", (16 * k, 16 * k), 0)
    dibujo(ImageDraw.Draw(im), k)
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


def cinto(d, k):
    d.rectangle((1 * k, 6 * k, 15 * k, 10 * k), fill=255)
    d.rectangle((6 * k, 4.5 * k, 10 * k, 11.5 * k), fill=255)
    d.rectangle((7.2 * k, 6 * k, 8.8 * k, 10 * k), fill=0)


def choker(d, k):
    d.ellipse((2.5 * k, 5 * k, 13.5 * k, 12 * k), fill=255)
    d.ellipse((4.5 * k, 3.5 * k, 11.5 * k, 9.6 * k), fill=0)
    d.rectangle((7 * k, 10.5 * k, 9 * k, 13.5 * k), fill=255)


if __name__ == "__main__":
    item().save(RAIZ / "item/banda.png")
    _slot(cinto).save(RAIZ / "gui/slot/cinto.png")
    _slot(choker).save(RAIZ / "gui/slot/choker.png")
