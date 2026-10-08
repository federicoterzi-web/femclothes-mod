"""Botón físico de la línea de producción en el frente de las 5 máquinas automáticas (2026-10-08, "agregar el mismo
botoncito de encendido y apagado que esta dentro de las guis en el frente"): un botón de metal abajo a la izquierda del
frente (hueso `boton_linea`) con un LED verde encima (hueso `led_linea`, el cliente lo esconde con la línea apagada).

Idempotente: saca los huesos si ya estaban y los vuelve a poner; pinta dos texels libres en el atlas normal y en el
`_creativa`. Correr DESPUÉS de generar_modelo_estilista.py / generar_modelo_telar.py / generar_texturas_creativas.py
(que regeneran sus archivos). Click: util/BotonLinea (misma geometría: x -6,4..-2,4 y 1,8..3,7 del modelo).
Uso: python tools/agregar_boton_linea.py
"""
import json
import os

from PIL import Image

RAIZ = os.path.join(os.path.dirname(__file__), "..")
RES = os.path.join(RAIZ, "src", "main", "resources", "assets", "modamod")

# (geo, atlas) de las 5 máquinas, con el texel del botón y el del LED en cada atlas (libres: alfa 0).
MAQUINAS = {
    "garment_shaper": ("garment_shaper_atlas", (103, 60), (104, 60)),
    "dye_station": ("dye_station_atlas", (63, 63), (62, 63)),
    "sublimator": ("sublimator_atlas", (103, 60), (104, 60)),
    "estilista": ("estilista_atlas", (103, 60), (104, 60)),
    "telar": ("telar_atlas", (103, 60), (104, 60)),
}
METAL = (0xC9, 0xC2, 0xB0, 255)
VERDE = (0x3C, 0xFF, 0x6B, 255)


def caras(texel):
    return {c: {"uv": [texel[0], texel[1]], "uv_size": [0.3, 0.3]} for c in ("north", "south", "east", "west", "up", "down")}


def main():
    for geo_nombre, (atlas, t_boton, t_led) in MAQUINAS.items():
        ruta = os.path.join(RES, "geo", geo_nombre + ".geo.json")
        data = json.load(open(ruta, encoding="utf-8"))
        g = data["minecraft:geometry"][0]
        g["bones"] = [b for b in g["bones"] if b["name"] not in ("boton_linea", "led_linea")]
        g["bones"].append({"name": "boton_linea", "parent": "root", "pivot": [-4.8, 2.8, -8.0],
                           "cubes": [{"origin": [-5.6, 2.3, -8.32], "size": [1.6, 1.0, 0.32], "uv": caras(t_boton)}]})
        g["bones"].append({"name": "led_linea", "parent": "root", "pivot": [-3.25, 2.8, -8.0],
                           "cubes": [{"origin": [-3.5, 2.55, -8.22], "size": [0.5, 0.5, 0.22], "uv": caras(t_led)}]})
        json.dump(data, open(ruta, "w", encoding="utf-8"), indent=2)
        for sufijo in ("", "_creativa"):
            p = os.path.join(RES, "textures", "block", atlas + sufijo + ".png")
            im = Image.open(p).convert("RGBA")
            im.putpixel(t_boton, METAL)
            im.putpixel(t_led, VERDE)
            im.save(p)
        print("ok", geo_nombre)


if __name__ == "__main__":
    main()
