package com.femclothes.modelado;

import com.femclothes.Femclothes;
import com.femclothes.item.Botamanga;
import com.femclothes.item.Calce;
import com.femclothes.item.FemclothesComponents;
import com.femclothes.item.PantalonTiro;
import com.femclothes.item.PatronRed;
import com.femclothes.sublimadora.Variante;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

/** Registro de la Mesa de Modelado: bloque, item, block entity type, molde de corte. */
public final class ModeladoMod {

    public static final ModeladoBlock MODELADO_BLOCK = new ModeladoBlock(
            AbstractBlock.Settings.create()
                    .strength(2.5f, 4.0f)
                    .sounds(BlockSoundGroup.WOOD)
                    // El bloque es INVISIBLE (GeckoLib lo dibuja desde el
                    // block entity) — sin esto vanilla lo trata como cubo
                    // opaco sólido y cullea la cara del bloque de abajo,
                    // que se ve como si el modelo se "comiera" esa cara.
                    .nonOpaque());

    public static final ModeladoBlockItem MODELADO_BLOCK_ITEM = new ModeladoBlockItem(MODELADO_BLOCK, new Item.Settings());

    public static final BlockEntityType<ModeladoBlockEntity> MODELADO_BLOCK_ENTITY =
            BlockEntityType.Builder.create(ModeladoBlockEntity::new, MODELADO_BLOCK).build();

    public static final MoldeDeCorteItem MOLDE_DE_CORTE = new MoldeDeCorteItem(new Item.Settings().maxCount(16));

    // ── molde de rango unificado (a pedido) ─────────────────────────────
    // Un solo set de 5, para las 4 categorías de extremidad — ver
    // MoldeRangoItem y ModeladoBlockEntity#fijar (traduce el rango a la
    // escala real de cada prenda). Anclaje/Lado aplican normal.

    public static final MoldeRangoItem MOLDE_RANGO_MINIMO = new MoldeRangoItem(new Item.Settings().maxCount(1), MoldeRangoItem.Rango.MINIMO);
    public static final MoldeRangoItem MOLDE_RANGO_CORTO = new MoldeRangoItem(new Item.Settings().maxCount(1), MoldeRangoItem.Rango.CORTO);
    public static final MoldeRangoItem MOLDE_RANGO_MEDIO = new MoldeRangoItem(new Item.Settings().maxCount(1), MoldeRangoItem.Rango.MEDIO);
    public static final MoldeRangoItem MOLDE_RANGO_MEDIOLARGO = new MoldeRangoItem(new Item.Settings().maxCount(1), MoldeRangoItem.Rango.MEDIOLARGO);
    public static final MoldeRangoItem MOLDE_RANGO_LARGO = new MoldeRangoItem(new Item.Settings().maxCount(1), MoldeRangoItem.Rango.LARGO);
    public static final MoldeRangoItem MOLDE_RANGO_MAXIMO = new MoldeRangoItem(new Item.Settings().maxCount(1), MoldeRangoItem.Rango.MAXIMO);

    // ── molde de torso unificado (a pedido) ─────────────────────────────
    // Un solo set de 3, para largo de remera y tiro de pantalón — mismo
    // espíritu que MOLDE_RANGO_* pero para los dos ejes de TORSO (un solo
    // valor, sin anclaje ni lado). Ver MoldeTorsoItem.

    public static final MoldeTorsoItem MOLDE_TORSO_CORTO = new MoldeTorsoItem(new Item.Settings().maxCount(1), MoldeTorsoItem.Rango.CORTO);
    public static final MoldeTorsoItem MOLDE_TORSO_MEDIO = new MoldeTorsoItem(new Item.Settings().maxCount(1), MoldeTorsoItem.Rango.MEDIO);
    public static final MoldeTorsoItem MOLDE_TORSO_LARGO = new MoldeTorsoItem(new Item.Settings().maxCount(1), MoldeTorsoItem.Rango.LARGO);

    // ── molde de calce (a pedido 2026-09-15) ────────────────────────────
    // Transversal a las 4 categorías (remera/pantalón/medias/
    // calientabrazos) — no recorta filas, cambia la dilatación de la
    // geometría 3D (ver Calce, CuerpoGeometria).

