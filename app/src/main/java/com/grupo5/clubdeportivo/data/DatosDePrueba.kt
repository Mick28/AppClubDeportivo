package com.grupo5.clubdeportivo.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.grupo5.clubdeportivo.util.Seguridad
import java.time.LocalDate

/**
 * Carga inicial de la base: usuarios de prueba (al menos un empleado y un socio, como pide
 * el libro de Prácticas formativas), la configuración del club y socios con vencimientos
 * calculados a partir del día en que se instala la app, para que el listado diario nunca
 * aparezca vacío durante la corrección.
 *
 * Usuarios de prueba:
 *   EMPLEADO  admin      / admin123   (rol ADMIN: acceso completo)
 *   EMPLEADO  recepcion  / recep123   (rol CONSULTA: solo vencimientos y carnets)
 *   SOCIO     agomez     / socio123   (cuota al día, carnet vigente)
 *   SOCIO     jsosa      / socio123   (cuota vencida hace 14 días)
 *   SOCIO     arubio     / socio123   (registrado sin primera cuota: todavía no tiene carnet)
 */
object DatosDePrueba {

    const val VALOR_CUOTA = 18000.0

    fun cargar(db: SQLiteDatabase) {
        val hoy = LocalDate.now()

        // --- Configuración del club (mejora M03: valor de cuota parametrizado) ---
        configuracion(db, BaseDatosClub.CLAVE_VALOR_CUOTA, VALOR_CUOTA.toString())
        configuracion(db, BaseDatosClub.CLAVE_VIGENCIA_CARNET, "12")

        // --- Empleados ---
        val admin = persona(db, "DNI", "20111222", "GONZALEZ", "Emma", "1980-03-12", "11 4000-1000", "emma@clubdeportivo.com")
        empleado(db, admin, "admin", "admin123", "ADMIN")
        val recep = persona(db, "DNI", "25333444", "MORENO", "Diego", "1990-07-21", "11 4000-2000", "recepcion@clubdeportivo.com")
        empleado(db, recep, "recepcion", "recep123", "CONSULTA")

        // --- Socios con distintos vencimientos (relativos a hoy) ---
        socioConCuota(db, "30111222", "GOMEZ", "Ana", "1988-05-10", "11 5555-0001", "agomez", true, hoy.plusDays(20))
        socioConCuota(db, "28444555", "RUIZ", "Marta", "1982-11-02", "11 5555-4444", "mruiz", true, hoy)
        socioConCuota(db, "31222333", "PEREZ", "Luis", "1985-01-23", "11 4444-3333", "lperez", true, hoy)
        socioConCuota(db, "33444555", "DIAZ", "Carla", "1991-08-14", "11 3333-2222", "cdiaz", true, hoy.minusDays(1))
        socioConCuota(db, "27888999", "LOPEZ", "Hugo", "1979-04-30", "11 2222-1111", "hlopez", true, hoy.minusDays(1))
        socioConCuota(db, "29555666", "SOSA", "Julio", "1983-09-09", "11 6666-7777", "jsosa", true, hoy.minusDays(14))
        socioConCuota(db, "35666777", "BENITEZ", "Rosa", "1994-12-01", "11 7777-8888", "rbenitez", true, hoy.minusDays(40))
        socioConCuota(db, "34999000", "ACOSTA", "Nicolás", "1996-06-18", "11 8888-9999", "nacosta", false, hoy.plusDays(9))

        // Socio registrado que todavía no abonó la primera cuota (sin carnet, estado pendiente).
        val rubio = persona(db, "DNI", "32777888", "RUBIO", "Ana", "1990-02-27", "11 9999-0000", "arubio@mail.com")
        socio(db, rubio, "arubio", "socio123", apto = false, activo = false, inscripcion = hoy.minusDays(2))

        // --- Cartilla de actividades (mejora M02), los valores del prototipo de Figma ---
        actividad(db, "Natación - Pileta libre", 8500.0, "DIA")
        actividad(db, "Musculación", 6000.0, "DIA")
        actividad(db, "Danza", 5500.0, "DIA")
        actividad(db, "Cancha de fútbol", 12000.0, "TURNO")
        actividad(db, "Karate", 6500.0, "TURNO")
        actividad(db, "Tenis", 9000.0, "TURNO", vigente = false)
    }

