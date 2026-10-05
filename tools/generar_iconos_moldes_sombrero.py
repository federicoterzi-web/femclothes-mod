"""Íconos 64x64 de los 4 moldes de sombrero (2026-10-05, geometría simple dibujada por código):
papel kraft (la silueta del papel del molde de capa) con la forma del ala o de la punta dibujada encima."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter

RAIZ = Path(__file__).resolve().parent.parent
TEX = RAIZ / "src/main/resources/assets/femclothes/textures/item"
papel_src = Image.open(TEX / "molde_capa_ruedo_recto.png").convert("RGBA")
S = 8  # supersampling
LINEA = (190, 34, 98, 255)
MORADO = (92, 52, 140, 255)
MORADO_C = (132, 90, 180, 255)


def papel():
    a = papel_src.split()[3]
    base = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    kraft = Image.new("RGBA", (64, 64), (196, 150, 98, 255))
    base.paste(kraft, mask=a)
    borde = a.filter(ImageFilter.MaxFilter(3))
    contorno = Image.new("RGBA", (64, 64), (96, 62, 32, 255))
    out = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    out.paste(contorno, mask=borde)
    out.paste(base, mask=a)
    return out


def dibujo(tipo):
    im = Image.new("RGBA", (64 * S, 64 * S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    k = S
    if tipo == "ala_ancha":
        d.ellipse((6 * k, 30 * k, 58 * k, 50 * k), fill=MORADO, outline=LINEA, width=k)
        d.ellipse((22 * k, 34 * k, 42 * k, 44 * k), fill=MORADO_C)
    elif tipo == "ala_corta":
        d.ellipse((16 * k, 32 * k, 48 * k, 48 * k), fill=MORADO, outline=LINEA, width=k)
        d.ellipse((24 * k, 36 * k, 40 * k, 44 * k), fill=MORADO_C)
    elif tipo == "punta_recta":
        d.polygon([(32 * k, 12 * k), (18 * k, 50 * k), (46 * k, 50 * k)], fill=MORADO, outline=LINEA)
        d.line([(32 * k, 12 * k), (18 * k, 50 * k), (46 * k, 50 * k), (32 * k, 12 * k)], fill=LINEA, width=k)
        d.rectangle((20 * k, 42 * k, 44 * k, 47 * k), fill=MORADO_C)
    else:  # punta_doblada
        d.polygon([(30 * k, 50 * k), (18 * k, 50 * k), (26 * k, 28 * k), (36 * k, 16 * k), (48 * k, 20 * k),
                   (40 * k, 22 * k), (36 * k, 30 * k), (46 * k, 50 * k)], fill=MORADO)
        d.line([(18 * k, 50 * k), (26 * k, 28 * k), (36 * k, 16 * k), (48 * k, 20 * k), (40 * k, 22 * k),
                (36 * k, 30 * k), (46 * k, 50 * k), (18 * k, 50 * k)], fill=LINEA, width=k)
        d.rectangle((20 * k, 43 * k, 44 * k, 48 * k), fill=MORADO_C)
    return im.resize((64, 64), Image.LANCZOS)


for t in ("ala_ancha", "ala_corta", "punta_recta", "punta_doblada"):
    ic = papel()
    ic.alpha_composite(dibujo(t))
    ic.save(TEX / f"molde_sombrero_{t}.png")
    print("ok", t)
