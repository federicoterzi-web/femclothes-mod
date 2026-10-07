package com.femclothes.ropa;

import com.femclothes.screen.FemclothesScreenHandlers;
import com.mojang.datafixers.util.Pair;
import dev.emi.trinkets.SurvivalTrinketSlot;
import dev.emi.trinkets.api.SlotGroup;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;

import java.util.Map;

/**
 * Pantalla de Ropa (2026-10-05, "limpiar la gui de tanto ruido... un boton de ropa que te lleve a una gui
 * especifica con los slots de ropa"): los slots de Trinkets del mod, los 4 de armadura real, los 4 cosméticos y el
 * inventario. Los botones de ver cosmético / ocultar son {@code onButtonClick}: 0..3 ver, 4..7 ocultar.
 */
public class RopaScreenHandler extends ScreenHandler {

    /** Los slots de Trinkets que son del mod (grupo, slot): los mismos se esconden del inventario de siempre. */
    public static final String[][] SLOTS_DEL_MOD = {
            {"head", "sombrero"}, {"head", "choker"}, {"espalda", "capa"},
            {"torso", "chaqueta"}, {"torso", "prenda"}, {"torso", "cinto"},
            {"arms", "armwarmer"}, {"piernas", "exterior"}, {"socks", "pair"},
    };

    public static boolean esDelMod(String grupo, String slot) {
        for (String[] s : SLOTS_DEL_MOD) if (s[0].equals(grupo) && s[1].equals(slot)) return true;
        return false;
    }

    /** Medidas de la pantalla (px). */
    public static final int ANCHO = 284, ALTO = 238;
    public static final int X_MOD = 74, Y_MOD = 26, PASO_FILA = 20;
    /** El cinto va solo, abajo del muñeco (2026-10-06): no entra en las filas de a 3. */
    public static final int X_CINTO = 30, Y_CINTO = 130;
    public static final int X_REAL = 150, X_COSM = 172, Y_ARMADURA = 26, PASO_ARMADURA = 22;
    public static final int X_INV = 61, Y_INV = 152;

    private static final Identifier[] FONDO_ARMADURA = {
            PlayerScreenHandler.EMPTY_HELMET_SLOT_TEXTURE, PlayerScreenHandler.EMPTY_CHESTPLATE_SLOT_TEXTURE,
            PlayerScreenHandler.EMPTY_LEGGINGS_SLOT_TEXTURE, PlayerScreenHandler.EMPTY_BOOTS_SLOT_TEXTURE};

    private final PlayerEntity jugador;
    /** Cuántos slots hay antes del inventario: para el shift-click. */
    private int hastaArmadura;
    private int inicioInventario;

    public RopaScreenHandler(int syncId, PlayerInventory inv) {
        super(FemclothesScreenHandlers.ROPA, syncId);
        this.jugador = inv.player;

        int celda = 0, filaGrupo = 1;
        var comp = TrinketsApi.getTrinketComponent(jugador);
        if (comp.isPresent()) {
            Map<String, SlotGroup> grupos = TrinketsApi.getPlayerSlots(jugador);
            for (String[] s : SLOTS_DEL_MOD) {
                Map<String, TrinketInventory> delGrupo = comp.get().getInventory().get(s[0]);
                TrinketInventory ti = delGrupo == null ? null : delGrupo.get(s[1]);
                SlotGroup grupo = grupos.get(s[0]);
                if (ti == null || grupo == null) continue;
                // Acomodo (2026-10-06, "la mitad de los casilleros de ropa me aparecen fusionados con el inventario"): con
                // 3 slots por prenda, cada prenda ocupa una fila de 3; sombrero, gargantilla y capa comparten la primera
                // y el cinto va solo debajo del muñeco.
                boolean superior = s[1].equals("sombrero") || s[1].equals("choker") || s[1].equals("capa");
                boolean cinto = s[1].equals("cinto");
                for (int i = 0; i < ti.size(); i++) {
                    int x, y;
                    if (cinto) { x = X_CINTO; y = Y_CINTO; }
                    else if (superior) { x = X_MOD + (celda++ % 3) * 22; y = Y_MOD; }
                    else { x = X_MOD + (i % 3) * 22; y = Y_MOD + (filaGrupo + i / 3) * PASO_FILA; }
                    addSlot(new SurvivalTrinketSlot(ti, i, x, y, grupo, ti.getSlotType(), i, true));
                }
                if (!superior && !cinto) filaGrupo += (ti.size() + 2) / 3;
            }
        }

        EquipmentSlot[] reales = Cosmeticos.ZONAS;
        for (int z = 0; z < 4; z++) {
            int zona = z;
            int y = Y_ARMADURA + z * PASO_ARMADURA;
            addSlot(new Slot(inv, 39 - z, X_REAL, y) {
                @Override public int getMaxItemCount() { return 1; }

                @Override
                public boolean canInsert(ItemStack stack) { return jugador.getPreferredEquipmentSlot(stack) == reales[zona]; }

                @Override
                public boolean canTakeItems(PlayerEntity player) {
                    ItemStack s = getStack();
                    return (s.isEmpty() || player.isCreative() || !net.minecraft.enchantment.EnchantmentHelper.hasAnyEnchantmentsWith(
                            s, net.minecraft.component.EnchantmentEffectComponentTypes.PREVENT_ARMOR_CHANGE)) && super.canTakeItems(player);
                }

                @Override
                public Pair<Identifier, Identifier> getBackgroundSprite() {
                    return Pair.of(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, FONDO_ARMADURA[zona]);
                }
            });
        }
        hastaArmadura = slots.size();

        CosmeticosInventario cosm = new CosmeticosInventario(jugador);
        for (int z = 0; z < 4; z++) {
            int zona = z;
            addSlot(new Slot(cosm, z, X_COSM, Y_ARMADURA + z * PASO_ARMADURA) {
                @Override public int getMaxItemCount() { return 1; }

                @Override
                public boolean canInsert(ItemStack stack) { return cosm.isValid(zona, stack); }

                @Override
                public Pair<Identifier, Identifier> getBackgroundSprite() {
                    return Pair.of(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, FONDO_ARMADURA[zona]);
                }
            });
        }

        inicioInventario = slots.size();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) addSlot(new Slot(inv, j + i * 9 + 9, X_INV + j * 18, Y_INV + i * 18));
        }
        for (int i = 0; i < 9; i++) addSlot(new Slot(inv, i, X_INV + i * 18, Y_INV + 58));
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        Cosmeticos c = Cosmeticos.de(player);
        if (id >= 0 && id < 4) {
            Cosmeticos.guardar(player, c.alternarVer(id));
            return true;
        }
        if (id >= 4 && id < 8) {
            Cosmeticos.guardar(player, c.alternarOcultar(id - 4));
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        Slot s = slots.get(index);
        if (s == null || !s.hasStack()) return ItemStack.EMPTY;
        ItemStack stack = s.getStack();
        ItemStack copia = stack.copy();
        if (index < inicioInventario) {
            if (!insertItem(stack, inicioInventario, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!insertItem(stack, 0, hastaArmadura, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) s.setStack(ItemStack.EMPTY);
        else s.markDirty();
        return copia;
    }

    @Override
    public boolean canUse(PlayerEntity player) { return true; }
}
