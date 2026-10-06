package com.femclothes.cinta;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/**
 * Lo que la cinta le hace a quien se sube (2026-10-06, "si el jugador se sube a un conveyor belt lo mueva") y el
 * contagio de la señal de redstone a las cintas encadenadas (2026-10-06, "el apagado de redstone se contagie hasta 8
 * conveyors de distancia encadenados").
 */
public final class CintaFisica {

    private CintaFisica() {}

    /** Avance de la banda: 1 px por tick. */
    public static final double PX_POR_TICK = 1.0 / 16.0;
    /** Cuántas cintas hacia cada lado alcanza una señal de redstone. */
    public static final int ALCANCE_PAUSA = 8;
    /** Parte de la velocidad que se pierde por el rozamiento del suelo: se agrega esto para llegar a la velocidad de la banda. */
    private static final double IMPULSO = PX_POR_TICK * 0.45;

    /** Aceleración por el bloque de abajo (2026-10-06, "hielo comprimido x2 hielo azul x3"). */
    public static double multiplicador(World mundo, BlockPos pos) {
        BlockState abajo = mundo.getBlockState(pos.down());
        if (abajo.isOf(net.minecraft.block.Blocks.BLUE_ICE)) return 3.0;
        if (abajo.isOf(net.minecraft.block.Blocks.PACKED_ICE)) return 2.0;
        return 1.0;
    }

    // ── Empuje ──

    /** Lo llaman {@code onSteppedOn} de la cinta y del empalme (en cliente y servidor). */
    public static void empujar(World mundo, BlockPos pos, BlockState estado, Entity e) {
        if (!e.isOnGround()) return;
        // El jugador se mueve en su cliente: ahí se empuja; en el servidor su velocidad no cuenta. El resto, al revés.
        if (e instanceof PlayerEntity ? !mundo.isClient : mundo.isClient) return;
        Direction frente;
        boolean pausada;
        CintaBlock.Forma forma = null;
        if (estado.getBlock() instanceof CintaBlock) {
            frente = estado.get(CintaBlock.FACING);
            pausada = estado.get(CintaBlock.POWERED);
            forma = estado.get(CintaBlock.FORMA);
        } else if (estado.getBlock() instanceof EmpalmeBlock) {
            frente = estado.get(EmpalmeBlock.FACING);
            pausada = estado.get(EmpalmeBlock.POWERED);
        } else return;
        if (pausada) return;

        Vec3d dir = new Vec3d(frente.getOffsetX(), 0, frente.getOffsetZ());
        if (forma != null && forma.esCurva()) {
            Direction lado = forma == CintaBlock.Forma.CURVA_IZQ ? frente.rotateYCounterclockwise() : frente.rotateYClockwise();
            // Centro del arco = la esquina entre el lado por donde entra y el frente; la tangente gira el radio 90°.
            double cx = pos.getX() + 0.5 + 0.5 * lado.getOffsetX() + 0.5 * frente.getOffsetX();
            double cz = pos.getZ() + 0.5 + 0.5 * lado.getOffsetZ() + 0.5 * frente.getOffsetZ();
            double rx = e.getX() - cx, rz = e.getZ() - cz;
            double a = rx * frente.getOffsetX() + rz * frente.getOffsetZ();      // componente del radio sobre el frente
            double b = rx * lado.getOffsetX() + rz * lado.getOffsetZ();          // ... sobre el lado
            // T(a·f + b·L) = a·L − b·f
            double tx = a * lado.getOffsetX() - b * frente.getOffsetX();
            double tz = a * lado.getOffsetZ() - b * frente.getOffsetZ();
            double n = Math.sqrt(tx * tx + tz * tz);
            if (n > 1e-3) dir = new Vec3d(tx / n, 0, tz / n);
        }
        double k = IMPULSO * multiplicador(mundo, pos);
        double vy = 0;
        // Las prendas y los ítems caídos no suben escalones solos: la rampa de subida los levanta un poco.
        if (forma == CintaBlock.Forma.RAMPA_SUBE && !(e instanceof net.minecraft.entity.LivingEntity)) vy = 0.05;
        e.addVelocity(dir.x * k, vy, dir.z * k);
    }

