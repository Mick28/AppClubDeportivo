package com.grupo5.clubdeportivo.model

/**
 * Actividad del club: natación, musculación, etc. (cartilla de precios, mejora M02).
 * Se usa en el Spinner de la pantalla de registro de no socio para seleccionar la actividad
 * que va a realizar el visitante y pre-completar el importe a cobrar.
 */
data class Actividad(
    val id: Int,
    val nombre: String,
    val precio: Double,
    val unidadCobro: String   // "DIA" | "TURNO"
) {
    /**
     * Texto que muestra el Spinner: solo el nombre, para mantenerlo legible.
     * El precio se muestra en el campo de importe al seleccionar la actividad.
     */
    override fun toString(): String = nombre
}
