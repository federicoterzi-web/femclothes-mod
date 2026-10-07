package com.femclothes.mixin;

import com.femclothes.sublimadora.BannerConImagen;
import com.femclothes.sublimadora.Estampa;
import com.femclothes.sublimadora.ModItems;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.ComponentMap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.DyeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * El banner guarda la foto sublimada (2026-10-06, "solo tintes y sublimadoras a banners"): se copia del ítem al
 * colocarlo, vuelve al ítem al recogerlo, se guarda en el chunk y viaja al cliente con el resto del bloque.
 */
@Mixin(BannerBlockEntity.class)
public abstract class BannerBlockEntityMixin extends BlockEntity implements BannerConImagen {

    public BannerBlockEntityMixin(net.minecraft.block.entity.BlockEntityType<?> tipo, net.minecraft.util.math.BlockPos pos, net.minecraft.block.BlockState estado) {
        super(tipo, pos, estado);
    }

    @Unique private Estampa femclothes$imagen;

    @Override public Estampa femclothes$imagen() { return femclothes$imagen; }
    @Override public void femclothes$ponerImagen(Estampa e) { femclothes$imagen = e; }

    /** El banner "de ítem" que dibuja el render del inventario y de la mano. */
    @Inject(method = "readFrom", at = @At("TAIL"))
    private void femclothes$leerDelItem(ItemStack stack, DyeColor color, CallbackInfo ci) {
        femclothes$imagen = stack.get(ModItems.BANNER_ESTAMPA);
    }

    @Inject(method = "readComponents", at = @At("TAIL"))
    private void femclothes$leerComponentes(BlockEntity.ComponentsAccess componentes, CallbackInfo ci) {
        femclothes$imagen = componentes.get(ModItems.BANNER_ESTAMPA);
    }

    @Inject(method = "addComponents", at = @At("TAIL"))
    private void femclothes$sumarComponentes(ComponentMap.Builder builder, CallbackInfo ci) {
        if (femclothes$imagen != null) builder.add(ModItems.BANNER_ESTAMPA, femclothes$imagen);
    }

    @Inject(method = "writeNbt", at = @At("TAIL"))
    private void femclothes$escribir(NbtCompound nbt, RegistryWrapper.WrapperLookup registros, CallbackInfo ci) {
        if (femclothes$imagen == null) return;
        Estampa.CODEC.encodeStart(NbtOps.INSTANCE, femclothes$imagen).result().ifPresent(n -> nbt.put("FemclothesImagen", n));
    }

    @Inject(method = "readNbt", at = @At("TAIL"))
    private void femclothes$leer(NbtCompound nbt, RegistryWrapper.WrapperLookup registros, CallbackInfo ci) {
        femclothes$imagen = nbt.contains("FemclothesImagen")
                ? Estampa.CODEC.parse(NbtOps.INSTANCE, nbt.get("FemclothesImagen")).result().orElse(null) : null;
    }
}
