# Guia de Defensa - MotorBD

## Resumen para exponer

MotorBD es un gestor de bases de datos simple desarrollado en Java. Permite manejar espacios de almacenamiento parecidos a tablas, usando comandos tipo SQL desde una consola. Internamente usa un arbol AVL propio para indexar la clave primaria de cada espacio, lo que mejora busquedas, inserciones y eliminaciones por clave.

## Que problema resuelve

Permite almacenar registros, consultarlos y persistirlos en archivos, demostrando como una estructura de datos autobalanceada puede servir como indice de un gestor de datos.

## Punto de entrada

- `src/cli/REPL.java`
- Metodo principal: `main(String[] args)`

Desde ahi se crea un `Motor`, se reciben comandos y se imprimen resultados.

## Nucleo del gestor

- `src/motor/parser/Motor.java`

Esta clase recibe comandos como `CREATE SPACE`, `INSERT`, `SELECT`, `UPDATE`, `DELETE`, `TREE`, `SAVE` y `HELP`. Cada comando se convierte en llamadas a las clases de almacenamiento, catalogo y persistencia.

## Gestion de esquemas

Ubicacion:

- `src/motor/catalogo/Esquema.java`
- `src/motor/catalogo/Campo.java`
- `src/motor/catalogo/TipoDato.java`
- `src/motor/catalogo/Catalogo.java`

Demostracion:

```sql
CREATE SPACE alumnos (id ENTERO PK, nombre TEXTO, nota REAL, activo BOOLEAN);
DESC alumnos;
SHOW SPACES;
```

Tambien se permiten espacios libres:

```sql
CREATE SPACE notas;
INSERT INTO notas (id, materia, nota) VALUES ('n001', 'Calculo', 4.2);
```

## CRUD

Ubicacion principal:

- `src/motor/parser/Motor.java`
- `src/motor/almacenamiento/Espacio.java`
- `src/motor/almacenamiento/Registro.java`

Demostracion:

```sql
INSERT INTO alumnos VALUES (1, 'Ana', 4.5, true);
SELECT * FROM alumnos WHERE id = 1;
SELECT * FROM alumnos WHERE nota >= 4;
UPDATE alumnos SET nota=4.8 WHERE id = 1;
DELETE FROM alumnos WHERE id = 1;
```

## Busqueda por rango

Por clave primaria usa el AVL:

```sql
SELECT * FROM alumnos WHERE id BETWEEN 1 AND 10;
```

Por campos no indexados funciona con escaneo lineal:

```sql
SELECT * FROM alumnos WHERE nota BETWEEN 3.5 AND 5.0;
```

## Arbol AVL

Ubicacion:

- `src/motor/arbol/ArbolAVL.java`
- `src/motor/arbol/NodoAVL.java`

Metodos importantes:

- `insertar()`
- `buscar()`
- `eliminar()`
- `inorden()`
- `rango()`
- `rotDer()`
- `rotIzq()`
- `balancear()`

Como se balancea:

El arbol calcula la altura de cada nodo y el factor de balance. Si el factor sale de `[-1, 1]`, aplica rotaciones simples o dobles para recuperar el balance.

Demostracion:

```sql
TREE alumnos;
```

## Conexion entre AVL y registros

Ubicacion:

- `src/motor/almacenamiento/Espacio.java`

Cada `Espacio` tiene:

```java
private ArbolAVL<String, Registro> indice;
```

La clave del AVL es la clave primaria del registro. El valor es el objeto `Registro`.

## Persistencia

Ubicacion:

- `src/motor/persistencia/GestorPersistencia.java`

Archivos:

- `data/esquemas/<espacio>.schema`
- `data/espacios/<espacio>.json`

Como se guarda:

```sql
SAVE ALL;
```

Como se recupera:

Al crear `new Motor("data")`, el constructor llama a `persistencia.cargarTodo(...)`. Este metodo lee los archivos y reconstruye el indice AVL insertando los registros cargados.

