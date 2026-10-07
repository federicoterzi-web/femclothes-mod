# Respaldo de la pollera (2026-10-01)

Pedido: "backupeame las polleras hasta aqui", antes de rehacer las UV (patrones
sin deformar) y sacar el cinto cuadrado del torso.

Estado respaldado: commit `327449d` (tag `respaldo-pollera-2026-10-01`).
Estos archivos son copias de referencia, fuera de `src/` (no se compilan).

Para volver a la pollera de antes:

    git checkout respaldo-pollera-2026-10-01 -- \
      src/main/java/com/femclothes/render/PolleraMalla.java \
      src/main/java/com/femclothes/client/PiezasDelMod.java \
      src/main/java/com/femclothes/render/GarmentFeatureRenderer.java \
      src/main/java/com/femclothes/render/PatronGenerador.java
