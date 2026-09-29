# FemClothes — Manual del mod

Versión del mod: 0.1.0 · Minecraft 1.21.1 (Fabric) · Manual actualizado: {{FECHA}}

![Personaje vestido con prendas del mod](img/captura_jugador_1.png)

FemClothes agrega ropa que se viste SOBRE el cuerpo del personaje: remeras, pantalones, medias, calientabrazos y polleras que siguen la forma del modelo, se superponen en capas (una media debajo de un pantalón, una remera arriba de todo) y se pueden personalizar casi por completo: el corte, el color, los patrones, las estampas con fotos y hasta la trama de la tela (redes, encaje, arneses).

La personalización pasa por cuatro máquinas, cada una con su trabajo:

| Máquina | Qué hace |
|---|---|
| **Mesa de Modelado** | El CORTE: largo de mangas, de pierna, tiro, cuello, calce (holgura) y la trama (red, arnés). |
| **Estación de Tintes** | El COLOR: colores lisos o con patrones (rayas, corazones, estrellas, lunares, vichy), por zona de la prenda y mezclando capas. |
| **Sublimadora** | Las ESTAMPAS: imprime fotos (del mod Camerapture) sobre la prenda. |
| **Guardarropas** | COMBINAR prendas y guardar outfits. |

## 1. Requisitos e instalación

- Minecraft **1.21.1** con **Fabric Loader** y **Fabric API**.
- **Trinkets** (obligatorio): la ropa se viste en slots de Trinkets, no en los de armadura.
- **GeckoLib** (obligatorio): las máquinas son modelos animados.
- **Camerapture** (opcional): solo hace falta para la Sublimadora (las fotos salen de ahí).
- **3D Skin Layers** (opcional): compatible; el mod ajusta la segunda capa de la skin debajo de la ropa.

Se instala como cualquier mod: el `.jar` va en la carpeta `mods` del perfil.

## 2. Primeros pasos: el recorrido completo

1. **Crafteá una prenda base** (ver Recetas): por ejemplo una remera con 8 lanas del mismo color, o unas medias con 6 lanas.
2. **Vestila:** abrí el inventario, pestaña de Trinkets, y poné la prenda en su slot (torso, piernas, medias o brazos).
3. **Cambiale el corte** en la Mesa de Modelado: poné los moldes en el dibujo de la prenda, fijalos con la chincheta, poné la prenda en la Entrada y cerrá la interfaz.
4. **Teñila** en la Estación de Tintes: elegí zonas del dibujo, poneles color y patrón, fijalos y apretá Teñir.
5. **Estampala** en la Sublimadora con una foto.
6. **Combiná** varias prendas en el Guardarropas y guardá el outfit.

Cada máquina **no consume la prenda ni los moldes**: la prenda sale modificada y los moldes y patrones se reusan para siempre. Lo que se gasta son los insumos: tinta (tintes vanilla) y papel.

## 3. Las prendas

![Personaje con medias, pantalón y remera](img/captura_jugador_2.png)

| Prenda | Slot de Trinkets | Se modela | Se tiñe | Se estampa |
|---|---|---|---|---|
| Remera | Torso | Largo, mangas (por lado), cuello, calce, trama | Sí | Sí (frente y espalda) |
| Pantalón | Piernas (exterior) | Largo de pierna (por lado), tiro, calce, trama | Sí | Sí |
| Medias | Medias | Largo arriba y abajo (por lado), calce, trama | Sí | Sí |
| Calientabrazos | Brazos | Cobertura arriba y abajo (por lado), calce, trama | Sí | Sí |
| Pollera | Piernas (exterior) | — (todavía sin moldes) | Sí | No |

**Capas de dibujo.** Las prendas se dibujan en orden fijo, así una no borra a la otra: cuerpo base → medias → calientabrazos → pantalón → remera → pollera. Donde una prenda no tiene tela (una media corta, una remera sin mangas) se ve lo que hay abajo.

**El ícono muestra la prenda de verdad.** Los íconos de remera, pantalón, pollera, medias y calientabrazos son de 64×64 y se arman con la misma tela que se ve puesta: aparecen las capas y los patrones de Tintes, cada manga o pierna con su color, las redes y los arneses, las fotos de la Sublimadora y el largo real (manga corta, crop, short, zoquete). La remera tiene un dibujo por cuello (redondo, V, polera).

**Varias prendas del mismo tipo.** Cada slot de Trinkets tiene **4 lugares**: se pueden llevar a la vez un croptop sobre un remerón largo, o medias de red debajo de unas medias cortas.

**La remera** sale en los 16 colores de lana. Su nombre cambia según el corte (croptop, musculosa, polera, remerón, remera) y el resto del corte aparece en el tooltip.

**Prendas viejas (armadura).** Hay cuatro prendas de una versión anterior que se equipan en los slots de armadura: Medias 3/4, Medias de Red, Traje de Maid y Buzo Oversize. No pasan por las máquinas.

## 4. Cuerpo base y ropa interior

