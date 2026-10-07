package com.modamod.client;

import com.modamod.sublimadora.EscanearEstampa;
import me.chrr.camerapture.CameraptureClient;
import me.chrr.camerapture.config.SyncedConfig;
import me.chrr.camerapture.util.ImageUtil;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Lado cliente de "Escanear estampa" (2026-10-02, "abre explorador para
 * seleccionar imagen y produce una imagen de camerapture"): el mismo camino
 * que "Cargar imagen" de la cámara de Camerapture — explorador de archivos
 * de TinyFileDialogs en otro hilo, la imagen achicada a la resolución máxima
 * del servidor y comprimida a WebP bajando la calidad hasta que entre en el
 * tope de bytes — y después se manda en partes a la Sublimadora
 * ({@link EscanearEstampa.Parte}), que la convierte en la foto.
 */
public final class EscanerEstampaCliente {

    private EscanerEstampaCliente() {}

    private static final Logger LOG = LoggerFactory.getLogger("modamod-escanear");
    private static final String[] EXTENSIONES = {"*.png", "*.jpg", "*.jpeg", "*.webp", "*.bmp", "*.gif"};
    /** Si ya hay un explorador abierto (no se abren dos). */
    private static volatile boolean ocupado = false;
    private static int subidas = 0;

    /** Abre el explorador y, si se elige una imagen, la manda a la Sublimadora de {@code pos}. */
    public static void escanear(BlockPos pos) {
        if (ocupado) return;
        ocupado = true;
        Thread hilo = new Thread(() -> {
            try {
                String ruta = elegirArchivo();
                if (ruta != null) preparar(pos, Path.of(ruta));
            } catch (Exception e) {
                LOG.error("No se pudo escanear la estampa", e);
                avisar("modamod.sublimadora.escanear.fallo", Formatting.RED);
            } finally {
                ocupado = false;
            }
        }, "modamod-escaner");
        hilo.setDaemon(true);
        hilo.start();
    }

    private static String elegirArchivo() {
        try (MemoryStack pila = MemoryStack.stackPush()) {
            PointerBuffer filtros = pila.mallocPointer(EXTENSIONES.length);
            for (String e : EXTENSIONES) filtros.put(pila.UTF8(e));
            filtros.flip();
            return TinyFileDialogs.tinyfd_openFileDialog(
                    Text.translatable("modamod.sublimadora.escanear.titulo").getString(), "", filtros,
                    Text.translatable("modamod.sublimadora.escanear.imagenes").getString(), false);
        }
    }

    private static void preparar(BlockPos pos, Path archivo) throws Exception {
        BufferedImage imagen = ImageIO.read(archivo.toFile());
        if (imagen == null && archivo.toString().toLowerCase(java.util.Locale.ROOT).endsWith(".webp")) {
            imagen = ImageUtil.decodeImageFromWebP(Files.readAllBytes(archivo));
        }
        if (imagen == null) {
            avisar("modamod.sublimadora.escanear.no_imagen", Formatting.RED);
            return;
        }
        // Los mismos límites que la cámara (los manda el servidor de Camerapture).
        SyncedConfig config = CameraptureClient.syncedConfig;
        int resolucion = config != null ? config.maxImageResolution() : 1280;
        int maximo = config != null ? config.maxImageBytes() : 500_000;
        imagen = ImageUtil.normalize(ImageUtil.clampSize(imagen, resolucion));
        float calidad = 0.95f;
        byte[] bytes = ImageUtil.compressIntoWebP(imagen, calidad);
        while (bytes.length > maximo) {
            calidad -= 0.05f;
            if (calidad < 0.1f) {
                avisar("modamod.sublimadora.escanear.grande", Formatting.RED);
                return;
            }
            bytes = ImageUtil.compressIntoWebP(imagen, calidad);
        }
        byte[] listos = bytes;
        MinecraftClient.getInstance().execute(() -> enviar(pos, listos));
    }

    private static void enviar(BlockPos pos, byte[] bytes) {
        int id = ++subidas;
        int tam = EscanearEstampa.TAMANO_PARTE;
        int total = Math.max(1, (bytes.length + tam - 1) / tam);
        for (int i = 0; i < total; i++) {
            byte[] parte = Arrays.copyOfRange(bytes, i * tam, Math.min(bytes.length, (i + 1) * tam));
            ClientPlayNetworking.send(new EscanearEstampa.Parte(pos, id, i, total, parte));
        }
        avisar("modamod.sublimadora.escanear.enviando", Formatting.GRAY);
    }

    private static void avisar(String clave, Formatting color) {
        MinecraftClient cliente = MinecraftClient.getInstance();
        cliente.execute(() -> {
            if (cliente.player != null) cliente.player.sendMessage(Text.translatable(clave).formatted(color), true);
        });
    }
}
