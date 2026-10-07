"""Esquema de la Modeladora para la chaqueta (2026-10-07, "como se te ocurre que mejor hacemos las chaquetas"): parte
del esquema de la remera (los 8 marcos de siempre: cuello, 3 materiales, mangas, calce y torso) y le suma tres marcos
con su línea y su punto fucsia: Frente, Capucha y Remate. Escribe `esquema_chaqueta.png` e imprime los PIN_POS/PIN_BTN
de esos tres para `ModeladoScreenHandler` (origen del ítem = centro/4 - 8; la Y de PIN_POS es absoluta: +36).

Uso: python tools/generar_esquema_chaqueta.py
"""
import os

from PIL import Image, ImageDraw, ImageFilter

RAIZ = os.path.join(os.path.dirname(__file__), "..")
GUI = os.path.join(RAIZ, "src", "main", "resources", "assets", "femclothes", "textures", "gui", "container")
ESQUEMA_Y = 36
SS = 4

MARCO = (162, 187, 236, 261)   # marco de la manga izquierda de la remera
PUNTO = (298, 220)             # su punto fucsia sobre la prenda
# (rol, lado del marco que toca la línea, centro del marco, punto sobre la prenda)
NUEVOS = [
    ("FRENTE", "der", (790, 345), (592, 322)),
    ("CAPUCHA", "izq", (150, 330), (392, 178)),
    ("REMATE", "der", (700, 470), (586, 402)),
]


def linea(base, desde, hasta, color):
    capa = Image.new("RGBA", (base.width * SS, base.height * SS), (0, 0, 0, 0))
    d = ImageDraw.Draw(capa)
    d.line([(desde[0] * SS, desde[1] * SS), (hasta[0] * SS, hasta[1] * SS)], fill=color + (255,), width=3 * SS)
    capa = capa.resize(base.size, Image.LANCZOS)
    base.alpha_composite(capa)


def main():
    im = Image.open(os.path.join(GUI, "esquema_remera.png")).convert("RGBA")
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
    salida = []
    for rol, lado, centro, objetivo in NUEVOS:
        x0, y0 = centro[0] - marco.width // 2, centro[1] - marco.height // 2
        im.paste(marco, (x0, y0))
        borde = (x0 + marco.width, centro[1]) if lado == "izq" else (x0, centro[1])
        linea(im, borde, objetivo, color)
        im.paste(punto, (objetivo[0] - 12, objetivo[1] - 12), mascara)
        salida.append((rol, round(centro[0] / 4 - 8), round(centro[1] / 4 - 8) + ESQUEMA_Y))
    destino = os.path.join(GUI, "esquema_chaqueta.png")
    im.convert("RGB").quantize(colors=256, method=Image.MEDIANCUT).save(destino, optimize=True)
    print("PIN_POS ", ", ".join(f"{{{x}, {y}}}" for _, x, y in salida), "# " + " ".join(r for r, _, _ in salida))
    print("PIN_BTN ", ", ".join(f"{{{x + 17}, {y - 1}}}" for _, x, y in salida))


if __name__ == "__main__":
    main()
