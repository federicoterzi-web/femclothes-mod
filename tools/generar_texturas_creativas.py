"""Texturas de las máquinas creativas (2026-10-01, "3 maquinas alternativas...
distinguibles de alguna manera de las maquinas normales" → "Modelo
recoloreado"): la misma textura de cada máquina pasada a violeta iridiscente
con detalles dorados.

  - madera y cobre (naranjas/marrones) → violeta, con un vaivén de tono por
    posición para que brille como nácar;
  - metal gris y el verdín de Tintes → dorado;
  - negro → negro violáceo;
  - crema/blanco (tela, papel) y los acentos de color (LEDs, tintas CMYK,
    pantallas) quedan igual: son funcionales.

Lee textures/block/<x>.png y escribe <x>_creativa.png (lo usa
util.MaquinaCreativa.textura).

Uso: python tools/generar_texturas_creativas.py
"""
import colorsys
import math
from pathlib import Path

from PIL import Image

CARPETA = Path(__file__).resolve().parent.parent / "src/main/resources/assets/modamod/textures/block"
ATLAS = ["garment_shaper_atlas", "dye_station_atlas", "sublimator_atlas", "styling_table_atlas", "estilista_atlas", "telar_atlas"]


def recolorear(r, g, b, x, y):
    h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    grados = h * 360
    if s < 0.15:
        if v < 0.22:                       # negro → negro violáceo
            h2, s2, v2 = 275 / 360, 0.35, v * 1.1
        elif v > 0.82:                     # crema/blanco: igual
            return r, g, b
        else:                              # metal gris → dorado
            h2, s2, v2 = 44 / 360, 0.62, min(1.0, v * 1.12)
    elif 12 <= grados <= 40 and s >= 0.3 and v <= 0.85:  # madera y cobre → violeta nacarado
        brillo = math.sin((x + y) / 5.0) * 22 + math.sin(x / 3.0 - y / 7.0) * 8
        h2, s2, v2 = ((276 + brillo) % 360) / 360, min(1.0, s * 1.05), min(1.0, v * 1.05)
    elif 140 <= grados <= 200 and s < 0.6 and v < 0.75:  # verdín de Tintes → dorado
        h2, s2, v2 = 44 / 360, 0.62, min(1.0, v * 1.25)
    else:                                  # acentos (LEDs, tintas, pantallas): igual
        return r, g, b
    r2, g2, b2 = colorsys.hsv_to_rgb(h2, s2, v2)
    return int(r2 * 255), int(g2 * 255), int(b2 * 255)


def main():
    for nombre in ATLAS:
        img = Image.open(CARPETA / f"{nombre}.png").convert("RGBA")
        px = img.load()
        for y in range(img.height):
            for x in range(img.width):
                r, g, b, a = px[x, y]
                if a == 0:
                    continue
                if nombre in ("estilista_atlas", "telar_atlas") and (x, y) in ((100, 60), (101, 60), (102, 60)):
                    continue                           # los acentos lila / hilo / cuero de la Estilista no se recolorean
                if (x, y) in ((103, 60), (104, 60), (63, 63), (62, 63)):
                    continue                           # botón y LED de la línea de producción (tools/agregar_boton_linea.py)
                px[x, y] = (*recolorear(r, g, b, x, y), a)
        img.save(CARPETA / f"{nombre}_creativa.png")
    print("ok", CARPETA)


if __name__ == "__main__":
    main()
