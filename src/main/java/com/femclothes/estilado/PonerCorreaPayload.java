package com.femclothes.estilado;

import com.femclothes.Femclothes;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Los dos clicks de una correa libre en la vista 3D de la Mesa de estilado (2026-10-05): la parte, los dos puntos
 * (px, espacio local de la parte) con su cara, el modo (0 pegada, 1 colgante) y el ancho en px.
 */
public record PonerCorreaPayload(BlockPos pos, int parte, float[] desde, int caraDesde, float[] hasta, int caraHasta,
                                 int modo, float ancho) implements CustomPayload {

    public static final Id<PonerCorreaPayload> ID = new Id<>(Identifier.of(Femclothes.MOD_ID, "poner_correa"));
    public static final PacketCodec<RegistryByteBuf, PonerCorreaPayload> CODEC = PacketCodec.of(
            (p, buf) -> {
                buf.writeBlockPos(p.pos());
                buf.writeVarInt(p.parte());
                for (float f : p.desde()) buf.writeFloat(f);
                buf.writeVarInt(p.caraDesde());
                for (float f : p.hasta()) buf.writeFloat(f);
                buf.writeVarInt(p.caraHasta());
                buf.writeVarInt(p.modo());
                buf.writeFloat(p.ancho());
            },
            buf -> new PonerCorreaPayload(buf.readBlockPos(), buf.readVarInt(),
                    new float[] {buf.readFloat(), buf.readFloat(), buf.readFloat()}, buf.readVarInt(),
                    new float[] {buf.readFloat(), buf.readFloat(), buf.readFloat()}, buf.readVarInt(),
                    buf.readVarInt(), buf.readFloat()));

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
