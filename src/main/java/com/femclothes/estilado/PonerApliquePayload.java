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
 * qué parte, el punto en su caja (píxeles, espacio local de la parte) y la cara.
 * Lleva floats, así que no entra en un clickButton.
 */
public record PonerApliquePayload(BlockPos pos, int parte, float x, float y, float z, int cara) implements CustomPayload {

    public static final Id<PonerApliquePayload> ID = new Id<>(Identifier.of(Femclothes.MOD_ID, "poner_aplique"));
    public static final PacketCodec<RegistryByteBuf, PonerApliquePayload> CODEC = PacketCodec.tuple(
            BlockPos.PACKET_CODEC, PonerApliquePayload::pos,
            PacketCodecs.VAR_INT, PonerApliquePayload::parte,
            PacketCodecs.FLOAT, PonerApliquePayload::x,
            PacketCodecs.FLOAT, PonerApliquePayload::y,
            PacketCodecs.FLOAT, PonerApliquePayload::z,
            PacketCodecs.VAR_INT, PonerApliquePayload::cara,
            PonerApliquePayload::new);

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
