# ModaMod — Manual del mod

Versión del mod: 0.1.0 · Minecraft 1.21.1 (Fabric) · Manual actualizado: {{FECHA}}

![Personaje vestido con prendas del mod](img/captura_jugador_1.png)

ModaMod agrega ropa que se viste SOBRE el cuerpo del personaje: remeras, pantalones, medias, calientabrazos, polleras y capas que siguen la forma del modelo, se superponen en capas (una media debajo de un pantalón, una remera arriba de todo) y se pueden personalizar casi por completo: el corte, el color, los patrones, las estampas con fotos y hasta la trama de la tela (redes, encaje, arneses).

La personalización pasa por cuatro máquinas, cada una con su trabajo (más el Maniquí para exhibir y la Mesa de estilado para los apliques):

| Máquina | Qué hace |
|---|---|
| **Mesa de Modelado** | El CORTE: largo de mangas, de pierna, tiro, cuello, calce (holgura) y la trama (red, arnés). |
| **Estación de Tintes** | El COLOR: colores lisos o con patrones (rayas, corazones, estrellas, lunares, vichy), por zona de la prenda y mezclando capas. |
| **Sublimadora** | Las ESTAMPAS: imprime fotos (del mod Camerapture) sobre la prenda. |
| **Guardarropas** | COMBINAR prendas y guardar outfits. |
| **Maniquí** | EXHIBIR un outfit: la ropa se ve puesta en la figura, que puede girar. |
| **Mesa de estilado** | APLIQUES 3D (moño, mariposa, flor) pegados a cualquier prenda. |

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
| Pollera | Piernas (exterior) | Forma (campana o tableada), largo (6), calce, trama | Sí | Sí (frente y espalda) |
| Capa | Espalda | Largo (7), ruedo (recto, redondeado o con cola), capucha, cuello alto | Sí (exterior, forro y detalles por separado) | Sí (Frente = exterior, Espalda = forro) |
| Chaqueta (Hoodie) | Chaqueta | Igual que la remera (en las máquinas cuenta como remera) | Sí | Sí (frente y espalda) |

**Capas de dibujo.** Las prendas se dibujan en orden fijo, así una no borra a la otra: cuerpo base → medias → calientabrazos → pantalón → remera → pollera → chaqueta. Donde una prenda no tiene tela (una media corta, una remera sin mangas) se ve lo que hay abajo.

**El ícono muestra la prenda de verdad.** Los íconos de remera, pantalón, pollera, capa, medias y calientabrazos son de 64×64 y se arman con la misma tela que se ve puesta: aparecen las capas y los patrones de Tintes, cada manga o pierna con su color, las redes y los arneses, las fotos de la Sublimadora y el largo real (manga corta, crop, short, zoquete). La remera tiene un dibujo por cuello (redondo, V, polera, cuadrado, corazón).

**La pollera** es una malla propia que baja desde la cintura y se abre hacia el ruedo. Puede ser **campana** (lisa) o **tableada** (pliegues en V) y tiene 7 largos, del ultramicro hasta el tobillo. **Se mueve con vos:** la tela choca con las piernas (con su forma y su pose reales: caminando, agachada, sentada o nadando, la pierna la empuja hacia afuera y nunca la atraviesa), el ruedo queda atrás al caminar o correr, se abre al caer, se achica al saltar, se balancea de costado y se retuerce un poco con cada paso (la misma inercia que usa la capa vanilla). **Cuelga hacia abajo** aunque te agaches: el torso se inclina pero la tela no se va para atrás, así las piernas no la atraviesan ni la estiran como una bolsa. Con busto, pasa tirante por fuera de la cola (sin meterse entre las nalgas). **Twirl:** con una pollera puesta, apretá **R** (se cambia en Controles → ModaMod → "Girar (con pollera)") y el personaje da una vuelta entera con los brazos abiertos mientras la pollera se abre casi horizontal y gira arrastrada; los demás jugadores también lo ven. Su tela tiene el mismo formato que las demás prendas, así que se tiñe con patrones y zonas, se estampa y se le puede poner red. **Los patrones no se deforman:** lunares, motivos y rayas miden lo mismo en la cintura que en el ruedo y de frente que de costado, en cualquier forma y largo (el dibujo se "despliega" sobre la campana, como el molde de una pollera real; la costura queda en el centro de la espalda). La cintura es casi recta y tapa sola la unión con el torso (ya no hay cinto aparte). **Varias polleras:** si llevás más de una (el slot de Pantalón/pollera tiene 3 lugares), se dibujan todas, una debajo de otra según el slot: la de más arriba en la lista queda abajo y cada una siguiente sale un poco más afuera; igual en el Maniquí y el Guardarropas. Los motivos de la pollera se dibujan a resolución completa (antes se achicaban a la mitad y salían borrosos).

**La capa** cuelga de los hombros y se mueve como tela: usa la misma inercia que la capa vanilla (se levanta al correr, rebota con los saltos, se balancea de costado y se abre al agacharte), pero no es una placa rígida: arriba sigue a la espalda y se va curvando hacia el ruedo, le baja una onda al moverte, se mece un poco quieta y se abre lo necesario para no atravesar las piernas. Va en su propio slot de Trinkets, **Espalda**. Tiene 7 largos (del capelet al tobillo), ruedo **recto**, **redondeado** (las puntas suben en arco) o **con cola** (10 px de tela de más que se apoyan en el piso y se arrastran detrás de vos; sentado, montado, nadando, durmiendo o gateando no se apoya), **capucha** caída sobre la espalda (con **H** te la ponés) y **cuello alto** abierto detrás de la cabeza; todo se elige en la Modeladora. El **forro** (la cara de adentro) se tiñe y se estampa aparte del exterior. **La del mod manda:** con una capa del mod puesta, la capa vanilla (de Minecraft o de Optifine/Minecon) no se dibuja; con élitros puestos es al revés, se ven los élitros y la del mod se esconde.

**Dónde se ponen.** En el inventario, al lado del muñeco hay una columna con tres lugares: **Remera/Chaqueta** (arriba), **Pantalón o pollera** y **Medias** (abajo). Pasando el mouse por un grupo se despliegan sus lugares. La **Capa** se abre desde el slot de la **pechera** (como los élitros) y los **Calentadores de brazo** desde el de la **mano secundaria**. Cada slot tiene un dibujito de la prenda que lleva y, vacío, al pasar el mouse dice qué va ahí, en qué capa se dibuja y cuántos lugares tiene.

**Varias prendas del mismo tipo.** Cada slot de Trinkets tiene **4 lugares**: se pueden llevar a la vez un croptop sobre un remerón largo, o medias de red debajo de unas medias cortas.

**La remera** sale en los 16 colores de lana. Su nombre cambia según el corte (croptop, musculosa, polera, remerón, remera) y el resto del corte aparece en el tooltip. Los cuellos son **redondo, en V, polera, cuadrado y corazón** (un molde de cuello por cada uno, con su dibujo). El cuadrado y el corazón no tienen archivos propios: el juego recorta el escote al armar la tela. El **cuadrado** es un recorte recto en el frente; el **corazón** es más ancho y sus dos lóbulos siguen la parte de arriba de cada pecho hasta una punta al centro; la tela empieza justo arriba del medio del pecho. La tapa de arriba de la prenda (por la que asoma el cuello) tiene el mismo ancho que el corte del frente. Con el cuello **polera** lleva un **cuellito** alto en 3D alrededor del cuello (acanalado, del color de la remera; tapa la barbilla, no la boca).

**Hoodie con otras prendas.** Con **pollera**, el ruedo del hoodie se abre por fuera de la pollera (el elástico no la aprieta) y la pollera asoma por debajo. Con **capa**, la capa cuelga por fuera del hoodie y de su capucha caída; la capucha y el cuello alto de la capa no se dibujan (manda la del hoodie). La **capucha puesta** cierra abajo y alrededor de la cara (marco de forro también bajo la barbilla).

**Silueta del hoodie.** El torso del hoodie va **más pegado arriba y se ensancha hacia abajo** (al 35 % de la holgura en el pecho y al 100 % en el ruedo), con el ruedo elástico que aprieta al final. Donde la tela se estrecha de golpe (el puño y el ruedo elásticos) hay un anillo de tela que tapa el borde, así no se ve el interior desde abajo.

**Mangas y ruedos.** En las prendas holgadas, los costados del torso y el lado de adentro de las mangas (donde se tocan) casi no se inflan, así manga y torso no se meten uno en el otro. La boca de las mangas, las botamangas y los ruedos cortados se cierran con un anillo de tela entre la prenda y el cuerpo (ya no se ve el hueco).

**Chaquetas.** Categoría nueva que va **encima de la remera y de la pollera**, en su propio slot de Trinkets (**Chaqueta**, 4 lugares). La primera es el **Hoodie**: sale largo, con manga larga y calce **Oversize**, y tiene bolsillo canguro, puños y ruedo **elásticos** (la última fila aprieta y la tela hace globo arriba, sin colgar), **cordones** y **capucha**. **Capucha:** apretá **H** (Controles → ModaMod → "Subir/bajar capucha") para ponértela o bajarla; los demás lo ven. La misma tecla sube y baja la capucha de la **capa**. En las máquinas el hoodie se trata como una remera: la Modeladora le cambia largo, mangas, cuello, calce y trama con los mismos moldes, Tintes usa las zonas de la remera y la Sublimadora estampa frente y espalda (comparte los diseños guardados de la remera). La capucha y los cordones son lisos, del color base (no llevan patrones ni fotos todavía).

**Chaqueta modular** (2026-10-07, "como se te ocurre que mejor hacemos las chaquetas"). La chaqueta tiene **su propia categoría en la Modeladora** (la prenda que ponés en la máquina elige la pestaña sola). Usa los moldes de siempre para el **cuello**, las **mangas** (los dos lados), el **largo** (el pin del torso), el **calce** y los 3 **materiales**, y suma tres pines propios con **moldes de Chaqueta**: **Frente** (cerrado o **abierto**: una franja del medio queda transparente y se ve lo que llevás debajo, con las dos orillas más oscuras), **Capucha** (con o sin: sin capucha no hay capucha, cordones, bolsillo canguro ni tecla H) y **Remate** (puños y ruedo **elásticos** o **rectos**). De fábrica es el hoodie de siempre (frente cerrado, con capucha, elástico). El nombre cambia solo según el corte: **Hoodie**, **Hoodie abierto**, **Buzo** (sin capucha), **Campera** (sin capucha y abierta), **Chaleco** y **Chaleco abierto** (sin mangas en los dos lados). **Bolsillos y botones** se ponen con **apliques** en la Estilista, no son moldes. Los moldes de cuello y de manga se guardan también en el banco de la categoría Chaqueta. **Traje** (2026-10-07, "traje separado"): la chaqueta suma un 4.º pin, **Solapa**, con moldes de **Chaqueta**: **sin solapas**, **en pico**, **redondas** y **chal** (esmoquin). Son dos tiras de tela que bajan del cuello a cada lado del frente, armadas con cajitas de 1 px; con busto cada una sube hasta apoyarse sobre la cúpula. Con solapas y sin capucha el nombre pasa a **Saco** (o **Saco cerrado**). Además hay un **cuello de camisa** (molde de cuello **Camisa**, para la remera): escote en V chico y dos puntas que asoman por encima de un saco. El traje se arma separado: saco (chaqueta abierta, sin capucha, remate recto, solapas) + pantalón de vestir + remera de cuello camisa. *(Pendiente: corbata como zona de la Banda, y abrigos largos, que necesitan una falda de malla como la de la pollera.)*

**Casilleros desplegables y ruedos** (2026-10-08). Cada pin principal que tiene un par lleva un botón **▾** al lado: **manga ↔ puño**, **cuello ↔ solapa**, y en el pantalón, las medias y los calientabrazos **corte ↔ ruedo** de cada botamanga y de cada borde. Al abrirlo aparece el segundo casillero con su marco, y **los dos aceptan cualquiera de los dos moldes** (un molde de rango en el del puño corta la manga; uno de ruedo en el de la manga arma el puño). Se abre solo si ya tiene un molde o un corte puesto y no se cierra con algo adentro. Hay **una sola Trama** por prenda (antes eran tres de Personalización). El **molde de ruedo** (recto, ajustado, campana; ondulado, festoneado y pico solo en la pollera) funciona en botamangas y bordes de medias y calientabrazos con la geometría (ajustado aprieta la última fila, campana abre las últimas tres) y en la **pollera** (ajustado cierra el ruedo contra las piernas, campana lo abre). **Frente de la chaqueta:** además de cerrado y abierto, hay **cruzado** (una solapa monta sobre la otra, con costura corrida) y **abierto hasta el pecho** (la apertura baja 6 filas y de ahí cierra); el ancho de la apertura crece con el calce (2 px hasta Normal, 3 Suelto, 4 Oversize), y en las prendas holgadas el busto también se abre por el medio (no queda tela cruzando la apertura). Los ruedos **ajustados** de botamangas, medias y calientabrazos llevan además **rib** (canales) pintado en esa fila.

**Sombrero de bruja** (2026-10-04, "modelemos y agreguemos un sombrero de bruja"). Va en su **propio slot de Trinkets** (**Sombrero**, en el grupo de la cabeza, aparte del casco: se puede llevar con casco, con la capucha del hoodie y con apliques). Está hecho de cajas que se afinan hacia arriba: un **ala** plana (ancha o corta), un **cono** de cuatro tramos y una **cinta** alrededor de la base, más una **punta** de dos tramos, **recta o doblada** hacia atrás, que se **mece con el movimiento** (la misma tela blanda de los apliques: al correr o saltar se inclina). Tiene **3 zonas de color** (ala, cono y cinta) que se pintan en la **Mesa de estilado** (2026-10-05, "que se le apliquen los colores en la mesa de estilado sobre todo si tiene tres areas"): poné el sombrero en la prenda y un **retazo** (teñido en la Estación de Tintes) o una **muestra de color** en el slot del retazo, y apretá **Colorear sombrero**. Aparecen tres filas (**Ala**, **Cono**, **Cinta**) con un botón por cada color del retazo (■ en su color): el que apretás le pasa ese color a esa zona; **Retazo entero** pasa los 3 colores del retazo a las 3 zonas de una. **No se gasta el retazo** (el color se copia). Con una muestra de color, los 3 botones dan el mismo color. **También podés clickear el sombrero en la vista 3D**: la zona que tocás (ala, cono o cinta; al pasar el mouse aparece su nombre) se pinta con el **color activo**, que es el último botón ■ que apretaste (se marca con corchetes) y se ve en el texto de ayuda. **Volver a apliques** devuelve los controles de siempre. Sale **blanco de fábrica**; las zonas que no pintás se quedan como están. Como cualquier ítem puesto, **admite apliques** de la Mesa de estilado, que por ahora se apoyan en la caja de la cabeza (no en el cono). **Moldes en la Modeladora** (2026-10-05, segunda tanda): la categoría **Sombrero** (botón de categoría) tiene un esquema con dos pines, **Ala** (molde de sombrero: ala ancha o corta) y **Punta** (recta o doblada); poné el sombrero en el slot de prenda, soltá los moldes en los pines y fijá con la chincheta como en el resto. Los 4 moldes están en la pestaña creativa y la Modeladora creativa ya los trae. **Dibujo por zona** (2026-10-05): en modo Colorear, cada fila (Ala, Cono, Cinta) tiene a la derecha un botón que cicla el dibujo de esa zona — Liso, Rayas, Rayas verticales, Lunares, Cuadros —, pintado con el color de la zona y un tono de contraste (más claro si es oscuro, más oscuro si es claro). **Apliques sobre el ala y el cono**: el click en la vista 3D apunta a la superficie del sombrero (ala, cono, cinta o punta) y el aplique queda apoyado ahí, ya no sobre la caja de la cabeza. **Maniquí y Guardarropas**: un slot más (debajo de la columna de armadura) para el sombrero; se ve en la figura, en la vista previa y "Equipar"/"Intercambiar" lo pasan al slot de sombrero de Trinkets.

