"""Esquema de la Modeladora del Top (remera + chaqueta fusionadas; 2026-10-07, "un solo top" + "dejemos la posibilidad de
construir asimetricamente"): parte del esquema ORIGINAL de la remera (los 8 marcos de siempre: cuello, 3 materiales,
mangas, calce y torso, guardado en assets_viejos/esquemas_modeladora_2026-10-07/) y le suma 6 marcos con su línea y su
punto fucsia: Ruedo del torso, Puño de cada manga, Frente, Capucha y Solapa.
Escribe `esquema_remera.png` e imprime los PIN_POS/PIN_BTN de los marcos nuevos para
`ModeladoScreenHandler` (origen del ítem = centro/4 - 8; la Y de PIN_POS es absoluta: +36).

Uso: python tools/generar_esquemas_top.py
"""
import os

from PIL import Image, ImageDraw, ImageFilter

RAIZ = os.path.join(os.path.dirname(__file__), "..")
GUI = os.path.join(RAIZ, "src", "main", "resources", "assets", "modamod", "textures", "gui", "container")
ESQUEMA_Y = 36
SS = 4

MARCO = (162, 187, 236, 261)   # marco de la manga izquierda de la remera
PUNTO = (298, 220)             # su punto fucsia sobre la prenda
BASE = os.path.join(RAIZ, "assets_viejos", "esquemas_modeladora_2026-10-07", "esquema_remera.png")
# (rol, lado del marco que toca la línea, centro del marco, punto sobre la prenda)
RUEDOS = [
    ("RUEDO_TORSO", "der", (700, 470), (586, 402)),
    ("PUNO_IZQ", "izq", (150, 330), (297, 258)),
    ("PUNO_DER", "der", (790, 345), (663, 258)),
]
EXTRA_CHAQUETA = [
    ("FRENTE", "der", (860, 130), (540, 210)),
    ("CAPUCHA", "izq", (100, 130), (440, 135)),
    # Solapa (2026-10-07, "traje separado... varios tipos de solapa"): abajo a la izquierda, apunta al pecho.
    ("SOLAPA", "izq", (110, 445), (420, 212)),
]


def linea(base, desde, hasta, color):
    capa = Image.new("RGBA", (base.width * SS, base.height * SS), (0, 0, 0, 0))
    d = ImageDraw.Draw(capa)
    d.line([(desde[0] * SS, desde[1] * SS), (hasta[0] * SS, hasta[1] * SS)], fill=color + (255,), width=3 * SS)
    capa = capa.resize(base.size, Image.LANCZOS)
    base.alpha_composite(capa)


def agregar(im, marco, punto, mascara, color, nuevos):
    salida = []
    for rol, lado, centro, objetivo in nuevos:
        x0, y0 = centro[0] - marco.width // 2, centro[1] - marco.height // 2
        im.paste(marco, (x0, y0))
        borde = (x0 + marco.width, centro[1]) if lado == "izq" else (x0, centro[1])
        linea(im, borde, objetivo, color)
        im.paste(punto, (objetivo[0] - 12, objetivo[1] - 12), mascara)
        salida.append((rol, round(centro[0] / 4 - 8), round(centro[1] / 4 - 8) + ESQUEMA_Y))
    return salida


def guardar(im, nombre, salida):
    destino = os.path.join(GUI, nombre)
    im.convert("RGB").quantize(colors=256, method=Image.MEDIANCUT).save(destino, optimize=True)
    print(nombre)
    print("  PIN_POS ", ", ".join(f"{{{x}, {y}}}" for _, x, y in salida), "# " + " ".join(r for r, _, _ in salida))
    print("  PIN_BTN ", ", ".join(f"{{{x + 17}, {y - 1}}}" for _, x, y in salida))


def main():
    original = Image.open(BASE).convert("RGBA")
    marco = original.crop(MARCO)
    px0, py0 = PUNTO
    punto = original.crop((px0 - 12, py0 - 12, px0 + 12, py0 + 12))
    color = original.getpixel(PUNTO)[:3]
    mascara = Image.new("L", punto.size, 0)
    pp, mp = punto.load(), mascara.load()
    for yy in range(punto.height):
        for xx in range(punto.width):
            r, g, b, _ = pp[xx, yy]
            if (r - g > 55) or (r > 215 and g > 200 and b > 190 and (xx - 12) ** 2 + (yy - 12) ** 2 < 49):
                mp[xx, yy] = 255
    mascara = mascara.filter(ImageFilter.MaxFilter(3))

    remera = original.copy()
    salida = agregar(remera, marco, punto, mascara, color, RUEDOS + EXTRA_CHAQUETA)
    guardar(remera, "esquema_remera.png", salida)


if __name__ == "__main__":
    main()
