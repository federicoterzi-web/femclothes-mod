p = 'src/main/java/com/femclothes/modelado/ModeladoBlockEntity.java'
s = open(p, encoding='utf-8').read()


def rep(old, new, cnt=1):
    global s
    assert old in s, old[:80]
    s = s.replace(old, new, cnt)


# constants
a = s.index("    public static final int PINES_INICIO = SALIDA + 1;")
b = s.index("    public static final int TAMANO = PINES_FIN;")
s = s[:a] + '''    public static final int PINES_INICIO = SALIDA + 1;
    /** 8 pines por categoría (la remera ocupa los primeros 8, así las partidas viejas no se corren). */
    public static final int PINES_POR_CATEGORIA = 8;
    public static final int PINES_TAMANO = PINES_POR_CATEGORIA * 4;
    public static final int PINES_FIN = PINES_INICIO + PINES_TAMANO;
''' + s[b:]

rep("    public enum Categoria { REMERA, PANTALON, MEDIAS, CALIENTABRAZOS }\n", '''    public enum Categoria { REMERA, PANTALON, MEDIAS, CALIENTABRAZOS }

    /** Qué hace cada pin del esquema (2026-09-26, pines para las 4 prendas). NINGUNO = pin sin usar en esa categoría. */
    public enum Rol { CUELLO, MAT1, MAT2, MAT3, MANGA_IZQ, MANGA_DER, CALCE, TORSO, TIRO, BOTA_IZQ, BOTA_DER,
        SUP_IZQ, SUP_DER, INF_IZQ, INF_DER, NINGUNO }

    /** Rol de cada uno de los 8 pines por categoría — MISMO orden que {@code ModeladoScreenHandler#PIN_POS}. */
    public static final Rol[][] ROLES = {
            {Rol.CUELLO, Rol.MAT1, Rol.MAT2, Rol.MANGA_IZQ, Rol.MAT3, Rol.MANGA_DER, Rol.CALCE, Rol.TORSO},
            {Rol.TIRO, Rol.MAT1, Rol.MAT2, Rol.MAT3, Rol.CALCE, Rol.BOTA_IZQ, Rol.BOTA_DER, Rol.NINGUNO},
            {Rol.SUP_IZQ, Rol.SUP_DER, Rol.MAT1, Rol.MAT2, Rol.MAT3, Rol.CALCE, Rol.INF_IZQ, Rol.INF_DER},
            {Rol.SUP_IZQ, Rol.MAT1, Rol.SUP_DER, Rol.MAT2, Rol.MAT3, Rol.INF_IZQ, Rol.INF_DER, Rol.CALCE},
    };
''')

a = s.index("    /**\n     * El combo que representa {@code molde} puesto en el pin {@code idx}")
b = s.index("    // Estado por pin (2026-09-24")
s = s[:a] + '''    /**
     * El combo que representa {@code molde} puesto en el pin {@code i} de la
     * categoría {@code cat}, o null si ese pin no acepta ese ítem.
     * {@code lado}: el lado que aplica un pin de lado (con simetría activa
     * se pasa AMBAS sin importar en cuál de los dos se soltó).
     */
    private static ComboCorte comboDePin(Categoria cat, int i, ItemStack molde, Lado lado) {
        Rol rol = ROLES[cat.ordinal()][i];
        Item item = molde.getItem();
        ComboCorte c = null;
        switch (rol) {
            case CUELLO -> {
                if (item instanceof MoldeCuelloItem m) c = ComboCorte.remeraCuello(m.valor);
            }
            case MAT1, MAT2, MAT3 -> {
                // Medias de red/calado (2026-09-25): un solo eje, cualquiera
                // de los 3 pines la acepta. Los patrones son capas apiladas.
                if (item instanceof MoldeRedItem m) c = ComboCorte.red(m.valor);
                else if (item instanceof ClothingPatternItem m) {
                    c = ComboCorte.capaPatron(rol == Rol.MAT1 ? 0 : rol == Rol.MAT2 ? 1 : 2, m.patternId);
                }
            }
            case MANGA_IZQ, MANGA_DER -> {
                if (item instanceof MoldeRangoItem m) c = ComboCorte.remeraManga(mangaRemeraDeRango(m.rango), lado);
            }
            case CALCE -> {
                if (item instanceof MoldeCalceItem m) c = ComboCorte.calce(m.valor);
            }
            case TORSO -> {
                if (item instanceof MoldeLargoRemeraItem m) c = ComboCorte.remeraLargo(m.valor);
                else if (item instanceof MoldeTorsoItem m) c = ComboCorte.remeraLargo(largoDeRango(m.rango));
            }
            case TIRO -> {
                if (item instanceof MoldeTiroItem m) c = ComboCorte.tiro(m.valor);
                else if (item instanceof MoldeTorsoItem m) c = ComboCorte.tiro(tiroDeRango(m.rango));
            }
            case BOTA_IZQ, BOTA_DER -> {
                Botamanga v = null;
                if (item instanceof MoldePantalonItem m) v = m.valor;
                else if (item instanceof MoldeRangoItem m) v = pantalonDeRango(m.rango);
                if (v != null) c = ComboCorte.pantalonInferior(v, lado);
            }
            case SUP_IZQ, SUP_DER, INF_IZQ, INF_DER -> {
                boolean sup = rol == Rol.SUP_IZQ || rol == Rol.SUP_DER;
                if (cat == Categoria.MEDIAS) {
                    Botamanga v = null;
                    if (item instanceof MoldeMediaItem m) v = m.valor;
                    else if (item instanceof MoldeRangoItem m) v = mediasDeRango(m.rango);
                    if (v != null) c = sup ? ComboCorte.mediasSuperior(v, lado) : ComboCorte.mediasInferior(v, lado);
                } else if (cat == Categoria.CALIENTABRAZOS && item instanceof MoldeRangoItem m) {
                    Variante.Manga v = mangaCalientabrazosDeRango(m.rango);
                    c = sup ? ComboCorte.calientabrazosSuperior(v, lado) : ComboCorte.calientabrazosInferior(v, lado);
                }
            }
            default -> { }
        }
        return c == null ? null : c.conIcono(net.minecraft.registry.Registries.ITEM.getId(item));
    }

    /** El lado que le toca a un pin de lado; con simetría activa (o en pines sin lado) es AMBAS. */
    private Lado ladoDePin(Categoria cat, int i) {
        if (remeraSimetria) return Lado.AMBAS;
        return switch (ROLES[cat.ordinal()][i]) {
            case MANGA_IZQ, BOTA_IZQ, SUP_IZQ, INF_IZQ -> Lado.IZQUIERDA;
            case MANGA_DER, BOTA_DER, SUP_DER, INF_DER -> Lado.DERECHA;
            default -> Lado.AMBAS;
        };
    }

    public boolean remeraSimetria() { return remeraSimetria; }

    /** ¿Acepta este pin este ítem? (independiente de la categoría activa/encendida — eso lo chequea {@link #isValid}). */
    public static boolean pinAcepta(Categoria cat, int i, ItemStack molde) {
        return comboDePin(cat, i, molde, Lado.AMBAS) != null;
    }

''' + s[b:]

