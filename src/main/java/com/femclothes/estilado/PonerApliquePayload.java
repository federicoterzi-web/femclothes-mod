package com.femclothes.estilado;

import com.femclothes.Femclothes;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Click sobre la prenda en la vista 3D de la Mesa de estilado (2026-10-01):
 * qué parte, el punto en su caja (píxeles, espacio local de la parte) y la cara;
 * o, en la pollera y la capa ({@code superficie}), el punto (u, v) de su tela.
 * Lleva floats, así que no entra en un clickButton.
 */
public record PonerApliquePayload(BlockPos pos, int parte, float x, float y, float z, int cara, int superficie)
        implements CustomPayload {

    public static final Id<PonerApliquePayload> ID = new Id<>(Identifier.of(Femclothes.MOD_ID, "poner_aplique"));
    /** A mano: son 7 campos y {@code PacketCodec.tuple} llega hasta 6. */
    public static final PacketCodec<RegistryByteBuf, PonerApliquePayload> CODEC = PacketCodec.of(
            (p, buf) -> {
                buf.writeBlockPos(p.pos());
                buf.writeVarInt(p.parte());
                buf.writeFloat(p.x());
                buf.writeFloat(p.y());
                buf.writeFloat(p.z());
                buf.writeVarInt(p.cara());
                buf.writeVarInt(p.superficie());
            },
            buf -> new PonerApliquePayload(buf.readBlockPos(), buf.readVarInt(), buf.readFloat(), buf.readFloat(),
                    buf.readFloat(), buf.readVarInt(), buf.readVarInt()));

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
