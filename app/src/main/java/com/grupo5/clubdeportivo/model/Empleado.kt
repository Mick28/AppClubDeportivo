package com.grupo5.clubdeportivo.model

/**
 * Empleado que inició sesión. El rol define qué puede hacer (mejora M06):
 * ADMIN accede a todo; CONSULTA solo puede ver vencimientos y carnets, sin cobrar ni dar altas.
 */
data class Empleado(
    val id: Int,
    val usuario: String,
    val apellido: String,
    val nombre: String,
    val rol: String
) {
    val esAdministrador: Boolean
        get() = rol == ROL_ADMIN

    companion object {
        const val ROL_ADMIN = "ADMIN"
        const val ROL_CONSULTA = "CONSULTA"
    }
}
