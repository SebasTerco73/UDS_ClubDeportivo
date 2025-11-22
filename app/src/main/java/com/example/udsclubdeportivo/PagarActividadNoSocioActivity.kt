package com.example.udsclubdeportivo

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Constante para recibir el documento (la misma que en RegistrarClienteActivity)
const val extra_documentos = "com.example.udsclubdeportivo.DOCUMENTO"

class PagarActividadNoSocioActivity : AppCompatActivity() {

    // --- PROPIEDADES DE VISTAS Y ESTADO ---
    private lateinit var repository: ClienteRepository

    // Vistas
    private lateinit var docSocio: EditText
    private lateinit var edtFechaPago: EditText
    private lateinit var spinnerActividades: Spinner
    private lateinit var rbEfectivo: RadioButton // rb_efectivo3
    private lateinit var rbTarjeta: RadioButton // rb_tarjeta3
    private lateinit var tvPrecio: EditText // tv_precio
    private lateinit var tvCupo: EditText // tv_cupo
    private lateinit var btnConfirmarNs: Button // Botón Aceptar/Confirmar

    // Estado para la validación
    private var isNoSocioCargado: Boolean = false

    // Formateadores
    private val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val formatoMonto = NumberFormat.getCurrencyInstance(Locale.getDefault()) as DecimalFormat