**Bandas: cintos y chokers** (2026-10-05, "correas y cintos"). Un ítem, la **Banda**: un aro que rodea la **cintura** (cinto, slot **Cinto** de Trinkets, encima de la ropa y holgado según lo que lleves puesto) o el **cuello** (choker, slot **Choker**, en la base de la cabeza). Cada zona solo entra en su slot. Tiene un **ancho** (fino, medio, ancho) y un **herraje** al frente (ninguno, placa o aro), y 3 partes con color y dibujo: **Banda**, **Borde** (dos tiras finas arriba y abajo) y **Herraje**. Sale de fábrica de cuero oscuro con herraje dorado. En la **Mesa de estilado** se colorea y se le cambia el dibujo igual que al sombrero (modo "Colorear y dibujar", click en la vista 3D sobre la banda, el borde o el herraje), y se le pueden poner **colgantes**: cualquier aplique o aplique de objeto apoyado sobre cualquier caja de la banda, con su tela blanda. En la pestaña creativa hay un cinto y un choker de fábrica. **Moldes en la Modeladora**: la categoría **Banda** tiene un esquema con tres pines — **Zona** (molde de cintura o de cuello), **Ancho** (fino, medio, ancho) y **Herraje** (sin herraje, placa o aro); poné la banda en el slot de prenda, soltá los moldes en los pines y fijá con la chincheta. Los 8 moldes están en la pestaña creativa y la Modeladora creativa ya los trae. Por ahora no se guardan en el Maniquí ni en el Guardarropas. **Correas libres** (2026-10-05, "correas libres y pensemos un molde de cadenas"): en la Mesa de estilado, con **cualquier cosa** en el slot de prenda (prendas, armaduras, wearables, el sombrero, la banda, la pollera y la capa), poné un **molde de correa** en el slot del molde y apretá **Correas**. Con **dos clicks** en la vista 3D definís la correa: el primero es el inicio y el segundo el fin (una guía amarilla muestra el camino; los dos puntos tienen que estar en la misma parte del cuerpo). El botón **Modo** elige: **Pegada** (da la vuelta por la superficie de la caja entre los dos puntos, también de una cara a otra o a la opuesta, como un arnés) o **Colgante** (solo el primer punto la sujeta y cuelga hacia el segundo con tela blanda, y se mueve con la inercia). **Ancho** va de 1 a 4 px. Abajo de los botones hay una **lista numerada** de las correas de la prenda (hasta 8): elegí una y **Modo** y **Ancho** pasan a cambiar *esa* correa, **Recolorear** le pasa los colores del retazo (sin gastarlo) y **Quitar** saca *esa*; sin ninguna elegida, Quitar saca la última (o cancela el inicio si ya hiciste el primer click). En el **sombrero** y la **banda** la correa se apoya sobre sus cajas (ala, cono, banda, herraje...) y en la **pollera** y la **capa** sigue la tela entre los dos puntos (los dos clicks tienen que estar en la misma prenda); ahí la colgante cuelga del primer punto hacia el segundo. Los **5 moldes** (pestaña creativa) son los estilos: **lisa** (cuero o tela con borde y puntadas), **cadena** (eslabones alternados, color de herraje), **cadena fina** (bolitas de metal), **ojalillos** (tira con agujeros) y **cordón** (con puntas de herraje, para el de la capucha de un hoodie). Los colores salen del retazo del slot del retazo (banda, borde, herraje); sin retazo, cuero oscuro con herraje dorado. Por ahora no se gasta nada (el material se define más adelante). Las colgantes se empujan fuera de la caja de su propia parte en cada cuadro, pero todavía no chocan con las otras partes del cuerpo (un brazo contra el torso).

**Prendas viejas (armadura).** Hay prendas de una versión anterior que se equipan en los slots de armadura: Medias 3/4, Medias de Red y Traje de Maid. No pasan por las máquinas. El Buzo Oversize viejo se reemplazó por el Hoodie: su receta ahora da el nuevo y ya no aparece en la pestaña.

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
  - **Rubor:** rodillas, codos, pecho, panza y cara (los cuerpos humanos lo traen marcado en el dibujo). En automático es **tu mismo color, más intenso y un poco más oscuro**: en una piel da un rosado tibio y en una skin azul, verde o gris, un tono más profundo del mismo color, sin manchas violetas. Se puede elegir a mano, por ejemplo un rosa. Con la zona Rubor elegida aparece el slider **Fuerza del rubor** (0 a 100%, de a 5; arranca en 50%): en 0 no hay rubor.

  Click en una zona para editarla. La Base arranca **calculada de tu skin** (**Tono de mi skin** la vuelve a eso). Clara, Oscura y Rubor arrancan en **automático**: salen del color Base, más claras u oscuras solas (marcadas con una "A"). El botón **Automática** las devuelve a ese estado.
- **Ropa interior** (debajo de la vista previa), en dos partes que se eligen por separado: **↑ arriba** (Nada arriba, Bralette, Deportivo, Binder) y **↓ abajo** (Slip, Culotte, Boxer). Click pasa a la siguiente. La muestra de color de al lado elige su **color** en los sliders y la paleta (con la "A" es el blanco roto de siempre). La tela toma el relieve del cuerpo. El binder tapa y da la forma de la faja, pero no achata: para pecho plano, elegí un cuerpo plano. La ropa interior es el mínimo que siempre está; lo personalizable (encaje, patrones, fotos) se hace con remeras y pantalones recortados en las máquinas.
- **Skin siempre** (debajo de la grilla, a la izquierda): con **Sí**, el cuerpo elegido, con sus colores y su ropa interior, se ve siempre, aunque no tengas ropa del mod puesta (la segunda capa de tu skin se oculta, salvo el sombrero y la capa). Con **No**, aparece solo debajo de la ropa del mod.
- **Busto** (debajo de la grilla, a la derecha): **Redondo** (dos cúpulas) o **Cuadrado** (una caja por pecho que sale del pecho y se inclina hacia abajo, al estilo cúbico de Minecraft). Mismo talle, rebote, ropa, armaduras y apliques en los dos estilos.
- Con un cuerpo elegido, la cadera (de la cintura para abajo) también es del cuerpo; con "Mi propia skin" ahí se sigue viendo tu skin real.
- **De tu skin:** una paleta con hasta **5 colores sacados de tu skin** (los más usados, sin repetir parecidos). Click en uno lo pone en la zona elegida; por ejemplo, con una skin de osito, el marrón del pelaje en Base y el beige de la pancita en Clara. Los sliders Rojo/Verde/Azul ajustan la zona a mano.
- **Confirmar** lo guarda y la pantalla ya no vuelve a aparecer sola. **Ahora no** la cierra sin guardar y vuelve a aparecer la próxima vez que entres al mundo.

Los cuerpos dibujados están en alta resolución (6 veces la de una skin común). Cada uno está pintado para brazos anchos (classic) o finos (slim). Si tu skin tiene los otros, la textura se adapta sola; tus brazos no cambian de ancho.

### Relieve (volumen del cuerpo y de la ropa)

El cuerpo y la ropa ya no son cajas planas: **los costados de torso, brazos y piernas tienen relieve** (la cabeza y las tapas, no).

- **Forma del cuerpo:** cada cuerpo trae la suya: pectorales, abdominales y brazos marcados en Atlético y Musculoso; panza redonda en Gordito, Osito y Panda; nalgas (abajo de la espalda) y cintura en los femeninos (Fem con curvas, la más marcada); muslos con cuádriceps, isquios y rodilla marcados; pecho de pelaje en Lobo y Zorro; felinos atléticos. **Definición** (0 a 200 %, de fábrica 100 %) marca más o menos los músculos: `/modamod definicion <n>`.
- **Busto** (como Only Jugs y el Female Gender Mod): dos cúpulas en el pecho, una por lado, que nacen arriba del pecho y bajan en pendiente hasta la punta, redondas abajo, que caen un poco según el talle y se abren hacia los costados sin despegarse del pecho, con la textura de esa zona (la piel, o la prenda de encima con su patrón y sus fotos). Los cuerpos femeninos traen uno de fábrica; con **Estrógenos** activos ese se apaga y manda solo el de los Estrógenos (no se suman): se toman como una poción y se **acumulan en 7 tomas**, cada una un talle más (los dos primeros chiquitos, el último bastante más grande). El efecto dura **24 horas reales desde la última toma**, aunque el mundo esté cerrado; tomar con el talle máximo solo renueva el plazo. Al vencer, el busto vuelve al del cuerpo. `/modamod busto <0..7>` lo fija a mano (también por 24 h). El **binder** lo aplana y el top **deportivo** lo sujeta un poco. Cada tela que tapa el pecho lo envuelve apenas por fuera de la punta (por capas, como el resto de la ropa): con calce Normal todavía marca los dos pechos; las prendas **Sueltas y Oversize** (el hoodie) ya no: la tela queda tirante de una punta a la otra, sin meterse entre los pechos, y cae derecho desde las puntas hasta el ruedo, como un estante (Oversize, más chato). Un busto chico queda tapado. La ropa interior pintada en la piel (corpiño, breteles) se ve derecha sobre el busto y empalma con la del pecho; el corpiño cubre el busto entero hasta el pliegue de abajo, aunque el busto baje más que el corpiño plano (lo mismo un croptop). Debajo de una prenda Suelta u Oversize, el busto del cuerpo y de las prendas de adentro se apaga y lo dibuja solo el manto de la prenda de afuera, así nunca la atraviesa. Sin ropa del mod en el torso, va con la piel y la segunda capa de tu skin (la segunda capa, en su propia cúpula por fuera: la remera pintada de la skin ya no queda plana en la base).
- **Busto y cola cuadrados** (2026-10-04, como el Female Gender Mod, que se estudió abriendo su código): cada pecho es una **cuña** que cuelga de su arista de arriba, pegada al pecho: la cara de adelante **baja en pendiente** hasta la punta y la de abajo **vuelve y se entierra** en el pecho, con costados triangulares. Se inclina más cuanto más talle (hasta ~42°; las armaduras, menos) y hay una **rendija** entre los dos pechos para que se note la división (con una tela holgada, un solo manto). Las nalgas usan las mismas cuñas, con una rendija más fina.
- **Ropa interior de abajo:** además de Slip, Culotte y Boxer hay **Nada abajo**, para ponerte la tuya. Por comando: `/modamod interior abajo ninguna`.
- **Rebote:** el busto y la cola rebotan una o dos veces y se asientan en menos de un segundo (antes quedaban oscilando varios).
- **Cola:** con busto, las nalgas pasan a ser volumen propio, como el busto (redondas o, con busto cuadrado, cajas), y **crecen con el talle** (de ~0,9 px con el talle chico a 2 px con el más grande; los cuerpos con más curvas, más). La **ropa interior de abajo** (slip, culotte, boxer) las cubre enteras, con el elástico donde empieza la cola. Tienen su **propio resorte**: rebotan al saltar y aterrizar, y con cada paso sube una y baja la otra. La ropa que pasa por ahí (remeras largas, hoodie, pantalón) las envuelve con su tela, lo que la prenda no cubre deja ver lo de abajo, y la **pollera** pasa por fuera y cae derecho desde lo más saliente. Las pecheras y las piernas de armadura también las envuelven (rígidas). Los apliques de la espalda se apoyan en ellas. Sin busto, las nalgas siguen siendo el relieve de siempre.
- **Armaduras y ropa de otros mods:** cualquier pechera o prenda que se dibuje con un modelo humanoide (vanilla, armaduras de otros mods, Cosmetic Armor, cosméticos) también lleva el busto, con su propia textura, tinte, ajustes y brillo. Las **armaduras quedan rígidas** (sin rebote, casi sin caída, como un peto moldeado); la ropa de otros mods rebota como la del mod. Las armaduras con modelo 3D propio (GeckoLib) no se adaptan.
- **Apliques sobre el busto:** un aplique puesto en el frente del torso se apoya en la cúpula y rebota con ella. En la Mesa de estilado el click le pega al busto, pero el aplique se guarda en el frente plano: la prenda sigue sirviendo sin busto o con otro talle.
- **Física de resorte:** el busto rebota al saltar, caer y aterrizar, con cada paso al caminar o correr, y se balancea de costado al girar (y en el twirl). Cuanto más grande, más lento y más amplio. La ropa de encima rebota con él. Cada cliente lo simula para los jugadores que ve.
- **La ropa sigue al cuerpo según el calce:** **Pegado** lo copia como una segunda piel; **Ajustado** casi igual; **Normal** marca lo que sobresale y "puentea" los huecos (entre los pectorales, el escote); **Suelto** y **Oversize** apenas. Una prenda nunca queda por dentro de la de abajo.
- **Arrugas automáticas** en codos, rodillas, cintura, axilas, muñecas y tobillos, más marcadas cuanto más holgado el calce (Pegado no arruga).
- **Detalles en relieve por prenda:** el **hoodie** tiene el bolsillo canguro con su costura, rib en el ruedo y los puños y la costura de la sisa; el **pantalón**, costuras laterales, bolsillos de atrás, pretina y bragueta; las **medias**, tejido acanalado.
- **Textura de tela** con un **Molde de textura** en la Mesa de estilado: **Fruncido** (pliegues finos verticales) o **Acolchado** (almohadones en rombo).
- **Volumen: redondeado o voxel** (2026-10-04): en la pantalla de elegir cuerpo, el botón **Volumen** alterna **Redondeado** (superficie suave, el de fábrica), **Voxel** (bloquecitos finos de medio píxel, como 3D Skin Layers) y **Voxel grueso** (bloques de 1 px). Cambia el volumen del cuerpo y de la tela de la ropa juntos; el busto sigue con su propio botón (redondo o cuadrado). Se guarda en el perfil y lo ven los demás jugadores. Por comando: `/modamod volumen redondeado|voxel|grueso`.
- **Forzar un estilo para comparar** (solo para vos): `/modamoddebug relieve suave` (superficie continua), `escalonado`, `escalonado_grueso` y `apagado` (las cajas de antes); `/modamoddebug relieve perfil` suelta el forzado y vuelve a usar el volumen que eligió cada jugador.

