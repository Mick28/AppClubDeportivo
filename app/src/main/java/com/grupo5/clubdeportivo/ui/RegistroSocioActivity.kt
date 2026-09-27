package com.grupo5.clubdeportivo.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.grupo5.clubdeportivo.R
import com.grupo5.clubdeportivo.data.BaseDatosClub
import com.grupo5.clubdeportivo.util.Formato
import com.grupo5.clubdeportivo.util.Sesion
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId

/**
 * Registro de socio (Módulo 1). Lo realiza el empleado en el club, no el socio
 * (Tema I, 4: el club necesita datos específicos, no solo correo y contraseña).
 *
 * Orden de validación antes de guardar:
 *  1. Datos obligatorios completos (mismo criterio que GuardarCurso del libro).
 *  2. Formato de documento, fecha, teléfono, correo, alias y contraseña (mejora M10).
 *  3. Documento no registrado como socio.
 *  4. Alias único con SELECT COUNT(*): si ya existe, aviso y foco en el EditText del alias
 *     (Tema I, 4.1 y 4.2).
 * Si todo está bien se inserta y la app pasa AUTOMÁTICAMENTE al cobro de la primera
 * cuota, sin pasar por el botón del menú (Tema II, 8).
 */
class RegistroSocioActivity : AppCompatActivity() {

    private lateinit var bd: BaseDatosClub
    private lateinit var spTipoDoc: Spinner
    private lateinit var etNroDoc: EditText
    private lateinit var etApellido: EditText
    private lateinit var etNombre: EditText
    private lateinit var etFechaNac: EditText
    private lateinit var etTelefono: EditText
    private lateinit var etEmail: EditText
    private lateinit var cbAptoFisico: CheckBox
    private lateinit var etAlias: EditText
    private lateinit var etClave: EditText
    private lateinit var etClaveRepetida: EditText

