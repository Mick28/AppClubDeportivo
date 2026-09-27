package com.grupo5.clubdeportivo.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.grupo5.clubdeportivo.R
import com.grupo5.clubdeportivo.data.BaseDatosClub
import com.grupo5.clubdeportivo.util.Formato
import com.grupo5.clubdeportivo.util.Sesion
import java.time.LocalDate

/**
 * Menú Principal del empleado (Módulo 1): Registro de socio, Cobro de cuota,
 * Ver vencimientos y Salir. Muestra además la tarjeta con los vencimientos del día.
 *
 * Control de acceso por rol (mejora M06): el rol CONSULTA no puede registrar ni cobrar.
 */
class MenuPrincipalActivity : AppCompatActivity() {

    private lateinit var bd: BaseDatosClub
    private lateinit var tvCantVencenHoy: TextView
    private lateinit var tvTextoVencenHoy: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_principal)

        val empleado = Sesion.empleado
        if (empleado == null) {
            // Sin sesión (por ejemplo, Android cerró el proceso): se vuelve a la pantalla inicial.
            volverAlInicio()
            return
        }
        bd = BaseDatosClub(this)

        findViewById<TextView>(R.id.tvSaludo).text =
            "${Formato.saludo()},\n${empleado.nombre.uppercase()}"
        findViewById<TextView>(R.id.tvFecha).text = Formato.fechaLarga(LocalDate.now())
        findViewById<TextView>(R.id.tvRol).text = empleado.rol
        tvCantVencenHoy = findViewById(R.id.tvCantVencenHoy)
        tvTextoVencenHoy = findViewById(R.id.tvTextoVencenHoy)

        val btnRegistro = findViewById<Button>(R.id.btnRegistroSocio)
        val btnCobro = findViewById<Button>(R.id.btnCobroCuota)
        val btnVencimientos = findViewById<Button>(R.id.btnVerVencimientos)
        val cardVencimientos = findViewById<LinearLayout>(R.id.cardVencimientos)
        val btnSalir = findViewById<Button>(R.id.btnSalir)

        btnRegistro.setOnClickListener {
            startActivity(Intent(this, RegistroSocioActivity::class.java))
        }
        btnCobro.setOnClickListener {
            startActivity(Intent(this, CobroCuotaActivity::class.java))
        }
        val abrirVencimientos = View.OnClickListener {
            startActivity(Intent(this, VencimientosActivity::class.java))
        }
        btnVencimientos.setOnClickListener(abrirVencimientos)
        cardVencimientos.setOnClickListener(abrirVencimientos)

        btnSalir.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Salir")
                .setMessage("¿Querés cerrar la sesión?")
                .setPositiveButton("SALIR") { _, _ ->
                    Sesion.cerrar()
                    volverAlInicio()
                }
                .setNegativeButton("CANCELAR", null)
                .show()
        }

        if (!empleado.esAdministrador) {
            btnRegistro.isEnabled = false
            btnCobro.isEnabled = false
            btnRegistro.alpha = 0.4f
            btnCobro.alpha = 0.4f
            findViewById<TextView>(R.id.tvAvisoRol).visibility = View.VISIBLE
        }
    }

    /** Se refresca al volver de otra pantalla (por ejemplo, después de cobrar una cuota). */
    override fun onResume() {
        super.onResume()
        if (Sesion.empleado == null || !::bd.isInitialized) return
        val cantidad = bd.contarVencenHoy()
        tvCantVencenHoy.text = cantidad.toString()
        tvTextoVencenHoy.text = when (cantidad) {
            0 -> "No hay cuotas que venzan hoy"
            1 -> "socio con cuota que vence hoy"
            else -> "socios con cuota que vence hoy"
        }
    }

    private fun volverAlInicio() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        if (::bd.isInitialized) bd.close()
        super.onDestroy()
    }
}
