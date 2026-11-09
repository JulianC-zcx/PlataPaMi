<?php
declare(strict_types=1);

namespace PlataPaMi;

/** Cifrado del número de documento (AES-256-GCM) y utilidades de tokens. */
final class Crypto
{
    private static function key(): string
    {
        $b64 = Env::get('APP_KEY', '');
        $key = base64_decode((string)$b64, true);
        if ($key === false || strlen($key) !== 32) {
            throw new \RuntimeException('APP_KEY inválida: debe ser 32 bytes en base64.');
        }
        return $key;
    }

    public static function encrypt(string $plain): string
    {
        $iv = random_bytes(12);
        $tag = '';
        $cipher = openssl_encrypt($plain, 'aes-256-gcm', self::key(), OPENSSL_RAW_DATA, $iv, $tag);
        if ($cipher === false) {
            throw new \RuntimeException('No se pudo cifrar.');
        }
        return base64_encode($iv . $tag . $cipher);
    }

    public static function decrypt(string $blob): string
    {
        $raw = base64_decode($blob, true);
        if ($raw === false || strlen($raw) < 29) {
            throw new \RuntimeException('Dato cifrado inválido.');
        }
        $plain = openssl_decrypt(substr($raw, 28), 'aes-256-gcm', self::key(), OPENSSL_RAW_DATA, substr($raw, 0, 12), substr($raw, 12, 16));
        if ($plain === false) {
            throw new \RuntimeException('No se pudo descifrar (¿cambió APP_KEY?).');
        }
        return $plain;
    }

    /** Huella determinista del documento para detectar duplicados sin guardarlo en claro. */
    public static function docHash(string $doc): string
    {
        return hash_hmac('sha256', self::normalizeDoc($doc), self::key());
    }

    public static function normalizeDoc(string $doc): string
    {
        return strtoupper(preg_replace('/[\s.\-]/', '', $doc) ?? $doc);
    }

    /** Token aleatorio de 64 hex; se entrega al cliente y solo se guarda su SHA-256. */
    public static function newToken(): array
    {
        $token = bin2hex(random_bytes(32));
        return [$token, hash('sha256', $token)];
    }

    public static function hashToken(string $token): string
    {
        return hash('sha256', $token);
    }
}