    /** Fecha elegida en el calendario; null mientras no se elija. */
    private var fechaNacimiento: LocalDate? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro_socio)

        // Solo un empleado con rol ADMIN puede dar de alta socios.
        if (Sesion.empleado?.esAdministrador != true) {
            Toast.makeText(this, "Tu usuario no tiene permiso para registrar socios", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        bd = BaseDatosClub(this)
        configurarBarra(getString(R.string.alta_titulo))

        spTipoDoc = findViewById(R.id.spTipoDoc)
        etNroDoc = findViewById(R.id.etNroDoc)
        etApellido = findViewById(R.id.etApellido)
        etNombre = findViewById(R.id.etNombre)
        etFechaNac = findViewById(R.id.etFechaNac)
        etTelefono = findViewById(R.id.etTelefono)
        etEmail = findViewById(R.id.etEmail)
        cbAptoFisico = findViewById(R.id.cbAptoFisico)
        etAlias = findViewById(R.id.etAlias)
        etClave = findViewById(R.id.etClave)
        etClaveRepetida = findViewById(R.id.etClaveRepetida)

        val adaptador = ArrayAdapter.createFromResource(this, R.array.tipos_documento, R.layout.item_spinner)
        adaptador.setDropDownViewResource(R.layout.item_spinner)
        spTipoDoc.adapter = adaptador

        etFechaNac.setOnClickListener { elegirFechaNacimiento() }
        findViewById<Button>(R.id.btnGuardar).setOnClickListener { guardarSocio() }
        findViewById<Button>(R.id.btnCancelar).setOnClickListener { finish() }
    }

    /** Abre un calendario para elegir la fecha de nacimiento (no se permiten fechas futuras). */
    private fun elegirFechaNacimiento() {
        val base = fechaNacimiento ?: LocalDate.now().minusYears(25)
        val dialogo = DatePickerDialog(
            this,
            { _, anio, mes, dia ->
                // DatePicker numera los meses de 0 a 11; LocalDate de 1 a 12.
                val elegida = LocalDate.of(anio, mes + 1, dia)
                fechaNacimiento = elegida
                etFechaNac.setText(Formato.fecha(elegida))
                etFechaNac.error = null
            },
            base.year, base.monthValue - 1, base.dayOfMonth
        )
        dialogo.datePicker.maxDate =
            LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        dialogo.show()
    }

    private fun guardarSocio() {
        ocultarTeclado()
        val tipoDoc = spTipoDoc.selectedItem.toString()
        val nroDoc = etNroDoc.text.toString().trim()
        val apellido = etApellido.text.toString().trim()
        val nombre = etNombre.text.toString().trim()
        val telefono = etTelefono.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val alias = etAlias.text.toString().trim().lowercase()
        val clave = etClave.text.toString()
        val claveRepetida = etClaveRepetida.text.toString()

        // ---- 1. Datos obligatorios completos ----
        val obligatorios = listOf(
            etNroDoc to nroDoc, etApellido to apellido, etNombre to nombre,
            etTelefono to telefono, etAlias to alias, etClave to clave, etClaveRepetida to claveRepetida
        )
        var primerVacio: EditText? = null
        for ((campo, valor) in obligatorios) {
            if (valor.isEmpty()) {
                campo.error = "Dato obligatorio"
                if (primerVacio == null) primerVacio = campo
            }
        }
        val fecha = fechaNacimiento
        if (fecha == null) etFechaNac.error = "Elegí la fecha de nacimiento"
        if (primerVacio != null || fecha == null) {
            Toast.makeText(this, "SE DEBEN COMPLETAR TODOS LOS DATOS OBLIGATORIOS", Toast.LENGTH_SHORT).show()
            primerVacio?.requestFocus()
            return
        }

        // ---- 2. Formatos (mejora M10) ----
        if (tipoDoc == "DNI" && !nroDoc.matches(Regex("\\d{7,8}"))) {
            return marcarError(etNroDoc, "El DNI debe tener 7 u 8 dígitos, sin puntos")
        }
        if (!nroDoc.matches(Regex("\\d{6,9}"))) {
            return marcarError(etNroDoc, "Número de documento inválido")
        }
        val edad = Period.between(fecha, LocalDate.now()).years
        if (edad < 3 || edad > 110) {
            etFechaNac.error = "Revisá la fecha de nacimiento"
            Toast.makeText(this, "Revisá la fecha de nacimiento (edad calculada: $edad años)", Toast.LENGTH_SHORT).show()
            return
        }
        if (!telefono.matches(Regex("[0-9 +()\\-]{6,20}"))) {
            return marcarError(etTelefono, "Solo números, espacios, guiones o paréntesis")
        }
        if (email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return marcarError(etEmail, "Ingresá un correo válido")
        }
        if (!alias.matches(Regex("[a-z0-9._]{4,20}"))) {
            return marcarError(etAlias, "Entre 4 y 20 letras, números, punto o guion bajo, sin espacios")
        }
        if (clave.length < 6) {
            return marcarError(etClave, "La contraseña debe tener al menos 6 caracteres")
        }
        if (clave != claveRepetida) {
            return marcarError(etClaveRepetida, "Las contraseñas no coinciden")
        }

        // ---- 3. Documento no registrado como socio ----
        if (bd.buscaDocumentoSocio(tipoDoc, nroDoc) > 0) {
            Toast.makeText(this, "ATENCIÓN: $tipoDoc $nroDoc ya está registrado como socio", Toast.LENGTH_LONG).show()
            return marcarError(etNroDoc, "Documento ya registrado")
        }

        // ---- 4. Alias único: SELECT COUNT(*) antes de insertar ----
        val filas = bd.buscaAlias(alias)
        if (filas != 0) {
            Toast.makeText(this, "ATENCIÓN!!! EL ALIAS YA EXISTE. Elegí otro alias", Toast.LENGTH_LONG).show()
            etAlias.error = "Alias en uso"
            etAlias.requestFocus()
            etAlias.selectAll()
            return
        }

        // ---- Inserción ----
        val nroSocio = bd.insertarSocio(
            tipoDocumento = tipoDoc,
            nroDocumento = nroDoc,
            apellido = apellido.uppercase(),
            nombre = nombre.replaceFirstChar { it.uppercase() },
            fechaNacimiento = fecha.toString(),
            telefono = telefono,
            email = email,
            aptoFisico = cbAptoFisico.isChecked,
            alias = alias,
            clave = clave
        )
        if (nroSocio <= 0) {
            AlertDialog.Builder(this)
                .setTitle("No se pudo registrar")
                .setMessage("Ocurrió un error al guardar el socio. Revisá los datos e intentá de nuevo.")
                .setPositiveButton("ENTENDIDO", null)
                .show()
            return
        }
        Toast.makeText(this, "Socio N° $nroSocio registrado", Toast.LENGTH_SHORT).show()

        // ---- Pasa automáticamente al cobro de la primera cuota ----
        val intent = Intent(this, CobroCuotaActivity::class.java)
        intent.putExtra(CobroCuotaActivity.EXTRA_NRO_SOCIO, nroSocio.toInt())
        intent.putExtra(CobroCuotaActivity.EXTRA_PRIMERA_CUOTA, true)
        startActivity(intent)
        finish()
    }

    /** Marca el campo con error, deja el foco en él y corta la validación. */
    private fun marcarError(campo: EditText, mensaje: String) {
        campo.error = mensaje
        campo.requestFocus()
    }

    override fun onDestroy() {
        if (::bd.isInitialized) bd.close()
        super.onDestroy()
    }
}