El relieve se ve donde el mod dibuja el cuerpo: debajo de la ropa del mod, o en todo el cuerpo con **Skin siempre**. En el Maniquí la ropa tiene arrugas y detalles, pero la figura no tiene forma.

### Comandos

Sin permisos especiales, cada quien cambia el propio:

| Comando | Qué hace |
|---|---|
| `/modamod elegir` | Vuelve a abrir la pantalla Elegí tu cuerpo. |
| `/modamod cuerpo <tipo>` | Cambia el cuerpo directo: `skin_real`, `estandar`, `delgado`, `atletico`, `musculoso`, `gordito`, `velludo`, `fem_estandar`, `fem_atletica`, `fem_curvas`, `fem_gordita`, `cebra`, `dalmata`, `leopardo`, `lobo`, `osito`, `panda`, `tigre`, `vaca`, `zorro`. Los cuerpos viejos (plano, curvy, binder) pasaron a Estándar y Fem con curvas. |
| `/modamod interior arriba <tipo>` | Parte de arriba: `ninguna`, `bralette`, `deportivo`, `binder`. |
| `/modamod interior abajo <tipo>` | Parte de abajo: `slip`, `culotte`, `boxer`. |
| `/modamod interior color <rgb>` | Color de la ropa interior, como número (ej. `16777215` = blanco). |
| `/modamod tono skin` | Tono de piel tomado de tu propia skin (el valor por defecto). |
| `/modamod tono <número>` | Tono de piel a mano, como color RGB en decimal (ej. 14329120). |
| `/modamod busto <0..7>` | Dosis de Estrógenos (talle de busto), por 24 horas reales. |
| `/modamod definicion <0..200>` | Cuánto se marcan los músculos del relieve, en %. |
| `/modamod volumen redondeado\|voxel\|grueso` | Volumen del cuerpo y la tela: suave, bloquecitos finos o bloques de 1 px. |
| `/modamod ver` | Muestra tu configuración actual. |
| `/modamod reset` | Vuelve todo al valor por defecto. |

## 5. Cómo funcionan las máquinas (común a todas)

Las tres máquinas de confección (Modelado, Tintes, Sublimadora) comparten la misma lógica:

- **Configuración y fijadas.** En la interfaz armás el ajuste y lo **fijás** (con la chincheta o un botón). Lo fijado es lo que se aplica; lo que no está fijado es solo un borrador que se ve en la vista previa.
- **Vista previa 3D.** La columna izquierda muestra a tu personaje con la prenda como va a quedar. Se gira arrastrando con el mouse o con el botón Vista (frente, costado, espalda), y se hace zoom con la ruedita.
- **Diseños guardados.** Escribís un nombre y apretás **Guardar diseño**: se guarda todo lo fijado de esa prenda (hasta 8 diseños por prenda, en las tres máquinas). Click en un casillero numerado lo carga (el nombre aparece al pasar el mouse); click derecho lo borra.
- **Entrada y salida.** Cada máquina tiene un slot grande de **Entrada** (la prenda a procesar) y uno de **Salida** (la prenda terminada). Mientras trabaja, la máquina se anima y hace ruido; al terminar suena una campanita. La pantallita del frente está **siempre iluminada** y da una luz tenue (nivel 4); trabajando o con la prenda lista (LED rojo o verde), la máquina ilumina más fuerte (nivel 10). La barrita de progreso del frente, sobre fondo negro, se llena de izquierda a derecha y queda llena mientras la prenda terminada espera en la salida.
- **Colores de cada máquina.** Las tres interfaces son de pergamino y madera, pero cada una tiene su metal: la Estación de Tintes es **verdosa** (verdín), la Modeladora **cobriza** y la Sublimadora **dorada**. El color se ve en los filos, las esquinas, los slots y los botones.
- **La prenda en el bloque.** La prenda cargada se ve sobre la máquina con su ícono real (el mismo del inventario, con colores, patrones y estampas): acostada sobre la mesa en la Modeladora y la Sublimadora, y enrollada alrededor del rodillo de Tintes (gira con él mientras tiñe y queda ahí hasta que la retirás). La pantallita del frente muestra también ese ícono.
- **Tiempos.** Modelado 15 s, Tintes 10 s, Sublimadora 15 s.
- **¡Cuidado!** Meter la mano (click derecho) en una máquina mientras trabaja lastima: la Modeladora corta, la de Tintes marea con los vapores y la Sublimadora quema.
- **Al romperlas se guardan enteras** (como una shulker box, a pedido del 2026-09-30): el ítem que cae lleva TODO lo de adentro — prendas, moldes, pines, cuadraditos, diseños guardados, fotos, papel, tinta y hasta el trabajo a medias — y al volver a colocarla queda igual. Vale para las 4 (Modeladora, Tintes, Sublimadora y Guardarropas), rotas a mano o por una explosión. En creativo, como la shulker, cae solo si tiene algo adentro.

### Automatización con tolvas

Las máquinas se pueden encadenar con tolvas (hoppers):

| Cara del bloque | Qué entra o sale |
|---|---|
| **Arriba** | La prenda a procesar (entra sola y arranca sola). |
| **Izquierda** | También entra la prenda (es la cara donde empalma la máquina de al lado). |
| **Atrás** | Los insumos: tintes (C, M, Y, K) y papel. |
| **Derecha** | La prenda terminada sale sola hacia un cofre o tolva de ese lado. |

Con tolva, la prenda **arranca sola** si hay un diseño fijado. Desde la interfaz, las tres tienen un botón sobre la flecha Entrada → Salida: **Modelar**, **Teñir** y **Prensar**. Además, la Modeladora arranca al cerrar la interfaz (solo si hay prenda en la Entrada, algo fijado y la Salida libre; si no, queda apagada) y la Sublimadora al cerrar la tapa. 

**Línea de producción** (2026-10-04): si ponés dos máquinas una al lado de la otra (la salida de la primera mira a la entrada izquierda de la segunda, mismo sentido), la prenda terminada pasa directo a la siguiente sin tolvas, y la última la deja en un cofre/tolva a su derecha. Si una máquina de la cadena no tiene nada fijado para esa prenda (sin corte, sin cuadraditos de tinte o sin fotos/máscaras), la **saltea** y la prenda sigue de largo. Una prenda que cargás a mano o con tolva por arriba se comporta como siempre (espera en la entrada). La Sublimadora con fotos fijadas deja la prenda esperando a que cierres la tapa. **Patrones y tramas de color** (rayas, corazones, estrellas, lunares, vichy...) se usan solo en la Estación de Tintes; la Modeladora ya no los acepta (2026-10-04). Al almacén general de la Modeladora van los moldes de corte, rango, calce, red y manga (25 de 27 casilleros).

**La GUI sigue a la prenda** (2026-10-04): cuando entra una prenda a una máquina (a mano, por tolva, por cinta o cadena), la pantalla pasa sola a la categoría de esa prenda. **Sublimadora:** al abrir su pantalla la tapa se abre; al salir se cierra y, si hay algo fijado, arranca el prensado (como las otras máquinas; con una prenda lista esperando, la tapa queda abierta para retirarla). Si una prenda llega por tolva o cadena con un diseño fijado, la Sublimadora arranca sola. Las **capas con máscara** que fijás ahora son el diseño de la máquina: se quedan entre prenda y prenda y se suman a las capas que la prenda ya traía (hasta 12), así no hay que volver a fijar.

**Cinta transportadora** (2026-10-04, pestaña creativa, receta pendiente): un bloque que lleva una prenda por bloque hacia adelante (el lado en que mirabas al ponerla) y la entrega al inventario de enfrente: otra cinta, una máquina por su cara izquierda, un cofre o una tolva. Recibe por atrás, por los costados y por arriba (tolvas). Se encadenan las que quieras, así las máquinas pueden quedar separadas. **Curvas:** se arman solas como un riel: si por atrás no le llega nada y desde un costado sí (otra cinta que mira hacia ella, o una máquina que empuja hacia ella), se curva hacia ese lado. **Rampas** (2026-10-04, "que el conveyor belt conecte con un conveyor mas abajo o mas arriba"): si adelante, a la misma altura, no hay nada que reciba pero sí una cinta o máquina un nivel más ARRIBA, la cinta se vuelve **rampa de subida**; si está un nivel más ABAJO, **rampa de bajada**. La rampa ocupa su bloque como una escalera (45°). La de subida va a la altura de quien la alimenta y entrega un nivel más arriba. La de bajada se coloca en el nivel de abajo: la cinta de arriba le entrega por atrás y la rampa baja hasta la banda del bloque de adelante, sin meterse en el bloque de abajo, y si es una máquina, por su cara izquierda. La rampa no se curva a la vez (para subir y girar: rampa y después curva). La prenda se ve viajando acostada sobre la banda animada (16 ticks por bloque recto, 13 por curva). Si adelante no hay lugar, espera al final. Cada máquina muestra la prenda terminada sobre su bandeja de salida (la que se desliza hacia afuera) hasta que sale. Si rompés una cinta con una prenda encima, la prenda cae. **Cualquier ítem, click y redstone** (2026-10-05, "que no solo transporte ropa", "que se pueda sacar las cosas cliqueando en la cinta y poner cosas asi", "dejan de circular items cuando reciben señal de redstone"): la cinta lleva cualquier ítem (de a uno por bloque; los que no son prendas se ven apoyados en la banda). **Click derecho con la mano vacía** saca lo que lleva; **con un ítem en la mano** lo pone si la cinta está libre (con un bloque en la mano hay que agacharse, si no se coloca el bloque). La forma (curva, rampa) se **recalcula sola** al cambiar los bloques de alrededor y cada segundo, ya no hace falta el click. **Con señal de redstone** la cinta se ve parada (banda quieta) y el ítem se congela donde está; las **tres máquinas** también se detienen del todo (no trabajan ni reciben ni empujan) hasta que se corta la señal. **Empalme de cintas**: bloque que junta hasta **tres entradas** (atrás y los dos costados, cintas o lo que empuje) y una **tolva por arriba**, y las saca por **una salida** (el frente). Es **rotativo**: alterna entre las entradas que tienen algo esperando. También se saca/pone con click derecho y se frena con redstone. Arreglo: las rampas tenían planos transparentes por encima (las cajas salían de 0..16 y las UV automáticas quedaban fuera de la textura): ahora llevan UV explícitas. **Deslizamiento** (2026-10-07, "que sea un deslizamiento, se ve como si el personaje caminara"): quien se sube a la cinta y no toca las teclas se **desliza** con las piernas quietas y sin bamboleo de cámara, como sobre hielo; si camina o corre sobre la banda, se anima por su propio paso. **Cinta parada sobre hielo:** con la cinta parada por redstone caminás normal aunque tenga hielo abajo (no te deslizás); en marcha, el hielo de abajo sigue acelerándola. *(La cadena completa todavía no se probó a fondo; la Mesa de estilado queda afuera hasta que se rehaga.)*

**Pantalla de Ropa y armadura cosmética** (2026-10-05, "limpiar la gui de tanto ruido... un boton de ropa que te lleve a una gui especifica con los slots de ropa... meter aqui la funcionalidad del cosmetic armor"): los slots de ropa del mod (sombrero, collar/choker, capa, hoodie, prenda del torso, cinto, calentadores, pantalón/pollera y medias) **ya no aparecen en el inventario de siempre**; los de otros mods de Trinkets se quedan donde estaban. Se abren desde el botón **Ropa** del inventario (junto al libro de recetas) o con la tecla **K** (configurable en Controles: "Abrir ropa"; también sirve en creativo y vuelve a cerrar la pantalla). La pantalla muestra al personaje, los slots de ropa del mod, la **armadura real** (la que da las estadísticas), la **armadura cosmética** (solo se ve; casco, pechera, pantalón y botas; la pechera acepta también élitros y el casco calabazas y cabezas) y el inventario. Por cada pieza hay dos interruptores: **Cosm.** (se ve el ítem cosmético en vez de la armadura real) y **Ocultar** (no se dibuja nada). Si "Cosm." está prendido y el slot cosmético está vacío, la pieza se ve oculta. Con los dos apagados se ve la armadura real, como siempre. Los demás jugadores ven lo mismo que vos, y se guarda con el personaje (también al morir). Las estadísticas siempre salen de la armadura real.

**Estilista automática, etapa 1** (2026-10-05, "rearmemos la estiladora como un bloque de la cadena, copia la base de la modeladora ponele la tapa de la autostyler arriba y vamos a migrar toda la funcionalidad de la mesa estilizadora"; pestaña creativa, receta pendiente): la máquina nueva que va a reemplazar a la Mesa de estilado. Es un bloque de la línea textil igual que la Modeladora: la prenda entra por **arriba** o por la **izquierda** (cinta, tolva o la salida de la máquina de al lado), sale por la **derecha**, se frena con redstone, tiene luz y LED, y se rompe con todo adentro. El modelo es el cuerpo de la Modeladora (pantalla, LED, barra de progreso, ventilador, bandeja de salida) con la tapa del auto_styler (el pórtico que va y viene) arriba. **Hoy todavía no aplica ningún diseño**: una prenda que llega por la cadena pasa de largo, y a mano el botón **Probar** de su pantalla provisoria corre los 13 segundos de animación del pórtico con la prenda cargada. Lo que falta (etapas 2 y 3): migrar apliques, correas, colores y dibujos de sombrero y bandas, y textura de la Mesa; diseño guardado en la máquina y por tipo de prenda; almacén propio con **hilo** (apliques) y **cuero** (correas) como insumos; y la pantalla definitiva en estilo pergamino con la vista grande. La Mesa de estilado sigue funcionando hasta que la nueva ande.

