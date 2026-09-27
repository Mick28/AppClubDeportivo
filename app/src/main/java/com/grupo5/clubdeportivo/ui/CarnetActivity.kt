package com.grupo5.clubdeportivo.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.grupo5.clubdeportivo.R
import com.grupo5.clubdeportivo.data.BaseDatosClub
import com.grupo5.clubdeportivo.model.EstadoSocio
import com.grupo5.clubdeportivo.util.Formato
import com.grupo5.clubdeportivo.util.Sesion
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Carnet digital del socio (Módulo 2, Tema II 9). El diseño es igual para todos y cambian
 * los datos: nombre y apellido y, como dato extra, la fecha de vencimiento de la cuota.
 *
 * Dos modos de uso:
 *  - EMPLEADO: lo abre después del alta y el cobro (o tocando un socio en el listado).
 *    Puede registrar el apto físico o ir a cobrar si la cuota está vencida.
 *  - SOCIO: es la única pantalla a la que accede el socio, después de validar su alias
 *    y contraseña con SELECT COUNT(*) en el login.
 *
 * Los datos se buscan con una consulta que proyecta lo que necesita el diseño.
 * Se reemplaza el carnet en PDF de DSOO por una credencial en pantalla (mejora M13).
 */
class CarnetActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NRO_SOCIO = "nroSocio"
        const val EXTRA_MODO = "modo"
        const val MODO_EMPLEADO = "EMPLEADO"
        const val MODO_SOCIO = "SOCIO"
    }

    private lateinit var bd: BaseDatosClub
    private var nroSocio = -1
    private var modoSocio = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_carnet)

        nroSocio = intent.getIntExtra(EXTRA_NRO_SOCIO, -1)
        modoSocio = intent.getStringExtra(EXTRA_MODO) == MODO_SOCIO

        // Control de acceso: el socio solo puede ver SU carnet; el empleado debe estar logueado.
        val autorizado = if (modoSocio) Sesion.nroSocio == nroSocio else Sesion.empleado != null
        if (nroSocio <= 0 || !autorizado) {
            Toast.makeText(this, "Sesión no válida: volvé a iniciar sesión", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        bd = BaseDatosClub(this)
        configurarBarra(getString(R.string.carnet_titulo), mostrarVolver = !modoSocio)

        val btnCerrarSesion = findViewById<Button>(R.id.btnCerrarSesion)
        if (modoSocio) {
            btnCerrarSesion.visibility = View.VISIBLE
            btnCerrarSesion.setOnClickListener { finish() }
        }

        findViewById<Button>(R.id.btnRegistrarApto).setOnClickListener { confirmarAptoFisico() }
        findViewById<Button>(R.id.btnCobrarDesdeCarnet).setOnClickListener {
            val intent = Intent(this, CobroCuotaActivity::class.java)
            intent.putExtra(CobroCuotaActivity.EXTRA_NRO_SOCIO, nroSocio)
            startActivity(intent)
        }
    }

    /** Se recarga al volver (por ejemplo, después de cobrar desde acá). */
    override fun onResume() {
        super.onResume()
        if (::bd.isInitialized) mostrarCarnet()
    }

    private fun mostrarCarnet() {
        val esAdmin = Sesion.empleado?.esAdministrador == true
        val tvSinCarnet = findViewById<TextView>(R.id.tvSinCarnet)
        val llCarnet = findViewById<LinearLayout>(R.id.llCarnet)
        val tvMotivo = findViewById<TextView>(R.id.tvMotivoEstado)
        val btnApto = findViewById<Button>(R.id.btnRegistrarApto)
        val btnCobrar = findViewById<Button>(R.id.btnCobrarDesdeCarnet)
        val tvBienvenida = findViewById<TextView>(R.id.tvBienvenidaSocio)

        val carnet = bd.obtenerCarnet(nroSocio)
        if (carnet == null) {
            // El carnet se genera con el primer pago: un socio registrado sin pagar no lo tiene.
            val socio = bd.buscarSocioPorNumero(nroSocio)
            llCarnet.visibility = View.GONE
            tvMotivo.visibility = View.GONE
            btnApto.visibility = View.GONE
            tvSinCarnet.visibility = View.VISIBLE
            if (modoSocio) {
                tvBienvenida.text = "Hola, ${socio?.nombre ?: ""}"
                tvBienvenida.visibility = View.VISIBLE
                tvSinCarnet.text = "Tu carnet se emite cuando abonás la primera cuota. " +
                    "Acercate al mostrador del club para completar el pago."
            } else {
                tvSinCarnet.text = "${socio?.apellidoYNombre ?: "El socio"} todavía no abonó la primera cuota. " +
                    "El carnet se genera con ese pago."
                btnCobrar.visibility = if (esAdmin) View.VISIBLE else View.GONE
            }
            return
        }

        val socio = carnet.socio
        val hoy = LocalDate.now()
        val estado = socio.estado(hoy)

        tvSinCarnet.visibility = View.GONE
        llCarnet.visibility = View.VISIBLE
        if (modoSocio) {
            tvBienvenida.text = "Hola, ${socio.nombre}"
            tvBienvenida.visibility = View.VISIBLE
        }
        findViewById<TextView>(R.id.tvApellidoCarnet).text = socio.apellido.uppercase()
        findViewById<TextView>(R.id.tvNombreCarnet).text = socio.nombre
        findViewById<TextView>(R.id.tvDocumentoCarnet).text = socio.documento
        findViewById<TextView>(R.id.tvNroSocioCarnet).text = "%04d".format(socio.nroSocio)
        findViewById<TextView>(R.id.tvVencCuotaCarnet).text = Formato.fecha(socio.ultimoVencimiento)
        findViewById<TextView>(R.id.tvVigenciaCarnet).text =
            "Carnet N° ${carnet.nroCarnet} · Válido hasta ${Formato.fecha(carnet.fechaVencimientoCarnet)}"
        findViewById<TextView>(R.id.tvEstadoCarnet).mostrarEstado(estado)

        // Motivo del estado, escrito para quien lo está leyendo (socio o empleado).
        var motivo = when (estado) {
            EstadoSocio.HABILITADO ->
                if (modoSocio) "Tu cuota está al día: podés ingresar al club y realizar actividades."
                else "Cuota al día: puede ingresar al club y realizar actividades."
            EstadoSocio.INHABILITADO -> {
                val vence = LocalDate.parse(socio.ultimoVencimiento)
                val dias = ChronoUnit.DAYS.between(vence, hoy)
                "Cuota vencida el ${Formato.fecha(vence)} ($dias día(s)). " +
                    if (modoSocio) "Para retomar las actividades abonala en el mostrador."
                    else "Para retomar las actividades tiene que abonar la cuota."
            }
            EstadoSocio.PENDIENTE ->
                if (modoSocio) "Falta presentar tu apto físico. Hasta entonces no podés realizar actividades."
                else "Falta el apto físico. Hasta presentarlo no puede realizar actividades."
            else -> "El socio fue dado de baja."
        }
        if (LocalDate.parse(carnet.fechaVencimientoCarnet).isBefore(hoy)) {
            motivo += "\nEl carnet está vencido: se renueva con el próximo pago."
        }
        tvMotivo.mostrarAviso(motivo, estado)

        // Acciones del empleado administrador.
        btnApto.visibility = if (!modoSocio && esAdmin && !socio.aptoFisico) View.VISIBLE else View.GONE
        btnCobrar.visibility =
            if (!modoSocio && esAdmin && estado == EstadoSocio.INHABILITADO) View.VISIBLE else View.GONE
    }

    /** HU04: registrar la presentación del apto físico, con confirmación previa. */
    private fun confirmarAptoFisico() {
        AlertDialog.Builder(this)
            .setTitle("Registrar apto físico")
            .setMessage("¿Confirmás que el socio presentó el certificado de apto físico?")
            .setPositiveButton("CONFIRMAR") { _, _ ->
                if (bd.registrarAptoFisico(nroSocio)) {
                    Toast.makeText(this, "Apto físico registrado", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "No se pudo registrar el apto físico", Toast.LENGTH_LONG).show()
                }
                mostrarCarnet()
            }
            .setNegativeButton("CANCELAR", null)
            .show()
    }

    override fun onDestroy() {
        // Cuando el socio sale de su carnet (botón o atrás), se cierra su sesión.
        if (modoSocio && isFinishing && Sesion.nroSocio == nroSocio) Sesion.cerrar()
        if (::bd.isInitialized) bd.close()
        super.onDestroy()
    }
}
