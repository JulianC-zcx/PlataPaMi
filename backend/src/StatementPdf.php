<?php
declare(strict_types=1);

namespace PlataPaMi;

/**
 * Extracto mensual en PDF (estilo app financiera: cabecera cian redondeada, tarjetas de resumen,
 * dona de gastos y tabla de movimientos) cifrado con el número de documento del usuario.
 */
final class StatementPdf extends \TCPDF
{
    // Paleta de la app (Theme.kt)
    private const CYAN   = [156, 246, 255];
    private const GREEN  = [0, 160, 80];
    private const PINK   = [201, 53, 103];
    private const TEAL   = [0, 153, 184];
    private const SKY    = [58, 181, 255];
    private const INK    = [27, 27, 27];
    private const GREY   = [120, 120, 120];
    private const LINE   = [226, 232, 234];
    private const SLICES = [[83, 109, 254], [201, 53, 103], [0, 153, 184], [58, 181, 255], [0, 192, 96], [244, 184, 108]];

    private string $mesLabel = '';

    /**
     * @param array{usuario:array,mes:string,movimientos:array,entradas:int,gastos:int,saldo:int,por_gasto:array} $d
     * @param string $documento número de documento en claro (será la contraseña del PDF)
     */
    public static function render(array $d, string $documento): string
    {
        $pdf = new self('P', 'mm', 'A4', true, 'UTF-8', false);
        // Fuentes Poppins (convertidas con tools/build_fonts.php) → mismo tipo de letra que la app
        foreach (['poppins_light', 'poppins_', 'poppins_b'] as $f) {
            $pdf->AddFont($f, '', BASE_PATH . '/fonts/tcpdf/' . $f . '.php');
        }
        $pdf->mesLabel = ucfirst(Fmt::monthLong($d['mes']));
        $pdf->SetCreator('PlataPaMi');
        $pdf->SetAuthor('PlataPaMi');
        $pdf->SetTitle('Extracto financiero ' . $pdf->mesLabel);
        $pdf->setPrintHeader(true);
        $pdf->setPrintFooter(true);
        $pdf->SetMargins(15, 30, 15);
        $pdf->SetAutoPageBreak(true, 22);
        // Contraseña = número de documento. AES-128 por compatibilidad con visores móviles.
        $pdf->SetProtection([], $documento, bin2hex(random_bytes(16)), 2);
        $pdf->AddPage();
        $pdf->cover($d);
        $pdf->summaryCards($d);
        $pdf->budgetBar($d);
        $pdf->donut($d);
        $pdf->movementsTable($d);
        $pdf->closing($d);
        return $pdf->Output('extracto.pdf', 'S');
    }

    // ───────────── Cabecera / pie de cada página ─────────────
    public function Header(): void
    {
        if ($this->getPage() === 1) {
            return; // la portada se dibuja a mano
        }
        $this->SetFillColor(...self::CYAN);
        $this->RoundedRect(0, -10, 210, 30, 10, '0011', 'F');
        $this->SetTextColor(...self::INK);
        $this->SetFont('poppins_b', '', 13);
        $this->SetXY(15, 6);
        $this->Cell(80, 8, 'PlataPaMi', 0, 0, 'L');
        $this->SetFont('poppins_', '', 9);
        $this->Cell(100, 8, 'Extracto · ' . $this->mesLabel, 0, 0, 'R');
    }

    public function Footer(): void
    {
        $this->SetY(-16);
        $this->SetDrawColor(...self::LINE);
        $this->Line(15, $this->GetY(), 195, $this->GetY());
        $this->SetFont('poppins_', '', 7.5);
        $this->SetTextColor(...self::GREY);
        $this->SetXY(15, $this->GetY() + 2);
        $this->Cell(130, 5, 'PlataPaMi · Generado el ' . date('d/m/Y H:i') . ' · Documento confidencial', 0, 0, 'L');
        $this->Cell(50, 5, 'Página ' . $this->getAliasNumPage() . ' de ' . $this->getAliasNbPages(), 0, 0, 'R');
    }

