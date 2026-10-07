package com.femclothes.telar;

import com.femclothes.item.FemclothesItems;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.DyeColor;

import java.util.function.Supplier;

/**
 * Las prendas básicas que teje el Telar automático (2026-10-07, "produzca prendas basicas a base de lana e hilo";
 * charlado: remera, pantalón, medias, calientabrazos, pollera y capa, sin hoodie). Nuevos valores siempre al FINAL
 * (viajan por la red por ordinal en la pantalla).
 */
public enum TelarPrenda {
    REMERA(() -> com.femclothes.sublimadora.ModItems.REMERA, 4, 2),
    PANTALON(() -> FemclothesItems.PANTALON, 5, 2),
    MEDIAS(() -> FemclothesItems.SOCKS_SOLID, 2, 1),
    CALIENTABRAZOS(() -> FemclothesItems.CALIENTABRAZOS, 2, 1),
    POLLERA(() -> FemclothesItems.POLLERA, 3, 2),
    CAPA(() -> FemclothesItems.CAPA, 6, 3);

    private final Supplier<? extends Item> item;
    /** Cuántas lanas (bloques) y cuántos hilos gasta cada prenda. */
    public final int lana, hilo;

    TelarPrenda(Supplier<? extends Item> item, int lana, int hilo) {
        this.item = item;
        this.lana = lana;
        this.hilo = hilo;
    }

    public Item item() { return item.get(); }

    public String clave() { return name().toLowerCase(); }

    /** Las 16 lanas en el orden de {@link DyeColor}. */
    private static final Item[] LANAS = {
            Items.WHITE_WOOL, Items.ORANGE_WOOL, Items.MAGENTA_WOOL, Items.LIGHT_BLUE_WOOL, Items.YELLOW_WOOL,
            Items.LIME_WOOL, Items.PINK_WOOL, Items.GRAY_WOOL, Items.LIGHT_GRAY_WOOL, Items.CYAN_WOOL,
            Items.PURPLE_WOOL, Items.BLUE_WOOL, Items.BROWN_WOOL, Items.GREEN_WOOL, Items.RED_WOOL, Items.BLACK_WOOL};

    public static boolean esLana(Item item) {
        for (Item l : LANAS) if (l == item) return true;
        return false;
    }

    /** El color de la prenda que sale de esa lana (la blanca queda blanca de verdad); -1 si no es lana. */
    public static int colorDeLana(Item item) {
        for (int i = 0; i < LANAS.length; i++) {
            if (LANAS[i] == item) return i == 0 ? 0xFFFFFF : DyeColor.byId(i).getFireworkColor();
        }
        return -1;
    }
}