El mod dibuja un **cuerpo base** debajo de la ropa (en vez de la skin pintada), así la ropa se ve pegada al cuerpo y las zonas descubiertas muestran piel. La cabeza siempre queda con la cara de tu skin.

### Elegir cuerpo

La **primera vez que te ponés una prenda** del mod se abre la pantalla **Elegí tu cuerpo**:

- **Vista previa 3D** tuya con el cuerpo y los colores que estás eligiendo, **sin ropa** y sin la segunda capa de la skin (tampoco la de 3D Skin Layers); se gira arrastrando.
- **Cuerpos:** "Mi propia skin" (el de siempre: liso, con el tono de tu skin) y 19 cuerpos dibujados. Los humanos son Estándar, Delgado, Atlético, Musculoso, Gordito, Velludo, Fem estándar, Fem atlética, Fem con curvas y Fem gordita. Los animales son Cebra, Dálmata, Leopardo, Lobo, Osito, Panda, Tigre, Vaca y Zorro. Todos se tiñen con tu tono de piel; las manchas y rayas de los animales quedan oscuras.
- **Colores por zona:** el cuerpo tiene cuatro zonas, que salen solas del dibujo:
  - **Base:** la piel, o el pelaje en los animales.
  - **Clara:** pancita, hocico y manchas claras (solo animales).
  - **Oscura:** rayas y manchas oscuras (solo animales).
  - **Rubor:** rodillas, codos, pecho, panza y cara (los cuerpos humanos lo traen marcado en el dibujo). En automático es **tu mismo color, más intenso y un poco más oscuro**: en una piel da un rosado tibio y en una skin azul, verde o gris, un tono más profundo del mismo color, sin manchas violetas. Se puede elegir a mano, por ejemplo un rosa. Con la zona Rubor elegida aparece el slider **Fuerza del rubor** (0 a 100%, de a 5; arranca en 80%): en 0 no hay rubor.

  Click en una zona para editarla. La Base arranca **calculada de tu skin** (**Tono de mi skin** la vuelve a eso). Clara, Oscura y Rubor arrancan en **automático**: salen del color Base, más claras u oscuras solas (marcadas con una "A"). El botón **Automática** las devuelve a ese estado.
- **De tu skin:** una paleta con hasta **5 colores sacados de tu skin** (los más usados, sin repetir parecidos). Click en uno lo pone en la zona elegida; por ejemplo, con una skin de osito, el marrón del pelaje en Base y el beige de la pancita en Clara. Los sliders Rojo/Verde/Azul ajustan la zona a mano.
- **Confirmar** lo guarda y la pantalla ya no vuelve a aparecer sola. **Ahora no** la cierra sin guardar y vuelve a aparecer la próxima vez que entres al mundo.

Los cuerpos dibujados están en alta resolución (6 veces la de una skin común). Cada uno está pintado para brazos anchos (classic) o finos (slim). Si tu skin tiene los otros, la textura se adapta sola; tus brazos no cambian de ancho.

### Comandos

Sin permisos especiales, cada quien cambia el propio:

| Comando | Qué hace |
|---|---|
| `/femclothes elegir` | Vuelve a abrir la pantalla Elegí tu cuerpo. |
| `/femclothes cuerpo <tipo>` | Cambia el cuerpo directo: `skin_real`, `estandar`, `delgado`, `atletico`, `musculoso`, `gordito`, `velludo`, `fem_estandar`, `fem_atletica`, `fem_curvas`, `fem_gordita`, `cebra`, `dalmata`, `leopardo`, `lobo`, `osito`, `panda`, `tigre`, `vaca`, `zorro`. Los cuerpos viejos (plano, curvy, binder) pasaron a Estándar y Fem con curvas. |
| `/femclothes interior <tipo>` | Ropa interior base: `basica`, `slip`, `boxer`, `bralette`, `deportiva`. |
| `/femclothes tono skin` | Tono de piel tomado de tu propia skin (el valor por defecto). |
| `/femclothes tono <número>` | Tono de piel a mano, como color RGB en decimal (ej. 14329120). |
| `/femclothes ver` | Muestra tu configuración actual. |
| `/femclothes reset` | Vuelve todo al valor por defecto. |

## 5. Cómo funcionan las máquinas (común a todas)

Las tres máquinas de confección (Modelado, Tintes, Sublimadora) comparten la misma lógica:

