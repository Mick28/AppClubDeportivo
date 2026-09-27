package com.grupo5.clubdeportivo.model

/** Datos que proyecta la consulta del carnet (Tema II, 9: "realizar la consulta que proyecte lo que necesitás en el diseño"). */
data class Carnet(
    val nroCarnet: Int,
    val socio: SocioDetalle,
    val fechaEmision: String,
    val fechaVencimientoCarnet: String
)