## Consistencia basica

El guardado usa archivos temporales `.tmp` y reemplaza el archivo final solo despues de escribir correctamente. Ademas:

- No se permite actualizar la clave primaria, porque rompería el indice AVL.
- Se validan campos inexistentes en esquemas fijos.
- Los errores se devuelven como mensajes comprensibles, sin cerrar abruptamente el programa.

## Secuencia de demostracion en vivo

Abrir:

```powershell
.\ejecutar.bat --data data_demo
```

Comandos:

```sql
HELP;
CREATE SPACE alumnos (id ENTERO PK, nombre TEXTO, nota REAL, activo BOOLEAN);
INSERT INTO alumnos VALUES (3, 'Laura', 4.1, true);
INSERT INTO alumnos VALUES (1, 'Ana', 4.8, true);
INSERT INTO alumnos VALUES (2, 'Carlos', 3.7, false);
SELECT * FROM alumnos;
SELECT * FROM alumnos WHERE id = 2;
SELECT * FROM alumnos WHERE id BETWEEN 1 AND 3;
SELECT * FROM alumnos WHERE nota BETWEEN 4 AND 5;
UPDATE alumnos SET nota=4.0 WHERE id = 2;
DELETE FROM alumnos WHERE activo = false;
TREE alumnos;
SAVE ALL;
EXIT;
```

Volver a abrir:

```powershell
.\ejecutar.bat --data data_demo
```

Demostrar persistencia:

```sql
SHOW SPACES;
SELECT * FROM alumnos;
EXIT;
```

## Requisitos y ubicacion rapida

| Requisito | Ubicacion |
| --------- | --------- |
| REPL | `src/cli/REPL.java` |
| Parser/comandos | `src/motor/parser/Motor.java` |
| Esquemas | `src/motor/catalogo/*` |
| CRUD | `src/motor/almacenamiento/Espacio.java`, `Registro.java` |
| AVL propio | `src/motor/arbol/ArbolAVL.java` |
| Persistencia | `src/motor/persistencia/GestorPersistencia.java` |
| Pruebas | `src/test/TestMotor.java` |

## Interfaz grafica implementada

Archivo principal de la GUI:

- `src/gui/DatabaseManagerGUI.java`

Libreria usada:

- Java Swing.

Pestanas o pantallas:

- `Espacios`: crea, lista y elimina espacios.
- `Registros`: inserta, lista, actualiza y elimina registros.
- `Busquedas`: busca por clave, igualdad o rango.
- `Indice / Arbol`: muestra recorrido inorder, altura y estructura textual del AVL.
- `Ayuda`: explica tareas basicas y comandos disponibles.

Operaciones soportadas:

- Crear espacios con esquema.
- Crear espacios libres.
- Eliminar espacios con confirmacion.
- Insertar registros en esquemas fijos.
- Listar registros.
- Actualizar registros seleccionados.
- Eliminar registros seleccionados.
- Buscar registros por clave, condicion o rango.
- Mostrar informacion del indice AVL.

Relacion con el motor del gestor:

La GUI no reimplementa el motor. Cada boton construye comandos y llama a `Motor.ejecutar(...)`, el mismo punto de entrada usado por el REPL. Por eso la consola y la GUI demuestran el mismo sistema.

Como demuestra los requisitos:

- Permite ejecutar CRUD desde botones y formularios.
- Muestra los espacios y sus esquemas.
- Muestra el arbol AVL y las claves en orden.
- Usa la persistencia existente del motor.
- Conserva la consola obligatoria.

Como ejecutarla:

```powershell
.\gui.bat
```

O con carpeta de datos separada:

```powershell
.\gui.bat --data data_demo_gui
```

Limitaciones:

- La visualizacion del arbol es textual, no grafica.
- La pestana Registros esta orientada a espacios con esquema fijo.
- No implementa concurrencia ni transacciones avanzadas.
