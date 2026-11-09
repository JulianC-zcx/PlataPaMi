<div align="center">

# 💸 PlataPaMi

**Tu plata, clara y en tu bolsillo.**
App Android para llevar el control de tus entradas y gastos del mes, ver a dónde se te va la plata en una dona y recibir tu extracto mensual en PDF directo al correo.

</div>

---

## 🧠 ¿De qué va esto?

Yo quería una app sencilla para saber **cuánta plata me queda en el mes**, sin hojas de Excel ni apps llenas de cosas que nunca uso. Así nació **PlataPaMi**: registras lo que entra y lo que sale, la app te muestra el presupuesto del mes, te avisa antes de que te pases y al final del mes te manda un extracto con diseño de app financiera (tipo Nu o Nequi).

Es un proyecto académico de **Desarrollo de Aplicaciones Móviles**, hecho con la idea de que funcione como una app de verdad: base de datos real, correos reales, seguridad y tratamiento de datos conforme a la ley colombiana.

---

## ✨ Qué puedes hacer con la app

| | Funcionalidad |
|---|---|
| 🔐 | **Registro e inicio de sesión** con nombre, documento, correo y contraseña |
| 📜 | **Términos y Condiciones + Habeas Data** (Ley 1581 de 2012): hay que leerlos y aceptarlos sí o sí para poder registrarse |
| 🔑 | **Recuperar contraseña** con un enlace directo al correo (de un solo uso y con vencimiento) |
| 📅 | **Presupuesto mensual** con el rango de fechas del mes (`OCT 01 - OCT 31 / 2026`) que se actualiza solo y puedes navegar entre meses |
| ➕ | **Registrar entradas y gastos** con nombre, detalle, categoría, valor, fecha y foto del comprobante (cámara o galería) |
| 🍩 | **Gráfico circular** de en qué se te va la plata + botón **Desglosar** con porcentajes y totales |
| 📋 | **Lista de movimientos** con filtros (Todos / Entradas / Gastos) y buscador; puedes editar o eliminar |
| 🔔 | **Alertas de presupuesto**: aviso al llegar al **80 %** de tus entradas del mes y otro si te pasas del **100 %** (en la campana y por correo) |
| 📄 | **Extracto mensual en PDF**, con diseño propio, enviado a tu correo y **protegido con tu número de documento como contraseña** |
| 📴 | **Modo sin conexión (solo lectura)**: si no hay internet, ves la última copia guardada del mes |
| 🗑️ | **Eliminar mi cuenta** y todos mis datos desde el perfil (derecho de supresión) |

---

## 🎨 Diseño

El diseño sale de los mockups del proyecto y se mantiene en toda la app y también en el PDF del extracto.

- **Tipografía:** Poppins (Light, Regular y Bold)
- **Colores principales:**

| Color | Hex | Uso |
|---|---|---|
| Cian | `#9CF6FF` | Cabeceras y fondos principales |
| Verde | `#00C060` | Entradas |
| Rosa/Rojo | `#C93567` / `#FF5757` | Gastos y alertas |
| Naranja | `#F4B86C` | Saldo disponible en la dona |
| Teal | `#0099B8` | Botones y acentos |

---

## 🧱 Tecnologías

**App (Android)**
- Kotlin + **Jetpack Compose** (Material 3) + Navigation Compose
- Arquitectura con `ViewModel` y estado reactivo
- OkHttp para hablar con la API, Coil para cargar los comprobantes
- `minSdk 26` (Android 8.0 o superior)

**Backend**
- **PHP 8.1+** sin framework, organizado en clases (`Auth`, `Movements`, `Statements`, `Mail`…)
- **MySQL** con consultas preparadas (PDO)
- **TCPDF** para generar el extracto en PDF cifrado (AES) con la fuente Poppins
- **PHPMailer** para los correos por SMTP
- Tarea programada (cron) para enviar los extractos el día 1 de cada mes

---

## 🗂️ Estructura del proyecto

