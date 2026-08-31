# Recordatorio de turnos — FKT Lomas

> **Superado**: la funcionalidad de este proyecto (recordatorios automáticos
> por email/SMS/WhatsApp, mensaje puntual, canal preferido por paciente) se
> integró directamente en `fkt-turnos-backend` (job programado,
> `RecordatorioService`) con una pestaña "Recordatorios" nueva en el panel de
> admin de `fkt-turnos-frontend`, para que la secretaria tenga un solo
> programa (agenda + recordatorios) en vez de dos apps separadas. Este
> proyecto queda como referencia — la lógica de envío por Twilio y el
> armado de la plantilla del mensaje se portaron casi textual desde acá — y
> los arreglos de empaquetado (`AppPaths`, `Launcher`, jpackage para Mac)
> siguen siendo válidos si en algún momento hace falta un recordatorio
> desacoplado del backend, pero no es la app que se sigue desarrollando.

App de escritorio que centraliza el envío de recordatorios automáticos de
turno a los pacientes de FKT Lomas, por el canal que cada uno prefiera
(email, SMS o WhatsApp), y permite mandarles un mensaje puntual fuera del
recordatorio automático.

## Arquitectura: dos bases, no una

Esta app **nunca escribe en la base del sistema de turnos real**. Usa dos
bases separadas:

- **Origen** (el sistema de turnos de FKT Lomas): conexión de **solo
  lectura**. Esta app únicamente hace `SELECT` ahí — nunca `INSERT`,
  `UPDATE` ni `DELETE` — así que no hay riesgo de romper ni trabar un
  sistema del que depende el negocio.
- **Local** (propia de esta app, embebida con **H2**): un espejo de
  pacientes y turnos, más el canal de notificación preferido de cada
  paciente (que vive *únicamente* acá) y el historial de recordatorios
  enviados. Se crea sola en `./data/recordatorios` la primera vez que
  arranca la app — no requiere instalar ni configurar nada aparte.

Un proceso de sincronización (`SincronizadorService`) copia periódicamente
pacientes y turnos desde el origen hacia la base local: agrega los nuevos,
actualiza nombre/teléfono/email/horario si cambiaron, y da de baja lógica
(no borra) lo que dejó de aparecer en el origen — pero **nunca toca el
canal preferido**, que es lo único que esta app decide por su cuenta.

```
Sistema de turnos (solo lectura)  →  Sincronizador  →  Base local (H2)
                                                          ├─ pacientes (+ canal)
                                                          ├─ turnos
                                                          └─ recordatorios
```

## Patrones de diseño usados

- **Repository** (`TurnoRepository`, `PacienteRepository`,
  `RecordatorioRepository` sobre la base local; `OrigenRepository` sobre la
  base de origen, solo lectura): aísla el acceso a datos del resto del
  sistema.
- **Strategy** (`Notificador`): cada canal (`EmailNotificador`,
  `SmsNotificador`, `WhatsAppNotificador`) implementa la misma interfaz.
- **Factory** (`NotificadorFactory`): dado el canal preferido del paciente,
  entrega la estrategia correspondiente. Un canal sin configurar
  simplemente no se registra, sin romper los demás.
- **Scheduler** (`SchedulerService`): job periódico que dispara todo sin
  intervención manual.

## Flujo

1. `SchedulerService` corre cada `recordatorio.intervaloMinutos` minutos
   (y también al tocar "Sincronizar y chequear ahora" en la GUI).
2. `RecordatorioService` primero sincroniza (`SincronizadorService`) y
   después le pide a `TurnoRepository` (base local) los turnos dentro de la
   ventana de anticipación configurada que todavía no tengan un
   recordatorio enviado.
3. Para cada turno, mira el canal preferido del paciente (base local) y le
   pide a `NotificadorFactory` el `Notificador` correspondiente.
4. El `Notificador` envía el mensaje por el servicio externo que
   corresponda (SMTP para email, Twilio para SMS/WhatsApp).
5. El resultado se guarda en la base local, con reintentos automáticos
   hasta `MAX_INTENTOS`.

## Interfaz gráfica (Windows)

`RecordatorioApp` es la app de escritorio (JavaFX) pensada para instalar en
las PCs de FKT Lomas. Tiene cuatro secciones:

- **Recordatorios**: turnos próximos y estado de cada recordatorio
  (pendiente/enviado/fallido), con reintento manual y chequeo/sincronización
  a demanda.
- **Pacientes**: listado con el canal preferido editable (se guarda en la
  base local, nunca en el sistema de turnos), y un botón para mandarle un
  mensaje libre a un paciente puntual.
- **Mensaje**: el texto del recordatorio automático, editable con
  placeholders (`{paciente}`, `{profesional}`, `{especialidad}`, `{fecha}`,
  `{hora}`, `{consultorio}`) y vista previa en vivo.
