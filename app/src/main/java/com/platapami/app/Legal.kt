package com.platapami.app

/**
 * Términos y Condiciones + Política de Tratamiento de Datos (Habeas Data, Ley 1581 de 2012).
 * IMPORTANTE: es un texto base para el proyecto académico; antes de publicar la app en producción
 * conviene que un abogado lo revise y que actualices CORREO_CONTACTO.
 */
object Legal {
    const val VERSION = "1.0"
    /** Canal oficial para consultas, reclamos y ejercicio de derechos del titular. CÁMBIALO por el correo real de soporte. */
    const val CORREO_CONTACTO = "julianan.cortesac@ecci.edu.co"

    data class Seccion(val titulo: String, val texto: String)

    val secciones: List<Seccion> = listOf(
        Seccion(
            "1. Quiénes somos y qué es PlataPaMi",
            "PlataPaMi es una aplicación móvil desarrollada por estudiantes de la Universidad ECCI (Colombia) que te ayuda a registrar tus " +
                "entradas y gastos, ver cuánto te queda en el mes mediante un gráfico circular y recibir tu extracto mensual por correo. " +
                "Al crear una cuenta aceptas estos Términos y Condiciones y la Política de Tratamiento de Datos Personales de este documento."
        ),
        Seccion(
            "2. Uso de la aplicación",
            "• Debes ser mayor de edad (o contar con autorización de tus padres o representantes) y entregar información veraz.\n" +
                "• Eres responsable de la confidencialidad de tu contraseña y de lo que ocurra con tu cuenta.\n" +
                "• PlataPaMi es una herramienta informativa de control personal: no es un banco, no mueve dinero y sus extractos " +
                "no son certificados bancarios ni asesoría financiera.\n" +
                "• No puedes usar la app para actividades ilegales, intentar acceder a cuentas de otras personas o afectar el servicio."
        ),
        Seccion(
            "3. Tu información financiera",
            "Los movimientos que registras (nombre, valor, fecha, categoría y comprobante opcional) son tuyos. Los usamos únicamente para " +
                "mostrarte tu presupuesto, el gráfico, la lista de registros, las alertas de gasto y tu extracto. " +
                "Cuando tus gastos del mes llegan al 80 % de tus entradas te enviaremos un aviso por correo; al terminar el mes " +
                "podemos enviarte tu extracto automáticamente."
        ),
        Seccion(
            "4. Autorización y Política de Tratamiento de Datos (Habeas Data)",
            "En cumplimiento de la Ley 1581 de 2012, el Decreto 1377 de 2013 (compilado en el Decreto 1074 de 2015) y demás normas " +
                "de protección de datos personales de Colombia, al aceptar autorizas de manera previa, expresa e informada el tratamiento " +
                "de tus datos por parte de PlataPaMi, que actúa como Responsable del Tratamiento."
        ),
        Seccion(
            "5. Datos que tratamos y para qué",
            "Datos: nombre, tipo y número de documento, correo electrónico, contraseña (almacenada cifrada, nunca en texto plano) y " +
                "los movimientos financieros que registres.\n\nFinalidades:\n" +
                "• Crear y administrar tu cuenta y autenticarte.\n" +
                "• Restablecer tu contraseña mediante un enlace enviado a tu correo.\n" +
                "• Calcular y mostrarte tu presupuesto, gráficos y listados.\n" +
                "• Enviarte alertas de gasto y tu extracto mensual en PDF, protegido con tu número de documento como contraseña.\n" +
                "• Cumplir obligaciones legales y atender tus consultas y reclamos."
        ),
        Seccion(
            "6. Seguridad y no divulgación",
            "Las conexiones viajan cifradas (HTTPS), las contraseñas se guardan con algoritmos de cifrado de un solo sentido y el número " +
                "de documento se almacena cifrado. Tu información financiera no se vende, no se comparte ni se transfiere a terceros sin tu " +
                "autorización, salvo orden de autoridad competente. Solo usamos proveedores técnicos indispensables (por ejemplo, el servicio " +
                "de correo y el servidor en la nube) bajo deberes de confidencialidad."
        ),
        Seccion(
            "7. Tus derechos como titular",
            "Puedes en cualquier momento: (a) conocer, actualizar y rectificar tus datos; (b) solicitar prueba de la autorización dada; " +
                "(c) ser informado del uso que se ha dado a tus datos; (d) presentar quejas ante la Superintendencia de Industria y Comercio; " +
                "(e) revocar la autorización y solicitar la supresión de tus datos cuando no se respeten los principios y garantías legales; " +
                "y (f) acceder gratuitamente a tus datos. Para ejercerlos escribe a $CORREO_CONTACTO; responderemos dentro de los plazos " +
                "legales (10 días hábiles para consultas y 15 días hábiles para reclamos)."
        ),
        Seccion(
            "8. Conservación y cambios",
            "Conservamos tus datos mientras tengas una cuenta activa o mientras la ley lo exija. Si cambiamos estos términos te avisaremos " +
                "en la app y, cuando corresponda, volveremos a pedir tu aceptación. Versión $VERSION."
        ),
        Seccion(
            "9. Aceptación",
            "Si pulsas “Acepto” confirmas que leíste y entendiste estos Términos y Condiciones y que autorizas el tratamiento de tus datos " +
                "personales en los términos de la Ley de Habeas Data. Si pulsas “No acepto” no podrás crear la cuenta, porque sin esta " +
                "autorización no es posible prestar el servicio."
        )
    )
}
