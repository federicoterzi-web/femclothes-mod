"""Esquema de Tintes para la remera / Top (2026-10-08, "fijate porque remera en la estacion de tintes tenia un monton de
slots no usables"): Tintes compartía `esquema_remera.png` con la Modeladora, así que mostraba marcos de pines que no
pintan nada (calce, ruedo, puños, frente, capucha...). Parte del esquema ORIGINAL de la remera (8 marcos, guardado en
assets_viejos/esquemas_modeladora_2026-10-07/), tapa el marco de calce (con un parche espejado del otro lado de la
prenda: el dibujo es casi simétrico) y pega en su lugar el marco de las SOLAPAS con su línea y su punto sobre la solapa
izquierda. Escribe `esquema_tintes_remera.png` e imprime las posiciones para `TinturasScreenHandler.POS_REMERA`
(origen del ítem = centro/4 - 8; Y absoluta: +36). También copia los esquemas de pantalón, medias y calientabrazos
como `esquema_tintes_<prenda>.png`, para que Tintes sea independiente de la Modeladora.

Uso: python tools/generar_esquema_tintes_remera.py
"""
import os
import shutil

from PIL import Image, ImageDraw, ImageFilter

RAIZ = os.path.join(os.path.dirname(__file__), "..")
GUI = os.path.join(RAIZ, "src", "main", "resources", "assets", "modamod", "textures", "gui", "container")
BASE = os.path.join(RAIZ, "assets_viejos", "esquemas_modeladora_2026-10-07", "esquema_remera.png")
ESQUEMA_Y = 36
SS = 4

MARCO = (162, 187, 236, 261)      # marco de la manga izquierda de la remera
PUNTO = (298, 220)                # su punto fucsia sobre la prenda
CALCE = (223, 359, 297, 433)      # marco de calce (a tapar)
LINEA_CALCE = (296, 345, 390, 410)  # la línea y el punto del calce (a tapar)
SOLAPAS = ((120, 330), (432, 188))   # centro del marco nuevo, punto sobre la solapa izquierda


def espejar(im, caja):
    """Pega en `caja` (x0, y0, x1, y1) el parche espejado del otro lado de la prenda."""
    x0, y0, x1, y1 = caja
    ancho = im.width
    parche = im.crop((ancho - x1, y0, ancho - x0, y1)).transpose(Image.FLIP_LEFT_RIGHT)
    im.paste(parche, (x0, y0))


def linea(base, desde, hasta, color):
    capa = Image.new("RGBA", (base.width * SS, base.height * SS), (0, 0, 0, 0))
    d = ImageDraw.Draw(capa)
    d.line([(desde[0] * SS, desde[1] * SS), (hasta[0] * SS, hasta[1] * SS)], fill=color + (255,), width=3 * SS)
    capa = capa.resize(base.size, Image.LANCZOS)
    base.alpha_composite(capa)


def main():
    im = Image.open(BASE).convert("RGBA")
    marco = im.crop(MARCO)
    px0, py0 = PUNTO
    punto = im.crop((px0 - 12, py0 - 12, px0 + 12, py0 + 12))
    color = im.getpixel(PUNTO)[:3]
    mascara = Image.new("L", punto.size, 0)
    pp, mp = punto.load(), mascara.load()
    for yy in range(punto.height):
        for xx in range(punto.width):
            r, g, b, _ = pp[xx, yy]
            if (r - g > 55) or (r > 215 and g > 200 and b > 190 and (xx - 12) ** 2 + (yy - 12) ** 2 < 49):
                mp[xx, yy] = 255
    mascara = mascara.filter(ImageFilter.MaxFilter(3))

    # 1) tapar el calce: el marco y su línea con el punto.
    espejar(im, (CALCE[0] - 4, CALCE[1] - 4, CALCE[2] + 4, CALCE[3] + 4))
    espejar(im, LINEA_CALCE)
    # 2) el marco de las solapas, con su línea y su punto.
    (cx, cy), objetivo = SOLAPAS
    x0, y0 = cx - marco.width // 2, cy - marco.height // 2
    im.paste(marco, (x0, y0))
    linea(im, (x0 + marco.width, cy), objetivo, color)
    im.paste(punto, (objetivo[0] - 12, objetivo[1] - 12), mascara)

    destino = os.path.join(GUI, "esquema_tintes_remera.png")
    im.convert("RGB").quantize(colors=256, method=Image.MEDIANCUT).save(destino, optimize=True)
    x, y = round(cx / 4 - 8), round(cy / 4 - 8) + ESQUEMA_Y
    print("esquema_tintes_remera.png  SOLAPAS PIN_POS {%d, %d}  PIN_BTN {%d, %d}" % (x, y, x + 17, y - 1))

    for prenda in ("pantalon", "medias", "calientabrazos"):
        shutil.copyfile(os.path.join(GUI, f"esquema_{prenda}.png"), os.path.join(GUI, f"esquema_tintes_{prenda}.png"))
        print("copiado esquema_tintes_%s.png" % prenda)


if __name__ == "__main__":
    main()