rep('''        if (world == null || world.isClient) return;
        List<ComboCorte> lista = fijadasPorCategoria.get(Categoria.REMERA);''',
    '''        if (world == null || world.isClient) return;
        Categoria cat = Categoria.values()[idx / PINES_POR_CATEGORIA];
        List<ComboCorte> lista = fijadasPorCategoria.get(cat);''')
rep('''            Lado lado = remeraSimetria ? Lado.AMBAS : ladoDePin(idx);
            ComboCorte c = comboDePin(idx, nuevo, lado);''',
    '''            int i = idx % PINES_POR_CATEGORIA;
            ComboCorte c = comboDePin(cat, i, nuevo, ladoDePin(cat, i));''')
rep('''        if (idx < 0 || idx >= PINES_TAMANO || categoria != Categoria.REMERA) return false;
        int slot = PINES_INICIO + idx;
        ItemStack molde = items.get(slot);
        List<ComboCorte> lista = fijadasPorCategoria.get(Categoria.REMERA);''',
    '''        if (idx < 0 || idx >= PINES_TAMANO || idx / PINES_POR_CATEGORIA != categoria.ordinal()) return false;
        int slot = PINES_INICIO + idx;
        ItemStack molde = items.get(slot);
        List<ComboCorte> lista = fijadasPorCategoria.get(categoria);''')
rep("if (!guardarMolde(molde.copyWithCount(1))) return false;", "if (!guardarMolde(categoria, molde.copyWithCount(1))) return false;")
rep("    private boolean guardarMolde(ItemStack molde) {", "    private boolean guardarMolde(Categoria cat, ItemStack molde) {")
rep('''        } else if (esMoldeExclusivoDe(molde, Categoria.REMERA)) {
            desde = porPrendaInicio(Categoria.REMERA);''', '''        } else if (esMoldeExclusivoDe(molde, cat)) {
            desde = porPrendaInicio(cat);''')
rep('''    private void soltarPinDeFijada(ComboCorte quitada) {
        for (int i = 0; i < PINES_TAMANO; i++) {''',
    '''    private void soltarPinDeFijada(Categoria cat, ComboCorte quitada) {
        for (int i = cat.ordinal() * PINES_POR_CATEGORIA; i < (cat.ordinal() + 1) * PINES_POR_CATEGORIA; i++) {''')
rep("if (categoria == Categoria.REMERA) soltarPinDeFijada(quitada);", "soltarPinDeFijada(categoria, quitada);")
rep('''            return categoria == Categoria.REMERA && pinAcepta(slot - PINES_INICIO, stack);''',
    '''            int p = slot - PINES_INICIO;
            Categoria cat = Categoria.values()[p / PINES_POR_CATEGORIA];
            return categoria == cat && pinAcepta(cat, p % PINES_POR_CATEGORIA, stack);''')
# Activo: sin uso para ninguna categoría (los pines lo reemplazan)
rep('''            if (cat == Categoria.REMERA) return false;
            return esMoldeDeCategoria(stack, cat);''', '''            if (cat != null) return false;
            return esMoldeDeCategoria(stack, cat);''')
open(p, 'w', encoding='utf-8').write(s)
print('ok')
