package com.ejemplo.sublimadora;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroups;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

public class SublimadoraMod implements ModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("sublimadora");

    @Override
    public void onInitialize() {
        // Sello de build, escrito por processResources. Comparado contra
        // build/resources/main/sublimadora_build.txt contesta sin ambiguedad
        // si el cliente que esta corriendo trae los ultimos cambios.
        LOGGER.info("Sublimadora cargada - build {}", sello());

        ModBlocks.register();
        ModItems.register();
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL)
                .register(entries -> {
                    entries.add(ModBlocks.SUBLIMADORA_ITEM);
                    entries.add(ModItems.REMERA);
                });
    }

    private static String sello() {
        try (InputStream in = SublimadoraMod.class.getResourceAsStream("/sublimadora_build.txt")) {
            return in == null ? "sin sello" : new String(in.readAllBytes()).trim();
        } catch (Exception e) {
            return "sin sello: " + e;
        }
    }
}
