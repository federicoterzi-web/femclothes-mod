import json, re
pos = json.load(open('.scratch_mpm/posiciones.json'))
orden = ['remera', 'pantalon', 'medias', 'calientabrazos']
P, B = [], []
for n in orden:
    W, H = pos[n]['size']
    sx, sy = 240.0 / W, 136.0 / H
    ps, bs = [], []
    for (cx, cy) in pos[n]['slots']:
        lx, ly = cx * sx, 36 + cy * sy
        ps.append('{%d, %d}' % (round(lx - 8), round(ly - 8)))
        bs.append('{%d, %d}' % (round(lx + 9), round(ly - 9)))
    while len(ps) < 12:
        ps.append('{0, 0}')
        bs.append('{0, 0}')
    P.append('            {' + ', '.join(ps) + '}')
    B.append('            {' + ', '.join(bs) + '}')

p = 'src/main/java/com/femclothes/modelado/ModeladoScreenHandler.java'
s = open(p, encoding='utf-8').read()
for name, rows in (('PIN_POS', P), ('PIN_BTN', B)):
    a = s.index('public static final int[][][] %s = {' % name)
    b = s.index('    };\n', a) + len('    };\n')
    s = s[:a] + 'public static final int[][][] %s = {\n' % name + ',\n'.join(rows) + ',\n    };\n' + s[b:]
open(p, 'w', encoding='utf-8').write(s)
print('\n'.join(P))
