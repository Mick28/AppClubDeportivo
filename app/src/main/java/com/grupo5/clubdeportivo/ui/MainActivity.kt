package com.grupo5.clubdeportivo.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.grupo5.clubdeportivo.R
import com.grupo5.clubdeportivo.data.BaseDatosClub

/**
 * Pantalla inicial de la app (Módulo 1): logo, color de fondo y el botón "Ingreso a la App".
 *
 * Además, cada vez que se abre la app se ejecuta el control automático de vencimientos
 * (HU08 / mejora M09): los socios con la cuota vencida pasan a inhabilitados sin depender
 * de que alguien inicie sesión, como ocurría en el sistema de escritorio.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            ejecutarControlDeVencimientos()
        }

        val btnIngreso = findViewById<Button>(R.id.btnIngreso)
        btnIngreso.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    /**
     * Si el control falla, se avisa (mejora M08): un error silencioso dejaría habilitados
     * a socios morosos sin que nadie se entere (defecto detectado en DSOO).
     */
    private fun ejecutarControlDeVencimientos() {
        val bd = BaseDatosClub(this)
        try {
            val inhabilitados = bd.inhabilitarVencidos()
            if (inhabilitados > 0) {
                Toast.makeText(
                    this,
                    "Control de vencimientos: $inhabilitados socio(s) pasaron a inhabilitados",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            AlertDialog.Builder(this)
                .setTitle("Control de vencimientos")
                .setMessage(
                    "No se pudo completar el control automático de vencimientos. " +
                        "Verificá el estado de los socios antes de habilitar el ingreso."
                )
                .setPositiveButton("ENTENDIDO", null)
                .show()
        } finally {
            bd.close()
        }
    }
}
