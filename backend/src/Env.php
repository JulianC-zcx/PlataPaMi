<?php
declare(strict_types=1);

namespace PlataPaMi;

/** Lector mínimo de archivos .env (KEY=valor, # comentarios). */
final class Env
{
    /** @var array<string,string> */
    private static array $vars = [];

    public static function load(string $path): void
    {
        if (!is_file($path)) {
            return;
        }
        foreach (file($path, FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES) ?: [] as $line) {
            $line = trim($line);
            if ($line === '' || $line[0] === '#' || !str_contains($line, '=')) {
                continue;
            }
            [$k, $v] = explode('=', $line, 2);
            $v = trim($v);
            // Quita comentario al final cuando el valor no está entre comillas
            if ($v !== '' && $v[0] !== '"' && $v[0] !== "'") {
                $v = trim(preg_replace('/\s+#.*$/', '', $v) ?? $v);
            }
            $v = trim($v, "\"'");
            self::$vars[trim($k)] = $v;
        }
    }

    public static function get(string $key, ?string $default = null): ?string
    {
        return self::$vars[$key] ?? ($_ENV[$key] ?? (getenv($key) !== false ? (string)getenv($key) : $default));
    }

    public static function set(string $key, string $value): void
    {
        self::$vars[$key] = $value;
    }

    public static function int(string $key, int $default): int
    {
        $v = self::get($key);
        return $v !== null && is_numeric($v) ? (int)$v : $default;
    }
}
