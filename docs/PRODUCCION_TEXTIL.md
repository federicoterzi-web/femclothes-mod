# Sistema de Producción Textil — informe de diseño (GUI y mecánicas)

> Documento del dueño, pegado tal cual (2026-09-09), fuente autoritativa para
> el rediseño de Modeladora / Estación de Tintes / Sublimadora. Reemplaza el
> modelo de interacción "instantánea sin estado" descrito en
> `MAQUINAS.md` §3 para estas tres máquinas — ver nota al final.

## 1. Filosofía general del sistema

Las máquinas funcionan como equipamiento industrial configurable. Primero se
preparan y configuran mediante GUI; luego se encienden físicamente y pasan a
producir automáticamente.

### Estados de funcionamiento

**Apagada / Configuración:**
- Se puede abrir la GUI.
- Se pueden agregar y retirar recursos.
- Se pueden gestionar patrones e imágenes.
- Se pueden modificar settings.
- Se pueden crear, fijar y desfijar configuraciones.
- La configuración permanece guardada en la máquina.

**Encendida / Producción:**
- La configuración queda bloqueada.
- La GUI no puede abrirse.
- Cuando recibe una prenda compatible comienza automáticamente el
  procesamiento.
- Al terminar queda lista para procesar la siguiente prenda.
- Para modificar la configuración es necesario apagar físicamente la
  máquina.

## 2. Interacción física

La GUI no reemplaza completamente la interacción con el bloque.

La prenda no se coloca desde la GUI: se coloca físicamente en la máquina o
llega mediante la línea de producción. La GUI únicamente muestra una
representación de la prenda actualmente cargada.

Algunos recursos pueden cargarse rápidamente mediante click secundario sobre
la máquina. La GUI permite además ver, agregar y retirar recursos del
inventario interno.

## 3. Panel de previsualización

Todas las GUI deben tener como elemento principal una gran pantalla central
de previsualización de la prenda:
- Forma actual.
- Color base.
- Patrones aplicados.
- Imágenes sublimadas.
- Resultado final esperado.

### Rotación del preview

El panel debe permitir inspeccionar la prenda desde seis direcciones: frente,
atrás, izquierda, derecha, arriba, abajo.

La previsualización debe actualizarse en tiempo real mientras se modifican
configuraciones antes de fijarlas.

## 4. Sistema general de configuraciones fijadas

Todas las máquinas siguen el flujo:

```
Configuración temporal → Previsualización → FIJAR → Configuración persistente
```

Una configuración fijada queda guardada en la máquina y debe poder
visualizarse, seleccionarse, desfijarse, eliminarse o reemplazarse.

## 5. Modeladora

**Función:** modificar la estructura y forma de las prendas mediante
patrones físicos reutilizables.

### Almacén de patrones

Los patrones deben estar físicamente almacenados dentro de la máquina para
poder utilizarlos. No se puede configurar un patrón que no esté presente en
el almacenamiento.

El almacén puede filtrar categorías como: Cobertura, Fit, Detalles, otros
tipos futuros.

### Lógica de patrones

El patrón determina qué transformación se realiza. La prenda determina dónde
puede aplicarse. Un mismo patrón reutilizable puede funcionar sobre
diferentes zonas compatibles: torso de una remera, manga izquierda, manga
derecha, ambas mangas, pierna izquierda o derecha, ambas piernas, medias,
cubrebrazos.

### Configuración de la Modeladora

1. Seleccionar un patrón del almacén.
2. La máquina detecta la prenda.
3. La GUI muestra las zonas compatibles.
4. Seleccionar dónde aplicarlo.
5. Seleccionar anclaje o dirección cuando corresponda.
6. Ver el resultado en el preview.
7. Presionar FIJAR.

Estructura conceptual: **Patrón + Zona + Anclaje/Configuración espacial.**

### Zonas y anclajes

Las zonas se adaptan dinámicamente a la prenda y pueden incluir izquierda,
derecha, ambas, torso/centro u otras zonas específicas.

Para patrones de longitud/cobertura pueden utilizarse dos sentidos de
anclaje: desde arriba o desde abajo.

### Panel de configuraciones fijadas

Las configuraciones deben mostrarse espacialmente alrededor del preview, no
solamente como una lista textual. Cada tarjeta debe mostrar: ícono del
patrón, zona, dirección o anclaje cuando corresponda.

## 6. Estación de Tintes

**Función:** aplicar colores y diseños cromáticos sobre una prenda que ya
posee un color base.

### Inventario de tintas CMYK

Cyan, Magenta, Yellow, Black. Las tintas pueden insertarse mediante click
secundario sobre la máquina o gestionarse desde la GUI. La GUI permite ver,
agregar y retirar tintas.

### Patrones de tinte

Los patrones deben estar almacenados físicamente dentro de la máquina. La
estación permite utilizar uno o dos patrones simultáneamente como capas de
diseño.

### Configuración de una capa

Cada capa se define como: **Patrón + Zona/Máscara + Color.**

Después de configurar y previsualizar, el jugador presiona FIJAR. Luego
puede configurar una segunda capa.

### Zonas / máscaras

Las máscaras son settings de GUI, no objetos físicos. El patrón responde qué
diseño aplicar; la máscara responde dónde aplicarlo; el color responde de
qué color.

Ejemplos posibles: prenda completa, parte superior, parte inferior, pecho,
espalda, izquierda, derecha, ambas, manga izquierda/derecha/ambas, torso,
pierna izquierda/derecha/ambas.

### Color base y cobertura completa

