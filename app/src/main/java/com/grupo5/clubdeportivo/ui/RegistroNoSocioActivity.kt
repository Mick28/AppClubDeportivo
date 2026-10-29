package com.grupo5.clubdeportivo.ui

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.grupo5.clubdeportivo.R
import com.grupo5.clubdeportivo.data.BaseDatosClub
import com.grupo5.clubdeportivo.model.Actividad
import com.grupo5.clubdeportivo.util.Formato
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Registro de visita de no socio (HU09).
 *
 * El empleado ADMIN o CONSULTA puede registrar la visita de una persona que NO es socio.
 * Se selecciona la actividad del catálogo y el importe se pre-completa automáticamente
 * (aunque el empleado puede ajustarlo si hay descuentos). Si el documento ingresado
 * pertenece a un socio activo, se muestra un aviso para cobrar la cuota en cambio.
 */
class RegistroNoSocioActivity : AppCompatActivity() {

    private lateinit var bd: BaseDatosClub

    private lateinit var spTipoDoc: Spinner
    private lateinit var etNroDoc: EditText
    private lateinit var etApellido: EditText
    private lateinit var etNombre: EditText
    private lateinit var spActividad: Spinner
    private lateinit var etMonto: EditText
    private lateinit var tvAyudaMonto: TextView

