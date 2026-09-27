package com.grupo5.clubdeportivo.model

import com.grupo5.clubdeportivo.R
import java.time.LocalDate

/**
 * Estado del socio. Es el dato que más veces por día se consulta en el mostrador,
 * por eso se comunica con color y texto a la vez (1ª entrega, 3.2):
 * verde = habilitado, rojo = inhabilitado, ámbar = pendiente de apto físico.
 */
enum class EstadoSocio(val etiqueta: String, val colorRes: Int, val fondoRes: Int, val avisoRes: Int) {
    HABILITADO("HABILITADO", R.color.estado_habilitado, R.drawable.bg_badge_habilitado, R.drawable.bg_aviso_habilitado),
    INHABILITADO("INHABILITADO", R.color.estado_inhabilitado, R.drawable.bg_badge_inhabilitado, R.drawable.bg_aviso_inhabilitado),
    SIN_CUOTA("SIN 1ª CUOTA", R.color.estado_inhabilitado, R.drawable.bg_badge_inhabilitado, R.drawable.bg_aviso_inhabilitado),
    PENDIENTE("PENDIENTE", R.color.estado_pendiente, R.drawable.bg_badge_pendiente, R.drawable.bg_aviso_pendiente),
    BAJA("DADO DE BAJA", R.color.texto_secundario, R.drawable.bg_tarjeta, R.drawable.bg_tarjeta);

    companion object {
        /**
         * Reglas del enunciado del Club Deportivo:
         *  - "vencido el período de pago, automáticamente el socio no puede realizar actividades";
         *  - "la inscripción implica completar los datos de la ficha y presentar apto físico".
         * La cuota vencida tiene prioridad sobre el apto físico porque bloquea el ingreso
         * aunque el certificado esté presentado.
         */
        fun calcular(aptoFisico: Boolean, ultimoVencimiento: String?, baja: Boolean, hoy: LocalDate): EstadoSocio {
            if (baja) return BAJA
            if (ultimoVencimiento == null) return SIN_CUOTA
            val vence = LocalDate.parse(ultimoVencimiento)
            if (vence.isBefore(hoy)) return INHABILITADO
            if (!aptoFisico) return PENDIENTE
            return HABILITADO
        }
    }
}
