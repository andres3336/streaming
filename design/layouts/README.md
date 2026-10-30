# Layouts de Streaming

Wireframes neutros y editables de la app, en formato draw.io. Un archivo por
pantalla; dentro de cada archivo hay una página de leyenda, la pantalla en
`1280×720`, el contenido completo con scroll y las variantes/estados
secundarios.

```text
INICIO.drawio        Inicio + hero + filas + variantes del hero
TV.drawio            En vivo + Canales + EPG en dos columnas + estados
PELICULAS.drawio     Parrilla + scroll + ficha de película + estados
SERIES.drawio        Parrilla + scroll + ficha de serie/episodios + estados
BUSCAR.drawio        Vacío + resultados + sin resultados + estados
AJUSTES.drawio       Viewport + scroll completo + estados
REPRODUCTOR.drawio   Player completo/compacto + overlays + salir/red
```

## Cómo editar

- Abrir el `.drawio` en [draw.io](https://app.diagrams.net/) o en la app de
  escritorio.
- Cada página tiene cuatro capas:
  - **Wireframe**: cajas, textos y placeholders visibles.
  - **Anotaciones**: notas de página.
  - **Etiquetas de elemento**: IDs azules `[elemento]`; oculta por defecto.
  - **Medidas**: reservada para cotas; oculta.
- El marco de `1280×720` representa la pantalla del TV. La línea roja
  discontinua es el borde inferior; lo que queda debajo es el contenido que
  aparece al hacer scroll.
- Las cajas grises discontinuas son imágenes, pósteres, logos o vídeo.
- El borde negro grueso indica foco; el relleno gris indica seleccionado.
- Las formas con borde discontinuo son condicionales.

## Volver a pasarlo al agente

Cada bloque lógico es un grupo con un `id` estable. Los grupos principales
también llevan atributos de datos que draw.io muestra en **Editar datos**:

- `element`: nombre lógico, por ejemplo `movies.hero`.
- `source`: archivo/línea de referencia en el código Compose.

Al devolver el XML, basta con conservar esos atributos. Si se añade un bloque
nuevo, conviene darle un `element` descriptivo o una nota con el nombre que
debería tener en la app. El agente puede comparar geometría, textos y IDs para
traducir los cambios a `MainActivity.kt` y a los componentes de `ui/`.

Los archivos están guardados como `mxGraphModel` sin comprimir: se pueden leer,
comparar y editar sin herramientas especiales.
