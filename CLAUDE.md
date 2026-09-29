# FemClothes — contexto para Claude

Mod de ropa para Minecraft **1.21.1 / Fabric** (Yarn). Ropa que se viste sobre el cuerpo en slots de **Trinkets**, dibujada en capas, y 4 máquinas para personalizarla: **Mesa de Modelado** (corte), **Estación de Tintes** (color/patrones), **Sublimadora** (fotos de Camerapture), **Guardarropas** (combinar outfits). Modelos de máquinas con **GeckoLib**.

La descripción completa de mecánicas, comandos y recetas está en [docs/manual/MANUAL.md](docs/manual/MANUAL.md) — leerla antes de tocar algo que no conozcas.

## Cómo trabajar con el usuario

- Hablar en **español rioplatense** (vos). El código, los comentarios y los nombres también van en español.
- **Preguntar antes de compilar** (`gradlew build`) y antes de lanzar el cliente. Nunca commitear ni pushear sin que lo pida.
- Antes de reworks grandes, **hacer preguntas** (el usuario lo prefiere) y proponer un plan.
- Los comentarios del código citan el pedido original entre comillas con fecha ("a pedido (2026-09-18, ...)"). Mantener ese estilo.

## Compilar y probar

- `./gradlew.bat build -x test` → `build/libs/femclothes-0.1.0.jar`.
- El usuario prueba en el perfil de Modrinth `Fabric 1.21.1 (2)` (carpeta `mods` en `%APPDATA%\ModrinthApp\profiles\`). **Antes de copiar el jar, verificar que no haya un `javaw.exe` corriendo**: copiar sobre el jar abierto lo corrompe (`ZipException: invalid LOC header`).
- En una sesión web/nube no hay Minecraft ni acceso a esas carpetas: compilar ahí y dejar que el usuario traiga los cambios y pruebe.

## El manual

- Después de cada cambio que afecte mecánicas, ítems, recetas, comandos, interfaces o arquitectura: **actualizar `docs/manual/MANUAL.md` en el mismo cambio**.
- Se regenera con `python tools/generar_manual.py --destino "C:/Users/feder/OneDrive/Documentos/Drive/FemClothes"` (arma las imágenes, genera el `.docx` y lo copia a la carpeta sincronizada con Google Drive). Requiere `python-docx` y `Pillow`. Desde la nube, solo actualizar el `.md`; el usuario regenera en su PC.
- Las vistas previas de redes/arneses/motivos del generador reimplementan en Python los algoritmos de `PatronGenerador` y `ClothingTextureCache` (`esHilo`, `perforarArnes`): si cambian en Java, actualizar el Python.

## Mapa rápido del código

- `render/GarmentFeatureRenderer` — único punto que dibuja toda la ropa, por capa (`garment/Capa`), sobre el cuerpo base. Render layer `ArmorCutoutNoCull` (alfa binario).
- `client/PiezasDelMod` — piezas de cada prenda (parte, capa, textura, filas visibles).
- `render/ClothingTextureCache` — composición de texturas (atlas de skin a 8x, 512x512), fundido de capas (`mezclar`, `tramar`), redes y arneses (`perforarRed`).
- `render/PatronGenerador` — máscaras de patrones (rayas, motivos `Motivo`, `Repeticion`, `Variacion`); los canales RGBA de la máscara llevan cobertura/contorno/n° de repetición/altura. Pinta las 4 caras + las 2 tapas de cada pieza (`recorrer`).
- `tinturas/TinturasBlockEntity` — cuadraditos (`Casilla`: mezcla CMYK+T de 21 niveles, hasta 3 colores, modo, opacidad, repetición, variación...), región por cuadradito (`regionDe`), diseños guardados. Las capas aplicadas van en el componente `femclothes:capas_tinte` (`RegionResolver.CapaPatron`).
- `region/RegionPintura` — zonas (cuello = borde real del escote vía `ClothingTextureCache.mascaraBordeCuello`, pecho, mangas, sup/inf...).
- `modelado/` — Modeladora: pines por rol (`ModeladoBlockEntity.ROLES`, `ModeladoScreenHandler.PIN_POS` con Y absoluta), `ComboCorte`, `PrendaModelado`.
- `sublimadora/` — Sublimadora, remera y su `Variante`, estampas. `SublimadoraBlockEntity.caraFijada` (chincheta por cara) y `DisenoEstampa` (diseños con nombre).

## Trampas conocidas

- **`BlockWithEntity.getRenderType` es `INVISIBLE` por defecto**: todo bloque nuevo tiene que overridearlo (`MODEL` si no usa GeckoLib), si no el bloque y la cara del vecino se ven como un agujero. Pasó con todas las máquinas.
- **`Slot.canInsert` no consulta `Inventory.isValid`**: cada slot restringido necesita un `Slot` anónimo con `canInsert` que delegue en `isValid`. Y `insertItem` (shift-click) no mira `isEnabled`.
- **Enums que viajan por red por ordinal** (`PatronRed`, etc.): agregar valores nuevos siempre al final.
- **Medias y calientabrazos en los esquemas se leen de frente**: el pin de la izquierda del dibujo es el lado DERECHO del jugador.
- **`PIN_POS` ya trae la Y absoluta del panel**: no sumarle el offset del esquema.
- **Primera persona**: la ropa nunca se dibuja en el brazo de primera persona (no hay hook en `renderArm`).
- **Outfit de otro jugador desactualizado un rato**: probablemente lag de sync de Trinkets, no un bug de caché del mod.

## Pendientes

**Estación de Tintes — plan en curso** (Fase A hecha el 2026-09-28: tapas pintadas, cuello = borde del escote, pecho con tapa de arriba, CMYK de a 5%, canal de Transparencia con recorte y tramado):
- **Fase B (hecha el 2026-09-28, falta probar en el juego):** panel de capas abajo a la derecha (ojo = oculta también al teñir y no gasta tinta; ▲▼ por fila; click selecciona) y "resto apagado" en 3D para la zona con el mouse encima (velo con `CapaPatron.fueraDeRegion`). Sin vista plana 2D (el usuario dijo que no hace falta).
- **Fase C:** rotar el motivo, girar la grilla, espejo, distancia horizontal/vertical independiente del tamaño, desplazamiento X/Y, más escalones de tamaño.
- **Fase D:** secuencia de alternancia escrita (ej. `1 1 2`), dirección de la alternancia, aleatorio con pesos, degradé con dirección y bandas, hasta 4 colores, paleta guardada y cuentagotas.
- Reorganizar la interfaz en pestañas (Color / Patrón / Mezcla / Capas) para que entre todo.

**Máquinas — sintonía y pulido** (2026-09-28, "cambiemos la gui de la sublimadora para hacerla sintonizar con sus bloques hermanos y que recien ahi revisemos y pulamos las 3"):
- Sublimadora rehecha al estilo de Tintes (hecha, falta probar en el juego): 560×408 pergamino, fotos sobre el dibujo de la prenda (el ítem real agrandado, Frente | Espalda) con chincheta por cara (solo se estampan las fijadas), diseños con nombre (`GuardarDisenoSublimadoraPayload`), botón Prensar, tanques CMYK + papel.
- Sublimadora: simetría lateral en medias y calientabrazos (`Estampa.espejo`; `EstampaTextures.conEspejo` espeja el lienzo del lado izquierdo). Modeladora: botón Modelar sobre la flecha (`BTN_MODELAR`). Hechos el 2026-09-28, falta probar.
- Almacenes más grandes (2026-09-28): Tintes 30 (9 de siempre en `items` + 21 en `almacenExtra` al FINAL del inventario, para no correr los índices guardados de los cuadraditos; grilla 5x6 a la izquierda, `slotAlmacen`), Sublimadora 27. Arnés: la tapa del hombro (arriba del brazo) lleva el mismo dibujo del arnés + un marco que empalma con las tiras que suben (`ClothingTextureCache.continuarTirasEnHombro`); la tapa de arriba del torso queda debajo de la cabeza. Modeladora (2026-09-29): "Moldes de <prenda>" de 12 a 36 (los 24 nuevos al FINAL del inventario, `porPrendaSlot`).
- **Siguiente:** revisar y pulir las tres máquinas juntas.

**Luces de las máquinas** (2026-09-29): propiedad `LIT` en los 3 bloques (`util/LuzMaquina`, luminancia 7), prendida por el tick del servidor mientras el LED está prendido (trabajando o prenda lista).

**Cuerpo base** (2026-09-29, zip "Texturas de cuerpos para mod Minecraft.zip" en la raíz): 19 máscaras en gris HD (384x384, 6x) en `textures/entity/cuerpo/<clave>.png`; el cuerpo se compone a la escala de la máscara (UV normalizadas, la geometría no cambia) (`CuerpoBase`, reemplazan a plano/curvy/binder, que migran por `CuerpoBase.deClave`); se colorean por 3 zonas según el gris (Base / Clara ≥165 / Oscura <75, solo animales; `PerfilCuerpo.tonoClaro/tonoOscuro`, 0 = automática desde la Base; cada zona × gris/mediana de su zona, aclarando hacia el blanco; `CuerpoBaseTextures.colorearPorZonas`), paleta de 5 colores de la skin (`paletaDeLaSkin`), vista previa sin ropa ni segunda capa (`PlayerEntityCapasSkinMixin`) y los brazos se adaptan classic/slim solos (`mascaraPara`). GUI `ElegirCuerpoScreen` la primera vez que te ponés una prenda (`ElegirCuerpoCliente`) y con `/femclothes elegir`. Falta probar en el juego.

**Otros, sin empezar:**
- Volumen 3D real en la ropa con 3D Skin Layers.
- Cadena textil: el hueso `cargo` de las máquinas debería moverse solo con máquinas encadenadas de izquierda a derecha.
- Prenda cargada visible encima de cada máquina; pantallita de vista previa en el bloque (falta definir).
- Cadena de tolvas: la base existe (`SidedInventory`), falta probarla de punta a punta.
- Guardarropas: sistema de estilos guardados (hay un prototipo de outfits de 4 prendas).
- Ropa en el brazo de primera persona.
- Piernas redondeadas (malla) como opción v2: prototipo aprobado en otro proyecto (`C:\mods\muslos-test`), no traer hasta que lo pida.
- Recetas para Estación de Tintes, Guardarropas, Pollera, moldes nuevos y patrones de motivo (hoy solo en creativo).