**Nombres y tooltips de los ítems** (2026-10-05): cada molde, trama, máscara, plantilla y motivo muestra, bajo el nombre, su **tipo** (Molde de corte de cuello, Molde de calce, Trama, Máscara de sublimado, Plantilla de aplique / de correa, Motivo, Patrón...), las **prendas** a las que aplica (con más de tres se abrevia y **Shift** muestra la lista completa) y **dónde se usa** (Mesa de Modelado, Estación de Tintes, Sublimadora o Mesa de estilado, cada una con el color de su tema). Cambios de nombre: las redes son **Trama: …**, los moldes de máscara son **Máscara de sublimado: …**, los de aplique y correa son **Plantilla de aplique / de correa: …**, y los patrones con dibujo (corazones, estrellas, lunares, vichy) son **Motivo: …** (las rayas siguen siendo Patrón). El **Molde de mangas** cíclico se jubiló (ya no está en la pestaña creativa ni tiene receta; los mundos viejos lo conservan). El **Empalme** de cintas ahora tiene los postes de las esquinas más bajos para que las cintas empalmen al ras.


### Máquinas creativas y kits

En la pestaña creativa hay una **versión creativa** de cada máquina de confección: **Mesa de Modelado creativa**, **Estación de Tintes creativa**, **Sublimadora creativa** y **Mesa de estilado creativa**. Son iguales a las normales, pero:

- **Se distinguen** por el color: madera violeta nacarada y herrajes dorados.
- **No esperan:** el proceso termina al instante.
- **No piden insumos:** Tintes no gasta tinta, Envasar no pide frasco, la Sublimadora no gasta tinta ni papel y la Mesa de estilado no pide retazo (sin retazo, el aplique sale blanco).
- **Vienen cargadas una vez** al colocarlas: la Modeladora con uno de cada molde (los compartidos en el almacén general y los de cada prenda en su banco) y Tintes con uno de cada patrón en el almacén. La Mesa de estilado tiene un solo slot de molde, así que trae el botón **Molde ▸** para pasar por todos los moldes de aplique y de textura. Lo que saques o se gaste no vuelve. Si la rompés y la volvés a colocar, vuelve con lo que tenía, no se recarga.

También hay **kits** (shulker boxes con nombre): **Kit: todos los patrones**, **Kit: todos los moldes** (en las cajas que hagan falta) y **Kit: insumos** (64 de cada tinte, papel y frascos).

## 6. Mesa de Modelado

![Mesa de Modelado, pantalón](img/captura_modelado_pantalon.png)

La Modeladora cambia el **corte** de la prenda. En el centro está el **dibujo de la prenda** con cuadraditos (pines) en cada parte: cuello, mangas, largo, tiro, botamangas, etc. Cada pin acepta los moldes que tienen sentido ahí.

**Cómo se usa:**

1. Elegí la prenda con el botón **Categoría** (remera, pantalón, medias, calientabrazos, pollera, capa).
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
| Pantalón | Tiro, Corte Bota Izq., Corte Bota Der., Calce, 1 de Trama |
| Medias | Corte Superior e Inferior de cada pierna (cada uno con su **ruedo** en un casillero desplegable), Calce, 1 de Trama por lado |
| Calientabrazos | Corte Superior e Inferior de cada brazo (cada uno con su **ruedo** desplegable), Calce, 1 de Trama por lado |
| Pollera | Forma (molde de pollera), Largo (molde de rango), Calce, 1 de Trama |
| Capa | Largo (molde de rango), Ruedo, Capucha y Cuello (moldes de capa), 1 de Trama |

### Los moldes

| Molde | Dónde va | Qué hace |
|---|---|---|
| **Molde de rango** (Cero, Mínimo, Corto, Medio, Medio largo, Largo, Máximo) | Mangas, piernas, cortes sup./inf., largo de remera, tiro de pantalón, largo de pollera y de capa | Son **7 niveles de una línea medida desde arriba**: el nivel *k* está a *2k* filas desde el hombro o la cintura (0, 2, 4, 6, 8, 10, 12). Sirve para todas las prendas: cada una lo traduce a su propia medida (nivel 3 = una manga media, una pantorrilla, una pollera a la rodilla). El Cero es la línea más alta: manga sin nada, pierna de 0, top, tiro hasta los hombros, pollera ultramicro (3 px) y capelet (6 px). |
| **Molde de cuello** (Redondo, V, Polera) | Cuello de remera | Forma del cuello. |
| **Molde de manga** | Activo (remera, calientabrazos) | Molde viejo de manga: cada uso pasa al largo siguiente. |
| **Molde de calce** (Pegado, Ajustado, Normal, Suelto, Oversize) | Calce | Qué tan despegada del cuerpo va la prenda. |
| **Molde de pollera** (Campana, Tableada, Tubo, Globo, Circular) | Forma de la pollera | **Campana** y **Tableada** (pliegues) como siempre. **Tubo** (2026-10-05): recta o lápiz, pegada a la cadera, casi sin vuelo ni movimiento. **Globo**: abombada, ancha en el medio y cerrada en el ruedo. **Circular**: de círculo completo, se abre desde la cadera y es la que más vuela (y más se abre en el twirl). |
| **Molde de volado** (Recto, Circular) | Volados de la pollera | **Recto**: tira fruncida con muchas ondas chicas y poca apertura. **Circular**: ruedo abierto con pocas ondas grandes. El mismo molde sirve en dos pines de la categoría Pollera de la Modeladora: **Corte inferior** (una fila de volado en el borde de abajo, el último tercio) y **Toda la pollera** (tres filas parejas de arriba a abajo; cada una nace pegada y se abre hacia su borde, y la de abajo se apoya sobre la de arriba). Si ponés los dos, el del ruedo manda en la última fila. Las rayas, lunares y fotos de la tela siguen la pollera lisa (sobre el volado se estiran un poco). |
| **Molde de borde** (Ondulado, Festoneado, Con pico) | Borde decorativo del ruedo | Va en el pin **Custom** de la Pollera (2026-10-05, "era un custom para el ruedo"): cambia el largo de la tela a lo largo del borde de abajo — **ondulado** (onda suave de ±0,9 px), **festoneado** (arcos de medio círculo hacia abajo, hasta 1,6 px) o **con pico** (triángulos, hasta 2,2 px) — 8 repeticiones por vuelta. Se combina con los volados y con cualquier forma. |
| **Moldes de capa** (Ruedo recto, Ruedo redondeado, Ruedo con cola, Con capucha, Sin capucha, Cuello alto, Sin cuello) | Ruedo, Capucha y Cuello de la capa | Cada uno en su pin. Los "Sin" le sacan la capucha o el cuello a una capa que ya los tiene. |
| **Molde de red** | Materiales / Personalización | La trama de la tela (ver abajo). |
| **Molde de corte** | Activo | Un combo de cortes guardado en un solo ítem. |

**Los cinco calces:**

| Calce | Cómo queda |
|---|---|
| Pegado | Reemplaza la piel: la tela va justo donde estaba la skin. |
| Ajustado | Aprieta: donde hay tela el brazo, la pierna o el torso quedan más finos (medio píxel menos), y la piel de afuera sigue normal, así se nota el escalón donde corta la prenda. |
| Normal | Un poco holgado: un cuarto de píxel de aire. |
| Suelto | Más holgado, se abre hacia el ruedo (el puño, la botamanga, el borde de la remera) y cuelga 1 píxel por debajo de donde corta. |
| Oversize | Muy grande, se abre el doble hacia el ruedo y cuelga 2 píxeles. |

Las capas se respetan siempre: una prenda de arriba nunca queda por dentro de una de abajo (se corre apenas hacia afuera solo donde se superponen). Un pantalón largo no cuelga por debajo del pie, y la pollera no aprieta por dentro del cuerpo.

**Cómo funcionan los cortes de extremidades.** Los 7 niveles del molde de rango son siempre **la altura de una línea medida de arriba hacia abajo** (nivel 0 = la línea más alta, nivel 6 = la más baja). En medias y calientabrazos hay dos cortes: el pin de **arriba** del esquema dice **dónde empieza** la tela y el de **abajo dónde termina** (sin pin, la tela empieza arriba del todo y llega hasta el final). En el **pantalón** y en la **manga de remera** hay un solo corte por lado, que dice dónde termina, medido desde la cintura o el hombro. Así se arman desde zoquetes hasta medias hasta el muslo, o calientabrazos que solo cubren el antebrazo. Los pines de izquierda y derecha del esquema se leen de frente, así que el de la izquierda del dibujo es el lado derecho del jugador, en todas las categorías (2026-10-04: antes estaba al revés en remera y pantalón).

**Torso, tiro, pollera y capa con el molde de rango.** Los pines de **largo de remera** y de **tiro** usan el molde de rango (el **Molde de torso** corto/medio/largo se sacó el 2026-10-04), con estos 7 puntos:

| Nivel | Largo de remera (filas desde el hombro) | Tiro (la cintura sube) |
|---|---|---|
| 0 | 3 (top) | 12 filas, hasta los hombros |
| 1 | 5 (crop) | 10, hasta el pecho |
| 2 | 7 (corto) | 8, bajo el pecho |
| 3 | 9 (normal) | 6, muy alto |
| 4 | 10 (cadera alta) | 4, alto |
| 5 | 11 (cadera) | 2, medio |
| 6 | 12 (largo) | 0, a la cadera (sin banda) |

**Pollera** (px desde la cintura): ultramicro 3, micro 5, mini 7, a la rodilla 9, midi 11, larga 13 y hasta el tobillo 15. **Capa** (px desde los hombros): capelet 6, a la cintura 9, a la cadera 12, clásica 16, a la rodilla 18, larga 21 y hasta el tobillo 24.

El top, el corto, la cadera alta y la cadera comparten la textura del crop, el normal y el largo que los contienen: la tela se recorta a las filas elegidas. Un tiro hasta los hombros hoy es solo una banda que cubre todo el torso; el peto con tirantes (overall, jardinera) queda para más adelante.

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

Cada color se arma con 5 sliders de 0 a 100% en pasos de 5%: **Cyan, Magenta, Yellow, Key** (negro) y **Transparencia**. La transparencia no gasta tinta: al 100% hace un **recorte** (un agujero en la tela) y entre medio la tela queda **translúcida de verdad** (como un tul o un voile: se ve el cuerpo o la prenda de abajo a través). Las prendas sin transparencia a medias se siguen dibujando como siempre; solo las que la tienen pasan al modo translúcido. Va por color, así se puede, por ejemplo, calar solo los corazones de un patrón. Las muestras con transparencia se ven con fondo a cuadros.

### Muestras de color

Para guardar un color o pasárselo a otra persona (2026-09-30): **Envasar** (debajo de la muestra grande) gasta un **frasco de vidrio** de tu inventario y te da una **Muestra de color** con la mezcla que estás editando. El frasquito toma ese color y su tooltip dice el código y los porcentajes C/M/Y/K/T. **Usar muestra** copia la mezcla de la muestra que tenés agarrada con el cursor (o, si no, de la primera que haya en tu inventario) al color que estás editando. La muestra es solo la receta del color: no trae tinta y no se gasta al usarla. En creativo, envasar no pide frasco.

### Los cuadraditos

| Prenda | Cuadraditos de zona | Cuadraditos de prenda entera |
|---|---|---|
| Remera | Cuello (solo el borde del escote), Manga Izq., Manga Der. (incluyen el hombro), Pecho (el del medio: todo el cuerpo y la parte de arriba), Borde inferior | Los dos de arriba a los costados |
| Pantalón | Cintura (Tiro), Bota Izq., Bota Der. | Los 1 de Trama |
| Medias | Superior e Inferior de cada pierna | Los 6 de Personalización |
| Calientabrazos | Superior e Inferior de cada brazo | Los 6 de Personalización |
| Pollera | — | 3 (fila fija) |
| Capa | Exterior, Forro, Capucha y cuello (fila fija) | — |

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
| **Ángulo** | Gira las rayas en pasos de 15°. En un patrón de motivos gira **toda la grilla** (como girar la tela): con 45° los corazones quedan en filas diagonales. |
| **Giro** | Solo motivos (corazones, estrellas, lunares): gira **cada motivo en su lugar**, de a 15°, sin mover la grilla. |
| **↔ / ↕** (sliders) | Distancia horizontal y vertical entre motivos, de **50 %** (pegados, o apenas superpuestos) a **300 %** (el triple de la separación normal), de a 5 %. En las rayas es la distancia entre rayas: el ↕ separa las horizontales y el ↔ las verticales (las diagonales mezclan los dos). Con la grilla girada, el motivo que cae en la costura de atrás puede no empalmar. |
| **Espejo** | Solo motivos: da vuelta cada motivo, izquierda↔derecha (↔), arriba↔abajo (↕) o los dos. |
| **Alternar** | Solo motivos: de por medio el motivo sale espejado — por **filas**, por **columnas** o en **damero**. Con **Giro** arma un zigzag tipo tejido (una fila inclinada para un lado, la siguiente para el otro). |
| **Simetría** | En el torso (remera, hoodie, pollera), la mitad izquierda es el reflejo de la derecha, espejada en el centro del frente y de la espalda: las rayas en diagonal quedan en **V** y los motivos se enfrentan. Brazos y piernas ya salen espejados solos. |
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

La Sublimadora **imprime fotos** del mod Camerapture sobre remeras, pantalones, medias, calientabrazos, polleras y capas (en la pollera, Frente y Espalda cubren cada mitad de la campana; en la capa, Frente es el exterior y Espalda el forro).

> *Captura pendiente: la interfaz se rehízo el 2026-09-28 con el mismo estilo que Tintes y la Modeladora.*

**La interfaz** tiene el mismo esqueleto que sus hermanas:

| Zona | Qué hay |
|---|---|
| **Izquierda** | Vista previa 3D, botón Vista, nombre, **Guardar diseño** y **Escanear estampa**. |
| **Centro** | **Categoría**; el dibujo de la prenda dos veces, **Frente** y **Espalda**, cada una con su slot de foto y su chincheta; el cinturón **Entrada → Salida** con **Prensar** sobre la flecha; los 8 casilleros de diseño; y los controles de la cara elegida: **Escala**, **Posición X**, **Posición Y**, **Ángulo** y **Cara**, más **Simetría** en medias y calientabrazos. |
| **Derecha** | Los tanques de tinta **C, M, Y, K** y el de **papel** con su nivel (n/64), la guía de pasos y el **almacén** de 27 fotos (3 filas). Al romper la máquina, todo queda guardado adentro del ítem. |

