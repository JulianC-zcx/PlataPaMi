<?php
declare(strict_types=1);

namespace PlataPaMi;

/** Entradas y gastos de cada usuario, resumen mensual y alertas de presupuesto. */
final class Movements
{
    public const CATEGORIAS = ['comida', 'transporte', 'hogar', 'ocio', 'salud', 'educacion', 'compras', 'servicios', 'salario', 'otros'];
    private const MAX_MONTO = 9_000_000_000_000;

    // ───────────── Lectura ─────────────
    public static function format(array $r): array
    {
        return [
            'id'        => (int)$r['id'],
            'tipo'      => $r['tipo'],
            'nombre'    => $r['nombre'],
            'detalle'   => $r['detalle'],
            'categoria' => $r['categoria'],
            'monto'     => (int)$r['monto'],
            'fecha'     => $r['fecha'],
            'imagen'    => $r['imagen'] ? 'uploads/' . $r['imagen'] : null,   // ruta relativa a /public: la app la une a su propia URL base
        ];
    }

    /** @return list<array<string,mixed>> */
    public static function forMonth(int $userId, string $ym): array
    {
        [$ini, $fin] = Fmt::monthBounds($ym);
        $rows = Db::all(
            'SELECT * FROM movimientos WHERE usuario_id = ? AND fecha BETWEEN ? AND ? ORDER BY fecha DESC, id DESC',
            [$userId, $ini, $fin]
        );
        return array_map([self::class, 'format'], $rows);
    }

    public static function summary(int $userId, string $ym): array
    {
        [$ini, $fin] = Fmt::monthBounds($ym);
        $r = Db::one(
            "SELECT COALESCE(SUM(CASE WHEN tipo='entrada' THEN monto END),0) AS entradas,
                    COALESCE(SUM(CASE WHEN tipo='gasto'   THEN monto END),0) AS gastos
             FROM movimientos WHERE usuario_id = ? AND fecha BETWEEN ? AND ?",
            [$userId, $ini, $fin]
        );
        $e = (int)$r['entradas'];
        $g = (int)$r['gastos'];
        return [
            'mes'         => $ym,
            'entradas'    => $e,
            'gastos'      => $g,
            'saldo'       => $e - $g,
            'pct_gastado' => $e > 0 ? round($g * 100 / $e, 1) : null,
        ];
    }

    // ───────────── Escritura ─────────────
    /** Valida el cuerpo y devuelve los campos limpios. */
    private static function clean(array $in): array
    {
        $tipo = Http::str($in, 'tipo', 10);
        if (!in_array($tipo, ['entrada', 'gasto'], true)) {
            Http::fail(422, 'El tipo debe ser "entrada" o "gasto".', ['campo' => 'tipo']);
        }
        $nombre = Http::str($in, 'nombre', 80);
        if ($nombre === '') {
            Http::fail(422, 'Escribe un nombre para el movimiento.', ['campo' => 'nombre']);
        }
        $montoRaw = $in['monto'] ?? null;
        if (is_string($montoRaw) && ctype_digit($montoRaw)) {
            $montoRaw = (int)$montoRaw;
        }
        if (!is_int($montoRaw) || $montoRaw <= 0) {
            Http::fail(422, 'Indica el valor (mayor a cero) antes de guardar.', ['campo' => 'monto']);
        }
        if ($montoRaw > self::MAX_MONTO) {
            Http::fail(422, 'El valor es demasiado grande.', ['campo' => 'monto']);
        }
        $fecha = Http::str($in, 'fecha', 10);
        $d = \DateTime::createFromFormat('!Y-m-d', $fecha);
        if (!$d || $d->format('Y-m-d') !== $fecha || (int)$d->format('Y') < 2000 || (int)$d->format('Y') > 2100) {
            Http::fail(422, 'Fecha no válida (usa AAAA-MM-DD).', ['campo' => 'fecha']);
        }
        $cat = Http::str($in, 'categoria', 30) ?: 'otros';
        if (!in_array($cat, self::CATEGORIAS, true)) {
            $cat = 'otros';
        }
        return [$tipo, $nombre, Http::str($in, 'detalle', 120), $cat, $montoRaw, $fecha];
    }