- **Configuración**: datos de conexión al sistema de turnos (solo lectura),
  SMTP, Twilio, los tiempos del recordatorio, inicio automático con Windows,
  y botones para probar la conexión y para mandar un mensaje de prueba con
  la configuración actual (sin necesidad de guardarla antes).

Al cerrar la ventana, la app sigue corriendo en la bandeja del sistema
(no se corta el envío de recordatorios). Solo puede haber una instancia
corriendo a la vez (`InstanceLock`) — si se intenta abrir de nuevo, avisa
que ya está corriendo en vez de arrancar un segundo scheduler en paralelo.

### Robustez

- **Log a archivo** (`./logs/app.log`, con rotación): necesario porque la
  GUI no tiene consola visible al abrirse con doble click.
- **Respaldo diario de la base local** (`./data/backups/`, últimos 7 días):
  usa el comando `BACKUP` de H2, que genera una copia consistente aunque la
  base esté en uso. Es la única fuente de verdad del canal preferido de
  cada paciente.
- **Inicio automático con Windows** (checkbox en Configuración): agrega/quita
  una entrada en `HKCU\...\Run`, sin necesitar permisos de administrador.
  Solo funciona con la app ya instalada (jpackage) — en modo desarrollo el
  checkbox queda deshabilitado, porque apuntaría a `java.exe` en vez del
  ejecutable final. **No lo pude probar en una PC con Windows real** — antes
  de confiar en él, activalo una vez y confirmá que la entrada aparezca en
  el Administrador de tareas → pestaña Inicio.
- **Aviso en la bandeja** si aparecen recordatorios en estado FALLIDO nuevos
  desde el último chequeo (`TrayIconManager.mostrarNotificacion`), para
  enterarse sin tener que abrir la app.
- **Confirmación al salir** desde el menú de la bandeja: cerrar corta el
  envío de recordatorios hasta reabrir la app, así que pregunta antes.
- **Mensajes de error traducidos** (`MensajesAmigables`): los más comunes de
  JDBC/SMTP/Twilio se muestran en texto entendible en vez del mensaje crudo
  de la excepción.
- **Reintento en lote y buscador** en las tablas de Recordatorios y
  Pacientes (selección múltiple + filtro por nombre).
- **Validación** de los campos numéricos de Configuración antes de guardar.

### Probarla en desarrollo

```bash
mvn javafx:run
```

### Empaquetarla como app de escritorio (Windows y Mac)

