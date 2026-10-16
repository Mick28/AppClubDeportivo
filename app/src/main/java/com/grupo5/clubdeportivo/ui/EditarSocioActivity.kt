package com.grupo5.clubdeportivo.ui

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.grupo5.clubdeportivo.R
import com.grupo5.clubdeportivo.data.BaseDatosClub
import com.grupo5.clubdeportivo.model.SocioDetalle
import com.grupo5.clubdeportivo.util.Sesion

/**
 * Editar datos de contacto y baja lógica de un socio (HU03 y M01).
 *
 * El empleado ADMIN primero busca al socio (igual que en el cobro: por número o por
 * documento) y luego puede:
 *  – Modificar el teléfono y/o el correo electrónico (HU03).
 *  – Dar de baja al socio con una confirmación por AlertDialog (M01).
 *
 * Solo accede el rol ADMIN; CONSULTA no puede modificar datos.
 */
class EditarSocioActivity : AppCompatActivity() {

    private lateinit var bd: BaseDatosClub

    // Sección de búsqueda
    private lateinit var llBusqueda: LinearLayout
    private lateinit var rbPorNumero: RadioButton
    private lateinit var etNroSocio: EditText
    private lateinit var llPorDocumento: LinearLayout
    private lateinit var spTipoDoc: Spinner
    private lateinit var etNroDoc: EditText

    // Sección del formulario (visible solo después de encontrar al socio)
    private lateinit var llFormulario: LinearLayout
    private lateinit var tvSocioNombre: TextView
    private lateinit var tvSocioDatos: TextView
    private lateinit var tvEstado: TextView
    private lateinit var etTelefono: EditText
    private lateinit var etEmail: EditText
    private lateinit var btnDarDeBaja: Button

