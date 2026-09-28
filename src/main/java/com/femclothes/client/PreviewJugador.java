package com.femclothes.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Dibuja al jugador en una caja del inventario a un ÁNGULO EXACTO — a
 * pedido (2026-09-20, "arreglame el boton de back... no anda"): el truco
 * de {@code InventoryScreen.drawEntity(..., mouseX, mouseY, ...)} que
 * usaban {@code TinturasScreen}/{@code SublimadoraScreen} deriva la
 * rotación con {@code Math.atan((centro-mouseX)/40)}, que por como es
 * atan NUNCA puede llegar a 180° (su rango es -90°..90°, asintótico) —
 * asi que un botón "ver de espaldas" que fuerce ese mouseX nunca daba una
 * vista de espaldas de verdad, por más lejos que empujara el mouseX falso.
 *
 * Esta versión reimplementa el mismo posado de {@code InventoryScreen}
 * (bodyYaw/yaw/pitch/headYaw + los dos Quaternionf de cámara/luz) pero
 * recibiendo el ángulo YA HECHO en vez de derivarlo de una posición de
 * mouse — así cualquier ángulo, incluido 180°, sale exacto.
 */
public final class PreviewJugador {

    private PreviewJugador() {}

    /**
     * @param anguloGrados 0 = de frente (la pose de siempre), 90/-90 = de
     *                     perfil, 180 = de espaldas — cualquier valor vale,
     *                     no solo los 4 "redondos".
     * @param mouseY       posición real del mouse en pantalla, para el
     *                     leve seguimiento vertical (mirar arriba/abajo)
     *                     que ya tenía el preview.
     */
    public static void dibujar(DrawContext context, LivingEntity entity,
                                int x1, int y1, int x2, int y2, int size,
                                float anguloGrados, float mouseY) {
        float g = (x1 + x2) / 2.0F;
        float h = (y1 + y2) / 2.0F;
        context.enableScissor(x1, y1, x2, y2);

        float j = (float) Math.atan((h - mouseY) / 40.0F);
        Quaternionf quaternionf = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf quaternionf2 = new Quaternionf().rotateX(j * 20.0F * (float) (Math.PI / 180.0));
        quaternionf.mul(quaternionf2);

        float bodyYawViejo = entity.bodyYaw;
        float yawViejo = entity.getYaw();
        float pitchViejo = entity.getPitch();
        float prevHeadYawViejo = entity.prevHeadYaw;
        float headYawViejo = entity.headYaw;

        float destino = 180.0F + anguloGrados;
        entity.bodyYaw = destino;
        entity.setYaw(destino);
        entity.setPitch(-j * 20.0F);
        entity.headYaw = entity.getYaw();
        entity.prevHeadYaw = entity.getYaw();

        float p = entity.getScale();
        Vector3f vector3f = new Vector3f(0.0F, entity.getHeight() / 2.0F, 0.0F);
        float q = size / p;
        InventoryScreen.drawEntity(context, g, h, q, vector3f, quaternionf, quaternionf2, entity);

        entity.bodyYaw = bodyYawViejo;
        entity.setYaw(yawViejo);
        entity.setPitch(pitchViejo);
        entity.prevHeadYaw = prevHeadYawViejo;
        entity.headYaw = headYawViejo;
        context.disableScissor();
    }

    /** Clave de traducción de la perspectiva más cercana a {@code anguloGrados} — redondea a 0/90/180/270. */
    public static String nombreVista(float anguloGrados) {
        int redondo = Math.floorMod(Math.round(anguloGrados / 90f) * 90, 360);
        return switch (redondo) {
            case 90 -> "femclothes.preview.vista.derecha";
            case 180 -> "femclothes.preview.vista.espalda";
            case 270 -> "femclothes.preview.vista.izquierda";
            default -> "femclothes.preview.vista.frente";
        };
    }
}
