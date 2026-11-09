<?php
declare(strict_types=1);

namespace PlataPaMi;

/** Registro, inicio de sesión, sesiones y restablecimiento de contraseña. */
final class Auth
{
    private const MAX_FAILS = 5;
    private const LOCK_MIN  = 15;

    // ───────────── Validaciones ─────────────
    public static function validatePassword(string $p): ?string
    {
        if (mb_strlen($p) < 8) {
            return 'La contraseña debe tener mínimo 8 caracteres.';
        }
        if (!preg_match('/[A-Za-z]/', $p) || !preg_match('/\d/', $p)) {
            return 'La contraseña debe incluir letras y números.';
        }
        if (mb_strlen($p) > 72) {
            return 'La contraseña es demasiado larga (máximo 72 caracteres).';
        }
        return null;
    }

    // ───────────── Registro ─────────────
    public static function register(array $in): array
    {
        $nombre = Http::str($in, 'nombre', 100);
        $tipo   = strtoupper(Http::str($in, 'tipo_documento', 2)) ?: 'CC';
        $doc    = Crypto::normalizeDoc(Http::str($in, 'documento', 20));
        $correo = strtolower(Http::str($in, 'correo', 150));
        $clave  = is_string($in['password'] ?? null) ? $in['password'] : '';
        $acepta = ($in['acepta_terminos'] ?? false) === true;

        if (!$acepta) {
            Http::fail(422, 'Debes aceptar los Términos y Condiciones y la Política de Habeas Data para registrarte.', ['campo' => 'acepta_terminos']);
        }
        if (mb_strlen($nombre) < 2) {
            Http::fail(422, 'Escribe tu nombre completo.', ['campo' => 'nombre']);
        }
        if (!in_array($tipo, ['CC', 'CE', 'TI', 'PA'], true)) {
            Http::fail(422, 'Tipo de documento no válido.', ['campo' => 'tipo_documento']);
        }
        if (!preg_match('/^[A-Z0-9]{5,15}$/', $doc) || ($tipo !== 'PA' && !ctype_digit($doc))) {
            Http::fail(422, 'Número de documento no válido.', ['campo' => 'documento']);
        }
        if (!filter_var($correo, FILTER_VALIDATE_EMAIL)) {
            Http::fail(422, 'Correo electrónico no válido.', ['campo' => 'correo']);
        }
        if ($err = self::validatePassword($clave)) {
            Http::fail(422, $err, ['campo' => 'password']);
        }

        if (Db::one('SELECT id FROM usuarios WHERE correo = ?', [$correo])) {
            Http::fail(409, 'Ya existe una cuenta con ese correo.', ['campo' => 'correo']);
        }
        if (Db::one('SELECT id FROM usuarios WHERE documento_hash = ?', [Crypto::docHash($doc)])) {
            Http::fail(409, 'Ya existe una cuenta con ese número de documento.', ['campo' => 'documento']);
        }

        Db::q(
            'INSERT INTO usuarios (nombre, tipo_documento, documento_enc, documento_hash, correo, password_hash,
                                   acepto_terminos_at, version_terminos, ip_aceptacion)
             VALUES (?,?,?,?,?,?,NOW(),?,?)',
            [$nombre, $tipo, Crypto::encrypt($doc), Crypto::docHash($doc), $correo,
             password_hash($clave, PASSWORD_DEFAULT), (string)Env::get('TERMS_VERSION', '1.0'), Http::ip()]
        );
        $id = (int)Db::pdo()->lastInsertId();

        try {
            $n = Mail::e($nombre);
            Mail::send(
                $correo, $nombre, '¡Bienvenido a PlataPaMi!',
                "<p>Hola <b>{$n}</b>,</p><p>Tu cuenta quedó creada. Desde la app puedes registrar tus <b>entradas</b> y "
                . "<b>gastos</b>, ver tu gráfico mensual y descargar tu extracto financiero.</p>"
                . '<p>Tu extracto en PDF se protege con tu número de documento.</p>',
                "Hola {$nombre}, tu cuenta en PlataPaMi quedó creada."
            );
        } catch (\Throwable $e) {
            error_log('Correo de bienvenida falló: ' . $e->getMessage());
        }