- **Configuración y fijadas.** En la interfaz armás el ajuste y lo **fijás** (con la chincheta o un botón). Lo fijado es lo que se aplica; lo que no está fijado es solo un borrador que se ve en la vista previa.
- **Vista previa 3D.** La columna izquierda muestra a tu personaje con la prenda como va a quedar. Se gira arrastrando con el mouse o con el botón Vista (frente, costado, espalda), y se hace zoom con la ruedita.
- **Diseños guardados.** Escribís un nombre y apretás **Guardar diseño**: se guarda todo lo fijado de esa prenda (hasta 8 diseños por prenda, en las tres máquinas). Click en un casillero numerado lo carga (el nombre aparece al pasar el mouse); click derecho lo borra.
- **Entrada y salida.** Cada máquina tiene un slot grande de **Entrada** (la prenda a procesar) y uno de **Salida** (la prenda terminada). Mientras trabaja, la máquina se anima y hace ruido; al terminar suena una campanita. El LED (rojo trabajando, verde con la prenda lista) **ilumina** el bloque y lo que tiene alrededor (nivel de luz 7). La barrita de progreso del frente, sobre fondo negro, se llena de izquierda a derecha y queda llena mientras la prenda terminada espera en la salida.
- **Colores de cada máquina.** Las tres interfaces son de pergamino y madera, pero cada una tiene su metal: la Estación de Tintes es **verdosa** (verdín), la Modeladora **cobriza** y la Sublimadora **dorada**. El color se ve en los filos, las esquinas, los slots y los botones.
- **Tiempos.** Modelado 15 s, Tintes 10 s, Sublimadora 15 s.
- **¡Cuidado!** Meter la mano (click derecho) en una máquina mientras trabaja lastima: la Modeladora corta, la de Tintes marea con los vapores y la Sublimadora quema.
- **Al romperlas**, la tinta cargada viaja adentro del ítem (como una shulker box). El resto de la configuración se pierde.

### Automatización con tolvas

Las máquinas se pueden encadenar con tolvas (hoppers):

| Cara del bloque | Qué entra o sale |
|---|---|
| **Arriba** | La prenda a procesar (entra sola y arranca sola). |
| **Atrás** | Los insumos: tintes (C, M, Y, K) y papel. |
| **Derecha** | La prenda terminada sale sola hacia un cofre o tolva de ese lado. |

Con tolva, la prenda **arranca sola** si hay un diseño fijado. Desde la interfaz, las tres tienen un botón sobre la flecha Entrada → Salida: **Modelar**, **Teñir** y **Prensar**. Además, la Modeladora arranca al cerrar la interfaz y la Sublimadora al cerrar la tapa. *(La cadena completa de máquinas todavía no se probó a fondo.)*

## 6. Mesa de Modelado

![Mesa de Modelado, pantalón](img/captura_modelado_pantalon.png)

La Modeladora cambia el **corte** de la prenda. En el centro está el **dibujo de la prenda** con cuadraditos (pines) en cada parte: cuello, mangas, largo, tiro, botamangas, etc. Cada pin acepta los moldes que tienen sentido ahí.

**Cómo se usa:**

1. Elegí la prenda con el botón **Categoría** (remera, pantalón, medias, calientabrazos).
2. Arrastrá un **molde** al pin que quieras cambiar.
3. Apretá la **chincheta** del pin: el corte queda fijado y el molde vuelve al almacén. En el pin queda un ícono fantasma del molde fijado.
4. Click en la chincheta de un pin fijado lo quita.
5. Poné la prenda en la **Entrada** y apretá **Modelar** (el botón sobre la flecha), o **cerrá la interfaz**: la máquina se enciende sola y empieza a producir. Modelar solo se habilita con prenda en la Entrada, algún corte fijado y la Salida libre. Mientras está encendida (sin estar produciendo), un click en el bloque la apaga.

![Mesa de Modelado, medias](img/captura_modelado_medias.png)

**Simetría (remera):** con simetría activada, soltar un molde en cualquiera de las dos mangas fija las dos iguales. Sin simetría, cada manga tiene su propio largo.

**Almacén:** a la derecha hay un almacén general de moldes (27 lugares) y uno por prenda, "Moldes de <prenda>" (36 lugares, 9×4), para tener los moldes de cada categoría a mano.

### Pines por prenda

| Prenda | Pines |
|---|---|
| Remera | Cuello, Manga Izq., Manga Der., Corte inferior (largo), Calce, 3 de Materiales (patrones y redes) |
| Pantalón | Tiro, Corte Bota Izq., Corte Bota Der., Calce, 3 de Materiales |
| Medias | Corte Superior e Inferior de cada pierna, Calce, 3 de Personalización por lado |
| Calientabrazos | Corte Superior e Inferior de cada brazo, Calce, 3 de Personalización por lado |

### Los moldes

| Molde | Dónde va | Qué hace |
|---|---|---|
| **Molde de rango** (Mínimo, Corto, Medio, Medio largo, Largo, Máximo) | Mangas, botas, cortes sup./inf. | Hasta dónde llega la tela. Sirve para todas las prendas: cada una lo traduce a su propia medida (una manga "corta" llega al codo, una media "corta" es un zoquete). |
| **Molde de torso** (Corto, Medio, Largo) | Corte inferior de remera, Tiro de pantalón | Largo de la remera (crop, normal, largo) o altura del tiro. |
| **Molde de cuello** (Redondo, V, Polera) | Cuello de remera | Forma del cuello. |
| **Molde de manga** | Activo (remera, calientabrazos) | Molde viejo de manga: cada uso pasa al largo siguiente. |
| **Molde de calce** (Pegado, Ajustado, Normal, Suelto, Oversize) | Calce | Qué tan despegada del cuerpo va la prenda. |
| **Molde de red** | Materiales / Personalización | La trama de la tela (ver abajo). |
| **Molde de corte** | Activo | Un combo de cortes guardado en un solo ítem. |

