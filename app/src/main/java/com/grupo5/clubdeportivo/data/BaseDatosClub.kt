package com.grupo5.clubdeportivo.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.grupo5.clubdeportivo.model.Carnet
import com.grupo5.clubdeportivo.model.Empleado
import com.grupo5.clubdeportivo.model.ResultadoPago
import com.grupo5.clubdeportivo.model.Socio
import com.grupo5.clubdeportivo.model.SocioDetalle
import com.grupo5.clubdeportivo.util.Seguridad
import java.time.LocalDate

/**
 * Clase de la base de datos de la app (SQLiteOpenHelper), como se vio en la Etapa 3
 * (Tema III) y en los códigos de ejemplo del libro de la Etapa 4.
 *
 * Concentra en un solo lugar todas las consultas de los tres módulos:
 *  - Módulo 1: login de empleado y de socio, alias único, alta de socio.
 *  - Módulo 2: búsqueda de socio, registro del cobro, emisión del carnet.
 *  - Módulo 3: listado diario de vencimientos (devuelve List<Socio>).
 *
 * Modelo de datos: se conservan las seis tablas heredadas de DSOO (persona, socio,
 * no_socio, cuota, carnet y usuario, que acá se llama "empleado") y se agregan las
 * tablas actividad y configuracion que propuso la 1ª entrega (mejoras M02 y M03).
 * Las fechas se guardan como TEXT en formato ISO (yyyy-MM-dd), que SQLite compara
 * y ordena correctamente como texto.
 */