    public static function create(array $user, array $in): array
    {
        [$tipo, $nombre, $detalle, $cat, $monto, $fecha] = self::clean($in);
        $img = isset($in['imagen_base64']) && is_string($in['imagen_base64']) && $in['imagen_base64'] !== ''
            ? self::saveImage($in['imagen_base64']) : null;
        Db::q(
            'INSERT INTO movimientos (usuario_id, tipo, nombre, detalle, categoria, monto, fecha, imagen) VALUES (?,?,?,?,?,?,?,?)',
            [$user['id'], $tipo, $nombre, $detalle, $cat, $monto, $fecha, $img]
        );
        $id = (int)Db::pdo()->lastInsertId();
        self::afterChange($user, substr($fecha, 0, 7));
        return self::format(Db::one('SELECT * FROM movimientos WHERE id = ?', [$id]) ?? []);
    }

    public static function update(array $user, int $id, array $in): array
    {
        $old = Db::one('SELECT * FROM movimientos WHERE id = ? AND usuario_id = ?', [$id, $user['id']]);
        if (!$old) {
            Http::fail(404, 'Movimiento no encontrado.');
        }
        [$tipo, $nombre, $detalle, $cat, $monto, $fecha] = self::clean($in);
        $img = $old['imagen'];
        if (($in['quitar_imagen'] ?? false) === true) {
            self::deleteImage($img);
            $img = null;
        }
        if (isset($in['imagen_base64']) && is_string($in['imagen_base64']) && $in['imagen_base64'] !== '') {
            self::deleteImage($img);
            $img = self::saveImage($in['imagen_base64']);
        }
        Db::q(
            'UPDATE movimientos SET tipo=?, nombre=?, detalle=?, categoria=?, monto=?, fecha=?, imagen=? WHERE id=? AND usuario_id=?',
            [$tipo, $nombre, $detalle, $cat, $monto, $fecha, $img, $id, $user['id']]
        );
        self::afterChange($user, substr($fecha, 0, 7));
        return self::format(Db::one('SELECT * FROM movimientos WHERE id = ?', [$id]) ?? []);
    }

    public static function delete(array $user, int $id): void
    {
        $old = Db::one('SELECT * FROM movimientos WHERE id = ? AND usuario_id = ?', [$id, $user['id']]);
        if (!$old) {
            Http::fail(404, 'Movimiento no encontrado.');
        }
        Db::q('DELETE FROM movimientos WHERE id = ? AND usuario_id = ?', [$id, $user['id']]);
        self::deleteImage($old['imagen']);
    }

    // ───────────── Alertas de presupuesto ─────────────
    private static function afterChange(array $user, string $ym): void
    {
        try {
            self::checkAlerts($user, $ym);
        } catch (\Throwable $e) {
            error_log('Alertas fallaron: ' . $e->getMessage());
        }
    }

    /**
     * Presupuesto del mes = total de entradas del mes.
     *  · Gastos >= 80 % de las entradas → alerta80 (una vez por mes, por correo + campana)
     *  · Gastos  > 100 %                → alerta100 (sobrepresupuesto, una vez por mes)
     */
    public static function checkAlerts(array $user, string $ym): void
    {
        $s = self::summary((int)$user['id'], $ym);
        if ($s['entradas'] <= 0 || $s['gastos'] <= 0) {
            return;
        }
        $ratio = $s['gastos'] / $s['entradas'];
        $mes = Fmt::monthLong($ym);
        $pct = (int)floor($ratio * 100);

        if ($ratio > 1.0) {
            // Si de un salto pasó el 100 %, solo se envía el aviso de sobrepresupuesto (el del 80 % queda marcado en silencio).
            Db::q('INSERT IGNORE INTO notificaciones (usuario_id, tipo, titulo, mensaje, mes, leida) VALUES (?,?,?,?,?,1)',
                [$user['id'], 'alerta80', 'Llevas más del 80% de tu presupuesto', 'Superaste el 80% del presupuesto de ' . $mes . '.', $ym]);
        } elseif ($ratio >= 0.8) {
            self::notify(
                $user, 'alerta80', $ym,
                'Llevas el ' . $pct . '% de tu presupuesto',
                "En {$mes} ya gastaste " . Fmt::cop($s['gastos']) . ' de ' . Fmt::cop($s['entradas']) . ' que has recibido. Cuida lo que queda.',
                "<p>Hola <b>" . Mail::e($user['nombre']) . '</b>,</p>'
                . "<p>En <b>{$mes}</b> ya llevas gastado el <b style=\"color:#C93567\">{$pct}%</b> de tus entradas.</p>"
                . self::miniTable($s)
                . '<p>Revisa tus gastos en la app y ajusta lo que haga falta para terminar el mes con tranquilidad. 💪</p>'
            );
        }
        if ($ratio > 1.0) {
            self::notify(
                $user, 'alerta100', $ym,
                '¡Te pasaste del presupuesto!',
                "En {$mes} tus gastos (" . Fmt::cop($s['gastos']) . ') superan tus entradas (' . Fmt::cop($s['entradas']) . ').',
                "<p>Hola <b>" . Mail::e($user['nombre']) . '</b>,</p>'
                . "<p>En <b>{$mes}</b> tus gastos <b style=\"color:#C93567\">superaron</b> tus entradas.</p>"
                . self::miniTable($s)
                . '<p>Entra a la app y mira en qué se te va el dinero con el gráfico.</p>'
            );
        }
    }

