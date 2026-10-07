package com.modamod.mixin;

import dev.emi.trinkets.TrinketSlot;
import dev.emi.trinkets.api.SlotType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Info al pasar el mouse por un slot VACÍO de ropa del mod (2026-09-30,
 * "agregale info en hover"): Trinkets no muestra nada sobre un slot vacío.
 * Nombre del slot, qué prendas van y en qué capa, cuántos lugares tiene y
 * que se trabajan en las máquinas. Solo para los slots que tienen texto
 * {@code modamod.slot.<grupo>.<slot>} — los de otros mods no se tocan.
 */
@Mixin(HandledScreen.class)
public abstract class HandledScreenSlotInfoMixin extends Screen {

    @Shadow protected Slot focusedSlot;
    @Shadow @Final protected ScreenHandler handler;

    protected HandledScreenSlotInfoMixin(Text title) {
        super(title);
    }

    @Inject(method = "drawMouseoverTooltip", at = @At("HEAD"), cancellable = true)
    private void modamod$infoSlotDeRopa(DrawContext context, int x, int y, CallbackInfo ci) {
        if (!(focusedSlot instanceof TrinketSlot trinket) || focusedSlot.hasStack()
                || !handler.getCursorStack().isEmpty()) {
            return;
        }
        SlotType tipo = trinket.getType();
        String clave = "modamod.slot." + tipo.getGroup() + "." + tipo.getName();
        if (!I18n.hasTranslation(clave)) return;

        List<OrderedText> lineas = new ArrayList<>();
        lineas.add(tipo.getTranslation().formatted(Formatting.WHITE).asOrderedText());
        Text info = Text.translatable(clave, Text.keybind("key.modamod.capucha")).formatted(Formatting.GRAY);
        lineas.addAll(this.textRenderer.wrapLines(info, 200));
        net.minecraft.text.MutableText lugares = tipo.getAmount() > 1
                ? Text.translatable("modamod.slot.lugares", tipo.getAmount())
                : Text.translatable("modamod.slot.lugar");
        lineas.addAll(this.textRenderer.wrapLines(lugares.formatted(Formatting.DARK_AQUA), 200));
        lineas.add(Text.translatable("modamod.slot.maquinas").formatted(Formatting.DARK_GRAY).asOrderedText());
        context.drawOrderedTooltip(this.textRenderer, lineas, x, y);
        ci.cancel();
    }
}
