package com.example.udsclubdeportivo

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase

class ClienteRepository(context: Context) {

    // Instancia del Helper obtenida a través del Singleton.
    // Esto asegura que solo haya una instancia del Helper en toda la aplicación.
    private val dbHelper = DatabaseHelper.getInstance(context)

    /**
     * Inserta un nuevo cliente en la tabla 'clientes'.
     * @return El ID de la fila insertada o -1 si hubo un error.
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

        // Obtener la base de datos en modo escritura justo antes de la operación
        val db = dbHelper.writableDatabase

        // Usar ContentValues para mapear las columnas a sus valores
        val values = ContentValues().apply {
            put(DatabaseHelper.COLUMN_TIPO, tipoCliente)
            put(DatabaseHelper.COLUMN_DOCUMENTO, documento)
            put(DatabaseHelper.COLUMN_NOMBRE, nombre)
            put(DatabaseHelper.COLUMN_APELLIDO, apellido)
            put(DatabaseHelper.COLUMN_TELEFONO, telefono)
            put(DatabaseHelper.COLUMN_FECHA_NAC, fechaNacimiento)
            put(DatabaseHelper.COLUMN_FECHA_INS, fechaInscripcion)

            // Convertir Boolean a INTEGER: true=1, false=0 (Estándar de SQLite)
            put(DatabaseHelper.COLUMN_FICHA_MEDICA, if (tieneFichaMedica) 1 else 0)
            put(DatabaseHelper.COLUMN_APTO_FISICO, if (tieneAptoFisico) 1 else 0)
        }

        // Ejecutar la inserción
        val newRowId = db.insert(DatabaseHelper.TABLE_CLIENTES, null, values)
        // Nota: NO se cierra la base de datos aquí. El sistema lo gestiona a través del Singleton.
        return newRowId
    }

    /**
     * Busca un cliente por su número de documento.
     * @return El tipo de cliente ("Socio" o "No Socio") si se encuentra, o null si no existe.
     */
    fun buscarClientePorDocumento(documento: String): String? {
        // Usamos readableDatabase para una consulta de solo lectura
        val db = dbHelper.readableDatabase
        var tipoCliente: String? = null

        // Proyección: solo obtenemos la columna del tipo de cliente
        val projection = arrayOf(DatabaseHelper.COLUMN_TIPO)

        // Cláusula WHERE
        val selection = "${DatabaseHelper.COLUMN_DOCUMENTO} = ?"
        val selectionArgs = arrayOf(documento)

        val cursor = db.query(
            DatabaseHelper.TABLE_CLIENTES,
            projection,
            selection,
            selectionArgs,
            null, null, null,
            "1" // LIMIT 1: para detener la búsqueda al encontrar el primero
        )

        // Usamos 'use' para asegurar que el Cursor se cierre automáticamente
        cursor?.use {
            if (it.moveToFirst()) {
                // Obtener el índice de la columna y luego el valor como String
                val tipoIndex = it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TIPO)
                tipoCliente = it.getString(tipoIndex)
            }
        }
        // Retorna el tipo de cliente ("Socio", "No Socio") o null
        return tipoCliente
    }
}