    private static function miniTable(array $s): string
    {
        $row = static fn(string $l, string $v, string $c) =>
            "<tr><td style=\"padding:8px 0;border-bottom:1px solid #eee\">{$l}</td><td align=\"right\" style=\"padding:8px 0;border-bottom:1px solid #eee;font-weight:bold;color:{$c}\">{$v}</td></tr>";
        return '<table width="100%" cellpadding="0" cellspacing="0" style="margin:14px 0">'
            . $row('Entradas', Fmt::cop($s['entradas']), '#00A050')
            . $row('Gastos', Fmt::cop($s['gastos']), '#C93567')
            . $row('Saldo', Fmt::cop($s['saldo']), '#0099B8')
            . '</table>';
    }

    private static function notify(array $user, string $tipo, string $ym, string $titulo, string $mensaje, string $html): void
    {
        // La clave única (usuario, tipo, mes) garantiza una sola alerta de cada nivel por mes.
        $st = Db::q('INSERT IGNORE INTO notificaciones (usuario_id, tipo, titulo, mensaje, mes) VALUES (?,?,?,?,?)', [$user['id'], $tipo, $titulo, $mensaje, $ym]);
        if ($st->rowCount() === 1) {
            try {
                Mail::send($user['correo'], $user['nombre'], $titulo, $html, $titulo . ' — ' . $mensaje);
            } catch (\Throwable $e) {
                error_log('Correo de alerta falló: ' . $e->getMessage());
            }
        }
    }

    // ───────────── Imágenes de comprobante ─────────────
    private static function uploadsDir(): string
    {
        $dir = BASE_PATH . '/public/uploads';
        if (!is_dir($dir)) {
            mkdir($dir, 0755, true);
        }
        return $dir;
    }

    private static function saveImage(string $b64): string
    {
        if (str_contains($b64, ',')) {
            $b64 = substr($b64, strpos($b64, ',') + 1);
        }
        $bin = base64_decode($b64, true);
        if ($bin === false || strlen($bin) > 6_000_000) {
            Http::fail(422, 'La imagen no es válida o pesa demasiado (máx. 6 MB).', ['campo' => 'imagen']);
        }
        $info = @getimagesizefromstring($bin);
        if (!$info || !in_array($info[2], [IMAGETYPE_JPEG, IMAGETYPE_PNG], true)) {
            Http::fail(422, 'Solo se permiten imágenes JPG o PNG.', ['campo' => 'imagen']);
        }
        $src = @imagecreatefromstring($bin);
        if (!$src) {
            Http::fail(422, 'No se pudo leer la imagen.', ['campo' => 'imagen']);
        }
        [$w, $h] = [imagesx($src), imagesy($src)];
        $scale = min(1, 800 / max($w, $h));
        $nw = max(1, (int)round($w * $scale));
        $nh = max(1, (int)round($h * $scale));
        $dst = imagecreatetruecolor($nw, $nh);
        imagefill($dst, 0, 0, imagecolorallocate($dst, 255, 255, 255)); // PNG con transparencia → fondo blanco
        imagecopyresampled($dst, $src, 0, 0, 0, 0, $nw, $nh, $w, $h);
        $name = bin2hex(random_bytes(16)) . '.jpg';
        imagejpeg($dst, self::uploadsDir() . '/' . $name, 82);
        return $name;
    }

    public static function deleteAllImages(int $userId): void
    {
        foreach (Db::all('SELECT imagen FROM movimientos WHERE usuario_id = ? AND imagen IS NOT NULL', [$userId]) as $r) {
            self::deleteImage($r['imagen']);
        }
    }

    private static function deleteImage(?string $name): void
    {
        if ($name && preg_match('/^[a-f0-9]{32}\.jpg$/', $name)) {
            @unlink(self::uploadsDir() . '/' . $name);
        }
    }
}
