package com.grupo5.clubdeportivo.ui

import android.os.Bundle
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
import com.grupo5.clubdeportivo.util.Sesion
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Registro de visita de no socio (Módulo 5, 3ª entrega). Implementa HU09.
 *
 * El empleado carga los datos del visitante (tipo y número de documento, apellido y nombre),
 * elige la actividad de la cartilla (tabla actividad, mejora M02) y cobra el pase. El importe
 * se pre-carga con el precio de la actividad y la fecha y hora las toma la app con java.time.
 *
 * Accesible para ADMIN y CONSULTA (M06): es un cobro de mostrador, como el de la recepción.
 *
 * Control de socio activo: si el documento pertenece a un socio con baja = 0, se avisa que
 * debe usarse "Cobro de cuota" y no se registra nada.
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

    /** Cartilla de actividades vigentes, en el mismo orden que el Spinner. */
    private var actividades: List<Actividad> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro_no_socio)

        if (Sesion.empleado == null) {
            Toast.makeText(this, "Sesión no válida: volvé a iniciar sesión", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        bd = BaseDatosClub(this)
        configurarBarra(getString(R.string.no_socio_titulo))
        vincularVistas()

        // Cartilla de actividades (Readable Database, semana 11). Sin actividades no hay visita posible.
        actividades = bd.listarActividades()
        if (actividades.isEmpty()) {
            Toast.makeText(this, "No hay actividades vigentes en la cartilla", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        cargarActividades()
    }

    private fun vincularVistas() {
        spTipoDoc = findViewById(R.id.spTipoDoc)
        etNroDoc = findViewById(R.id.etNroDoc)
        etApellido = findViewById(R.id.etApellido)
        etNombre = findViewById(R.id.etNombre)
        spActividad = findViewById(R.id.spActividad)
        etMonto = findViewById(R.id.etMonto)
        tvAyudaMonto = findViewById(R.id.tvAyudaMonto)

        val adaptadorDoc = ArrayAdapter.createFromResource(this, R.array.tipos_documento, R.layout.item_spinner)
        adaptadorDoc.setDropDownViewResource(R.layout.item_spinner)
        spTipoDoc.adapter = adaptadorDoc

        findViewById<Button>(R.id.btnRegistrarVisita).setOnClickListener { validarYRegistrar() }
        findViewById<Button>(R.id.btnCancelar).setOnClickListener { finish() }
    }

    /**
     * Carga el Spinner con la cartilla (Actividad.toString() devuelve el nombre) y, al elegir
     * una actividad, pre-carga el importe con su precio y muestra la unidad de cobro.
     */
    private fun cargarActividades() {
        val adaptador = ArrayAdapter(this, R.layout.item_spinner, actividades)
        adaptador.setDropDownViewResource(R.layout.item_spinner)
        spActividad.adapter = adaptador

        spActividad.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                val act = actividades[pos]
                // Sin decimales si son cero ("12000", no "12000.0"), igual que el monto de la cuota.
                etMonto.setText(if (act.precio % 1.0 == 0.0) act.precio.toLong().toString() else act.precio.toString())
                val unidad = if (act.unidadCobro == "TURNO") "por turno" else "por día"
                tvAyudaMonto.text = "Precio de cartilla: ${Formato.moneda(act.precio)} $unidad"
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    // ------------------------------------------------------------------------------------
    // Validaciones y registro (HU09, M08, M10)
    // ------------------------------------------------------------------------------------

    /** Valida en orden: documento, apellido y nombre, importe > 0; luego el control de socio activo. */
    private fun validarYRegistrar() {
        val tipoDoc = spTipoDoc.selectedItem.toString()
        val nroDoc = etNroDoc.text.toString().trim()
        val apellido = etApellido.text.toString().trim()
        val nombre = etNombre.text.toString().trim()

        if (nroDoc.isEmpty()) { marcarError(etNroDoc, "Ingresá el número de documento"); return }
        // Mismas reglas de formato que el alta de socio (M10).
        if (tipoDoc == "DNI" && !nroDoc.matches(Regex("\\d{7,8}"))) {
            marcarError(etNroDoc, "El DNI debe tener 7 u 8 números"); return
        }
        if (!nroDoc.matches(Regex("\\d{6,9}"))) { marcarError(etNroDoc, "Documento inválido"); return }
        if (apellido.isEmpty()) { marcarError(etApellido, "Dato obligatorio"); return }
        if (nombre.isEmpty()) { marcarError(etNombre, "Dato obligatorio"); return }

        val monto = etMonto.text.toString().replace(",", ".").toDoubleOrNull()
        if (monto == null || monto <= 0) { marcarError(etMonto, "Ingresá un importe mayor a cero"); return }

        ocultarTeclado()

        // A un socio activo no se le cobra el pase: tiene que pagar la cuota.
        if (bd.esSocioActivo(tipoDoc, nroDoc)) {
            AlertDialog.Builder(this)
                .setTitle("Es un socio activo")
                .setMessage(
                    "El documento $tipoDoc $nroDoc pertenece a un socio del club. " +
                        "No corresponde cobrarle un pase diario: usá \"Cobro de cuota\" desde el menú."
                )
                .setPositiveButton("ENTENDIDO", null)
                .show()
            return
        }

        val actividad = actividades[spActividad.selectedItemPosition]
        // Fecha y hora de la visita con java.time (Tema I, 4.3).
        val fechaHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        val id = bd.insertarNoSocio(
            tipoDoc, nroDoc, apellido.uppercase(), nombre, actividad.id, monto, fechaHora
        )

        if (id > 0) {
            AlertDialog.Builder(this)
                .setTitle("Visita registrada")
                .setMessage(
                    "Visitante: ${apellido.uppercase()}, $nombre ($tipoDoc $nroDoc)\n" +
                        "Actividad: ${actividad.nombre}\n" +
                        "Importe: ${Formato.moneda(monto)}"
                )
                .setCancelable(false)
                .setPositiveButton(getString(R.string.btn_volver_menu)) { _, _ -> finish() }
                .show()
        } else {
            // M08: errores visibles.
            AlertDialog.Builder(this)
                .setTitle("No se pudo registrar la visita")
                .setMessage("Ocurrió un error al guardar la visita. No se cobró nada: intentá de nuevo.")
                .setPositiveButton("ENTENDIDO", null)
                .show()
        }
    }

    private fun marcarError(campo: EditText, mensaje: String) {
        campo.error = mensaje
        campo.requestFocus()
    }

    override fun onDestroy() {
        if (::bd.isInitialized) bd.close()
        super.onDestroy()
    }
}
