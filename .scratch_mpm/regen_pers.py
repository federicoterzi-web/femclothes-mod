import re


def edit(path, fn):
    s = open(path, encoding='utf-8').read()
    s = fn(s)
    open(path, 'w', encoding='utf-8').write(s)


def rep(s, old, new, cnt=1):
    assert old in s, old[:90]
    return s.replace(old, new, cnt)


# ---------- ComboCorte: capaPatron con lado
def combo(s):
    return rep(s, '''    public static ComboCorte capaPatron(int indice, Identifier patronId) {
        return new ComboCorte(''', '''    public static ComboCorte capaPatron(int indice, Identifier patronId) {
        return capaPatron(indice, patronId, Lado.AMBAS);
    }

    /** Personalización por lado (medias/calientabrazos, 2026-09-26): la capa va solo a la pierna/brazo de {@code lado}. */
    public static ComboCorte capaPatron(int indice, Identifier patronId, Lado lado) {
        return new ComboCorte(''')


edit('src/main/java/com/femclothes/modelado/ComboCorte.java', lambda s: rep(combo(s), '''                Optional.of(new CapaIndexada(indice, patronId)),
                Lado.AMBAS, Optional.empty());''', '''                Optional.of(new CapaIndexada(indice, patronId)),
                lado, Optional.empty());'''))


# ---------- PrendaModelado: la capa respeta el lado
def prenda(s):
    s = rep(s, "capasAplicadas(out, com.femclothes.region.Lado.AMBAS));", "capasAplicadas(out, lado));")
    s = rep(s, "com.femclothes.region.RegionResolver.ponerPatron(out, com.femclothes.region.Lado.AMBAS, actuales);", "com.femclothes.region.RegionResolver.ponerPatron(out, lado, actuales);")
    return s


edit('src/main/java/com/femclothes/modelado/PrendaModelado.java', prenda)


# ---------- BlockEntity: 12 pines por categoría + roles de personalización por lado
def be(s):
    s = rep(s, "public static final int PINES_POR_CATEGORIA = 8;", "public static final int PINES_POR_CATEGORIA = 12;")
    s = rep(s, "    /** 8 pines por categoría (la remera", "    /** 12 pines por categoría (la remera")
    s = rep(s, "SUP_IZQ, SUP_DER, INF_IZQ, INF_DER, NINGUNO }", "SUP_IZQ, SUP_DER, INF_IZQ, INF_DER,\n        PERS_IZQ1, PERS_IZQ2, PERS_IZQ3, PERS_DER1, PERS_DER2, PERS_DER3, NINGUNO }")
    a = s.index("    public static final Rol[][] ROLES = {")
    b = s.index("    };\n", a) + len("    };\n")
    s = s[:a] + '''    public static final Rol[][] ROLES = {
            {Rol.CUELLO, Rol.MAT1, Rol.MAT2, Rol.MANGA_IZQ, Rol.MAT3, Rol.MANGA_DER, Rol.CALCE, Rol.TORSO,
                    Rol.NINGUNO, Rol.NINGUNO, Rol.NINGUNO, Rol.NINGUNO},
            {Rol.TIRO, Rol.MAT1, Rol.MAT2, Rol.MAT3, Rol.CALCE, Rol.BOTA_IZQ, Rol.BOTA_DER, Rol.NINGUNO,
                    Rol.NINGUNO, Rol.NINGUNO, Rol.NINGUNO, Rol.NINGUNO},
            {Rol.SUP_IZQ, Rol.SUP_DER, Rol.INF_IZQ, Rol.INF_DER, Rol.CALCE, Rol.PERS_IZQ1, Rol.PERS_IZQ2,
                    Rol.PERS_IZQ3, Rol.PERS_DER1, Rol.PERS_DER2, Rol.PERS_DER3, Rol.NINGUNO},
            {Rol.SUP_IZQ, Rol.SUP_DER, Rol.INF_IZQ, Rol.INF_DER, Rol.CALCE, Rol.PERS_IZQ1, Rol.PERS_IZQ2,
                    Rol.PERS_IZQ3, Rol.PERS_DER1, Rol.PERS_DER2, Rol.PERS_DER3, Rol.NINGUNO},
    };
''' + s[b:]
    s = rep(s, '''            case MANGA_IZQ, MANGA_DER -> {
                if (item instanceof MoldeRangoItem m)''', '''            case PERS_IZQ1, PERS_IZQ2, PERS_IZQ3, PERS_DER1, PERS_DER2, PERS_DER3 -> {
                int capa = rol == Rol.PERS_IZQ1 || rol == Rol.PERS_DER1 ? 0
                        : rol == Rol.PERS_IZQ2 || rol == Rol.PERS_DER2 ? 1 : 2;
                if (item instanceof MoldeRedItem m) c = ComboCorte.red(m.valor);
                else if (item instanceof ClothingPatternItem m) c = ComboCorte.capaPatron(capa, m.patternId, lado);
            }
            case MANGA_IZQ, MANGA_DER -> {
                if (item instanceof MoldeRangoItem m)''')
    s = rep(s, "            case MANGA_IZQ, BOTA_IZQ, SUP_IZQ, INF_IZQ -> Lado.IZQUIERDA;\n            case MANGA_DER, BOTA_DER, SUP_DER, INF_DER -> Lado.DERECHA;",
            "            case MANGA_IZQ, BOTA_IZQ, SUP_IZQ, INF_IZQ, PERS_IZQ1, PERS_IZQ2, PERS_IZQ3 -> Lado.IZQUIERDA;\n            case MANGA_DER, BOTA_DER, SUP_DER, INF_DER, PERS_DER1, PERS_DER2, PERS_DER3 -> Lado.DERECHA;")
    return s


edit('src/main/java/com/femclothes/modelado/ModeladoBlockEntity.java', be)


# ---------- Handler: tablas a 12 entradas
def pad(row, n=12):
    items = re.findall(r'\{\d+, \d+\}', row)
    items += ['{0, 0}'] * (n - len(items))
    return '{' + ', '.join(items) + '}'


def handler(s):
    for name in ('PIN_POS', 'PIN_BTN'):
        a = s.index('public static final int[][][] %s = {' % name)
        b = s.index('    };\n', a) + len('    };\n')
        block = s[a:b]
        rows = re.findall(r'\{\{.*?\}\}', block)
        s = s[:a] + 'public static final int[][][] %s = {\n' % name + ',\n'.join('            ' + pad(r) for r in rows) + ',\n    };\n' + s[b:]
    return s


edit('src/main/java/com/femclothes/modelado/ModeladoScreenHandler.java', handler)
print('ok')
