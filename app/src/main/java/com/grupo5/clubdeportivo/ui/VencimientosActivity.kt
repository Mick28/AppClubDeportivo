package com.grupo5.clubdeportivo.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.grupo5.clubdeportivo.R
import com.grupo5.clubdeportivo.data.BaseDatosClub
import com.grupo5.clubdeportivo.model.Socio
import com.grupo5.clubdeportivo.util.Formato
import com.grupo5.clubdeportivo.util.Sesion
import java.time.LocalDate

/**
 * Listado diario de vencimientos (Módulo 3). Se abre con el botón "Ver vencimientos"
 * del Menú Principal (Tema III, 12.1).
 *
 * Se usa un RecyclerView con la clase de modelo Socio y el adaptador SocioAdapter, como
 * pide la consigna de la entrega final, en lugar del GridLayout del libro.
 *
 * Solapas:
 *  - VENCEN HOY / AYER: criterio del libro (vencimiento igual a hoy o a ayer).
 *  - VENCIDAS ACUMULADAS: mejora M05, para no perder los vencimientos de días anteriores.
 */
class VencimientosActivity : AppCompatActivity() {

    private enum class Solapa { HOY, ACUMULADAS }

    private lateinit var bd: BaseDatosClub
    private lateinit var adaptador: SocioAdapter
    private lateinit var tabHoy: TextView
    private lateinit var tabAcumuladas: TextView
    private lateinit var tvCantHoy: TextView
    private lateinit var tvCantAcumuladas: TextView
    private lateinit var tvCriterio: TextView
    private lateinit var tvVacio: TextView
    private lateinit var rvSocios: RecyclerView

    private var solapaActual = Solapa.HOY
    private var listaHoy: List<Socio> = emptyList()
    private var listaAcumuladas: List<Socio> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vencimientos)

        val empleado = Sesion.empleado
        if (empleado == null) {
            Toast.makeText(this, "Sesión no válida: volvé a iniciar sesión", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        bd = BaseDatosClub(this)
        configurarBarra(getString(R.string.venc_titulo))

        tabHoy = findViewById(R.id.tabHoy)
        tabAcumuladas = findViewById(R.id.tabAcumuladas)
        tvCantHoy = findViewById(R.id.tvCantHoy)
        tvCantAcumuladas = findViewById(R.id.tvCantAcumuladas)
        tvCriterio = findViewById(R.id.tvCriterio)
        tvVacio = findViewById(R.id.tvVacio)
        rvSocios = findViewById(R.id.rvSocios)

        // Fecha actual en el encabezado del listado (LocalDate.now()).
        findViewById<TextView>(R.id.tvFechaHoy).text = Formato.fechaLarga(LocalDate.now()).uppercase()

        adaptador = SocioAdapter(
            socios = emptyList(),
            puedeCobrar = empleado.esAdministrador,
            alTocarSocio = { socio -> abrirCarnet(socio) },
            alCobrar = { socio -> abrirCobro(socio) }
        )
        rvSocios.layoutManager = LinearLayoutManager(this)
        rvSocios.adapter = adaptador

        tabHoy.setOnClickListener { mostrar(Solapa.HOY) }
        tabAcumuladas.setOnClickListener { mostrar(Solapa.ACUMULADAS) }
    }

    /** Se consulta la base cada vez que la pantalla vuelve a estar visible (por ejemplo, tras un cobro). */
    override fun onResume() {
        super.onResume()
        if (!::bd.isInitialized) return
        val hoy = LocalDate.now()
        listaHoy = bd.listarVencimientosDelDia(hoy)
        listaAcumuladas = bd.listarVencidasAcumuladas(hoy)
        tvCantHoy.text = listaHoy.size.toString()
        tvCantAcumuladas.text = listaAcumuladas.size.toString()
        mostrar(solapaActual)
    }

    private fun mostrar(solapa: Solapa) {
        solapaActual = solapa
        tabHoy.isSelected = solapa == Solapa.HOY
        tabAcumuladas.isSelected = solapa == Solapa.ACUMULADAS

        val lista: List<Socio>
        if (solapa == Solapa.HOY) {
            lista = listaHoy
            tvCriterio.text = "Socios cuya cuota vence hoy (último día habilitado) o venció ayer " +
                "(hoy ya no pueden ingresar). Ordenados por apellido y nombre."
            tvVacio.text = "No hay socios con cuota que venza hoy ni que haya vencido ayer.\n" +
                "Revisá la solapa de vencidas acumuladas."
        } else {
            lista = listaAcumuladas
            tvCriterio.text = "Socios con la cuota vencida desde antes de ayer, del más atrasado al más reciente."
            tvVacio.text = "No hay cuotas vencidas acumuladas."
        }
        adaptador.actualizar(lista)
        tvVacio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
        rvSocios.visibility = if (lista.isEmpty()) View.INVISIBLE else View.VISIBLE
    }

    private fun abrirCarnet(socio: Socio) {
        val intent = Intent(this, CarnetActivity::class.java)
        intent.putExtra(CarnetActivity.EXTRA_NRO_SOCIO, socio.id)
        intent.putExtra(CarnetActivity.EXTRA_MODO, CarnetActivity.MODO_EMPLEADO)
        startActivity(intent)
    }

    private fun abrirCobro(socio: Socio) {
        val intent = Intent(this, CobroCuotaActivity::class.java)
        intent.putExtra(CobroCuotaActivity.EXTRA_NRO_SOCIO, socio.id)
        startActivity(intent)
    }

    override fun onDestroy() {
        if (::bd.isInitialized) bd.close()
        super.onDestroy()
    }
}
