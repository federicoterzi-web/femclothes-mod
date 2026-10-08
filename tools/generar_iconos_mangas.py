"""Íconos de remera con manga mínima, media y siete octavos (2026-10-08, hallazgo H06 de ChatGPT: 18 modelos apuntaban a
PNG que no existían, y "vamos con tu arte"). Se arman recortando los íconos de manga larga: las filas de la manga que
pasan del largo se reemplazan por las del ícono sin manga, columnas de los costados incluidas (ahí está el borde claro del
torso). Largo de manga en el ícono: filas 4 .. 4 + filas de la manga (corta 8, tres cuartos 12, larga 15).

Entrada: textures/item/corte_<largo>_{sin,larga}_<cuello>.png. Salida: corte_<largo>_{minima,media,siete_octavos}_<cuello>.png
y los modelos models/item/corte_<largo>_<manga>_<cuello>.json apuntando a ellos.
Uso: python tools/generar_iconos_mangas.py
"""
import json
import os

from PIL import Image

RAIZ = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "modamod")
ITEM = os.path.join(RAIZ, "textures", "item")
MODELOS = os.path.join(RAIZ, "models", "item")
LARGOS = ("crop", "normal", "largo")
CUELLOS = ("redondo", "v", "polera", "cuadrado", "corazon")
MANGAS = {"minima": 2, "media": 6, "siete_octavos": 10}     # filas de manga (Variante.Manga)


def main():
    n = 0
    for largo in LARGOS:
        for cuello in CUELLOS:
            sin = Image.open(os.path.join(ITEM, f"corte_{largo}_sin_{cuello}.png")).convert("RGBA")
            larga = Image.open(os.path.join(ITEM, f"corte_{largo}_larga_{cuello}.png")).convert("RGBA")
            for manga, filas in MANGAS.items():
                fin = min(15, 4 + filas)
                im = larga.copy()
                for y in range(fin + 1, 16):
                    for x in list(range(0, 5)) + list(range(11, 16)):
                        im.putpixel((x, y), sin.getpixel((x, y)))
                nombre = f"corte_{largo}_{manga}_{cuello}"
                im.save(os.path.join(ITEM, nombre + ".png"))
                modelo = {"parent": "minecraft:item/generated", "textures": {"layer0": f"modamod:item/{nombre}"}}
                with open(os.path.join(MODELOS, nombre + ".json"), "w", encoding="utf-8") as f:
                    json.dump(modelo, f, indent=2)
                    f.write("\n")
                n += 1
    print("íconos y modelos:", n)


if __name__ == "__main__":
    main()
