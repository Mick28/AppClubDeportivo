package com.grupo5.clubdeportivo.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.grupo5.clubdeportivo.R
import com.grupo5.clubdeportivo.data.BaseDatosClub
import com.grupo5.clubdeportivo.util.Sesion

/**
 * Inicio de sesión (Módulo 1): usuario, contraseña y perfil (Empleado / Socio).
 *
 * Lógica de autenticación por rol (libro PF · Iniciar proyecto en Kotlin):
 *  - EMPLEADO: se valida contra la tabla empleado y se abre el Menú Principal.
 *  - SOCIO: se valida alias + contraseña con SELECT COUNT(*) y se abre SOLO su carnet.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var bd: BaseDatosClub
    private lateinit var rbSocio: RadioButton
    private lateinit var tvEtiquetaUsuario: TextView
    private lateinit var etUsuario: EditText
    private lateinit var etClave: EditText
    private lateinit var tvError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        bd = BaseDatosClub(this)

        val rgPerfil = findViewById<RadioGroup>(R.id.rgPerfil)
        rbSocio = findViewById(R.id.rbSocio)
        tvEtiquetaUsuario = findViewById(R.id.tvEtiquetaUsuario)
        etUsuario = findViewById(R.id.etUsuario)
        etClave = findViewById(R.id.etClave)
        tvError = findViewById(R.id.tvError)
        val btnIngresar = findViewById<Button>(R.id.btnIngresar)

        // Al cambiar de perfil cambia la etiqueta: el empleado usa "usuario" y el socio su "alias".
        rgPerfil.setOnCheckedChangeListener { _, idMarcado ->
            val esSocio = idMarcado == R.id.rbSocio
            val etiqueta = getString(if (esSocio) R.string.hint_alias else R.string.hint_usuario)
            tvEtiquetaUsuario.text = etiqueta
            etUsuario.hint = etiqueta
            tvError.visibility = View.GONE
        }

        btnIngresar.setOnClickListener { ingresar() }
    }

    private fun ingresar() {
        ocultarTeclado()
        val usuario = etUsuario.text.toString().trim()
        val clave = etClave.text.toString()

        if (usuario.isEmpty()) {
            mostrarError("Ingresá tu ${if (rbSocio.isChecked) "alias" else "usuario"}.")
            etUsuario.requestFocus()
            return
        }
        if (clave.isEmpty()) {
            mostrarError("Ingresá tu contraseña.")
            etClave.requestFocus()
            return
        }

        if (rbSocio.isChecked) ingresarComoSocio(usuario.lowercase(), clave)
        else ingresarComoEmpleado(usuario, clave)
    }

    private fun ingresarComoEmpleado(usuario: String, clave: String) {
        val empleado = bd.validarEmpleado(usuario, clave)
        if (empleado == null) {
            // HU01, escenario 2: no se precisa cuál de los dos datos falló.
            mostrarError("Usuario o contraseña incorrectos.")
            return
        }
        Sesion.cerrar()
        Sesion.empleado = empleado
        startActivity(Intent(this, MenuPrincipalActivity::class.java))
        finish()
    }

    private fun ingresarComoSocio(alias: String, clave: String) {
        val nroSocio = bd.validarSocio(alias, clave)
        if (nroSocio == null) {
            mostrarError("Alias o contraseña incorrectos.")
            return
        }
        Sesion.cerrar()
        Sesion.nroSocio = nroSocio
        // El socio solo accede a la visualización de su carnet (Tema I 5.1 y Tema II 9).
        val intent = Intent(this, CarnetActivity::class.java)
        intent.putExtra(CarnetActivity.EXTRA_NRO_SOCIO, nroSocio)
        intent.putExtra(CarnetActivity.EXTRA_MODO, CarnetActivity.MODO_SOCIO)
        startActivity(intent)
        etClave.text.clear()
        tvError.visibility = View.GONE
    }

    private fun mostrarError(mensaje: String) {
        tvError.text = mensaje
        tvError.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        bd.close()
        super.onDestroy()
    }
}