    public static final MoldeCalceItem MOLDE_CALCE_PEGADO = new MoldeCalceItem(new Item.Settings().maxCount(1), Calce.PEGADO);
    public static final MoldeCalceItem MOLDE_CALCE_AJUSTADO = new MoldeCalceItem(new Item.Settings().maxCount(1), Calce.AJUSTADO);
    public static final MoldeCalceItem MOLDE_CALCE_NORMAL = new MoldeCalceItem(new Item.Settings().maxCount(1), Calce.NORMAL);
    public static final MoldeCalceItem MOLDE_CALCE_SUELTO = new MoldeCalceItem(new Item.Settings().maxCount(1), Calce.SUELTO);
    public static final MoldeCalceItem MOLDE_CALCE_OVERSIZE = new MoldeCalceItem(new Item.Settings().maxCount(1), Calce.OVERSIZE);

    // ── molde de red (a pedido 2026-09-20) ──────────────────────────────
    // Transversal a las 4 categorías, mismo criterio que el molde de calce
    // — no recorta filas ni cambia geometría, perfora la textura ya
    // compuesta (ver PatronRed, ClothingTextureCache#perforarRed).

    public static final MoldeRedItem MOLDE_RED_FINA = new MoldeRedItem(new Item.Settings().maxCount(1), PatronRed.FINA);
    public static final MoldeRedItem MOLDE_RED_GRUESA = new MoldeRedItem(new Item.Settings().maxCount(1), PatronRed.GRUESA);
    /** Revierte la red: deja la textura lisa (2026-09-26, "un molde de textura lisa para revertir el media red"). */
    public static final MoldeRedItem MOLDE_RED_LISA = new MoldeRedItem(new Item.Settings().maxCount(1), PatronRed.LISA);
    /** Panal de abejas (2026-09-28, "media red hexagonal"). */
    public static final MoldeRedItem MOLDE_RED_HEXAGONAL = new MoldeRedItem(new Item.Settings().maxCount(1), PatronRed.HEXAGONAL);
    /** Agujeritos redondos en tresbolillo, tipo broderie (2026-09-28). */
    public static final MoldeRedItem MOLDE_RED_PERFORADA = new MoldeRedItem(new Item.Settings().maxCount(1), PatronRed.PERFORADA);
    /** Encaje de rombos con punto central (2026-09-28). */
    public static final MoldeRedItem MOLDE_RED_ENCAJE = new MoldeRedItem(new Item.Settings().maxCount(1), PatronRed.ENCAJE);
    /** Rayas caladas horizontales (2026-09-28). */
    public static final MoldeRedItem MOLDE_RED_RAYAS = new MoldeRedItem(new Item.Settings().maxCount(1), PatronRed.RAYAS);
    /** Cuadrícula recta estilo escocés (2026-09-28). */
    public static final MoldeRedItem MOLDE_RED_ESCOCESA = new MoldeRedItem(new Item.Settings().maxCount(1), PatronRed.ESCOCESA);
    /** Arnés cruzado en X (2026-09-28). */
    public static final MoldeRedItem MOLDE_RED_ARNES_X = new MoldeRedItem(new Item.Settings().maxCount(1), PatronRed.ARNES_X);
    /** Arnés de tirantes (2026-09-28). */
    public static final MoldeRedItem MOLDE_RED_ARNES_TIRANTES = new MoldeRedItem(new Item.Settings().maxCount(1), PatronRed.ARNES_TIRANTES);
    /** Arnés de bandas (2026-09-28). */
    public static final MoldeRedItem MOLDE_RED_ARNES_BANDAS = new MoldeRedItem(new Item.Settings().maxCount(1), PatronRed.ARNES_BANDAS);

