package com.grupo5.clubdeportivo.ui

import android.content.Intent
import android.os.Bundle
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
import com.grupo5.clubdeportivo.model.EstadoSocio
import com.grupo5.clubdeportivo.model.SocioDetalle
import com.grupo5.clubdeportivo.util.Formato
import com.grupo5.clubdeportivo.util.Sesion
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Cobro de cuota (Módulo 2). Se activa desde dos situaciones (Tema II, 8):
 *  1. Después del registro de un socio nuevo: llega el número de socio por Intent y se
 *     cobra la primera cuota directamente (que da origen al carnet).
 *  2. Desde el botón "Cobro de cuota" del menú o desde el listado de vencimientos:
 *     se busca al socio por número o por tipo y número de documento.
 *
 * El importe se toma de la tabla de configuración (registro de precios, Tema II 8.1), la
 * fecha de vencimiento la calcula la app con java.time (Tema I 4.3) y el recibo se arma
 * en otra activity pasando los datos con Intent + putExtra() (Tema II 8.5 y 8.6).
 */
class CobroCuotaActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NRO_SOCIO = "nroSocio"
        const val EXTRA_PRIMERA_CUOTA = "primeraCuota"
    }

    private lateinit var bd: BaseDatosClub
    private lateinit var llBusqueda: LinearLayout
    private lateinit var llCobro: LinearLayout
    private lateinit var rbPorNumero: RadioButton
    private lateinit var etNroSocio: EditText
    private lateinit var llPorDocumento: LinearLayout
    private lateinit var spTipoDoc: Spinner
    private lateinit var etNroDoc: EditText
    private lateinit var tvSocioNombre: TextView
    private lateinit var tvSocioDatos: TextView
    private lateinit var tvEstado: TextView
    private lateinit var tvAvisoCuota: TextView
    private lateinit var etMonto: EditText
    private lateinit var rbTarjeta: RadioButton
    private lateinit var rgCuotas: RadioGroup
    private lateinit var rbCuotas3: RadioButton
    private lateinit var rbCuotas6: RadioButton
    private lateinit var tvAvisoCuotas: TextView
    private lateinit var tvVencActual: TextView
    private lateinit var tvNuevoVenc: TextView

    /** Socio sobre el que se va a registrar el cobro (null hasta encontrarlo). */
    private var socio: SocioDetalle? = null
    private var esPrimeraCuota = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cobro_cuota)

        if (Sesion.empleado?.esAdministrador != true) {
            Toast.makeText(this, "Tu usuario no tiene permiso para cobrar cuotas", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        bd = BaseDatosClub(this)
        vincularVistas()

        val nroSocio = intent.getIntExtra(EXTRA_NRO_SOCIO, -1)
        esPrimeraCuota = intent.getBooleanExtra(EXTRA_PRIMERA_CUOTA, false)
        configurarBarra(if (esPrimeraCuota) "PRIMERA CUOTA" else getString(R.string.cobro_titulo))

        if (nroSocio > 0) {
            // Llega con el socio ya identificado: después del alta o desde el listado.
            llBusqueda.visibility = View.GONE
            val encontrado = bd.buscarSocioPorNumero(nroSocio)
            if (encontrado == null) {
                Toast.makeText(this, "No se encontró el socio N° $nroSocio", Toast.LENGTH_LONG).show()
                finish()
                return
            }
            mostrarSocio(encontrado)
        }
        actualizarOpcionesDeCuotas()
    }

    private fun vincularVistas() {
        llBusqueda = findViewById(R.id.llBusqueda)
        llCobro = findViewById(R.id.llCobro)
        rbPorNumero = findViewById(R.id.rbPorNumero)
        etNroSocio = findViewById(R.id.etNroSocio)
        llPorDocumento = findViewById(R.id.llPorDocumento)
        spTipoDoc = findViewById(R.id.spTipoDoc)
        etNroDoc = findViewById(R.id.etNroDoc)
        tvSocioNombre = findViewById(R.id.tvSocioNombre)
        tvSocioDatos = findViewById(R.id.tvSocioDatos)
        tvEstado = findViewById(R.id.tvEstado)
        tvAvisoCuota = findViewById(R.id.tvAvisoCuota)
        etMonto = findViewById(R.id.etMonto)
        rbTarjeta = findViewById(R.id.rbTarjeta)
        rgCuotas = findViewById(R.id.rgCuotas)
        rbCuotas3 = findViewById(R.id.rbCuotas3)
        rbCuotas6 = findViewById(R.id.rbCuotas6)
        tvAvisoCuotas = findViewById(R.id.tvAvisoCuotas)
        tvVencActual = findViewById(R.id.tvVencActual)
        tvNuevoVenc = findViewById(R.id.tvNuevoVenc)

        val adaptador = ArrayAdapter.createFromResource(this, R.array.tipos_documento, R.layout.item_spinner)
        adaptador.setDropDownViewResource(R.layout.item_spinner)
        spTipoDoc.adapter = adaptador

        // Criterio de búsqueda: número de socio o tipo + número de documento (Tema II, 8.3).
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

        // Las cuotas 3 y 6 solo existen con tarjeta (regla del enunciado, HU06 escenario 2).
        findViewById<RadioGroup>(R.id.rgFormaPago).setOnCheckedChangeListener { _, _ ->
            actualizarOpcionesDeCuotas()
        }
        findViewById<Button>(R.id.btnRegistrarPago).setOnClickListener { validarYConfirmarPago() }
    }

    // ------------------------------------------------------------------------------------
    // Búsqueda y verificación del socio
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
            llCobro.visibility = View.GONE
            socio = null
            Toast.makeText(this, "No se encontró ningún socio con ese dato", Toast.LENGTH_SHORT).show()
            return
        }
        if (encontrado.baja) {
            llCobro.visibility = View.GONE
            socio = null
            Toast.makeText(this, "El socio está dado de baja: no se le puede cobrar la cuota", Toast.LENGTH_LONG).show()
            return
        }
        mostrarSocio(encontrado)
    }

    /** Muestra los datos del socio, verifica si la cuota está vencida y propone el importe. */
    private fun mostrarSocio(s: SocioDetalle) {
        socio = s
        tvSocioNombre.text = s.apellidoYNombre
        tvSocioDatos.text = "Socio N° ${s.nroSocio} · ${s.documento}"
        tvEstado.mostrarEstado(s.estado())

        // Verificación de si la cuota está vencida (Tema II, 8.3).
        val hoy = LocalDate.now()
        val ultimo = s.ultimoVencimiento?.let { LocalDate.parse(it) }
        var mensaje: String
        val tono: EstadoSocio
        when {
            ultimo == null -> {
                mensaje = "Primera cuota del socio: al registrar el pago se emite su carnet."
                tono = EstadoSocio.PENDIENTE
            }
            ultimo.isBefore(hoy) -> {
                val dias = ChronoUnit.DAYS.between(ultimo, hoy)
                mensaje = "Cuota VENCIDA el ${Formato.fecha(ultimo)} · $dias día(s) de atraso. " +
                    "No puede realizar actividades hasta pagar."
                tono = EstadoSocio.INHABILITADO
            }
            else -> {
                mensaje = "Cuota al día hasta el ${Formato.fecha(ultimo)}. " +
                    "Este pago cubre el mes siguiente."
                tono = EstadoSocio.HABILITADO
            }
        }
        if (!s.aptoFisico) {
            mensaje += "\nFalta el apto físico: después del pago quedará PENDIENTE hasta presentarlo."
        }
        tvAvisoCuota.mostrarAviso(mensaje, tono)

        // Importe tomado del registro de precios; se puede ajustar en casos excepcionales.
        val valorCuota = bd.obtenerValorCuota()
        if (valorCuota > 0) {
            etMonto.setText(if (valorCuota % 1.0 == 0.0) valorCuota.toLong().toString() else valorCuota.toString())
        } else {
            etMonto.text.clear()
            etMonto.hint = "Ingresá el importe"
        }

        tvVencActual.text = if (ultimo == null) "Sin cuotas" else Formato.fecha(ultimo)
        val (_, nuevoVencimiento) = bd.calcularPeriodo(s.ultimoVencimiento)
        tvNuevoVenc.text = Formato.fecha(nuevoVencimiento)

        llCobro.visibility = View.VISIBLE
    }

    private fun actualizarOpcionesDeCuotas() {
        val conTarjeta = rbTarjeta.isChecked
        rbCuotas3.isEnabled = conTarjeta
        rbCuotas6.isEnabled = conTarjeta
        if (!conTarjeta) rgCuotas.check(R.id.rbCuotas1)
        tvAvisoCuotas.visibility = if (conTarjeta) View.GONE else View.VISIBLE
    }

    // ------------------------------------------------------------------------------------
    // Registro del cobro
    // ------------------------------------------------------------------------------------

    private fun validarYConfirmarPago() {
        val s = socio ?: return
        val monto = etMonto.text.toString().replace(",", ".").toDoubleOrNull()
        if (monto == null || monto <= 0) {
            etMonto.error = "Ingresá un importe mayor a cero"
            etMonto.requestFocus()
            return
        }
        val formaPago = if (rbTarjeta.isChecked) "TARJETA" else "EFECTIVO"
        val cantidadCuotas = when (rgCuotas.checkedRadioButtonId) {
            R.id.rbCuotas3 -> 3
            R.id.rbCuotas6 -> 6
            else -> 1
        }
        if (formaPago == "EFECTIVO" && cantidadCuotas != 1) {
            Toast.makeText(this, getString(R.string.aviso_cuotas), Toast.LENGTH_LONG).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Confirmar cobro")
            .setMessage(
                "¿Registrar el pago de ${Formato.moneda(monto)} de ${s.apellidoYNombre}?\n" +
                    "Forma de pago: ${formaPago.lowercase().replaceFirstChar { it.uppercase() }}" +
                    if (formaPago == "TARJETA") " en $cantidadCuotas cuota(s)" else ""
            )
            .setPositiveButton("REGISTRAR") { _, _ -> registrarPago(s, monto, formaPago, cantidadCuotas) }
            .setNegativeButton("CANCELAR", null)
            .show()
    }

    private fun registrarPago(s: SocioDetalle, monto: Double, formaPago: String, cantidadCuotas: Int) {
        val empleado = Sesion.empleado
        // Se registra la operación en la base usando el id del socio (Tema II, 8.2).
        val resultado = bd.registrarPago(s.nroSocio, empleado?.id, monto, formaPago, cantidadCuotas)
        if (resultado == null) {
            AlertDialog.Builder(this)
                .setTitle("No se pudo registrar el pago")
                .setMessage("Ocurrió un error al guardar la operación. No se cobró nada: intentá de nuevo.")
                .setPositiveButton("ENTENDIDO", null)
                .show()
            return
        }

        val estadoResultante = if (s.aptoFisico) EstadoSocio.HABILITADO else EstadoSocio.PENDIENTE
        val formaTexto = if (formaPago == "TARJETA") "Tarjeta de crédito · $cantidadCuotas cuota(s)" else "Efectivo"
        val atendio = if (empleado != null) "${empleado.apellido}, ${empleado.nombre}" else "—"

        // Documento de pago: los datos viajan a la otra activity con putExtra() (Tema II, 8.5 y 8.6).
        val intentar = Intent(this, ComprobanteActivity::class.java)
        intentar.putExtra(ComprobanteActivity.NRO_DOCUMENTO, Formato.nroComprobante(resultado.nroComprobante))
        intentar.putExtra(ComprobanteActivity.NOMBRE_DOCUMENTO, "RECIBO DE PAGO")
        intentar.putExtra(ComprobanteActivity.FECHA, Formato.fecha(resultado.fechaPago))
        intentar.putExtra(ComprobanteActivity.EMISOR, "Club Deportivo · CUIT 30-71234567-8\nAtendió: $atendio")
        intentar.putExtra(ComprobanteActivity.RECEPTOR, "${s.apellidoYNombre}\nSocio N° ${s.nroSocio} · ${s.documento}")
        intentar.putExtra(
            ComprobanteActivity.DETALLE,
            "Cuota social mensual" + (if (esPrimeraCuota || s.ultimoVencimiento == null) " (primera cuota)" else "") +
                "\nPeríodo: ${Formato.fecha(resultado.fechaInicio)} al ${Formato.fecha(resultado.fechaVencimiento)}" +
                "\nForma de pago: $formaTexto"
        )
        intentar.putExtra(ComprobanteActivity.MONTO, Formato.moneda(monto))
        intentar.putExtra(ComprobanteActivity.VENCIMIENTO, Formato.fecha(resultado.fechaVencimiento))
        intentar.putExtra(ComprobanteActivity.ESTADO, estadoResultante.name)
        intentar.putExtra(ComprobanteActivity.NRO_SOCIO, s.nroSocio.toString())
        intentar.putExtra(ComprobanteActivity.CARNET_EMITIDO, resultado.carnetEmitido.toString())
        startActivity(intentar)
        // Se cierra el cobro para que "volver" desde el recibo no permita cobrar dos veces.
        finish()
    }

    override fun onDestroy() {
        if (::bd.isInitialized) bd.close()
        super.onDestroy()
    }
}
