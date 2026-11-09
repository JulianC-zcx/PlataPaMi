-- PlataPaMi · Esquema MySQL 8 / MariaDB 10.4+
-- Importar con:  mysql -u root -p < database/schema.sql
CREATE DATABASE IF NOT EXISTS platapami
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE platapami;

-- ─────────── Usuarios ───────────
CREATE TABLE IF NOT EXISTS usuarios (
  id                  INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  nombre              VARCHAR(100)  NOT NULL,
  tipo_documento      ENUM('CC','CE','TI','PA') NOT NULL DEFAULT 'CC',
  documento_enc       VARCHAR(255)  NOT NULL,            -- AES-256-GCM (nunca en claro)
  documento_hash      CHAR(64)      NOT NULL,            -- HMAC-SHA256, evita duplicados
  correo              VARCHAR(150)  NOT NULL,
  password_hash       VARCHAR(255)  NOT NULL,            -- password_hash() (bcrypt/argon2)
  acepto_terminos_at  DATETIME      NOT NULL,            -- constancia Habeas Data
  version_terminos    VARCHAR(20)   NOT NULL,
  ip_aceptacion       VARCHAR(45)   NULL,
  intentos_fallidos   TINYINT UNSIGNED NOT NULL DEFAULT 0,
  bloqueado_hasta     DATETIME      NULL,
  creado_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_usuarios_correo (correo),
  UNIQUE KEY uq_usuarios_doc (documento_hash)
) ENGINE=InnoDB;

-- ─────────── Sesiones (token Bearer, se guarda solo su hash) ───────────
CREATE TABLE IF NOT EXISTS sesiones (
  id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  usuario_id  INT UNSIGNED NOT NULL,
  token_hash  CHAR(64)     NOT NULL,
  user_agent  VARCHAR(200) NULL,
  expira_at   DATETIME     NOT NULL,
  creado_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_sesion_token (token_hash),
  KEY ix_sesion_usuario (usuario_id),
  CONSTRAINT fk_sesion_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ─────────── Restablecer contraseña (enlace por correo, un solo uso) ───────────
CREATE TABLE IF NOT EXISTS reset_tokens (
  id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  usuario_id  INT UNSIGNED NOT NULL,
  token_hash  CHAR(64)     NOT NULL,
  expira_at   DATETIME     NOT NULL,
  usado_at    DATETIME     NULL,
  creado_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_reset_token (token_hash),
  KEY ix_reset_usuario (usuario_id),
  CONSTRAINT fk_reset_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ─────────── Movimientos (entradas y gastos) ───────────
CREATE TABLE IF NOT EXISTS movimientos (
  id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  usuario_id     INT UNSIGNED NOT NULL,
  tipo           ENUM('entrada','gasto') NOT NULL,
  nombre         VARCHAR(80)  NOT NULL,
  detalle        VARCHAR(120) NOT NULL DEFAULT '',
  categoria      VARCHAR(30)  NOT NULL DEFAULT 'otros',
  monto          BIGINT UNSIGNED NOT NULL,                -- pesos colombianos, sin decimales
  fecha          DATE         NOT NULL,
  imagen         VARCHAR(80)  NULL,                       -- nombre de archivo en public/uploads
  creado_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  actualizado_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY ix_mov_usuario_fecha (usuario_id, fecha),
  CONSTRAINT fk_mov_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ─────────── Notificaciones (campana de la app + alertas por correo) ───────────
CREATE TABLE IF NOT EXISTS notificaciones (
  id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  usuario_id  INT UNSIGNED NOT NULL,
  tipo        ENUM('alerta80','alerta100','extracto','info') NOT NULL,
  titulo      VARCHAR(120) NOT NULL,
  mensaje     VARCHAR(400) NOT NULL,
  mes         CHAR(7)      NOT NULL DEFAULT '',          -- 'YYYY-MM'
  leida       TINYINT(1)   NOT NULL DEFAULT 0,
  creado_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- Una alerta de cada nivel por mes (evita correos repetidos)
  UNIQUE KEY uq_notif_alerta (usuario_id, tipo, mes),
  KEY ix_notif_usuario (usuario_id, creado_at),
  CONSTRAINT fk_notif_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ─────────── Registro de extractos enviados ───────────
CREATE TABLE IF NOT EXISTS extractos_enviados (
  id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  usuario_id  INT UNSIGNED NOT NULL,
  mes         CHAR(7)      NOT NULL,
  origen      ENUM('manual','automatico') NOT NULL,
  enviado_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY ix_ext_usuario (usuario_id, mes, origen),
  CONSTRAINT fk_ext_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
) ENGINE=InnoDB;