**Cómo funcionan los cortes de extremidades.** En medias, calientabrazos y pantalón, el largo son **dos anclajes que se cruzan**: el corte superior dice desde dónde arranca la tela y el inferior hasta dónde llega. Así se arman desde zoquetes hasta medias hasta el muslo, o calientabrazos que solo cubren el antebrazo.

### Redes y arneses (trama de la tela)

El molde de red **perfora** la tela ya teñida: deja ver la piel por los agujeros y refuerza solo los bordes (cuello, puños, dobladillo, el borde del largo elegido) para que la prenda no se vea deshilachada.

| Molde | Cómo se ve |
|---|---|
| **Red fina** | Malla diagonal chica y apretada. |
| **Red gruesa** | Malla diagonal grande, bien abierta. |
| **Red hexagonal** | Panal de abejas. |
| **Red perforada** | Tela sólida con agujeritos redondos, tipo broderie. |
| **Encaje** | Rombos grandes con un punto en el centro. |
| **Rayas caladas** | Franjas horizontales abiertas, con puentes alternados. |
| **Escocesa** | Cuadrícula recta. |
| **Arnés cruzado** | Tiras en X por cara con un anillo plateado en el cruce. |
| **Arnés de tirantes** | Dos tirantes verticales y una banda al medio, anillos en los cruces. |
| **Arnés de bandas** | Bandas horizontales con un anillo cada una. |
| **Textura lisa** | Quita la red que tuviera la prenda. |

![Redes: fina, gruesa, hexagonal, perforada, encaje, rayas caladas, escocesa](img/redes.png)

![Arneses: cruzado, tirantes, bandas (torso con hombros)](img/arneses.png)

**Arneses.** Son lo contrario de una red: todo queda abierto salvo las tiras. Las tiras siguen los bordes de corte reales, así que si acortás la remera a crop, el arnés se acomoda solo. En la remera, las tiras pasan por los **hombros** y empalman siempre con el frente y la espalda. En las mangas y calientabrazos que llegan al hombro, el **hombro** (la tapa de arriba del brazo) lleva el mismo dibujo del arnés que los costados (la X con su anillo, los tirantes o las bandas) más un marco que empalma con cada tira que sube por el brazo. Los anillos son plateados y no se tiñen; las tiras toman el color de la prenda.

## 7. Estación de Tintes

La Estación de Tintes pinta la prenda. En el bloque, la pantallita del frente muestra la prenda cargada, con el mismo marco y tamaño que en la Modeladora y la Sublimadora. Funciona por **cuadraditos**: el dibujo de la prenda (el mismo de la Modeladora) tiene un cuadradito por zona, y **cada cuadradito es una capa de color** con su propia configuración.

> *Captura pendiente: la interfaz de Tintes cambió después de las últimas capturas.*

### Tinta

La máquina usa tinta **CMYK** (cian, magenta, amarillo y negro), que se carga con los tintes vanilla correspondientes: click derecho con el tinte en la mano, o por tolva desde atrás. Cada tanque guarda hasta 64 dosis. Cada teñido gasta **una dosis de cada canal** que use alguno de los colores fijados. Cuánta tinta queda se ve **dentro de cada slider C/M/Y/K**: el fondo del slider se llena con el color de la tinta según lo cargado y a la derecha dice `n/64` (en rojo si está vacío).

Cada color se arma con 5 sliders de 0 a 100% en pasos de 5%: **Cyan, Magenta, Yellow, Key** (negro) y **Transparencia**. La transparencia no gasta tinta: al 100% hace un **recorte** (un agujero en la tela) y entre medio deja la tela **calada**, con un tramado fino de puntitos abiertos que se lee como tul. Va por color, así se puede, por ejemplo, calar solo los corazones de un patrón. Las muestras con transparencia se ven con fondo a cuadros.

### Los cuadraditos

| Prenda | Cuadraditos de zona | Cuadraditos de prenda entera |
|---|---|---|
| Remera | Cuello (solo el borde del escote), Manga Izq., Manga Der. (incluyen el hombro), Pecho (el del medio: todo el cuerpo y la parte de arriba), Borde inferior | Los dos de arriba a los costados |
| Pantalón | Cintura (Tiro), Bota Izq., Bota Der. | Los 3 de Materiales |
| Medias | Superior e Inferior de cada pierna | Los 6 de Personalización |
| Calientabrazos | Superior e Inferior de cada brazo | Los 6 de Personalización |
| Pollera | — | 3 (fila fija) |

