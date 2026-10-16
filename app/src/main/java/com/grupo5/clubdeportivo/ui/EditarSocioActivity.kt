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
 * Edición de datos de contacto y baja lógica del socio (Módulo 4, 3ª entrega).
 *
 * Cubre HU03 (modificar datos de contacto) y la mejora M01 (ABM de socios con baja lógica).
 * Reutiliza el flujo buscar → actuar del cobro de cuota (Módulo 2): primero se encuentra
 * al socio por número o por tipo y número de documento (Tema II, 8.3) y después se muestra
 * su ficha con el estado y los campos editables.
 *
 * Solo se editan teléfono y correo: documento, apellido, nombre y fecha de nacimiento son
 * datos de identidad y no se modifican desde la app.
 *
 * Control de acceso por rol (M06): solo ADMIN. Si un empleado CONSULTA llega a la pantalla
 * por otro camino, se le informa y la Activity termina.
 */
class EditarSocioActivity : AppCompatActivity() {

    private lateinit var bd: BaseDatosClub
    private lateinit var llBusqueda: LinearLayout
    private lateinit var llFormulario: LinearLayout
    private lateinit var rbPorNumero: RadioButton
    private lateinit var etNroSocio: EditText
    private lateinit var llPorDocumento: LinearLayout
    private lateinit var spTipoDoc: Spinner
    private lateinit var etNroDoc: EditText
    private lateinit var tvSocioNombre: TextView
    private lateinit var tvSocioDatos: TextView
    private lateinit var tvEstado: TextView
    private lateinit var etTelefono: EditText
    private lateinit var etEmail: EditText
    private lateinit var btnDarDeBaja: Button

