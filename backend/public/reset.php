<?php
declare(strict_types=1);

/**
 * Página web a la que lleva el enlace del correo "Restablece tu contraseña".
 * El token viaja en la URL, solo sirve una vez y vence según RESET_TTL_MIN.
 */

use PlataPaMi\{Auth, Mail};

require dirname(__DIR__) . '/src/bootstrap.php';

header('Content-Type: text/html; charset=utf-8');
header('Cache-Control: no-store');
header('Referrer-Policy: no-referrer');

$token = (string)($_GET['token'] ?? $_POST['token'] ?? '');
$estado = 'form';   // form | ok | invalido
$error = '';

$row = Auth::findResetToken($token);
if (!$row) {
    $estado = 'invalido';
} elseif ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $p1 = (string)($_POST['password'] ?? '');
    $p2 = (string)($_POST['password2'] ?? '');
    if ($err = Auth::validatePassword($p1)) {
        $error = $err;
    } elseif ($p1 !== $p2) {
        $error = 'Las contraseñas no coinciden.';
    } elseif (Auth::resetPassword($token, $p1)) {
        $estado = 'ok';
    } else {
        $estado = 'invalido';
    }
}
$e = static fn(string $s): string => Mail::e($s);
?>
<!doctype html>
<html lang="es">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="robots" content="noindex">
<title>Restablecer contraseña · PlataPaMi</title>
<link href="https://fonts.googleapis.com/css2?family=Poppins:wght@300;400;700&display=swap" rel="stylesheet">
<style>
  :root { --cyan:#9CF6FF; --sky:#3AB5FF; --teal:#0099B8; --pink:#C93567; --green:#00C060; --cream:#FFFEF8; }
  * { box-sizing: border-box; }
  body { margin:0; font-family:'Poppins',Arial,sans-serif; background:#fff; color:#1b1b1b; }
  header { background:var(--cyan); padding:56px 32px 40px; border-radius:0 0 48px 48px; }
  header h1 { margin:0; font-size:34px; }
  header p { margin:4px 0 0; }
  main { max-width:440px; margin:0 auto; padding:32px; }
  h2 { font-size:22px; margin:0 0 6px; }
  label { display:block; font-size:13px; margin:16px 0 6px; font-weight:400; }
  input[type=password] { width:100%; padding:15px 18px; font:inherit; background:var(--cream); border:2px solid transparent; border-radius:20px; outline:none; }
  input[type=password]:focus { border-color:var(--teal); }
  button { width:100%; margin-top:24px; padding:14px; font:inherit; font-size:20px; font-weight:300; color:#fff; background:var(--sky); border:0; border-radius:14px; cursor:pointer; }
  .err { color:var(--pink); font-size:13px; margin-top:12px; }
  .hint { font-size:12px; color:#666; margin-top:6px; }
  .ok { color:var(--green); }
</style>
</head>
<body>
<header><h1>PlataPaMi</h1><p>Tu plata, bajo control 💸</p></header>
<main>
<?php if ($estado === 'ok'): ?>
  <h2 class="ok">¡Contraseña actualizada!</h2>
  <p>Ya puedes volver a la app e iniciar sesión con tu nueva contraseña. Por seguridad cerramos tus sesiones abiertas.</p>
<?php elseif ($estado === 'invalido'): ?>
  <h2>Enlace no válido</h2>
  <p>Este enlace ya se usó o venció. Vuelve a la app, toca <b>“¿Olvidaste tu contraseña?”</b> y pide uno nuevo.</p>
<?php else: ?>
  <h2>Nueva contraseña</h2>
  <p>Hola <b><?= $e($row['nombre']) ?></b>, elige tu nueva contraseña.</p>
  <form method="post" autocomplete="off">
    <input type="hidden" name="token" value="<?= $e($token) ?>">
    <label for="p1">Nueva contraseña</label>
    <input id="p1" type="password" name="password" minlength="8" maxlength="72" required autofocus>
    <div class="hint">Mínimo 8 caracteres, con letras y números.</div>
    <label for="p2">Repite la contraseña</label>
    <input id="p2" type="password" name="password2" minlength="8" maxlength="72" required>
    <?php if ($error): ?><div class="err"><?= $e($error) ?></div><?php endif; ?>
    <button type="submit">Guardar contraseña</button>
  </form>
<?php endif; ?>
</main>
</body>
</html>