```
PlataPaMi/
├── app/                      ← App Android (Kotlin + Compose)
│   └── src/main/java/com/platapami/app/
│       ├── MainActivity.kt        navegación y barra inferior
│       ├── AppViewModel.kt        estado de la app
│       ├── Api.kt                 cliente HTTP y sesión
│       ├── AuthScreens.kt         login, registro, recuperar contraseña
│       ├── Legal.kt               Términos y Habeas Data
│       ├── HomeScreens.kt         inicio y gráfico
│       ├── MovimientosScreens.kt  lista y formulario de movimientos
│       ├── OtherScreens.kt        extracto, notificaciones y perfil
│       ├── Components.kt          componentes reutilizables (la dona, botones…)
│       └── Theme.kt               colores y tipografía
└── backend/                  ← API en PHP + MySQL
    ├── public/                    lo único que se publica (api/, reset.php, uploads/)
    ├── src/                       lógica del servidor
    ├── database/schema.sql        estructura de la base de datos
    ├── cron/monthly_statements.php
    ├── fonts/                     Poppins para el PDF
    └── .env.example               configuración (se copia a .env)
```

---

## 🚀 Cómo correrlo (paso a paso)

Esta guía está pensada para **Windows con XAMPP**, que es como lo desarrollé yo. Si usas otro sistema, los pasos son los mismos; solo cambian las rutas.

**Lo que necesitas instalado antes de empezar:**

| Programa | Para qué | Dónde se descarga |
|---|---|---|
| **XAMPP** (con PHP 8.1 o superior) | Apache + MySQL + phpMyAdmin | https://www.apachefriends.org |
| **Composer** | Instalar las librerías del backend (PDF y correos) | https://getcomposer.org/download/ |
| **Android Studio** | Abrir y correr la app | https://developer.android.com/studio |

> Para saber qué versión de PHP trae tu XAMPP, abre una terminal y ejecuta `C:\xampp\php\php.exe -v`.

---

### 🅰️ Parte 1: Backend (PHP + MySQL)

#### Paso 1. Poner el backend en XAMPP