Las zonas cubren también las **tapas** de cada pieza (la parte de arriba del torso va con el Pecho, la de abajo con el Borde inferior; la planta de la media y la punta del calientabrazos con Inferior), y los patrones de prenda entera cruzan el hombro de adelante hacia atrás sin cortarse. El **Cuello** sigue la forma real del molde de cuello (redondo, en V) y siempre se pinta arriba del Pecho.

En medias y calientabrazos, el dibujo se lee **de frente**: el cuadradito de la izquierda del dibujo es la pierna o brazo **derecho** del personaje (igual que en la Modeladora).

### Cómo se usa

1. Elegí la prenda con **Categoría**.
2. **Click en un cuadradito** para seleccionarlo (queda con marco dorado). Los sliders y botones editan ese cuadradito.
3. Poné un **molde de patrón** en el cuadradito si querés patrón. **Sin molde, la capa es un color liso** en esa zona.
4. Elegí el color con los **sliders CMYK** (a la derecha, con la muestra del color).
5. Apretá la **chincheta** del cuadradito para **fijarlo**:
   - con molde: queda fijado con ese patrón y el molde vuelve al **almacén** (30 lugares, en la columna izquierda debajo de Guardar diseño);
   - sin molde: queda fijado como color liso;
   - si ya estaba fijado y no tiene molde: se desfija.
6. Poné la prenda en la **Entrada** y apretá **Teñir** (el botón sobre la flecha).

Debajo de cada cuadradito que participa aparece una tira con su color. La vista previa muestra **exactamente lo fijado** más el cuadradito que estás editando (si ese no está fijado, la línea "Editando" dice *sin fijar*: no va a salir en la prenda). Si ponés un molde nuevo en un cuadradito que ya estaba fijado, se usa ese molde sin tener que volver a clavar la chincheta.

**Resaltado en 3D.** Con el mouse encima de un cuadradito del dibujo (o de su chincheta, o de su fila en el panel de capas), la vista previa **apaga todo lo que no es su zona**: lo demás de la prenda se oscurece y la zona queda con su color real. En los cuadraditos de prenda entera no se apaga nada.

Al teñir, el **ícono** del ítem toma el color de la capa lisa de prenda entera que quede más arriba (el ícono no muestra patrones).

### Panel de capas

Abajo a la derecha hay una **lista de las capas** que participan (las fijadas más la que estás editando), ordenadas como se pintan: **la de arriba de la lista tapa a las de abajo**. Cada fila muestra:

| Parte de la fila | Qué hace |
|---|---|
| **Ojo** | Click para ocultar o mostrar la capa. Una capa oculta **sigue fijada** y se guarda en los diseños, pero **no se aplica al teñir, no gasta tinta y no sale en la vista previa**. Si todas las fijadas están ocultas, Teñir avisa que no hay nada para aplicar. |
| **Muestra** | Los colores de la capa (una franja por color). |
| **Nombre** | Zona y patrón (o "liso"). En cursiva si todavía no está fijada. Click selecciona la capa, igual que tocar su cuadradito. |
| **▲ ▼** | Sube o baja esa capa en el orden de pintado (y la selecciona). |

La fila de la capa seleccionada tiene marco dorado. Pasar el mouse por una fila resalta su zona en la vista previa.

### Controles de cada cuadradito

| Control | Qué hace |
|---|---|
| **Mezcla** | Normal (tapa lo de abajo), Multiplicar (oscurece: ideal para un patrón sobre otros colores), Superponer (contraste). |
| **Opacidad** | De 10% a 100%. |
| **▼ Capa n/N ▲** | El orden de pintado: más arriba tapa a las de abajo. Por defecto, las de prenda entera van abajo y las de zona arriba. Es el mismo orden del panel de capas. |
| **Tamaño** | Extra chico a Extra grande. |
| **Ángulo** | Gira las rayas en pasos de 15°. |
| **Posición** | Corre el patrón (rayas: la franja; motivos: la grilla o el logo). |
| **Forma / Repetición** | Con rayas: Alternado, Arriba, Abajo, Medio, Tres rayas. Con motivos: Grilla, Ladrillo, Disperso, Único. |
| **Azar** | Nueva tirada al azar (posiciones de Disperso y colores de Variación: Aleatorio). |
| **Invertir** | Pinta el negativo del patrón. |

### Varios colores en una capa

A la derecha, sobre la muestra grande, hay **tres muestras de color**: click en una elige cuál editan los sliders (si estaba apagada, se prende).

| Control | Qué hace |
|---|---|
| **Colores: 1 / 2 / 3** | Cuántos colores usa la capa. |
| **Contorno** | Una línea alrededor de cada motivo o raya, con el **último** color activo (necesita al menos 2 colores). |
| **Variación: Fijo** | Todo el relleno del Color 1. |
| **Variación: Alternar** | Cada motivo, raya o cuadro toma el color siguiente. |
| **Variación: Aleatorio** | Cada uno toma un color al azar (Azar vuelve a sortear). |
| **Variación: Degradé** | Pasa de un color al otro de arriba a abajo. Funciona también en capas lisas, sin molde. |