**Escanear estampa** (botón de la izquierda): abre el explorador de archivos de tu compu para elegir una imagen (PNG, JPG, WebP, BMP o GIF), igual que "Cargar imagen" de la cámara de Camerapture, pero sin necesitar la cámara. La imagen se achica y se comprime con los mismos límites que la cámara, se convierte en una **foto de Camerapture** firmada por vos y gasta **1 papel del tanque** de la Sublimadora (nada en creativo ni en la Sublimadora creativa). La foto queda en **Frente** si está vacío; si Frente ya tiene foto, en **Espalda**; si las dos están ocupadas, en el almacén (o en tu inventario si está lleno).

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

### Máscaras y capas

Con un **molde de máscara** en su slot (columna derecha, abajo) la foto se estampa **solo adentro de una forma**: **Cuadrado, Franja, Círculo, Estrella o Triángulo** (un molde por forma, no se gasta; por ahora solo en la pestaña creativa).

1. Poné el molde en el slot **Máscara** y una foto en Frente o Espalda. La vista previa ya muestra la foto recortada con la forma.
2. El botón **Editar: imagen / Editar: máscara** decide qué mueven los controles **Escala, X, Y y Ángulo**: la **imagen adentro de la máscara** (escala 100 % = la foto cubre la máscara; el ángulo de la foto se suma al de la máscara) o la **máscara sobre la prenda** (escala = alto de la máscara respecto del cuerpo, de 10 % a 150 %). Cada cara tiene su propio ajuste de máscara.
3. Con máscara, la **chincheta** de una cara ya no fija la cara entera: **agrega una capa** con esa foto, esa forma y esos ajustes. La foto queda en su slot, así podés moverla y fijar otra capa con la misma foto, o cambiar de foto o de molde.
4. Hasta **12 capas** entre frente y espalda. La lista **Capas (n/12)** las muestra en orden ("3F" = capa 3, frente; "5E" = capa 5, espalda); la de más arriba en la lista (número más alto) se pinta encima. Click elige una capa; **▲ ▼** la suben o la bajan y **✕** la borra.
5. **Prensar** pone la lista de capas en la prenda, además de las caras fijadas sin máscara. Las capas cuentan como **una pasada** de tinta, sin importar cuántas sean. Si la prenda ya traía capas, la lista arranca desde ellas al fijar la primera capa nueva, así no se pierden.

Con máscara, una prenda que ya tiene las dos caras estampadas puede volver a entrar para sumarle capas.

## 8b. Telar automático

El **Telar automático** teje **prendas básicas lisas** con **lana e hilo**: remera, pantalón, medias, calientabrazos, pollera y capa. Tiene la misma base que las otras máquinas de la línea textil y la tapa del telar arriba.

1. Cargá **lana** (cualquiera de las 16 de Minecraft) e **hilo** (string) en sus dos casilleros, a mano, con shift-click, con el ítem en la mano sobre el bloque, o por tolva o cinta desde cualquier lado menos la derecha.
2. **Tildá las prendas** que querés (los seis botones son casillas: podés tener varias). La máquina las teje **de a una, en orden fijo** (remera, pantalón, medias, calientabrazos, pollera, capa) y salta la que no alcance con los insumos. La pantallita del frente muestra la que sigue, teñida con la lana, y la prenda se ve tendida sobre los hilos del telar mientras teje y hasta que la retirás de la salida. En el frente tiene la barra de progreso y dos barras verticales de insumos (lana e hilo).
3. El **lote** es la cantidad total de prendas (botones −10 − + +10; 0 = sin límite). Al terminarlo la máquina se detiene; **↻** lo reinicia con la misma cantidad. **El color de cada prenda es el de la lana.** Cuesta: remera 4 lana + 2 hilo, pantalón 5 + 2, medias 2 + 1, calientabrazos 2 + 1, pollera 3 + 2, capa 6 + 3.
4. La prenda sale por la **derecha** (hacia la cinta o la próxima máquina: Tintes, Modeladora…) o se retira con la mano vacía.

La versión **creativa** no gasta insumos y teje en un tick. Con redstone la máquina se detiene.

## 9. Guardarropas

El Guardarropas sirve para **combinar prendas**: tiene 4 lugares por categoría (remera, pantalón/pollera, medias, calientabrazos, chaqueta) y una columna de **armadura** (casco, pechera, pantalones, botas), así se pueden probar juntas varias prendas del mismo tipo (un croptop sobre un remerón, un pantalón con una pollera y una calza), con vista previa. **Guardar outfit** y **Equipar** guardan y se ponen la combinación, armadura incluida (lo que tenías puesto en ese lugar vuelve a tu inventario; un lugar vacío no te saca nada). La vista previa muestra también la armadura.

Es un mueble con puerta: se pone de frente a quien lo coloca y la puerta se abre hacia afuera mientras alguien tiene la pantalla abierta.

*El sistema de estilos guardados todavía está en construcción.*

### Maniquí

Exhibe un outfit completo. La figura es de **tamaño normal**, como un jugador: con el plato mide casi dos bloques y medio (necesita lugar libre arriba). Tiene los mismos lugares que el Guardarropas (4 por categoría de prenda + la columna de armadura) y **todo se ve puesto en la figura**, en el mundo.

- **Click derecho con una prenda o una pieza de armadura:** se la pone (la prenda en el primer lugar libre de su categoría; la armadura en su lugar, si está libre).
- **Click derecho con la mano vacía:** abre la pantalla.
- **Agachado con la mano vacía**, o botón **Girar/Detener:** el plato gira (una vuelta cada 14 segundos) y se detiene donde está.
- **Intercambiar conmigo:** la ropa y la armadura del maniquí pasan a vos y las tuyas al maniquí, lugar por lugar. No se pierde nada.
- **Pose:** el botón pasa por Parado, En jarra, Saludo, Brazos abiertos, Pasarela y Sentado (sentado, la figura baja y se apoya en el plato). Los **sliders** de la derecha mueven cabeza, brazos y piernas por separado (adelante/atrás y abrir); al tocar uno la pose pasa a **Libre**.
- **Figura:** el botón pasa por *maniquí* (liso, color madera), *tu skin* (la de quien aprieta el botón), las **nueve skins de Mojang** (Steve, Alex, Ari, Efe, Kai, Makena, Noor, Sunny y Zuri, cada una con sus brazos anchos o finos) y, escribiendo un nombre en el casillero de abajo a la derecha y apretando **Ir**, la **skin de ese jugador** (el servidor la busca; tiene que existir).
- **Emote** (2026-10-04, "una animacion del emotecraft si esta instalado"): si tenés **Emotecraft** instalado aparece un botón abajo a la derecha que pasa por *ninguno* y por cada emote que tengas cargado; el maniquí lo baila en bucle, con la ropa y la armadura puestas. El servidor solo guarda cuál es: cada jugador lo ve si tiene Emotecraft y ese emote (si no, ve la pose normal). Solo mueve cabeza, torso, brazos y piernas (no dobleces ni escala). Sin Emotecraft el botón no existe.
- **Candado** (2026-10-04, "que si algun jugador quiere ponerlo en su tienda no le roben"): quien coloca el Maniquí es su dueño. El botón **Candado** lo cierra o lo abre (en un mueble puesto antes de esto, el primero que aprieta pasa a ser el dueño). Cerrado, solo el dueño, los **operadores** y los jugadores en **creativo** pueden poner o sacar ropa, intercambiar, girar, cambiar pose/figura/busto o romperlo; las tolvas tampoco sacan ni meten, y las explosiones no lo rompen. Los demás pueden abrirlo en **solo lectura** y ven en la vista previa **su propio personaje con el outfit del maniquí puesto**, para probárselo. Funciona igual en el **Guardarropas**.
- **Relieve:** la figura tiene el mismo relieve que un jugador (músculos, curvas, nalgas). Con tu skin usa tu perfil de cuerpo si estás conectado.
- **Busto:** el botón pasa por los 7 talles (los mismos de los Estrógenos) y vuelve a "sin". La ropa y la pechera lo envuelven como en un jugador (la pechera, rígida); en la figura no rebota.
- **Vista previa:** la pantalla muestra la figura del maniquí tal cual se ve en el bloque (pose, figura, busto, armadura y ropa), y el botón de vista la gira.
- **Armadura:** se ve como en un jugador (teñido del cuero, trims y brillo de encantamiento). La calabaza, las cabezas y los bloques van en la cabeza. Los élitros todavía no se dibujan.
- Al romperlo, todo cae al piso.

Las medias se ven sin el volumen extra de pierna que tienen en el jugador. Con calce Pegado o Ajustado la figura se afina en esa parte para que la tela no quede adentro.

### Mesa de estilado

Pone **apliques 3D** (moño, mariposa, flor) sobre cualquier prenda del mod, **la pollera y la capa incluidas**, y sobre **cualquier cosa que se ponga**: armaduras vanilla y de otros mods (cascos, pecheras, pantalones, botas) y lo que va en Trinkets. Click derecho abre la pantalla (herrajes lila):

- **Tres slots** arriba a la derecha: **Prenda**, **Molde** de aplique (la forma: moño, mariposa o flor; **no se gasta**) y **Retazo** de aplique (los **3 colores** del aplique; se gasta **uno por aplique**, hasta 64 en el slot).
- **Vista 3D grande** a la izquierda con tu personaje vistiendo solo esa prenda. **Click izquierdo sobre la tela** = ahí queda el aplique, mirando hacia afuera de la cara tocada (con "arriba" hacia la cabeza). Una cruz marca dónde caería. **Click derecho y arrastrar** gira la vista **libremente**: de lado con el movimiento horizontal y **de arriba o de abajo** con el vertical (para poner apliques en hombros y puños); la **rueda** hace zoom, y los botones ⟲ ⟳ giran de a 45°.
- **Apliques sobre apliques.** El click izquierdo también acierta sobre un aplique ya puesto: el nuevo se pega a esa pieza, mirando hacia afuera de la cara tocada, y se mueve con ella (si el padre se balancea, el hijo lo acompaña). Quitar un aplique quita también los que tenía pegados.
- **Lista de apliques** (hasta **12 por prenda**): click en uno lo elige (otra vez lo suelta). Con uno elegido: **Giro** `<` `>` de a 15°, **Tamaño** `<` `>` de 50 % a 250 % de a 25 %, **Quitar**, que devuelve un retazo con sus mismos colores. Todo lo demás (cara de contacto, profundidad, desplazamiento, rotación, escala, pivote y movimiento) está en el **panel lateral**, que aparece al costado de la ventana (a la derecha; a la izquierda si no hay lugar) y ajusta el aplique elegido en vivo.
- **Tela blanda.** Con blandura mayor a 0 el aplique se mueve como tela con la inercia de quien lo lleva (saltar y caer, arrancar y frenar, girar, cada paso): las **colas del moño** se curvan en tramos, las **alas** de la mariposa baten solas (y también reaccionan al movimiento) y las del moño aletean con el movimiento, los **pétalos** se inclinan apenas y las **hojas** cuelgan. El nudo, el cuerpo de la mariposa y el centro de la flor no se mueven. Con 0 % queda rígido como antes. Un aplique de objeto se balancea entero. El botón **Sacudir** alterna sacudones fuertes (uno cada 2,4 s, alternando de lado, y después se suelta) y **exagera** el balanceo para que se vea cómo pendulea y se va frenando; en el **Maniquí** los apliques se mecen suavemente como adorno.
- Si falta algo, el texto de abajo avisa qué (prenda, molde, retazo, prenda llena, click fuera de la tela).

**Mesa creativa y moldes personalizados.** Lo de **apliques de objeto**, el **slot Objeto**, la franja Ítem/Bloque/Variante y el **panel lateral** (colocación y movimiento) existen **solo en la Mesa de estilado creativa**. Ahí armás un aplique y, con el botón **Crear molde** del panel, te da un **Molde de aplique** que guarda todo: el modelo del mod o el objeto (con su modo Ítem/Bloque y variante), la cara de contacto, la colocación, el movimiento, la blandura, el giro y el tamaño (no la posición: esa se elige al ponerlo). El casillero **Nombre del molde** del panel (al lado de Crear molde) le pone el nombre; también se puede cambiar en el yunque. Su ícono se arma solo en el juego: **papel kraft doblado** (cuatro zonas de luz, como cuatro pliegues), el **dibujo lineal de la cara de adelante del bloque** o del sprite del ítem (en tinta marrón; si la textura es muy ruidosa, en escala de grises) y un **recuadro lila**, el color de la Mesa de estilado. Los moldes de moño, mariposa y flor llevan el dibujo que hagas a mano (`molde_aplique_*.png`). En la **Mesa normal** va en el slot del molde como los demás: **no se gasta y, si trae un objeto, tampoco gasta el objeto** (un modelo del mod pide su retazo; a un objeto lo tiñe una muestra de color opcional, que tampoco se gasta). Al quitar un aplique que vino de un molde (o que se puso en la Mesa creativa) no se devuelve nada: nunca se gastó. La Mesa normal sigue como antes de esta tanda: Giro, Tamaño, Quitar, el slider **Blandura**, el botón **Sacudir** y la vista libre.

**Biblioteca y comando de moldes** (2026-10-04). Cada molde que fabricás con **Crear molde** se guarda de dos maneras, para no perderlo: (1) en la **Mesa creativa**, en la página **Moldes** del panel lateral (hasta 24; click en uno = una copia, la **x** lo borra de la lista; si se llena, se va el más viejo; viaja con la máquina si la recogés) y (2) en el **mundo**, a tu nombre (hasta 50 por jugador). Comandos: `/modamod moldes` (los tuyos, del más nuevo al más viejo), `/modamod moldes dar <n>` (una copia del tuyo número n), `/modamod moldes de <jugador>` (los moldes que hizo otro jugador; cualquiera puede verlos) y `/modamod moldes de <jugador> dar <n>` (una copia del suyo; solo operadores, nivel 2).

