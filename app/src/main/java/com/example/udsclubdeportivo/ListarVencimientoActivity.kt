package com.example.udsclubdeportivo

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ListarVencimientoActivity : AppCompatActivity() {

    // --- Vistas ---
    private lateinit var tableContainer: LinearLayout
    private lateinit var stcSocio: Switch
    private lateinit var stcNoSocio: Switch
    private lateinit var radioButtons: List<RadioButton>

    // --- Base de Datos ---
    private lateinit var repository: ClienteRepository
    private var listaCompletaClientes: List<ClienteParaListado> = emptyList()

    // --- Formateador de Fechas (para el filtro) ---
    private val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_listar_vencimiento)

        // --- Inicializar Repositorio ---
        // Asumimos que ClienteRepository está en el mismo paquete
        // y que el 'data class ClienteParaListado' también existe.
        repository = ClienteRepository(this)

        // --- Encontrar Vistas ---
        tableContainer = findViewById(R.id.table_container)
        stcSocio = findViewById(R.id.swc_socio) // Asegúrate de que el ID sea correcto
        stcNoSocio = findViewById(R.id.swc_noSocio) // Asegúrate de que el ID sea correcto

        val rbDia = findViewById<RadioButton>(R.id.rb_dia)
        val rbSemana = findViewById<RadioButton>(R.id.rb_semana)
        val rbMes = findViewById<RadioButton>(R.id.rb_mes)
        val rbTodos = findViewById<RadioButton>(R.id.rb_todos)
        radioButtons = listOf(rbDia, rbSemana, rbMes, rbTodos)

        val btnVolver = findViewById<Button>(R.id.btn_volver)

        // --- Cargar datos de la BD ---
        // Obtenemos los datos de la BD *una sola vez* al crear la actividad
        cargarDatosDeLaBaseDeDatos()

        // --- Lógica de RadioButtons (Exclusividad) ---
        radioButtons.forEach { rb ->
            rb.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    radioButtons.forEach { if (it != buttonView) it.isChecked = false }
                    actualizarListado() // Actualiza al cambiar el filtro de tiempo
                }
            }
        }

        // --- Lógica de Switches (Exclusividad) ---
        stcSocio.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                stcNoSocio.isChecked = false
            }
            actualizarListado() // Actualiza al cambiar el tipo de cliente
        }

        stcNoSocio.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                stcSocio.isChecked = false
            }
            actualizarListado() // Actualiza al cambiar el tipo de cliente
        }

        // --- Acciones Iniciales ---
        rbTodos.isChecked = true
        stcSocio.isChecked = true
        // actualizarListado() se llamará automáticamente por el listener del switch

        btnVolver.setOnClickListener {
            finish()
        }
    }

    /**
     * Carga la lista completa de clientes desde el repositorio.
     */
    private fun cargarDatosDeLaBaseDeDatos() {
        // Ejecutamos la consulta a la BD (idealmente esto iría en un hilo secundario)
        // Esta función (obtenerClientesParaListado) debe ser la versión
        // actualizada que usa los JOINS.
        listaCompletaClientes = repository.obtenerClientesParaListado()
    }

    /**
     * Filtra los datos y redibuja la lista en el contenedor.
     */
    private fun actualizarListado() {
        tableContainer.removeAllViews()

        // 1. Filtrar por tipo de Cliente (Socio/No Socio)
        val esModoSocio = stcSocio.isChecked
        val esModoNoSocio = stcNoSocio.isChecked

        val clientesFiltradosPorTipo = listaCompletaClientes.filter { cliente ->
            (esModoSocio && cliente.esSocio) || (esModoNoSocio && !cliente.esSocio)
        }

        // 2. Determinar el filtro de tiempo
        val filtroTiempo = radioButtons.find { it.isChecked }?.text.toString()
        val calendarioHoy = Calendar.getInstance()

        // 3. Aplicar filtro de tiempo
        val clientesFinal = clientesFiltradosPorTipo.filter { cliente ->
            // Intentamos convertir la fecha (vencimiento) de la DB
            val fechaVencimiento = try {
                formatoFecha.parse(cliente.vencimiento)
            } catch (e: Exception) {
                null // Fecha inválida, no lo mostramos
            }

            if (fechaVencimiento == null) {
                false // Ignorar si la fecha no se puede leer
            } else {
                val calVencimiento = Calendar.getInstance().apply { time = fechaVencimiento }

                when (filtroTiempo) {
                    "Día" -> esMismoDia(calVencimiento, calendarioHoy)
                    "Semana" -> esMismaSemana(calVencimiento, calendarioHoy)
                    "Mes" -> esMismoMes(calVencimiento, calendarioHoy)
                    "Todos" -> true
                    else -> false
                }
            }
        }

        // 4. Generar las filas
        if (clientesFinal.isEmpty()) {
            val mensaje = TextView(this).apply { text = "No hay vencimientos para los filtros seleccionados." }
            tableContainer.addView(mensaje)
        } else {
            clientesFinal.forEach { cliente ->
                generarFila(cliente)
            }
        }
    }

    /**
     * Crea y añade una fila al LinearLayout contenedor.
     */
    private fun generarFila(cliente: ClienteParaListado) {
        val row = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xFFF0F0F0.toInt())
            setPadding(24, 24, 30, 30)
        }

        // La UI pide 4 columnas: ID, NOMBRE, VENC., ACTIVIDAD
        // Usamos los datos de ClienteParaListado
        val idText = crearTextView(cliente.id, 1f)
        val nombreText = crearTextView(cliente.nombreCompleto, 2f)
        val reservaText = crearTextView(cliente.vencimiento, 1.5f)

        // -----------------------------------------------------------------
        // ✅ MODIFICACIÓN REALIZADA
        // La actividad solo corresponde al No Socio.
        // Si es Socio, la columna de actividad se mostrará vacía.
        val actividadDisplay = if (cliente.esSocio) {
            "" // Vacío si es Socio
        } else {
            cliente.actividad // Muestra la actividad real si es No Socio
        }
        val actividadText = crearTextView(actividadDisplay, 1.5f)
        // -----------------------------------------------------------------

        row.addView(idText)
        row.addView(nombreText)
        row.addView(reservaText)
        row.addView(actividadText)

        tableContainer.addView(row)
    }

    /**
     * Función helper para crear un TextView con parámetros de peso.
     */
    private fun crearTextView(texto: String, peso: Float): TextView {
        return TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                peso
            )
            text = texto
        }
    }

    // --- Funciones Helper de Fechas ---
    private fun esMismoDia(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun esMismaSemana(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.WEEK_OF_YEAR) == cal2.get(Calendar.WEEK_OF_YEAR)
    }

    private fun esMismoMes(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
    }
}