Ejemplos: corazones rojos y rosas alternados con contorno negro (3 colores, Contorno sí, Alternar); una remera en degradé (cuadradito de prenda entera, sin molde, 2 colores, Degradé); rayas multicolor (Alternar con 3 colores).

### Patrones

| Molde de patrón | Tipo |
|---|---|
| Rayas superiores, Rayas alternadas, Tres rayas | Rayas (la forma se cambia con el botón Forma) |
| Corazones, Estrellas, Lunares | Motivos repetidos |
| Vichy | Cuadros de tres tonos con un solo color (los cruces más oscuros) |

![Patrones de motivo: corazones en Grilla, Ladrillo, Disperso y Único](img/motivos.png)

**Repeticiones de motivos:** **Grilla** (filas parejas), **Ladrillo** (cada fila corrida media posición), **Disperso** (al azar, con huecos; Azar cambia la tirada), **Único** (un motivo grande en el frente de cada pieza, tipo logo; Posición lo sube o baja). Los motivos se reparten para que entren justo alrededor de la pieza: el que cae en la costura de atrás sigue del otro lado sin cortarse.

## 8. Sublimadora

La Sublimadora **imprime fotos** del mod Camerapture sobre remeras, pantalones, medias y calientabrazos. La pollera todavía no se puede estampar: su geometría en abanico no encaja con el mapeo de estampas.

> *Captura pendiente: la interfaz se rehízo el 2026-09-28 con el mismo estilo que Tintes y la Modeladora.*

**La interfaz** tiene el mismo esqueleto que sus hermanas:

| Zona | Qué hay |
|---|---|
| **Izquierda** | Vista previa 3D, botón Vista, nombre y **Guardar diseño**. |
| **Centro** | **Categoría**; el dibujo de la prenda dos veces, **Frente** y **Espalda**, cada una con su slot de foto y su chincheta; el cinturón **Entrada → Salida** con **Prensar** sobre la flecha; los 8 casilleros de diseño; y los controles de la cara elegida: **Escala**, **Posición X**, **Posición Y**, **Ángulo** y **Cara**, más **Simetría** en medias y calientabrazos. |
| **Derecha** | Los tanques de tinta **C, M, Y, K** y el de **papel** con su nivel (n/64), la guía de pasos y el **almacén** de 27 fotos (3 filas). Al romper la máquina, las fotos del almacén se tiran al piso. |

El dibujo muestra la prenda que está en la Entrada, con su color. Si no hay ninguna, muestra la prenda terminada de la Salida, y si tampoco, una de la categoría elegida.

**Cómo se usa:**

1. Elegí la prenda con **Categoría** (o poné una en la Entrada: la categoría la sigue sola).
2. Poné una foto en el slot de **Frente** o de **Espalda**. **Tocar un slot de foto elige esa cara**: los controles de abajo editan esa cara, que queda con marco dorado.
3. Ajustá **Escala**, **X**, **Y** y **Ángulo**. A escala mínima la foto queda chica y centrada, tipo logo; a escala máxima cubre la prenda entera (full print), recortada a la silueta.
4. Clavá la **chincheta** de cada cara que quieras imprimir. **Solo se estampan las caras fijadas que tienen foto**. La vista previa muestra lo fijado más la cara que estás editando; la línea "Editando" dice si esa cara está fijada.
5. Poné la prenda en la **Entrada** y apretá **Prensar**: la tapa baja y arranca. Cerrar la tapa a mano también arranca.

**Detalles:**

- **Insumos:** tinta CMYK (tintes vanilla, hasta 64 por tanque) y **papel** (hasta 64). Cada cara estampada gasta una dosis de cada color; cada prensado gasta una hoja de papel. Las fotos **no se consumen**: quedan cargadas para reimprimir.
- **Simetría lateral (medias y calientabrazos):** las dos piernas (o brazos) llevan la misma foto. Con **Simetría: No** llevan la misma copia; con **Simetría: Sí**, la izquierda lleva el **espejo** de la derecha, así un logo corrido hacia afuera queda hacia afuera en las dos. Ojo: un texto en la foto se lee al revés en el lado espejado.
- **Diseños guardados:** como en las otras máquinas, nombre + Guardar diseño y 8 casilleros por categoría: guardan el ajuste de las dos caras, cuáles están fijadas y la simetría. Click carga, click derecho borra. Las "fijadas" de la versión anterior pasaron a ser diseños "#1", "#2"...
- **Prensado:** 15 segundos, con vapor, pitidos y LED rojo; al terminar, LED verde y campanita.
- Frente y espalda se pueden estampar en una sola pasada. No pisa una cara ya estampada: una prenda con el frente hecho puede volver a entrar para imprimirle la espalda.
- Las fotos con transparencia (PNG) funcionan.

## 9. Guardarropas

