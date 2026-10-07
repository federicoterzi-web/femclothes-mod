"""Íconos 64x64 de los 8 moldes de la banda (2026-10-05, geometría simple dibujada por código): papel kraft (la
silueta del papel del molde de capa) con la zona, el ancho o el herraje dibujados encima, como los del sombrero."""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

RAIZ = Path(__file__).resolve().parent.parent
TEX = RAIZ / "src/main/resources/assets/modamod/textures/item"
papel_src = Image.open(TEX / "molde_capa_ruedo_recto.png").convert("RGBA")
S = 8
LINEA = (190, 34, 98, 255)
CUERO = (92, 58, 36, 255)
CUERO_C = (138, 98, 64, 255)
ORO = (214, 170, 72, 255)
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


def banda(d, k, alto, herraje="placa"):
    y0 = 32 - alto / 2
    d.rectangle((9 * k, y0 * k, 55 * k, (y0 + alto) * k), fill=CUERO, outline=TINTA, width=k)
    d.rectangle((9 * k, y0 * k, 55 * k, (y0 + 1.2) * k), fill=CUERO_C)
    if herraje == "placa":
        d.rectangle((26 * k, (y0 - 2) * k, 38 * k, (y0 + alto + 2) * k), fill=ORO, outline=TINTA, width=k)
    elif herraje == "aro":
        d.rectangle((25 * k, (y0 - 2) * k, 39 * k, (y0 + alto + 2) * k), fill=ORO, outline=TINTA, width=k)
        d.rectangle((29 * k, (y0 + 1) * k, 35 * k, (y0 + alto - 1) * k), fill=CUERO)


def dibujo(tipo):
    im = Image.new("RGBA", (64 * S, 64 * S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    k = S
    if tipo == "zona_cintura":
        banda(d, k, 8)
        d.line([(14 * k, 20 * k), (50 * k, 20 * k)], fill=LINEA, width=k)
        d.line([(14 * k, 44 * k), (50 * k, 44 * k)], fill=LINEA, width=k)
    elif tipo == "zona_cuello":
        d.ellipse((16 * k, 18 * k, 48 * k, 46 * k), outline=CUERO, width=5 * k)
        d.ellipse((28 * k, 40 * k, 36 * k, 50 * k), fill=ORO, outline=TINTA, width=k)
    elif tipo == "ancho_fino":
        banda(d, k, 5, "ninguno")
    elif tipo == "ancho_medio":
        banda(d, k, 10, "ninguno")
    elif tipo == "ancho_ancho":
        banda(d, k, 17, "ninguno")
    elif tipo == "herraje_ninguno":
        banda(d, k, 10, "ninguno")
        d.line([(10 * k, 12 * k), (54 * k, 52 * k)], fill=LINEA, width=2 * k)
    elif tipo == "herraje_placa":
        banda(d, k, 10, "placa")
    else:
        banda(d, k, 10, "aro")
    return im.resize((64, 64), Image.LANCZOS)


TIPOS = ["zona_cintura", "zona_cuello", "ancho_fino", "ancho_medio", "ancho_ancho",
         "herraje_ninguno", "herraje_placa", "herraje_aro"]

if __name__ == "__main__":
    for t in TIPOS:
        ic = papel()
        ic.alpha_composite(dibujo(t))
        ic.save(TEX / f"molde_banda_{t}.png")
        modelo = RAIZ / f"src/main/resources/assets/modamod/models/item/molde_banda_{t}.json"
        modelo.write_text('{\n  "parent": "minecraft:item/generated",\n  "textures": {\n    "layer0": "modamod:item/molde_banda_%s"\n  }\n}\n' % t)
        print("ok", t)
