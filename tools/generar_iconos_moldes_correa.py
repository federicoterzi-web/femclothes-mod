"""Íconos 64x64 de los 5 moldes de correa (2026-10-05, geometría simple dibujada por código): papel kraft (la silueta
del papel del molde de capa) con el estilo de correa dibujado encima en diagonal: lisa, cadena, cadena fina,
ojalillos y cordón."""
import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

RAIZ = Path(__file__).resolve().parent.parent
TEX = RAIZ / "src/main/resources/assets/femclothes/textures/item"
papel_src = Image.open(TEX / "molde_capa_ruedo_recto.png").convert("RGBA")
S = 8
CUERO = (92, 58, 36, 255)
CUERO_C = (138, 98, 64, 255)
ORO = (214, 170, 72, 255)
ORO_L = (244, 214, 130, 255)
TINTA = (59, 36, 16, 255)


def papel():
    a = papel_src.split()[3]
    base = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    base.paste(Image.new("RGBA", (64, 64), (196, 150, 98, 255)), mask=a)
    borde = a.filter(ImageFilter.MaxFilter(3))
    out = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    out.paste(Image.new("RGBA", (64, 64), (96, 62, 32, 255)), mask=borde)
    out.paste(base, mask=a)
    return out


A, B = (14, 50), (50, 16)       # extremos de la diagonal


def punto(t):
    return A[0] + (B[0] - A[0]) * t, A[1] + (B[1] - A[1]) * t


def cinta(d, k, ancho, ojal=False):
    nx, ny = (B[1] - A[1]), -(B[0] - A[0])
    ln = math.hypot(nx, ny)
    nx, ny = nx / ln * ancho / 2, ny / ln * ancho / 2
    pts = [(A[0] + nx, A[1] + ny), (B[0] + nx, B[1] + ny), (B[0] - nx, B[1] - ny), (A[0] - nx, A[1] - ny)]
    d.polygon([(x * k, y * k) for x, y in pts], fill=CUERO, outline=TINTA)
    d.line([(A[0] * k, A[1] * k), (B[0] * k, B[1] * k)], fill=CUERO_C, width=k)
    if ojal:
        for t in (0.3, 0.5, 0.7):
            x, y = punto(t)
            d.ellipse(((x - 1.4) * k, (y - 1.4) * k, (x + 1.4) * k, (y + 1.4) * k), fill=TINTA)


def dibujo(tipo):
    im = Image.new("RGBA", (64 * S, 64 * S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    k = S
    if tipo == "lisa":
        cinta(d, k, 7)
    elif tipo == "ojalillos":
        cinta(d, k, 7, True)
    elif tipo == "cadena":
        for i in range(8):
            x, y = punto(i / 7)
            if i % 2 == 0:
                d.rounded_rectangle(((x - 3.6) * k, (y - 2.2) * k, (x + 3.6) * k, (y + 2.2) * k), radius=2 * k,
                                    outline=ORO, width=int(1.4 * k))
            else:
                d.ellipse(((x - 1.6) * k, (y - 3.2) * k, (x + 1.6) * k, (y + 3.2) * k), fill=ORO, outline=TINTA)
    elif tipo == "cadena_fina":
        for i in range(12):
            x, y = punto(i / 11)
            d.ellipse(((x - 2.2) * k, (y - 2.2) * k, (x + 2.2) * k, (y + 2.2) * k), fill=ORO, outline=TINTA)
            d.ellipse(((x - 1.2) * k, (y - 1.4) * k, (x - 0.2) * k, (y - 0.4) * k), fill=ORO_L)
    else:  # cordón: curva con puntas
        pts = []
        for i in range(21):
            t = i / 20
            x, y = punto(t)
            pts.append(((x + 5 * math.sin(t * math.pi * 2)) * k, (y + 5 * math.sin(t * math.pi * 2)) * k))
        d.line(pts, fill=TINTA, width=int(4.2 * k), joint="curve")
        d.line(pts, fill=CUERO, width=int(2.6 * k), joint="curve")
        for p in (pts[0], pts[-1]):
            d.ellipse((p[0] - 3.2 * k, p[1] - 3.2 * k, p[0] + 3.2 * k, p[1] + 3.2 * k), fill=ORO, outline=TINTA)
    return im.resize((64, 64), Image.LANCZOS)


TIPOS = ["lisa", "cadena", "cadena_fina", "ojalillos", "cordon"]

if __name__ == "__main__":
    for t in TIPOS:
        ic = papel()
        ic.alpha_composite(dibujo(t))
        ic.save(TEX / f"molde_correa_{t}.png")
        modelo = RAIZ / f"src/main/resources/assets/femclothes/models/item/molde_correa_{t}.json"
        modelo.write_text('{\n  "parent": "minecraft:item/generated",\n  "textures": {\n    "layer0": "femclothes:item/molde_correa_%s"\n  }\n}\n' % t)
        print("ok", t)
