package com.grupo5.clubdeportivo.model

/**
 * Clase de modelo que representa una fila del listado diario de vencimientos
 * (Módulo 3). La consigna de la entrega final pide un RecyclerView con una clase
 * de modelo Socio: cada fila del listado es un objeto con nombre y tipo, en lugar
 * del List<List<String>> del ejemplo del libro (Tema III, 12.3).
 *
 * @property id               número de socio (clave primaria de la tabla socio)
 * @property apellido         apellido del socio
 * @property nombre           nombre del socio
 * @property fechaVencimiento fecha de vencimiento de la última cuota paga (ISO yyyy-MM-dd)
 * @property telefono         teléfono de contacto, para llamarlo (dato que el cliente pidió conservar)
 * @property email            correo de contacto
 */
data class Socio(
    val id: Int,
    val apellido: String,
    val nombre: String,
    val fechaVencimiento: String,
    val telefono: String = "",
    val email: String = ""
) {
    /** "RUIZ, Marta": apellido en mayúsculas, igual que en el prototipo de Figma. */
    val apellidoYNombre: String
        get() = "${apellido.uppercase()}, $nombre"
}
