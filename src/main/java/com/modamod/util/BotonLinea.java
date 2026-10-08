package com.modamod.util;

import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Botón físico de la línea de producción en el frente de las 5 máquinas automáticas (a pedido, 2026-10-08, "agregar el
 * mismo botoncito de encendido y apagado que esta dentro de las guis en el frente"). El modelo lo dibuja abajo a la
 * izquierda del frente (x -5,6..-4, y 2,3..3,3; ver tools/agregar_boton_linea.py) y el click lo acepta con margen.
 */
public final class BotonLinea {

    private BotonLinea() {}

    /** ¿El click cayó sobre el botón? {@code frente} es el FACING del bloque. */
    public static boolean golpea(Direction frente, BlockPos pos, BlockHitResult hit) {
        if (hit.getSide() != frente) return false;
        double lx = hit.getPos().x - pos.getX();
        double lz = hit.getPos().z - pos.getZ();
        // Misma tabla que SublimadoraBlock#queControl: GeckoLib dibuja el modelo espejado en X, así que la x del modelo
        // sale de (0,5 - lateral), con lateral medido desde el lado del bloque que cada rumbo toma como referencia.
        double lateral = switch (frente) {
            case NORTH -> lx;
            case SOUTH -> 1.0 - lx;
            case WEST -> 1.0 - lz;
            default -> lz;      // EAST
        };
        double xModelo = (0.5 - lateral) * 16.0;
        double yModelo = (hit.getPos().y - pos.getY()) * 16.0;
        return xModelo >= -6.4 && xModelo <= -2.4 && yModelo >= 1.8 && yModelo <= 3.7;
    }

    /** Clic del botón y aviso en la barra de acciones. */
    public static void avisar(net.minecraft.world.World world, BlockPos pos, net.minecraft.entity.player.PlayerEntity jugador, boolean encendida) {
        world.playSound(null, pos, net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK.value(),
                net.minecraft.sound.SoundCategory.BLOCKS, 0.6f, encendida ? 1.2f : 0.8f);
        jugador.sendMessage(net.minecraft.text.Text.translatable(encendida
                ? "modamod.maquina.linea.encendida" : "modamod.maquina.linea.apagada"), true);
    }
}