    // Asumimos un precio base, que podría venir de una configuración de BD
    private val PRECIO_BASE_ACTIVIDAD = 5000.00


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pagar_actividad_no_socio)

        repository = ClienteRepository(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.pagar_actividad_no_socio)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- ENCONTRAR VISTAS ---
        docSocio = findViewById(R.id.tv_documento_no_socio)
        edtFechaPago = findViewById(R.id.txt_fecha_pago)
        spinnerActividades = findViewById(R.id.spinner_actividades)
        rbEfectivo = findViewById(R.id.rb_efectivo3)
        rbTarjeta = findViewById(R.id.rb_tarjeta3)
        tvPrecio = findViewById(R.id.tv_precio)
        tvCupo = findViewById(R.id.tv_cupo)
        btnConfirmarNs = findViewById(R.id.btn_confirmar_ns)

        // --- LÓGICA DE CARGA INICIAL ---
        val documentoIntent = intent.getStringExtra(extra_documentos)
        if (!documentoIntent.isNullOrEmpty()) {
            docSocio.setText(documentoIntent)
            buscarClienteYValidarTipo(documentoIntent) // Búsqueda automática
        } else {
            limpiarEstado()
        }

        edtFechaPago.setText(formatoFecha.format(Date()))
        tvPrecio.setText(formatoMonto.format(PRECIO_BASE_ACTIVIDAD))
        rbEfectivo.isChecked = false
        rbTarjeta.isChecked = false

        // --- LISTENERS ---

        // Listener del Spinner de Actividades
        spinnerActividades.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                // Solo actualizamos el cupo si ya hay una fecha de pago válida
                val fecha = edtFechaPago.text.toString().trim()
                if (fecha.isNotEmpty() && isNoSocioCargado) {
                    actualizarCupoDisponible(fecha, parent.selectedItem.toString())
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Listener para Fecha de Pago (si cambia la fecha, recalcular el cupo)
        edtFechaPago.setOnClickListener { mostrarDatePicker(edtFechaPago)}

       // Lógica para RadioButtons
        rbEfectivo.setOnClickListener {
            if (rbEfectivo.isChecked) {
                rbTarjeta.isChecked = false
            }
        }

        rbTarjeta.setOnClickListener {
            if (rbTarjeta.isChecked) {
                rbEfectivo.isChecked = false
            }
        }
        // ----------------------------------------------------------------------


        // --- EVENTOS ---
        val btnBuscar = findViewById<Button>(R.id.btn_buscar_no_socio)
        btnBuscar.setOnClickListener {
            val documento = docSocio.text.toString().trim()
            if (documento.isNotEmpty()) {
                buscarClienteYValidarTipo(documento)
            } else {
                Toast.makeText(this, "Por favor, ingrese un número de documento.", Toast.LENGTH_SHORT).show()
            }
        }

        val btnVolver = findViewById<Button>(R.id.btn_volver_ns)
        btnVolver.setOnClickListener {
            val intent = Intent(this, MenuPrincipalActivity::class.java)
            startActivity(intent)
            finish()
        }

        val btnLimpiar = findViewById<Button>(R.id.btn_limpiar_ns)
        btnLimpiar.setOnClickListener { limpiarCampos() }

        btnConfirmarNs.setOnClickListener { registrarPagoActividad() }

        // Inicialmente deshabilitar campos de pago hasta que se valide el DNI
        habilitarCamposPago(false)
    }

    // --- LÓGICA DE CUPO ---

    private fun actualizarCupoDisponible(fecha: String, actividad: String) {
        // Operación de BD en Coroutine
        lifecycleScope.launch(Dispatchers.IO) {
            val cuposUsados = repository.contarCuposUsadosPorActividadYFecha(actividad, fecha)
            val cupoMax = repository.MAX_CUPO_ACTIVIDAD
            val cupoDisponible = cupoMax - cuposUsados

            withContext(Dispatchers.Main) {
                tvCupo.setText(cupoDisponible.toString())

                if (cupoDisponible <= 0) {
                    tvCupo.setTextColor(resources.getColor(android.R.color.holo_red_dark))
                    btnConfirmarNs.isEnabled = false
                    Toast.makeText(this@PagarActividadNoSocioActivity, "CUPO AGOTADO para $actividad en esa fecha.", Toast.LENGTH_LONG).show()
                } else {
                    tvCupo.setTextColor(resources.getColor(android.R.color.white)) // Asumiendo color blanco
                    btnConfirmarNs.isEnabled = isNoSocioCargado // Solo habilitar si el cliente está validado
                }
            }
        }
    }


    /**
     * Valida el DNI en la base de datos y habilita/deshabilita la UI.
     */
    private fun buscarClienteYValidarTipo(documento: String) {
        isNoSocioCargado = false // Resetear estado
        habilitarCamposPago(false)

        lifecycleScope.launch(Dispatchers.IO) {
            val tipoCliente = repository.buscarClientePorDocumento(documento)
            val datosCliente = repository.obtenerDatosSocio(documento)

            withContext(Dispatchers.Main) {
                when (tipoCliente) {
                    "No Socio" -> {
                        isNoSocioCargado = true
                        habilitarCamposPago(true)
                        val nombreCompleto = "${datosCliente?.nombre ?: "Cliente"} ${datosCliente?.apellido ?: "No Socio"}"
                        Toast.makeText(this@PagarActividadNoSocioActivity,
                            "Cliente $nombreCompleto listo para pagar actividad.",
                            Toast.LENGTH_LONG).show()
                        // Actualizar cupo después de validar
                        actualizarCupoDisponible(edtFechaPago.text.toString().trim(), spinnerActividades.selectedItem?.toString() ?: "")
                    }
                    "Socio" -> {
                        mostrarAlerta(
                            "Error de Acceso",
                            "El DNI corresponde a un SOCIO. Debe usar la pantalla de Pago de Cuotas."
                        )
                    }
                    else -> {
                        mostrarAlerta(
                            "Error de Búsqueda",
                            "No se encontró un cliente registrado con el DNI: $documento."
                        )
                    }
                }
            }
        }
    }

    /**
     * Habilita o deshabilita los campos de pago.
     */
    private fun habilitarCamposPago(habilitar: Boolean) {
        // La fecha de pago siempre es editable via DatePicker
        spinnerActividades.isEnabled = habilitar
        rbEfectivo.isEnabled = habilitar
        rbTarjeta.isEnabled = habilitar
        btnConfirmarNs.isEnabled = habilitar && (tvCupo.text.toString().toIntOrNull() ?: 0 > 0)
    }


    /**
     * Lógica para registrar el pago de actividad en la tabla pagos_no_socios.
     */
    private fun registrarPagoActividad() {
        val documento = docSocio.text.toString().trim()
        val fechaPagoStr = edtFechaPago.text.toString().trim()
        val actividad = spinnerActividades.selectedItem?.toString() ?: ""
        val cupoActual = tvCupo.text.toString().toIntOrNull() ?: 0

        // 1. Manejo de parseo de monto
        val monto = try {
            val usParser = NumberFormat.getNumberInstance(Locale.US)
            val symbols = DecimalFormatSymbols(Locale.US)
            (usParser as DecimalFormat).decimalFormatSymbols = symbols
            val textoLimpio = tvPrecio.text.toString().replace(formatoMonto.currency.symbol, "").replace(",", "")
            usParser.parse(textoLimpio)?.toDouble() ?: 0.0
        } catch (e: Exception) {
            0.0
        }

        val medioPago = when {
            rbEfectivo.isChecked -> "Efectivo"
            rbTarjeta.isChecked -> "Tarjeta de Crédito"
            else -> {
                Toast.makeText(this, "Debe seleccionar un método de pago.", Toast.LENGTH_LONG).show()
                return
            }
        }

        // --- VALIDACIÓN FINAL ---
        if (!isNoSocioCargado || documento.isEmpty() || monto <= 0 || actividad.isEmpty() || cupoActual <= 0) {
            val mensajeError = when {
                !isNoSocioCargado -> "ERROR: Busque un DNI válido (debe ser No Socio)."
                actividad.isEmpty() -> "ERROR: Seleccione una actividad."
                monto <= 0 -> "ERROR: El precio no puede ser $0."
                cupoActual <= 0 -> "ERROR: Cupo agotado para esta actividad y fecha."
                else -> "Complete todos los campos."
            }
            Toast.makeText(this, mensajeError, Toast.LENGTH_LONG).show()
            return
        }

        // --- REGISTRO EN BD ---
        lifecycleScope.launch(Dispatchers.IO) {
            val idInsertado = repository.registrarPagoNoSocio(
                idNoSocio = documento,
                fechaPago = fechaPagoStr,
                medioPago = medioPago,
                monto = monto,
                actividad = actividad
            )

            withContext(Dispatchers.Main) {
                if (idInsertado > 0) {
                    Toast.makeText(this@PagarActividadNoSocioActivity, "Pago de $actividad registrado exitosamente.", Toast.LENGTH_LONG).show()
                    limpiarCampos()
                } else {
                    Toast.makeText(this@PagarActividadNoSocioActivity, "Error al registrar el pago. Verifique el DNI.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // 🔹 Función que abre un DatePicker y setea el resultado en el EditText
    private fun mostrarDatePicker(editText: EditText) {
        val calendario = Calendar.getInstance()
        val año = calendario.get(Calendar.YEAR)
        val mes = calendario.get(Calendar.MONTH)
        val dia = calendario.get(Calendar.DAY_OF_MONTH)
        val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val dpd = DatePickerDialog(this, { _, year, month, dayOfMonth ->
            calendario.set(year, month, dayOfMonth)
            editText.setText(formatoFecha.format(calendario.time))
            // Recalcular cupo al cambiar la fecha
            actualizarCupoDisponible(editText.text.toString().trim(), spinnerActividades.selectedItem?.toString() ?: "")
        }, año, mes, dia)
        dpd.show()
    }

    private fun limpiarCampos(){
        docSocio.setText("")
        edtFechaPago.setText(formatoFecha.format(Date()))
        spinnerActividades.setSelection(0)
        tvPrecio.setText(formatoMonto.format(PRECIO_BASE_ACTIVIDAD))
        tvCupo.setText(repository.MAX_CUPO_ACTIVIDAD.toString()) // Resetear cupo visual
        rbEfectivo.isChecked = false
        rbTarjeta.isChecked = false
        limpiarEstado()
        habilitarCamposPago(false)
    }

    private fun limpiarEstado() {
        isNoSocioCargado = false
    }

    /**
     * Muestra un diálogo de alerta.
     */
    private fun mostrarAlerta(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Aceptar", null)
            .show()
    }
}