    /** Socio cuya ficha se está mostrando (null hasta encontrarlo). */
    private var socio: SocioDetalle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_socio)

        // M06: el rol CONSULTA no edita socios (el botón del menú ya está deshabilitado).
        if (Sesion.empleado?.esAdministrador != true) {
            Toast.makeText(this, "Tu usuario no tiene permiso para editar socios", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        bd = BaseDatosClub(this)
        configurarBarra(getString(R.string.editar_titulo))
        vincularVistas()
    }

    private fun vincularVistas() {
        llBusqueda = findViewById(R.id.llBusqueda)
        llFormulario = findViewById(R.id.llFormulario)
        rbPorNumero = findViewById(R.id.rbPorNumero)
        etNroSocio = findViewById(R.id.etNroSocio)
        llPorDocumento = findViewById(R.id.llPorDocumento)
        spTipoDoc = findViewById(R.id.spTipoDoc)
        etNroDoc = findViewById(R.id.etNroDoc)
        tvSocioNombre = findViewById(R.id.tvSocioNombre)
        tvSocioDatos = findViewById(R.id.tvSocioDatos)
        tvEstado = findViewById(R.id.tvEstado)
        etTelefono = findViewById(R.id.etTelefono)
        etEmail = findViewById(R.id.etEmail)
        btnDarDeBaja = findViewById(R.id.btnDarDeBaja)

        val adaptador = ArrayAdapter.createFromResource(this, R.array.tipos_documento, R.layout.item_spinner)
        adaptador.setDropDownViewResource(R.layout.item_spinner)
        spTipoDoc.adapter = adaptador

        // Mismo patrón de búsqueda que el cobro de cuota (Módulo 2, Tema II 8.3).
        findViewById<RadioGroup>(R.id.rgCriterio).setOnCheckedChangeListener { _, idMarcado ->
            val porNumero = idMarcado == R.id.rbPorNumero
            etNroSocio.visibility = if (porNumero) View.VISIBLE else View.GONE
            llPorDocumento.visibility = if (porNumero) View.GONE else View.VISIBLE
        }
        val buscarConTeclado = TextView.OnEditorActionListener { _, accion, _ ->
            if (accion == EditorInfo.IME_ACTION_SEARCH) { buscarSocio(); true } else false
        }
        etNroSocio.setOnEditorActionListener(buscarConTeclado)
        etNroDoc.setOnEditorActionListener(buscarConTeclado)
        findViewById<Button>(R.id.btnBuscar).setOnClickListener { buscarSocio() }

        findViewById<Button>(R.id.btnGuardar).setOnClickListener { guardarCambios() }
        btnDarDeBaja.setOnClickListener { confirmarBaja() }
        findViewById<Button>(R.id.btnCancelar).setOnClickListener { finish() }
    }

    // ------------------------------------------------------------------------------------
    // Búsqueda del socio (CRUD Read)
    // ------------------------------------------------------------------------------------

    /**
     * Busca al socio con los métodos existentes del Módulo 2. A diferencia del cobro, acá
     * también se muestra un socio dado de baja (con el distintivo DADO DE BAJA) para poder
     * ver su ficha y corregir su contacto.
     */
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

    /**
     * Muestra la ficha del socio: nombre, número, documento y estado (color + texto), y los
     * campos editables pre-completados con los valores actuales de la tabla persona.
     * El botón de baja solo aparece si el socio todavía no está dado de baja.
     */
    private fun mostrarFormulario(s: SocioDetalle) {
        socio = s
        tvSocioNombre.text = s.apellidoYNombre
        tvSocioDatos.text = "Socio N° ${s.nroSocio} · ${s.documento}"
        tvEstado.mostrarEstado(s.estado())
        etTelefono.setText(s.telefono)
        etEmail.setText(s.email)
        etTelefono.error = null
        etEmail.error = null
        btnDarDeBaja.visibility = if (s.baja) View.GONE else View.VISIBLE

        llBusqueda.visibility = View.GONE
        llFormulario.visibility = View.VISIBLE
    }

    // ------------------------------------------------------------------------------------
    // Edición de contacto (HU03, CRUD Update)
    // ------------------------------------------------------------------------------------

    /**
     * Valida con las mismas reglas del alta (M10) y actualiza teléfono y correo.
     * Cada error se marca en el campo con setError() y recibe el foco.
     */
    private fun guardarCambios() {
        val s = socio ?: return
        val telefono = etTelefono.text.toString().trim()
        val email = etEmail.text.toString().trim()

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

        ocultarTeclado()
        val ok = bd.actualizarContactoSocio(s.nroSocio, telefono, email)
        if (ok) {
            Toast.makeText(this, "Datos actualizados correctamente", Toast.LENGTH_SHORT).show()
            // Se vuelve a leer de la base para que la ficha refleje lo guardado.
            bd.buscarSocioPorNumero(s.nroSocio)?.let { mostrarFormulario(it) }
        } else {
            // M08: errores visibles.
            AlertDialog.Builder(this)
                .setTitle("No se pudieron guardar los cambios")
                .setMessage("Ocurrió un error al actualizar los datos de contacto. Intentá de nuevo.")
                .setPositiveButton("ENTENDIDO", null)
                .show()
        }
    }

    // ------------------------------------------------------------------------------------
    // Baja lógica (M01, AlertDialog de la clase de la semana 8)
    // ------------------------------------------------------------------------------------

    /** Pide confirmación antes de una acción irreversible: título, mensaje y dos botones. */
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
        if (bd.darDeBajaSocio(s.nroSocio)) {
            Toast.makeText(this, "${s.apellidoYNombre} fue dado/a de baja", Toast.LENGTH_LONG).show()
            // Recarga la ficha: distintivo DADO DE BAJA y sin botón de baja.
            bd.buscarSocioPorNumero(s.nroSocio)?.let { mostrarFormulario(it) }
        } else {
            AlertDialog.Builder(this)
                .setTitle("No se pudo dar de baja")
                .setMessage("El socio ya estaba dado de baja o ocurrió un error al guardar. No se hizo ningún cambio.")
                .setPositiveButton("ENTENDIDO", null)
                .show()
        }
    }

    override fun onDestroy() {
        if (::bd.isInitialized) bd.close()
        super.onDestroy()
    }
}