    private var actividades: List<Actividad> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro_no_socio)

        bd = BaseDatosClub(this)
        configurarBarra(getString(R.string.nosocio_titulo))

        vincularVistas()
        cargarActividades()
        configurarBotones()
        configurarBusquedaPersona()
    }

    private lateinit var tvInfoDoc: TextView

    private fun vincularVistas() {
        spTipoDoc = findViewById(R.id.spTipoDoc)
        etNroDoc = findViewById(R.id.etNroDoc)
        tvInfoDoc = findViewById(R.id.tvInfoDoc)
        etApellido = findViewById(R.id.etApellido)
        etNombre = findViewById(R.id.etNombre)
        spActividad = findViewById(R.id.spActividad)
        etMonto = findViewById(R.id.etMonto)
        tvAyudaMonto = findViewById(R.id.tvAyudaMonto)

        // Spinner de tipo de documento
        val adaptadorTipo = ArrayAdapter.createFromResource(
            this, R.array.tipos_documento, R.layout.item_spinner
        )
        adaptadorTipo.setDropDownViewResource(R.layout.item_spinner)
        spTipoDoc.adapter = adaptadorTipo
    }

    /** Carga la cartilla de actividades vigentes desde la BD y configura el Spinner. */
    private fun cargarActividades() {
        actividades = bd.listarActividades()
        if (actividades.isEmpty()) {
            Toast.makeText(this, "No hay actividades configuradas en el sistema", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val adaptadorAct = ArrayAdapter(this, R.layout.item_spinner, actividades)
        adaptadorAct.setDropDownViewResource(R.layout.item_spinner)
        spActividad.adapter = adaptadorAct

        // Al seleccionar una actividad, pre-completar el importe y la descripción de cobro.
        spActividad.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                val act = actividades[pos]
                etMonto.setText(if (act.precio % 1.0 == 0.0) act.precio.toLong().toString() else act.precio.toString())
                val unidad = if (act.unidadCobro == "TURNO") "por turno" else "por día"
                tvAyudaMonto.text = "Precio de cartilla: ${Formato.moneda(act.precio)} $unidad"
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun configurarBotones() {
        findViewById<Button>(R.id.btnRegistrarVisita).setOnClickListener { registrarVisita() }
        findViewById<Button>(R.id.btnCancelar).setOnClickListener { finish() }
    }

    private var personaAutocompletada = false

    /**
     * Autocompleta automáticamente el apellido y nombre si la persona
     * ya estuvo registrada previamente en la base de datos (como socio o no socio).
     * Si el número de documento no posee registro previo, limpia los campos.
     */
    private fun configurarBusquedaPersona() {
        etNroDoc.setOnFocusChangeListener { _, tieneFoco ->
            if (!tieneFoco) buscarYCompletarPersona()
        }

        etNroDoc.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val texto = s?.toString()?.trim() ?: ""
                if (texto.length in 7..10) {
                    buscarYCompletarPersona()
                } else if (texto.isEmpty() && personaAutocompletada) {
                    limpiarCamposPersona()
                }
            }
        })
    }

    private fun buscarYCompletarPersona() {
        val tipoDoc = spTipoDoc.selectedItem?.toString() ?: return
        val nroDoc = etNroDoc.text.toString().trim()
        if (nroDoc.length < 6) {
            tvInfoDoc.visibility = View.GONE
            if (personaAutocompletada) limpiarCamposPersona()
            return
        }

        val persona = bd.buscarPersonaPorDocumento(tipoDoc, nroDoc)
        if (persona != null) {
            etApellido.setText(persona.apellido)
            etNombre.setText(persona.nombre)
            personaAutocompletada = true
            tvInfoDoc.visibility = View.VISIBLE
            tvInfoDoc.setTextColor(getColor(R.color.estado_habilitado))
            tvInfoDoc.text = "✓ Registrado/a en la base: ${persona.apellido}, ${persona.nombre}"
        } else {
            if (personaAutocompletada) limpiarCamposPersona()
            tvInfoDoc.visibility = View.VISIBLE
            tvInfoDoc.setTextColor(getColor(R.color.texto_secundario))
            tvInfoDoc.text = "ℹ Documento nuevo (sin registro previo)"
        }
    }

    private fun limpiarCamposPersona() {
        etApellido.text.clear()
        etNombre.text.clear()
        personaAutocompletada = false
    }

    // ------------------------------------------------------------------------------------
    // HU09: Registrar visita
    // ------------------------------------------------------------------------------------

    private fun registrarVisita() {
        ocultarTeclado()

        val tipoDoc = spTipoDoc.selectedItem.toString()
        val nroDoc = etNroDoc.text.toString().trim()
        val apellido = etApellido.text.toString().trim()
        val nombre = etNombre.text.toString().trim()
        val monto = etMonto.text.toString().trim().toDoubleOrNull()

        // ---- Validaciones ----
        if (nroDoc.isEmpty()) {
            etNroDoc.error = "Ingresá el número de documento"
            etNroDoc.requestFocus()
            return
        }
        if (apellido.isEmpty()) {
            etApellido.error = "El apellido es obligatorio"
            etApellido.requestFocus()
            return
        }
        if (nombre.isEmpty()) {
            etNombre.error = "El nombre es obligatorio"
            etNombre.requestFocus()
            return
        }
        if (monto == null || monto <= 0) {
            etMonto.error = "Ingresá un importe válido"
            etMonto.requestFocus()
            return
        }

        // ---- Verificar que no sea socio activo ----
        if (bd.esSocioActivo(tipoDoc, nroDoc)) {
            AlertDialog.Builder(this)
                .setTitle("Es un socio activo")
                .setMessage(
                    "El documento ingresado corresponde a un socio activo del club.\n\n" +
                    "Para cobrarle una visita, usá la opción «Cobro de cuota» del menú principal."
                )
                .setPositiveButton("ENTENDIDO", null)
                .show()
            return
        }

        // ---- Insertar registro ----
        val actividad = actividades[spActividad.selectedItemPosition]
        val fechaHora = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

        val id = bd.insertarNoSocio(
            tipoDocumento = tipoDoc,
            nroDocumento = nroDoc,
            apellido = apellido.uppercase(),
            nombre = nombre,
            idActividad = actividad.id,
            montoAbonado = monto,
            fechaHoraVisita = fechaHora
        )

        if (id > 0) {
            AlertDialog.Builder(this)
                .setTitle("Visita registrada")
                .setMessage(
                    "Se registró la visita de $apellido, $nombre.\n\n" +
                    "Actividad: ${actividad.nombre}\n" +
                    "Importe abonado: ${Formato.moneda(monto)}"
                )
                .setPositiveButton("VOLVER AL MENÚ") { _, _ -> finish() }
                .setCancelable(false)
                .show()
        } else {
            AlertDialog.Builder(this)
                .setTitle("No se pudo registrar")
                .setMessage("Ocurrió un error al registrar la visita. Intentá de nuevo.")
                .setPositiveButton("ENTENDIDO", null)
                .show()
        }
    }

    override fun onDestroy() {
        if (::bd.isInitialized) bd.close()
        super.onDestroy()
    }
}