    // ── Pausa por redstone con contagio ──

    private static boolean esCinta(BlockState s) { return s.getBlock() instanceof CintaBlock || s.getBlock() instanceof EmpalmeBlock; }

    /** A qué bloques entrega esta cinta o empalme (misma altura, un nivel arriba si sube, uno abajo si ahí hay una bajada). */
    private static boolean entregaA(BlockView mundo, BlockPos pos, BlockState s, BlockPos otro) {
        Direction f = s.getBlock() instanceof CintaBlock ? s.get(CintaBlock.FACING) : s.get(EmpalmeBlock.FACING);
        BlockPos d = pos.offset(f);
        if (d.equals(otro)) return true;
        if (s.getBlock() instanceof CintaBlock) {
            if (s.get(CintaBlock.FORMA) == CintaBlock.Forma.RAMPA_SUBE && d.up().equals(otro)) return true;
            if (d.down().equals(otro)) {
                BlockState t = mundo.getBlockState(otro);
                return t.getBlock() instanceof CintaBlock && t.get(CintaBlock.FORMA) == CintaBlock.Forma.RAMPA_BAJA;
            }
        }
        return false;
    }

    /** Las cintas encadenadas con esta (entrega una a la otra, en cualquiera de los dos sentidos) dentro del alcance. */
    private static Map<BlockPos, Integer> cadena(World mundo, BlockPos origen) {
        Map<BlockPos, Integer> visto = new HashMap<>();
        ArrayDeque<BlockPos> cola = new ArrayDeque<>();
        visto.put(origen, 0);
        cola.add(origen);
        while (!cola.isEmpty()) {
            BlockPos p = cola.poll();
            int paso = visto.get(p);
            if (paso >= ALCANCE_PAUSA) continue;
            BlockState s = mundo.getBlockState(p);
            if (!esCinta(s)) continue;   // el origen puede ser un bloque que todavía se está colocando
            for (Direction d : Direction.Type.HORIZONTAL) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos q = p.offset(d).up(dy);
                    if (visto.containsKey(q)) continue;
                    BlockState t = mundo.getBlockState(q);
                    if (!esCinta(t)) continue;
                    if (entregaA(mundo, p, s, q) || entregaA(mundo, q, t, p)) {
                        visto.put(q, paso + 1);
                        cola.add(q);
                    }
                }
            }
        }
        return visto;
    }

    /** ¿Esta cinta debe estar parada? Con señal propia o con alguna encadenada (hasta 8 de distancia) que la tenga. */
    public static boolean debePausar(World mundo, BlockPos pos) {
        for (BlockPos p : cadena(mundo, pos).keySet()) {
            if (mundo.isReceivingRedstonePower(p)) return true;
        }
        return false;
    }

    /** Cambió algo cerca de {@code pos}: actualiza el estado parada/en marcha de todas las cintas de su cadena. */
    public static void refrescarCadena(World mundo, BlockPos pos) {
        Map<BlockPos, Integer> c = cadena(mundo, pos);
        // Una cinta tiene señal propia; las demás de la cadena (a ≤ 8 de ella) se paran también.
        // Las cintas más lejos que el alcance de la fuente quedan en marcha: se miran una por una.
        for (BlockPos p : c.keySet()) {
            BlockState s = mundo.getBlockState(p);
            boolean pausa = debePausar(mundo, p);
            if (s.getBlock() instanceof CintaBlock && s.get(CintaBlock.POWERED) != pausa) {
                mundo.setBlockState(p, s.with(CintaBlock.POWERED, pausa), net.minecraft.block.Block.NOTIFY_LISTENERS);
            } else if (s.getBlock() instanceof EmpalmeBlock && s.get(EmpalmeBlock.POWERED) != pausa) {
                mundo.setBlockState(p, s.with(EmpalmeBlock.POWERED, pausa), net.minecraft.block.Block.NOTIFY_LISTENERS);
            }
        }
    }
}
