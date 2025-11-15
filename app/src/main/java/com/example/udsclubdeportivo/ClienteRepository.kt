package com.example.udsclubdeportivo

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import java.util.ArrayList

// --- DATA CLASSES ---
data class DatosSocio(
    val nombre: String,
    val apellido: String
)

data class ClienteParaListado(
    val id: String,
    val nombreCompleto: String,
    val vencimiento: String,
    val actividad: String,
    val esSocio: Boolean
)
// --- FIN DATA CLASSES ---


class ClienteRepository(context: Context) {

    private val dbHelper = DatabaseHelper.getInstance(context)

    // --- NUEVA CONSTANTE DE CONTROL DE CUPO ---
    // El cupo máximo que se puede vender por actividad y por día
    val MAX_CUPO_ACTIVIDAD = 30


    /**
     * Inserta un nuevo cliente en la tabla 'clientes'.
     */
    fun insertarCliente(
        tipoCliente: String,
        documento: String,
        nombre: String,
        apellido: String,
        telefono: String,
        fechaNacimiento: String,
        fechaInscripcion: String,
        tieneFichaMedica: Boolean,
        tieneAptoFisico: Boolean
    ): Long {

        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_TIPO, tipoCliente)
            put(DatabaseHelper.COLUMN_DOCUMENTO, documento)
            put(DatabaseHelper.COLUMN_NOMBRE, nombre)
            put(DatabaseHelper.COLUMN_APELLIDO, apellido)
            put(DatabaseHelper.COLUMN_TELEFONO, telefono)
            put(DatabaseHelper.COLUMN_FECHA_NAC, fechaNacimiento)
            put(DatabaseHelper.COLUMN_FECHA_INS, fechaInscripcion)
            put(DatabaseHelper.COLUMN_FICHA_MEDICA, if (tieneFichaMedica) 1 else 0)
            put(DatabaseHelper.COLUMN_APTO_FISICO, if (tieneAptoFisico) 1 else 0)
        }
        val newRowId = db.insert(DatabaseHelper.TABLE_CLIENTES, null, values)
        return newRowId
    }

    /**
     * Busca un cliente por su número de documento.
     */
    fun buscarClientePorDocumento(documento: String): String? {
        val db = dbHelper.readableDatabase
        var tipoCliente: String? = null
        val projection = arrayOf(DatabaseHelper.COLUMN_TIPO)
        val selection = "${DatabaseHelper.COLUMN_DOCUMENTO} = ?"
        val selectionArgs = arrayOf(documento)
        val cursor = db.query(
            DatabaseHelper.TABLE_CLIENTES,
            projection,
            selection,
            selectionArgs,
            null, null, null,
            "1"
        )
        cursor?.use {
            if (it.moveToFirst()) {
                val tipoIndex = it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TIPO)
                tipoCliente = it.getString(tipoIndex)
            }
        }
        return tipoCliente
    }

    /**
     * Busca el nombre y apellido de un cliente por su número de documento.
     */
    fun obtenerDatosSocio(documento: String): DatosSocio? {
        val db = dbHelper.readableDatabase
        var datosSocio: DatosSocio? = null
        val projection = arrayOf(DatabaseHelper.COLUMN_NOMBRE, DatabaseHelper.COLUMN_APELLIDO)
        val selection = "${DatabaseHelper.COLUMN_DOCUMENTO} = ?"
        val selectionArgs = arrayOf(documento)

        val cursor = db.query(
            DatabaseHelper.TABLE_CLIENTES,
            projection,
            selection,
            selectionArgs,
            null, null, null,
            "1"
        )
        cursor?.use {
            if (it.moveToFirst()) {
                val nombreIndex = it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NOMBRE)
                val apellidoIndex = it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_APELLIDO)
                datosSocio = DatosSocio(
                    nombre = it.getString(nombreIndex),
                    apellido = it.getString(apellidoIndex)
                )
            }
        }
        return datosSocio
    }

    /**
     * Busca la última fecha de vencimiento registrada para un socio.
     * @return La fecha de vencimiento (String) si se encuentra, o null.
     */
    fun obtenerUltimoVencimientoSocio(documento: String): String? {
        val db = dbHelper.readableDatabase
        var ultimoVencimiento: String? = null

        val projection = arrayOf(DatabaseHelper.COLUMN_PAGO_SOCIO_FECHA_VENC)
        val selection = "${DatabaseHelper.COLUMN_PAGO_SOCIO_ID} = ?"
        val selectionArgs = arrayOf(documento)
        val orderBy = "${DatabaseHelper.COLUMN_PAGO_SOCIO_FECHA_VENC} DESC"

        // Consultamos y ordenamos por fecha de vencimiento para obtener la más reciente
        val cursor = db.query(
            DatabaseHelper.TABLE_PAGOS_SOCIOS,
            projection,
            selection,
            selectionArgs,
            null, null,
            orderBy,
            "1" // LIMIT 1
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val vencIndex = it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PAGO_SOCIO_FECHA_VENC)
                ultimoVencimiento = it.getString(vencIndex)
            }
        }
        return ultimoVencimiento
    }

    /**
     * Busca el ID del ÚLTIMO registro de pago para un socio.
     */
    private fun obtenerUltimoPagoId(documento: String): Long? {
        val db = dbHelper.readableDatabase
        var ultimoId: Long? = null

        val projection = arrayOf(DatabaseHelper.COLUMN_ID_CUOTA)
        val selection = "${DatabaseHelper.COLUMN_PAGO_SOCIO_ID} = ?"
        val selectionArgs = arrayOf(documento)
        // Ordenamos por ID descendente para obtener el último registro insertado
        val orderBy = "${DatabaseHelper.COLUMN_ID_CUOTA} DESC"

        val cursor = db.query(
            DatabaseHelper.TABLE_PAGOS_SOCIOS,
            projection,
            selection,
            selectionArgs,
            null, null,
            orderBy,
            "1"
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val idIndex = it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID_CUOTA)
                ultimoId = it.getLong(idIndex)
            }
        }
        return ultimoId
    }


    // --- FUNCIÓN PRINCIPAL DE PAGO SOCIO (UPDATE/INSERT) ---
    /**
     * Registra un pago de cuota para un Socio. Si ya existe un registro de pago,
     * ACTUALIZA el último registro en lugar de insertar uno nuevo.
     */
    fun registrarPagoSocio(
        codSocio: String,
        fechaPago: String,
        fechaVencimiento: String,
        monto: Double,
        estadoPago: String,
        medioPago: String,
        cantidadCuotas: Int
    ): Long {

        val db = dbHelper.writableDatabase

        // 1. Mapear los valores a actualizar/insertar
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_PAGO_SOCIO_ID, codSocio)
            put(DatabaseHelper.COLUMN_PAGO_SOCIO_FECHA_PAGO, fechaPago)
            put(DatabaseHelper.COLUMN_PAGO_SOCIO_FECHA_VENC, fechaVencimiento)
            put(DatabaseHelper.COLUMN_PAGO_SOCIO_MONTO, monto)
            put(DatabaseHelper.COLUMN_PAGO_SOCIO_ESTADO, estadoPago)
            put(DatabaseHelper.COLUMN_PAGO_SOCIO_MEDIO_PAGO, medioPago)
            put(DatabaseHelper.COLUMN_PAGO_SOCIO_CANT_CUOTAS, cantidadCuotas)
        }

        // 2. Intentar encontrar el último registro de pago existente
        val ultimoPagoId = obtenerUltimoPagoId(codSocio)

        return if (ultimoPagoId != null) {
            // 3. Si existe, ACTUALIZAR la fila existente
            val whereClause = "${DatabaseHelper.COLUMN_ID_CUOTA} = ?"
            val whereArgs = arrayOf(ultimoPagoId.toString())

            val filasAfectadas = db.update(
                DatabaseHelper.TABLE_PAGOS_SOCIOS,
                values,
                whereClause,
                whereArgs
            )
            if (filasAfectadas > 0) ultimoPagoId else -1L

        } else {
            // 4. Si NO existe (primer pago), INSERTAR una nueva fila
            db.insert(DatabaseHelper.TABLE_PAGOS_SOCIOS, null, values)
        }
    }


    // --- FUNCIÓN REGISTRAR PAGO NO SOCIO (INSERT) ---
    /**
     * Registra un pago de actividad para un No Socio. Siempre inserta una nueva fila.
     */
    fun registrarPagoNoSocio(
        idNoSocio: String,
        fechaPago: String,
        medioPago: String,
        monto: Double,
        actividad: String
    ): Long {

        val db = dbHelper.writableDatabase

        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_PAGO_ID_NO_SOCIO, idNoSocio)
            put(DatabaseHelper.COLUMN_FECHA_PAGO_NO_SOCIO, fechaPago)
            put(DatabaseHelper.COLUMN_MEDIO_PAGO_NO_SOCIO, medioPago)
            put(DatabaseHelper.COLUMN_MONTO_PAGO_NO_SOCIO, monto)
            put(DatabaseHelper.COLUMN_ACTIVIDAD_PAGO, actividad)
        }

        val newRowId = db.insert(DatabaseHelper.TABLE_PAGOS_NO_SOCIOS, null, values)
        return newRowId
    }

    // --- NUEVA FUNCIÓN: CONTAR CUPOS USADOS ---
    /**
     * Cuenta cuántos pagos se han registrado para una actividad específica en un día dado.
     * @return El número de cupos usados.
     */
    fun contarCuposUsadosPorActividadYFecha(actividad: String, fecha: String): Int {
        val db = dbHelper.readableDatabase
        var cuposUsados = 0

        val selection = "${DatabaseHelper.COLUMN_ACTIVIDAD_PAGO} = ? AND ${DatabaseHelper.COLUMN_FECHA_PAGO_NO_SOCIO} = ?"
        val selectionArgs = arrayOf(actividad, fecha)

        // Usamos una consulta raw para simplificar el COUNT
        val query = "SELECT COUNT(*) FROM ${DatabaseHelper.TABLE_PAGOS_NO_SOCIOS} WHERE $selection"

        val cursor = db.rawQuery(query, selectionArgs)

        cursor?.use {
            if (it.moveToFirst()) {
                // El resultado de COUNT(*) está en la columna 0
                cuposUsados = it.getInt(0)
            }
        }
        return cuposUsados
    }


    // --- Función para listado (se mantiene) ---
    fun obtenerClientesParaListado(): List<ClienteParaListado> {
        val listaConsolidada = mutableListOf<ClienteParaListado>()
        val db = dbHelper.readableDatabase

        // --- 1. OBTENER PAGOS DE SOCIOS (JOIN) ---
        val querySocios = """
            SELECT
                c.${DatabaseHelper.COLUMN_DOCUMENTO},
                c.${DatabaseHelper.COLUMN_NOMBRE},
                c.${DatabaseHelper.COLUMN_APELLIDO},
                p.${DatabaseHelper.COLUMN_PAGO_SOCIO_FECHA_VENC}
            FROM ${DatabaseHelper.TABLE_PAGOS_SOCIOS} p
            JOIN ${DatabaseHelper.TABLE_CLIENTES} c ON p.${DatabaseHelper.COLUMN_PAGO_SOCIO_ID} = c.${DatabaseHelper.COLUMN_DOCUMENTO}
            WHERE c.${DatabaseHelper.COLUMN_TIPO} = 'Socio'
        """
        db.rawQuery(querySocios, null)?.use { cursorSocios ->
            val docIndex = cursorSocios.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DOCUMENTO)
            val nombreIndex = cursorSocios.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NOMBRE)
            val apellidoIndex = cursorSocios.getColumnIndexOrThrow(DatabaseHelper.COLUMN_APELLIDO)
            val vencIndex = cursorSocios.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PAGO_SOCIO_FECHA_VENC)

            while (cursorSocios.moveToNext()) {
                val nombreCompleto = "${cursorSocios.getString(nombreIndex)} ${cursorSocios.getString(apellidoIndex)}"

                listaConsolidada.add(ClienteParaListado(
                    cursorSocios.getString(docIndex),
                    nombreCompleto,
                    cursorSocios.getString(vencIndex),
                    "Membresía",
                    true
                ))
            }
        }

        // --- 2. OBTENER PAGOS DE NO SOCIOS (JOIN) ---
        val queryNoSocios = """
            SELECT
                c.${DatabaseHelper.COLUMN_DOCUMENTO},
                c.${DatabaseHelper.COLUMN_NOMBRE},
                c.${DatabaseHelper.COLUMN_APELLIDO},
                p.${DatabaseHelper.COLUMN_FECHA_PAGO_NO_SOCIO},
                p.${DatabaseHelper.COLUMN_ACTIVIDAD_PAGO}
            FROM ${DatabaseHelper.TABLE_PAGOS_NO_SOCIOS} p
            JOIN ${DatabaseHelper.TABLE_CLIENTES} c ON p.${DatabaseHelper.COLUMN_PAGO_ID_NO_SOCIO} = c.${DatabaseHelper.COLUMN_DOCUMENTO}
            WHERE c.${DatabaseHelper.COLUMN_TIPO} = 'No Socio'
        """

        db.rawQuery(queryNoSocios, null)?.use { cursorNoSocios ->
            val docIndex = cursorNoSocios.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DOCUMENTO)
            val nombreIndex = cursorNoSocios.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NOMBRE)
            val apellidoIndex = cursorNoSocios.getColumnIndexOrThrow(DatabaseHelper.COLUMN_APELLIDO)
            val vencIndex = cursorNoSocios.getColumnIndexOrThrow(DatabaseHelper.COLUMN_FECHA_PAGO_NO_SOCIO)
            val actIndex = cursorNoSocios.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ACTIVIDAD_PAGO)

            while (cursorNoSocios.moveToNext()) {
                val nombreCompleto = "${cursorNoSocios.getString(nombreIndex)} ${cursorNoSocios.getString(apellidoIndex)}"

                listaConsolidada.add(ClienteParaListado(
                    cursorNoSocios.getString(docIndex),
                    nombreCompleto,
                    cursorNoSocios.getString(vencIndex),
                    cursorNoSocios.getString(actIndex),
                    false
                ))
            }
        }
        return listaConsolidada
    }
}