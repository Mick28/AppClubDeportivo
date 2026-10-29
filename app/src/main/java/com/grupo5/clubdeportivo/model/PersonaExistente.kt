package com.grupo5.clubdeportivo.model

/**
 * Representa los datos personales de una persona previamente registrada
 * en el sistema (como socio, ex-socio o visitante no socio).
 */
data class PersonaExistente(
    val idPersona: Long,
    val tipoDocumento: String,
    val nroDocumento: String,
    val apellido: String,
    val nombre: String,
    val fechaNacimiento: String? = null,
    val telefono: String? = null,
    val email: String? = null
)