**Apliques de objeto (solo creativa).** Además de los modelos del mod, **cualquier ítem o bloque** (de vanilla o de otros mods) puede ser un aplique: calabazas talladas, cabezas de esqueleto o de jugador, velas, una espada, una flor.
- Poné el ítem en el slot **Objeto** (a la derecha de Retazo) y hacé click en la prenda: ese objeto queda puesto (**no hace falta molde**; con un objeto en su slot, el aplique que se pone es el objeto). **Se gasta uno por aplique y vuelve tal cual al quitarlo.** En la Mesa creativa no se gasta. No entran ítems con contenido adentro (cofres llenos, shulkers, bolsas, libros escritos, ballestas cargadas).
- **Tinte:** si hay una **muestra de color** de la Estación de Tintes en el slot del retazo, tiñe el objeto de ese tono (se multiplica sobre su textura, así que funciona mejor sobre colores claros); se gasta una y vuelve al quitarlo. Sin muestra, el objeto sale con sus colores.
- Con un objeto elegido aparece arriba de la vista una franja de controles: **Ítem / Bloque** (solo si el ítem es de un bloque: *Ítem* lo dibuja como en un marco, *Bloque* como el bloque colocado), **Variante** (en modo bloque, pasa por los estados que se ven distinto: la **vela encendida**, 1 a 4 velas juntas...) además del Giro y el Tamaño de siempre (los objetos admiten hasta 25 %). La rotación libre y la cara de contacto están en el panel lateral.
- **Panel lateral, página Colocación.** *Cara que se apoya en la tela* (solo objetos): Arriba, Abajo, Izquierda, Derecha, Frente o Atrás; esa cara queda pegada a la prenda y **su centro es el punto de referencia**, así que la rotación y el tamaño giran desde ahí sin correr el objeto de lado. *Profundidad*: saca (+) o mete (−) el aplique, siempre **perpendicular a la tela** (0 = apoyado justo en la superficie). *Desplazamiento* X/Y/Z en px sobre la tela (Z = hacia afuera), *Rotación* X/Y/Z en grados y *Escala* X/Y/Z, todos en los ejes de la tela (X a la derecha, Y hacia arriba, Z hacia afuera). Los modelos del mod (moño, mariposa, flor) también los usan, pero siempre se apoyan por atrás.
- **Panel lateral, página Movimiento.** *Pivote*: de dónde cuelga al balancearse; por defecto el **centro del borde de arriba de la cara apoyada** (como un pin o un cartelito colgado); también centro de la cara, centro del objeto o un punto libre, con corrimientos X/Y/Z. *Intensidad* (es la **blandura** del aplique: 0 = rígido), *Velocidad* (vaivén propio en Hz; con 0 solo reacciona al movimiento de quien lo lleva), *Amplitud* (ángulo máximo) y *Eje*: **En el plano** (gira pegado a la tela), **Bisagra** (se despega de la tela, nunca la atraviesa) o **Ambos**. **Restablecer** devuelve colocación y movimiento a sus valores de fábrica. En los modelos del mod la intensidad sigue moviendo sus huesos (colas, alas, pétalos) y la velocidad suma un vaivén de todo el modelo.
- Todo objeto, grande o chico, se ajusta para que su lado más largo mida unos 5 px a tamaño 100 %, y se apoya por el centro de la cara elegida (por defecto la de atrás). Con intensidad mayor a 0 cuelga y se balancea desde el centro del borde de arriba de esa cara.

**Textura de tela.** Con un **Molde de textura** (Fruncido o Acolchado) en el slot del molde, el botón **Textura** le pone esa textura en relieve a la prenda, o se la saca si ya la tiene (el molde no se gasta). Ver Relieve en la sección 4.

Si tu personaje tiene busto, el click le pega a la cúpula y el aplique se guarda en el punto plano de debajo (sin busto la prenda sigue igual). En la **pollera y la capa** el aplique queda pegado a la tela (sigue el vuelo, el twirl y el balanceo). En una **armadura** la vista previa te la muestra puesta, el click cae en las partes que cubre su slot (casco: cabeza; pechera: torso y brazos; pantalón: cintura y piernas; botas: pantorrillas) y el aplique va por fuera de la armadura; los wearables de Trinkets aceptan todo el cuerpo. Los apliques viajan en la prenda (componente `modamod:apliques`) y se ven en el jugador, en el Maniquí y en el Guardarropas, siguiendo la pose de la parte donde están (brazo, pierna, cabeza o torso) y por fuera del calce de la prenda.

**Colores.** Cada modelo tiene 3 zonas: moño = alas / nudo / colas; mariposa = alas de arriba / alas de abajo y lunares / cuerpo y antenas; flor = pétalos / centro / hojas. Los colores salen del retazo, que ahora se tiñe en la **Estación de Tintes** (categoría **Retazo de aplique**): poné el retazo en la entrada, elegí los cuadraditos **1, 2 y 3** (uno por zona), mezclá el color de cada uno (CMYK, o una muestra de color), fijá la chincheta de las zonas que quieras cambiar y apretá Teñir. Las zonas sin fijar conservan el color que ya traían. Entra de a un retazo por vez (el resto queda en tu mano) y la vista previa muestra las 3 zonas con su color. Por ahora el color es liso por zona; los patrones por zona vienen en una segunda tanda. En creativo hay tres retazos de muestra. Más adelante la tela blanda va a poder conectarse con la pollera.

## 10. Comandos

**Para todos los jugadores** (cambian solo tu apariencia): ver la sección 4, `/modamod elegir`, `cuerpo`, `interior`, `tono`, `ver`, `reset`.

Para probar sin esperas ni insumos ya no hay comandos: están las **máquinas creativas** y los **kits** de la pestaña creativa (ver sección 5).

**Del cliente** (solo cambian lo que VOS ves, para probar):

| Comando | Qué hace |
|---|---|
| `/modamoddebug skin` | Cicla skins de prueba (Steve, Alex, Zuri, Noor, Kai) y la tuya. |
| `/modamoddebug slim` / `ancho` / `automodelo` | Fuerza brazos finos, anchos, o los de la skin. |
| `/modamoddebug reset` | Vuelve a tu skin. |
| `/modamoddebug relieve suave` / `escalonado` / `apagado` | Estilo del relieve del cuerpo y la ropa: continuo, en bloquecitos tipo 3D Skin Layers, o las cajas planas de antes. Solo para vos, para comparar. |
| `/modamoddebug cuello` (`torso` / `cabeza` / `partido`) | Cómo se ata el cuellito de la polera: al torso (quieto, como antes), a la cabeza (gira con ella) o partido (un aro bajo en el torso y el resto en la cabeza, el de fábrica). Sin argumento pasa al siguiente. Solo para vos, para comparar. |

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
| Hoodie (chaqueta) | 8 lanas blancas: `L L L / L L L / L _ L` (la receta del Buzo Oversize viejo) |
| Mesa de Modelado | 3 papeles, hilo + tijeras + hilo, 3 tablones de roble |
| Sublimadora | Hierro alrededor, un pistón en el medio y un horno abajo |
| Molde de manga | Papel + hilo (sin forma) |
| Rayas alternadas | Papel / hilo / papel (filas) |
| Rayas superiores | Hilo / papel / papel (filas) |
| Tres rayas | Hilo / hilo / papel (filas) |

**Sin receta todavía (solo en creativo):** las máquinas creativas y los kits, Estación de Tintes, Guardarropas, Maniquí, Mesa de estilado, Pollera, Estrógenos, moldes de textura, moldes de rango, torso, cuello, calce, red, arnés, pollera y capa, molde de corte, la Capa, y los patrones de corazones, estrellas, lunares y vichy.

Todo el contenido del mod está en su propia pestaña del inventario creativo: **ModaMod**.

## 12. Estado y pendientes

Funciones planeadas que todavía no están:

- Volumen 3D real en la ropa (integración con 3D Skin Layers).
- Cadena de máquinas por tolvas, probada de punta a punta.
- Guardarropas: sistema de estilos guardados.
- Cintos, collares y correas (sistema de armado en la Mesa de estilado, a pensar).
- Mesa de estilado: retazos teñidos con patrones en Tintes, física de los apliques, más modelos (bijouterie, chokers) y recetas.
- La ropa en el brazo en primera persona.
- Piernas redondeadas opcionales (versión 2).
- Recetas para los ítems que hoy son solo de creativo.

# Anexo técnico

Esta parte es para quien quiera entender o extender el mod.

## A. Organización del código

| Paquete | Contenido |
|---|---|
| `item` | Prendas, moldes de patrón, componentes de datos (`ModamodComponents`), redes (`PatronRed`). |
| `modelado` | Mesa de Modelado: bloque, block entity, pantalla, moldes de corte, `ComboCorte`, `PrendaModelado` (aplica un corte a una prenda). |
| `tinturas` | Estación de Tintes: cuadraditos (`Casilla`), capas, diseños, teñido. |
| `sublimadora` | Sublimadora, remera y su `Variante` (largo/manga/cuello), estampas (`EstampaTextures`). Máscaras: `FormaMascara`, `Mascara`, `CapaEstampa` (componente `modamod:estampas_capas`, máx. 12), `MoldeMascaraItem`; `EstampaTextures.pintarConMascara` las pinta después de las caras. |
| `guardarropas` | Guardarropas (modelo GeckoLib `wardrobe`, puerta por `OPEN`); `categoriaDe` reparte prendas en las 4 categorías y lo comparte el Maniquí. |
| `maniqui` | Maniquí: 16 slots sincronizados al cliente, giro del plato calculado en el cliente (`ManiquiRenderer` gira el hueso `turntable` y la ropa con el mismo ángulo) y ropa dibujada con `GarmentFeatureRenderer.dibujarTela` sobre un modelo de jugador slim a escala 0.6. |
| `estilado` | Mesa de estilado: block entity con prenda/molde/retazo, pantalla y `PonerApliquePayload`. |
| `aplique` | Apliques: `Aplique` (componente), `ModeloAplique`, molde y retazo. |
| `bloque` | Genéricos de GeckoLib por nombre de asset: `ModeloGeo` (geo/atlas/animación) y `BloqueGeoItem` (ítem dibujado con la malla del bloque). |
| `region` | Lado (izq./der./ambas), regiones de pintura (`RegionPintura`), `RegionResolver` (lee y escribe capas y colores por lado), `ModoMezcla`. |
| `render` | Composición de texturas (`ClothingTextureCache`), geometría (`CuerpoGeometria`, layout de skin a 8x), generador de patrones (`PatronGenerador`, `Motivo`, `Repeticion`, `Variacion`), el renderer único de ropa (`GarmentFeatureRenderer`). |
| `body` | Cuerpo base (`CuerpoBase`: las 19 máscaras en gris de `textures/entity/cuerpo/`, en HD de 384×384, multiplicadas canal por canal por el tono en `CuerpoBaseTextures`), ropa interior (`RopaInterior` = `InteriorArriba` + `InteriorAbajo` + color; texturas grises `interior_arriba_*`/`interior_abajo_*` de `tools/generar_ropa_interior.py`, teñidas y sombreadas con la máscara en `CuerpoBaseTextures.superponer`), perfil por jugador (`PerfilCuerpo.elegido`), comandos y paquetes de la GUI de elegir cuerpo (`RedCuerpo`). La GUI es `client/ElegirCuerpoScreen` y el disparador `ElegirCuerpoCliente`. |
| `client` | Pantallas, vista previa 3D, estilo pergamino, piezas de cada prenda (`PiezasDelMod`). |

## B. Cómo se dibuja una prenda

1. `GarmentFeatureRenderer` junta todo lo que el jugador tiene puesto en Trinkets y lo dibuja desde un solo lugar, ordenado por **capa** (`Capa`), sobre el cuerpo base.
2. Cada prenda devuelve sus **piezas** (`PiezasDelMod`): qué parte del cuerpo cubre, en qué capa, con qué textura y qué filas de la caja son visibles (así se recortan largos sin generar un archivo por cada largo).
3. La textura se **compone en tiempo real** sobre un atlas con el layout de la skin a escala 8x (512×512) y se cachea por combinación: color base, capas de color, estampas, recorte y red.
4. **Pollera y capa van aparte**, con malla propia: `PolleraMalla` en el marco del torso (la tela se reparte por largo de tela de la pollera quieta, `PolleraMalla.Perfil`, con el centro del frente en u 24 y el de la espalda en u 36; `PatronGenerador.desplegar` dibuja los patrones con la clave `pollera:<forma>:<largo>` llevando cada pixel a la tela desplegada: vuelta de esa fila / 24 en horizontal, largo de tela / 12 en vertical) y `CapaMalla`, una cadena de 16 tramos (`doblar`) que parte de la inercia de `CapeFeatureRenderer` (`CapaMalla.movimiento`), se curva hacia el ruedo, ondea y se abre para no atravesar las piernas (`PolleraMalla.Piernas` llevado al marco de la capa). La tela de la capa (`capa_tela.png`) usa el layout del cuboide de la capa vanilla (10×16×1 en uv 0,0: exterior u 1..11, forro u 12..22) más una caja de detalles 12×8×2 en uv 24,0 para capucha y cuello; `PatronGenerador` la pinta como las cajas `CAPA` y `CAPA_DETALLES` y las zonas son `RegionPintura.CAPA_EXTERIOR/CAPA_FORRO/CAPA_DETALLES`. `PlayerEntityCapasSkinMixin` apaga la capa vanilla (`isPartVisible(CAPE)`) mientras haya una del mod y no haya élitros.

**Apliques.** `render/ApliqueRenderer` dibuja los `.geo.json` de GeckoLib (`tools/generar_apliques.py`: miran a −Z, espalda en z = 0, atlas de 96×32 con una columna de 32 px por zona) recorriendo huesos y cubos a mano, en la misma pila de matrices que la ropa: `ModelPart.rotate` de la parte, el punto del click corrido hacia afuera por la dilatación del calce, y una base (derecha, arriba, atrás) que apunta −Z a la normal de la cara. El atlas se tiñe por columna y se cachea por terna de colores. La Mesa ubica el click invirtiendo la matriz de cada parte que guarda `GarmentFeatureRenderer.capturaPoses` durante el render de la vista previa (rayo de pantalla contra la caja de la pieza, cara de salida = la que se ve).

**Máquinas que se guardan enteras.** `util/DropMaquina`: los 4 bloques sobreescriben `getDroppedStacks` (romper a mano y explosiones) y devuelven el ítem con todo el NBT del block entity en `minecraft:block_entity_data` más sus componentes (la tinta); `BlockItem` lo vuelve a cargar al colocarla. Ya no hay loot tables ni `onStateReplaced` que desparrame. En creativo, `onBreak` tira el ítem si la máquina no está vacía.

## C2. Relieve

