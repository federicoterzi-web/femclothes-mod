"""Íconos 64x64 de los 5 moldes nuevos de la pollera (2026-10-05, "tres polleras más y dos moldes de volado"):
papel kraft (la silueta del papel del molde de capa) con la silueta de la pollera dibujada encima — tubo, globo,
circular — y los dos volados (recto: tira con ondas chicas; circular: ruedo abierto con ondas grandes).
Geometría simple dibujada por código."""
import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

RAIZ = Path(__file__).resolve().parent.parent
TEX = RAIZ / "src/main/resources/assets/modamod/textures/item"
papel_src = Image.open(TEX / "molde_capa_ruedo_recto.png").convert("RGBA")
S = 8
TINTA = (59, 36, 16, 255)
TELA = (236, 218, 184, 255)
TELA_S = (206, 184, 146, 255)
LINEA = (190, 34, 98, 255)


def papel():
    a = papel_src.split()[3]
    base = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    base.paste(Image.new("RGBA", (64, 64), (196, 150, 98, 255)), mask=a)
    borde = a.filter(ImageFilter.MaxFilter(3))
    out = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    out.paste(Image.new("RGBA", (64, 64), (96, 62, 32, 255)), mask=borde)
    out.paste(base, mask=a)
    return out


def pol(d, k, pts, relleno=TELA):
    d.polygon([(x * k, y * k) for x, y in pts], fill=relleno, outline=TINTA)
    d.line([(x * k, y * k) for x, y in pts + [pts[0]]], fill=TINTA, width=k, joint="curve")


def cintura(d, k, x0, x1, y):
    d.rectangle((x0 * k, (y - 2) * k, x1 * k, (y + 1) * k), fill=TELA_S, outline=TINTA, width=k)


def borde_ondulado(x0, x1, y, n, amp, fase=0.0):
    pts = []
    for i in range(n * 8 + 1):
        t = i / (n * 8)
        pts.append((x0 + (x1 - x0) * t, y + amp * math.sin(2 * math.pi * n * t + fase)))
    return pts


def dibujo(tipo):
    im = Image.new("RGBA", (64 * S, 64 * S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    k = S
    if tipo == "tubo":
        pol(d, k, [(24, 18), (40, 18), (41, 50), (23, 50)])
        cintura(d, k, 24, 40, 18)
        d.line([(32 * k, 22 * k), (32 * k, 48 * k)], fill=TELA_S, width=k)
    elif tipo == "globo":
        pts = [(24, 18), (40, 18)]
        for i in range(1, 12):
            t = i / 11
            ancho = 8 + 8 * math.sin(math.pi * min(1, t * 1.05) ** 0.8) + 2 * t
            pts.append((32 + ancho, 18 + 32 * t))
        for i in range(11, 0, -1):
            t = i / 11
            ancho = 8 + 8 * math.sin(math.pi * min(1, t * 1.05) ** 0.8) + 2 * t
            pts.append((32 - ancho, 18 + 32 * t))
        pol(d, k, pts)
        cintura(d, k, 24, 40, 18)
    elif tipo == "circular":
        pts = [(24, 18), (40, 18), (53, 48)] + [(32 + 21 * math.cos(math.radians(a)), 46 + 6 * math.sin(math.radians(a)))
                                                for a in range(20, 161, 10)] + [(11, 48)]
        pol(d, k, pts)
        cintura(d, k, 24, 40, 18)
        for x in (22, 32, 42):
            d.line([(32 * k, 22 * k), (x * k + (x - 32) * 0.4 * k, 48 * k)], fill=TELA_S, width=k)
    elif tipo == "volado_recto":
        pol(d, k, [(23, 20), (41, 20), (43, 40), (21, 40)])
        cintura(d, k, 23, 41, 20)
        pts = [(21, 36)] + borde_ondulado(21, 43, 51, 8, 1.4) + [(43, 36)]
        pol(d, k, [(21, 38), (43, 38)] + borde_ondulado(43, 21, 51, 8, 1.4, math.pi * 0), TELA)
        d.line([(21 * k, 38 * k), (43 * k, 38 * k)], fill=LINEA, width=k)
    elif tipo.startswith("borde_"):
        pol(d, k, [(24, 18), (40, 18), (42, 44), (22, 44)])
        cintura(d, k, 24, 40, 18)
        n = 4
        if tipo == "borde_ondulado":
            hem = [(20 + 24 * t, 48 + 2 * math.sin(2 * math.pi * n * t)) for t in [i / 32 for i in range(33)]]
        elif tipo == "borde_festoneado":
            hem = []
            for i in range(n):
                for j in range(9):
                    f = j / 8
                    hem.append((20 + 24 * (i + f) / n, 46 + 4 * math.sqrt(max(0, 1 - (2 * f - 1) ** 2))))
        else:
            hem = []
            for i in range(n + 1):
                hem.append((20 + 24 * i / n, 45))
                if i < n:
                    hem.append((20 + 24 * (i + 0.5) / n, 52))
        pol(d, k, [(22, 44), (42, 44)] + hem[::-1] if tipo != "borde_pico" else [(22, 44), (42, 44)] + hem[::-1], TELA)
        d.line([(22 * k, 44 * k), (42 * k, 44 * k)], fill=LINEA, width=k)
    else:  # volado circular
        pol(d, k, [(24, 20), (40, 20), (42, 38), (22, 38)])
        cintura(d, k, 24, 40, 20)
        arriba = [(22, 38), (42, 38)]
        ondas = [(32 + (11 + 11 * t) * (1 if False else 1) * 0, 0) for t in (0,)]
        hem = [(10 + 44 * t, 52 + 3 * math.sin(2 * math.pi * 3 * t)) for t in [i / 24 for i in range(25)]]
        pol(d, k, arriba + hem[::-1], TELA)
        d.line([(22 * k, 38 * k), (42 * k, 38 * k)], fill=LINEA, width=k)
    return im.resize((64, 64), Image.LANCZOS)


TIPOS = {"molde_pollera_tubo": "tubo", "molde_pollera_globo": "globo", "molde_pollera_circular": "circular",
         "molde_volado_recto": "volado_recto", "molde_volado_circular": "volado_circular",
         "molde_borde_ondulado": "borde_ondulado", "molde_borde_festoneado": "borde_festoneado", "molde_borde_pico": "borde_pico"}

if __name__ == "__main__":
    for nombre, t in TIPOS.items():
        ic = papel()
        ic.alpha_composite(dibujo(t))
        ic.save(TEX / f"{nombre}.png")
        modelo = RAIZ / f"src/main/resources/assets/modamod/models/item/{nombre}.json"
        modelo.write_text('{\n  "parent": "minecraft:item/generated",\n  "textures": {\n    "layer0": "modamod:item/%s"\n  }\n}\n' % nombre)
        print("ok", nombre)
