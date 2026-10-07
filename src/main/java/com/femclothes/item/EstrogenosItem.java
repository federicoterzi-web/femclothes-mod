package com.femclothes.item;

import com.femclothes.body.PerfilCuerpo;
import com.femclothes.body.PerfilesDeCuerpo;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
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
 * generara pechos de distintos tamaños en el cuerpo" → "3 tamaños de mamas
 * con fisicas estilo resorte a traves de la acumulacion de tres ingestas y
 * que los efectos duren 24 hs"): se toma como una poción; cada dosis suma un
 * talle (chico, mediano, grande) hasta {@link PerfilCuerpo#BUSTO_MAXIMO} y
 * deja el efecto por {@link PerfilCuerpo#DURACION_ESTROGENOS} desde la última
 * toma (tomar con el talle máximo solo renueva el tiempo). Al vencer, el busto
 * vuelve al del cuerpo ({@code PerfilesDeCuerpo#vencerEstrogenos}). Lo dibuja
 * el relieve, con el rebote de {@code render.relieve.FisicaBusto}.
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
        return ItemUsage.consumeHeldItem(world, user, hand);
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (world instanceof ServerWorld servidor && user instanceof PlayerEntity jugador) {
            PerfilCuerpo perfil = PerfilesDeCuerpo.de(jugador);
            int dosis = Math.min(PerfilCuerpo.BUSTO_MAXIMO, perfil.busto() + 1);
            long hasta = System.currentTimeMillis() + PerfilCuerpo.DURACION_ESTROGENOS;
            PerfilesDeCuerpo.poner(jugador, perfil.conBusto(dosis, hasta));
            jugador.sendMessage(Text.translatable(perfil.busto() >= PerfilCuerpo.BUSTO_MAXIMO
                    ? "femclothes.estrogenos.renovado" : "femclothes.estrogenos.dosis",
                    dosis, PerfilCuerpo.BUSTO_MAXIMO), true);
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
