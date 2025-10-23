package com.example.udsclubdeportivo

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    // 1. Definir la consulta SQL para crear la tabla
    private val SQL_CREATE_ENTRIES =
        """CREATE TABLE $TABLE_CLIENTES (
            $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COLUMN_TIPO TEXT,
            $COLUMN_DOCUMENTO TEXT,
            $COLUMN_NOMBRE TEXT,
            $COLUMN_APELLIDO TEXT,
            $COLUMN_TELEFONO TEXT,
            $COLUMN_FECHA_NAC TEXT,
            $COLUMN_FECHA_INS TEXT,
            $COLUMN_FICHA_MEDICA INTEGER,
            $COLUMN_APTO_FISICO INTEGER
        )""".trimMargin()

    // 2. Metodo llamado cuando la BD se crea por primera vez
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_ENTRIES)
    }

    // 3. Meodo llamado cuando cambias DATABASE_VERSION
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Esto elimina la tabla si ya existe y la recrea (¡Borra todos los datos!)
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CLIENTES")
        onCreate(db)
    }

    companion object {
        // --- Constantes de la Base de Datos ---
        private const val DATABASE_NAME = "club_deportivo.db"
        private const val DATABASE_VERSION = 1

        // --- Constantes de la Tabla CLIENTES ---
        const val TABLE_CLIENTES = "clientes"
        const val COLUMN_ID = "_id"
        const val COLUMN_TIPO = "tipoCliente"
        const val COLUMN_DOCUMENTO = "documento"
        const val COLUMN_NOMBRE = "nombre"
        const val COLUMN_APELLIDO = "apellido"
        const val COLUMN_TELEFONO = "telefono"
        const val COLUMN_FECHA_NAC = "fechaNacimiento"
        const val COLUMN_FECHA_INS = "fechaInscripcion"
        const val COLUMN_FICHA_MEDICA = "tieneFichaMedica" // INTEGER (0=false, 1=true)
        const val COLUMN_APTO_FISICO = "tieneAptoFisico" // INTEGER (0=false, 1=true)

        // --- Patrón Singleton para acceso seguro ---
        @Volatile
        private var INSTANCE: DatabaseHelper? = null

        fun getInstance(context: Context): DatabaseHelper {
            // ⭐ CORRECCIÓN: La lógica del Singleton debe estar dentro del bloque synchronized
            return INSTANCE ?: synchronized(this) {
                val instance = DatabaseHelper(context.applicationContext)
                INSTANCE = instance
                instance // Retorna la instancia
            } // ⭐ CORRECCIÓN: Falta el cierre del bloque synchronized y el bloque 'return'
        }
    }
}