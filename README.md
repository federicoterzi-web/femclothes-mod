# FemClothes — Fabric 1.21.1, Loader 0.18.4

> Toolchain confirmada: Fabric Loom **1.15.3** + Gradle **9.2.0**.
> En Windows usar `gradlew.bat` desde PowerShell — el `./gradlew` de
> Bash falla por finales de línea CRLF.

Mod completo usando Trinkets + Cosmetic Armor Updated + Armor Model API.
Toda la sincronización e inventario extra la resuelven esas librerías —
no hay `WardrobeComponent` propio.

> **En este repo hay DOS mods.** `sublimadora/` es un mod aparte, con su
> propio jar y su propio `build.gradle` — una prensa térmica que estampa
> fotos de Camerapture sobre remeras. Lo único que comparten es el slot de
> torso de Trinkets, y sin depender uno del otro. Su documentación está en
> [`sublimadora/README.md`](sublimadora/README.md).
>
> Para correr el cliente: `gradlew.bat :runClient` **con los dos puntos**.
> Sin ellos, Gradle corre la tarea en los dos subproyectos y te levanta dos
> instancias con mundos distintos.

## Cómo queda mapeada cada prenda

| Prenda | Mecanismo | Clase |
|---|---|---|
| Medias 3/4 | Slot cosmético BOOTS | `ClothingArmorItem` (dyeable) |
| Medias de red | Slot cosmético BOOTS (excluyente con medias 3/4) | `ClothingArmorItem` |
| Shorts | Slot cosmético LEGGINGS | `ClothingArmorItem` (dyeable) |
| Croptop | Slot cosmético CHESTPLATE + relleno de piel | `ClothingArmorItem` + `CroptopArmorRenderProvider` |
| Traje de maid | Slot cosmético CHESTPLATE (dibuja el set completo) | `ClothingArmorItem` |
| Buzo oversize | Slot cosmético CHESTPLATE + geometría propia | `ClothingArmorItem` + Armor Model API |
| Calentadores de brazo | Slot custom "arms" (Trinkets, no vanilla) | `ArmWarmerItem` (`TrinketItem`) |
| **Medias color pleno** | Slot custom `socks/pair` (Trinkets), dibujada sobre la ModelPart real | `ClothingTrinketItem` + `BodyPartTrinketRenderer` |

El croptop además entra en el slot custom `torso/prenda`, **compartido con
la remera estampada de la sublimadora**. Que compartan slot es a propósito:
dos prendas sobre el mismo pedazo de cuerpo se pisarían, así que en vez de
resolver el solapamiento se vuelven mutuamente excluyentes. Cada mod declara
su ítem en el tag y los tags se fusionan, así que ninguno depende del otro.
Hoy entra al slot pero todavía no se dibuja en el cuerpo.

Los iconos de los huecos vacíos van en `textures/gui/slot/`, en gris y al 45%
de alpha. No apuntar a la textura del ítem: a todo color se lee como si ya
hubiera algo equipado. Y el slot sin campo `icon` hace que Trinkets pida
`minecraft:textures/.png` en cada frame.

Las prendas marcadas con `ClothingArmorItem` siguen en el sistema viejo
(geometria de armadura, mas ancha). Solo `socks_solid` esta migrada al
sistema "pegado al cuerpo". La personalizacion (patron + color de patron)
se aplica en el **Telar vanilla**, no hay bloque propio — ver
`ClothingLoomInteraction`.

## Armor Model API: como archivo local (no vía Modrinth Maven)

El coordinate de Modrinth Maven para este mod no resolvió pase lo que
pase (probamos `1.0.0`, `1.0.0+1.21.1` con el número de versión en
texto — ninguno le pegó al path real del repositorio). En vez de
seguir adivinando IDs hash, la solución robusta es bajar el jar directo
del autor y referenciarlo como archivo local:

1. Entrá a https://github.com/FabricExtras/ArmorModelAPI/releases
2. Buscá el release taggeado `1.0.0+1.21.1` (el que NO dice
   `-neoforge` al final — ese es para NeoForge, no te sirve).
3. Descargá el `.jar` de los "Assets" de ese release.
4. **Renombralo a `armor-model-api-1.0.0-1.21.1.jar`** (con guión en
   vez de `+`) y ponelo en `libs/` dentro de tu proyecto. El `+` en el
   nombre de archivo, combinado con espacios en la ruta de carpeta en
   Windows, puede romper el cálculo de hash de Gradle/Loom — por eso
   el `build.gradle` ya apunta a ese nombre sin el símbolo.
5. **Además, renombrá la carpeta del proyecto** de
   `femclothes-mod (1)` a `femclothes-mod` (sin espacio ni paréntesis)
   si todavía no lo hiciste — es la otra mitad del mismo problema de
   rutas raras en Windows, y probablemente esté afectando más cosas
   además de este archivo.

Con eso Gradle lo toma como dependencia local, sin depender de que
el maven de Modrinth resuelva bien el string de versión, y sin la
combinación de caracteres que rompe el hash en Windows.

## Pila confirmada para 1.21.1 + Fabric Loader 0.18.4

- **Trinkets** (original, emilyploszaj) `3.10.0` — puesto directo en el
  `build.gradle`, es la build real para 1.21.1.
- **Cardinal Components API** `6.1.3` — la pide Cosmetic Armor Updated
  en este rango de MC. Puesto directo.