class BaseDatosClub(context: Context) :
    SQLiteOpenHelper(context, NOMBRE_BD, null, VERSION_BD) {

    companion object {
        const val NOMBRE_BD = "clubdeportivo.db"
        const val VERSION_BD = 1

        // Las sentencias CREATE TABLE se agrupan como constantes en el companion object,
        // tal como sugiere el libro de la Etapa 3 para bases con varias tablas relacionadas.

        /** Superclase de socio, no socio y empleado (herencia del modelo de MDS/DSOO). */
        const val CREAR_PERSONA = """
            CREATE TABLE persona (
                id_persona        INTEGER PRIMARY KEY AUTOINCREMENT,
                tipo_documento    TEXT    NOT NULL DEFAULT 'DNI',
                nro_documento     TEXT    NOT NULL,
                apellido          TEXT    NOT NULL,
                nombre            TEXT    NOT NULL,
                fecha_nacimiento  TEXT,
                telefono          TEXT,
                email             TEXT,
                UNIQUE (tipo_documento, nro_documento)
            )"""

        /** Usuarios del personal del club (tabla "usuario" de DSOO, con salt y rol que ahora sí restringe). */
        const val CREAR_EMPLEADO = """
            CREATE TABLE empleado (
                id_empleado    INTEGER PRIMARY KEY AUTOINCREMENT,
                id_persona     INTEGER NOT NULL UNIQUE REFERENCES persona(id_persona),
                usuario        TEXT    NOT NULL UNIQUE,
                password_hash  TEXT    NOT NULL,
                salt           TEXT    NOT NULL,
                rol            TEXT    NOT NULL CHECK (rol IN ('ADMIN','CONSULTA')),
                activo         INTEGER NOT NULL DEFAULT 1
            )"""

        /** Socios. alias + password_hash permiten que el socio entre a la app a ver su carnet. */
        const val CREAR_SOCIO = """
            CREATE TABLE socio (
                nro_socio          INTEGER PRIMARY KEY AUTOINCREMENT,
                id_persona         INTEGER NOT NULL UNIQUE REFERENCES persona(id_persona),
                fecha_inscripcion  TEXT    NOT NULL,
                apto_fisico        INTEGER NOT NULL DEFAULT 0,
                estado_activo      INTEGER NOT NULL DEFAULT 0,
                baja               INTEGER NOT NULL DEFAULT 0,
                alias              TEXT    NOT NULL UNIQUE,
                password_hash      TEXT    NOT NULL,
                salt               TEXT    NOT NULL
            )"""

        /** Cada fila es una operación de cobro. fecha_vencimiento = último día que cubre el pago. */
        const val CREAR_CUOTA = """
            CREATE TABLE cuota (
                nro_cuota          INTEGER PRIMARY KEY AUTOINCREMENT,
                nro_socio          INTEGER NOT NULL REFERENCES socio(nro_socio),
                id_empleado        INTEGER REFERENCES empleado(id_empleado),
                monto              REAL    NOT NULL CHECK (monto > 0),
                fecha_pago         TEXT    NOT NULL,
                fecha_inicio       TEXT    NOT NULL,
                fecha_vencimiento  TEXT    NOT NULL,
                forma_pago         TEXT    NOT NULL CHECK (forma_pago IN ('EFECTIVO','TARJETA')),
                cantidad_cuotas    INTEGER NOT NULL DEFAULT 1 CHECK (cantidad_cuotas IN (1,3,6)),
                CHECK (forma_pago = 'TARJETA' OR cantidad_cuotas = 1)
            )"""

        /** Carnet del socio: uno por socio (UNIQUE), se genera con el primer pago. */
        const val CREAR_CARNET = """
            CREATE TABLE carnet (
                nro_carnet         INTEGER PRIMARY KEY AUTOINCREMENT,
                nro_socio          INTEGER NOT NULL UNIQUE REFERENCES socio(nro_socio),
                fecha_emision      TEXT    NOT NULL,
                fecha_vencimiento  TEXT    NOT NULL
            )"""

        /** Cartilla de actividades con precios (mejora M02). */
        const val CREAR_ACTIVIDAD = """
            CREATE TABLE actividad (
                id_actividad  INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre        TEXT    NOT NULL UNIQUE,
                precio        REAL    NOT NULL CHECK (precio > 0),
                unidad_cobro  TEXT    NOT NULL CHECK (unidad_cobro IN ('DIA','TURNO')),
                vigente       INTEGER NOT NULL DEFAULT 1
            )"""

        /** Visitas de no socios: una persona puede tener muchas visitas (1:N). */
        const val CREAR_NO_SOCIO = """
            CREATE TABLE no_socio (
                id_no_socio        INTEGER PRIMARY KEY AUTOINCREMENT,
                id_persona         INTEGER NOT NULL REFERENCES persona(id_persona),
                id_actividad       INTEGER NOT NULL REFERENCES actividad(id_actividad),
                fecha_hora_visita  TEXT    NOT NULL,
                monto_abonado      REAL    NOT NULL CHECK (monto_abonado > 0)
            )"""

        /** Valores configurables del club, por ejemplo el valor vigente de la cuota (mejora M03). */
        const val CREAR_CONFIGURACION = """
            CREATE TABLE configuracion (
                clave  TEXT PRIMARY KEY,
                valor  TEXT NOT NULL
            )"""

        const val CLAVE_VALOR_CUOTA = "valor_cuota_mensual"
        const val CLAVE_VIGENCIA_CARNET = "vigencia_carnet_meses"

        /** Orden de borrado respetando las claves foráneas (primero las tablas hijas). */
        private val TABLAS = listOf(
            "no_socio", "carnet", "cuota", "socio", "empleado",
            "actividad", "configuracion", "persona"
        )
    }

    // ------------------------------------------------------------------------------------
    // Ciclo de vida de la base
    // ------------------------------------------------------------------------------------

    /** SQLite trae las claves foráneas desactivadas por defecto: se activan en cada conexión. */
    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREAR_PERSONA)
        db.execSQL(CREAR_EMPLEADO)
        db.execSQL(CREAR_SOCIO)
        db.execSQL(CREAR_CUOTA)
        db.execSQL(CREAR_CARNET)
        db.execSQL(CREAR_ACTIVIDAD)
        db.execSQL(CREAR_NO_SOCIO)
        db.execSQL(CREAR_CONFIGURACION)
        // Índice para acelerar el listado diario, que agrupa y filtra por socio y vencimiento.
        db.execSQL("CREATE INDEX idx_cuota_socio_venc ON cuota (nro_socio, fecha_vencimiento)")
        DatosDePrueba.cargar(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Versión académica: ante un cambio de estructura se recrea la base completa.
        TABLAS.forEach { db.execSQL("DROP TABLE IF EXISTS $it") }
        onCreate(db)
    }

    // ------------------------------------------------------------------------------------
    // MÓDULO 1 · Login
    // ------------------------------------------------------------------------------------

    /**
     * Valida usuario y contraseña de un empleado.
     * 1) Trae el salt del usuario. 2) Calcula el hash. 3) Cuenta con SELECT COUNT(*) si existe
     * la combinación usuario + hash (mismo recurso que el libro usa para el alias único).
     *
     * @return el empleado si las credenciales son correctas; null en caso contrario.
     */
    fun validarEmpleado(usuario: String, clave: String): Empleado? {
        val salt = obtenerSalt("SELECT salt FROM empleado WHERE usuario = ? AND activo = 1", usuario)
            ?: return null
        val hash = Seguridad.hashear(clave, salt)
        val coincidencias = contar(
            "SELECT COUNT(*) FROM empleado WHERE usuario = ? AND password_hash = ? AND activo = 1",
            arrayOf(usuario, hash)
        )
        if (coincidencias == 0) return null

        val bd = readableDatabase
        val cursor = bd.rawQuery(
            """SELECT e.id_empleado, e.usuario, p.apellido, p.nombre, e.rol
               FROM empleado e INNER JOIN persona p ON p.id_persona = e.id_persona
               WHERE e.usuario = ?""",
            arrayOf(usuario)
        )
        var empleado: Empleado? = null
        if (cursor.moveToFirst()) {
            empleado = Empleado(
                id = cursor.getInt(0),
                usuario = cursor.getString(1),
                apellido = cursor.getString(2),
                nombre = cursor.getString(3),
                rol = cursor.getString(4)
            )
        }
        cursor.close()
        return empleado
    }

    /**
     * Valida el alias y la contraseña de un socio (Tema II, 9): se cuenta con COUNT(*) si
     * existe esa combinación; si es distinto de 0 hay coincidencia.
     *
     * @return el número de socio si las credenciales son correctas; null en caso contrario.
     */
    fun validarSocio(alias: String, clave: String): Int? {
        val salt = obtenerSalt("SELECT salt FROM socio WHERE alias = ? AND baja = 0", alias)
            ?: return null
        val hash = Seguridad.hashear(clave, salt)
        val coincidencias = contar(
            "SELECT COUNT(*) FROM socio WHERE alias = ? AND password_hash = ? AND baja = 0",
            arrayOf(alias, hash)
        )
        if (coincidencias == 0) return null

        val bd = readableDatabase
        val cursor = bd.rawQuery("SELECT nro_socio FROM socio WHERE alias = ?", arrayOf(alias))
        var nro: Int? = null
        if (cursor.moveToFirst()) nro = cursor.getInt(0)
        cursor.close()
        return nro
    }

    // ------------------------------------------------------------------------------------
    // MÓDULO 1 · Registro de socio
    // ------------------------------------------------------------------------------------

    /**
     * Alias único (Tema I, 4.1 y 4.2): devuelve cuántos socios usan ese alias.
     * Si es 0 se puede insertar; si no, hay que avisar y pedir otro.
     */
    fun buscaAlias(alias: String): Int =
        contar("SELECT COUNT(*) FROM socio WHERE alias = ?", arrayOf(alias))

    /** Cuántos socios hay con ese tipo y número de documento (evita registrar dos veces a la misma persona). */
    fun buscaDocumentoSocio(tipoDocumento: String, nroDocumento: String): Int =
        contar(
            """SELECT COUNT(*) FROM socio s INNER JOIN persona p ON p.id_persona = s.id_persona
               WHERE p.tipo_documento = ? AND p.nro_documento = ?""",
            arrayOf(tipoDocumento, nroDocumento)
        )

    /**
     * Inserta un socio nuevo: la persona (si no existía como no socio) y el socio, en una
     * única transacción para que no quede una persona sin socio si algo falla.
     * El socio nace con estado_activo = 0: se habilita recién al pagar la primera cuota.
     *
     * @return el número de socio asignado (autoincremental), o -1 si no se pudo insertar.
     */
    fun insertarSocio(
        tipoDocumento: String,
        nroDocumento: String,
        apellido: String,
        nombre: String,
        fechaNacimiento: String,
        telefono: String,
        email: String,
        aptoFisico: Boolean,
        alias: String,
        clave: String
    ): Long {
        val bd = writableDatabase
        bd.beginTransaction()
        try {
            val datosPersona = ContentValues().apply {
                put("tipo_documento", tipoDocumento)
                put("nro_documento", nroDocumento)
                put("apellido", apellido)
                put("nombre", nombre)
                put("fecha_nacimiento", fechaNacimiento)
                put("telefono", telefono)
                put("email", email)
            }
            // Si la persona ya existe (por ejemplo, vino antes como no socio), se actualizan sus datos
            // y se reutiliza su id; si no existe, se inserta.
            var idPersona = idPersonaPorDocumento(bd, tipoDocumento, nroDocumento)
            if (idPersona > 0) {
                bd.update("persona", datosPersona, "id_persona = ?", arrayOf(idPersona.toString()))
            } else {
                idPersona = bd.insertOrThrow("persona", null, datosPersona)
            }

            val salt = Seguridad.generarSalt()
            val datosSocio = ContentValues().apply {
                put("id_persona", idPersona)
                put("fecha_inscripcion", LocalDate.now().toString())
                put("apto_fisico", if (aptoFisico) 1 else 0)
                put("estado_activo", 0)
                put("baja", 0)
                put("alias", alias)
                put("password_hash", Seguridad.hashear(clave, salt))
                put("salt", salt)
            }
            val nroSocio = bd.insertOrThrow("socio", null, datosSocio)
            bd.setTransactionSuccessful()
            return nroSocio
        } catch (e: Exception) {
            return -1
        } finally {
            bd.endTransaction()
        }
    }

    /** Registra la presentación del apto físico de un socio existente (HU04). */
    fun registrarAptoFisico(nroSocio: Int): Boolean {
        val valores = ContentValues().apply { put("apto_fisico", 1) }
        return writableDatabase.update("socio", valores, "nro_socio = ?", arrayOf(nroSocio.toString())) == 1
    }

    // ------------------------------------------------------------------------------------
    // MÓDULO 2 · Búsqueda del socio y cobro de cuota
    // ------------------------------------------------------------------------------------

    /** Consulta base de los datos del socio, con el vencimiento de su última cuota paga. */
    private val CONSULTA_SOCIO = """
        SELECT s.nro_socio, p.tipo_documento, p.nro_documento, p.apellido, p.nombre,
               IFNULL(p.telefono, ''), IFNULL(p.email, ''), s.fecha_inscripcion,
               s.apto_fisico, s.baja,
               (SELECT MAX(c.fecha_vencimiento) FROM cuota c WHERE c.nro_socio = s.nro_socio) AS ultimo_venc
        FROM socio s INNER JOIN persona p ON p.id_persona = s.id_persona
    """

    /** Busca un socio por su número (id). */
    fun buscarSocioPorNumero(nroSocio: Int): SocioDetalle? =
        buscarSocio("$CONSULTA_SOCIO WHERE s.nro_socio = ?", arrayOf(nroSocio.toString()))

    /**
     * Busca un socio por tipo y número de documento: la opción "que nunca falla" según el
     * libro (Tema II, 8.3), porque nombre y apellido pueden repetirse.
     */
    fun buscarSocioPorDocumento(tipoDocumento: String, nroDocumento: String): SocioDetalle? =
        buscarSocio(
            "$CONSULTA_SOCIO WHERE p.tipo_documento = ? AND p.nro_documento = ?",
            arrayOf(tipoDocumento, nroDocumento)
        )

    private fun buscarSocio(query: String, args: Array<String>): SocioDetalle? {
        val bd = readableDatabase
        val cursor = bd.rawQuery(query, args)
        var socio: SocioDetalle? = null
        if (cursor.moveToFirst()) socio = leerSocioDetalle(cursor)
        cursor.close()
        return socio
    }

    private fun leerSocioDetalle(cursor: Cursor) = SocioDetalle(
        nroSocio = cursor.getInt(0),
        tipoDocumento = cursor.getString(1),
        nroDocumento = cursor.getString(2),
        apellido = cursor.getString(3),
        nombre = cursor.getString(4),
        telefono = cursor.getString(5),
        email = cursor.getString(6),
        fechaInscripcion = cursor.getString(7),
        aptoFisico = cursor.getInt(8) == 1,
        baja = cursor.getInt(9) == 1,
        ultimoVencimiento = if (cursor.isNull(10)) null else cursor.getString(10)
    )

    /**
     * Monto de la cuota tomado de la tabla de configuración (registro de precios, Tema II 8.1):
     * así el empleado no tipea el importe y no se cobra un precio equivocado.
     */
    fun obtenerValorCuota(): Double {
        val bd = readableDatabase
        val cursor = bd.rawQuery("SELECT valor FROM configuracion WHERE clave = ?", arrayOf(CLAVE_VALOR_CUOTA))
        var valor = 0.0
        if (cursor.moveToFirst()) valor = cursor.getString(0).toDoubleOrNull() ?: 0.0
        cursor.close()
        return valor
    }

    /**
     * Calcula el período que cubre un pago nuevo. La app calcula el vencimiento por sí sola
     * con java.time (Tema I, 4.3):
     *  - Primera cuota o cuota vencida: el período arranca hoy y vence LocalDate.now().plusMonths(1).
     *  - Pago adelantado (cuota todavía vigente): el período arranca el día siguiente al
     *    vencimiento actual, para no perder días ("el plazo comienza a correr a partir del
     *    día siguiente al vencimiento de la cuota", enunciado del cliente).
     *
     * @return par (fecha de inicio, fecha de vencimiento).
     */
    fun calcularPeriodo(ultimoVencimiento: String?): Pair<LocalDate, LocalDate> {
        val hoy = LocalDate.now()
        val ultimo = ultimoVencimiento?.let { LocalDate.parse(it) }
        return if (ultimo == null || ultimo.isBefore(hoy)) {
            Pair(hoy, LocalDate.now().plusMonths(1))
        } else {
            val inicio = ultimo.plusDays(1)
            Pair(inicio, inicio.plusMonths(1))
        }
    }

    /**
     * Registra la operación de cobro en la base usando el id del socio (Tema II, 8.2).
     * En la misma transacción: inserta la cuota, reactiva al socio (estado_activo = 1) y,
     * si es su primer pago, emite el carnet (Tema II, 9: el carnet se genera con la primera cuota).
     *
     * @return los datos para armar el recibo, o null si la operación falló.
     */
    fun registrarPago(
        nroSocio: Int,
        idEmpleado: Int?,
        monto: Double,
        formaPago: String,
        cantidadCuotas: Int
    ): ResultadoPago? {
        val socio = buscarSocioPorNumero(nroSocio) ?: return null
        val (inicio, vencimiento) = calcularPeriodo(socio.ultimoVencimiento)
        val hoy = LocalDate.now()

        val bd = writableDatabase
        bd.beginTransaction()
        try {
            val cuota = ContentValues().apply {
                put("nro_socio", nroSocio)
                if (idEmpleado != null) put("id_empleado", idEmpleado)
                put("monto", monto)
                put("fecha_pago", hoy.toString())
                put("fecha_inicio", inicio.toString())
                put("fecha_vencimiento", vencimiento.toString())
                put("forma_pago", formaPago)
                put("cantidad_cuotas", cantidadCuotas)
            }
            val nroCuota = bd.insertOrThrow("cuota", null, cuota)

            // Reactivación automática del socio al pagar (regla de DSOO que se conserva).
            bd.execSQL("UPDATE socio SET estado_activo = 1 WHERE nro_socio = ?", arrayOf<Any>(nroSocio))

            // Emisión del carnet con el primer pago; si ya tenía uno vencido, se renueva.
            val meses = leerConfiguracion(bd, CLAVE_VIGENCIA_CARNET)?.toLongOrNull() ?: 12L
            var carnetEmitido = false
            val cursor = bd.rawQuery(
                "SELECT fecha_vencimiento FROM carnet WHERE nro_socio = ?", arrayOf(nroSocio.toString())
            )
            if (!cursor.moveToFirst()) {
                val carnet = ContentValues().apply {
                    put("nro_socio", nroSocio)
                    put("fecha_emision", hoy.toString())
                    put("fecha_vencimiento", hoy.plusMonths(meses).toString())
                }
                bd.insertOrThrow("carnet", null, carnet)
                carnetEmitido = true
            } else if (LocalDate.parse(cursor.getString(0)).isBefore(hoy)) {
                bd.execSQL(
                    "UPDATE carnet SET fecha_emision = ?, fecha_vencimiento = ? WHERE nro_socio = ?",
                    arrayOf<Any>(hoy.toString(), hoy.plusMonths(meses).toString(), nroSocio)
                )
            }
            cursor.close()

            bd.setTransactionSuccessful()
            return ResultadoPago(
                nroComprobante = nroCuota,
                fechaPago = hoy.toString(),
                fechaInicio = inicio.toString(),
                fechaVencimiento = vencimiento.toString(),
                carnetEmitido = carnetEmitido
            )
        } catch (e: Exception) {
            return null
        } finally {
            bd.endTransaction()
        }
    }

    /**
     * Datos del carnet de un socio. Devuelve null si el socio todavía no tiene carnet
     * (se registró pero no abonó la primera cuota).
     */
    fun obtenerCarnet(nroSocio: Int): Carnet? {
        val socio = buscarSocioPorNumero(nroSocio) ?: return null
        val bd = readableDatabase
        val cursor = bd.rawQuery(
            "SELECT nro_carnet, fecha_emision, fecha_vencimiento FROM carnet WHERE nro_socio = ?",
            arrayOf(nroSocio.toString())
        )
        var carnet: Carnet? = null
        if (cursor.moveToFirst()) {
            carnet = Carnet(
                nroCarnet = cursor.getInt(0),
                socio = socio,
                fechaEmision = cursor.getString(1),
                fechaVencimientoCarnet = cursor.getString(2)
            )
        }
        cursor.close()
        return carnet
    }

    // ------------------------------------------------------------------------------------
    // Control automático de vencimientos (HU08, mejora M09)
    // ------------------------------------------------------------------------------------

    /**
     * Inhabilita a los socios cuya última cuota venció antes de hoy o que nunca pagaron.
     * Se ejecuta cada vez que se abre la app (no solo al iniciar sesión, como en DSOO).
     *
     * @return cantidad de socios que pasaron a inhabilitados.
     */
    fun inhabilitarVencidos(hoy: LocalDate = LocalDate.now()): Int {
        val bd = writableDatabase
        val sql = """
            UPDATE socio SET estado_activo = 0
            WHERE estado_activo = 1 AND baja = 0
              AND IFNULL((SELECT MAX(c.fecha_vencimiento) FROM cuota c
                          WHERE c.nro_socio = socio.nro_socio), '') < ?
        """
        val sentencia = bd.compileStatement(sql)
        sentencia.bindString(1, hoy.toString())
        val afectados = sentencia.executeUpdateDelete()
        sentencia.close()
        return afectados
    }

    // ------------------------------------------------------------------------------------
    // MÓDULO 3 · Listado diario de vencimientos
    // ------------------------------------------------------------------------------------

    /**
     * Listado diario de socios a vencer (Tema III, 10 a 12).
     *
     * Criterio adoptado: se listan los socios cuya última cuota VENCE HOY (hoy es su último
     * día habilitado) o VENCIÓ AYER (hoy ya no pueden ingresar y tienen que pagar para
     * retomar). Son los dos casos que el libro propone como "a vencer", y cubren tanto a
     * quienes hay que avisar como a quienes hay que cobrar en el mostrador.
     *
     * Se usa ORDER BY apellido y nombre para que las filas ya lleguen ordenadas.
     * A diferencia del libro (que devuelve List<List<String>>), devuelve List<Socio>,
     * que es lo que consume el adaptador del RecyclerView.
     */
    fun listarVencimientosDelDia(hoy: LocalDate = LocalDate.now()): List<Socio> {
        val ayer = hoy.minusDays(1)
        val query = """
            SELECT s.nro_socio, p.apellido, p.nombre, MAX(c.fecha_vencimiento) AS vence,
                   IFNULL(p.telefono, ''), IFNULL(p.email, '')
            FROM socio s
                 INNER JOIN persona p ON p.id_persona = s.id_persona
                 INNER JOIN cuota c   ON c.nro_socio  = s.nro_socio
            WHERE s.baja = 0
            GROUP BY s.nro_socio, p.apellido, p.nombre, p.telefono, p.email
            HAVING MAX(c.fecha_vencimiento) = ? OR MAX(c.fecha_vencimiento) = ?
            ORDER BY p.apellido, p.nombre
        """
        return listarSocios(query, arrayOf(hoy.toString(), ayer.toString()))
    }

    /**
     * Cuotas vencidas acumuladas (mejora M05): socios con la cuota vencida desde antes de ayer.
     * Tapa el agujero del sistema de DSOO, donde un vencimiento no consultado ese día se perdía.
     * Se ordena por fecha de vencimiento (los más atrasados primero) y luego por apellido y nombre.
     */
    fun listarVencidasAcumuladas(hoy: LocalDate = LocalDate.now()): List<Socio> {
        val ayer = hoy.minusDays(1)
        val query = """
            SELECT s.nro_socio, p.apellido, p.nombre, MAX(c.fecha_vencimiento) AS vence,
                   IFNULL(p.telefono, ''), IFNULL(p.email, '')
            FROM socio s
                 INNER JOIN persona p ON p.id_persona = s.id_persona
                 INNER JOIN cuota c   ON c.nro_socio  = s.nro_socio
            WHERE s.baja = 0
            GROUP BY s.nro_socio, p.apellido, p.nombre, p.telefono, p.email
            HAVING MAX(c.fecha_vencimiento) < ?
            ORDER BY vence, p.apellido, p.nombre
        """
        return listarSocios(query, arrayOf(ayer.toString()))
    }

    /** Recorre el cursor (una o varias filas) y arma la lista de objetos Socio. */
    private fun listarSocios(query: String, args: Array<String>): List<Socio> {
        val lista = mutableListOf<Socio>()
        val bd = readableDatabase
        val cursor = bd.rawQuery(query, args)
        if (cursor.moveToFirst()) {
            do {
                lista.add(
                    Socio(
                        id = cursor.getInt(0),
                        apellido = cursor.getString(1),
                        nombre = cursor.getString(2),
                        fechaVencimiento = cursor.getString(3),
                        telefono = cursor.getString(4),
                        email = cursor.getString(5)
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return lista
    }

    /** Cantidad de socios cuya cuota vence hoy (tarjeta del Menú Principal). */
    fun contarVencenHoy(hoy: LocalDate = LocalDate.now()): Int =
        contar(
            """SELECT COUNT(*) FROM (
                   SELECT c.nro_socio FROM cuota c INNER JOIN socio s ON s.nro_socio = c.nro_socio
                   WHERE s.baja = 0
                   GROUP BY c.nro_socio HAVING MAX(c.fecha_vencimiento) = ?)""",
            arrayOf(hoy.toString())
        )

    // ------------------------------------------------------------------------------------
    // Funciones auxiliares
    // ------------------------------------------------------------------------------------

    /** Ejecuta una consulta SELECT COUNT(*) y devuelve el número (mismo patrón que buscaCurso del libro). */
    private fun contar(query: String, args: Array<String>): Int {
        val bd = readableDatabase
        val cursor: Cursor = bd.rawQuery(query, args)
        var cantidad = 0
        if (cursor.moveToFirst()) {
            cantidad = cursor.getInt(0)
        }
        cursor.close()
        return cantidad
    }

    private fun obtenerSalt(query: String, clave: String): String? {
        val bd = readableDatabase
        val cursor = bd.rawQuery(query, arrayOf(clave))
        var salt: String? = null
        if (cursor.moveToFirst()) salt = cursor.getString(0)
        cursor.close()
        return salt
    }

    private fun idPersonaPorDocumento(bd: SQLiteDatabase, tipo: String, nro: String): Long {
        val cursor = bd.rawQuery(
            "SELECT id_persona FROM persona WHERE tipo_documento = ? AND nro_documento = ?",
            arrayOf(tipo, nro)
        )
        var id = -1L
        if (cursor.moveToFirst()) id = cursor.getLong(0)
        cursor.close()
        return id
    }

    private fun leerConfiguracion(bd: SQLiteDatabase, clave: String): String? {
        val cursor = bd.rawQuery("SELECT valor FROM configuracion WHERE clave = ?", arrayOf(clave))
        var valor: String? = null
        if (cursor.moveToFirst()) valor = cursor.getString(0)
        cursor.close()
        return valor
    }
}
