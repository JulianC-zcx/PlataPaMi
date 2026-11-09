<?php
declare(strict_types=1);

namespace PlataPaMi;

/** Utilidades de petición/respuesta JSON. */
final class Http
{
    public static function json(int $status, array $payload): never
    {
        http_response_code($status);
        header('Content-Type: application/json; charset=utf-8');
        header('Cache-Control: no-store');
        echo json_encode($payload, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES | JSON_PRESERVE_ZERO_FRACTION);
        exit;
    }

    public static function ok(array $data = [], int $status = 200): never
    {
        self::json($status, ['ok' => true] + $data);
    }

    public static function fail(int $status, string $message, array $extra = []): never
    {
        self::json($status, ['ok' => false, 'error' => $message] + $extra);
    }

    /** Cuerpo JSON de la petición (array vacío si no hay). */
    public static function body(): array
    {
        $raw = file_get_contents('php://input');
        if ($raw === false || $raw === '') {
            return [];
        }
        $data = json_decode($raw, true);
        if (!is_array($data)) {
            self::fail(400, 'El cuerpo de la petición no es un JSON válido.');
        }
        return $data;
    }

    public static function bearer(): ?string
    {
        $h = $_SERVER['HTTP_AUTHORIZATION'] ?? $_SERVER['REDIRECT_HTTP_AUTHORIZATION'] ?? '';
        if ($h === '' && function_exists('getallheaders')) {
            foreach (getallheaders() as $k => $v) {
                if (strtolower($k) === 'authorization') {
                    $h = $v;
                }
            }
        }
        return preg_match('/^Bearer\s+([A-Fa-f0-9]{64})$/', $h, $m) ? strtolower($m[1]) : null;
    }

    public static function ip(): string
    {
        return substr($_SERVER['REMOTE_ADDR'] ?? '', 0, 45);
    }

    public static function str(array $a, string $key, int $max = 255): string
    {
        $v = $a[$key] ?? '';
        $v = is_string($v) ? trim($v) : '';
        return mb_substr($v, 0, $max);
    }
}