        return self::startSession($id);
    }

    // ───────────── Login ─────────────
    public static function login(array $in): array
    {
        $correo = strtolower(Http::str($in, 'correo', 150));
        $clave  = is_string($in['password'] ?? null) ? $in['password'] : '';
        $u = Db::one('SELECT * FROM usuarios WHERE correo = ?', [$correo]);

        $generic = 'Correo o contraseña incorrectos.';
        if (!$u) {
            password_verify($clave, '$2y$10$DEa5iyTnYCfIL6nK50La2OH2tF7sMgJGyPCbn872NU5rIUraUy3Xi'); // mismo costo que un login real
            Http::fail(401, $generic);
        }
        if ($u['bloqueado_hasta'] && strtotime((string)$u['bloqueado_hasta']) > time()) {
            Http::fail(429, 'Demasiados intentos fallidos. Intenta de nuevo en unos minutos o restablece tu contraseña.');
        }
        if (!password_verify($clave, (string)$u['password_hash'])) {
            $fails = (int)$u['intentos_fallidos'] + 1;
            if ($fails >= self::MAX_FAILS) {
                Db::q('UPDATE usuarios SET intentos_fallidos = 0, bloqueado_hasta = DATE_ADD(NOW(), INTERVAL ' . self::LOCK_MIN . ' MINUTE) WHERE id = ?', [$u['id']]);
            } else {
                Db::q('UPDATE usuarios SET intentos_fallidos = ? WHERE id = ?', [$fails, $u['id']]);
            }
            Http::fail(401, $generic);
        }
        Db::q('UPDATE usuarios SET intentos_fallidos = 0, bloqueado_hasta = NULL WHERE id = ?', [$u['id']]);
        if (password_needs_rehash((string)$u['password_hash'], PASSWORD_DEFAULT)) {
            Db::q('UPDATE usuarios SET password_hash = ? WHERE id = ?', [password_hash($clave, PASSWORD_DEFAULT), $u['id']]);
        }
        return self::startSession((int)$u['id']);
    }

    private static function startSession(int $userId): array
    {
        [$token, $hash] = Crypto::newToken();
        $days = Env::int('SESSION_DAYS', 30);
        Db::q(
            'INSERT INTO sesiones (usuario_id, token_hash, user_agent, expira_at)
             VALUES (?,?,?,DATE_ADD(NOW(), INTERVAL ' . $days . ' DAY))',
            [$userId, $hash, substr($_SERVER['HTTP_USER_AGENT'] ?? '', 0, 200)]
        );
        Db::q('DELETE FROM sesiones WHERE expira_at < NOW()');
        return ['token' => $token, 'usuario' => self::publicUser($userId)];
    }

    public static function publicUser(int $id): array
    {
        $u = Db::one('SELECT id, nombre, correo, tipo_documento, documento_enc FROM usuarios WHERE id = ?', [$id]) ?? [];
        $doc = '';
        try {
            $doc = Crypto::decrypt((string)$u['documento_enc']);
        } catch (\Throwable) {
        }
        return [
            'id'             => (int)$u['id'],
            'nombre'         => $u['nombre'],
            'correo'         => $u['correo'],
            'tipo_documento' => $u['tipo_documento'],
            'documento_mask' => $doc === '' ? '' : str_repeat('•', max(0, strlen($doc) - 4)) . substr($doc, -4),
        ];
    }

    /** Devuelve la fila del usuario autenticado o responde 401. */
    public static function requireUser(): array
    {
        $token = Http::bearer();
        if ($token === null) {
            Http::fail(401, 'Sesión no válida.');
        }
        $u = Db::one(
            'SELECT u.* FROM sesiones s JOIN usuarios u ON u.id = s.usuario_id
             WHERE s.token_hash = ? AND s.expira_at > NOW()',
            [Crypto::hashToken($token)]
        );
        if (!$u) {
            Http::fail(401, 'Tu sesión expiró. Inicia sesión de nuevo.');
        }
        return $u;
    }

    /** Derecho de supresión (Habeas Data): borra la cuenta, sus movimientos, comprobantes y notificaciones. */
    public static function deleteAccount(array $user, string $password): void
    {
        if (!password_verify($password, (string)$user['password_hash'])) {
            Http::fail(403, 'La contraseña no es correcta.');
        }
        Movements::deleteAllImages((int)$user['id']);
        Db::q('DELETE FROM usuarios WHERE id = ?', [$user['id']]); // ON DELETE CASCADE limpia el resto
    }

    public static function logout(): void
    {
        $token = Http::bearer();
        if ($token !== null) {
            Db::q('DELETE FROM sesiones WHERE token_hash = ?', [Crypto::hashToken($token)]);
        }
    }

    // ───────────── Restablecer contraseña ─────────────
    /** Siempre responde igual para no revelar si el correo existe. */
    public static function forgot(array $in): void
    {
        $correo = strtolower(Http::str($in, 'correo', 150));
        $u = filter_var($correo, FILTER_VALIDATE_EMAIL) ? Db::one('SELECT id, nombre, correo FROM usuarios WHERE correo = ?', [$correo]) : null;
        if ($u) {
            $recent = Db::one('SELECT id FROM reset_tokens WHERE usuario_id = ? AND creado_at > DATE_SUB(NOW(), INTERVAL 2 MINUTE)', [$u['id']]);
            if (!$recent) {
                [$token, $hash] = Crypto::newToken();
                $ttl = Env::int('RESET_TTL_MIN', 60);
                Db::q('DELETE FROM reset_tokens WHERE usuario_id = ? AND usado_at IS NULL', [$u['id']]);
                Db::q(
                    'INSERT INTO reset_tokens (usuario_id, token_hash, expira_at) VALUES (?,?,DATE_ADD(NOW(), INTERVAL ' . $ttl . ' MINUTE))',
                    [$u['id'], $hash]
                );
                $link = rtrim((string)Env::get('APP_URL', ''), '/') . '/reset.php?token=' . $token;
                try {
                    Mail::send(
                        $u['correo'], $u['nombre'], 'Restablece tu contraseña',
                        '<p>Hola <b>' . Mail::e($u['nombre']) . '</b>,</p>'
                        . '<p>Recibimos una solicitud para restablecer la contraseña de tu cuenta. Toca el botón para elegir una nueva. '
                        . "El enlace funciona una sola vez y vence en {$ttl} minutos.</p>"
                        . Mail::button($link, 'Crear nueva contraseña')
                        . '<p style="font-size:12px;color:#777">Si el botón no funciona, copia este enlace en tu navegador:<br>' . Mail::e($link) . '</p>'
                        . '<p>Si tú no lo solicitaste, ignora este correo: tu contraseña actual sigue siendo la misma.</p>',
                        "Restablece tu contraseña de PlataPaMi (vence en {$ttl} minutos): {$link}"
                    );
                } catch (\Throwable $e) {
                    error_log('Correo de restablecimiento falló: ' . $e->getMessage());
                }
            }
        }
    }

    /** @return array|null fila de reset_tokens + usuario válida (no usada ni vencida) */
    public static function findResetToken(string $token): ?array
    {
        if (!preg_match('/^[a-f0-9]{64}$/', $token)) {
            return null;
        }
        return Db::one(
            'SELECT r.id AS reset_id, r.usuario_id, u.nombre, u.correo
             FROM reset_tokens r JOIN usuarios u ON u.id = r.usuario_id
             WHERE r.token_hash = ? AND r.usado_at IS NULL AND r.expira_at > NOW()',
            [Crypto::hashToken($token)]
        );
    }

    public static function resetPassword(string $token, string $nueva): bool
    {
        $row = self::findResetToken($token);
        if (!$row) {
            return false;
        }
        $pdo = Db::pdo();
        $pdo->beginTransaction();
        try {
            Db::q('UPDATE usuarios SET password_hash = ?, intentos_fallidos = 0, bloqueado_hasta = NULL WHERE id = ?', [password_hash($nueva, PASSWORD_DEFAULT), $row['usuario_id']]);
            Db::q('UPDATE reset_tokens SET usado_at = NOW() WHERE id = ?', [$row['reset_id']]);
            Db::q('DELETE FROM sesiones WHERE usuario_id = ?', [$row['usuario_id']]); // cierra sesiones abiertas
            $pdo->commit();
        } catch (\Throwable $e) {
            $pdo->rollBack();
            throw $e;
        }
        return true;
    }
}
