p = 'src/main/java/com/femclothes/render/CuerpoGeometria.java'
s = open(p, encoding='utf-8').read()

a = s.index("    /**\n     * Cuánto se INFLA cada una de las 12 filas de la pierna")
b = s.index("    /**\n     * El CUERPO de UNA parte, partido en tramos de fila")
new = '''    /**
     * Perfil de la pierna con volumen (2026-09-26, "en vez de aditivo,
     * sustractivo"): el MUSLO es la parte más ancha —la caja vanilla
     * ensanchada— y desde ahí se va RESTANDO: la rodilla se afina, la
     * pantorrilla queda algo más llena, el tobillo se afina de nuevo. Cada
     * valor es la dilatación de esa fila respecto de la base de la tela, en
     * unidades de modelo (px de skin); negativo = más fina que la caja
     * vanilla. Fila 0 = cadera, fila 11 = tobillo/pie.
     */
    private static final float[] PERFIL_PIERNA = {
            0.35F, 0.55F, 0.58F, 0.48F, 0.30F, 0.05F, -0.25F, -0.15F, -0.05F, -0.08F, -0.35F, -0.25F};

    /**
     * Cuánto se marca el perfil según el calce: cuanto MÁS ajustada la
     * media, MÁS se marca (tela que aprieta = carne que abulta; a pedido).
     * Ojo: el calce NORMAL dilata 0.02 (no los 0.32 fijos de TELA), ajustado
     * -0.04, pegado ~0, suelto 0.15, oversize 0.30: normal = 1, ajustado/
     * pegado más, suelto/oversize cada vez menos.
     */
    private static float factorVolumen(float dilatacionBase) {
        return Math.max(0.25F, Math.min(1.7F, 1.0F + (0.02F - dilatacionBase) / 0.06F * 0.6F));
    }

    private static float dilTelaFila(float dilatacionBase, int fila) {
        return dilatacionBase + PERFIL_PIERNA[fila] * factorVolumen(dilatacionBase);
    }

    private static final Map<String, ModelPart> RAICES_TELA_VOLUMEN = new java.util.HashMap<>();
    private static final Map<String, ModelPart> RAICES_CUERPO_VOLUMEN = new java.util.HashMap<>();

    /** Cuánto queda la piel por DENTRO de la tela en cada fila (para no atravesarla). */
    private static final float MARGEN_PIEL = 0.04F;

    /**
     * La TELA de una pierna partida en 12 filas, cada una con el ancho del
     * {@link #PERFIL_PIERNA} — misma técnica que {@link #cuerpoSegmentado} pero
     * a la escala de la tela. El grosor es solo en x/z (cada fila mantiene su
     * altura exacta, así el perfil puede ser negativo sin colapsar). La tapa
     * de arriba solo en la fila 0; la de abajo va aparte, con la caja entera
     * (su franja de UV no corre con la fila); y una PUNTA del pie hacia
     * adelante — la pierna vanilla no tiene pie, es una caja recta.
     */
    public static ModelPart telaConVolumenDePierna(Parte parte, float dilatacionBase) {
        String key = parte.clave() + "|" + dilatacionBase;
        ModelPart cacheada = RAICES_TELA_VOLUMEN.get(key);
        if (cacheada != null) return cacheada;

        final int S = Superficie.TELA.escala;
        float texW = LayoutSkin.LADO * S;
        int uvX = parte == Parte.PIERNA_DER ? 0 : 16 * S;
        int uvY = parte == Parte.PIERNA_DER ? 16 * S : 48 * S;
        java.util.List<ModelPart.Cuboid> cuboides = new java.util.ArrayList<>();
        for (int fila = 0; fila < 12; fila++) {
            java.util.EnumSet<net.minecraft.util.math.Direction> caras =
                    java.util.EnumSet.allOf(net.minecraft.util.math.Direction.class);
            caras.remove(net.minecraft.util.math.Direction.UP);
            if (fila != 0) caras.remove(net.minecraft.util.math.Direction.DOWN);
            float dil = dilTelaFila(dilatacionBase, fila) * S;
            cuboides.add(new ModelPart.Cuboid(
                    uvX, uvY + fila * S,
                    -2 * S, fila * S, -2 * S,
                    4 * S, S, 4 * S,
                    dil, 0F, dil,
                    false, texW, texW, caras));
        }
        float dilPie = dilTelaFila(dilatacionBase, 11) * S;
        // tapa de abajo (la planta): caja entera, solo esa cara
        cuboides.add(new ModelPart.Cuboid(
                uvX, uvY,
                -2 * S, 0, -2 * S,
                4 * S, 12 * S, 4 * S,
                dilPie, 0F, dilPie,
                false, texW, texW, java.util.EnumSet.of(net.minecraft.util.math.Direction.UP)));
        // Punta del pie: 3px hacia adelante, últimas 2 filas. Su UV se acomoda para que la
        // cara de adelante caiga justo en las filas 10-11 de la cara delantera de la pierna
        // y los costados en el borde delantero de los laterales (continuidad de la tela).
        int prof = 3 * S;
        cuboides.add(new ModelPart.Cuboid(
                uvX + 4 * S - prof, uvY + 14 * S - prof,
                -2 * S, 10 * S, -2 * S - prof,
                4 * S, 2 * S, prof,
                dilPie, 0F, dilPie,
                false, texW, texW, java.util.EnumSet.complementOf(java.util.EnumSet.of(net.minecraft.util.math.Direction.UP))));
        ModelPart resultado = new ModelPart(cuboides, java.util.Map.of());
        RAICES_TELA_VOLUMEN.put(key, resultado);
        return resultado;
    }

    /**
     * La PIEL de la pierna con la misma forma que la tela de arriba (apenas
     * por dentro) en las filas [desde, hasta) donde hay tela — "el muslo
     * tiene que incluir la piel, sino veo una media embolsada con el pie
     * vanilla flotando en el medio". Fuera de esas filas queda la caja normal.
     */
    public static ModelPart cuerpoConVolumenDePierna(Parte parte, float dilatacionBase, int desde, int hasta) {
        String key = parte.clave() + "|" + dilatacionBase + "|" + desde + "|" + hasta;
        ModelPart cacheada = RAICES_CUERPO_VOLUMEN.get(key);
        if (cacheada != null) return cacheada;

        PlantillaCuerpo t = PLANTILLAS_CUERPO.get(parte.clave());
        final int S = Superficie.CUERPO.escala;
        float texW = LayoutSkin.LADO * S;
        java.util.List<ModelPart.Cuboid> cuboides = new java.util.ArrayList<>();
        for (int fila = 0; fila < 12; fila++) {
            java.util.EnumSet<net.minecraft.util.math.Direction> caras =
                    java.util.EnumSet.allOf(net.minecraft.util.math.Direction.class);
            caras.remove(net.minecraft.util.math.Direction.UP);
            if (fila != 0) caras.remove(net.minecraft.util.math.Direction.DOWN);
            float dil = (fila >= desde && fila < hasta)
                    ? (dilTelaFila(dilatacionBase, fila) - MARGEN_PIEL) * S : 0F;
            cuboides.add(new ModelPart.Cuboid(
                    t.uvX(), t.uvY() + fila * S,
                    t.originX(), t.originY() + fila * S, t.originZ(),
                    t.sizeX(), S, t.sizeZ(),
                    dil, 0F, dil,
                    false, texW, texW, caras));
        }
        ModelPart resultado = new ModelPart(cuboides, java.util.Map.of());
        RAICES_CUERPO_VOLUMEN.put(key, resultado);
        return resultado;
    }

'''
s = s[:a] + new + s[b:]
open(p, 'w', encoding='utf-8').write(s)

