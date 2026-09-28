"""Compone los 4 esquemas de la Mesa de Modelado a partir de los PNG pergamino
generados aparte (2026-09-26): borra los rótulos horneados (los textos los
dibuja el juego, para que se traduzcan), borra las chinchetas horneadas (las
reales son botones), y para MEDIAS y CALIENTABRAZOS reubica los slots de
personalización en 2 columnas (izquierda/derecha) redibujando las líneas.

Uso: python3 tools/componer_esquemas.py — lee de Descargas, escribe en
src/main/resources/assets/femclothes/textures/gui/container/esquema_*.png y
en .scratch_mpm/posiciones.json (centros de slot en píxeles fuente).
"""
import cv2, json, numpy as np

D = 'C:/Users/feder/Downloads/'
OUT = 'src/main/resources/assets/femclothes/textures/gui/container/'
FUENTES = {
    'remera': 'ChatGPT Image 24 sept 2026, 12_50_36.png',
    'pantalon': 'pantalon.png',
    'medias': 'medias.png',
    'calientabrazos': 'calientabrazos.png',
}
MAGENTA = (140, 32, 236)  # BGR

# Centros de slot (px fuente) en el orden de ModeladoBlockEntity#ROLES
SLOTS = {
    'remera': [(835, 129), (456, 160), (1213, 161), (347, 390), (836, 424), (1322, 390), (453, 686), (836, 845)],
    'pantalon': [(835, 122), (836, 352), (836, 500), (836, 650), (383, 465), (400, 799), (1273, 804)],
    'medias': [(361, 169), (1311, 169), (352, 790), (1339, 789), (839, 790),
               (250, 330), (250, 460), (250, 590), (1420, 330), (1420, 460), (1420, 590)],
    'calientabrazos': [(323, 186), (1344, 186), (311, 736), (1360, 738), (835, 800),
                       (250, 340), (250, 470), (250, 600), (1420, 340), (1420, 470), (1420, 600)],
}
# Slots ORIGINALES que hay que borrar antes de reubicar (centros en la imagen fuente)
BORRAR = {
    'medias': [(839, 287), (839, 459), (839, 633), (331, 527)],
    'calientabrazos': [(835, 189), (832, 386), (835, 582)],
}
# Zonas donde borrar líneas/puntos magenta viejos (x0, y0, x1, y1)
BORRAR_LINEAS = {
    'medias': [(600, 200, 1100, 700), (380, 490, 575, 645)],
    'calientabrazos': [(630, 190, 1040, 760)],
}
# Slot fuente para copiar el marco (centro en la imagen ya limpia)
SLOT_MODELO = {'medias': (361, 169), 'calientabrazos': (323, 186)}
MEDIO = 66  # semilado de la caja del slot


def pines_horneados(img):
    hsv = cv2.cvtColor(img, cv2.COLOR_BGR2HSV)
    pink = cv2.inRange(hsv, (150, 100, 130), (180, 255, 255))
    pink = cv2.morphologyEx(pink, cv2.MORPH_CLOSE, cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (9, 9)))
    big = cv2.morphologyEx(pink, cv2.MORPH_OPEN, cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (13, 13)))
    n, lab, st, cen = cv2.connectedComponentsWithStats(big)
    m = np.zeros(img.shape[:2], np.uint8)
    centros = []
    for i in range(1, n):
        x, y, w, h, a = st[i]
        if w >= 30 and h >= 30:
            m[lab == i] = 255
            centros.append((x + w / 2, y + h / 2))
    return m, centros


def quitar_pines(img, slots_orig):
    m, centros = pines_horneados(img)
    if not centros:
        return img
    m = cv2.dilate(m, cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (19, 19)))
    hsv = cv2.cvtColor(img, cv2.COLOR_BGR2HSV)
    gris = cv2.dilate(cv2.inRange(hsv, (0, 0, 60), (180, 70, 190)), np.ones((5, 5), np.uint8))
    for (px, py) in centros:
        v = np.zeros_like(m)
        cv2.rectangle(v, (int(px) - 40, int(py) - 10), (int(px) + 25, int(py) + 55), 255, -1)
        m |= cv2.bitwise_and(gris, v)
    limpio = cv2.inpaint(img, m, 6, cv2.INPAINT_TELEA)
    # esquina superior derecha del marco: se reconstruye espejando la izquierda
    for (sx, sy) in slots_orig:
        for dy in range(-66, -30):
            for dx in range(30, 67):
                limpio[sy + dy, sx + dx] = limpio[sy + dy, sx - dx]
    return limpio