Con el proyecto ya en tu computador, copia la carpeta **`backend`** dentro de `C:\xampp\htdocs\`. Quedaría así:

```
C:\xampp\htdocs\backend\
```

> 💡 Acuérdate del nombre de esa carpeta (`backend`): va a hacer parte de la URL en los pasos siguientes. Si le pones otro nombre, cámbialo también en las URLs.

#### Paso 2. Activar las extensiones de PHP que se necesitan

Composer y el backend necesitan que PHP tenga activas las extensiones `gd`, `zip`, `mbstring`, `openssl`, `pdo_mysql` y `curl`.

1. Abre el archivo `C:\xampp\php\php.ini`. En el Explorador de Windows se ve como **`php`** con tipo *"Opciones de configuración"* (Windows le oculta la extensión). También lo abres desde una terminal:
   ```powershell
   notepad C:\xampp\php\php.ini
   ```
2. Con `Ctrl + F` busca cada una de estas líneas y **quítales el `;` del inicio** (el `;` significa que está desactivada). Por ejemplo `;extension=gd` pasa a ser `extension=gd`:
   ```
   extension=curl
   extension=gd
   extension=mbstring
   extension=openssl
   extension=pdo_mysql
   extension=zip
   ```
3. Guarda el archivo (`Ctrl + S`).
4. Abre el **panel de XAMPP** y reinicia Apache (**Stop** y luego **Start**).
5. Comprueba que quedaron activas (en PowerShell):
   ```powershell
   C:\xampp\php\php.exe -m | findstr /i "curl gd mbstring openssl pdo_mysql zip"
   ```
   Deben salir las seis. Si falta alguna, revisa que la línea no tenga el `;` y que editaste el `php.ini` de `C:\xampp\php\`.

#### Paso 3. Instalar Composer y las librerías

1. Descarga y ejecuta `Composer-Setup.exe`. Cuando te pregunte por el PHP, elige el de XAMPP: `C:\xampp\php\php.exe`.
2. **Cierra y vuelve a abrir** PowerShell (si no, no reconoce el comando).
3. Comprueba que quedó instalado:
   ```powershell
   composer --version
   ```
4. Entra a la carpeta del backend e instala:
   ```powershell
   cd C:\xampp\htdocs\backend
   composer install
   ```
   Al terminar debe aparecer una carpeta **`vendor`** dentro de `backend` (ahí quedan PHPMailer y TCPDF).

#### Paso 4. Crear la base de datos en phpMyAdmin

1. En el panel de XAMPP pulsa **Start** en **Apache** y en **MySQL** (los dos en verde).
2. Abre en el navegador: http://localhost/phpmyadmin
3. En la barra de arriba haz clic en la pestaña **Importar**.
4. En **"Archivo a importar"** pulsa **Seleccionar archivo** y elige:
   ```
   C:\xampp\htdocs\backend\database\schema.sql
   ```
   *(No hace falta crear la base antes: el mismo archivo la crea con el nombre `platapami`).*
5. Baja hasta el final de la página y pulsa **Importar** (o **Continuar**).
6. Si salió bien, aparece un mensaje verde de éxito y en el menú de la izquierda se ve la base **`platapami`**. Al abrirla debes ver **6 tablas**:
   `usuarios`, `sesiones`, `reset_tokens`, `movimientos`, `notificaciones` y `extractos_enviados`.

> 🔐 En XAMPP el usuario de MySQL es `root` y **sin contraseña**. Eso es solo para desarrollo en tu PC. En un servidor real crea un usuario propio con contraseña y úsalo en el `.env`.

#### Paso 5. Crear y llenar el archivo `.env`

El `.env` guarda tus datos privados (clave de cifrado, base de datos, correo). **Nunca lo compartas ni lo publiques.**

1. Genera la **clave de cifrado** (`APP_KEY`). En PowerShell:
   ```powershell
   C:\xampp\php\php.exe -r "echo base64_encode(random_bytes(32));"
   ```
   Te imprime una línea de caracteres. **Cópiala y guárdala** en un lugar seguro: con esa clave se cifra el documento de cada usuario, y si la pierdes no se podrán abrir los extractos de los usuarios que ya existan.
2. Crea el `.env` a partir del ejemplo:
   ```powershell
   cd C:\xampp\htdocs\backend
   copy .env.example .env
   notepad .env
   ```
3. Completa estas líneas (lo demás puedes dejarlo igual):

   ```ini
   APP_URL=http://localhost/backend/public
   APP_KEY=PEGA_AQUI_LA_CLAVE_QUE_GENERASTE

   DB_HOST=127.0.0.1
   DB_PORT=3306
   DB_NAME=platapami
   DB_USER=root
   DB_PASS=

   MAIL_DRIVER=file
   APP_DEBUG=1
   ```

   | Variable | Qué poner |
   |---|---|
   | `APP_URL` | `http://localhost/` + el nombre de tu carpeta + `/public`. Se usa para armar el enlace del correo de recuperar contraseña. |
   | `APP_KEY` | La clave del punto 1. |
   | `DB_USER` / `DB_PASS` | En XAMPP: `root` y la contraseña vacía. |
   | `MAIL_DRIVER` | `file` para **probar sin enviar correos** (los guarda como archivos). Para correos de verdad, mira el **Paso 6**. |
   | `APP_DEBUG` | `1` mientras desarrollas (muestra más detalle de los errores). Ponlo en `0` en producción. |

4. Guarda el archivo.

#### Paso 6. Configurar el envío de correos

La app manda correos para: **bienvenida**, **restablecer contraseña**, **alertas de presupuesto (80 % y 100 %)** y **extracto mensual en PDF**. Tienes tres opciones:

**Opción A: Probar sin SMTP (la más fácil para empezar)**