p = 'src/main/java/com/femclothes/render/GarmentFeatureRenderer.java'
s = open(p, encoding='utf-8').read()


def rep(old, new):
    global s
    assert old in s, old[:80]
    s = s.replace(old, new, 1)


rep("""            if (llevaCuerpo) {
                List<CuerpoGeometria.SegmentoCuerpo> segmentos = segmentosCuerpo(piezas);
                if (parte == Parte.TORSO) {""", """            if (piezas != null) piezas.sort(Comparator.comparingInt(Pieza::capa));
            // Medias con volumen: solo si son la tela de más arriba de esa pierna (si hay un
            // pantalón encima, el volumen asomaría a través de él).
            Pieza conVolumen = null;
            if ((parte == Parte.PIERNA_DER || parte == Parte.PIERNA_IZQ) && piezas != null && !piezas.isEmpty()
                    && piezas.get(piezas.size() - 1).volumenPierna()) {
                conVolumen = piezas.get(piezas.size() - 1);
            }

            if (llevaCuerpo) {
                List<CuerpoGeometria.SegmentoCuerpo> segmentos = segmentosCuerpo(piezas);
                if (conVolumen != null) {
                    dibujarModelPart(CuerpoGeometria.cuerpoConVolumenDePierna(parte, conVolumen.dilatacion(),
                                    conVolumen.filaDesde(), conVolumen.filaHasta()),
                            CuerpoGeometria.Superficie.CUERPO, cuerpo, delJugador, matrices, vertexConsumers, luz);
                } else if (parte == Parte.TORSO) {""")
rep("""            piezas.sort(Comparator.comparingInt(Pieza::capa));
            for (int k = 0; k < piezas.size(); k++) {
                Pieza pieza = piezas.get(k);
                boolean pierna = parte == Parte.PIERNA_DER || parte == Parte.PIERNA_IZQ;
                // Muslos y pantorrillas de las medias (2026-09-26): SOLO si son la
                // tela de más arriba en esa pierna — si hay un pantalón encima, el
                // volumen asomaría a través de él (toda la tela va a la misma dilatación).
                if (pierna && pieza.volumenPierna() && k == piezas.size() - 1) {""",
    """            for (Pieza pieza : piezas) {
                if (pieza == conVolumen) {""")
open(p, 'w', encoding='utf-8').write(s)
print('ok')