Cada prenda entra con un color base propio. Los patrones se aplican sobre
ese color. Un patrón de **Cobertura Completa** puede cubrir visualmente toda
la superficie y reemplazar el color base visible, por lo que no es necesario
un sistema separado para teñir la base.

## 7. Sublimadora

**Función:** aplicar imágenes sobre las prendas. Puede aplicar múltiples
imágenes durante un mismo proceso de sublimación.

### Inventarios

- Depósitos de tinta CMYK: Cyan, Magenta, Yellow y Black.
- Inventario de papel para transferencias.
- Almacén interno de imágenes.

Una imagen debe estar almacenada físicamente dentro de la máquina para poder
utilizarse en una configuración.

### Configuración de una imagen

Cada aplicación se define como: **Imagen + Setting de aplicación +
Zona/Máscara cuando corresponda.**

Luego se presiona FIJAR y puede configurarse otra imagen.

### Settings de sublimación

- **FULL PRINT**: la imagen ocupa completamente la superficie disponible de
  la zona seleccionada.
- **LOGO PEQUEÑO A LA DERECHA**: la imagen se reduce automáticamente y se
  coloca como pequeño logo o emblema en una posición derecha predefinida.
- **CENTRO**: la imagen se centra automáticamente en la zona
  correspondiente.

Estos modos son settings de la máquina, no patrones físicos, porque
determinan cómo se aplica una imagen.

### Múltiples imágenes

No existe un límite arbitrario de imágenes. El jugador puede fijar múltiples
configuraciones y todas forman parte del resultado final. Como pueden
superponerse, debe existir un orden de capas. La GUI debería permitir
visualizar claramente ese orden y, si es posible, modificarlo.

### Consumo de recursos

La cantidad de imágenes determina directamente el costo de producción.

- **Papel**: más imágenes = más transferencias/consumo de papel.
- **Tinta**: depende de cantidad de imágenes, colores presentes en las
  imágenes, setting de aplicación, y cobertura o tamaño de impresión.

Consumo relativo sugerido:

| Setting | Consumo de tinta |
|---|---|
| Logo pequeño | Bajo |
| Centro | Medio |
| Full Print | Alto |

El consumo funciona como límite natural, evitando restringir
artificialmente la creatividad.

## 8. Tiempos de procesamiento

| Máquina | Tiempo inicial sugerido |
|---|---|
| Modeladora | 8–10 segundos |
| Estación de Tintes | 12–15 segundos |
| Sublimadora | 20 segundos |

Estos valores son iniciales y deben ajustarse mediante pruebas de gameplay.
Los distintos tiempos generan ritmo y posibles cuellos de botella naturales
en una línea industrial. (La Sublimadora actual ya usa `TICKS_PRENSADO=400`
= 20s — coincide.)

## 9. Easter eggs e interacción peligrosa

Como detalle inmersivo, las máquinas en funcionamiento pueden reaccionar
negativamente si el jugador intenta manipularlas físicamente:
- Sublimadora: daño por calor o quemadura.
- Modeladora: daño o corte.

La intención es reforzar diegéticamente que la maquinaria industrial en
funcionamiento no debe manipularse. (Baja prioridad — flavor, no bloquea el
resto.)

## 10. Resumen del lenguaje común

| Máquina | Configuración | Función |
|---|---|---|
| Modeladora | Patrón + Zona + Anclaje → FIJAR | Cambia la forma. |
| Estación de Tintes | Patrón + Máscara/Zona + Color → FIJAR | Aplica diseño cromático. |
| Sublimadora | Imagen + Setting + Zona cuando corresponda → FIJAR | Aplica identidad visual. |

## 11. Flujo industrial completo

```
PRENDA BASE → MODELADORA (forma, cobertura, fit, detalles)
    → ESTACIÓN DE TINTES (color, patrones, máscaras)
    → SUBLIMADORA (imágenes, logos, full prints)
    → PRENDA FINAL
```

**Principio fundamental:** los objetos físicos determinan qué recursos o
diseños están disponibles. Los settings de la GUI determinan cómo y dónde se
aplican. Las configuraciones se fijan antes de encender la máquina. Una vez
encendida, la máquina deja de ser una herramienta de diseño y se convierte
en una máquina de producción automática.

---

## Nota de integración (2026-09-09)

Este documento llegó DESPUÉS de que ya existiera una primera implementación
de la Mesa de Modelado (`com.femclothes.modelado`, ver `FEMCLOTHES.md`) con
un modelo de interacción distinto: sin estado on/off, GUI siempre abierta,
aplicación instantánea por click derecho con la prenda en mano (el gesto de
`docs/MAQUINAS.md` §3). Este informe pide algo bastante más grande:

- Estado ON/OFF por máquina (config solo editable apagada).
- Preview 3D real de la prenda, rotable en 6 direcciones, actualizado en
  vivo — no existe nada parecido todavía en el mod.
- Sistema de configuraciones FIJADAS (temporal → preview → FIJAR →
  persistente), con tarjetas dispuestas espacialmente alrededor del
  preview, no una lista.
- Multi-capa (Tintes: 1-2 patrones; Sublimadora: N imágenes con orden/
  reordenamiento).
- Detección automática de prenda + auto-arranque al recibir una prenda
  compatible en modo Producción.

Esto no es una extensión chica de lo ya construido — es un modelo de
interacción distinto que conviene planificar de cero antes de tocar más
código, en vez de ir parchando el `ModeladoBlockEntity`/`ModeladoScreenHandler`
actuales pieza por pieza.