El Guardarropas sirve para **combinar prendas**: tiene 4 lugares por categoría (remera, pantalón, medias, calientabrazos), así se pueden probar juntas varias prendas del mismo tipo (un croptop sobre un remerón, un pantalón con una pollera y una calza), con vista previa. **Guardar outfit** y **Equipar** guardan y se ponen la combinación.

*El sistema de estilos guardados todavía está en construcción.*

## 10. Comandos

**Para todos los jugadores** (cambian solo tu apariencia): ver la sección 4, `/femclothes elegir`, `cuerpo`, `interior`, `tono`, `ver`, `reset`.

**Para operadores** (afectan a todo el servidor):

| Comando | Qué hace |
|---|---|
| `/femclothes debug instantaneo` | Las máquinas terminan al instante (la tinta y el papel se siguen gastando igual). |
| `/femclothes debug normal` | Vuelve a los tiempos reales. |
| `/femclothes debug ver` | Muestra en qué modo está. |
| `/femclothes debug patrones` | Te da shulker boxes con uno de cada molde de patrón. |
| `/femclothes debug moldes` | Te da shulker boxes con uno de cada molde de corte y de red. |

**Del cliente** (solo cambian lo que VOS ves, para probar):

| Comando | Qué hace |
|---|---|
| `/femclothesdebug skin` | Cicla skins de prueba (Steve, Alex, Zuri, Noor, Kai) y la tuya. |
| `/femclothesdebug slim` / `ancho` / `automodelo` | Fuerza brazos finos, anchos, o los de la skin. |
| `/femclothesdebug reset` | Vuelve a tu skin. |

## 11. Recetas

![Íconos de los ítems del mod](img/iconos.png)

| Ítem | Receta |
|---|---|
| Remera (16 colores) | 8 lanas del mismo color: `L _ L / L L L / L L L` |
| Medias Color Pleno (16 colores) | 6 lanas del mismo color: `L _ L / L _ L / L _ L` |
| Pantalón | 5 lanas blancas: `L _ L / L L L` |
| Calentadores de Brazo | 2 lanas blancas, una arriba de la otra |
| Medias 3/4 (armadura) | 4 lanas blancas: `L _ L / L _ L` |
| Medias de Red (armadura) | 4 hilos: `H _ H / H _ H` |
| Traje de Maid (armadura) | 8 lanas blancas alrededor de 1 lana negra |
| Buzo Oversize (armadura) | 8 lanas blancas: `L L L / L L L / L _ L` |
| Mesa de Modelado | 3 papeles, hilo + tijeras + hilo, 3 tablones de roble |
| Sublimadora | Hierro alrededor, un pistón en el medio y un horno abajo |
| Molde de manga | Papel + hilo (sin forma) |
| Rayas alternadas | Papel / hilo / papel (filas) |
| Rayas superiores | Hilo / papel / papel (filas) |
| Tres rayas | Hilo / hilo / papel (filas) |

**Sin receta todavía (solo en creativo o con `/femclothes debug`):** Estación de Tintes, Guardarropas, Pollera, moldes de rango, torso, cuello, calce, red y arnés, molde de corte, y los patrones de corazones, estrellas, lunares y vichy.

Todo el contenido del mod está en su propia pestaña del inventario creativo: **FemClothes**.

## 12. Estado y pendientes

Funciones planeadas que todavía no están:

- Volumen 3D real en la ropa (integración con 3D Skin Layers).
- Cadena de máquinas por tolvas, probada de punta a punta.
- La prenda cargada visible encima de cada máquina y una pantallita con vista previa en el bloque.
- Guardarropas: sistema de estilos guardados.
- La ropa en el brazo en primera persona.
- Piernas redondeadas opcionales (versión 2).
- Recetas para los ítems que hoy son solo de creativo.

# Anexo técnico

Esta parte es para quien quiera entender o extender el mod.

## A. Organización del código

| Paquete | Contenido |
|---|---|
| `item` | Prendas, moldes de patrón, componentes de datos (`FemclothesComponents`), redes (`PatronRed`). |
| `modelado` | Mesa de Modelado: bloque, block entity, pantalla, moldes de corte, `ComboCorte`, `PrendaModelado` (aplica un corte a una prenda). |
| `tinturas` | Estación de Tintes: cuadraditos (`Casilla`), capas, diseños, teñido. |
| `sublimadora` | Sublimadora, remera y su `Variante` (largo/manga/cuello), estampas (`EstampaTextures`). |
| `guardarropas` | Guardarropas. |
| `region` | Lado (izq./der./ambas), regiones de pintura (`RegionPintura`), `RegionResolver` (lee y escribe capas y colores por lado), `ModoMezcla`. |
| `render` | Composición de texturas (`ClothingTextureCache`), geometría (`CuerpoGeometria`, layout de skin a 8x), generador de patrones (`PatronGenerador`, `Motivo`, `Repeticion`, `Variacion`), el renderer único de ropa (`GarmentFeatureRenderer`). |
| `body` | Cuerpo base (`CuerpoBase`: las 19 máscaras en gris de `textures/entity/cuerpo/`, en HD de 384×384, multiplicadas canal por canal por el tono en `CuerpoBaseTextures`), ropa interior, perfil por jugador (`PerfilCuerpo.elegido`), comandos y paquetes de la GUI de elegir cuerpo (`RedCuerpo`). La GUI es `client/ElegirCuerpoScreen` y el disparador `ElegirCuerpoCliente`. |
| `client` | Pantallas, vista previa 3D, estilo pergamino, piezas de cada prenda (`PiezasDelMod`). |

