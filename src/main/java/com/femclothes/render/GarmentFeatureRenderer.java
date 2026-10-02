package com.femclothes.render;

import com.femclothes.body.PerfilCuerpo;
import com.femclothes.body.PerfilesDeCuerpo;
import com.femclothes.garment.Capa;
import com.femclothes.garment.Garment;
import com.femclothes.garment.Garments;
import com.femclothes.garment.Parte;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Dibuja TODA la ropa del mod, en orden, desde un solo lugar.
 *
 * Antes cada prenda se registraba como TrinketRenderer y se dibujaba sola.
 * Eso alcanzaba mientras hubiera una prenda por parte del cuerpo, pero el
 * orden lo decidia Trinkets segun el order de los slots, no nosotros: no
 * habia forma de decir "el short va arriba de la media". El ordinal de capa
 * necesita un unico punto de dibujo, y este es.
 *
 * Se engancha con LivingEntityFeatureRendererRegistrationCallback, que es un
 * hook publico de Fabric API — sin Mixins, igual que el resto del mod.
 *
 * <h2>El orden</h2>
 * <ol>
 *   <li>el CUERPO BASE, una sola vez por parte del cuerpo, dilatado 0.30;</li>
 *   <li>las piezas de esa parte ordenadas por {@link Capa}, todas a 0.32.</li>
 * </ol>
 * Donde una prenda tiene tela es opaca y gana; donde no, se ve la capa de
 * abajo o el cuerpo. De yapa esto arregla el midriff: una musculosa deja ver
 * la panza sin que la prenda tenga que rellenarla ella misma.
 *
 * <h2>El cuerpo base va donde la prenda MANDA, no donde tiene tela</h2>
 * Las partes del sustrato salen de {@link Garment#partes} y no de las piezas
 * dibujadas. La diferencia importa para una musculosa: no dibuja nada en los
 * brazos, pero igual tiene que taparle las mangas pintadas de la skin, asi
 * que declara los brazos como suyos y ahi va el cuerpo.
 */
public class GarmentFeatureRenderer<T extends LivingEntity, M extends EntityModel<T>>
        extends FeatureRenderer<T, M> {

    /**
     * Hook del visor 3D de la Mesa de Modelado: cuando no es null, se usa
     * ESTA lista en vez de leer Trinkets real — permite previsualizar una
     * prenda en construcción (fijadas aplicadas sobre el slot PRENDA, ver
     * {@code ModeladoBlockEntity#previsualizar}) sin que el jugador se la
     * tenga puesta encima. Se setea y limpia en el mismo frame, alrededor
     * de un único {@code InventoryScreen.drawEntity}, así que nunca afecta
     * el render normal del jugador en el mundo.
     */
    @Nullable
    public static List<ItemStack> previewOverride;

    /**
     * Click en la vista 3D de la Mesa de estilado (2026-10-01): si no es null,
     * el próximo {@link #render} guarda acá, por parte, la matriz que lleva del
     * espacio local de esa parte (en bloques, como los cuboides de ModelPart)
     * a coordenadas de pantalla de la GUI. Se setea y se lee en el mismo frame.
     */
    @Nullable
    public static Map<Parte, org.joml.Matrix4f> capturaPoses;

    public GarmentFeatureRenderer(FeatureRendererContext<T, M> contexto) {
        super(contexto);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int luz,
                       T entidad, float limbAngle, float limbDistance, float tickDelta,
                       float animationProgress, float headYaw, float headPitch) {

        if (!(getContextModel() instanceof BipedEntityModel<?> biped)) return;

        List<ItemStack> prendas = previewOverride != null ? previewOverride : equipadas(entidad);
        if (capturaPoses != null) {
            for (Parte parte : Parte.values()) {
                ModelPart p = CuerpoGeometria.delJugador(biped, parte);
                org.joml.Matrix4f m = new org.joml.Matrix4f(matrices.peek().getPositionMatrix());
                m.translate(p.pivotX / 16f, p.pivotY / 16f, p.pivotZ / 16f);
                m.rotate(new org.joml.Quaternionf().rotationZYX(p.roll, p.yaw, p.pitch));
                capturaPoses.put(parte, m);
            }
        }
        // En la vista previa de elegir cuerpo, el cuerpo va entero aunque no haya ropa.
        // Cuerpo entero: la vista previa de elegir cuerpo, o un perfil que usa
        // el cuerpo como skin aunque no haya ropa (2026-09-29, "un selector que
        // directamente te deje esa skin de default").
        boolean cuerpoEntero = perfilOverride != null || perfilDe(entidad).siempre();
        // Busto (2026-10-01, estilo Only Jugs): uno por dibujo, con su rebote.
        prepararBusto(entidad, tickDelta);
        if (prendas.isEmpty() && !cuerpoEntero) {
            dibujarBustoEnLaSkin(entidad, biped, matrices, vertexConsumers, luz);
            com.femclothes.render.relieve.BustoRender.actual = null;
            return;
        }

        boolean slim = esSlim(entidad);

        // Las partes que alguna prenda gobierna: ahi va el cuerpo base.
        List<Parte> conCuerpo = cuerpoEntero ? List.of(Parte.values()) : Garments.partesCubiertas(prendas);

        // Las piezas de tela, agrupadas por parte. EnumMap para que el
        // recorrido sea estable: dos frames no pueden dibujar en distinto
        // orden dos piezas de la misma capa.
        Map<Parte, List<Pieza>> porParte = new EnumMap<>(Parte.class);
        // De qué prenda sale cada pieza: el relieve mira su calce, su textura y su archivo de extrusión.
        Map<Pieza, ItemStack> origen = new java.util.IdentityHashMap<>();
        for (ItemStack stack : prendas) {
            for (Pieza pieza : PiezasDePrenda.de(stack, entidad)) {
                porParte.computeIfAbsent(pieza.parte(), k -> new ArrayList<>()).add(pieza);
                origen.put(pieza, stack);
            }
        }
        // La capa no gobierna ninguna parte: se dibuja antes del corte de abajo.
        dibujarCapa(prendas, entidad, biped, matrices, vertexConsumers, luz, tickDelta, false);
        if (conCuerpo.isEmpty() && porParte.isEmpty()) {
            dibujarBustoEnLaSkin(entidad, biped, matrices, vertexConsumers, luz);
            com.femclothes.render.relieve.BustoRender.actual = null;
            dibujarCapa(prendas, entidad, biped, matrices, vertexConsumers, luz, tickDelta, true);
            return;
        }

        Identifier cuerpo = texturaDelCuerpo(entidad, slim);
        // Relieve del cuerpo (2026-10-01): músculos, curvas y busto del perfil.
        com.femclothes.render.relieve.MapaRelieve mapaCuerpo =
                com.femclothes.render.relieve.RelieveRender.estilo == com.femclothes.render.relieve.RelieveRender.Estilo.APAGADO
                        ? com.femclothes.render.relieve.MapaRelieve.PLANO
                        : com.femclothes.render.relieve.RelieveCuerpo.de(perfilDe(entidad), slim);
        // El torso lo dibuja vanilla (ninguna prenda lo gobierna): el busto va con la piel de la skin.
        if (!conCuerpo.contains(Parte.TORSO)) dibujarBustoEnLaSkin(entidad, biped, matrices, vertexConsumers, luz);

        for (Parte parte : Parte.values()) {
            boolean llevaCuerpo = cuerpo != null && conCuerpo.contains(parte)
                    // La cabeza nunca lleva cuerpo base: es la cara del
                    // jugador, y taparla con un tono plano le borra los ojos.
                    && parte != Parte.CABEZA;
            List<Pieza> piezas = porParte.get(parte);
            if (!llevaCuerpo && piezas == null) continue;

            ModelPart delJugador = CuerpoGeometria.delJugador(biped, parte);
            if (!delJugador.visible) continue;

            if (piezas != null) piezas.sort(Comparator.comparingInt(Pieza::capa));
            // Medias con volumen: solo si son la tela de más arriba de esa pierna (si hay un
            // pantalón encima, el volumen asomaría a través de él).
            Pieza conVolumen = null;
            if ((parte == Parte.PIERNA_DER || parte == Parte.PIERNA_IZQ) && piezas != null && !piezas.isEmpty()
                    && piezas.get(piezas.size() - 1).volumenPierna()) {
                conVolumen = piezas.get(piezas.size() - 1);
            }

            if (llevaCuerpo && parte == Parte.TORSO) {
                dibujarBusto(cuerpo, delJugador, infladoBustoCuerpo(piezas), 0f, 20f, matrices, vertexConsumers, luz);
            }
            if (llevaCuerpo) {
                com.femclothes.render.relieve.RelieveRender.actual =
                        new com.femclothes.render.relieve.RelieveRender.Contexto(parte, mapaCuerpo);
                List<CuerpoGeometria.SegmentoCuerpo> segmentos = segmentosCuerpo(piezas);
                if (conVolumen != null) {
                    dibujarModelPart(CuerpoGeometria.cuerpoConVolumenDePierna(parte, conVolumen.dilatacion(),
                                    conVolumen.filaDesde(), conVolumen.filaHasta()),
                            CuerpoGeometria.Superficie.CUERPO, cuerpo, delJugador, matrices, vertexConsumers, luz);
                } else if (parte == Parte.TORSO) {
                    dibujarCuerpoTorso(entidad, slim, cuerpo, segmentos, delJugador, matrices, vertexConsumers, luz);
                } else if (segmentos == null) {
                    dibujar(CuerpoGeometria.Superficie.CUERPO, parte, slim, cuerpo,
                            CuerpoGeometria.Superficie.CUERPO.dilatacion, delJugador, matrices, vertexConsumers, luz);
                } else {
                    ModelPart nuestra = CuerpoGeometria.cuerpoSegmentado(parte, slim, segmentos);
                    dibujarModelPart(nuestra, CuerpoGeometria.Superficie.CUERPO, cuerpo,
                            delJugador, matrices, vertexConsumers, luz);
                }
            }
            com.femclothes.render.relieve.RelieveRender.actual = null;
            if (piezas == null) continue;
            dibujarPiezas(parte, slim, piezas, conVolumen, delJugador, matrices, vertexConsumers, luz,
                    llevaCuerpo ? mapaCuerpo : com.femclothes.render.relieve.MapaRelieve.PLANO, origen);
        }

        // Cuello de polera, capucha y cordones del hoodie (2026-09-30).
        CuelloYCapucha.dibujar(prendas, biped, matrices, vertexConsumers, luz);
        // Apliques de la Mesa de estilado (2026-10-01).
        ApliqueRenderer.dibujar(prendas, biped, matrices, vertexConsumers, luz);
        dibujarTranslucidas();
        com.femclothes.render.relieve.BustoRender.actual = null;
        dibujarPollera(prendas, entidad, biped, matrices, vertexConsumers, luz, tickDelta);
        dibujarCapa(prendas, entidad, biped, matrices, vertexConsumers, luz, tickDelta, true);
    }

    private static final Identifier POLLERA_BASE = Identifier.of("femclothes",
            "textures/models/armor/pollera_tela.png");

    /**
     * La pollera se dibuja APARTE del loop de {@link Pieza} de arriba —
     * su geometría no es una caja por {@link Parte} como el resto (ver
     * {@link PolleraGeometria}, paneles en abanico), así que no encaja en
     * {@code CuerpoGeometria.parte(...)}. Ancla en el pivote del TORSO —
     * mismo lugar de donde cuelga la banda de cintura del pantalón — y
     * usa la dilatación de {@code Superficie.CUERPO} (escala 1) para el
     * factor de escala final: la geometría de la pollera ya está en
     * unidades naturales, no hace falta el truco de 1/8 que usa
     * {@code TELA} para prendas estampadas.
     *
     * <p>Sin balanceo por ahora (2026-09-16): se probó un balanceo tipo
     * capa sobre la versión anterior (tubo de aritos) y "no gustaba" —
     * "forma primero, balanceo después" otra vez, hasta confirmar que esta
     * forma de paneles en abanico se ve bien parada quieta.
     */
    private static void dibujarPollera(List<ItemStack> prendas, LivingEntity entidad, BipedEntityModel<?> biped,
                                       MatrixStack matrices, VertexConsumerProvider vertexConsumers, int luz,
                                       float tickDelta) {
        ItemStack stack = prendas.stream()
                .filter(s -> s.getItem() instanceof com.femclothes.item.PolleraItem)
                .findFirst().orElse(null);
        if (stack == null) return;

        // La cintura de la pollera no puede apretar por dentro del cuerpo
        // (no es una Pieza: el cuerpo de abajo no se achica por ella) —
        // Ajustado pasó a -0.25 el 2026-09-30.
        float dilatacion = Math.max(0F, com.femclothes.item.Calce.dilatacionEfectiva(stack));
        Identifier textura = texturaPollera(stack);

        ModelPart delJugador = CuerpoGeometria.delJugador(biped, Parte.TORSO);
        if (!delJugador.visible) return;
        // Malla propia (2026-09-29, "resolveme la pollera que se ve horrible a
        // veces"): reemplaza a los gajos de PolleraGeometria. Se dibuja en el
        // marco del torso; las piernas van relativas al torso para que el
        // ruedo se abra donde pasan (ver PolleraMalla#piernasAbiertas).
        matrices.push();
        delJugador.rotate(matrices);
        VertexConsumer buffer = vertexConsumers.getBuffer(ClothingTextureCache.capaDeRender(textura));
        // Movimiento (2026-09-30, "habria que animarlas segun el movimiento"):
        // la misma inercia que usa la capa vanilla (ver CapaMalla#movimiento).
        CapaMalla.Movimiento mov = entidad instanceof AbstractClientPlayerEntity jugador && previewOverride == null
                ? CapaMalla.movimiento(jugador, tickDelta) : CapaMalla.Movimiento.QUIETO;
        PolleraMalla.dibujar(matrices, buffer, luz,
                com.femclothes.item.PolleraItem.forma(stack), com.femclothes.item.PolleraItem.largo(stack),
                dilatacion, new PolleraMalla.Piernas(biped.body, biped.rightLeg, biped.leftLeg), mov,
                // entidad null = Maniquí (dibujarTela): sin twirl.
                previewOverride == null && entidad != null
                        ? com.femclothes.client.TwirlCliente.progreso(entidad, tickDelta) : -1f);
        matrices.pop();
    }

    /**
     * La tela de la pollera ya teñida — separada de {@link #dibujarPollera}
     * (2026-09-29) para que el ícono del ítem ({@code IconoPrenda}) saque el
     * color del mismo lugar que la prenda puesta.
     */
    public static Identifier texturaPollera(ItemStack stack) {
        int colorBase = com.femclothes.region.RegionResolver.colorBase(stack, com.femclothes.region.Lado.IZQUIERDA);
        // Tela nueva (2026-09-29, "teñirla y sublimarla"): pollera_tela.png
        // tiene el layout de la caja del TORSO (ver PolleraMalla), así que
        // los patrones salen de PatronGenerador("pollera") = caja de torso,
        // las zonas de Tintes son las del torso, y las fotos y la red se
        // pintan encima como en las demás prendas.
        java.util.List<com.femclothes.region.RegionResolver.CapaPatron> capasPollera =
                com.femclothes.region.RegionResolver.capasTinte(stack, com.femclothes.region.Lado.IZQUIERDA);
        java.util.List<ClothingTextureCache.CapaMascara> capasMascaraPollera = new java.util.ArrayList<>(capasPollera.size());
        // Patrones desplegados según la forma y el largo (2026-10-01, ver PatronGenerador#desplegar).
        String clavePollera = com.femclothes.render.PatronGenerador.clavePollera(
                com.femclothes.item.PolleraItem.forma(stack), com.femclothes.item.PolleraItem.largo(stack));
        for (com.femclothes.region.RegionResolver.CapaPatron capa : capasPollera) {
            net.minecraft.client.texture.NativeImage mascara = com.femclothes.render.PatronGenerador.mascaraDeCapa(clavePollera, capa);
            if (!capa.lisa() && mascara == null) continue;
            java.util.List<CajaSkin.Rect> region = capa.region() == com.femclothes.region.RegionPintura.TODO ? null
                    : capa.region().rects(com.femclothes.tinturas.TinturasBlockEntity.Categoria.POLLERA, CuerpoGeometria.ESCALA_TELA);
            capasMascaraPollera.add(ClothingTextureCache.CapaMascara.de(capa, mascara, region));
        }
        com.femclothes.item.PatronRed red = com.femclothes.item.PatronRed.leer(stack);
        boolean estampada = com.femclothes.sublimadora.EstampaTextures.tieneEstampa(stack);
        ClothingTextureCache.Encima encima = !estampada && red == null ? null : new ClothingTextureCache.Encima() {
            @Override
            public String clave() {
                return "pollera_" + red + (estampada ? "_" + com.femclothes.sublimadora.EstampaTextures.claveEstampas(stack) : "");
            }

            @Override
            public boolean aplicar(net.minecraft.client.texture.NativeImage destino) {
                boolean listo = !estampada || com.femclothes.sublimadora.EstampaTextures.estampar(destino, stack);
                if (red != null) ClothingTextureCache.perforarRed(destino, red, Parte.TORSO, 0, 12);
                return listo;
            }
        };
        return ClothingTextureCache.composeGarmentCapas(POLLERA_BASE, colorBase, capasMascaraPollera,
                ClothingTextureCache.Shading.NONE, encima);
    }

    private static final Identifier CAPA_BASE = Identifier.of("femclothes",
            "textures/models/armor/capa_tela.png");

    /** La capa del mod puesta en el slot espalda/capa, o null. */
    @Nullable
    public static ItemStack capaDe(List<ItemStack> prendas) {
        for (ItemStack s : prendas) if (s.getItem() instanceof com.femclothes.item.CapaItem) return s;
        return null;
    }

    /**
     * La capa (2026-09-29, "capas... como las capas vanilla (misma dinamica
     * de tela) pero usable y personalizable"): mismo marco y mismos giros que
     * {@code CapeFeatureRenderer} (ver {@link CapaMalla#giros}), así que
     * flamea igual que la vanilla. Con élitros puestos no se dibuja (como la
     * vanilla); la capa vanilla se oculta mientras esté esta
     * ({@code CapeFeatureRendererMixin}).
     */
    private static void dibujarCapa(List<ItemStack> prendas, LivingEntity entidad, BipedEntityModel<?> biped,
                                    MatrixStack matrices,
                                    VertexConsumerProvider vertexConsumers, int luz, float tickDelta,
                                    boolean translucidas) {
        if (!(entidad instanceof AbstractClientPlayerEntity jugador)) return;
        ItemStack stack = capaDe(prendas);
        if (stack == null || jugador.isInvisible()) return;
        if (jugador.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST).isOf(net.minecraft.item.Items.ELYTRA)) return;

        Identifier textura = texturaCapa(stack);
        // Translúcida (2026-10-01): va al final, después del cuerpo — si se
        // dibuja antes, su profundidad tapa al cuerpo que queda detrás.
        if (ClothingTextureCache.esTranslucida(textura) != translucidas) return;
        VertexConsumer buffer = vertexConsumers.getBuffer(ClothingTextureCache.capaDeRender(textura));
        boolean agachado = jugador.isInSneakingPose();
        // Tela (2026-09-30, "es una placa tiesa"): sin giros de matriz; CapaMalla
        // dobla el paño tramo por tramo y lo hace chocar con las piernas.
        float sy = agachado ? 1.85f : 0f, sz = 2f + (agachado ? 1.4f : 0f);
        PolleraMalla.Piernas piernas = new PolleraMalla.Piernas(
                new org.joml.Matrix4f().translation(0f, sy, sz), biped.rightLeg, biped.leftLeg);
        matrices.push();
        matrices.translate(0f, sy / 16f, sz / 16f);
        CapaMalla.dibujarPano(matrices, buffer, luz, stack, CapaMalla.movimiento(jugador, tickDelta), agachado,
                jugador.age + tickDelta, piernas);
        matrices.pop();

        if (com.femclothes.item.CapaItem.cuelloAlto(stack)) {
            matrices.push();
            matrices.translate(0f, 0f, 0.125f);
            if (jugador.isInSneakingPose()) matrices.translate(0f, 1.85f / 16f, 1.4f / 16f);
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(180f));
            CapaMalla.dibujarCuello(matrices, buffer, luz);
            matrices.pop();
        }
    }

    /** La tela de la capa ya teñida y estampada (también la usa el ícono). */
    public static Identifier texturaCapa(ItemStack stack) {
        int colorBase = com.femclothes.region.RegionResolver.colorBase(stack, com.femclothes.region.Lado.IZQUIERDA);
        java.util.List<com.femclothes.region.RegionResolver.CapaPatron> capas =
                com.femclothes.region.RegionResolver.capasTinte(stack, com.femclothes.region.Lado.IZQUIERDA);
        java.util.List<ClothingTextureCache.CapaMascara> mascaras = new java.util.ArrayList<>(capas.size());
        for (com.femclothes.region.RegionResolver.CapaPatron capa : capas) {
            net.minecraft.client.texture.NativeImage mascara = PatronGenerador.mascaraDeCapa("capa", capa);
            if (!capa.lisa() && mascara == null) continue;
            java.util.List<CajaSkin.Rect> region = capa.region() == com.femclothes.region.RegionPintura.TODO ? null
                    : capa.region().rects(com.femclothes.tinturas.TinturasBlockEntity.Categoria.CAPA, CuerpoGeometria.ESCALA_TELA);
            mascaras.add(ClothingTextureCache.CapaMascara.de(capa, mascara, region));
        }
        boolean estampada = com.femclothes.sublimadora.EstampaTextures.tieneEstampa(stack);
        ClothingTextureCache.Encima encima = !estampada ? null : new ClothingTextureCache.Encima() {
            @Override
            public String clave() {
                return "capa_" + com.femclothes.sublimadora.EstampaTextures.claveEstampas(stack);
            }

            @Override
            public boolean aplicar(net.minecraft.client.texture.NativeImage destino) {
                return com.femclothes.sublimadora.EstampaTextures.estampar(destino, stack);
            }
        };
        return ClothingTextureCache.composeGarmentCapas(CAPA_BASE, colorBase, mascaras,
                ClothingTextureCache.Shading.NONE, encima);
    }

    /** Cuánto queda cada capa por fuera de la de abajo, en las filas donde se superponen (px de skin). */
    private static final float SEPARACION_CAPAS = 0.03F;

    /**
     * Dibuja la tela de una parte, ya ordenada por {@link Capa}, con el
     * calce de cada pieza (2026-09-30, "quiero que resolvamos el calce para
     * que quede diferenciado"):
     * <ul>
     *   <li>Suelto/Oversize se abren hacia el ruedo ({@code Calce.caida},
     *       creciendo de a poco fila a fila) y cuelgan por debajo de donde
     *       corta la prenda ({@code Calce.colgado});</li>
     *   <li>a pedido ("respetar siempre las capas"), una pieza nunca queda
     *       por dentro de una de capa más baja: en cada fila donde se
     *       superponen se la empuja {@link #SEPARACION_CAPAS} por fuera de la
     *       de abajo — solo en esas filas, así un croptop ajustado sobre un
     *       pantalón suelto sigue apretando donde no hay pantalón.</li>
     * </ul>
     * Una pieza que queda con todas sus filas iguales y sin colgar va por la
     * caja entera de siempre ({@link #dibujar}); si no, fila por fila
     * ({@link CuerpoGeometria#telaPorFilas}).
     */
    private static void dibujarPiezas(Parte parte, boolean slim, List<Pieza> piezas, @Nullable Pieza conVolumen,
                                      ModelPart delModelo, MatrixStack matrices,
                                      VertexConsumerProvider vertexConsumers, int luz,
                                      com.femclothes.render.relieve.MapaRelieve debajo, Map<Pieza, ItemStack> origen) {
        // Relieve (2026-10-01): cada tela envuelve a lo de abajo (el cuerpo o
        // la pieza anterior) según su calce, y suma arrugas, textura y extrusión.
        com.femclothes.render.relieve.MapaRelieve previo = debajo;
        // Busto (2026-10-01): cada tela que tapa el pecho lo envuelve, por fuera de la de abajo.
        float infladoBusto = parte == Parte.TORSO ? infladoBustoCuerpo(piezas) : 0f;
        boolean conRelieve = com.femclothes.render.relieve.RelieveRender.estilo
                != com.femclothes.render.relieve.RelieveRender.Estilo.APAGADO;
        try {
        boolean pierna = parte == Parte.PIERNA_DER || parte == Parte.PIERNA_IZQ;
        // Lo más afuera que ya hay dibujado en cada fila (12 + lo que puede colgar).
        float[] exterior = new float[12 + 4];
        java.util.Arrays.fill(exterior, Float.NEGATIVE_INFINITY);

        for (Pieza pieza : piezas) {
            int desde = Math.max(0, Math.min(12, pieza.filaDesde()));
            int hasta = Math.max(desde, Math.min(12, pieza.filaHasta()));
            float base = pieza.dilatacion();
            if (conRelieve && parte != Parte.CABEZA) {
                ItemStack prenda = origen.get(pieza);
                previo = com.femclothes.render.relieve.RelieveTela.de(previo, com.femclothes.item.Calce.de(base),
                        prenda == null ? com.femclothes.item.TexturaTela.LISA
                                : prenda.getOrDefault(com.femclothes.item.FemclothesComponents.TEXTURA_TELA,
                                        com.femclothes.item.TexturaTela.LISA),
                        prenda == null ? null : com.femclothes.render.relieve.RelieveTela.extrusionDe(
                                net.minecraft.registry.Registries.ITEM.getId(prenda.getItem())));
                com.femclothes.render.relieve.RelieveRender.actual =
                        new com.femclothes.render.relieve.RelieveRender.Contexto(parte, previo);
            }

            if (pieza == conVolumen) {
                dibujarModelPart(CuerpoGeometria.telaConVolumenDePierna(parte, base),
                        CuerpoGeometria.Superficie.TELA, pieza.textura(), delModelo, matrices, vertexConsumers, luz);
                for (int f = desde; f < hasta; f++) exterior[f] = Math.max(exterior[f], base);
                continue;
            }

            com.femclothes.item.Calce calce = com.femclothes.item.Calce.de(base);
            // La caída es del RUEDO: la parte de torso del pantalón (tiro) y el
            // cinto de la pollera terminan en la cintura, no tienen ruedo libre.
            // La cabeza (banda de la polera) tampoco.
            boolean ruedoLibre = calce != null && parte != Parte.CABEZA
                    && !(parte == Parte.TORSO && (pieza.capa() == Capa.PIERNA_EXTERIOR || pieza.capa() == Capa.POLLERA));
            float caida = ruedoLibre ? calce.caida : 0F;
            int colgado = ruedoLibre ? calce.colgado : 0;
            // Una pierna entera no cuelga por debajo del pie (quedaría enterrada).
            if (pierna && hasta >= 12) colgado = 0;
            if (hasta == desde) colgado = 0;
            // Elástico (puños y ruedo del hoodie): la banda sostiene la tela,
            // no cuelga — la caída queda como globo arriba de la banda.
            if (pieza.elastico()) colgado = 0;

            float[] fila = new float[12];
            boolean uniforme = true;
            for (int f = 0; f < 12; f++) {
                float d = base;
                if (f >= desde && f < hasta) {
                    if (caida > 0F) {
                        float t = (f - desde + 1) / (float) (hasta - desde);
                        d += caida * t * (float) Math.sqrt(t);
                    }
                    // La banda elástica aprieta: 40 % de la holgura, nunca por
                    // dentro del cuerpo (0.05 de aire).
                    if (pieza.elastico() && f == hasta - 1) d = Math.max(0.05F, base * 0.4F);
                    if (exterior[f] != Float.NEGATIVE_INFINITY) d = Math.max(d, exterior[f] + SEPARACION_CAPAS);
                }
                fila[f] = d;
                if (d != base) uniforme = false;
            }
            float dilColgado = hasta > 0 ? fila[hasta - 1] : base;
            for (int k = 0; k < colgado; k++) {
                int f = hasta + k;
                if (exterior[f] != Float.NEGATIVE_INFINITY) dilColgado = Math.max(dilColgado, exterior[f] + SEPARACION_CAPAS);
            }

            if ((uniforme && colgado == 0) || parte == Parte.CABEZA) {
                dibujar(CuerpoGeometria.Superficie.TELA, parte, slim, pieza.textura(), base,
                        delModelo, matrices, vertexConsumers, luz);
            } else {
                dibujarModelPart(CuerpoGeometria.telaPorFilas(parte, slim, fila, hasta, colgado, dilColgado),
                        CuerpoGeometria.Superficie.TELA, pieza.textura(), delModelo, matrices, vertexConsumers, luz);
            }

            if (parte == Parte.TORSO && com.femclothes.render.relieve.BustoRender.actual != null
                    && com.femclothes.render.relieve.BustoRender.cubre(desde, hasta)) {
                infladoBusto = Math.max(fila[3], infladoBusto + SEPARACION_CAPAS);
                float carpa = calce == null ? 0f : switch (calce) {
                    case NORMAL -> 0.4f;
                    case SUELTO -> 1.5f;
                    case OVERSIZE -> 2.5f;
                    default -> 0f;
                };
                dibujarBusto(pieza.textura(), delModelo, infladoBusto, carpa, 20f, matrices, vertexConsumers, luz);
            }

            for (int f = desde; f < hasta; f++) exterior[f] = Math.max(exterior[f], fila[f]);
            for (int k = 0; k < colgado; k++) exterior[hasta + k] = Math.max(exterior[hasta + k], dilColgado);
        }
        } finally {
            com.femclothes.render.relieve.RelieveRender.actual = null;
        }
    }

    /** Copia la pose ya calculada de la parte del jugador y dibuja la nuestra encima. */
    private static void dibujar(CuerpoGeometria.Superficie superficie, Parte parte, boolean slim,
                                Identifier textura, float dilatacion, ModelPart delJugador, MatrixStack matrices,
                                VertexConsumerProvider vertexConsumers, int luz) {
        ModelPart nuestra = CuerpoGeometria.parte(superficie, parte, slim, dilatacion);
        dibujarModelPart(nuestra, superficie, textura, delJugador, matrices, vertexConsumers, luz);
    }

    /** Como {@link #dibujar}, para cuando la ModelPart ya viene resuelta (CUERPO segmentado). */
    static void dibujarModelPart(ModelPart nuestra, CuerpoGeometria.Superficie superficie,
                                Identifier textura, ModelPart delJugador, MatrixStack matrices,
                                VertexConsumerProvider vertexConsumers, int luz) {
        nuestra.copyTransform(delJugador);
        // copyTransform tambien copia xScale/yScale/zScale, asi que la nuestra
        // va DESPUES o se pierde.
        float escala = superficie.escalaDeParte();
        nuestra.xScale = nuestra.yScale = nuestra.zScale = escala;
        com.femclothes.render.relieve.RelieveRender.Contexto relieve =
                com.femclothes.render.relieve.RelieveRender.aplica(com.femclothes.render.relieve.RelieveRender.actual)
                        ? com.femclothes.render.relieve.RelieveRender.actual : null;
        if (ClothingTextureCache.esTranslucida(textura)) {
            // Transparencia real (2026-10-01): segunda pasada, después de todo
            // lo opaco — si no, la tela translúcida del torso escribe su
            // profundidad antes que el brazo de atrás y lo tapa. La ModelPart
            // puede venir de un caché compartido: se guarda su pose acá.
            net.minecraft.client.model.ModelTransform pose = nuestra.getTransform();
            MatrixStack copia = new MatrixStack();
            copia.peek().getPositionMatrix().set(matrices.peek().getPositionMatrix());
            copia.peek().getNormalMatrix().set(matrices.peek().getNormalMatrix());
            TRANSLUCIDAS_PENDIENTES.add(() -> {
                nuestra.setTransform(pose);
                nuestra.xScale = nuestra.yScale = nuestra.zScale = escala;
                VertexConsumer vc = vertexConsumers.getBuffer(ClothingTextureCache.capaDeRender(textura));
                if (relieve != null) {
                    com.femclothes.render.relieve.RelieveRender.dibujar(nuestra, relieve, superficie.escala,
                            copia, vc, luz, OverlayTexture.DEFAULT_UV);
                } else {
                    nuestra.render(copia, vc, luz, OverlayTexture.DEFAULT_UV);
                }
            });
            return;
        }
        VertexConsumer buffer = vertexConsumers.getBuffer(ClothingTextureCache.capaDeRender(textura));
        if (relieve != null) {
            com.femclothes.render.relieve.RelieveRender.dibujar(nuestra, relieve, superficie.escala,
                    matrices, buffer, luz, OverlayTexture.DEFAULT_UV);
        } else {
            nuestra.render(matrices, buffer, luz, OverlayTexture.DEFAULT_UV);
        }
    }

    /**
     * El busto de este dibujo (2026-10-01, "buscando mas la estrategia del mod
     * only jugs"): talle del cuerpo + Estrógenos, la sujeción de la ropa
     * interior (binder aplana, deportivo sujeta) y el rebote de resorte.
     */
    private static void prepararBusto(LivingEntity entidad, float tickDelta) {
        com.femclothes.render.relieve.BustoRender.actual = bustoDe(entidad, tickDelta, true);
    }

    /**
     * El busto de {@code entidad} (null si no tiene): talle, sujeción de la
     * ropa interior y, con {@code fisica}, el rebote de este cuadro.
     */
    @Nullable
    public static com.femclothes.render.relieve.BustoRender.Busto bustoDe(@Nullable LivingEntity entidad,
                                                                          float tickDelta, boolean fisica) {
        if (entidad == null) return null;
        PerfilCuerpo perfil = perfilDe(entidad);
        float talle = com.femclothes.render.relieve.RelieveCuerpo.bustoDe(perfil);
        if (talle <= 0f) return null;
        com.femclothes.body.InteriorArriba arriba = perfil.interior().arriba();
        float sujecion = arriba == com.femclothes.body.InteriorArriba.BINDER ? 0.3f
                : arriba == com.femclothes.body.InteriorArriba.DEPORTIVO ? 0.75f : 1f;
        return new com.femclothes.render.relieve.BustoRender.Busto(talle, sujecion,
                fisica ? com.femclothes.render.relieve.FisicaBusto.de(entidad, tickDelta, talle) : null);
    }

    /** {@link #infladoBustoCuerpo} para un conjunto de prendas (las piezas de su torso). */
    public static float infladoBustoDe(List<ItemStack> prendas) {
        List<Pieza> torso = new ArrayList<>();
        for (ItemStack s : prendas) {
            for (Pieza p : PiezasDePrenda.de(s, null)) if (p.parte() == Parte.TORSO) torso.add(p);
        }
        return infladoBustoCuerpo(torso);
    }

    /**
     * Cuánto se infla el busto de la piel: un poco por dentro de la tela más
     * pegada que lo tape (Ajustado aprieta por dentro del cuerpo).
     */
    private static float infladoBustoCuerpo(@Nullable List<Pieza> piezas) {
        float minimo = 0f;
        if (piezas != null) {
            for (Pieza p : piezas) {
                if (com.femclothes.render.relieve.BustoRender.cubre(p.filaDesde(), p.filaHasta())) {
                    minimo = Math.min(minimo, p.dilatacion() - 0.06f);
                }
            }
        }
        return minimo;
    }

    /** El busto con la textura de la skin (y su segunda capa), cuando el torso lo dibuja vanilla. */
    private static void dibujarBustoEnLaSkin(LivingEntity entidad, BipedEntityModel<?> biped, MatrixStack matrices,
                                             VertexConsumerProvider vertexConsumers, int luz) {
        if (com.femclothes.render.relieve.BustoRender.actual == null
                || !(entidad instanceof AbstractClientPlayerEntity jugador) || !biped.body.visible) return;
        Identifier skin = jugador.getSkinTextures().texture();
        boolean chaqueta = jugador.isPartVisible(net.minecraft.entity.player.PlayerModelPart.JACKET);
        dibujarBustoDeSkin(skin, biped.body, chaqueta, matrices, vertexConsumers, luz);
    }

    /**
     * El busto con una skin (la del jugador o la de la figura del maniquí).
     * La piel sale apenas por delante de la segunda capa plana (2026-10-02,
     * "la skin del pecho donde surgen las tetas muestra la remera en plano":
     * la chaqueta plana de la skin asomaba en la base) y la segunda capa va
     * en su propia cúpula, por fuera.
     */
    public static void dibujarBustoDeSkin(Identifier skin, ModelPart torso, boolean chaqueta, MatrixStack matrices,
                                          VertexConsumerProvider vertexConsumers, int luz) {
        dibujarBustoDeSkin(skin, torso, chaqueta, chaqueta ? 0.27f : 0f, matrices, vertexConsumers, luz);
    }

    /** Con el inflado de la piel a mano (por dentro de la ropa que la tape). */
    public static void dibujarBustoDeSkin(Identifier skin, ModelPart torso, boolean chaqueta, float inflado,
                                          MatrixStack matrices, VertexConsumerProvider vertexConsumers, int luz) {
        dibujarBusto(skin, torso, inflado, 0f, 20f, matrices, vertexConsumers, luz);
        if (chaqueta) dibujarBusto(skin, torso, 0.5f, 0f, 36f, matrices, vertexConsumers, luz);
    }

    /** Las dos cajas del busto con {@code textura}, en el marco del torso; las translúcidas, a la segunda pasada. */
    private static void dibujarBusto(Identifier textura, ModelPart torso, float inflado, float carpa, float filaSkin,
                                     MatrixStack matrices, VertexConsumerProvider vertexConsumers, int luz) {
        com.femclothes.render.relieve.BustoRender.Busto busto = com.femclothes.render.relieve.BustoRender.actual;
        if (busto == null) return;
        matrices.push();
        torso.rotate(matrices);
        if (ClothingTextureCache.esTranslucida(textura)) {
            MatrixStack copia = new MatrixStack();
            copia.peek().getPositionMatrix().set(matrices.peek().getPositionMatrix());
            copia.peek().getNormalMatrix().set(matrices.peek().getNormalMatrix());
            TRANSLUCIDAS_PENDIENTES.add(() -> com.femclothes.render.relieve.BustoRender.dibujar(busto, copia,
                    vertexConsumers.getBuffer(ClothingTextureCache.capaDeRender(textura)), luz,
                    OverlayTexture.DEFAULT_UV, inflado, carpa, filaSkin));
        } else {
            com.femclothes.render.relieve.BustoRender.dibujar(busto, matrices,
                    vertexConsumers.getBuffer(ClothingTextureCache.capaDeRender(textura)), luz,
                    OverlayTexture.DEFAULT_UV, inflado, carpa, filaSkin);
        }
        matrices.pop();
    }

    /** Telas translúcidas de este dibujo, para la segunda pasada ({@link #dibujarTranslucidas}). */
    private static final List<Runnable> TRANSLUCIDAS_PENDIENTES = new ArrayList<>();

    /** Dibuja (en el orden en que llegaron: de adentro hacia afuera) las telas translúcidas pendientes. */
    private static void dibujarTranslucidas() {
        for (Runnable r : TRANSLUCIDAS_PENDIENTES) r.run();
        TRANSLUCIDAS_PENDIENTES.clear();
    }

    /**
     * Fila del torso donde está la cintura natural — coincide con
     * {@code Variante.Largo.NORMAL.filas} (la remera "normal" baja justo
     * hasta ahí). Por debajo de esta fila NO se reconstruye piel: se
     * dibuja la piel REAL del jugador — a pedido (2026-09-19), "la piel
     * reconstruida hasta la cintura, de ahí la real". Antes CUERPO
     * rellenaba las 12 filas enteras siempre, así que una remera crop o
     * normal (que no llega a la cintura) mostraba ahí piel RECONSTRUIDA
     * en vez de la piel de verdad del jugador — el detalle que se pidió
     * corregir. Cuando la prenda sí cubre esas filas (remerón/LARGO), la
     * {@link Pieza} de tela se dibuja arriba de las dos capas igual y las
     * tapa por completo, así que el corte no cambia nada en ese caso.
     */
    private static final int FILA_CINTURA_TORSO = 9;

    /**
     * El CUERPO del torso, partido en la cintura ({@link #FILA_CINTURA_TORSO}):
     * arriba de la cintura, la piel reconstruida de siempre; de la
     * cintura para abajo, la piel REAL del jugador (nunca reconstruida
     * ahí). Si la entidad no es un jugador (mob con una prenda del mod,
     * caso raro) no hay piel "real" de la que tirar, así que se cae al
     * camino de siempre sin cortar nada.
     */
    private static void dibujarCuerpoTorso(LivingEntity entidad, boolean slim, Identifier cuerpoReconstruido,
                                            @Nullable List<CuerpoGeometria.SegmentoCuerpo> segmentos,
                                            ModelPart delJugador, MatrixStack matrices,
                                            VertexConsumerProvider vertexConsumers, int luz) {
        // Con un cuerpo elegido la pelvis también es del cuerpo (2026-09-29,
        // "porque no cubre la zona de la pelvis?"): la piel real de la cintura
        // para abajo tiene sentido solo con "Mi propia skin".
        Identifier pielReal = entidad instanceof AbstractClientPlayerEntity jugador
                && perfilDe(entidad).cuerpo() == com.femclothes.body.CuerpoBase.SKIN_REAL
                ? jugador.getSkinTextures().texture() : null;
        if (pielReal == null) {
            ModelPart nuestra = segmentos == null
                    ? CuerpoGeometria.parte(CuerpoGeometria.Superficie.CUERPO, Parte.TORSO, slim)
                    : CuerpoGeometria.cuerpoSegmentado(Parte.TORSO, slim, segmentos);
            dibujarModelPart(nuestra, CuerpoGeometria.Superficie.CUERPO, cuerpoReconstruido,
                    delJugador, matrices, vertexConsumers, luz);
            return;
        }

        List<CuerpoGeometria.SegmentoCuerpo> completos = segmentos != null ? segmentos
                : List.of(new CuerpoGeometria.SegmentoCuerpo(0, 12, CuerpoGeometria.Superficie.CUERPO.dilatacion));

        List<CuerpoGeometria.SegmentoCuerpo> arriba = recortarSegmentos(completos, 0, FILA_CINTURA_TORSO);
        List<CuerpoGeometria.SegmentoCuerpo> abajo = recortarSegmentos(completos, FILA_CINTURA_TORSO, 12);

        if (!arriba.isEmpty()) {
            dibujarModelPart(CuerpoGeometria.cuerpoSegmentado(Parte.TORSO, slim, arriba),
                    CuerpoGeometria.Superficie.CUERPO, cuerpoReconstruido, delJugador, matrices, vertexConsumers, luz);
        }
        if (!abajo.isEmpty()) {
            dibujarModelPart(CuerpoGeometria.cuerpoSegmentado(Parte.TORSO, slim, abajo),
                    CuerpoGeometria.Superficie.CUERPO, pielReal, delJugador, matrices, vertexConsumers, luz);
        }
    }

    /** Recorta una lista de tramos contiguos [0,12) a la ventana [desde,hasta), partiendo el que cruce el borde. */
    private static List<CuerpoGeometria.SegmentoCuerpo> recortarSegmentos(
            List<CuerpoGeometria.SegmentoCuerpo> segmentos, int desde, int hasta) {
        List<CuerpoGeometria.SegmentoCuerpo> out = new ArrayList<>();
        for (CuerpoGeometria.SegmentoCuerpo s : segmentos) {
            int d = Math.max(s.filaDesde(), desde), h = Math.min(s.filaHasta(), hasta);
            if (d < h) out.add(new CuerpoGeometria.SegmentoCuerpo(d, h, s.dilatacion()));
        }
        return out;
    }

    /**
     * Qué partes hay que ocultarle al modelo BASE de vanilla porque acá se
     * dibuja encima — a pedido (2026-09-16), "quiero que nuestro modelo
     * reemplace el original": la capa base de la skin no se puede borrar
     * con textura (a diferencia de la segunda capa/overlay, que sí —
     * ver {@code SkinRegions}), así que la única forma de que no compita
     * en profundidad con {@code CUERPO} es que vanilla directamente no la
     * dibuje. {@code LivingEntityRendererMixin} llama esto para apagar
     * ({@code ModelPart.visible = false}) esas partes justo ANTES de que
     * vanilla dibuje su capa base, y las prende de vuelta justo DESPUÉS
     * (nunca antes de que {@link #render} corra — si quedaran apagadas,
     * {@code delJugador.visible} en el loop de abajo las saltearía a
     * ELLAS también, y no se dibujaría ni lo de vanilla ni lo nuestro).
     *
     * <p>Misma condición EXACTA que decide {@code llevaCuerpo} en
     * {@link #render}, factorizada para que las dos nunca se
     * desincronicen: si esto ocultara una parte que {@link #render} no
     * llega a dibujar (ej. {@code cuerpo == null}, la textura del cuerpo
     * base falló ese frame), quedaría un agujero real — invisible de
     * vanilla Y sin nada nuestro reemplazándolo.
     */
    public static List<Parte> partesAOcultarDeVanilla(LivingEntity entidad) {
        List<ItemStack> prendas = previewOverride != null ? previewOverride : equipadas(entidad);
        // Cuerpo entero: la vista previa de elegir cuerpo, o un perfil que usa
        // el cuerpo como skin aunque no haya ropa (2026-09-29, "un selector que
        // directamente te deje esa skin de default").
        boolean cuerpoEntero = perfilOverride != null || perfilDe(entidad).siempre();
        if (prendas.isEmpty() && !cuerpoEntero) return List.of();
        Identifier cuerpo = texturaDelCuerpo(entidad, esSlim(entidad));
        if (cuerpo == null) return List.of();
        List<Parte> out = new ArrayList<>(cuerpoEntero ? List.of(Parte.values()) : Garments.partesCubiertas(prendas));
        // La cabeza nunca lleva cuerpo base (ver render) — ocultar la de
        // vanilla ahí dejaría al jugador sin cara.
        out.remove(Parte.CABEZA);
        return out;
    }

    private static final float EPSILON_COMPRESION = 0.02F;

    /**
     * Si alguna pieza de esta parte tiene un calce más chico que el cuerpo
     * (Ajustado) arma los tramos de fila para
     * {@link CuerpoGeometria#cuerpoSegmentado} — la piel se ve más flaca
     * SOLO donde esa tela realmente tapa (unión de {@code filaDesde}/
     * {@code filaHasta} de cada pieza comprimida; si dos se superponen con
     * calces distintos, gana la más chica, para que ninguna quede tapada
     * por el cuerpo). Devuelve {@code null} cuando ninguna pieza comprime
     * — el caso normal, sigue el camino simple de siempre.
     */
    @Nullable
    private static List<CuerpoGeometria.SegmentoCuerpo> segmentosCuerpo(@Nullable List<Pieza> piezas) {
        if (piezas == null) return null;
        float base = CuerpoGeometria.Superficie.CUERPO.dilatacion;
        float[] porFila = new float[12];
        java.util.Arrays.fill(porFila, base);
        boolean hayCompresion = false;
        for (Pieza pieza : piezas) {
            // "< base + 0.01": también Pegado (base + 0.001), que "reemplaza la
            // skin" (2026-09-30) — el cuerpo se mete apenas por dentro de él en
            // vez de quedar a una milésima, así no hay empate de profundidad.
            if (pieza.dilatacion() >= base + 0.01F) continue;
            hayCompresion = true;
            float propia = pieza.dilatacion() - EPSILON_COMPRESION;
            int desde = Math.max(0, pieza.filaDesde()), hasta = Math.min(12, pieza.filaHasta());
            for (int f = desde; f < hasta; f++) {
                if (propia < porFila[f]) porFila[f] = propia;
            }
        }
        if (!hayCompresion) return null;
        List<CuerpoGeometria.SegmentoCuerpo> segmentos = new ArrayList<>();
        int inicio = 0;
        for (int f = 1; f <= 12; f++) {
            if (f == 12 || porFila[f] != porFila[inicio]) {
                segmentos.add(new CuerpoGeometria.SegmentoCuerpo(inicio, f, porFila[inicio]));
                inicio = f;
            }
        }
        return segmentos;
    }

    /**
     * Perfil a mostrar en vez del guardado mientras se dibuja la vista previa
     * de la GUI de elegir cuerpo (2026-09-29) — mismo mecanismo que
     * {@link #previewOverride}: se pone antes de dibujar y se limpia después.
     */
    @Nullable
    public static PerfilCuerpo perfilOverride = null;

    /** El perfil que se está dibujando: el de la vista previa, el del jugador o el de siempre. */
    public static PerfilCuerpo perfilDe(LivingEntity entidad) {
        return perfilOverride != null ? perfilOverride
                : entidad instanceof PlayerEntity jugador
                ? PerfilesDeCuerpo.de(jugador)
                : PerfilCuerpo.DEFECTO;
    }

    @Nullable
    private static Identifier texturaDelCuerpo(LivingEntity entidad, boolean slim) {
        return CuerpoBaseTextures.de(entidad, perfilDe(entidad), slim);
    }

    /**
     * Dibuja SOLO la tela ({@link Pieza}) de un brazo sobre el
     * {@link ModelPart} de PRIMERA PERSONA (2026-09-24, "la mano en
     * primera persona nunca muestra las prendas custom") — {@link #render}
     * de acá arriba nunca corre para esa vista: vanilla dibuja el brazo/
     * manga de primera persona por su cuenta
     * ({@code PlayerEntityRenderer#renderArm}), sin pasar por el modelo
     * completo del jugador ni sus {@code FeatureRenderer}. Llamado desde
     * {@code PlayerEntityRendererFirstPersonArmMixin}.
     *
     * <p>A propósito NO dibuja {@code Superficie.CUERPO} (el cuerpo
     * reconstruido): en primera persona vanilla ya muestra la piel REAL
     * del jugador tal cual (correcta), a diferencia de tercera persona
     * donde se reconstruye para tapar mangas pintadas en la textura de la
     * skin — acá lo único que falta es la PRENDA en sí.
     */
    public static void dibujarBrazoPrimeraPersona(Parte parte, ModelPart brazoDelJugador,
                                                   AbstractClientPlayerEntity jugador, MatrixStack matrices,
                                                   VertexConsumerProvider vertexConsumers, int luz) {
        List<ItemStack> prendas = equipadas(jugador);
        if (prendas.isEmpty()) return;

        List<Pieza> piezas = new ArrayList<>();
        for (ItemStack stack : prendas) {
            for (Pieza pieza : PiezasDePrenda.de(stack, jugador)) {
                if (pieza.parte() == parte) piezas.add(pieza);
            }
        }
        if (piezas.isEmpty()) return;

        boolean slim = esSlim(jugador);
        piezas.sort(Comparator.comparingInt(Pieza::capa));
        for (Pieza pieza : piezas) {
            dibujar(CuerpoGeometria.Superficie.TELA, parte, slim, pieza.textura(), pieza.dilatacion(),
                    brazoDelJugador, matrices, vertexConsumers, luz);
        }
    }

    /**
     * Solo la TELA de {@code prendas} (sin cuerpo base) sobre un modelo
     * bípedo ya posado que no es de ninguna entidad — para el Maniquí
     * (2026-09-30, "guarda y muestra la ropa... 16 slots, 4 por prenda"):
     * la figura del modelo {@code mannequin} hace de cuerpo, así que acá no
     * hay piel que reconstruir. Mismo orden por {@link Capa} y misma
     * pollera que {@link #render}; las medias se dibujan sin el volumen de
     * pierna (ese depende de la pierna real del jugador).
     */
    public static void dibujarTela(BipedEntityModel<?> biped, boolean slim, List<ItemStack> prendas,
                                   MatrixStack matrices, VertexConsumerProvider vertexConsumers, int luz) {
        dibujarTela(biped, slim, prendas, matrices, vertexConsumers, luz, null);
    }

    /** Con el busto de la figura (2026-10-02, maniquí "agregale la opcion de ponerle tetas"), o null. */
    public static void dibujarTela(BipedEntityModel<?> biped, boolean slim, List<ItemStack> prendas,
                                   MatrixStack matrices, VertexConsumerProvider vertexConsumers, int luz,
                                   @Nullable com.femclothes.render.relieve.BustoRender.Busto busto) {
        com.femclothes.render.relieve.BustoRender.actual = busto;
        try {
            dibujarTelaConBusto(biped, slim, prendas, matrices, vertexConsumers, luz);
        } finally {
            com.femclothes.render.relieve.BustoRender.actual = null;
        }
    }

    private static void dibujarTelaConBusto(BipedEntityModel<?> biped, boolean slim, List<ItemStack> prendas,
                                            MatrixStack matrices, VertexConsumerProvider vertexConsumers, int luz) {
        Map<Parte, List<Pieza>> porParte = new EnumMap<>(Parte.class);
        Map<Pieza, ItemStack> origen = new java.util.IdentityHashMap<>();
        for (ItemStack stack : prendas) {
            // Ningún proveedor de PiezasDelMod mira la entidad hoy — solo
            // la reciben por si el aspecto llegara a depender de ella.
            for (Pieza pieza : PiezasDePrenda.de(stack, null)) {
                porParte.computeIfAbsent(pieza.parte(), k -> new ArrayList<>()).add(pieza);
                origen.put(pieza, stack);
            }
        }
        for (Map.Entry<Parte, List<Pieza>> entrada : porParte.entrySet()) {
            Parte parte = entrada.getKey();
            List<Pieza> piezas = entrada.getValue();
            ModelPart delModelo = CuerpoGeometria.delJugador(biped, parte);
            piezas.sort(Comparator.comparingInt(Pieza::capa));
            // La figura del maniquí no tiene relieve: la tela arranca de la caja.
            dibujarPiezas(parte, slim, piezas, null, delModelo, matrices, vertexConsumers, luz,
                    com.femclothes.render.relieve.MapaRelieve.PLANO, origen);
        }
        CuelloYCapucha.dibujar(prendas, biped, matrices, vertexConsumers, luz);
        ApliqueRenderer.dibujar(prendas, biped, matrices, vertexConsumers, luz);
        dibujarTranslucidas();
        // Sin entidad: la pollera queda quieta (sin inercia ni twirl).
        dibujarPollera(prendas, null, biped, matrices, vertexConsumers, luz, 0f);
    }

    public static List<ItemStack> equipadas(LivingEntity entidad) {
        List<ItemStack> out = new ArrayList<>();
        TrinketsApi.getTrinketComponent(entidad).ifPresent(c -> {
            for (var par : c.getAllEquipped()) {
                ItemStack stack = par.getRight();
                if (Garments.esPrenda(stack)) out.add(stack);
            }
        });
        return out;
    }

    /**
     * Si el jugador usa el modelo de brazos finos.
     *
     * Se lee de la skin y no del modelo porque PlayerEntityModel guarda su
     * thinArms privado y no lo expone.
     */
    private static boolean esSlim(LivingEntity entidad) {
        return entidad instanceof AbstractClientPlayerEntity jugador
                && jugador.getSkinTextures().model() == SkinTextures.Model.SLIM;
    }
}
