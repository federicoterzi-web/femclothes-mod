package com.femclothes.sublimadora;

import org.jetbrains.annotations.Nullable;

/** Lo que el mixin de {@code BannerBlockEntity} le agrega: la foto sublimada del banner (2026-10-06). */
public interface BannerConImagen {
    @Nullable Estampa femclothes$imagen();
    void femclothes$ponerImagen(@Nullable Estampa estampa);
}
