<?php
// Convierte las fuentes Poppins (TTF) al formato de TCPDF. Solo hace falta correrlo si cambias las fuentes:
//   php tools/build_fonts.php
declare(strict_types=1);
define('BASE_PATH', dirname(__DIR__));
define('K_PATH_FONTS', BASE_PATH . '/fonts/tcpdf/');
require BASE_PATH . '/vendor/autoload.php';
foreach (['poppins_light', 'poppins_regular', 'poppins_bold'] as $f) {
    $name = TCPDF_FONTS::addTTFfont(BASE_PATH . "/fonts/src/$f.ttf", 'TrueTypeUnicode', '', 96, K_PATH_FONTS);
    echo "$f => $name\n";
}
