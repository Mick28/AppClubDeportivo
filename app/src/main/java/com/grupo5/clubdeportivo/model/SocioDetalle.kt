package com.grupo5.clubdeportivo.model

import java.time.LocalDate

/**
 * Datos completos de un socio que se usan en el cobro de cuota y en el carnet.
 *
 * @property ultimoVencimiento fecha de vencimiento de la última cuota paga (ISO), o null si
 *                             el socio todavía no abonó ninguna cuota.
 */
data class SocioDetalle(
    val nroSocio: Int,
    val tipoDocumento: String,
    val nroDocumento: String,
    val apellido: String,
    val nombre: String,
    val telefono: String,
    val email: String,
    val fechaInscripcion: String,
    val aptoFisico: Boolean,
    val baja: Boolean,
    val ultimoVencimiento: String?
) {
    val apellidoYNombre: String
        get() = "${apellido.uppercase()}, $nombre"

    val documento: String
        get() = "$tipoDocumento $nroDocumento"

    /** Estado del socio calculado a la fecha indicada (por defecto, hoy). */
    fun estado(hoy: LocalDate = LocalDate.now()): EstadoSocio =
        EstadoSocio.calcular(aptoFisico, ultimoVencimiento, baja, hoy)
}
