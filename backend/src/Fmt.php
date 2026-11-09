<?php
declare(strict_types=1);

namespace PlataPaMi;

/** Formatos en español de Colombia. */
final class Fmt
{
    public const MESES = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre'];
    public const MES3  = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];

    public static function cop(int $n): string
    {
        return ($n < 0 ? '-' : '') . '$ ' . number_format(abs($n), 0, ',', '.');
    }

    public static function isMonth(string $ym): bool
    {
        return (bool)preg_match('/^\d{4}-(0[1-9]|1[0-2])$/', $ym);
    }

    public static function currentMonth(): string
    {
        return date('Y-m');
    }

    public static function previousMonth(): string
    {
        return date('Y-m', strtotime('first day of last month'));
    }

    /** "agosto de 2026" */
    public static function monthLong(string $ym): string
    {
        [$y, $m] = array_map('intval', explode('-', $ym));
        return self::MESES[$m - 1] . ' de ' . $y;
    }

    /** "01 ago – 31 ago 2026" */
    public static function monthRange(string $ym): string
    {
        [$y, $m] = array_map('intval', explode('-', $ym));
        $last = (int)date('t', mktime(0, 0, 0, $m, 1, $y));
        $a = self::MES3[$m - 1];
        return sprintf('01 %s – %02d %s %d', $a, $last, $a, $y);
    }

    public static function date(string $ymd): string
    {
        [$y, $m, $d] = array_map('intval', explode('-', $ymd));
        return sprintf('%02d %s %d', $d, self::MES3[$m - 1], $y);
    }

    public static function monthBounds(string $ym): array
    {
        return [$ym . '-01', date('Y-m-t', strtotime($ym . '-01'))];
    }
}
