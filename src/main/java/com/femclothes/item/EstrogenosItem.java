package com.femclothes.item;

import com.femclothes.body.PerfilCuerpo;
import com.femclothes.body.PerfilesDeCuerpo;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

import java.util.List;

/**
 * Estrógenos (2026-10-01, "quiero agregar un item llamado estrogenos que
 * generara pechos de distintos tamaños en el cuerpo"): cada dosis (se toma
 * como una poción) sube un talle el busto del perfil de cuerpo, hasta
 * {@link PerfilCuerpo#BUSTO_MAXIMO}, sobre el busto que ya traiga el cuerpo.
 * Lo dibuja el relieve ({@code render.relieve.RelieveCuerpo}); con un binder
 * se ve aplanado. {@code /femclothes busto <n>} lo fija a mano.
 */
public class EstrogenosItem extends Item {

    public EstrogenosItem(Settings settings) {
        super(settings);
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.DRINK;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 32;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (PerfilesDeCuerpo.de(user).busto() >= PerfilCuerpo.BUSTO_MAXIMO) {
            if (!world.isClient) user.sendMessage(Text.translatable("femclothes.estrogenos.maximo"), true);
            return TypedActionResult.fail(user.getStackInHand(hand));
        }
        return ItemUsage.consumeHeldItem(world, user, hand);
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (!world.isClient && user instanceof PlayerEntity jugador) {
            PerfilCuerpo perfil = PerfilesDeCuerpo.de(jugador);
            int nuevo = Math.min(PerfilCuerpo.BUSTO_MAXIMO, perfil.busto() + 1);
            PerfilesDeCuerpo.poner(jugador, perfil.conBusto(nuevo));
            jugador.sendMessage(Text.translatable("femclothes.estrogenos.dosis", nuevo, PerfilCuerpo.BUSTO_MAXIMO), true);
            world.playSound(null, jugador.getX(), jugador.getY(), jugador.getZ(),
                    SoundEvents.ITEM_HONEY_BOTTLE_DRINK, SoundCategory.PLAYERS, 0.8f, 1.2f);
            stack.decrementUnlessCreative(1, jugador);
        }
        return stack;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("femclothes.estrogenos.tooltip").formatted(Formatting.GRAY));
    }
}