- **Cosmetic Armor Updated** `1.7.0` — la versión que ya tenés
  instalada; ese número se reusó para varias builds de MC en paralelo,
  así que debería resolver bien tal cual.
- **Armor Model API** `1.0.0+1.21.1` — RESUELTO: como jar local en
  `libs/armor-model-api-1.0.0-1.21.1.jar` (ver sección de arriba).
- ~~ArmorRenderLib~~ — **sacada del proyecto**, ver corrección abajo.

## Corrección importante: ArmorRenderLib no existe para 1.21.x

Ni el proyecto original "Armor Render Lib" (se quedó en 1.19.2) ni el
fork "ArmorRenderLib: Directors Cut" (se quedó en 1.20.1) tienen build
para 1.21.x — ambos sin mantenimiento hace años. Si tu Modrinth te
tira advertencia de incompatibilidad con esa dependencia, es por eso,
no por tu loader.

Se sacó del proyecto entero. El croptop ahora se renderiza directo
contra el `ArmorRenderer` que ya trae **Fabric API**
(`net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer`, paquete
confirmado en la build 0.115.6+1.21.1) — de hecho ArmorRenderLib nunca
fue más que una envoltura fina sobre ese mismo hook, así que no se
perdió funcionalidad, se sacó una dependencia de más que ya no se
mantiene.

`CroptopArmorRenderProvider` implementa `ArmorRenderer` directamente y
hace 2 pasadas de render por frame:
1. Una textura 1x1 sólida (generada y cacheada por `SolidColorTexture`)
   tintada al tono de piel muestreado del skin del jugador.
2. Encima, la textura real del croptop tintada al color que el
   jugador eligió al teñirlo — la franja de panza de esa textura tiene
   que tener alpha 0 para que se vea la pasada 1 por debajo.

## Texturas: DOS layouts distintos segun el renderer

Esta es la trampa mas facil de pisar del proyecto. El layout de la textura
depende de con que renderer se dibuja la prenda, y no son intercambiables:

- **Prendas migradas a `BodyPartTrinketRenderer`** (hoy solo `socks_solid`):
  van en **64x64, layout de SKIN DE JUGADOR**, porque el renderer dibuja las
  ModelPart reales del jugador. Las dos piernas estan en regiones SEPARADAS:
  pierna derecha en `uv(0,16)`, izquierda en `uv(16,48)`. Verificado en
  `PlayerEntityModel.getTexturedModelData`.
  - `textures/models/armor/socks_solid_layer_1.png` — escala de grises,
    blanco = tine al 100% con el color elegido.

- **Prendas que siguen en `ClothingArmorItem`** (croptop, shorts, maid,
  buzo): van en el layout de ARMADURA vanilla, tipo `leather_layer_1.png`.

Una textura 64x32 de armadura usada en una prenda de Trinkets hace que la
pierna izquierda samplee fuera de la imagen. Antes de dibujar, mirar con que
renderer esta registrada la prenda en `FemclothesClient`.

### Mascaras de patron

```
textures/models/armor/patterns/<prenda>/<patron>.png
```
Ej: `patterns/socks/stripe_top.png`. Es por prenda porque la mascara depende
del UV map de esa prenda. Opaca solo donde va el patron, transparente en el
resto. Si falta, la prenda cae a lisa en vez de romperse.

### Estado del arte

Hay texturas de PRUEBA generadas para `socks_solid` (media + mascara
`stripe_top`) y los iconos de item. Todo lo demas sigue sin arte.

## Lección que quedó escrita al revés

La nota vieja decía que para leer los píxeles de la skin bastaba castear a
`NativeImageBackedTexture`. Es falso: las skins de Mojang son
`PlayerSkinTexture`, que extiende `ResourceTexture`, y ese cast falla para
toda skin real. Lo que funciona es leer los píxeles de vuelta desde la GPU
con `NativeImage.loadFromTextureImage()`, en el hilo de render y cacheado
por Identifier. Está en `SkinTextureAccess`.

## Estado al 2026-09-05

Andando: crafteo de medias, telar vanilla con patrones y por pierna,
resolución x2, sombreado, reconstrucción de piel y el mixin de override de
skin. La sublimadora hace el ciclo completo de sublimado.

Lo próximo, por orden de valor:

1. **Dibujar la foto estampada en la remera** — hoy se guarda el UUID y no
   se dibuja nada, ni en el ítem ni sobre el cuerpo.
2. **Migrar croptop y shorts a `BodyPartTrinketRenderer`** — ver el problema
   abierto de dos prendas en la misma parte del cuerpo, en
   `RESUMEN_PRENDAS_Y_RECETAS.md`.
3. **Verificar la integración con 3D Skin Layers** — el puente
   `HttpTextureAccessor` está escrito pero nunca se probó en el juego; sus
   líneas de `modLocalRuntime` siguen comentadas en `build.gradle`.
4. **Geometría del buzo oversize y la falda del traje de maid** — vía Armor
   Model API, `.geo.json` estilo Bedrock hecho en Blockbench. Hay un TODO en
   `FemclothesClient.java` marcando dónde va el registro.
5. **Arte de verdad.** Solo las medias tienen textura, y es de prueba
   generada por script. Las otras siete prendas no tienen nada. El croptop
   necesita alpha 0 en la franja de panza.