def quitar_texto(img, slots, abajo=60):
    """Rótulos: letras (componentes oscuros chicos) en la franja de arriba de cada slot.
    Se filtra por tamaño para no llevarse contornos de la prenda ni marcos."""
    hsv = cv2.cvtColor(img, cv2.COLOR_BGR2HSV)
    oscuro = cv2.inRange(hsv, (0, 0, 0), (180, 255, 120))
    n, lab, st, _ = cv2.connectedComponentsWithStats(oscuro)
    m = np.zeros(img.shape[:2], np.uint8)
    for (cx, cy) in slots:
        x0, x1 = max(0, cx - 180), min(img.shape[1], cx + 180)
        y0, y1 = max(20, cy - 140), max(20, cy - abajo)
        for i in range(1, n):
            x, y, w, h, a = st[i]
            if a < 1200 and h <= 55 and w <= 70 and x >= x0 and x + w <= x1 and y >= y0 and y + h <= y1:
                m[lab == i] = 255
    m = cv2.dilate(m, cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (11, 11)))
    return cv2.inpaint(img, m, 5, cv2.INPAINT_TELEA)


def parche_limpio(img, w, h, evitar):
    """Busca en el margen un parche de pergamino liso (poca textura, saturado)."""
    hsv = cv2.cvtColor(img, cv2.COLOR_BGR2HSV)
    gris = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY).astype(float)
    mejor, mejor_s = None, 1e9
    H, W = img.shape[:2]
    for y in range(110, H - 110 - h, 40):
        for x in range(110, W - 110 - w, 40):
            if any(x < ex1 and x + w > ex0 and y < ey1 and y + h > ey0 for (ex0, ey0, ex1, ey1) in evitar):
                continue
            sat = hsv[y:y + h, x:x + w, 1].mean()
            if sat < 70:
                continue
            sc = gris[y:y + h, x:x + w].std()
            if sc < mejor_s:
                mejor, mejor_s = (x, y), sc
    return mejor


def pegar(img, parche, cx, cy, borde=10):
    """Pega {@code parche} centrado en (cx, cy) con bordes difuminados (sin cambiar los colores del parche)."""
    h, w = parche.shape[:2]
    m = np.zeros((h, w), np.float32)
    m[borde:h - borde, borde:w - borde] = 1.0
    m = cv2.GaussianBlur(m, (0, 0), borde / 2.0)[:, :, None]
    x0, y0 = cx - w // 2, cy - h // 2
    dst = img[y0:y0 + h, x0:x0 + w].astype(np.float32)
    img[y0:y0 + h, x0:x0 + w] = (parche.astype(np.float32) * m + dst * (1 - m)).astype(np.uint8)
    return img


FUENTE_PARCHE = {
    'medias': [(1370, 280), (1370, 300), (1370, 290), (1370, 285)],
    'calientabrazos': [(90, 290), (90, 440), (90, 440)],
}


def borrar_slot(img, cx, cy, fuente):
    w = h = 2 * (MEDIO + 40)
    x, y = fuente
    parche = img[y:y + h, x:x + w].copy()
    # iguala el tono del parche al de la zona destino (anillo exterior sin marco)
    x0, y0 = cx - w // 2, cy - h // 2
    def anillo(im, ox, oy):
        caja = im[oy:oy + h, ox:ox + w].astype(np.float32)
        m = np.ones((h, w), bool)
        m[10:h - 10, 10:w - 10] = False
        return caja[m].mean(axis=0)
    delta = anillo(img, x0, y0) - anillo(img, x, y)
    parche = np.clip(parche.astype(np.float32) + delta, 0, 255).astype(np.uint8)
    return pegar(img, parche, cx, cy, 26)


def poner_slot(img, sprite, cx, cy):
    return pegar(img, sprite, cx, cy, 6)