`render/relieve`: un `MapaRelieve` guarda alturas (px de skin, hacia afuera) de los 4 costados de torso, brazos y piernas en grillas de 4 celdas por px (caras en el orden de la skin: costado −x, frente, costado +x, espalda). `RelieveCuerpo` arma el del cuerpo con una receta por `CuerpoBase` (`Forma`: pecho, abdomen, panza, cola, brazos, piernas, cintura, busto de fábrica) y la definición del `PerfilCuerpo`. La cola con busto también es geometría (`BustoRender.formasCola`/`cajasCola`, `dibujarCola` en un marco espejado que mira a +Z, UV `Uv.skinEspalda`, tamaño `RelieveCuerpo.colaDe`, resorte `FisicaBusto.cola`; la pollera la esquiva en `PolleraMalla.pasarPorFueraDeLaCola`). El corpiño y los croptops: `comprimirAbajo` aprieta la textura de la mitad de abajo del busto hasta la última fila con tela. Bajo un manto, el cuerpo y las piezas de adentro no dibujan busto (`GarmentFeatureRenderer.indiceManto`). El busto no va en el relieve: `BustoRender` arma una malla (`malla`: cúpulas, `manto` para las telas Suelto/Oversize — campo de alturas calcado de las cúpulas, tensado por filas y columnas con `tensar`, apoyado en la dilatación por fila de la pieza (`Tela`) — o `cajas` si `PerfilCuerpo.bustoCuadrado`), con la que se dibuja, se apoyan los apliques (`sobreBusto`) y se apunta (`rayo`, con `carpaDe(calce)`); por defecto, dos cúpulas (media elipse de perfil redondo) en el marco del torso (talle = el del cuerpo + `BUSTO_POR_DOSIS`, caída, apertura, UV del frente del torso) para el cuerpo y para cada pieza que tapa el pecho, infladas por capas, con el rebote de `FisicaBusto`. `BustoEnModelos` (mixins `BustoEnModelosMixin` al final de `AnimalModel.render`, `ArmaduraBustoMixin` en `renderArmor` y el contexto en `LivingEntityRendererMixin`) agrega las cúpulas a cualquier `BipedEntityModel` ajeno que se dibuje sobre un jugador con busto, con las UV de la cara del frente de su torso (`Uv.deCaraDelTorso`) y el inflado de esa cara; la pechera va rígida, las demás piezas de armadura no lo llevan. `ApliqueRenderer` apoya los apliques del frente del torso con `BustoRender.sobreBusto` y la Mesa los apunta con `BustoRender.rayo` (guarda el punto plano de debajo). `RelieveTela` arma el de cada pieza sobre el de abajo: envolvente con radio y pendiente según el calce (Pegado = copia), arrugas por calce, `TexturaTela` (componente `modamod:textura_tela`) y el archivo `textures/models/relieve/<id>.png` (gris en el layout de la skin; `tools/generar_relieve_prendas.py`). `RelieveRender` reemplaza a `ModelPart.render` (vía `forEachCuboid` y las caras del cuboide, abiertas con `modamod.accesswidener`): reconoce cada cara por sus UV y la parte en una grilla desplazada (suave, con normales del relieve) o en bloquecitos (escalonado). `GarmentFeatureRenderer` pone el contexto (`RelieveRender.actual`) alrededor del cuerpo y de cada pieza, encadenando las piezas de adentro hacia afuera.

## C. Capas de color (Estación de Tintes)

**Transparencia real.** Una capa con el canal T entre 1 y 254 (`CapaMascara.translucida`) hace que `ClothingTextureCache.composeGarmentCapas` componga sin `tramar` (bandera `componiendoTranslucida`, que también respetan las estampas y la banda de cintura) y sin achicar la textura, y la anota en `TRANSLUCIDAS`. `capaDeRender` elige `RenderLayer.getEntityTranslucent` para esas texturas (si no, `ArmorCutoutNoCull`). En `GarmentFeatureRenderer` las piezas translúcidas van a una segunda pasada (`dibujarTranslucidas`, de adentro hacia afuera) después de todo lo opaco, y la capa translúcida se dibuja al final.

Cada capa es un `RegionResolver.CapaPatron` guardado en la lista `modamod:capas_tinte` de la prenda: patrón opcional (sin patrón = liso), color principal y extras, tamaño, ángulo, posición, forma, invertido, región, modo de mezcla, opacidad, repetición, semilla, contorno, variación y la distribución (`DistribucionPatron`: giro de cada motivo, distancias horizontal/vertical, espejo, espejo alternado y simetría del torso). Las capas viejas de la Modeladora (componentes `pattern_*`, con variantes `right_*` para el lado derecho) se siguen leyendo.

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

- **Un motivo nuevo:** agregar el dibujo (filas de `X` y `.`) en el enum `Motivo`, registrar un `ClothingPatternItem` con ese motivo en `ModamodItems`, y sumar ícono, modelo, traducción y entrada en la pestaña (`Modamod.PESTANA`).
- **Una red nueva:** agregar el valor al **final** del enum `PatronRed` (el orden viaja por red), su `Dibujo` y la lógica en `ClothingTextureCache.esHilo` (o `perforarArnes` si es un arnés), y registrar el `MoldeRedItem` en `ModeladoMod`.
- **Una prenda nueva:** registrar el ítem, declarar su `Garment` en `PrendasDelMod`, sus piezas en `PiezasDelMod`, y el slot de Trinkets si hace falta uno nuevo.

## E. Compilar y probar

- `gradlew build` genera el `.jar` en `build/libs/`.
- Para probar: copiar el `.jar` a la carpeta `mods` del perfil **con el juego cerrado**.
- Los fondos de interfaz de pergamino se generan con `tools/generar_textura_modelado.py`, `generar_textura_tinturas.py` y `generar_textura_sublimadora.py` (este último reusa las funciones del de Tintes). Los íconos de las prendas salen de `tools/generar_iconos_prendas.py`: por prenda, un `_sombra.png` (silueta y relieve) y un `_mapa.png` (de qué cara del cuerpo y de qué punto se saca cada píxel) en `textures/item/icono/`; `IconoPrenda` los combina en Java con las texturas de `PiezasDePrenda`, recorta las filas que la pieza no tapa y pone el contorno. El color de cada máquina sale de `TEMAS`/`aplicar_tema` (verdín, cobre, oro), con los mismos valores que `EstiloPergamino.Tema` usa para los botones.
- Este manual se genera con `python tools/generar_manual.py`, que arma las imágenes y convierte `docs/manual/MANUAL.md` en `docs/manual/ModaMod_Manual.docx`.

**Estilista automática, etapa 2: diseños, insumos y trabajo** (2026-10-05, "la primera que entre o la del input, vamos con 13 s, que gaste y recupere vamos con todo"; sin probar en el juego): la Estilista ya aplica diseños. Un **diseño** es lo que lleva una prenda de muestra —apliques, correas, textura de tela y, en sombrero y bandas, colores y dibujos— y la máquina guarda **uno por tipo de prenda** (hasta 32). Se arma así: ponés a mano una prenda que ya tenga su estilado (hoy se hace en la Mesa de estilado) y apretás **Fijar muestra**: ese diseño queda guardado para su tipo y la prenda sale por la derecha. **Borrar diseño** lo olvida. Después, cada prenda de ese tipo que llegue por la cadena arranca sola y tarda **13 segundos** (el pórtico trabaja con sus pistones); a mano se carga y se aprieta **Aplicar**. Una prenda sin diseño para su tipo pasa de largo. Los **insumos** se guardan en el almacén de la máquina (18 casilleros, entran también por los costados y por atrás con tolva): cada aplique cuesta **1 hilo** (el de vanilla), cada correa **1 cuero**, y un aplique de objeto además gasta ese objeto (y su muestra de color, si la lleva); los colores no cuestan nada. Si la prenda ya traía apliques o correas, al aplicar el diseño **se le sacan y sus insumos vuelven al almacén**. Si falta algo, la prenda espera en la entrada (la pantalla dice qué falta) y arranca sola cuando se repone. La variante creativa no gasta ni devuelve nada. La pantalla actual es provisoria; editar el diseño dentro de la máquina llega con la pantalla definitiva (etapa 3).

**Estilista automática, etapa 3: editor dentro de la máquina** (2026-10-05, "vamos con toda"; sin probar en el juego): la pantalla de la Estilista es ahora la **de la Mesa de estilado** (la vista 3D grande, apliques, correas, colores y dibujos de sombrero y bandas, panel de colocación en la creativa) editando la **prenda de muestra**, más un panel a la izquierda con la máquina: **Entrada** y **Salida**, los botones **Aplicar**, **Fijar** y **Borrar**, el diseño que hay para ese tipo de prenda y los 18 casilleros de **insumos**. Se edita gratis (no hace falta retazo ni se gasta nada: sin retazo los apliques salen blancos, con retazo o muestra de color en su casillero salen de ese color); el costo real —hilo por aplique, cuero por correa— se paga al aplicar el diseño a cada prenda. **Fijar** guarda la prenda de muestra como el diseño de su tipo y la deja en el editor para seguir ajustándola. En la Estilista creativa el editor trae también el casillero de objeto y el panel de moldes personalizados de la Mesa creativa. La Mesa de estilado sigue funcionando hasta que se jubile.

**Estilista automática: pantalla como las hermanas, contadores y almacén** (2026-10-05, "más como las otras guis con un gran slot para input y output y un apply en el medio... contador de hilo y cuero y el almacén que sea para guardar apliques, los insumos pueden cargar invisibles"; sin probar en el juego): la vista 3D grande queda a la izquierda y a la derecha está el panel de la máquina: **entrada grande**, **Aplicar ▶** en el medio y **salida grande**; **Fijar** y **Borrar** el diseño; dos **contadores** (hilo y cuero, tope 256 cada uno); y el **almacén** de 18 casilleros para plantillas, retazos y los objetos que gastan los apliques de objeto. El hilo rinde **4 apliques por ítem** y el cuero **2 correas por ítem**; los contadores se cargan sin casilleros visibles: soltando el ítem del cursor sobre el contador, con shift-click desde el inventario, con la mano sobre el bloque o con tolva/cinta por cualquier lado que no sea la entrada ni la salida. Al aplicar un diseño se gasta de los contadores (y los objetos, del almacén); lo que la prenda traía vuelve. Shift-click: del almacén al editor (molde, retazo, muestra) y del editor al almacén.

**Estilista: contadores a 64 e indicadores en el frente** (2026-10-05, "bajemos el almacenamiento de insumos hasta 64 cada uno y agreguemos algun indicador visible en el frente"; sin probar en el juego): cada contador (hilo y cuero) aguanta **64 unidades** (16 hilos o 32 cueros). En el frente de la máquina, arriba de los tres LED, hay dos barritas —crema para el hilo y marrón para el cuero— que se vacían a medida que se gastan los insumos.

**Cintas: empalme suave, borde continuo y curvas animadas** (2026-10-05, "la conjunction belt muestra los items diferentes y a diferente velocidad... deberia ser un viaje smooth" + "sigue habiendo un pixel de maderita al borde y parte de la animacion de las cintas curvas no se mueve"; sin probar en el juego): el **empalme** ahora dibuja lo que cruza igual que la cinta (la prenda con su ícono, los otros ítems acostados) y a la misma velocidad de 1 px por tick: de atrás en línea recta (16 ticks), de un costado en arco como las curvas (13 ticks) y desde arriba del centro al frente (8 ticks); antes se quedaba quieto 8 ticks en el centro y dibujaba todo como ítem suelto. En las cintas se sacó el **filete de latón** de la madera (marcaba una costura de 1 px en cada bloque), la losa llega a la altura de la banda (sin escalón ni rendija) y la banda cubre todo el bloque (en las curvas, lo de afuera del anillo es cuero quieto, sin madera a la vista). La banda de las **curvas** no se movía porque los 4 cuadros salían iguales (el paso era un período entero por cuadro): ahora avanza 1 px por tick a radio 8.

**Paneles de la Modeladora y la Estilista, y la tijera** (2026-10-05, "los paneles derechos de las modeladora y estiladora tienen los tres leds a la derecha y tienen tres cuadraditos sin uso, quiero que los saquemos... dos indicadores verticales grandes de los insumos... a la modeladora le vamos a poner un insumo... una pantallita donde va a aparecer una tijera... una barra verde su durabilidad"; sin probar en el juego): se sacaron los tres cubitos decorativos del panel derecho (los tres LED verticales del borde se quedan: indican el estado). **Estilista:** en ese hueco hay dos barras verticales grandes, crema para el hilo y marrón para el cuero, que bajan con los contadores (reemplazan a las barritas horizontales de antes). **Modeladora:** una pantallita muestra la **tijera** puesta —con su silueta y una barra verde de durabilidad— y apagada si no hay. La tijera es el insumo de la Modeladora: son las **tijeras de vanilla**; cada **Modelar gasta 1 uso** y **sin tijera no corta** (la pantalla avisa "Falta una tijera"; una prenda que pasa de largo por la cadena sin nada fijado no gasta). Se cargan con la tijera en la mano sobre el bloque, con shift-click en la pantalla o con tolva/cinta (por cualquier lado menos la entrada y abajo); **agachado y con la mano vacía** la tijera vuelve a tu inventario. La Modeladora creativa ni la necesita ni la gasta.

**Correas: Anclada, Colgada y largo** (2026-10-06, "a la correa hay que agregarle un tipo de anclaje de un solo punto y que cuelgue con la gravedad... al de los dos puntos ademas de ancho deberia poder agregarsele largo y que la cadena cuelgue"; sin probar en el juego): las correas libres ahora tienen **cuatro modos**: *Pegada* y *Colgante* como antes (el Colgante queda igual, se sigue probando) y dos nuevos. **Anclada**: **un solo click** en la prenda ancla la correa ahí y cuelga hacia abajo con la gravedad el **largo** elegido, balanceándose con el movimiento (según la blandura) y apoyándose en el cuerpo sin atravesarlo. **Colgada**: dos clicks fijan los extremos y el **largo** es la longitud real de la cadena; si es mayor que la distancia entre los dos puntos, **cuelga en curva** con la gravedad (si es igual, queda tensa). El **largo** se elige con los botones **− / +** (o clickeando el valor) de **2 a 32 px de a 2**, junto al ancho; vale solo para Anclada y Colgada y se guarda en cada correa (elegida de la lista, cambia la suya). Funciona también sobre la pollera y la capa, el sombrero y las bandas.

**Pantalla de Ropa: acomodo y 3 slots por prenda** (2026-10-06, "la mitad de los casilleros de ropa me aparecen fusionados con el inventario... reduzcamos a 3 los slots por prenda"; sin probar en el juego): las prendas de torso (remeras y chaquetas), los calentabrazos, las prendas de piernas y las medias pasaron de **4 a 3 slots** cada una. En la pantalla de Ropa (tecla K) cada prenda ocupa una fila de 3; sombrero, gargantilla y capa comparten la primera fila y el cinto va solo debajo del muñeco; la ventana es más alta y el inventario ya no se pisa con los casilleros. Si ya tenías algo en el 4.º slot de una prenda, el juego lo suelta al cargar. El Guardarropas y el Maniquí **conservan sus 4 casilleros por categoría** (cambiarlo movería lo que ya guardaste): el 4.º no se puede vestir.