    // ───────────── Bloques ─────────────
    private function cover(array $d): void
    {
        $u = $d['usuario'];
        $this->SetFillColor(...self::CYAN);
        $this->RoundedRect(0, -20, 210, 82, 16, '0011', 'F');

        $this->SetTextColor(...self::INK);
        $this->SetFont('poppins_b', '', 22);
        $this->SetXY(15, 12);
        $this->Cell(90, 10, 'PlataPaMi', 0, 0, 'L');
        $this->SetFont('poppins_', '', 9);
        $this->Cell(90, 10, 'EXTRACTO FINANCIERO', 0, 1, 'R');

        $this->SetFont('poppins_b', '', 20);
        $this->SetXY(15, 25);
        $this->Cell(180, 10, $this->mesLabel, 0, 1, 'L');
        $this->SetFont('poppins_', '', 10);
        $this->SetX(15);
        $this->Cell(180, 6, Fmt::monthRange($d['mes']), 0, 1, 'L');

        $this->SetFont('poppins_', '', 9);
        $this->SetXY(15, 44);
        $this->Cell(120, 5, $u['nombre'], 0, 1, 'L');
        $this->SetX(15);
        $this->Cell(120, 5, $u['tipo_documento'] . ' ' . $u['documento_mask'] . '  ·  ' . $u['correo'], 0, 1, 'L');
        $this->SetY(70);
    }

    private function summaryCards(array $d): void
    {
        $y = $this->GetY();
        $cards = [
            ['Entradas', $d['entradas'], [232, 250, 239], self::GREEN],
            ['Gastos', $d['gastos'], [255, 233, 239], self::PINK],
            ['Saldo del mes', $d['saldo'], [224, 251, 255], self::TEAL],
        ];
        $w = 57.5;
        foreach ($cards as $i => [$label, $val, $bg, $fg]) {
            $x = 15 + $i * ($w + 4);
            $this->SetFillColor(...$bg);
            $this->RoundedRect($x, $y, $w, 26, 6, '1111', 'F');
            $this->SetTextColor(...$fg);
            $this->SetFont('poppins_', '', 8);
            $this->SetXY($x + 5, $y + 5);
            $this->Cell($w - 10, 4, $label, 0, 0, 'L');
            $this->SetFont('poppins_b', '', 12);
            $this->SetXY($x + 5, $y + 12);
            $this->Cell($w - 10, 8, Fmt::cop($val), 0, 0, 'L');
        }
        $this->SetY($y + 34);
    }

    private function budgetBar(array $d): void
    {
        $y = $this->GetY();
        $this->SetTextColor(...self::INK);
        $this->SetFont('poppins_b', '', 10);
        $this->SetXY(15, $y);
        $this->Cell(100, 6, 'Uso de tu presupuesto', 0, 0, 'L');

        $ratio = $d['entradas'] > 0 ? $d['gastos'] / $d['entradas'] : ($d['gastos'] > 0 ? 1.0 : 0.0);
        $color = $ratio > 1 ? self::PINK : ($ratio >= 0.8 ? [244, 150, 40] : self::GREEN);
        $this->SetFont('poppins_', '', 9);
        $this->SetTextColor(...$color);
        $label = $d['entradas'] > 0 ? number_format($ratio * 100, 1, ',', '.') . '% de tus entradas' : ($d['gastos'] > 0 ? 'Sin entradas registradas' : 'Sin gastos');
        $this->Cell(80, 6, $label, 0, 1, 'R');

        $this->SetFillColor(...self::LINE);
        $this->RoundedRect(15, $y + 9, 180, 4, 2, '1111', 'F');
        if ($ratio > 0) {
            $this->SetFillColor(...$color);
            $this->RoundedRect(15, $y + 9, max(3, 180 * min(1, $ratio)), 4, 2, '1111', 'F');
        }
        $this->SetY($y + 22);
    }

