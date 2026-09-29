package com.femclothes.body;

import com.femclothes.Femclothes;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Paquetes de la GUI de elegir cuerpo (2026-09-29, "la primera vez que uno
 * se pone una prenda del mod te lance una gui con el color de skin calculado
 * y te de la alternativa de elegir cualquiera de esas texturas base"):
 * <ul>
 *   <li>{@link Elegir} (cliente -> servidor): lo que se confirmó en la GUI.</li>
 *   <li>{@link Abrir} (servidor -> cliente): {@code /femclothes elegir}
 *   vuelve a abrir la GUI — el comando corre en el servidor y la pantalla
 *   vive en el cliente.</li>
 * </ul>
 */
public final class RedCuerpo {

    /** Cuerpo por clave y tono RGB ({@link PerfilCuerpo#TONO_DE_LA_SKIN} = sacarlo de la skin). */
    public record Elegir(String cuerpo, int tono) implements CustomPayload {
        public static final Id<Elegir> ID = new Id<>(Identifier.of(Femclothes.MOD_ID, "elegir_cuerpo"));
        public static final PacketCodec<RegistryByteBuf, Elegir> CODEC = PacketCodec.tuple(
                PacketCodecs.string(32), Elegir::cuerpo,
                PacketCodecs.INTEGER, Elegir::tono,
                Elegir::new);

        @Override
        public Id<? extends CustomPayload> getId() { return ID; }
    }

    public record Abrir() implements CustomPayload {
        public static final Id<Abrir> ID = new Id<>(Identifier.of(Femclothes.MOD_ID, "abrir_elegir_cuerpo"));
        public static final PacketCodec<RegistryByteBuf, Abrir> CODEC = PacketCodec.unit(new Abrir());

        @Override
        public Id<? extends CustomPayload> getId() { return ID; }
    }

    private RedCuerpo() {}

    public static void init() {
        PayloadTypeRegistry.playC2S().register(Elegir.ID, Elegir.CODEC);
        PayloadTypeRegistry.playS2C().register(Abrir.ID, Abrir.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Elegir.ID, (payload, context) ->
                context.server().execute(() -> {
                    ServerPlayerEntity jugador = context.player();
                    CuerpoBase cuerpo = CuerpoBase.deClave(payload.cuerpo());
                    int tono = payload.tono() & 0xFFFFFF;
                    // conCuerpo/conTono ya marcan el perfil como elegido: la
                    // GUI no vuelve a saltar sola.
                    PerfilesDeCuerpo.poner(jugador, PerfilesDeCuerpo.de(jugador).conCuerpo(cuerpo).conTono(tono));
                }));
    }

    public static void abrirEn(ServerPlayerEntity jugador) {
        ServerPlayNetworking.send(jugador, new Abrir());
    }
}