**Guardarropas y Maniquí: 3 casilleros por categoría** (2026-10-06, "bajalos a 3 y regenera los fondos"; sin probar en el juego): igual que en la pantalla de Ropa, el Guardarropas y el Maniquí tienen ahora **3 casilleros por categoría** (antes 4) y sus fondos de pantalla se regeneraron. **Sin migración:** lo que ya hubiera guardado en un Guardarropas o Maniquí colocado, y sus outfits guardados, quedan desordenados (los índices cambian); conviene vaciarlos antes de actualizar.

**Cintas que llevan a quien se sube y redstone en cadena** (2026-10-06, "si el jugador se sube a un conveyor belt lo mueva" + "el apagado de redstone se contagie hasta 8 conveyors"; sin probar en el juego): cualquier entidad parada sobre una cinta (recta, curva, rampa o empalme) es llevada hacia donde mira, a la velocidad de la banda (1 px por tick); el jugador puede seguir caminando. En las curvas sigue el arco. Con señal de redstone no empuja. La señal de redstone ahora se **contagia a las cintas y empalmes encadenados hasta 8 bloques de distancia** (en los dos sentidos): todas se paran a la vez y reanudan al cortarse la señal. **Aceleración:** con **hielo compacto** debajo de la cinta el empuje a quien se sube es el **doble** y con **hielo azul** el **triple** (los ítems que lleva la cinta también viajan ×2 / ×3 más rápido). Las correas **Anclada** y **Colgada** sobre pollera y capa ahora cuelgan siempre hacia afuera de la tela, no por dentro.

**Banners y escudos con foto** (2026-10-06, base, sin probar en el juego): un banner puede llevar una **foto de Camerapture** que se dibuja a **16 veces la resolución** de vanilla, y un **escudo** hecho en la mesa de crafteo con ese banner la hereda. Se pone en la **Sublimadora**: cargás el banner como una prenda, la foto en el slot de **Frente** (el banner no tiene espalda) y ajustás escala, posición y ángulo como siempre; la vista previa muestra el banner con la foto. Al romper el banner la foto se conserva. La Estación de Tintes todavía no los acepta. No se estampan en prendas.

**Acabados con trims** (2026-10-06, sin probar en el juego): un **molde de trim de vanilla** puesto en el casillero del molde de la **Mesa de estilado** o de la **Estilista** da a la prenda entera un **acabado animado y brillante**, con un efecto propio por cada uno de los 18 patrones: **Sentry** acero con barrido de luz, **Dune** arena con granos que centellean, **Coast** espuma de olas, **Wild** venas de hoja con motas, **Ward** latido de sculk, **Eye** ojos del End que parpadean, **Vex** niebla espectral, **Tide** cáusticas profundas, **Snout** magma, **Rib** brasas, **Spire** iridiscencia del End con estrellas, **Wayfinder** haz de brújula que gira, **Shaper** líneas de cantera con pulso, **Silence** oscuridad con puntitos pálidos, **Raiser** calor que asciende, **Host** runas que se encienden de a una, **Flow** remolinos de viento, **Bolt** arcos de cobre; los patrones de otros mods tienen brillo metálico. El magma (Snout) son placas de corteza quietas con grietas incandescentes que se enfrían y se calientan; el efecto se calcula a la resolución de la tela y se anima a 10 cuadros por segundo. Todos los de vanilla se duplican (molde + 7 diamantes + el bloque de su estructura); lo raro es conseguir el primero. **Material (opcional):** en el casillero de material va un material de trim (lingote de hierro, cobre, oro o netherita, cuarzo, redstone, lapislázuli, esmeralda, diamante o amatista) que cambia los colores del efecto; sin material, cada patrón usa su paleta de fábrica. El botón de acabado pone el patrón (y el material, si hay) y **gasta el molde y 1 de material** sin devolverlos; si la prenda ya lleva ese patrón, lo quita. Sin molde pero con material, el botón cambia solo el material de un acabado que ya está. La Estilista creativa y la Mesa creativa no gastan nada; la Estilista normal los toma del almacén al aplicar el diseño (solo si cambian respecto de lo que la prenda ya lleva). Funciona con remera, chaqueta, pantalón, medias y calientabrazos; pollera, capa, sombrero y bandas llegan después.

**Pestañas de la Mesa de estilado** (2026-10-06, "conceptualmente son tres cosas aplique correa acabado"): abajo de los controles hay tres pestañas — **Aplique** (con su contador), **Correa** (con su contador) y **Acabado** (textura de la tela, trim con material y, en el sombrero y las bandas, el color y el dibujo de cada zona). Lo que se arma en la Mesa queda guardado por tipo de prenda en la Estilista (aplicadores, correas y acabado, con su material, en un mismo diseño).

**Polleras en capas** (2026-10-07, "que las polleras rendericen abajo de otras polleras" + "agrandame la resolucion de las polleras los motivos quedan mal"; sin probar en el juego): podés llevar **varias polleras a la vez** (los lugares del slot de pantalón/pollera). Se dibujan en el orden de los slots, cada una **0,3 px** más afuera que la de abajo, y la chaqueta y la capa pasan por fuera de la más externa. Los patrones de la pollera se componen ahora a la resolución completa (8x) en vez de achicarse a 4x cuando llevan dibujo. En el Maniquí funciona igual.

**Polleras: patrones sin estirar atrás** (2026-10-07, "minimizar la deformacion de los patrones en las polleras sobre todo atras que se ve bien feo"; sin probar en el juego): la tela de atrás se aparta de las nalgas y mide más que la pollera quieta, así que el dibujo salía estirado. Ahora el reparto del dibujo sobre la tela tiene en cuenta el tamaño de la cola, y cada talle compone su propia textura. **Pendiente:** el patrón sigue con un corte en el centro de la espalda y se estira un poco con el movimiento y al chocar con las piernas.

**Cinta: deslizarse en vez de caminar, y cinta parada sobre hielo** (2026-10-07, "en 3era persona se lo ve caminar por la conveyer belt" / "cuando esta detenida una cinta con redstone y tiene hielo abajo ... deslizo como si caminara sobre hielo"; sin probar en el juego): sobre una cinta que te lleva, las piernas y los brazos ya no se animan como caminando (vale también para los demás jugadores y mobs); si caminás por tu cuenta, se animan por tu propio paso. Con la cinta **parada por redstone** y hielo debajo, la fricción es la del hielo (te deslizás); en marcha, el hielo sigue acelerando la cinta como antes.

**Ruedo (remate de los bordes libres)** (2026-10-07, "un ruedo ajustado que podria servir para ruedo de remera mangas botamangas y hasta polleras" + "fusionemos borde pollera con ruedo" + "dejemos la posibilidad de construir asimetricamente"; sin probar en el juego): un nuevo molde, el **Molde de Ruedo**, cambia el remate de un borde libre de la prenda. Hay seis: **Recto** (el de siempre), **Ajustado** (elástico: la última fila aprieta y la tela no cuelga, con un rib pintado), **En campana** (las últimas filas se abren hacia afuera) y, solo para la pollera, **Ondulado**, **Festoneado** y **Con pico** (reemplazan al viejo "Molde de Borde" y al pin Custom: ahora es el pin de **Ruedo** de la pollera). En la Modeladora cada borde tiene su propio pin, así que se puede armar **asimétrico**: la **remera** y la **chaqueta** tienen un pin de **Ruedo** (torso) y uno de **Puño** por manga (con la simetría activa, el puño vale para las dos). La chaqueta de fábrica lleva ruedo y puños **ajustados** (el hoodie de siempre); la remera, rectos. El **Molde de Remate** de la chaqueta (elástico/recto) ya no existe: se usa el Molde de Ruedo. Los moldes de ruedo se guardan en el banco de cada prenda (no en el almacén general). Cada categoría de la Modeladora pasó a tener **14 pines** (antes 12). Todavía falta: ruedo de **pantalón** (botamanga por pierna), **calientabrazos** y **medias** (borde superior e inferior por lado), y que **Ajustado** y **En campana** se vean en la pollera.

**Top: remera y chaqueta, una sola prenda** (2026-10-07, "un solo top" → "vamos con la fusion"; sin probar en el juego): ya no hay un ítem "Chaqueta" aparte: la **remera** es la única prenda de torso, y lo que antes hacía a una chaqueta son **rasgos que cualquier remera puede tener**: **capucha** (con la tecla H), **frente abierto** y **solapas**, además del **ruedo** y los **puños** ajustados. Una remera con alguno de esos rasgos cambia de nombre sola: **Hoodie** (con capucha), **Saco** (solapas, sin capucha), **Campera** (frente abierto), **Chaleco** (sin mangas) o **Buzo** (manga larga sin capucha ni frente abierto). **Qué capa se dibuja lo decide el lugar donde la llevás:** en el lugar de **Remera** va debajo de la pollera y de las chaquetas; en el lugar de **Chaqueta** (el de arriba, al lado del muñeco) va **por encima** de todo, como la chaqueta de antes. Cualquier remera entra en los dos. En la **Modeladora** desaparece la pestaña **Chaqueta**: los pines de **Frente**, **Capucha** y **Solapa** están ahora en la pestaña **Remera** (14 pines: cuello, 3 materiales, mangas, calce, torso, ruedo, puño izquierdo y derecho, frente, capucha y solapa). El **hoodie de fábrica** (craftea con 8 lanas) es una remera larga, de manga larga, oversize, con capucha y ruedo y puños ajustados. **Los hoodies y los moldes de chaqueta de antes no se migran** (reset de mundos).

**Solapas pegadas al escote y a la apertura** (2026-10-08, "las solapas no esten pegadas a la abertura del cuello ni de la chaqueta"; sin probar en el juego): las solapas del Top (pico, redondas, chal) y las puntas del cuello de camisa ahora **siguen el borde real del escote** de la prenda (V, redondo, cuadrado, corazón, camisa), así que quedan pegadas a la tela en vez de flotar. Con el **frente abierto**, la solapa se pega al borde de la apertura (a 1 px del centro). Siguen siendo cajitas de tela de color liso; en próximas partes pasan a ser una tela continua con pliegue, y la apertura gana grosor y se adapta al calce y al busto.

**Solapas de tela continua** (2026-10-08, "se pueden hacer mas de tela como las polleras y pegarlas al borde del cuello?"; sin probar en el juego): las solapas del Top (pico, redondas y chal) ya no son pilas de cajitas: son una **tira de tela continua** a cada lado, pegada al borde del escote (o de la apertura si el frente está abierto), con el borde de afuera levantado como un **doblez** y sus cantos cerrados. Toman la **textura de la prenda**, así que llevan sus patrones y estampas, un poco más oscuras para que se lea el doblez; con busto suben sobre la curva. Falta que la apertura del frente gane grosor real y que la solapa siga hasta el ruedo.

**Cantos de la apertura** (2026-10-08; sin probar en el juego): con el **frente abierto**, cada borde de la apertura lleva ahora una **tira de tela fina** de arriba abajo, hasta el ruedo, con la textura de la prenda. Le da grosor a la abertura y continúa a las solapas (que van encima). Vale con y sin solapas. Falta que la apertura se adapte al calce y al busto, y los frentes **Cruzado** y **Abierto hasta el pecho**.

**Cuellos más bajos** (2026-10-08, "bajarle un poco mas el cuello cuadrado y el redondo"; sin probar en el juego): el cuello **cuadrado** baja de 1,75 a 2,75 texeles de hondo, y el **redondo** (el cuello de fábrica de toda remera) gana un escote de media elipse de 1,5 texeles de hondo; antes solo tenía la abertura de arriba y el frente entero. Las solapas y el cuello de camisa siguen el escote nuevo.

**Tintes: esquema propio y zona Solapas** (2026-10-08; sin probar en el juego): la Estación de Tintes ya no comparte el dibujo de la Modeladora. La **remera** (y el Top) muestra solo los lugares que pintan algo: **dos de prenda entera, pecho, cuello, manga izquierda, manga derecha, borde de abajo y Solapas**. La zona **Solapas** tiñe (color, patrón, mezcla, transparencia) la tela de las solapas por separado del resto: sigue su tipo (pico, redonda, chal) y su borde, también con el frente abierto; una remera sin solapas la ignora. El cuello de camisa no entra en esta zona.



**Apliques: bolsillo canguro y corbata** (2026-10-08). Dos plantillas nuevas de aplique en la Mesa de estilado / Estilista: **Bolsillo canguro** (parche plano de 6,5 × 3,5 px con las bocas en diagonal y la costura de abajo, tres zonas de color: tela, vivo y costura; no se mueve, está pegado a la tela) y **Corbata** (nudo y hoja que se ensancha hacia la punta, con raya al medio; la hoja cuelga en tres tramos y se curva con el movimiento según la blandura). Se ponen como cualquier aplique (click en la prenda, giro, escala, tres colores del retazo); la corbata va bien sobre el torso de una remera con cuello de camisa.


**Línea de producción, materiales y pollera corta** (2026-10-08). **Botón de línea de producción:** las cinco máquinas (Modeladora, Tintes, Sublimadora, Estilista y Telar) tienen un botón "Línea: sí/no" en su pantalla, **apagado de fábrica**. Apagada, la máquina se usa sola: no pasa la prenda terminada a la máquina del lado y no saltea las prendas que le llegan por cinta o tolva (las espera como si las hubieras puesto a mano). Encendida, trabaja en cadena como antes. La pausa por redstone sigue aparte. **Moño con cintas:** el aplique de moño ahora lleva dos cintas anchas con punta en V, en vez de cordones finos; se curvan con la tela blanda. **Materiales** (moldes "Material: Denim" y "Material: Cuero", van en el casillero de la Trama de la Modeladora): en lugar de agujerear, le dan trama a la tela ya teñida. *Denim*: sarga diagonal con desgaste a manchones. *Cuero*: grano de poros con grietas y un brillo "especular" pintado (una banda en cada cara y destellos en el grano; no es un mapa PBR, se ve igual con y sin shaders). Ocupan el mismo lugar que una trama, así que una prenda lleva una cosa o la otra. **Pollera corta:** la textura ya no se achata: cada texel mide lo mismo a lo alto que a lo ancho de la cintura, así que una pollera corta usa solo las filas de arriba de la tela (lo de abajo, como una foto, se recorta en vez de deformarse) y una larga la estira apenas.

## Herramienta de debug: HUD de rendimiento

`/modamoddebug perf` (solo cliente) prende o apaga un panel arriba a la izquierda con los FPS, los milisegundos por cuadro, la memoria de Java, el costo por cuadro de cada parte del mod (ropa, pollera, capa, apliques, correas, texturas, acabados) y el tamaño de sus cachés. Sirve para ver dónde se va el lag al probar varias prendas o acabados a la vez.
