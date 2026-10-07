"""Procesa los assets pergamino generados aparte (2026-09-26) para la Mesa de
Modelado: (1) borra las chinchetas HORNEADAS de cada esquema (las chinchetas
reales son botones del juego, ver ModeladoScreen#BotonChincheta) rellenando
con inpainting, y (2) recorta los 3 cuadros del sprite de la chincheta
(sin fijar / a mitad / fijada) con fondo transparente.

Uso: python3 tools/procesar_assets_esquemas.py  (lee de Descargas, escribe en
src/main/resources/assets/modamod/textures/gui/container/). Imprime los
centros de slot y de chincheta en píxeles FUENTE para medir ModeladoScreenHandler.
"""
import cv2, json, numpy as np

D = 'C:/Users/feder/Downloads/'
OUT = 'src/main/resources/assets/modamod/textures/gui/container/'

SLOTS = {
    'calientabrazos': [(323, 186), (835, 189), (1344, 186), (832, 386), (835, 582), (311, 736), (1360, 738), (835, 800)],
    'pantalon': [(835, 122), (836, 352), (836, 500), (836, 650), (383, 465), (400, 799), (1273, 804)],
    'medias': [(361, 169), (1311, 169), (839, 287), (839, 459), (839, 633), (331, 527), (352, 790), (1339, 789)],
}


def pines(img):
    hsv = cv2.cvtColor(img, cv2.COLOR_BGR2HSV)
    pink = cv2.inRange(hsv, (150, 100, 130), (180, 255, 255))
    pink = cv2.morphologyEx(pink, cv2.MORPH_CLOSE, cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (9, 9)))
    big = cv2.morphologyEx(pink, cv2.MORPH_OPEN, cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (13, 13)))
    n, lab, st, cen = cv2.connectedComponentsWithStats(big)
    out = []
    for i in range(1, n):
        x, y, w, h, a = st[i]
        if w >= 30 and h >= 30:
            out.append((i, (x + w / 2, y + h / 2)))
    return lab, out


def img_orig_izq(im, sx, sy, dx, dy):
    return im[sy + dy, sx - dx]


res = {}
for name, slots in SLOTS.items():
    img = cv2.imread(D + name + '.png')
    lab, ps = pines(img)
    mask = np.zeros(img.shape[:2], np.uint8)
    for i, _ in ps:
        mask[lab == i] = 255
    # contorno oscuro y brillo de la cabeza: poco margen (no comerse el rótulo de arriba)
    mask = cv2.dilate(mask, cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (19, 19)))
    # la aguja gris que asoma (sin saturación) cerca de cada chincheta
    hsv = cv2.cvtColor(img, cv2.COLOR_BGR2HSV)
    gris = cv2.inRange(hsv, (0, 0, 60), (180, 70, 235))
    for _, (px, py) in ps:
        v = np.zeros_like(mask)
        cv2.rectangle(v, (int(px) - 40, int(py) - 10), (int(px) + 25, int(py) + 55), 255, -1)
        mask |= cv2.bitwise_and(cv2.dilate(gris, np.ones((5, 5), np.uint8)), v)
    limpio = cv2.inpaint(img, mask, 6, cv2.INPAINT_TELEA)
    # esquina superior derecha del marco del slot: se reconstruye espejando la izquierda
    for (sx, sy) in slots:
        for dy in range(-66, -30):
            for dx in range(30, 67):
                limpio[sy + dy, sx + dx] = img_orig_izq(limpio, sx, sy, dx, dy)
    cv2.imwrite(OUT + 'esquema_%s.png' % name, limpio)
    # cada slot con su chincheta más cercana
    pares = []
    for (sx, sy) in slots:
        px, py = min((c for _, c in ps), key=lambda c: (c[0] - sx) ** 2 + (c[1] - sy) ** 2)
        pares.append({'slot': [sx, sy], 'pin': [round(px), round(py)]})
    res[name] = pares
    print(name, json.dumps(pares))

# sprite de chincheta
sp = cv2.imread(D + 'chincheta.png')
h, w = sp.shape[:2]
bg = np.median(sp.reshape(-1, 3), axis=0)
dist = np.sqrt(((sp.astype(int) - bg) ** 2).sum(axis=2))
m = (dist > 55).astype(np.uint8) * 255
m = cv2.morphologyEx(m, cv2.MORPH_OPEN, cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (5, 5)))
n, lab, st, _ = cv2.connectedComponentsWithStats(m)
comps = sorted([i for i in range(1, n) if st[i][4] > 2000], key=lambda i: st[i][0])
print('frames', [tuple(st[i][:4]) for i in comps])
cuadros = []
for i in comps[:3]:
    x, y, bw, bh, _ = st[i]
    pad = 6
    x0, y0, x1, y1 = max(0, x - pad), max(0, y - pad), min(w, x + bw + pad), min(h, y + bh + pad)
    crop = sp[y0:y1, x0:x1]
    alpha = ((lab[y0:y1, x0:x1] == i).astype(np.uint8) * 255)
    alpha = cv2.dilate(alpha, np.ones((3, 3), np.uint8))
    cuadros.append(np.dstack([crop, alpha]))
# MISMA escala para los 3 cuadros (si cada uno se ajustara a su caja, la fijada
# — más baja — se agrandaría), alineados abajo al centro: al clavarse solo baja.
S = max(max(c.shape[:2]) for c in cuadros)
for k, c in enumerate(cuadros):
    lienzo = np.zeros((S, S, 4), np.uint8)
    oy, ox = S - c.shape[0], (S - c.shape[1]) // 2
    lienzo[oy:oy + c.shape[0], ox:ox + c.shape[1]] = c
    lienzo = cv2.resize(lienzo, (64, 64), interpolation=cv2.INTER_AREA)
    cv2.imwrite(OUT + 'chincheta_%d.png' % k, lienzo)
