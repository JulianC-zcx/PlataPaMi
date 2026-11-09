<?php
declare(strict_types=1);

use PlataPaMi\Env;

define('BASE_PATH', dirname(__DIR__));

$autoload = BASE_PATH . '/vendor/autoload.php';
if (!is_file($autoload)) {
    http_response_code(500);
    header('Content-Type: application/json; charset=utf-8');
    echo json_encode(['ok' => false, 'error' => 'Servidor sin dependencias: ejecuta "composer install".']);
    exit;
}
require $autoload;

Env::load(BASE_PATH . '/.env');
date_default_timezone_set('America/Bogota');
mb_internal_encoding('UTF-8');

if (Env::get('APP_DEBUG', '0') === '1') {
    ini_set('display_errors', '1');
    error_reporting(E_ALL);
} else {
    ini_set('display_errors', '0');
    error_reporting(E_ALL & ~E_DEPRECATED);
}
ini_set('log_errors', '1');
ini_set('error_log', BASE_PATH . '/storage/logs/php-error.log');
