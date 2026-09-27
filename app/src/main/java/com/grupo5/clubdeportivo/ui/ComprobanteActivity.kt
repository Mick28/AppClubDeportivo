package com.grupo5.clubdeportivo.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.grupo5.clubdeportivo.R
import com.grupo5.clubdeportivo.model.EstadoSocio

/**
 * Documento de pago (Tema II, 8.4): recibo con número, nombre del documento, fecha,
 * emisor, receptor, detalle y monto.
 *
 * Igual que MainDocu en el libro (8.5 y 8.6), no consulta la base: todos los datos llegan
 * desde la activity de cobro en el Intent (putExtra) y se leen con getStringExtra().
 */
class ComprobanteActivity : AppCompatActivity() {

    companion object {
        // Nombres con los que viaja cada dato en el Intent.
        const val NRO_DOCUMENTO = "nroDocumento"
        const val NOMBRE_DOCUMENTO = "nombreDocumento"
        const val FECHA = "fecha"
        const val EMISOR = "emisor"
        const val RECEPTOR = "receptor"
        const val DETALLE = "detalle"
        const val MONTO = "monto"
        const val VENCIMIENTO = "vencimiento"
        const val ESTADO = "estado"
        const val NRO_SOCIO = "nroSocio"
        const val CARNET_EMITIDO = "carnetEmitido"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comprobante)

        // Recuperamos los datos con getStringExtra(), con el mismo nombre usado en putExtra().
        val nroDocumento = intent.getStringExtra(NRO_DOCUMENTO) ?: ""
        val nombreDocumento = intent.getStringExtra(NOMBRE_DOCUMENTO) ?: "RECIBO DE PAGO"
        val fecha = intent.getStringExtra(FECHA) ?: ""
        val emisor = intent.getStringExtra(EMISOR) ?: ""
        val receptor = intent.getStringExtra(RECEPTOR) ?: ""
        val detalle = intent.getStringExtra(DETALLE) ?: ""
        val monto = intent.getStringExtra(MONTO) ?: ""
        val vencimiento = intent.getStringExtra(VENCIMIENTO) ?: ""
        val estado = intent.getStringExtra(ESTADO) ?: EstadoSocio.HABILITADO.name
        val nroSocio = intent.getStringExtra(NRO_SOCIO)?.toIntOrNull() ?: -1
        val carnetEmitido = intent.getStringExtra(CARNET_EMITIDO) == "true"

        findViewById<TextView>(R.id.tvNombreDocumento).text = nombreDocumento
        findViewById<TextView>(R.id.tvNroDocumento).text = "N° $nroDocumento"
        findViewById<TextView>(R.id.tvFechaDocumento).text = "Fecha: $fecha"
        findViewById<TextView>(R.id.tvEmisor).text = emisor
        findViewById<TextView>(R.id.tvReceptor).text = receptor
        findViewById<TextView>(R.id.tvDetalle).text = detalle
        findViewById<TextView>(R.id.tvMonto).text = monto

        // Estado resultante del socio después del pago.
        val estadoSocio = EstadoSocio.valueOf(estado)
        var mensaje = if (estadoSocio == EstadoSocio.HABILITADO) {
            "El socio quedó HABILITADO para realizar actividades hasta el $vencimiento."
        } else {
            "Pago registrado hasta el $vencimiento. El socio queda PENDIENTE hasta presentar el apto físico."
        }
        if (carnetEmitido) mensaje += "\nSe emitió el carnet del socio."
        findViewById<TextView>(R.id.tvEstadoResultante).mostrarAviso(mensaje, estadoSocio)

        val btnVerCarnet = findViewById<Button>(R.id.btnVerCarnet)
        btnVerCarnet.setOnClickListener {
            val intent = Intent(this, CarnetActivity::class.java)
            intent.putExtra(CarnetActivity.EXTRA_NRO_SOCIO, nroSocio)
            intent.putExtra(CarnetActivity.EXTRA_MODO, CarnetActivity.MODO_EMPLEADO)
            startActivity(intent)
        }

        // Intent implícito (Etapa 3): se le pide al sistema una app para compartir el recibo.
        findViewById<Button>(R.id.btnCompartir).setOnClickListener {
            val texto = "$nombreDocumento N° $nroDocumento\nFecha: $fecha\n\nEMISOR\n$emisor\n\n" +
                "RECEPTOR\n$receptor\n\nDETALLE\n$detalle\n\nTOTAL: $monto"
            val compartir = Intent(Intent.ACTION_SEND)
            compartir.type = "text/plain"
            compartir.putExtra(Intent.EXTRA_SUBJECT, "$nombreDocumento N° $nroDocumento")
            compartir.putExtra(Intent.EXTRA_TEXT, texto)
            startActivity(Intent.createChooser(compartir, "Compartir comprobante"))
        }

        findViewById<Button>(R.id.btnVolverMenu).setOnClickListener {
            val intent = Intent(this, MenuPrincipalActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }
}