    private function donut(array $d): void
    {
        $this->ensureSpace(66);
        $y = $this->GetY();
        $this->SetTextColor(...self::INK);
        $this->SetFont('poppins_b', '', 10);
        $this->SetXY(15, $y);
        $this->Cell(180, 6, 'En qué se fue tu dinero', 0, 1, 'L');

        $cx = 45;
        $cy = $y + 36;
        $r  = 26;
        $items = $d['por_gasto'];           // [['nombre'=>..., 'monto'=>...], ...] ya agrupado (top 5 + otros)
        $total = max(1, array_sum(array_column($items, 'monto')) + max(0, $d['saldo']));

        if ($d['gastos'] <= 0) {
            $this->SetFillColor(...self::LINE);
            $this->PieSector($cx, $cy, $r, 0, 359.9, 'F', true, 90);
        } else {
            $start = 0.0;
            $slices = $items;
            if ($d['saldo'] > 0) {
                $slices[] = ['nombre' => 'Saldo disponible', 'monto' => $d['saldo'], 'saldo' => true];
            }
            foreach ($slices as $i => $s) {
                $sweep = 360 * $s['monto'] / $total;
                $col = isset($s['saldo']) ? self::SLICES[5] : self::SLICES[$i % 5];
                $this->SetFillColor(...$col);
                if ($sweep >= 359.9) {
                    $this->PieSector($cx, $cy, $r, 0, 359.9, 'F', true, 90);
                } elseif ($sweep > 0.05) {
                    $this->PieSector($cx, $cy, $r, $start, $start + $sweep, 'F', true, 90);
                }
                $start += $sweep;
            }
        }
        // Agujero blanco de la dona
        $this->SetFillColor(255, 255, 255);
        $this->Circle($cx, $cy, 14.5, 0, 360, 'F');

        // Leyenda
        $lx = 85;
        $ly = $y + 12;
        $this->SetFont('poppins_', '', 8.5);
        if ($d['gastos'] <= 0) {
            $this->SetTextColor(...self::GREY);
            $this->SetXY($lx, $ly + 14);
            $this->Cell(100, 6, 'Este mes no registraste gastos.', 0, 0, 'L');
        } else {
            $slices = $items;
            if ($d['saldo'] > 0) {
                $slices[] = ['nombre' => 'Saldo disponible', 'monto' => $d['saldo'], 'saldo' => true];
            }
            foreach (array_slice($slices, 0, 7) as $i => $s) {
                $col = isset($s['saldo']) ? self::SLICES[5] : self::SLICES[$i % 5];
                $this->SetFillColor(...$col);
                $this->Circle($lx + 1.5, $ly + 3, 1.6, 0, 360, 'F');
                $this->SetTextColor(...self::INK);
                $this->SetXY($lx + 6, $ly);
                $this->Cell(45, 6, $this->shorten($s['nombre'], 24), 0, 0, 'L');
                $this->SetTextColor(...self::GREY);
                $this->Cell(14, 6, ($s['monto'] * 100 / $total < 0.1 ? '<0,1' : number_format($s['monto'] * 100 / $total, 1, ',', '.')) . '%', 0, 0, 'R');
                $this->SetTextColor(...self::INK);
                $this->Cell(36, 6, Fmt::cop((int)$s['monto']), 0, 0, 'R');
                $ly += 7.2;
            }
        }
        $this->SetY($y + 70);
    }

