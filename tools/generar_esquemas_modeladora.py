"""Esquemas de la Modeladora para Pollera y Capa (2026-10-04, "es la ilustracion viejaaaaa" + "dibujalo al asset si es
geometrico simple"): parte de los esquemas de Tintes (`esquema_tintes_*.png`, arte del usuario) y les suma los marcos
que faltan copiando los que ya tienen, con su línea y su punto fucsia hacia la prenda. Escribe
`esquema_pollera.png` / `esquema_capa.png` (960x544, paleta de 256) e imprime los PIN_POS para pegar en
`ModeladoScreenHandler` (origen del ítem de 16x16 = centro/4 - 8; la Y de PIN_POS es absoluta: +36).

Uso: python tools/generar_esquemas_modeladora.py
"""
import os
import shutil

from PIL import Image, ImageDraw, ImageFilter

RAIZ = os.path.join(os.path.dirname(__file__), "..")
GUI = os.path.join(RAIZ, "src", "main", "resources", "assets", "modamod", "textures", "gui", "container")
RESPALDO = os.path.join(RAIZ, "assets_viejos", "esquemas_modeladora_2026-10-04")
ESQUEMA_Y = 36
SS = 4  # supermuestreo de las líneas

# bbox de los marcos existentes y de sus puntos (medidos sobre los PNG de Tintes)
POLLERA = {
    "izq": (146, 384, 230, 466), "der": (714, 212, 798, 295),
    "puntos": [(433, 128), (507, 263), (323, 417)],
    # (rol, lado, centro del marco, punto en la prenda); None = marco que ya existe
    "pines": [
        ("FORMA", None, (244, 108), None),
        ("LARGO", None, (188, 425), None),
        ("CALCE", "izq", (188, 268), (368, 268)),
        ("MAT1", "der", (756, 150), (542, 186)),
        ("MAT2", None, (756, 253), None),
        ("MAT3", "der", (756, 358), (612, 350)),
        # Volados (2026-10-05): el del borde de abajo y el de toda la pollera.
        ("VOLADO_INF", "der", (770, 462), (615, 440)),
        ("VOLADO_TOT", "izq", (100, 165), (375, 205)),
        # Custom (2026-10-05): el borde decorativo del ruedo, abajo a la izquierda del centro.
        ("BORDE", "izq", (330, 466), (415, 452)),
    ],
}
CAPA = {
    "izq": (184, 207, 261, 282), "der": (693, 65, 770, 141),
    "puntos": [(524, 95), (392, 241), (568, 379)],
    "pines": [
        ("LARGO", "der", (790, 290), (600, 330)),
        ("RUEDO", "izq", (223, 400), (345, 432)),
        ("CAPUCHA", None, (731, 103), None),
        ("CUELLO", "izq", (223, 110), (456, 86)),
        ("MAT1", None, (223, 244), None),
        ("MAT2", None, (790, 416), None),
        ("MAT3", "der", (790, 190), (556, 200)),
    ],
}


def linea(base, desde, hasta, color):
    capa = Image.new("RGBA", (base.width * SS, base.height * SS), (0, 0, 0, 0))
    d = ImageDraw.Draw(capa)
    d.line([(desde[0] * SS, desde[1] * SS), (hasta[0] * SS, hasta[1] * SS)], fill=color + (255,), width=3 * SS)
    capa = capa.resize(base.size, Image.LANCZOS)
    base.alpha_composite(capa)


def hacer(nombre, cfg):
    origen = os.path.join(GUI, f"esquema_tintes_{nombre}.png")
    im = Image.open(origen).convert("RGBA")
    marcos = {"izq": im.crop(cfg["izq"]), "der": im.crop(cfg["der"])}
    cx0, cy0 = cfg["puntos"][0]
    punto = im.crop((cx0 - 12, cy0 - 12, cx0 + 12, cy0 + 12))
    color = im.getpixel((cfg["puntos"][0][0], cfg["puntos"][0][1]))[:3]
    # Solo los píxeles del punto (fucsia o su brillo), sin el pergamino de alrededor
    px = punto.load()
    mascara = Image.new("L", punto.size, 0)
    mp = mascara.load()
    for yy in range(punto.height):
        for xx in range(punto.width):
            r, g, b, _ = px[xx, yy]
            if (r - g > 55) or (r > 215 and g > 200 and b > 190 and (xx - 12) ** 2 + (yy - 12) ** 2 < 49):
                mp[xx, yy] = 255
    mascara = mascara.filter(ImageFilter.MaxFilter(3))
    out = []
    for rol, lado, centro, objetivo in cfg["pines"]:
        if lado is not None:
            m = marcos[lado]
            x0, y0 = centro[0] - m.width // 2, centro[1] - m.height // 2
            im.paste(m, (x0, y0))
            borde = (x0 + m.width, centro[1]) if lado == "izq" else (x0, centro[1])
            linea(im, borde, objetivo, color)
            im.paste(punto, (objetivo[0] - 12, objetivo[1] - 12), mascara)
        out.append((rol, round(centro[0] / 4 - 8), round(centro[1] / 4 - 8) + ESQUEMA_Y))
    destino = os.path.join(GUI, f"esquema_{nombre}.png")
    os.makedirs(RESPALDO, exist_ok=True)
    if os.path.exists(destino) and not os.path.exists(os.path.join(RESPALDO, os.path.basename(destino))):
        shutil.copy(destino, RESPALDO)
    im.convert("RGB").quantize(colors=256, method=Image.MEDIANCUT).save(destino, optimize=True)
    print(nombre, ", ".join(f"{{{x}, {y}}}" for _, x, y in out), "# " + " ".join(r for r, _, _ in out))
    print(nombre, "PIN_BTN", ", ".join(f"{{{x + 17}, {y - 1}}}" for _, x, y in out))


if __name__ == "__main__":
    hacer("pollera", POLLERA)
    hacer("capa", CAPA)