Deja `MAIL_DRIVER=file`. Los correos **no se envían**: se guardan como archivos `.eml` en `backend\storage\mail\`. Haz doble clic en uno para abrirlo con tu programa de correo y ver cómo se ve (incluido el enlace de restablecer contraseña y el PDF adjunto).

**Opción B: Enviar correos reales con Gmail**

Sirve para pruebas y para unos pocos usuarios (Gmail deja unos 500 correos al día).

1. Entra a tu cuenta de Google y activa la **verificación en dos pasos**: https://myaccount.google.com/security
2. Crea una **contraseña de aplicación**: https://myaccount.google.com/apppasswords
   - Ponle un nombre, por ejemplo `PlataPaMi`, y pulsa **Crear**.
   - Google te muestra una clave de **16 letras**. Cópiala **sin espacios** (solo se muestra una vez).
   > Esta NO es tu contraseña normal de Gmail. Si no te aparece la opción, es porque falta activar la verificación en dos pasos.
3. Edita el `.env` y deja estas líneas con tus datos:
   ```ini
   MAIL_DRIVER=smtp
   SMTP_HOST=smtp.gmail.com
   SMTP_PORT=587
   SMTP_SECURE=tls
   SMTP_USER=tucorreo@gmail.com
   SMTP_PASS=las16letrasSinEspacios
   MAIL_FROM=tucorreo@gmail.com
   MAIL_FROM_NAME=PlataPaMi
   ```
   - `SMTP_USER` y `MAIL_FROM` deben ser **el mismo correo** (si no, Gmail cambia el remitente).
   - Si prefieres el puerto 465, usa `SMTP_PORT=465` y `SMTP_SECURE=ssl`.
4. Guarda. No hace falta reiniciar nada.
5. **Pruébalo**: regístrate en la app con otro correo tuyo y revisa que llegue el de bienvenida (mira también **spam**).
6. Si no llega, abre `backend\storage\logs\php-error.log`: ahí queda el motivo exacto (casi siempre es una contraseña de aplicación mal copiada).

**Opción C: Producción (para que llegue a la bandeja de entrada de cualquiera)**

Con Gmail los correos pueden caer en spam. Para una app real usa un **dominio propio** y un servicio de correo transaccional (**Brevo**, **SendGrid**, **Mailgun** o **Amazon SES**). Todos te dan un host, usuario y contraseña SMTP que van en las mismas variables `SMTP_*`. Además hay que verificar el dominio en ese servicio agregando en el DNS los registros **SPF**, **DKIM** y **DMARC**, y poner `MAIL_FROM=no-reply@tudominio.com`.

> ⚠️ El enlace de "restablecer contraseña" usa `APP_URL`. Con `localhost` solo abre en tu propio PC. Para abrirlo desde un celular en tu casa, pon la **IP de tu PC** (ver Paso 8); para usuarios reales, el backend debe estar en un servidor con dominio público y HTTPS.

#### Paso 7. Comprobar que el backend responde

Abre en el navegador:

```
http://localhost/backend/public/api/index.php?r=terms
```

Debe mostrar algo como:

```json
{"ok":true,"version":"1.0"}
```

Si ves eso, el backend y la base de datos están listos. 🎉

---

### 🅱️ Parte 2: La app (Android Studio)

#### Paso 8. Abrir el proyecto y apuntarlo a tu backend

1. Abre **Android Studio** ▸ **File ▸ Open** y elige la carpeta del proyecto (la que tiene `app`, `backend` y `build.gradle.kts`). Espera a que Gradle termine de sincronizar (barra de progreso abajo).
2. Abre `app/build.gradle.kts`, busca el bloque `debug` y cambia la línea de `API_BASE` según dónde vayas a probar:

   ```kotlin
   buildTypes {
       debug {
           buildConfigField("String", "API_BASE", "\"http://10.0.2.2/backend/public/api/\"")
           manifestPlaceholders["usesCleartext"] = "true"
       }
   }
   ```

   | Dónde pruebas | Qué URL poner |
   |---|---|
   | **Emulador de Android Studio** | `http://10.0.2.2/backend/public/api/` (`10.0.2.2` es tu PC visto desde el emulador) |
   | **Celular físico** | `http://IP_DE_TU_PC/backend/public/api/` |

   - Cambia `backend` por el nombre de tu carpeta en `htdocs`.
   - La URL **debe terminar en `/api/`** (con la barra final) y conservar las comillas escapadas `\"`.
   - Para saber la IP de tu PC: abre PowerShell, ejecuta `ipconfig` y copia la **"Dirección IPv4"** (algo como `192.168.1.50`).
