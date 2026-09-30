package com.femclothes.guardarropas;

import com.femclothes.screen.FemclothesScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;

/**
 * Guardarropas: 16 slots de borrador (4 por categoría: remera/pantalón/
 * medias/calientabrazos) + inventario del jugador. Extended (con
 * BlockPos): la lista de outfits guardados no cabe en slots simples,
 * viaja por NBT del block entity — mismo motivo que
 * {@code TinturasScreenHandler}/{@code SublimadoraScreenHandler}.
 */
public class GuardarropasScreenHandler extends ScreenHandler {

    private static final int PX = 100;
    public static final int M_MEDIO = 19 + PX;
    /** X de la columna de armadura: después de las 5 categorías, con un hueco de 6. */
    public static final int X_ARMADURA = M_MEDIO + GuardarropasBlockEntity.CATEGORIAS * 20 + 6;

    private static final net.minecraft.util.Identifier[] FONDO_ARMADURA = {
            net.minecraft.screen.PlayerScreenHandler.EMPTY_HELMET_SLOT_TEXTURE,
            net.minecraft.screen.PlayerScreenHandler.EMPTY_CHESTPLATE_SLOT_TEXTURE,
            net.minecraft.screen.PlayerScreenHandler.EMPTY_LEGGINGS_SLOT_TEXTURE,
            net.minecraft.screen.PlayerScreenHandler.EMPTY_BOOTS_SLOT_TEXTURE,
    };

    /**
     * Los 4 slots de la columna de armadura (2026-09-30), para el Guardarropas
     * y el Maniquí: canInsert delega en isValid (trampa conocida, CLAUDE.md),
     * uno por ítem, y el dibujito vanilla de la pieza cuando está vacío.
     */
    public static Slot slotArmadura(net.minecraft.inventory.Inventory inv, int i) {
        int index = GuardarropasBlockEntity.ARMADURA_INICIO + i;
        return new Slot(inv, index, X_ARMADURA, 20 + i * 20) {
            @Override
            public boolean canInsert(ItemStack stack) { return inv.isValid(index, stack); }

            @Override
            public int getMaxItemCount() { return 1; }

            @Override
            public com.mojang.datafixers.util.Pair<net.minecraft.util.Identifier, net.minecraft.util.Identifier> getBackgroundSprite() {
                return com.mojang.datafixers.util.Pair.of(
                        net.minecraft.screen.PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, FONDO_ARMADURA[i]);
            }
        };
    }
    private static final int INV_START = GuardarropasBlockEntity.TAMANO;

    public final GuardarropasBlockEntity be;

    /** Factory del lado del CLIENTE: busca el block entity REAL en la posición que mandó el servidor. */
    public static GuardarropasScreenHandler deCliente(int syncId, PlayerInventory inv, BlockPos pos) {
        var client = net.minecraft.client.MinecraftClient.getInstance();
        GuardarropasBlockEntity be = client.world != null && client.world.getBlockEntity(pos) instanceof GuardarropasBlockEntity g
                ? g
                : new GuardarropasBlockEntity(pos, GuardarropasMod.GUARDARROPAS_BLOCK.getDefaultState());
        return new GuardarropasScreenHandler(syncId, inv, be);
    }

    public GuardarropasScreenHandler(int syncId, PlayerInventory playerInventory, GuardarropasBlockEntity be) {
        super(FemclothesScreenHandlers.GUARDARROPAS, syncId);
        this.be = be;
        // Abre la puerta del modelo (2026-09-30) — mismo par onOpen/onClose
        // que usan los cofres vanilla; del lado del cliente no hace nada.
        be.onOpen(playerInventory.player);

        // Slot.canInsert() da true SIEMPRE por defecto — no llama solo a
        // Inventory.isValid() (esa la usan los hoppers, no el drag-and-drop
        // de la GUI). Hay que pisarlo acá, mismo patrón que ya usa
        // ModeladoScreenHandler para su slot PRENDA — bug real jugando
        // (2026-09-20, "entra en cualquier slot sin importar el tipo").
        //
        // Grilla: una columna por categoría, POR_CATEGORIA filas — a
        // pedido (2026-09-20, "tiene q haber 4 slots por categoria"):
        // croptop + remerón largo entran juntos en la columna de remera,
        // pantalón + pollera + calza juntos en la de pantalón, etc.
        for (int categoria = 0; categoria < GuardarropasBlockEntity.CATEGORIAS; categoria++) {
            for (int capa = 0; capa < GuardarropasBlockEntity.POR_CATEGORIA; capa++) {
                int index = categoria * GuardarropasBlockEntity.POR_CATEGORIA + capa;
                addSlot(new Slot(be, index, M_MEDIO + categoria * 20, 20 + capa * 20) {
                    @Override
                    public boolean canInsert(ItemStack stack) { return be.isValid(index, stack); }
                });
            }
        }

        for (int i = 0; i < GuardarropasBlockEntity.SLOTS_ARMADURA.length; i++) addSlot(slotArmadura(be, i));

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlot(new Slot(playerInventory, j + i * 9 + 9, M_MEDIO + j * 18, 180 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(playerInventory, i, M_MEDIO + i * 18, 238));
        }
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id == GuardarropasBlockEntity.BTN_EQUIPAR) {
            be.equiparEn(player);
            return true;
        }
        return be.onButtonClick(id);
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        be.onClose(player);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return be.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        Slot clickedSlot = this.slots.get(slot);
        if (clickedSlot == null || !clickedSlot.hasStack()) return ItemStack.EMPTY;
        ItemStack stack = clickedSlot.getStack();
        ItemStack result = stack.copy();
        if (slot < INV_START) {
            if (!this.insertItem(stack, INV_START, this.slots.size(), true)) return ItemStack.EMPTY;
        } else if (!this.insertItem(stack, 0, INV_START, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) clickedSlot.setStack(ItemStack.EMPTY);
        else clickedSlot.markDirty();
        return result;
    }
}
