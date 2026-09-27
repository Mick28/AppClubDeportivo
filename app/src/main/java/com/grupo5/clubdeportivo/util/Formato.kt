package com.grupo5.clubdeportivo.util

import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Funciones de formato para mostrar fechas e importes en pantalla.
 *
 * En la base las fechas se guardan como texto ISO (yyyy-MM-dd), que es el formato
 * que SQLite puede comparar y ordenar correctamente. Para mostrarlas al usuario se
 * convierten a dd/MM/yyyy.
 */
object Formato {

    private val LOCALE_AR = Locale("es", "AR")
    private val FECHA_CORTA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val FECHA_LARGA: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", LOCALE_AR)

    /** "2026-09-27" -> "27/09/2026". Si el texto no es una fecha válida, lo devuelve igual. */
    fun fecha(iso: String?): String {
        if (iso.isNullOrBlank()) return "—"
        return try {
            LocalDate.parse(iso).format(FECHA_CORTA)
        } catch (e: Exception) {
            iso
        }
    }

    fun fecha(fecha: LocalDate): String = fecha.format(FECHA_CORTA)

    /** Fecha larga con el día de la semana: "Domingo 27 de septiembre de 2026". */
    fun fechaLarga(fecha: LocalDate): String =
        fecha.format(FECHA_LARGA).replaceFirstChar { it.titlecase(LOCALE_AR) }

    /** 18000.0 -> "$ 18.000" (formato de moneda argentino, sin decimales si son cero). */
    fun moneda(monto: Double): String {
        val nf = NumberFormat.getNumberInstance(LOCALE_AR)
        nf.minimumFractionDigits = if (monto % 1.0 == 0.0) 0 else 2
        nf.maximumFractionDigits = 2
        return "$ " + nf.format(monto)
    }

    /** Número de comprobante con ceros a la izquierda: 12 -> "000012". */
    fun nroComprobante(numero: Long): String = "%06d".format(numero)

    /** Saludo según la hora del día, como en la pantalla de Inicio del prototipo. */
    fun saludo(hora: LocalTime = LocalTime.now()): String = when (hora.hour) {
        in 6..12 -> "Buenos días"
        in 13..19 -> "Buenas tardes"
        else -> "Buenas noches"
    }
}