3. Pulsa **Sync Now** en la barra amarilla que sale arriba y espera.

**Si pruebas en un celular físico**, además:
- El PC y el celular deben estar en la **misma red Wi-Fi**.
- En el `.env` cambia `APP_URL` a `http://IP_DE_TU_PC/backend/public` (para que el enlace del correo abra bien desde el celular).
- Si no conecta, permite **Apache HTTP Server** en el Firewall de Windows (redes privadas).
- El celular necesita tener activadas las **opciones de desarrollador** y la **depuración USB** (o usa un emulador, que es más fácil).

#### Paso 9. Ejecutar la app ▶️

1. Crea o elige un emulador en **Device Manager** (por ejemplo un Pixel con Android 13 o superior) o conecta tu celular.
2. Pulsa el botón **Run ▶** de Android Studio.
3. La primera compilación tarda unos minutos.

#### Paso 10. Probar que todo funcione (checklist)

- [ ] Abrir **Crear cuenta**, leer los términos y pulsar **Acepto** (sin aceptar no deja registrarse).
- [ ] Completar el registro y comprobar que llega el correo de bienvenida (o el `.eml` en `storage\mail`).
- [ ] Iniciar sesión.
- [ ] Pulsar **¿Olvidaste tu contraseña?**, abrir el enlace del correo y poner una contraseña nueva.
- [ ] Agregar una **entrada** y un **gasto** con el botón **+**.
- [ ] Ver el gráfico circular y pulsar **Desglosar**.
- [ ] Agregar gastos hasta superar el 80 % de tus entradas y ver la alerta (campana y correo).
- [ ] Entrar a **Extracto**, enviarlo a tu correo y abrir el PDF usando tu **número de documento** como contraseña.

---

### 🅲 Parte 3: Extractos automáticos cada mes (opcional)

El extracto del mes anterior se envía solo a cada usuario que tuvo movimientos, ejecutando este script:

```
backend/cron/monthly_statements.php
```

Puedes probarlo a mano (opcionalmente pasando el mes, formato `AAAA-MM`):

```powershell
cd C:\xampp\htdocs\backend
C:\xampp\php\php.exe cron\monthly_statements.php
C:\xampp\php\php.exe cron\monthly_statements.php 2026-10
```

Para que corra solo el día 1 de cada mes a las 7 a. m.:

- **En Linux/servidor (cron):**
  ```
  0 7 1 * * /usr/bin/php /ruta/al/backend/cron/monthly_statements.php
  ```
- **En Windows (Programador de tareas):** crea una tarea mensual que ejecute el programa `C:\xampp\php\php.exe` con el argumento `C:\xampp\htdocs\backend\cron\monthly_statements.php`.

---

### 🛠️ Si algo falla

<details>
<summary><b>"composer no se reconoce como nombre de un cmdlet..."</b></summary>

Composer no está instalado, o abriste la terminal antes de instalarlo. Instálalo (Paso 3), **cierra y abre PowerShell de nuevo** y prueba con `composer --version`.
</details>

<details>
<summary><b>Composer dice "requires PHP extension ext-gd / ext-zip ... is missing"</b></summary>

Falta activar esa extensión. Repite el Paso 2: quita el `;` de la línea `extension=gd` (o `zip`) en `C:\xampp\php\php.ini`, guarda y vuelve a correr `composer install`.
</details>

<details>
<summary><b>El navegador muestra error 500, pantalla en blanco o "Forbidden" en la URL del backend</b></summary>

- Revisa que Apache y MySQL estén en verde en XAMPP.
- Confirma que existe la carpeta `vendor` (corre `composer install`).
- Confirma que el `.env` existe y tiene `APP_KEY`.
- Pon `APP_DEBUG=1` y mira `backend\storage\logs\php-error.log`.
- Verifica que la URL tenga el nombre real de tu carpeta en `htdocs` y que termine en `/public/api/index.php?r=terms`.
</details>

<details>
<summary><b>En la app sale "No hay conexión" o "No se pudo conectar"</b></summary>