    /** Socio encontrado; null hasta que se busca exitosamente. */
    private var socio: SocioDetalle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_socio)

        // Solo el empleado ADMIN puede editar datos de un socio.
        if (Sesion.empleado?.esAdministrador != true) {
            Toast.makeText(this, "Tu usuario no tiene permiso para editar socios", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        bd = BaseDatosClub(this)
        configurarBarra(getString(R.string.editar_titulo))

        vincularVistas()
        configurarBusqueda()
        configurarBotones()
    }

    private fun vincularVistas() {
        llBusqueda = findViewById(R.id.llBusqueda)
        rbPorNumero = findViewById(R.id.rbPorNumero)
        etNroSocio = findViewById(R.id.etNroSocio)
        llPorDocumento = findViewById(R.id.llPorDocumento)
        spTipoDoc = findViewById(R.id.spTipoDoc)
        etNroDoc = findViewById(R.id.etNroDoc)
        llFormulario = findViewById(R.id.llFormulario)
        tvSocioNombre = findViewById(R.id.tvSocioNombre)
        tvSocioDatos = findViewById(R.id.tvSocioDatos)
        tvEstado = findViewById(R.id.tvEstado)
        etTelefono = findViewById(R.id.etTelefono)
        etEmail = findViewById(R.id.etEmail)
        btnDarDeBaja = findViewById(R.id.btnDarDeBaja)

        val adaptador = ArrayAdapter.createFromResource(this, R.array.tipos_documento, R.layout.item_spinner)
        adaptador.setDropDownViewResource(R.layout.item_spinner)
        spTipoDoc.adapter = adaptador
    }

    private fun configurarBusqueda() {
        // Alterna entre buscar por número de socio o por tipo + nro de documento.
        findViewById<RadioGroup>(R.id.rgCriterio).setOnCheckedChangeListener { _, idMarcado ->
            val porNumero = idMarcado == R.id.rbPorNumero
            etNroSocio.visibility = if (porNumero) View.VISIBLE else View.GONE
            llPorDocumento.visibility = if (porNumero) View.GONE else View.VISIBLE
        }

        // Buscar al presionar "Enter" en el teclado.
        val buscarConTeclado = TextView.OnEditorActionListener { _, accion, _ ->
            if (accion == EditorInfo.IME_ACTION_SEARCH) { buscarSocio(); true } else false
        }
        etNroSocio.setOnEditorActionListener(buscarConTeclado)
        etNroDoc.setOnEditorActionListener(buscarConTeclado)

        findViewById<Button>(R.id.btnBuscar).setOnClickListener { buscarSocio() }
    }

    private fun configurarBotones() {
        findViewById<Button>(R.id.btnGuardar).setOnClickListener { guardarCambios() }
        btnDarDeBaja.setOnClickListener { confirmarBaja() }
        findViewById<Button>(R.id.btnCancelar).setOnClickListener { finish() }
    }

    // ------------------------------------------------------------------------------------
    // Búsqueda del socio
    // ------------------------------------------------------------------------------------

    private fun buscarSocio() {
        ocultarTeclado()
        val encontrado: SocioDetalle?
        if (rbPorNumero.isChecked) {
            val nro = etNroSocio.text.toString().toIntOrNull()
            if (nro == null) {
                etNroSocio.error = "Ingresá el número de socio"
                etNroSocio.requestFocus()
                return
            }
            encontrado = bd.buscarSocioPorNumero(nro)
        } else {
            val nroDoc = etNroDoc.text.toString().trim()
            if (nroDoc.isEmpty()) {
                etNroDoc.error = "Ingresá el número de documento"
                etNroDoc.requestFocus()
                return
            }
            encontrado = bd.buscarSocioPorDocumento(spTipoDoc.selectedItem.toString(), nroDoc)
        }

        if (encontrado == null) {
            llFormulario.visibility = View.GONE
            socio = null
            Toast.makeText(this, "No se encontró ningún socio con ese dato", Toast.LENGTH_SHORT).show()
            return
        }
        mostrarFormulario(encontrado)
    }

    /** Completa el formulario con los datos actuales del socio. */
    private fun mostrarFormulario(s: SocioDetalle) {
        socio = s
        tvSocioNombre.text = s.apellidoYNombre
        tvSocioDatos.text = "Socio N° ${s.nroSocio} · ${s.documento}"
        tvEstado.mostrarEstado(s.estado())

        // Pre-completar los campos editables con los valores actuales.
        etTelefono.setText(s.telefono)
        etEmail.setText(s.email)

        // Botón de baja: solo si el socio no está dado de baja ya.
        btnDarDeBaja.visibility = if (!s.baja) View.VISIBLE else View.GONE
        if (s.baja) {
            Toast.makeText(this, "Este socio ya está dado de baja", Toast.LENGTH_LONG).show()
        }

        llBusqueda.visibility = View.GONE
        llFormulario.visibility = View.VISIBLE
    }

    // ------------------------------------------------------------------------------------
    // HU03: Actualizar datos de contacto
    // ------------------------------------------------------------------------------------

    private fun guardarCambios() {
        val s = socio ?: return
        ocultarTeclado()

        val telefono = etTelefono.text.toString().trim()
        val email = etEmail.text.toString().trim()

        // Validaciones de formato (mismas reglas que en el alta de socio).
        if (telefono.isEmpty()) {
            etTelefono.error = "El teléfono es obligatorio"
            etTelefono.requestFocus()
            return
        }
        if (!telefono.matches(Regex("[0-9 +()\\-]{6,20}"))) {
            etTelefono.error = "Solo números, espacios, guiones o paréntesis"
            etTelefono.requestFocus()
            return
        }
        if (email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.error = "Ingresá un correo válido"
            etEmail.requestFocus()
            return
        }

        val ok = bd.actualizarContactoSocio(s.nroSocio, telefono, email)
        if (ok) {
            Toast.makeText(this, "Datos actualizados correctamente", Toast.LENGTH_SHORT).show()
            // Recargar los datos del socio para reflejar los cambios.
            val actualizado = bd.buscarSocioPorNumero(s.nroSocio)
            if (actualizado != null) mostrarFormulario(actualizado)
        } else {
            AlertDialog.Builder(this)
                .setTitle("No se pudo actualizar")
                .setMessage("Ocurrió un error al guardar los cambios. Intentá de nuevo.")
                .setPositiveButton("ENTENDIDO", null)
                .show()
        }
    }

    // ------------------------------------------------------------------------------------
    // M01: Baja lógica del socio
    // ------------------------------------------------------------------------------------

    /** Muestra un AlertDialog de confirmación antes de ejecutar la baja. */
    private fun confirmarBaja() {
        val s = socio ?: return
        AlertDialog.Builder(this)
            .setTitle("Dar de baja al socio")
            .setMessage(
                "¿Dar de baja permanente a ${s.apellidoYNombre}?\n\n" +
                    "El socio no podrá volver a iniciar sesión ni figurará en los " +
                    "vencimientos del día. Su historial se conserva en la base.\n\n" +
                    "Esta acción no se puede deshacer."
            )
            .setPositiveButton("DAR DE BAJA") { _, _ -> ejecutarBaja(s) }
            .setNegativeButton("CANCELAR", null)
            .show()
    }

    private fun ejecutarBaja(s: SocioDetalle) {
        val ok = bd.darDeBajaSocio(s.nroSocio)
        if (ok) {
            Toast.makeText(this, "${s.apellidoYNombre} fue dado/a de baja", Toast.LENGTH_SHORT).show()
            // Actualizar la pantalla para reflejar el estado de baja.
            val actualizado = bd.buscarSocioPorNumero(s.nroSocio)
            if (actualizado != null) {
                mostrarFormulario(actualizado)
                tvEstado.mostrarEstado(actualizado.estado())
            }
        } else {
            AlertDialog.Builder(this)
                .setTitle("No se pudo dar de baja")
                .setMessage("Ocurrió un error al procesar la baja. Intentá de nuevo.")
                .setPositiveButton("ENTENDIDO", null)
                .show()
        }
    }

    override fun onDestroy() {
        if (::bd.isInitialized) bd.close()
        super.onDestroy()
    }
}
