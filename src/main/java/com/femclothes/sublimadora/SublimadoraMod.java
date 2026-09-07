package com.femclothes.sublimadora;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroups;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

public class SublimadoraMod implements ModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("femclothes");

    @Override
    public void onInitialize() {
        // Sello de build, escrito por processResources. Comparado contra
        // build/resources/main/sublimadora_build.txt contesta sin ambiguedad
        // si el cliente que esta corriendo trae los ultimos cambios.
        LOGGER.info("FemClothes cargado - build {}", sello());

        ModBlocks.register();
        ModItems.register();
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL)
                .register(entries -> {
                    entries.add(ModBlocks.SUBLIMADORA_ITEM);
                    entries.add(ModItems.REMERA);
                    entries.add(ModItems.MOLDE_LARGO_CROP);
                    entries.add(ModItems.MOLDE_LARGO_NORMAL);
                    entries.add(ModItems.MOLDE_LARGO_LARGO);
                    entries.add(ModItems.MOLDE_MANGA);
                    entries.add(ModItems.MOLDE_CUELLO);
                });
    }

    private static String sello() {
        try (InputStream in = SublimadoraMod.class.getResourceAsStream("/build.txt")) {
            return in == null ? "sin sello" : new String(in.readAllBytes()).trim();
        } catch (Exception e) {
            return "sin sello: " + e;
        }
    }
}