Requiere JDK 17+ instalado en una PC con el sistema operativo de destino
(jpackage no puede generar un instalador de Windows desde Mac, ni viceversa).
En Windows además hace falta [WiX Toolset](https://wixtoolset.org/).

`mvn clean package` deja en `target/app-libs/` exactamente lo que necesita
jpackage: el jar liviano de la app (`recordatorio-turnos.jar`) más cada
dependencia como jar suelto — **no** `target/` a secas, que además tiene
`target/classes`, `target/standalone/recordatorio-turnos.jar` (el jar con
todas las dependencias fusionadas que usa el modo headless, ver más abajo) y
otras carpetas de Maven: jpackage empaqueta *todo* lo que encuentra en el
directorio de entrada, así que si el jar fusionado quedara ahí también,
JavaFX terminaría con dos copias de sí mismo en el classpath y fallaría en
tiempo de ejecución con "faltan los componentes de JavaFX runtime". Por el
mismo motivo, la clase principal del empaquetado es `Launcher` (no
`RecordatorioApp` directamente): cuando la clase `main()` extiende
`javafx.application.Application` y se lanza por classpath (que es como la
arma jpackage acá, sin módulos), el propio launcher de la JVM rechaza el
arranque con ese mismo error aunque JavaFX esté bien presente — hay que
indireccionar el `main()` a través de una clase que no extienda
`Application` (ver [`Launcher.java`](src/main/java/ar/edu/uade/recordatorios/gui/Launcher.java)).

El `pom.xml` ya detecta el sistema operativo y arquitectura al compilar
(perfiles `javafx-*`), así que `mvn clean package` corrido en cada PC arma
automáticamente el jar con las librerías nativas correctas para ese SO.

**Windows** (en PowerShell/cmd, en una PC con Windows):

```bat
mvn clean package
jpackage --type exe --name "Recordatorio FKT Lomas" --app-version 1.0.0 ^
  --vendor "FKT Lomas" --input target\app-libs --main-jar recordatorio-turnos.jar ^
  --main-class ar.edu.uade.recordatorios.gui.Launcher ^
  --icon windows\icon.ico --win-shortcut --win-menu --dest dist
```

**Mac** (en una Mac; genera un `.dmg` con Apple Silicon o Intel según en cuál
se corra `mvn clean package`):

```bash
mvn clean package
jpackage --type dmg --name "Recordatorio FKT Lomas" --app-version 1.0.0 \
  --vendor "FKT Lomas" --input target/app-libs --main-jar recordatorio-turnos.jar \
  --main-class ar.edu.uade.recordatorios.gui.Launcher \
  --dest dist
```

(Falta un ícono `.icns` propio para Mac — sin `--icon` jpackage usa uno
genérico de Java. `windows/icon.ico` no sirve tal cual, Mac necesita el
formato `.icns`.)

`windows/icon.ico` ya está generado (círculo teal + campana, para no depender
del logo real de FKT Lomas ni de temas de derechos de autor). Si hace falta
regenerarlo:

```bash
mvn -q compile
java -cp target/classes ar.edu.uade.recordatorios.tools.GenerarIcono
```

El instalador/imagen de disco queda en `dist/`.

Tanto en Windows como en Mac, la app guarda su configuración, su base local
y sus logs en la carpeta estándar de datos de aplicaciones del usuario
actual (`%APPDATA%\RecordatorioFKTLomas` / `~/Library/Application
Support/RecordatorioFKTLomas`) — no en la carpeta donde se instaló la app,
porque ni Windows ni Mac garantizan cuál va a ser el directorio de trabajo
del proceso al abrirla con doble click, y en Windows el directorio de
instalación puede ni siquiera ser escribible sin privilegios de
administrador (ver [`AppPaths.java`](src/main/java/ar/edu/uade/recordatorios/config/AppPaths.java)).

## Instalación headless (Linux, sin interfaz gráfica)

Para un servidor sin pantalla, `Main` es el equivalente headless: mismo
`RecordatorioService`, sin GUI. `install.sh` lo instala como servicio:

```bash
./install.sh                        # instala en /opt/recordatorio-turnos
./install.sh /ruta/alternativa       # o en el directorio que elijas
```

Genera y habilita un servicio systemd si está disponible. Completar
`<directorio_instalación>/config/application.properties` (a partir de
`config/application.properties.example`) antes de arrancarlo.

## El sistema de origen: fkt-turnos-backend

`origen.db.*` apunta al backend real de turnos de FKT Lomas
([fkt-turnos-backend](https://github.com/LauCaren/fkt-lomas-turnos) —
Spring Boot + PostgreSQL). `JdbcOrigenRepository` y `sql/schema-origen.sql`
ya están ajustados a su esquema real (`db/migration/V1__init.sql` en ese
repo), no a uno de referencia:

- Pacientes y personal comparten la tabla `usuarios`, distinguidos por
  `role` (`PACIENTE` / `ADMIN`) — solo se sincronizan los `PACIENTE`.
- `turnos` también contiene los horarios libres (`estado = 'DISPONIBLE'`);
  la sincronización solo trae los reservados (`estado = 'RESERVADO'`).
- No hay concepto de "consultorio" (una sola sede) — el placeholder
  `{consultorio}` del mensaje queda vacío; la dirección va como texto fijo.

Si ese esquema cambia (nueva migración de Flyway), hay que actualizar el
SQL en `JdbcOrigenRepository` — es el único lugar del proyecto que lo
necesita.

Para `origen.db.usuario` en producción, crear un usuario de Postgres con
permisos de **solo SELECT** (no reusar el usuario de la app):

```sql
CREATE USER recordatorios_readonly WITH PASSWORD 'elegir-una-buena';
GRANT CONNECT ON DATABASE fkt_turnos TO recordatorios_readonly;
GRANT USAGE ON SCHEMA public TO recordatorios_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO recordatorios_readonly;
```

Refuerzo a nivel de motor de base de datos, no solo de código: aunque haya
un bug, ese usuario físicamente no puede escribir nada.

## Tests

```bash
mvn test
```

`RecordatorioServiceTest` (Mockito, mockea los repositorios y el
`Notificador`) cubre el envío, los reintentos, que no se dupliquen
recordatorios, y que un canal sin configurar no rompa el resto del proceso.
`SincronizadorServiceTest` corre contra una base H2 en memoria real (no
mockeada, porque lo que hay que validar ahí es el comportamiento del SQL) y
cubre que el canal preferido sobrevive a una resincronización y que las
bajas quedan marcadas como inactivas.

## Posibles mejoras

- Reemplazar `ScheduledExecutorService` por **Quartz Scheduler** si se
  necesita persistencia de jobs y recuperación ante caídas del proceso.
- Cifrar o proteger con permisos de archivo del SO la base local
  (`./data/recordatorios.mv.db`), ya que contiene datos de pacientes.
- Firmar el instalador de Windows (evita la advertencia de "editor
  desconocido" de SmartScreen).
