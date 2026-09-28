import json, math, uuid

# ---- matriz de rotacion, misma convencion que ModelPart.rotate():
#      v' = Rz(roll) * Ry(yaw) * Rx(pitch) * v   (grados)
def rx(deg):
    a = math.radians(deg); c, s = math.cos(a), math.sin(a)
    return [[1,0,0],[0,c,-s],[0,s,c]]
def ry(deg):
    a = math.radians(deg); c, s = math.cos(a), math.sin(a)
    return [[c,0,s],[0,1,0],[-s,0,c]]
def rz(deg):
    a = math.radians(deg); c, s = math.cos(a), math.sin(a)
    return [[c,-s,0],[s,c,0],[0,0,1]]
def matmul(a,b):
    return [[sum(a[i][k]*b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]
def matvec(m,v):
    return [sum(m[i][k]*v[k] for k in range(3)) for i in range(3)]
def add(a,b): return [a[i]+b[i] for i in range(3)]
IDENT = [[1,0,0],[0,1,0],[0,0,1]]

def decompose_zyx(R):
    """Inversa de rz(roll)@ry(yaw)@rx(pitch) -> (pitch,yaw,roll) en grados."""
    yaw = math.degrees(math.asin(max(-1.0, min(1.0, -R[2][0]))))
    pitch = math.degrees(math.atan2(R[2][1], R[2][2]))
    roll = math.degrees(math.atan2(R[1][0], R[0][0]))
    return pitch, yaw, roll

# self-test del round-trip antes de confiar en la formula
import random
random.seed(1)
for _ in range(200):
    p0,y0,r0 = (random.uniform(-80,80) for _ in range(3))
    R = matmul(matmul(rz(r0), ry(y0)), rx(p0))
    p1,y1,r1 = decompose_zyx(R)
    R2 = matmul(matmul(rz(r1), ry(y1)), rx(p1))
    for i in range(3):
        for j in range(3):
            assert abs(R[i][j]-R2[i][j]) < 1e-6, (p0,y0,r0,p1,y1,r1)
print("round-trip OK")

class Frame:
    def __init__(self, origin, rot):
        self.origin = origin
        self.rot = rot
    def child(self, pivot_local, pitch, yaw, roll):
        world_origin = add(self.origin, matvec(self.rot, pivot_local))
        local_rot = matmul(matmul(rz(roll), ry(yaw)), rx(pitch))
        world_rot = matmul(self.rot, local_rot)
        return Frame(world_origin, world_rot)

# ---- mismas constantes que PolleraGeometria.java ----
GAJOS = 10
PASO_YAW = 360.0 / GAJOS
ESCALA_RADIAL = 1.79
OFFSET_AFUERA = 2.4 * ESCALA_RADIAL
OFFSET_ABAJO = 8.8
TILT_PITCH, TILT_YAW, TILT_ROLL = 17.18, -11.5, -11.5
LARGO_GAJO = 9.0
ANCHO_GAJO = 2.0 * ESCALA_RADIAL
GROSOR = 0.1
OFFSET_PLIEGUE = 2.0 * ESCALA_RADIAL

root = Frame([0,0,0], IDENT)

elements = []
outliner = []

def make_cube(name, frame, local_from, local_to, color=0):
    pitch, yaw, roll = decompose_zyx(frame.rot)
    rot = [pitch, yaw, roll]
    # Blockbench limita rotacion de cubos a un solo eje no-cero por
    # elemento en algunos exportadores; para referencia visual en el editor
    # el triple completo se guarda igual (el archivo es para inspeccionar/
    # ajustar a mano en Blockbench, no para exportar de vuelta a Java).
    #
    # BUG FIX: "from"/"to" tienen que ser el punto LOCAL ya trasladado al
    # origen del pivote (origin + local) -- Blockbench rota el cubo
    # alrededor de "origin", asi que si "from/to" quedan en coordenadas
    # locales puras (cerca de 0,0,0) el cubo aparece pegado al centro del
    # modelo en vez de en la posicion real del gajo.
    world_from = add(frame.origin, local_from)
    world_to = add(frame.origin, local_to)
    eid = str(uuid.uuid4())
    elements.append({
        "name": name,
        "box_uv": False,
        "rescale": False,
        "locked": False,
        "light_emission": 0,
        "render_order": "default",
        "allow_mirror_modeling": True,
        "from": world_from,
        "to": world_to,
        "autouv": 0,
        "color": color,
        "origin": frame.origin,
        "rotation": rot,
        "uv_offset": [0, 0],
        "faces": {
            d: {"uv": [0,0,4,4], "texture": None}
            for d in ["north","east","south","west","up","down"]
        },
        "type": "cube",
        "uuid": eid,
    })
    return eid

for i in range(GAJOS):
    yaw_i = i * PASO_YAW
    gajo_frame = root.child([0,0,0], 0, yaw_i, 0)
    inner_frame = gajo_frame.child([OFFSET_AFUERA, OFFSET_ABAJO, 0], TILT_PITCH, TILT_YAW, TILT_ROLL)

    group_children = []

    # recto: offsetX=0, sin rotacion propia
    recto_frame = inner_frame.child([0,0,0], 0, 0, 0)
    x0, x1 = -GROSOR/2, GROSOR/2
    eid = make_cube(f"gajo{i}_recto", recto_frame,
                     [x0, 0, 0], [x1, LARGO_GAJO, ANCHO_GAJO], color=1)
    group_children.append(eid)

    # plegado: offsetX=OFFSET_PLIEGUE horneado en el cuboide, yaw propio -90
    plegado_frame = inner_frame.child([0,0,0], 0, -90, 0)
    x0, x1 = OFFSET_PLIEGUE - GROSOR/2, OFFSET_PLIEGUE + GROSOR/2
    eid = make_cube(f"gajo{i}_plegado", plegado_frame,
                     [x0, 0, 0], [x1, LARGO_GAJO, ANCHO_GAJO], color=2)
    group_children.append(eid)

    outliner.append({
        "name": f"gajo{i}",
        "origin": inner_frame.origin,
        "color": 0,
        "uuid": str(uuid.uuid4()),
        "export": True,
        "isOpen": False,
        "locked": False,
        "visibility": True,
        "autouv": 0,
        "children": group_children,
    })

# ---- cubo de referencia del torso (mismas proporciones que
#      PlayerEntityModel: 8 de ancho, 12 de alto, 4 de profundidad,
#      pivote arriba-centro, igual que usa CuerpoGeometria/ModelPart) ----
torso_frame = Frame([0,0,0], IDENT)
torso_id = make_cube("torso_referencia", torso_frame, [-4, 0, -2], [4, 12, 2], color=5)

outliner.append(torso_id)

bbmodel = {
    "meta": {
        "format_version": "4.10",
        "model_format": "free",
        "box_uv": False,
    },
    "name": "pollera_referencia",
    "model_identifier": "",
    "visible_box": [8, 8, 0],
    "variable_placeholders": "",
    "variable_placeholder_buttons": [],
    "unhandled_root_fields": {},
    "resolution": {"width": 64, "height": 64},
    "elements": elements,
    "outliner": outliner,
    "textures": [],
}

out_path = "pollera_referencia.bbmodel"
with open(out_path, "w", encoding="utf-8") as f:
    json.dump(bbmodel, f, indent=2)
print("escrito:", out_path, "elementos:", len(elements))
