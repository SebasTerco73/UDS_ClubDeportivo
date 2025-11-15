package com.example.udsclubdeportivo

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// El constructor se hace privado para forzar el uso del Singleton (getInstance)
class DatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    // 1. Metodo llamado cuando la BD se crea por primera vez
    override fun onCreate(db: SQLiteDatabase?) {
        // Usamos 'db?' (safe call) por la nulabilidad de Kotlin
        db?.execSQL(SQL_CREATE_TABLE_CLIENTES)
        db?.execSQL(SQL_CREATE_TABLE_PAGOS_NO_SOCIOS)
        db?.execSQL(SQL_CREATE_TABLE_PAGOS_SOCIOS)
    }

    // 2. Metodo llamado cuando cambias DATABASE_VERSION
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // Elimina las tablas existentes (¡Borra todos los datos!)
        // Se borran en orden inverso para evitar conflictos de Foreign Key
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_PAGOS_SOCIOS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_PAGOS_NO_SOCIOS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_CLIENTES")
        onCreate(db) // Vuelve a crear las tres tablas
    }

    // --- INICIO DEL CÓDIGO AÑADIDO ---

    /**
     * Elimina TODOS los registros (filas) de la tabla de pagos de socios.
     * La estructura de la tabla (columnas) permanece intacta.
     */
    fun limpiarTablaPagosSocios() {
        // 1. Obtenemos la base de datos en modo escritura
        val db = this.writableDatabase

        try {
            // 2. Ejecutamos la eliminación
            // Al pasar 'null' como cláusula WHERE, se borran todas las filas.
            db.delete(TABLE_PAGOS_SOCIOS, null, null)

            // Opcional: Si quieres reiniciar el autoincremento (el _id)
            // db.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = '$TABLE_PAGOS_SOCIOS'")

        } catch (e: Exception) {
            // Es buena práctica manejar cualquier excepción
            e.printStackTrace()
        } finally {
            // 3. Cerramos la base de datos
            db.close()
        }
    }

    // --- FIN DEL CÓDIGO AÑADIDO ---


    // 'companion object' es el equivalente a 'static' en Java
    // Aquí van todas las constantes y el método Singleton
    companion object {

        // --- Constantes de la Base de Datos ---
        private const val DATABASE_NAME = "club_deportivo.db"
        // ⚠️ ¡IMPORTANTE! Incrementa la versión para que se actualice la base de datos
        private const val DATABASE_VERSION = 2 // Era 1, ahora es 2

        // --- Constantes de la Tabla CLIENTES ---
        const val TABLE_CLIENTES = "clientes"
        const val COLUMN_ID_CLIENTE = "_id"
        const val COLUMN_TIPO = "tipoCliente"
        const val COLUMN_DOCUMENTO = "documento"
        const val COLUMN_NOMBRE = "nombre"
        const val COLUMN_APELLIDO = "apellido"
        const val COLUMN_TELEFONO = "telefono"
        const val COLUMN_FECHA_NAC = "fechaNacimiento"
        const val COLUMN_FECHA_INS = "fechaInscripcion"
        const val COLUMN_FICHA_MEDICA = "tieneFichaMedica" // INTEGER (0=false, 1=true)
        const val COLUMN_APTO_FISICO = "tieneAptoFisico" // INTEGER (0=false, 1=true)

        // --- Constantes Tabla PAGOS NO SOCIOS ---
        const val TABLE_PAGOS_NO_SOCIOS = "pagos_no_socios"
        const val COLUMN_ID_PAGO_NO_SOCIO = "_id" // ID único del pago
        const val COLUMN_PAGO_ID_NO_SOCIO = "idNoSocio" // Documento del no socio
        const val COLUMN_FECHA_PAGO_NO_SOCIO = "fechaPago"
        const val COLUMN_MEDIO_PAGO_NO_SOCIO = "medioPago"
        const val COLUMN_MONTO_PAGO_NO_SOCIO = "monto" // REAL
        const val COLUMN_ACTIVIDAD_PAGO = "actividad"

        // --- Constantes Tabla PAGOS SOCIOS ---
        const val TABLE_PAGOS_SOCIOS = "pagos_socios"
        const val COLUMN_ID_CUOTA = "_id" // idCuota (PK)
        const val COLUMN_PAGO_SOCIO_ID = "codSocio" // codsocio (FK = documento)
        const val COLUMN_PAGO_SOCIO_FECHA_PAGO = "fechaPago"
        const val COLUMN_PAGO_SOCIO_FECHA_VENC = "fechaVencimiento"
        const val COLUMN_PAGO_SOCIO_MONTO = "monto" // REAL
        const val COLUMN_PAGO_SOCIO_ESTADO = "estadoPago" // TEXT
        const val COLUMN_PAGO_SOCIO_MEDIO_PAGO = "medioPago" // TEXT
        const val COLUMN_PAGO_SOCIO_CANT_CUOTAS = "cantidadCuotas" // INTEGER


        // --- SQL CREATE (Clientes) ---
        // Usamos multiline strings """ de Kotlin para mejor legibilidad
        private const val SQL_CREATE_TABLE_CLIENTES =
            """CREATE TABLE $TABLE_CLIENTES (
                $COLUMN_ID_CLIENTE INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TIPO TEXT,
                $COLUMN_DOCUMENTO TEXT UNIQUE,
                $COLUMN_NOMBRE TEXT,
                $COLUMN_APELLIDO TEXT,
                $COLUMN_TELEFONO TEXT,
                $COLUMN_FECHA_NAC TEXT,
                $COLUMN_FECHA_INS TEXT,
                $COLUMN_FICHA_MEDICA INTEGER,
                $COLUMN_APTO_FISICO INTEGER
            )"""

        // --- SQL CREATE (Pagos No Socios) ---
        private const val SQL_CREATE_TABLE_PAGOS_NO_SOCIOS =
            """CREATE TABLE $TABLE_PAGOS_NO_SOCIOS (
                $COLUMN_ID_PAGO_NO_SOCIO INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_PAGO_ID_NO_SOCIO TEXT,
                $COLUMN_FECHA_PAGO_NO_SOCIO TEXT,
                $COLUMN_MEDIO_PAGO_NO_SOCIO TEXT,
                $COLUMN_MONTO_PAGO_NO_SOCIO REAL,
                $COLUMN_ACTIVIDAD_PAGO TEXT,
                FOREIGN KEY($COLUMN_PAGO_ID_NO_SOCIO) REFERENCES $TABLE_CLIENTES($COLUMN_DOCUMENTO) ON DELETE CASCADE
            )"""

        // --- SQL CREATE (Pagos Socios) ---
        private const val SQL_CREATE_TABLE_PAGOS_SOCIOS =
            """CREATE TABLE $TABLE_PAGOS_SOCIOS (
                $COLUMN_ID_CUOTA INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_PAGO_SOCIO_ID TEXT,
                $COLUMN_PAGO_SOCIO_FECHA_PAGO TEXT,
                $COLUMN_PAGO_SOCIO_FECHA_VENC TEXT,
                $COLUMN_PAGO_SOCIO_MONTO REAL,
                $COLUMN_PAGO_SOCIO_ESTADO TEXT,
                $COLUMN_PAGO_SOCIO_MEDIO_PAGO TEXT,
                $COLUMN_PAGO_SOCIO_CANT_CUOTAS INTEGER DEFAULT 1,
                FOREIGN KEY($COLUMN_PAGO_SOCIO_ID) REFERENCES $TABLE_CLIENTES($COLUMN_DOCUMENTO) ON DELETE CASCADE
            )"""


        // --- Patrón Singleton (Estilo Kotlin) ---
        @Volatile
        private var INSTANCE: DatabaseHelper? = null

        fun getInstance(context: Context): DatabaseHelper {
            // El 'synchronized(this)' asegura que solo un hilo a la vez
            // pueda crear la instancia si no existe.
            return INSTANCE ?: synchronized(this) {
                val instance = DatabaseHelper(context.applicationContext)
                INSTANCE = instance
                // Retorna la instancia recién creada
                instance
            }
        }
    }
}