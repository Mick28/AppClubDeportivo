package com.grupo5.clubdeportivo.util

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Hash de contraseñas con salt por usuario (mejora M07 de la 1ª entrega).
 *
 * El sistema de DSOO guardaba SHA-256 sin salt: dos usuarios con la misma clave
 * quedaban con el mismo hash. Acá cada usuario tiene su propio salt aleatorio y
 * se guarda SHA-256(salt + clave). Nunca se guarda la contraseña en texto plano.
 */
object Seguridad {

    /** Genera un salt aleatorio de 16 bytes, expresado en hexadecimal. */
    fun generarSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return aHex(bytes)
    }

    /** Devuelve el hash hexadecimal de la clave combinada con el salt. */
    fun hashear(clave: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest((salt + clave).toByteArray(Charsets.UTF_8))
        return aHex(bytes)
    }

    private fun aHex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02x".format(it) }
}
