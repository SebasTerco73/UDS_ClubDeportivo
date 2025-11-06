package com.example.udsclubdeportivo

import android.content.ContentValues
import android.content.Context
// Importaciones necesarias para la consulta compleja
import android.database.Cursor
import java.util.ArrayList

// -------------------------------------------------------------------
// 'data class' para los resultados (está igual)
// -------------------------------------------------------------------
data class ClienteParaListado(
    val id: String, // Usaremos el documento como ID en la lista
    val nombreCompleto: String,
    val vencimiento: String, // FechaVenc (Socio) o FechaPago (No Socio)
    val actividad: String, // "Membresía" (Socio) o la actividad (No Socio)
    val esSocio: Boolean
)

class ClienteRepository(context: Context) {

    // Instancia del Helper obtenida a través del Singleton.
    private val dbHelper = DatabaseHelper.getInstance(context)

    /**
     * Inserta un nuevo cliente en la tabla 'clientes'.
     * (Esta función está igual que la tuya)
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
     * (Esta función está igual que la tuya)
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

    // -------------------------------------------------------------------
    // MODIFICACIÓN: MÉTODO ACTUALIZADO CON LÓGICA DE JOIN
    // -------------------------------------------------------------------
    /**
     * Obtiene una lista consolidada de TODOS los pagos (Socios y No Socios)
     * que tienen vencimiento, uniéndolos con la información del cliente.
     * ESTA ES LA LÓGICA CENTRAL QUE NECESITAS.
     */
    fun obtenerClientesParaListado(): List<ClienteParaListado> {
        // Usamos mutableListOf para la lista que crecerá
        val listaConsolidada = mutableListOf<ClienteParaListado>()
        // dbHelper.readableDatabase es una propiedad en Kotlin
        val db = dbHelper.readableDatabase

        // --- 1. OBTENER PAGOS DE SOCIOS (JOIN) ---
        // Usamos cadenas multilínea (""") y plantillas de string ($)
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

        // El bloque 'use' de Kotlin reemplaza al 'try-finally' y cierra el cursor automáticamente
        db.rawQuery(querySocios, null)?.use { cursorSocios ->
            // Obtenemos los índices de las columnas
            val docIndex = cursorSocios.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DOCUMENTO)
            val nombreIndex = cursorSocios.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NOMBRE)
            val apellidoIndex = cursorSocios.getColumnIndexOrThrow(DatabaseHelper.COLUMN_APELLIDO)
            val vencIndex = cursorSocios.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PAGO_SOCIO_FECHA_VENC)

            while (cursorSocios.moveToNext()) {
                // Usamos plantillas de string para concatenar
                val nombreCompleto = "${cursorSocios.getString(nombreIndex)} ${cursorSocios.getString(apellidoIndex)}"

                listaConsolidada.add(ClienteParaListado(
                    cursorSocios.getString(docIndex),
                    nombreCompleto,
                    cursorSocios.getString(vencIndex), // Fecha Vencimiento
                    "Membresía", // Actividad (Socio)
                    true // esSocio = true
                ))
            }
        } // El cursorSocios se cierra aquí

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

        // Usamos 'use' de nuevo para el segundo cursor
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
                    cursorNoSocios.getString(vencIndex), // Fecha de Pago
                    cursorNoSocios.getString(actIndex),  // Actividad
                    false // esSocio = false
                ))
            }
        } // El cursorNoSocios se cierra aquí

        return listaConsolidada // Devuelve la lista combinada
    }
}