def borde_tela(img, y, lado):
    """x del borde de la prenda (crema poco saturada) en la fila y: lado 'izq' = primer pixel de tela de la pierna izquierda."""
    hsv = cv2.cvtColor(img, cv2.COLOR_BGR2HSV)
    fila = hsv[y]
    tela = (fila[:, 1] < 70) & (fila[:, 2] > 180)
    W = img.shape[1]
    xs = np.where(tela)[0]
    if lado == 'izq':
        xs = xs[(xs > 400) & (xs < W // 2)]
        return int(xs.min()) if len(xs) else 560
    xs = xs[(xs < W - 400) & (xs > W // 2)]
    return int(xs.max()) if len(xs) else W - 560


def borde_interno(img, y, lado):
    """Borde de la pieza que mira al centro: 'izq' = x más a la derecha de la pieza izquierda, 'der' = x más a la izquierda de la derecha."""
    hsv = cv2.cvtColor(img, cv2.COLOR_BGR2HSV)
    fila = hsv[y]
    tela = (fila[:, 1] < 70) & (fila[:, 2] > 180)
    W = img.shape[1]
    xs = np.where(tela)[0]
    if lado == 'izq':
        xs = xs[(xs > 400) & (xs < W // 2)]
        return int(xs.max()) if len(xs) else W // 2 - 150
    xs = xs[(xs < W - 400) & (xs > W // 2)]
    return int(xs.min()) if len(xs) else W // 2 + 150


def punto(img, x, y):
    cv2.circle(img, (x, y), 12, MAGENTA, -1, cv2.LINE_AA)
    cv2.circle(img, (x - 3, y - 3), 4, (235, 200, 250), -1, cv2.LINE_AA)


def linea(img, a, b):
    cv2.line(img, a, b, MAGENTA, 5, cv2.LINE_AA)
    punto(img, b[0], b[1])


posiciones = {}
for nombre, archivo in FUENTES.items():
    img = cv2.imread(D + archivo)
    slots_orig = {
        'remera': [], 'pantalon': SLOTS['pantalon'],
        'medias': [(361, 169), (1311, 169), (839, 287), (839, 459), (839, 633), (331, 527), (352, 790), (1339, 789)],
        'calientabrazos': [(323, 186), (835, 189), (1344, 186), (832, 386), (835, 582), (311, 736), (1360, 738), (835, 800)],
    }[nombre]
    img = quitar_pines(img, slots_orig)
    # texto de TODOS los slots originales
    orig_para_texto = slots_orig if slots_orig else SLOTS[nombre]
    img = quitar_texto(img, orig_para_texto, 46 if nombre == 'pantalon' else 60)

    if nombre in ('medias', 'calientabrazos'):
        # líneas/puntos magenta viejos de la zona central
        hsv = cv2.cvtColor(img, cv2.COLOR_BGR2HSV)
        rosa = cv2.inRange(hsv, (150, 100, 130), (180, 255, 255))
        m = np.zeros_like(rosa)
        for (x0, y0, x1, y1) in BORRAR_LINEAS[nombre]:
            m[y0:y1, x0:x1] = rosa[y0:y1, x0:x1]
        m = cv2.dilate(m, cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (9, 9)))
        img = cv2.inpaint(img, m, 5, cv2.INPAINT_TELEA)
        # sprite del marco (antes de borrar/pegar nada)
        mx, my = SLOT_MODELO[nombre]
        sprite = img[my - MEDIO:my + MEDIO, mx - MEDIO:mx + MEDIO].copy()
        # slots a borrar (las cajas nuevas también se evitan como fuente de parche)
        evitar = [(cx - 90, cy - 90, cx + 90, cy + 90) for cx, cy in SLOTS[nombre]] + \
                 [(cx - 90, cy - 90, cx + 90, cy + 90) for cx, cy in BORRAR[nombre]]
        for k, (cx, cy) in enumerate(BORRAR[nombre]):
            img = borrar_slot(img, cx, cy, FUENTE_PARCHE[nombre][k])
        cv2.imwrite('.scratch_mpm/dbg_%s.png' % nombre, img[90:300, 700:980])
        for (cx, cy) in SLOTS[nombre][5:] + [SLOTS[nombre][4]]:
            img = poner_slot(img, sprite, cx, cy)
        # conexiones nuevas: personalización izq/der + calce a las dos piezas
        pers_izq, pers_der = SLOTS[nombre][5:8], SLOTS[nombre][8:11]
        for (cx, cy) in pers_izq:
            linea(img, (cx + MEDIO, cy), (borde_tela(img, cy, 'izq') + 22, cy))
        for (cx, cy) in pers_der:
            linea(img, (cx - MEDIO, cy), (borde_tela(img, cy, 'der') - 22, cy))
        cx, cy = SLOTS[nombre][4]
        ty = cy if nombre == 'medias' else 690
        xi = borde_interno(img, ty, 'izq') - 22
        xd = borde_interno(img, ty, 'der') + 22
        linea(img, (cx - MEDIO + 8, cy - 25), (xi, ty))
        linea(img, (cx + MEDIO - 8, cy - 25), (xd, ty))

    cv2.imwrite(OUT + 'esquema_%s.png' % nombre, img)
    posiciones[nombre] = {'size': [img.shape[1], img.shape[0]], 'slots': SLOTS[nombre]}
    print(nombre, 'ok', img.shape[1], img.shape[0])

json.dump(posiciones, open('.scratch_mpm/posiciones.json', 'w'))
