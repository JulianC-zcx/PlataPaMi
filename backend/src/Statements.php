<?php
declare(strict_types=1);

namespace PlataPaMi;

/** Arma los datos del extracto mensual, genera el PDF cifrado y lo envía por correo. */
final class Statements
{
    /** @return array{usuario:array,mes:string,movimientos:array,entradas:int,gastos:int,saldo:int,por_gasto:array} */
    public static function data(array $user, string $ym): array
    {
        $movs = Db::all(
            'SELECT * FROM movimientos WHERE usuario_id = ? AND fecha BETWEEN ? AND ? ORDER BY fecha ASC, id ASC',
            [$user['id'], ...Fmt::monthBounds($ym)]
        );
        $s = Movements::summary((int)$user['id'], $ym);

        // Gastos agrupados por nombre: los 5 mayores + "Otros"
        $agr = [];
        foreach ($movs as $m) {
            if ($m['tipo'] === 'gasto') {
                $k = mb_strtolower(trim($m['nombre']));
                $agr[$k] = ['nombre' => $agr[$k]['nombre'] ?? $m['nombre'], 'monto' => ($agr[$k]['monto'] ?? 0) + (int)$m['monto']];
            }
        }
        usort($agr, static fn($a, $b) => $b['monto'] <=> $a['monto']);
        $top = array_slice($agr, 0, 5);
        if (count($agr) > 5) {
            $top[] = ['nombre' => 'Otros', 'monto' => array_sum(array_column(array_slice($agr, 5), 'monto'))];
        }

        return [
            'usuario'     => Auth::publicUser((int)$user['id']),
            'mes'         => $ym,
            'movimientos' => $movs,
            'entradas'    => $s['entradas'],
            'gastos'      => $s['gastos'],
            'saldo'       => $s['saldo'],
            'por_gasto'   => $top,
        ];
    }

    public static function pdf(array $user, string $ym): string
    {
        return StatementPdf::render(self::data($user, $ym), Crypto::decrypt((string)$user['documento_enc']));
    }

    /**
     * Genera el PDF protegido y lo manda al correo del usuario.
     * @throws \Throwable si falla el SMTP
     */
    public static function send(array $user, string $ym, string $origen = 'manual'): void
    {
        $d   = self::data($user, $ym);
        $pdf = StatementPdf::render($d, Crypto::decrypt((string)$user['documento_enc']));
        $mes = Fmt::monthLong($ym);
        $row = static fn(string $l, string $v, string $c) =>
            "<tr><td style=\"padding:9px 0;border-bottom:1px solid #eee\">{$l}</td><td align=\"right\" style=\"padding:9px 0;border-bottom:1px solid #eee;font-weight:bold;color:{$c}\">{$v}</td></tr>";

        Mail::send(
            $user['correo'], $user['nombre'], 'Tu extracto de ' . $mes,
            '<p>Hola <b>' . Mail::e($user['nombre']) . '</b>,</p>'
            . "<p>Aquí está tu extracto financiero de <b>{$mes}</b>.</p>"
            . '<table width="100%" cellpadding="0" cellspacing="0" style="margin:12px 0">'
            . $row('Entradas', Fmt::cop($d['entradas']), '#00A050')
            . $row('Gastos', Fmt::cop($d['gastos']), '#C93567')
            . $row('Saldo', Fmt::cop($d['saldo']), '#0099B8')
            . '</table>'
            . '<p style="background:#FFFEF8;border:1px solid #9CF6FF;border-radius:14px;padding:12px 16px">'
            . '🔒 El PDF adjunto está protegido. Para abrirlo, escribe como contraseña tu <b>número de documento</b> '
            . '(solo números, sin puntos ni espacios).</p>',
            "Tu extracto de {$mes}. Entradas " . Fmt::cop($d['entradas']) . ', gastos ' . Fmt::cop($d['gastos']) . ', saldo ' . Fmt::cop($d['saldo'])
            . '. El PDF adjunto se abre con tu número de documento.',
            [['data' => $pdf, 'name' => 'PlataPaMi_Extracto_' . $ym . '.pdf', 'type' => 'application/pdf']]
        );

        Db::q('INSERT INTO extractos_enviados (usuario_id, mes, origen) VALUES (?,?,?)', [$user['id'], $ym, $origen]);
        Db::q(
            'INSERT INTO notificaciones (usuario_id, tipo, titulo, mensaje, mes) VALUES (?,?,?,?,?)
             ON DUPLICATE KEY UPDATE creado_at = NOW(), leida = 0',
            [$user['id'], 'extracto', 'Tu extracto de ' . $mes . ' está listo',
             'Te lo enviamos a ' . $user['correo'] . '. Ábrelo con tu número de documento.', $ym]
        );
    }
}
