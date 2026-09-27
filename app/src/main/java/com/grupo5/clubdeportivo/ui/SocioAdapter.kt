package com.grupo5.clubdeportivo.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.grupo5.clubdeportivo.R
import com.grupo5.clubdeportivo.model.EstadoSocio
import com.grupo5.clubdeportivo.model.Socio
import com.grupo5.clubdeportivo.util.Formato
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Adaptador del RecyclerView del listado de vencimientos (Módulo 3).
 *
 * Toma cada objeto Socio de la lista y lo carga en su fila (item_socio.xml). El RecyclerView
 * recicla las filas que salen de la pantalla en lugar de crear un TextView por dato, como
 * ocurre con el GridLayout del ejemplo del libro.
 *
 * @param socios        lista a mostrar
 * @param puedeCobrar   si es false (rol CONSULTA) se oculta el botón COBRAR
 * @param alTocarSocio  acción al tocar la fila (abre el carnet del socio)
 * @param alCobrar      acción del botón COBRAR (abre el cobro de cuota del socio)
 */
class SocioAdapter(
    private var socios: List<Socio>,
    private val puedeCobrar: Boolean,
    private val alTocarSocio: (Socio) -> Unit,
    private val alCobrar: (Socio) -> Unit
) : RecyclerView.Adapter<SocioAdapter.SocioViewHolder>() {

    /** Guarda las referencias a las vistas de una fila para no buscarlas cada vez (findViewById). */
    class SocioViewHolder(vista: View) : RecyclerView.ViewHolder(vista) {
        val tvApellidoNombre: TextView = vista.findViewById(R.id.tvApellidoNombre)
        val tvNroSocio: TextView = vista.findViewById(R.id.tvNroSocio)
        val tvFechaVencimiento: TextView = vista.findViewById(R.id.tvFechaVencimiento)
        val tvTelefono: TextView = vista.findViewById(R.id.tvTelefono)
        val tvEtiqueta: TextView = vista.findViewById(R.id.tvEtiqueta)
        val btnCobrar: Button = vista.findViewById(R.id.btnCobrar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SocioViewHolder {
        val vista = LayoutInflater.from(parent.context).inflate(R.layout.item_socio, parent, false)
        return SocioViewHolder(vista)
    }

    override fun getItemCount(): Int = socios.size

    override fun onBindViewHolder(holder: SocioViewHolder, position: Int) {
        val socio = socios[position]
        val hoy = LocalDate.now()
        val vence = LocalDate.parse(socio.fechaVencimiento)
        val diasDeAtraso = ChronoUnit.DAYS.between(vence, hoy)

        holder.tvApellidoNombre.text = socio.apellidoYNombre
        holder.tvNroSocio.text = "Socio N° ${socio.id}"
        holder.tvFechaVencimiento.text =
            if (diasDeAtraso <= 0L) "Vence ${Formato.fecha(vence)}" else "Venció el ${Formato.fecha(vence)}"
        holder.tvTelefono.text = socio.telefono.ifEmpty { "Sin teléfono" }

        // Etiqueta con color + texto: vence hoy (ámbar), venció ayer o antes (rojo).
        when {
            diasDeAtraso <= 0L -> {
                holder.tvEtiqueta.mostrarEstado(EstadoSocio.PENDIENTE)
                holder.tvEtiqueta.text = "VENCE HOY"
            }
            diasDeAtraso == 1L -> {
                holder.tvEtiqueta.mostrarEstado(EstadoSocio.INHABILITADO)
                holder.tvEtiqueta.text = "VENCIÓ AYER"
            }
            else -> {
                holder.tvEtiqueta.mostrarEstado(EstadoSocio.INHABILITADO)
                holder.tvEtiqueta.text = "$diasDeAtraso DÍAS"
            }
        }

        holder.btnCobrar.visibility = if (puedeCobrar) View.VISIBLE else View.GONE
        holder.btnCobrar.setOnClickListener { alCobrar(socio) }
        holder.itemView.setOnClickListener { alTocarSocio(socio) }
    }

    /** Reemplaza la lista (al cambiar de solapa o al volver de un cobro). */
    fun actualizar(nuevaLista: List<Socio>) {
        socios = nuevaLista
        notifyDataSetChanged()
    }
}
