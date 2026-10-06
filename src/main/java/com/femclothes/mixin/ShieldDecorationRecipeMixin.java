package com.femclothes.mixin;

import com.femclothes.sublimadora.ModItems;
import net.minecraft.item.BannerItem;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.ShieldDecorationRecipe;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Escudo + banner con foto (2026-10-06, "ya hay una forma vanilla de aplicar banners a los escudos"): el escudo hereda la foto. */
@Mixin(ShieldDecorationRecipe.class)
public abstract class ShieldDecorationRecipeMixin {

    @Inject(method = "craft(Lnet/minecraft/recipe/input/CraftingRecipeInput;Lnet/minecraft/registry/RegistryWrapper$WrapperLookup;)Lnet/minecraft/item/ItemStack;",
            at = @At("RETURN"))
    private void femclothes$conFoto(CraftingRecipeInput input, RegistryWrapper.WrapperLookup registros, CallbackInfoReturnable<ItemStack> cir) {
        for (int i = 0; i < input.getSize(); i++) {
            ItemStack s = input.getStackInSlot(i);
            if (s.getItem() instanceof BannerItem && s.contains(ModItems.BANNER_ESTAMPA)) {
                cir.getReturnValue().set(ModItems.BANNER_ESTAMPA, s.get(ModItems.BANNER_ESTAMPA));
                return;
            }
        }
    }
}
