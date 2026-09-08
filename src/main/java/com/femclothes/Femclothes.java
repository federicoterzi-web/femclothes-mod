package com.femclothes;

import com.femclothes.body.ComandoCuerpo;
import com.femclothes.body.PerfilesDeCuerpo;
import com.femclothes.garment.PrendasDelMod;
import com.femclothes.item.FemclothesComponents;
import com.femclothes.item.FemclothesItems;
import com.femclothes.screen.FemclothesScreenHandlers;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroups;

public class Femclothes implements ModInitializer {

    public static final String MOD_ID = "femclothes";

    @Override
    public void onInitialize() {
        FemclothesItems.init();
        FemclothesComponents.init();
        // Que items son prendas del sistema de capas. Va DESPUES de los
        // items: registra por instancia, no por id.
        PrendasDelMod.init();
        PerfilesDeCuerpo.init();
        ComandoCuerpo.init();
        ClothingLoomInteraction.init();
        FemclothesScreenHandlers.init();
        registrarPestanaCreativa();
    }

    /**
     * Sin esto, la ÚNICA forma de conseguir cualquiera de estos ítems era
     * ya saber la receta de memoria — ninguno aparecía en el buscador
     * creativo. Se notó recién con calientabrazos ("me faltan"), pero el
     * agujero es de TODO `FemclothesItems` (pantalón, medias, los 16 moldes,
     * los 3 patrones): `SublimadoraMod` solo agrega los ítems de su propio
     * paquete (remera + sus 2 moldes cíclicos).
     */
    private static void registrarPestanaCreativa() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> {
            entries.add(FemclothesItems.SOCKS_34);
            entries.add(FemclothesItems.SOCKS_SOLID);
            entries.add(FemclothesItems.FISHNET_SOCKS);
            entries.add(FemclothesItems.PANTALON);
            entries.add(FemclothesItems.MOLDE_PANTALON_PANTALON);
            entries.add(FemclothesItems.MOLDE_PANTALON_TRES_CUARTOS);
            entries.add(FemclothesItems.MOLDE_PANTALON_BERMUDAS);
            entries.add(FemclothesItems.MOLDE_PANTALON_SHORTS);
            entries.add(FemclothesItems.MOLDE_PANTALON_ROPA_INTERIOR);
            entries.add(FemclothesItems.MOLDE_TIRO_CORTO);
            entries.add(FemclothesItems.MOLDE_TIRO_MEDIO);
            entries.add(FemclothesItems.MOLDE_TIRO_LARGO);
            entries.add(FemclothesItems.MOLDE_MEDIA_ZOQUETES);
            entries.add(FemclothesItems.MOLDE_MEDIA_MEDIAS);
            entries.add(FemclothesItems.MOLDE_MEDIA_RODILLA);
            entries.add(FemclothesItems.MOLDE_MEDIA_TRES_CUARTOS);
            entries.add(FemclothesItems.MOLDE_MEDIA_CANCAN);
            entries.add(FemclothesItems.CALIENTABRAZOS);
            entries.add(FemclothesItems.MAID_OUTFIT);
            entries.add(FemclothesItems.OVERSIZED_HOODIE);
            entries.add(FemclothesItems.PATTERN_STRIPE_TOP);
            entries.add(FemclothesItems.PATTERN_STRIPE_ALT);
            entries.add(FemclothesItems.PATTERN_TRIPLE_STRIPE);
        });
    }
}
