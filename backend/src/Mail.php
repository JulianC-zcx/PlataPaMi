<?php
declare(strict_types=1);

namespace PlataPaMi;

use PHPMailer\PHPMailer\Exception as MailException;
use PHPMailer\PHPMailer\PHPMailer;

/** Envío de correos por SMTP con la identidad visual de PlataPaMi. */
final class Mail
{
    /**
     * @param array<int,array{data:string,name:string,type:string}> $attachments contenido binario en memoria
     * @throws MailException
     */
    public static function send(string $to, string $toName, string $subject, string $innerHtml, string $altText, array $attachments = []): void
    {
        $m = new PHPMailer(true);
        $file = Env::get('MAIL_DRIVER', 'smtp') === 'file';   // desarrollo: guarda el .eml en storage/mail en vez de enviarlo
        if (!$file) {
            $m->isSMTP();
            $m->Host       = (string)Env::get('SMTP_HOST', 'localhost');
            $m->Port       = Env::int('SMTP_PORT', 587);
            $m->SMTPAuth   = true;
            $m->Username   = (string)Env::get('SMTP_USER', '');
            $m->Password   = (string)Env::get('SMTP_PASS', '');
            $secure        = strtolower((string)Env::get('SMTP_SECURE', 'tls'));
            $m->SMTPSecure = $secure === 'ssl' ? PHPMailer::ENCRYPTION_SMTPS : PHPMailer::ENCRYPTION_STARTTLS;
            $m->Timeout    = 20;
        }
        $m->CharSet    = 'UTF-8';
        $m->setFrom((string)Env::get('MAIL_FROM', (string)Env::get('SMTP_USER', 'no-reply@localhost')), (string)Env::get('MAIL_FROM_NAME', 'PlataPaMi'));
        $m->addAddress($to, $toName);
        $m->Subject = $subject;
        $m->isHTML(true);
        $m->Body    = self::layout($subject, $innerHtml);
        $m->AltBody = $altText;
        foreach ($attachments as $a) {
            $m->addStringAttachment($a['data'], $a['name'], PHPMailer::ENCODING_BASE64, $a['type']);
        }
        if ($file) {
            $m->preSend();
            $dir = BASE_PATH . '/storage/mail';
            if (!is_dir($dir)) {
                mkdir($dir, 0755, true);
            }
            file_put_contents($dir . '/' . date('Ymd-His') . '-' . bin2hex(random_bytes(3)) . '.eml', $m->getSentMIMEMessage());
            return;
        }
        $m->send();
    }

    public static function e(string $s): string
    {
        return htmlspecialchars($s, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8');
    }

    public static function button(string $url, string $label): string
    {
        return '<p style="text-align:center;margin:28px 0"><a href="' . self::e($url) . '" '
            . 'style="background:#3AB5FF;color:#ffffff;text-decoration:none;font-weight:bold;padding:14px 32px;'
            . 'border-radius:14px;display:inline-block;font-size:16px">' . self::e($label) . '</a></p>';
    }

    private static function layout(string $title, string $inner): string
    {
        return '<!doctype html><html lang="es"><body style="margin:0;background:#f4fdff;font-family:Poppins,Arial,Helvetica,sans-serif;color:#1b1b1b">'
            . '<table role="presentation" width="100%" cellpadding="0" cellspacing="0"><tr><td align="center" style="padding:24px 12px">'
            . '<table role="presentation" width="560" cellpadding="0" cellspacing="0" style="max-width:560px;width:100%;background:#ffffff;border-radius:28px;overflow:hidden">'
            . '<tr><td style="background:#9CF6FF;padding:28px 32px;border-radius:0 0 40px 40px">'
            . '<div style="font-size:26px;font-weight:bold">PlataPaMi</div>'
            . '<div style="font-size:14px;margin-top:4px">' . self::e($title) . '</div></td></tr>'
            . '<tr><td style="padding:28px 32px;font-size:15px;line-height:1.55">' . $inner . '</td></tr>'
            . '<tr><td style="padding:0 32px 28px;font-size:12px;color:#777">'
            . 'Tu información financiera es confidencial y está protegida conforme a la Ley 1581 de 2012 (Habeas Data). '
            . 'No la compartimos con terceros sin tu autorización.<br>Si no esperabas este correo, puedes ignorarlo.</td></tr>'
            . '</table></td></tr></table></body></html>';
    }
}