    private function movementsTable(array $d): void
    {
        $this->ensureSpace(40);
        $this->SetTextColor(...self::INK);
        $this->SetFont('poppins_b', '', 10);
        $this->Cell(180, 8, 'Movimientos del mes', 0, 1, 'L');

        $cols = [['Fecha', 26, 'L'], ['Detalle', 82, 'L'], ['Tipo', 22, 'C'], ['Valor', 50, 'R']];
        $drawHead = function () use ($cols): void {
            $this->SetFillColor(...self::CYAN);
            $this->SetTextColor(...self::INK);
            $this->SetFont('poppins_b', '', 8);
            $this->SetX(15);
            foreach ($cols as $i => [$t, $w, $a]) {
                $this->Cell($w, 8, ($a === 'L' ? '  ' : '') . $t . ($a === 'R' ? '  ' : ''), 0, $i === 3 ? 1 : 0, $a, true);
            }
        };
        $drawHead();

        if (!$d['movimientos']) {
            $this->SetFont('poppins_', '', 9);
            $this->SetTextColor(...self::GREY);
            $this->SetX(15);
            $this->Cell(180, 14, 'No hay movimientos registrados en este mes.', 0, 1, 'C');
            return;
        }

        $zebra = false;
        foreach ($d['movimientos'] as $m) {
            if ($this->GetY() > 297 - 22 - 12) {
                $this->AddPage();
                $drawHead();
            }
            $y = $this->GetY();
            $entrada = $m['tipo'] === 'entrada';
            if ($zebra) {
                $this->SetFillColor(247, 252, 253);
                $this->Rect(15, $y, 180, 11, 'F');
            }
            $zebra = !$zebra;

            $this->SetTextColor(...self::GREY);
            $this->SetFont('poppins_', '', 8);
            $this->SetXY(15, $y);
            $this->Cell(26, 11, '  ' . Fmt::date($m['fecha']), 0, 0, 'L');

            $this->SetTextColor(...self::INK);
            $this->SetFont('poppins_b', '', 8.5);
            $this->SetXY(41, $y + 1.2);
            $this->Cell(82, 4.6, $this->shorten($m['nombre'], 42), 0, 0, 'L');
            $this->SetFont('poppins_light', '', 7.5);
            $this->SetTextColor(...self::GREY);
            $this->SetXY(41, $y + 5.4);
            $this->Cell(82, 4, $this->shorten($m['detalle'] !== '' ? $m['detalle'] : ucfirst($m['categoria']), 52), 0, 0, 'L');

            // Pastilla de tipo
            $this->SetFillColor(...($entrada ? [232, 250, 239] : [255, 233, 239]));
            $this->RoundedRect(127, $y + 3, 18, 5, 2.5, '1111', 'F');
            $this->SetTextColor(...($entrada ? self::GREEN : self::PINK));
            $this->SetFont('poppins_b', '', 6.5);
            $this->SetXY(127, $y + 3);
            $this->Cell(18, 5, $entrada ? 'ENTRADA' : 'GASTO', 0, 0, 'C');

            $this->SetFont('poppins_b', '', 9);
            $this->SetXY(145, $y);
            $this->Cell(48, 11, ($entrada ? '+ ' : '− ') . Fmt::cop((int)$m['monto']), 0, 0, 'R');

            $this->SetDrawColor(...self::LINE);
            $this->Line(15, $y + 11, 195, $y + 11);
            $this->SetY($y + 11);
        }
    }

    private function closing(array $d): void
    {
        $this->ensureSpace(62);
        $this->Ln(6);
        $y = $this->GetY();
        $this->SetFillColor(...self::CYAN);
        $this->RoundedRect(15, $y, 180, 36, 8, '1111', 'F');
        $rows = [['Total entradas', '+ ' . Fmt::cop($d['entradas']), self::GREEN], ['Total gastos', '− ' . Fmt::cop($d['gastos']), self::PINK]];
        $yy = $y + 5;
        foreach ($rows as [$l, $v, $c]) {
            $this->SetFont('poppins_', '', 9);
            $this->SetTextColor(...self::INK);
            $this->SetXY(23, $yy);
            $this->Cell(80, 6, $l, 0, 0, 'L');
            $this->SetFont('poppins_b', '', 9.5);
            $this->SetTextColor(...$c);
            $this->Cell(82, 6, $v, 0, 0, 'R');
            $yy += 7;
        }
        $this->SetDrawColor(...self::INK);
        $this->Line(23, $yy + 0.5, 187, $yy + 0.5);
        $this->SetFont('poppins_b', '', 11);
        $this->SetTextColor(...self::TEAL);
        $this->SetXY(23, $yy + 2);
        $this->Cell(80, 8, 'SALDO FINAL', 0, 0, 'L');
        $this->SetTextColor(...self::INK);
        $this->Cell(82, 8, Fmt::cop($d['saldo']), 0, 1, 'R');

        $this->SetY($y + 42);
        $this->SetFont('poppins_light', '', 7);
        $this->SetTextColor(...self::GREY);
        $this->SetX(15);
        $this->MultiCell(
            180, 4,
            'Este extracto fue generado automáticamente por PlataPaMi con la información que registraste. Es un resumen informativo de tus finanzas personales y no constituye un certificado bancario. '
            . 'Tus datos personales y financieros están protegidos conforme a la Ley 1581 de 2012 (Habeas Data) y no se comparten con terceros sin tu autorización. '
            . 'Este archivo está cifrado: su contraseña es tu número de documento.',
            0, 'L'
        );
    }

    // ───────────── Utilidades ─────────────
    private function ensureSpace(float $h): void
    {
        if ($this->GetY() + $h > 297 - 22) {
            $this->AddPage();
        }
    }

    private function shorten(string $s, int $max): string
    {
        return mb_strlen($s) > $max ? mb_substr($s, 0, $max - 1) . '…' : $s;
    }
}
