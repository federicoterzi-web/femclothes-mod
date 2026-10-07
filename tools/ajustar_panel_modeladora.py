"""Panel derecho de la Modeladora (2026-10-05, "los paneles derechos de las modeladora y estiladora tienen los tres leds a la
derecha y tienen tres cuadraditos sin uso, quiero que los saquemos... a la modeladora le vamos a poner un insumo asi que
quiero que agreguemos una pantallita donde va a aparecer una tijera... y una barra verde su durabilidad").

Idempotente. En `geo/garment_shaper.geo.json`:
  * saca los tres cubos decorativos horizontales (1,1 x 1,1 en y 6,85, textura del texel 112,0), que no hacían nada;
    los tres LED verticales del borde (x 5,6) se quedan, esos sí indican el estado;
  * agrega el hueso `tijera`: una pantallita de 3,75 x 3,0 px en el hueco que quedó, cuya cara del frente lee el
    rectángulo RECT_TIJERA (90, 50, 38, 30) del atlas, que `render/PantallaTijera` pinta en runtime.
Uso: python tools/ajustar_panel_modeladora.py
"""
import json
import os

RAIZ = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "modamod")
RUTA = os.path.join(RAIZ, "geo", "garment_shaper.geo.json")
RECT_TIJERA = (90, 50, 38, 30)


def main():
    datos = json.load(open(RUTA, encoding="utf-8"))
    huesos = datos["minecraft:geometry"][0]["bones"]
    base = next(b for b in huesos if b["name"] == "base")
    antes = len(base["cubes"])
    base["cubes"] = [c for c in base["cubes"]
                     if not (abs(c["origin"][1] - 6.85) < 0.01 and abs(c["size"][0] - 1.1) < 0.01 and c["origin"][0] < 5)]
    huesos[:] = [b for b in huesos if b["name"] != "tijera"]
    negro = {"uv": [81, 1], "uv_size": [2, 2]}
    huesos.append({
        "name": "tijera", "parent": "root", "pivot": [3.3, 6.8, -9.02],
        "cubes": [{
            "origin": [1.45, 5.3, -9.02], "size": [3.75, 3.0, 0.2],
            "uv": {"north": {"uv": [RECT_TIJERA[0], RECT_TIJERA[1]], "uv_size": [RECT_TIJERA[2], RECT_TIJERA[3]]},
                   "east": negro, "south": negro, "west": negro, "up": negro, "down": negro},
        }],
    })
    json.dump(datos, open(RUTA, "w", encoding="utf-8"), indent=2)
    print("cubos de la base:", antes, "->", len(base["cubes"]), "| hueso tijera agregado")


if __name__ == "__main__":
    main()
