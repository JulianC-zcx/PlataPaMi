<?php
declare(strict_types=1);

/**
 * Envía automáticamente el extracto del MES ANTERIOR a cada usuario que tuvo movimientos
 * ("notificación de gastos al terminar el mes"). Programar para el día 1 de cada mes:
 *
 *   0 7 1 * *  /usr/bin/php /ruta/a/platapami_backend/cron/monthly_statements.php
 *
 * Opcional: pasar el mes explícito  →  php cron/monthly_statements.php 2026-08
 */

use PlataPaMi\{Db, Fmt, Statements};

if (PHP_SAPI !== 'cli') {
    http_response_code(403);
    exit('Solo por línea de comandos.');
}
require dirname(__DIR__) . '/src/bootstrap.php';

$ym = $argv[1] ?? Fmt::previousMonth();
if (!Fmt::isMonth($ym)) {
    fwrite(STDERR, "Mes no válido: {$ym}\n");
    exit(1);
}
[$ini, $fin] = Fmt::monthBounds($ym);

$users = Db::all(
    "SELECT u.* FROM usuarios u
     WHERE EXISTS (SELECT 1 FROM movimientos m WHERE m.usuario_id = u.id AND m.fecha BETWEEN ? AND ?)
       AND NOT EXISTS (SELECT 1 FROM extractos_enviados x WHERE x.usuario_id = u.id AND x.mes = ? AND x.origen = 'automatico')",
    [$ini, $fin, $ym]
);

$ok = $fail = 0;
foreach ($users as $u) {
    try {
        Statements::send($u, $ym, 'automatico');
        $ok++;
    } catch (\Throwable $e) {
        $fail++;
        error_log("Extracto automático falló para usuario {$u['id']}: " . $e->getMessage());
    }
    usleep(300_000); // no saturar el SMTP
}
echo "Extractos de {$ym}: {$ok} enviados, {$fail} con error.\n";
