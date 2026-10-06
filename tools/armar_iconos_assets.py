"""Arma los íconos de molde de 64x64 con los sprites unificados del usuario (2026-10-06, "escucha ahi tengo assets para armar
los items... esto me parece lo mejor para unificar" -> "empecemos solo por la tabla de faldas pantalones torsos y capas").

Cada ícono = el panel de pergamino plegado de fondo (`papel.png`) + el sprite centrado encima. Los sprites vienen en tiras
(`faldas.png`, `pantalones.png`, `torsos.png`, `capas.png`) de assets_nuevos_prueba/assets_2026-10-06/; se cortan por columnas
vacías (donde dos se tocan, por el valle de menos alfa). Los íconos que reemplaza se copian antes a
assets_viejos/iconos_assets_2026-10-06/.
"""
import os, shutil
import numpy as np
from PIL import Image

RAIZ = os.path.normpath(os.path.join(os.path.dirname(__file__), ".."))
FUENTE = os.path.join(RAIZ, "assets_nuevos_prueba", "assets_2026-10-06")
ITEMS = os.path.join(RAIZ, "src", "main", "resources", "assets", "femclothes", "textures", "item")
VIEJOS = os.path.join(RAIZ, "assets_viejos", "iconos_assets_2026-10-06")

# tira -> nombres de ítem en el orden de la imagen
TIRAS = {
    "faldas.png": ["molde_pollera_campana", "molde_pollera_circular", "molde_pollera_tubo", "molde_pollera_tableada", "molde_pollera_globo"],
    "pantalones.png": ["molde_calce_pegado", "molde_calce_ajustado", "molde_calce_normal", "molde_calce_suelto", "molde_calce_oversize"],
    "torsos.png": ["molde_cuello_redondo", "molde_cuello_v", "molde_cuello_polera", "molde_cuello_cuadrado", "molde_cuello_corazon"],
    "capas.png": ["molde_capa_ruedo_redondeado", "molde_capa_ruedo_recto", "molde_capa_cuello_alto", "molde_capa_ruedo_cola"],
}
TAM = 64
CAJA = 54          # lado máximo del sprite MÁS GRANDE de cada tira dentro del papel (margen para el marco); los demás de la tira escalan igual


def cortar(tira, n):
    """Las n columnas-rango de la tira; si hay menos rangos que sprites, parte el más ancho por su valle de alfa."""
    alfa = np.array(tira)[..., 3] > 20
    col = alfa.any(axis=0)
    rangos, s = [], None
    for x, v in enumerate(col):
        if v and s is None: s = x
        if not v and s is not None: rangos.append([s, x]); s = None
    if s is not None: rangos.append([s, len(col)])
    cuenta = alfa.sum(axis=0)
    while len(rangos) < n:
        i = max(range(len(rangos)), key=lambda k: rangos[k][1] - rangos[k][0])
        a, b = rangos[i]
        ancho = b - a
        # el valle en el tramo del medio, con tantos sprites como falten partir en ese rango
        desde, hasta = a + ancho // 5, b - ancho // 5
        corte = desde + int(np.argmin(cuenta[desde:hasta]))
        rangos[i:i + 1] = [[a, corte], [corte, b]]
    return [tuple(r) for r in rangos]


def sprite(tira, x0, x1):
    recorte = tira.crop((x0, 0, x1, tira.height))
    caja = recorte.getbbox()
    return recorte.crop(caja)


def componer(papel, spr, k):
    fondo = papel.resize((TAM, TAM), Image.LANCZOS).convert("RGBA")
    w, h = spr.size
    spr = spr.resize((max(1, round(w * k)), max(1, round(h * k))), Image.LANCZOS)
    fondo.alpha_composite(spr, ((TAM - spr.width) // 2, (TAM - spr.height) // 2 + 1))
    return fondo


def main():
    papel = Image.open(os.path.join(FUENTE, "papel.png")).convert("RGBA")
    os.makedirs(VIEJOS, exist_ok=True)
    for archivo, nombres in TIRAS.items():
        tira = Image.open(os.path.join(FUENTE, archivo)).convert("RGBA")
        rangos = cortar(tira, len(nombres))
        assert len(rangos) >= len(nombres), (archivo, rangos)   # torsos.png trae un 6.º (capucha) que todavía no se usa
        sprites = [sprite(tira, x0, x1) for (x0, x1) in rangos[:len(nombres)]]
        k = min(CAJA / max(sp.width for sp in sprites), CAJA / max(sp.height for sp in sprites))   # misma escala en toda la tira
        for nombre, spr in zip(nombres, sprites):
            destino = os.path.join(ITEMS, nombre + ".png")
            if os.path.exists(destino) and not os.path.exists(os.path.join(VIEJOS, nombre + ".png")):
                shutil.copy(destino, os.path.join(VIEJOS, nombre + ".png"))
            componer(papel, spr, k).save(destino)
            print("ok", nombre)


if __name__ == "__main__":
    main()
