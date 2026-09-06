package com.femclothes.sublimadora;

import com.femclothes.Femclothes;
import com.mojang.serialization.Codec;
import net.minecraft.component.ComponentType;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

/**
 * La remera: en blanco al craftearla, estampada despues de pasar por la
 * sublimadora.
 *
 * Es UN solo item, no dos. Lo que cambia son los componentes de estampa: sin
 * ninguno esta en blanco, y puede tener frente, espalda o las dos. Se guarda
 * el UUID de la foto y no la imagen, que es lo que hace que esto escale — la
 * imagen ya vive en el almacenamiento de Camerapture, y el ItemStack pesa lo
 * mismo con o sin estampa.
 */
public final class ModItems {

    /** Foto estampada en el frente. Ausente = ese lado en blanco. */
    public static final ComponentType<Estampa> ESTAMPA_FRENTE = registrarEstampa("estampa_frente");
    /** Idem en la espalda. Estampar las dos caras cuesta dos pasadas. */
    public static final ComponentType<Estampa> ESTAMPA_ESPALDA = registrarEstampa("estampa_espalda");

    /**
     * El componente viejo, de cuando la estampa era un UUID pelado y siempre
     * iba centrada en el frente. Sigue registrado para poder leer las remeras
     * que ya existen en mundos guardados; nada lo escribe.
     */
    public static final ComponentType<UUID> PICTURE_ID = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(Femclothes.MOD_ID, "picture_id"),
            ComponentType.<UUID>builder()
                    .codec(Uuids.CODEC)
                    .packetCodec(Uuids.PACKET_CODEC)
                    .build());

    /**
     * Las cuatro cargas de tinta, para que viajen adentro del item cuando se
     * levanta la maquina. Cuatro enteros en orden C, M, Y, K.
     *
     * Es un componente y no NBT crudo porque asi lo copia la loot table sola
     * con minecraft:copy_components, sin codigo de por medio, y ademas se ve
     * en el tooltip como cualquier otro dato del item.
     */
    public static final ComponentType<java.util.List<Integer>> CARGAS = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(Femclothes.MOD_ID, "cargas"),
            ComponentType.<java.util.List<Integer>>builder()
                    .codec(Codec.INT.listOf())
                    .packetCodec(net.minecraft.network.codec.PacketCodecs.INTEGER
                            .collect(net.minecraft.network.codec.PacketCodecs.toList()))
                    .build());

    /** Que corte de prenda es. Ausente = remera comun. */
    public static final ComponentType<Variante> VARIANTE = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(Femclothes.MOD_ID, "variante"),
            ComponentType.<Variante>builder()
                    .codec(Variante.CODEC)
                    .packetCodec(Variante.PACKET_CODEC)
                    .build());

    public static final RemeraItem REMERA = Registry.register(
            Registries.ITEM,
            Identifier.of(Femclothes.MOD_ID, "remera"),
            new RemeraItem(new Item.Settings().maxCount(16)));

    /**
     * Si la sublimadora puede imprimir sobre esto.
     *
     * Vive aca y no en EstampaTextures porque el bloque lo pregunta en el
     * servidor, y EstampaTextures es codigo de cliente: tocarlo del lado del
     * servidor cargaria NativeImage y MinecraftClient en un dedicado.
     */
    public static boolean esEstampable(net.minecraft.item.ItemStack stack) {
        return stack.getItem() == REMERA
                || stack.getItem() == com.femclothes.item.FemclothesItems.SOCKS_SOLID;
    }

    private static ComponentType<Estampa> registrarEstampa(String nombre) {
        return Registry.register(
                Registries.DATA_COMPONENT_TYPE,
                Identifier.of(Femclothes.MOD_ID, nombre),
                ComponentType.<Estampa>builder()
                        .codec(Estampa.CODEC)
                        .packetCodec(Estampa.PACKET_CODEC)
                        .build());
    }

    /**
     * Los tres moldes del telar, uno por eje del corte.
     *
     * Tres y no diez -uno por valor- porque ciclan: pasar el molde de mangas
     * lleva de sin a cortas, de cortas a tres cuartos, y asi. Con 36
     * combinaciones, un item por valor es ruido en el inventario y en las
     * recetas.
     */
    public static final MoldeItem MOLDE_LARGO = molde("largo", MoldeItem.Eje.LARGO);
    public static final MoldeItem MOLDE_MANGA = molde("manga", MoldeItem.Eje.MANGA);
    public static final MoldeItem MOLDE_CUELLO = molde("cuello", MoldeItem.Eje.CUELLO);

    private static MoldeItem molde(String nombre, MoldeItem.Eje eje) {
        return Registry.register(Registries.ITEM,
                Identifier.of(Femclothes.MOD_ID, "molde_" + nombre),
                new MoldeItem(new Item.Settings().maxCount(1), eje));
    }

    public static void register() {
        // fuerza class-loading
    }

    private ModItems() {}
}
