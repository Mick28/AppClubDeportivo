package com.grupo5.clubdeportivo.util

import com.grupo5.clubdeportivo.model.Empleado

/**
 * Datos de la sesión en curso, en memoria mientras la app está abierta.
 * Se usa un object (singleton de Kotlin) para no tener que pasar el empleado
 * logueado por Intent a cada pantalla.
 */
object Sesion {
    /** Empleado logueado, o null si entró un socio o no hay sesión. */
    var empleado: Empleado? = null

    /** Número de socio logueado (perfil Socio), o null. */
    var nroSocio: Int? = null

    fun cerrar() {
        empleado = null
        nroSocio = null
    }
}