## B. Cómo se dibuja una prenda

1. `GarmentFeatureRenderer` junta todo lo que el jugador tiene puesto en Trinkets y lo dibuja desde un solo lugar, ordenado por **capa** (`Capa`), sobre el cuerpo base.
2. Cada prenda devuelve sus **piezas** (`PiezasDelMod`): qué parte del cuerpo cubre, en qué capa, con qué textura y qué filas de la caja son visibles (así se recortan largos sin generar un archivo por cada largo).
3. La textura se **compone en tiempo real** sobre un atlas con el layout de la skin a escala 8x (512×512) y se cachea por combinación: color base, capas de color, estampas, recorte y red.

## C. Capas de color (Estación de Tintes)

Cada capa es un `RegionResolver.CapaPatron` guardado en la lista `femclothes:capas_tinte` de la prenda: patrón opcional (sin patrón = liso), color principal y extras, tamaño, ángulo, posición, forma, invertido, región, modo de mezcla, opacidad, repetición, semilla, contorno y variación. Las capas viejas de la Modeladora (componentes `pattern_*`, con variantes `right_*` para el lado derecho) se siguen leyendo.

Las capas **ocultas** del panel (`Casilla.oculta`, NBT `Oculta`) no entran en `TinturasBlockEntity.capasDe`, así que no se aplican ni gastan tinta.

**Resaltado de la vista previa.** `prendaDeVistaPrevia(resaltada)` le suma a la copia de la vista previa una capa "velo": lisa, negra, Multiplicar al 60%, con `fueraDeRegion = true`. Ese flag (que no está en el codec, nunca se guarda) hace que `CapaMascara.cobertura` pinte todo lo de AFUERA de la región en vez de adentro.

Al componer, cada pixel parte del color base de la tela y cada capa se **funde** encima (`ClothingTextureCache.mezclar`) según su cobertura, su modo y su opacidad.

**Canales de la máscara.** Las máscaras de patrón (`PatronGenerador`) guardan en cada pixel todo lo necesario para elegir el color al componer, sin generar una máscara por combinación de colores:

| Canal | Contenido |
|---|---|
| Alfa | Cobertura 0–255 (el vichy usa 128 en las franjas simples). |
| Rojo | Bit 7: es contorno. Bits 0–6: valor al azar de esa repetición. |
| Verde | Número de repetición (para Alternar). |
| Azul | Altura dentro de la pieza, de 0 arriba a 255 abajo (para Degradé). |

## D. Cómo agregar contenido

- **Un motivo nuevo:** agregar el dibujo (filas de `X` y `.`) en el enum `Motivo`, registrar un `ClothingPatternItem` con ese motivo en `FemclothesItems`, y sumar ícono, modelo, traducción y entrada en la pestaña (`Femclothes.PESTANA`).
- **Una red nueva:** agregar el valor al **final** del enum `PatronRed` (el orden viaja por red), su `Dibujo` y la lógica en `ClothingTextureCache.esHilo` (o `perforarArnes` si es un arnés), y registrar el `MoldeRedItem` en `ModeladoMod`.
- **Una prenda nueva:** registrar el ítem, declarar su `Garment` en `PrendasDelMod`, sus piezas en `PiezasDelMod`, y el slot de Trinkets si hace falta uno nuevo.

## E. Compilar y probar

- `gradlew build` genera el `.jar` en `build/libs/`.
- Para probar: copiar el `.jar` a la carpeta `mods` del perfil **con el juego cerrado**.
- Los fondos de interfaz de pergamino se generan con `tools/generar_textura_modelado.py`, `generar_textura_tinturas.py` y `generar_textura_sublimadora.py` (este último reusa las funciones del de Tintes). Los íconos de las prendas salen de `tools/generar_iconos_prendas.py`: por prenda, un `_sombra.png` (silueta y relieve) y un `_mapa.png` (de qué cara del cuerpo y de qué punto se saca cada píxel) en `textures/item/icono/`; `IconoPrenda` los combina en Java con las texturas de `PiezasDePrenda`, recorta las filas que la pieza no tapa y pone el contorno. El color de cada máquina sale de `TEMAS`/`aplicar_tema` (verdín, cobre, oro), con los mismos valores que `EstiloPergamino.Tema` usa para los botones.
- Este manual se genera con `python tools/generar_manual.py`, que arma las imágenes y convierte `docs/manual/MANUAL.md` en `docs/manual/FemClothes_Manual.docx`.
