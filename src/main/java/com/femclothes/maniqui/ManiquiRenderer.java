package com.femclothes.maniqui;

import com.femclothes.Femclothes;
import com.femclothes.garment.Garments;
import com.femclothes.garment.Parte;
import com.femclothes.render.GarmentFeatureRenderer;
import com.femclothes.render.Pieza;
import com.femclothes.render.PiezasDePrenda;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.entity.feature.HeadFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.item.trim.ArmorTrim;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

import java.util.List;

/**
 * Dibuja el Maniquí: el pedestal y el plato con GeckoLib, y encima una
 * figura ARTICULADA con pose, armadura y ropa (2026-09-30, "el maniqui que
 * tenga poses y le agreguemos una columna de slots de armadura").
 *
 * <p>La figura rígida del {@code mannequin.geo.json} (hueso {@code figura})
 * se esconde en el bloque y se reemplaza por un modelo de jugador a
 * {@link #ESCALA} — la figura del zip ya estaba hecha a esa escala (torso
 * 4.8 x 7 x 2.4 = 8 x 12 x 4 * 0.6), así que ocupa el mismo lugar. Dos
 * figuras ("dame las dos opciones"): maniquí liso
 * ({@code textures/entity/maniqui.png}, o un color madera generado mientras
 * no exista) o la skin de quien la eligió ({@link ManiquiBlockEntity#dueno}).
 *
 * <p>Todo (figura, armadura, ropa) gira con el plato y se posa con
 * {@link PoseManiqui}: la armadura copia la pose con {@code copyBipedStateTo}
 * y la ropa con el {@code copyTransform} de siempre.
 */
public class ManiquiRenderer extends GeoBlockRenderer<ManiquiBlockEntity> {

    /**
     * Escala de la figura respecto de un jugador: tamaño normal (2026-10-02,
     * "el maniquí ahora se dibuja chiquito, y la ropa sigue del mismo tamaño,
     * volvelo al tamaño normal"; antes 0.6, el tamaño de la figura del zip).
     */
    private static final float ESCALA = 1.0f;
    /** Altura de los pies de la figura (arriba del escalón del plato), en píxeles de bloque. */
    private static final float ALTURA_PIES = 7.6f;

    private static final Identifier TEXTURA_MANIQUI = Identifier.of(Femclothes.MOD_ID, "textures/entity/maniqui.png");
    private static final Identifier TEXTURA_MANIQUI_GENERADA = Identifier.of(Femclothes.MOD_ID, "dynamic/maniqui_liso");
    private static Identifier texturaManiqui;

    private final PlayerEntityModel<LivingEntity> cuerpoFino;
    private final PlayerEntityModel<LivingEntity> cuerpoAncho;
    private final BipedEntityModel<LivingEntity> armaduraInterior;
    private final BipedEntityModel<LivingEntity> armaduraExterior;

    public ManiquiRenderer(BlockEntityRendererFactory.Context ctx) {
        super(new ManiquiGeoModel());
        this.cuerpoFino = new PlayerEntityModel<>(ctx.getLayerModelPart(EntityModelLayers.PLAYER_SLIM), true);
        this.cuerpoAncho = new PlayerEntityModel<>(ctx.getLayerModelPart(EntityModelLayers.PLAYER), false);
        this.armaduraInterior = new BipedEntityModel<>(ctx.getLayerModelPart(EntityModelLayers.PLAYER_INNER_ARMOR));
        this.armaduraExterior = new BipedEntityModel<>(ctx.getLayerModelPart(EntityModelLayers.PLAYER_OUTER_ARMOR));
    }

