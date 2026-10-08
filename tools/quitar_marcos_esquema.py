"""Borra marcos (con su línea y su punto fucsia) de un esquema de la Modeladora (2026-10-08, "podemos sacar dos
customizaciones total lo unico que entra ahi hasta ahora es la trama"): rellena el hueco con cv2.inpaint, que alcanza
para el pergamino y para la tela. El marco se da por el centro (px del PNG); la línea y el punto son las manchas
fucsia conectadas al marco.

Uso como módulo: quitar(imagen_PIL, [(cx, cy), ...]) -> imagen_PIL
"""
import cv2
import numpy as np
from PIL import Image

MITAD = 42          # medio marco (74 px) + margen
SS = 1


def _fucsia(rgb):
    r, g, b = rgb[..., 0].astype(int), rgb[..., 1].astype(int), rgb[..., 2].astype(int)
    return ((r - g > 55) & (r > 140) & (b > 50)).astype(np.uint8)


def quitar(im, centros):
    rgb = np.array(im.convert("RGB"))
    alto, ancho = rgb.shape[:2]
    fuc = _fucsia(rgb)
    mascara = np.zeros((alto, ancho), np.uint8)
    # manchas fucsia (líneas y puntos), un poco engordadas para no dejar el borde suave
    grande = cv2.dilate(fuc, np.ones((7, 7), np.uint8))
    n, etiquetas = cv2.connectedComponents(grande)
    for cx, cy in centros:
        x0, y0, x1, y1 = max(0, cx - MITAD), max(0, cy - MITAD), min(ancho, cx + MITAD), min(alto, cy + MITAD)
        mascara[y0:y1, x0:x1] = 255
        # las manchas que tocan este marco (su línea y su punto)
        for e in set(np.unique(etiquetas[y0:y1, x0:x1])) - {0}:
            comp = (etiquetas == e).astype(np.uint8) * 255
            if comp.sum() // 255 < 12000:        # no tragarse el dibujo entero si algo se conecta de más
                mascara = cv2.bitwise_or(mascara, comp)
    mascara = cv2.dilate(mascara, np.ones((5, 5), np.uint8))
    hecho = cv2.inpaint(rgb, mascara, 9, cv2.INPAINT_TELEA)
    return Image.fromarray(hecho)
