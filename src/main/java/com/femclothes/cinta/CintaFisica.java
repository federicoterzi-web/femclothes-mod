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

    /** Hacia dónde empuja la banda a {@code e}, o null si el bloque no es una cinta o un empalme, o está parada. */
    static Vec3d direccion(BlockPos pos, BlockState estado, Entity e) {
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
        } else return null;
        if (pausada) return null;

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
        return dir;
    }

    /** Lo llaman {@code onSteppedOn} de la cinta y del empalme (en cliente y servidor). */
    public static void empujar(World mundo, BlockPos pos, BlockState estado, Entity e) {
        if (!e.isOnGround()) return;
        // El jugador se mueve en su cliente: ahí se empuja; en el servidor su velocidad no cuenta. El resto, al revés.
        if (e instanceof PlayerEntity ? !mundo.isClient : mundo.isClient) return;
        Vec3d dir = direccion(pos, estado, e);
        if (dir == null) return;
        CintaBlock.Forma forma = estado.getBlock() instanceof CintaBlock ? estado.get(CintaBlock.FORMA) : null;
        double k = IMPULSO * multiplicador(mundo, pos);
        double vy = 0;
        // Las prendas y los ítems caídos no suben escalones solos: la rampa de subida los levanta un poco.
        if (forma == CintaBlock.Forma.RAMPA_SUBE && !(e instanceof net.minecraft.entity.LivingEntity)) vy = 0.05;
        e.addVelocity(dir.x * k, vy, dir.z * k);
    }

    // ── Deslizamiento (2026-10-07, "se puede hacer que sea un deslizamiento? porque se ve como si el personaje caminara") ──

    /** Velocidad a la que queda quien se sube: el impulso de cada tick contra el rozamiento del bloque (v = k·f / (1 − f)). */
    private static double velocidadBanda(World mundo, BlockPos pos, BlockState estado) {
        double f = estado.getBlock().getSlipperiness() * 0.91;
        return IMPULSO * multiplicador(mundo, pos) * f / (1.0 - f);
    }

    /** La cinta que lleva a {@code e} ahora (la del bloque de abajo, en marcha), o null. */
    private static BlockPos cintaBajo(Entity e) {
        if (!e.isOnGround()) return null;
        BlockPos p = BlockPos.ofFloored(e.getX(), e.getY() - 0.2, e.getZ());
        BlockState s = e.getWorld().getBlockState(p);
        return direccion(p, s, e) != null ? p : null;
    }

    /** ¿Lo está llevando una cinta ahora? (para quitarle el vaivén de la cámara al jugador quieto). */
    public static boolean llevada(Entity e) { return cintaBajo(e) != null; }

    /**
     * Cuánto camina de verdad {@code e} este tick, para las piernas y los brazos: lo que avanzó menos lo que la cinta lo
     * lleva. Quieto sobre la banda da 0 y se desliza como sobre hielo; si camina o corre, se anima por lo suyo.
     */
    public static float pasoPropio(Entity e, float posDelta) {
        BlockPos p = cintaBajo(e);
        if (p == null) return posDelta;
        BlockState s = e.getWorld().getBlockState(p);
        Vec3d dir = direccion(p, s, e);
        double v = velocidadBanda(e.getWorld(), p, s);
        double dx = e.getX() - e.prevX, dz = e.getZ() - e.prevZ;
        double along = dx * dir.x + dz * dir.z;
        double perpX = dx - along * dir.x, perpZ = dz - along * dir.z;
        // Llevado: avanza como la banda (con un poco de margen al arrancar) y casi nada al costado.
        if (along >= -0.25 * v && along <= 1.3 * v && perpX * perpX + perpZ * perpZ < 0.012 * 0.012) return 0f;
        double rx = dx - dir.x * v, rz = dz - dir.z * v;
        double r = Math.sqrt(rx * rx + rz * rz);
        return r < 0.012 ? 0f : (float) r;
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
