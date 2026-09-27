package com.grupo5.clubdeportivo.model

/**
 * Resultado de registrar un cobro de cuota: lo que se necesita para armar el recibo.
 *
 * @property nroComprobante  número de la operación (nro_cuota en la base), usado como número del recibo
 * @property fechaInicio     primer día del período que cubre el pago (ISO)
 * @property fechaVencimiento último día del período que cubre el pago (ISO)
 * @property carnetEmitido   true si con este pago se generó el carnet (primer pago del socio)
 */
data class ResultadoPago(
    val nroComprobante: Long,
    val fechaPago: String,
    val fechaInicio: String,
    val fechaVencimiento: String,
    val carnetEmitido: Boolean
)