    // --------------------------------------------------------------------------------------

    private fun configuracion(db: SQLiteDatabase, clave: String, valor: String) {
        db.insertOrThrow("configuracion", null, ContentValues().apply {
            put("clave", clave); put("valor", valor)
        })
    }

    private fun persona(
        db: SQLiteDatabase, tipo: String, nro: String, apellido: String, nombre: String,
        nacimiento: String, telefono: String, email: String
    ): Long = db.insertOrThrow("persona", null, ContentValues().apply {
        put("tipo_documento", tipo)
        put("nro_documento", nro)
        put("apellido", apellido)
        put("nombre", nombre)
        put("fecha_nacimiento", nacimiento)
        put("telefono", telefono)
        put("email", email)
    })

    private fun empleado(db: SQLiteDatabase, idPersona: Long, usuario: String, clave: String, rol: String) {
        val salt = Seguridad.generarSalt()
        db.insertOrThrow("empleado", null, ContentValues().apply {
            put("id_persona", idPersona)
            put("usuario", usuario)
            put("password_hash", Seguridad.hashear(clave, salt))
            put("salt", salt)
            put("rol", rol)
            put("activo", 1)
        })
    }

    private fun socio(
        db: SQLiteDatabase, idPersona: Long, alias: String, clave: String,
        apto: Boolean, activo: Boolean, inscripcion: LocalDate
    ): Long {
        val salt = Seguridad.generarSalt()
        return db.insertOrThrow("socio", null, ContentValues().apply {
            put("id_persona", idPersona)
            put("fecha_inscripcion", inscripcion.toString())
            put("apto_fisico", if (apto) 1 else 0)
            put("estado_activo", if (activo) 1 else 0)
            put("baja", 0)
            put("alias", alias)
            put("password_hash", Seguridad.hashear(clave, salt))
            put("salt", salt)
        })
    }

    /** Crea persona + socio + una cuota paga que vence en la fecha indicada + su carnet. */
    private fun socioConCuota(
        db: SQLiteDatabase, dni: String, apellido: String, nombre: String, nacimiento: String,
        telefono: String, alias: String, apto: Boolean, vencimiento: LocalDate
    ) {
        val hoy = LocalDate.now()
        val inicio = vencimiento.minusMonths(1)
        val idPersona = persona(db, "DNI", dni, apellido, nombre, nacimiento, telefono, "$alias@mail.com")
        val nroSocio = socio(
            db, idPersona, alias, "socio123", apto,
            activo = !vencimiento.isBefore(hoy), inscripcion = inicio
        )
        db.insertOrThrow("cuota", null, ContentValues().apply {
            put("nro_socio", nroSocio)
            put("id_empleado", 1)
            put("monto", VALOR_CUOTA)
            put("fecha_pago", inicio.toString())
            put("fecha_inicio", inicio.toString())
            put("fecha_vencimiento", vencimiento.toString())
            put("forma_pago", "EFECTIVO")
            put("cantidad_cuotas", 1)
        })
        db.insertOrThrow("carnet", null, ContentValues().apply {
            put("nro_socio", nroSocio)
            put("fecha_emision", inicio.toString())
            put("fecha_vencimiento", inicio.plusMonths(12).toString())
        })
    }

    private fun actividad(db: SQLiteDatabase, nombre: String, precio: Double, unidad: String, vigente: Boolean = true) {
        db.insertOrThrow("actividad", null, ContentValues().apply {
            put("nombre", nombre)
            put("precio", precio)
            put("unidad_cobro", unidad)
            put("vigente", if (vigente) 1 else 0)
        })
    }
}