    public static void register() {
        Identifier bloqueId = Identifier.of(Femclothes.MOD_ID, "modelado");
        Registry.register(Registries.BLOCK, bloqueId, MODELADO_BLOCK);
        Registry.register(Registries.ITEM, bloqueId, MODELADO_BLOCK_ITEM);
        Registry.register(Registries.BLOCK_ENTITY_TYPE, bloqueId, MODELADO_BLOCK_ENTITY);
        Registry.register(Registries.ITEM, Identifier.of(Femclothes.MOD_ID, "molde_de_corte"), MOLDE_DE_CORTE);


        Registry.register(Registries.ITEM, Identifier.of(Femclothes.MOD_ID, "molde_rango_minimo"), MOLDE_RANGO_MINIMO);
        Registry.register(Registries.ITEM, Identifier.of(Femclothes.MOD_ID, "molde_rango_corto"), MOLDE_RANGO_CORTO);
        Registry.register(Registries.ITEM, Identifier.of(Femclothes.MOD_ID, "molde_rango_medio"), MOLDE_RANGO_MEDIO);
        Registry.register(Registries.ITEM, Identifier.of(Femclothes.MOD_ID, "molde_rango_mediolargo"), MOLDE_RANGO_MEDIOLARGO);
        Registry.register(Registries.ITEM, Identifier.of(Femclothes.MOD_ID, "molde_rango_largo"), MOLDE_RANGO_LARGO);
        Registry.register(Registries.ITEM, Identifier.of(Femclothes.MOD_ID, "molde_rango_maximo"), MOLDE_RANGO_MAXIMO);

        Registry.register(Registries.ITEM, Identifier.of(Femclothes.MOD_ID, "molde_torso_corto"), MOLDE_TORSO_CORTO);
        Registry.register(Registries.ITEM, Identifier.of(Femclothes.MOD_ID, "molde_torso_medio"), MOLDE_TORSO_MEDIO);
        Registry.register(Registries.ITEM, Identifier.of(Femclothes.MOD_ID, "molde_torso_largo"), MOLDE_TORSO_LARGO);

        Registry.register(Registries.ITEM, Identifier.of(Femclothes.MOD_ID, "molde_calce_pegado"), MOLDE_CALCE_PEGADO);
        Registry.register(Registries.ITEM, Identifier.of(Femclothes.MOD_ID, "molde_calce_ajustado"), MOLDE_CALCE_AJUSTADO);
        Registry.register(Registries.ITEM, Identifier.of(Femclothes.MOD_ID, "molde_calce_normal"), MOLDE_CALCE_NORMAL);
        Registry.register(Registries.ITEM, Identifier.of(Femclothes.MOD_ID, "molde_calce_suelto"), MOLDE_CALCE_SUELTO);
        Registry.register(Registries.ITEM, Identifier.of(Femclothes.MOD_ID, "molde_calce_oversize"), MOLDE_CALCE_OVERSIZE);

        Registry.register(Registries.ITEM, Identifier.of(Femclothes.MOD_ID, "molde_red_fina"), MOLDE_RED_FINA);
        Registry.register(Registries.ITEM, Identifier.of(Femclothes.MOD_ID, "molde_red_gruesa"), MOLDE_RED_GRUESA);
        Registry.register(Registries.ITEM, Identifier.of(Femclothes.MOD_ID, "molde_red_lisa"), MOLDE_RED_LISA);
        Registry.register(Registries.ITEM, Identifier.of(Femclothes.MOD_ID, "molde_red_hexagonal"), MOLDE_RED_HEXAGONAL);
        Registry.register(Registries.ITEM, Identifier.of(Femclothes.MOD_ID, "molde_red_perforada"), MOLDE_RED_PERFORADA);
        Registry.register(Registries.ITEM, Identifier.of(Femclothes.MOD_ID, "molde_red_encaje"), MOLDE_RED_ENCAJE);
        Registry.register(Registries.ITEM, Identifier.of(Femclothes.MOD_ID, "molde_red_rayas"), MOLDE_RED_RAYAS);
        Registry.register(Registries.ITEM, Identifier.of(Femclothes.MOD_ID, "molde_red_escocesa"), MOLDE_RED_ESCOCESA);
        Registry.register(Registries.ITEM, Identifier.of(Femclothes.MOD_ID, "molde_red_arnes_x"), MOLDE_RED_ARNES_X);
        Registry.register(Registries.ITEM, Identifier.of(Femclothes.MOD_ID, "molde_red_arnes_tirantes"), MOLDE_RED_ARNES_TIRANTES);
        Registry.register(Registries.ITEM, Identifier.of(Femclothes.MOD_ID, "molde_red_arnes_bandas"), MOLDE_RED_ARNES_BANDAS);

        // "Guardar diseño" con nombre (2026-09-27): el nombre viaja como paquete propio, ver GuardarDisenoPayload.
        PayloadTypeRegistry.playC2S().register(GuardarDisenoPayload.ID, GuardarDisenoPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(GuardarDisenoPayload.ID, (payload, context) ->
                context.server().execute(() -> {
                    if (context.player().getWorld().getBlockEntity(payload.pos()) instanceof ModeladoBlockEntity be
                            && be.canPlayerUse(context.player())) {
                        be.guardarDiseno(payload.nombre());
                    }
                }));
    }

    private ModeladoMod() {}
}
