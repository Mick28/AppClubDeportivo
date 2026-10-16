package com.grupo5.clubdeportivo.model

/**
 * Actividad de la cartilla del club (tabla actividad, mejora M02).
 * Alimenta el Spinner del registro de visita de no socio (Módulo 5, HU09).
 *
 * @property id          clave primaria de la tabla actividad
 * @property nombre      nombre que ve el empleado en el Spinner
 * @property precio      precio de cartilla; pre-carga el importe de la visita
 * @property unidadCobro "DIA" o "TURNO": se muestra como ayuda debajo del importe
 */
data class Actividad(
    val id: Int,
    val nombre: String,
    val precio: Double,
    val unidadCobro: String
) {
    /** El ArrayAdapter del Spinner usa toString(): así se muestra solo el nombre. */
    override fun toString(): String = nombre
}