- Revisa `API_BASE` en `app/build.gradle.kts`: la carpeta debe llamarse igual que en `htdocs` y debe terminar en `/api/`.
- En emulador usa `10.0.2.2`, no `localhost`. En celular físico, la IP de tu PC.
- Comprueba que la URL `http://localhost/backend/public/api/index.php?r=terms` abre bien en el navegador de tu PC.
- Después de cambiar `API_BASE`, haz **Sync Now** y vuelve a ejecutar la app.
</details>

<details>
<summary><b>No llegan los correos</b></summary>

- Mira si dejaste `MAIL_DRIVER=file` (en ese modo no se envía nada, solo se guardan en `storage\mail`).
- Revisa la carpeta **spam**.
- Abre `backend\storage\logs\php-error.log`: ahí aparece el motivo (por ejemplo `SMTP Error: Could not authenticate`).
- Con Gmail, usa la **contraseña de aplicación de 16 letras** (no tu contraseña normal), sin espacios, y que `SMTP_USER` y `MAIL_FROM` sean el mismo correo.
</details>

<details>
<summary><b>El enlace de "restablecer contraseña" no abre desde el celular</b></summary>

El enlace usa `APP_URL`. Si dice `localhost`, solo abre en tu PC. Cámbialo por `http://IP_DE_TU_PC/backend/public` (y que el celular esté en la misma red Wi-Fi).
</details>

<details>
<summary><b>No puedo abrir el PDF del extracto</b></summary>

La contraseña es **tu número de documento**, solo los números y sin puntos ni espacios (por ejemplo `1012345678`).
</details>

<details>
<summary><b>Perdí o cambié la APP_KEY</b></summary>

Con otra clave ya no se puede descifrar el documento de los usuarios existentes, y no podrán abrir sus extractos. En desarrollo basta con vaciar las tablas e iniciar de nuevo; en producción **guarda la clave en un lugar seguro desde el principio**.
</details>

---

## 🔌 API

Todas las rutas son `index.php?r=<ruta>` y responden JSON. Las privadas llevan `Authorization: Bearer <token>`.

| Método | Ruta | Para qué |
|---|---|---|
| `POST` | `auth/register` | Crear cuenta (exige aceptar los términos) |
| `POST` | `auth/login` | Iniciar sesión |
| `POST` | `auth/logout` | Cerrar sesión |
| `POST` | `auth/forgot` | Enviar el enlace para restablecer la contraseña |
| `GET` | `me` | Datos del usuario |
| `POST` | `me/delete` | Eliminar la cuenta y todos sus datos |
| `GET` | `movements?month=YYYY-MM` | Movimientos y resumen del mes |
| `POST` | `movements` | Crear un movimiento |
| `PUT` / `DELETE` | `movements/{id}` | Editar o eliminar |
| `GET` | `notifications` | Notificaciones de la campana |
| `POST` | `notifications/read` | Marcarlas como leídas |
| `POST` | `statements/send` | Generar y enviar el extracto del mes por correo |
| `GET` | `terms` | Versión vigente de los términos |

---

## 🔒 Seguridad y datos personales

- Contraseñas guardadas con **hash** (`password_hash`), nunca en texto plano.
- El **número de documento se guarda cifrado** (AES-256-GCM) y solo se usa para proteger el PDF del extracto.
- Consultas **preparadas** contra inyección SQL; los tokens de sesión y de recuperación se guardan solo como hash.
- **Bloqueo temporal** de la cuenta tras 5 intentos fallidos de inicio de sesión.
- El enlace de recuperar contraseña sirve **una sola vez**, vence en 60 minutos y cierra las sesiones abiertas al usarse.
- Se guarda la **constancia de aceptación** de los términos (fecha, versión e IP).
- En producción la app solo acepta conexiones **HTTPS**.

---

## 🛣️ Pendiente / ideas a futuro

- [ ] Versión para iOS
- [ ] Presupuestos por categoría
- [ ] Autenticación en dos pasos
- [ ] Guardado de movimientos sin conexión con sincronización posterior
- [ ] Revisión legal de los Términos y Condiciones antes de publicar la app

---

## 👤 Autor

Hecho por **Julian Cortes**