<?php
declare(strict_types=1);

/**
 * PlataPaMi · API REST (JSON)
 * Se llama como  /api/index.php?r=<ruta>   (funciona en cualquier hosting, sin reglas de reescritura).
 *
 *  POST auth/register        POST auth/login        POST auth/logout
 *  POST auth/forgot          GET  me          POST me/delete {password}
 *  GET  movements?month=YYYY-MM          POST movements
 *  PUT  movements/{id}                   DELETE movements/{id}
 *  GET  notifications        POST notifications/read
 *  POST statements/send  {month}         GET  terms
 */

use PlataPaMi\{Auth, Db, Env, Fmt, Http, Movements, Statements};

require dirname(__DIR__, 2) . '/src/bootstrap.php';

header('X-Content-Type-Options: nosniff');

set_exception_handler(static function (\Throwable $e): void {
    error_log($e::class . ': ' . $e->getMessage() . ' @ ' . $e->getFile() . ':' . $e->getLine());
    $debug = Env::get('APP_DEBUG', '0') === '1';
    Http::fail(500, $debug ? $e->getMessage() : 'Error interno del servidor. Intenta de nuevo.');
});

$method = $_SERVER['REQUEST_METHOD'] ?? 'GET';
$route  = trim((string)($_GET['r'] ?? ''), '/');

switch (true) {
    // ───────────── Autenticación ─────────────
    case $route === 'auth/register' && $method === 'POST':
        Http::ok(Auth::register(Http::body()), 201);

    case $route === 'auth/login' && $method === 'POST':
        Http::ok(Auth::login(Http::body()));

    case $route === 'auth/logout' && $method === 'POST':
        Auth::logout();
        Http::ok();

    case $route === 'auth/forgot' && $method === 'POST':
        Auth::forgot(Http::body());
        Http::ok(['mensaje' => 'Si el correo está registrado, te enviamos un enlace para restablecer tu contraseña.']);

    case $route === 'me' && $method === 'GET':
        $u = Auth::requireUser();
        Http::ok(['usuario' => Auth::publicUser((int)$u['id'])]);

    case $route === 'me/delete' && $method === 'POST':
        $u = Auth::requireUser();
        Auth::deleteAccount($u, is_string(Http::body()['password'] ?? null) ? Http::body()['password'] : '');
        Http::ok(['mensaje' => 'Tu cuenta y tus datos fueron eliminados.']);

    // ───────────── Movimientos ─────────────
    case $route === 'movements' && $method === 'GET':
        $u  = Auth::requireUser();
        $ym = (string)($_GET['month'] ?? Fmt::currentMonth());
        if (!Fmt::isMonth($ym)) {
            Http::fail(422, 'Mes no válido (usa AAAA-MM).');
        }
        Http::ok(['movimientos' => Movements::forMonth((int)$u['id'], $ym), 'resumen' => Movements::summary((int)$u['id'], $ym)]);

    case $route === 'movements' && $method === 'POST':
        $u = Auth::requireUser();
        Http::ok(['movimiento' => Movements::create($u, Http::body())], 201);

    case (bool)preg_match('#^movements/(\d+)$#', $route, $m) && $method === 'PUT':
        $u = Auth::requireUser();
        Http::ok(['movimiento' => Movements::update($u, (int)$m[1], Http::body())]);

    case (bool)preg_match('#^movements/(\d+)$#', $route, $m) && $method === 'DELETE':
        $u = Auth::requireUser();
        Movements::delete($u, (int)$m[1]);
        Http::ok();

    // ───────────── Notificaciones (campana) ─────────────
    case $route === 'notifications' && $method === 'GET':
        $u = Auth::requireUser();
        $rows = Db::all('SELECT id, tipo, titulo, mensaje, mes, leida, creado_at FROM notificaciones WHERE usuario_id = ? ORDER BY creado_at DESC, id DESC LIMIT 50', [$u['id']]);
        foreach ($rows as &$r) {
            $r['id'] = (int)$r['id'];
            $r['leida'] = (bool)$r['leida'];
        }
        unset($r);
        $sinLeer = (int)(Db::one('SELECT COUNT(*) AS n FROM notificaciones WHERE usuario_id = ? AND leida = 0', [$u['id']])['n'] ?? 0);
        Http::ok(['notificaciones' => $rows, 'sin_leer' => $sinLeer]);

    case $route === 'notifications/read' && $method === 'POST':
        $u = Auth::requireUser();
        Db::q('UPDATE notificaciones SET leida = 1 WHERE usuario_id = ?', [$u['id']]);
        Http::ok();

    // ───────────── Extracto mensual por correo ─────────────
    case $route === 'statements/send' && $method === 'POST':
        $u  = Auth::requireUser();
        $ym = Http::str(Http::body(), 'month', 7);
        if (!Fmt::isMonth($ym)) {
            Http::fail(422, 'Mes no válido (usa AAAA-MM).');
        }
        // Freno anti-abuso: máx. 5 envíos manuales por hora
        $n = (int)(Db::one("SELECT COUNT(*) AS n FROM extractos_enviados WHERE usuario_id = ? AND origen = 'manual' AND enviado_at > DATE_SUB(NOW(), INTERVAL 1 HOUR)", [$u['id']])['n'] ?? 0);
        if ($n >= 5) {
            Http::fail(429, 'Ya solicitaste varios extractos en la última hora. Intenta más tarde.');
        }
        try {
            Statements::send($u, $ym, 'manual');
        } catch (\Throwable $e) {
            error_log('Envío de extracto falló: ' . $e->getMessage());
            Http::fail(502, 'No pudimos enviar el correo en este momento. Intenta de nuevo en unos minutos.');
        }
        Http::ok(['mensaje' => 'Te enviamos el extracto de ' . Fmt::monthLong($ym) . ' a ' . $u['correo'] . '.', 'correo' => $u['correo']]);

    case $route === 'terms' && $method === 'GET':
        Http::ok(['version' => Env::get('TERMS_VERSION', '1.0')]);

    default:
        Http::fail(404, 'Ruta no encontrada.');
}