    @Override
    public void render(ManiquiBlockEntity be, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int luz, int overlay) {
        // Primero el ángulo de este frame: lo leen tanto el hueso turntable
        // (ManiquiGeoModel, dentro de super.render) como la figura de abajo.
        double tiempo = be.getWorld() == null ? 0 : be.getWorld().getTime() + (double) tickDelta;
        float angulo = be.avanzarAngulo(tiempo);

        super.render(be, tickDelta, matrices, vertexConsumers, luz, overlay);

        // ── figura ─────────────────────────────────────────────────────────
        matrices.push();
        // Misma transformación que GeoBlockRenderer le aplica al modelo
        // (centro del bloque + giro por FACING) y después el giro del plato.
        matrices.translate(0.5, 0, 0.5);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(giroPorFacing(be)));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotation(angulo));
        matrices.translate(0, (ALTURA_PIES - bajada(be) * ESCALA / 0.6f) / 16f, 0);
        matrices.scale(ESCALA, ESCALA, ESCALA);
        dibujarFigura(be, matrices, vertexConsumers, luz);
        matrices.pop();
    }

    /**
     * La figura vestida (cuerpo, busto, armadura y ropa) con los pies en el
     * origen y Y hacia arriba — el bloque la pone sobre el plato y la GUI del
     * maniquí la usa de vista previa (2026-10-02, "la vista previa que tiene
     * que generar la gui es del maniquí no del player").
     */
    public void dibujarFigura(ManiquiBlockEntity be, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int luz) {
        Identifier textura;
        boolean slim;
        if (be.figuraSkin() && be.dueno() != null) {
            SkinTextures skin = MinecraftClient.getInstance().getSkinProvider().getSkinTextures(be.dueno().gameProfile());
            textura = skin.texture();
            slim = skin.model() == SkinTextures.Model.SLIM;
        } else {
            textura = texturaManiqui();
            slim = true;
        }
        PlayerEntityModel<LivingEntity> cuerpo = slim ? cuerpoFino : cuerpoAncho;
        // Los modelos nacen como bebé (Model.child = true) y LivingEntityRenderer lo
        // corrige en cada cuadro; acá no hay entidad, así que la figura se dibujaba
        // a escala de bebé (cuerpo a la mitad) y la ropa, que no mira ese flag, a
        // tamaño normal (2026-10-02, "igual la tela esta saliendo normal, lo
        // chiquito es el maniqui").
        cuerpo.child = false;
        armaduraInterior.child = false;
        armaduraExterior.child = false;
        List<ItemStack> prendas = be.prendasPuestas();
        posar(cuerpo, be);
        ajustarAlCalce(cuerpo, prendas, slim);
        capasDeSkin(cuerpo, be.figuraSkin(), prendas);
        // Busto de la figura (2026-10-02, "agregale la opcion de ponerle tetas"): quieto, sin rebote.
        com.femclothes.render.relieve.BustoRender.Busto busto = be.busto() > 0
                ? new com.femclothes.render.relieve.BustoRender.Busto(
                        com.femclothes.render.relieve.RelieveCuerpo.bustoDeTalle(be.busto()), 1f, null)
                : null;

        matrices.push();
        // Lo mismo que hace LivingEntityRenderer antes de dibujar un modelo
        // de entidad: invertir X/Y y bajar 1.501 para que los pies queden en 0.
        matrices.scale(-1f, -1f, 1f);
        matrices.translate(0, -1.501f, 0);
        // La armadura de la figura recibe el busto como la de un jugador (BustoEnModelos).
        com.femclothes.render.relieve.BustoEnModelos.enEntidad = busto;
        com.femclothes.render.relieve.BustoEnModelos.modeloPrincipal = cuerpo;
        try {
            cuerpo.render(matrices, vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(textura)),
                    luz, OverlayTexture.DEFAULT_UV);
            if (busto != null && cuerpo.body.visible) {
                com.femclothes.render.relieve.BustoRender.actual = busto;
                try {
                    boolean chaqueta = cuerpo.jacket.visible;
                    float inflado = prendas.isEmpty() ? (chaqueta ? 0.27f : 0f)
                            : Math.min(chaqueta ? 0.27f : 0f, GarmentFeatureRenderer.infladoBustoDe(prendas));
                    // Bajo una tela holgada el busto lo dibuja su manto (2026-10-02, "el hoodie es atravesado").
                    GarmentFeatureRenderer.dibujarBustoDeSkin(textura, cuerpo.body, chaqueta, inflado,
                            !GarmentFeatureRenderer.bustoTapadoPorManto(prendas), matrices, vertexConsumers, luz);
                } finally {
                    com.femclothes.render.relieve.BustoRender.actual = null;
                }
            }
            dibujarArmadura(be, cuerpo, matrices, vertexConsumers, luz);
            if (!prendas.isEmpty()) {
                GarmentFeatureRenderer.dibujarTela(cuerpo, slim, prendas, matrices, vertexConsumers, luz, busto);
            }
        } finally {
            com.femclothes.render.relieve.BustoEnModelos.enEntidad = null;
            com.femclothes.render.relieve.BustoEnModelos.modeloPrincipal = null;
            com.femclothes.render.relieve.BustoEnModelos.slotArmadura = null;
        }
        matrices.pop();
    }

    // ── pose ───────────────────────────────────────────────────────────────
    private static void posar(PlayerEntityModel<LivingEntity> m, ManiquiBlockEntity be) {
        float r = MathHelper.RADIANS_PER_DEGREE;
        m.head.pitch = be.angulo(0) * r;
        m.head.yaw = be.angulo(1) * r;
        m.head.roll = 0;
        m.body.pitch = m.body.yaw = m.body.roll = 0;
        m.rightArm.pitch = be.angulo(2) * r;
        m.rightArm.roll = be.angulo(3) * r;
        m.rightArm.yaw = 0;
        m.leftArm.pitch = be.angulo(4) * r;
        m.leftArm.roll = -be.angulo(5) * r;
        m.leftArm.yaw = 0;
        m.rightLeg.pitch = be.angulo(6) * r;
        m.rightLeg.roll = be.angulo(7) * r;
        m.rightLeg.yaw = 0;
        m.leftLeg.pitch = be.angulo(8) * r;
        m.leftLeg.roll = -be.angulo(9) * r;
        m.leftLeg.yaw = 0;
    }

    /** Sentada (piernas hacia adelante), la figura baja hasta apoyarse en el plato. */
    private static float bajada(ManiquiBlockEntity be) {
        float adelante = -(be.angulo(6) + be.angulo(8)) / 2f;
        return PoseManiqui.BAJADA_SENTADO * MathHelper.clamp(adelante / 88f, 0f, 1f);
    }

    /**
     * La figura no tiene el cuerpo segmentado del jugador
     * ({@code GarmentFeatureRenderer#segmentosCuerpo}): con Pegado o
     * Ajustado la tela quedaría por dentro de la figura. En esas partes se
     * achica la figura (x/z) hasta quedar apenas por dentro de la tela más
     * ajustada — en toda la parte, no solo donde hay tela.
     */
    private static void ajustarAlCalce(PlayerEntityModel<LivingEntity> m, List<ItemStack> prendas, boolean slim) {
        for (Parte parte : Parte.values()) {
            if (parte == Parte.CABEZA) continue;
            ModelPart p = com.femclothes.render.CuerpoGeometria.delJugador(m, parte);
            p.xScale = p.yScale = p.zScale = 1f;
        }
        for (ItemStack stack : prendas) {
            for (Pieza pieza : PiezasDePrenda.de(stack, null)) {
                if (pieza.parte() == Parte.CABEZA || pieza.dilatacion() >= 0.05f) continue;
                ModelPart p = com.femclothes.render.CuerpoGeometria.delJugador(m, pieza.parte());
                float margen = pieza.dilatacion() - 0.03f;
                boolean brazo = pieza.parte() == Parte.BRAZO_DER || pieza.parte() == Parte.BRAZO_IZQ;
                float ancho = pieza.parte() == Parte.TORSO ? 8f : brazo && slim ? 3f : 4f;
                p.xScale = Math.min(p.xScale, (ancho + 2 * margen) / ancho);
                p.zScale = Math.min(p.zScale, (4f + 2 * margen) / 4f);
            }
        }
    }

    /**
     * Segunda capa de la skin: con la figura de maniquí no hay (la textura es
     * lisa); con skin, el sombrero siempre y el resto solo donde no hay ropa
     * del mod (si no, asoma a través de la tela). Copia la pose como hace
     * {@code PlayerEntityModel#setAngles}.
     */
    private static void capasDeSkin(PlayerEntityModel<LivingEntity> m, boolean skin, List<ItemStack> prendas) {
        List<Parte> cubiertas = skin ? Garments.partesCubiertas(prendas) : List.of();
        m.hat.visible = skin;
        m.jacket.visible = skin && !cubiertas.contains(Parte.TORSO);
        m.rightSleeve.visible = skin && !cubiertas.contains(Parte.BRAZO_DER);
        m.leftSleeve.visible = skin && !cubiertas.contains(Parte.BRAZO_IZQ);
        m.rightPants.visible = skin && !cubiertas.contains(Parte.PIERNA_DER);
        m.leftPants.visible = skin && !cubiertas.contains(Parte.PIERNA_IZQ);
        m.hat.copyTransform(m.head);
        m.jacket.copyTransform(m.body);
        m.rightSleeve.copyTransform(m.rightArm);
        m.leftSleeve.copyTransform(m.leftArm);
        m.rightPants.copyTransform(m.rightLeg);
        m.leftPants.copyTransform(m.leftLeg);
    }

    // ── armadura (calcado de ArmorFeatureRenderer#renderArmor, sin entidad) ──
    private void dibujarArmadura(ManiquiBlockEntity be, PlayerEntityModel<LivingEntity> cuerpo, MatrixStack matrices,
                                 VertexConsumerProvider vertexConsumers, int luz) {
        for (EquipmentSlot slot : new EquipmentSlot[] {
                EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.HEAD }) {
            ItemStack stack = be.armadura(slot);
            if (stack.isEmpty()) continue;
            // Apliques de la Mesa de estilado en la armadura (2026-10-02, "extender apliques para toda armadura").
            com.femclothes.render.ApliqueRenderer.dibujar(stack, com.femclothes.render.ApliqueRenderer.dilatacionDeSlot(slot),
                    cuerpo, matrices, vertexConsumers, luz);
            if (!(stack.getItem() instanceof ArmorItem armadura) || armadura.getSlotType() != slot) {
                if (slot == EquipmentSlot.HEAD) dibujarCabeza(stack, cuerpo, matrices, vertexConsumers, luz, be);
                continue; // élitros: todavía no
            }
            boolean interior = slot == EquipmentSlot.LEGS;
            BipedEntityModel<LivingEntity> modelo = interior ? armaduraInterior : armaduraExterior;
            cuerpo.copyBipedStateTo(modelo);
            // copyTransform también copia la escala del ajuste al calce: la armadura no se achica.
            for (ModelPart p : new ModelPart[] { modelo.head, modelo.hat, modelo.body, modelo.rightArm,
                    modelo.leftArm, modelo.rightLeg, modelo.leftLeg }) {
                p.xScale = p.yScale = p.zScale = 1f;
            }
            visibles(modelo, slot);
            // La pechera lleva el busto rígido; las otras piezas no (BustoEnModelos).
            com.femclothes.render.relieve.BustoEnModelos.slotArmadura = slot;

            RegistryEntry<ArmorMaterial> material = armadura.getMaterial();
            int color = stack.isIn(ItemTags.DYEABLE)
                    ? ColorHelper.Argb.fullAlpha(DyedColorComponent.getColor(stack, -6265536)) : -1;
            for (ArmorMaterial.Layer capa : material.value().layers()) {
                int c = capa.isDyeable() ? color : -1;
                modelo.render(matrices, vertexConsumers.getBuffer(RenderLayer.getArmorCutoutNoCull(capa.getTexture(interior))),
                        luz, OverlayTexture.DEFAULT_UV, c);
            }
            ArmorTrim trim = stack.get(DataComponentTypes.TRIM);
            if (trim != null) {
                Sprite sprite = MinecraftClient.getInstance().getBakedModelManager()
                        .getAtlas(TexturedRenderLayers.ARMOR_TRIMS_ATLAS_TEXTURE)
                        .getSprite(interior ? trim.getLeggingsModelId(material) : trim.getGenericModelId(material));
                modelo.render(matrices, sprite.getTextureSpecificVertexConsumer(
                                vertexConsumers.getBuffer(TexturedRenderLayers.getArmorTrims(trim.getPattern().value().decal()))),
                        luz, OverlayTexture.DEFAULT_UV);
            }
            if (stack.hasGlint()) {
                modelo.render(matrices, vertexConsumers.getBuffer(RenderLayer.getArmorEntityGlint()), luz, OverlayTexture.DEFAULT_UV);
            }
        }
    }

    /** Mismo reparto que {@code ArmorFeatureRenderer#setVisible}. */
    private static void visibles(BipedEntityModel<LivingEntity> m, EquipmentSlot slot) {
        m.setVisible(false);
        switch (slot) {
            case HEAD -> { m.head.visible = true; m.hat.visible = true; }
            case CHEST -> { m.body.visible = true; m.rightArm.visible = true; m.leftArm.visible = true; }
            case LEGS -> { m.body.visible = true; m.rightLeg.visible = true; m.leftLeg.visible = true; }
            case FEET -> { m.rightLeg.visible = true; m.leftLeg.visible = true; }
            default -> { }
        }
    }

    /** Calabaza, cabezas, bloques: el ítem en la cabeza, como {@code HeadFeatureRenderer}. */
    private static void dibujarCabeza(ItemStack stack, PlayerEntityModel<LivingEntity> cuerpo, MatrixStack matrices,
                                      VertexConsumerProvider vertexConsumers, int luz, ManiquiBlockEntity be) {
        matrices.push();
        cuerpo.head.rotate(matrices);
        HeadFeatureRenderer.translate(matrices, false);
        MinecraftClient.getInstance().getItemRenderer().renderItem(stack, ModelTransformationMode.HEAD, luz,
                OverlayTexture.DEFAULT_UV, matrices, vertexConsumers, be.getWorld(), 0);
        matrices.pop();
    }

    // ── textura de la figura de maniquí ─────────────────────────────────────
    /**
     * {@code textures/entity/maniqui.png} (skin de 64x64) si existe; si no,
     * una skin lisa color madera clara generada acá, para que se vea algo
     * mientras tanto.
     */
    private static Identifier texturaManiqui() {
        if (texturaManiqui != null) return texturaManiqui;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getResourceManager().getResource(TEXTURA_MANIQUI).isPresent()) {
            texturaManiqui = TEXTURA_MANIQUI;
        } else {
            NativeImage img = new NativeImage(64, 64, true);
            for (int y = 0; y < 64; y++) {
                for (int x = 0; x < 64; x++) {
                    // Beige con una veta suave cada 4 píxeles (ABGR).
                    int r = 214, g = 188, b = 150;
                    float f = (y % 4 == 0) ? 0.94f : 1f;
                    img.setColor(x, y, 0xFF000000 | ((int) (b * f) << 16) | ((int) (g * f) << 8) | (int) (r * f));
                }
            }
            client.getTextureManager().registerTexture(TEXTURA_MANIQUI_GENERADA, new NativeImageBackedTexture(img));
            texturaManiqui = TEXTURA_MANIQUI_GENERADA;
        }
        return texturaManiqui;
    }

    /** Igual que {@code GeoBlockRenderer#rotateBlock} para los 4 horizontales. */
    private static float giroPorFacing(ManiquiBlockEntity be) {
        var estado = be.getCachedState();
        if (!estado.contains(ManiquiBlock.FACING)) return 0f;
        Direction facing = estado.get(ManiquiBlock.FACING);
        return switch (facing) {
            case SOUTH -> 180f;
            case WEST -> 90f;
            case EAST -> 270f;
            default -> 0f;
        };
    }
}
