package com.grupo5.clubdeportivo.ui

import android.app.Activity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.TextView
import com.grupo5.clubdeportivo.R
import com.grupo5.clubdeportivo.model.EstadoSocio

/*
 * Funciones de extensión compartidas por varias pantallas. Evitan repetir el mismo código
 * en cada Activity (el libro recomienda crear muchas funciones para tener el código ordenado).
 */

/** Configura la barra superior incluida con <include layout="@layout/barra_superior">. */
fun Activity.configurarBarra(titulo: String, mostrarVolver: Boolean = true) {
    findViewById<TextView>(R.id.tvTituloBarra).text = titulo
    val btnVolver = findViewById<ImageButton>(R.id.btnVolver)
    if (mostrarVolver) {
        btnVolver.setOnClickListener { finish() }
    } else {
        btnVolver.visibility = View.GONE
    }
}

/** Pinta un distintivo de estado: texto + color + borde (nunca solo color). */
fun TextView.mostrarEstado(estado: EstadoSocio) {
    text = estado.etiqueta
    setTextColor(context.getColor(estado.colorRes))
    setBackgroundResource(estado.fondoRes)
    visibility = View.VISIBLE
}

/** Muestra un mensaje destacado con el color del estado indicado. */
fun TextView.mostrarAviso(mensaje: String, estado: EstadoSocio) {
    text = mensaje
    setTextColor(context.getColor(estado.colorRes))
    setBackgroundResource(estado.avisoRes)
    visibility = View.VISIBLE
}

/** Oculta el teclado en pantalla (por ejemplo, después de tocar Buscar). */
fun Activity.ocultarTeclado() {
    val imm = getSystemService(InputMethodManager::class.java)
    currentFocus?.let { imm?.hideSoftInputFromWindow(it.windowToken, 0) }
}
