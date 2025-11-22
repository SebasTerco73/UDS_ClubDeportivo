package com.example.udsclubdeportivo

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
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
import android.text.Editable
import android.text.TextWatcher

// Constante para recibir el documento (debe ser la misma que en RegistrarClienteActivity)
const val EXTRA_DOCUMENTOS = "com.example.udsclubdeportivo.DOCUMENTO"

class PagarCuotaSocioActivity : AppCompatActivity() {

    // --- PROPIEDADES DE VISTAS Y ESTADO ---
    private lateinit var repository: ClienteRepository

    // Vistas
    private lateinit var docSocio: EditText
    private lateinit var edtFechaPago: EditText
    private lateinit var cantCuotas: EditText
    private lateinit var tvTotal: TextView
    private lateinit var rbEfectivo: RadioButton
    private lateinit var rbTarjeta: RadioButton

    // Estado para el cálculo de vencimiento y validación
    private var ultimoVencimientoGuardado: String? = null
    private var isSocioCargado: Boolean = false // Flag para la validación de tipo/existencia

    // Asumimos un precio base, que podría venir de una configuración de BD
    private val PRECIO_BASE_CUOTA = 5000.00

    // Formateadores
    private val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    // Usamos NumberFormat y DecimalFormat para manejo de moneda local
    private val formatoMonto = NumberFormat.getCurrencyInstance(Locale.getDefault()) as DecimalFormat


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pagar_cuota_socio)

        repository = ClienteRepository(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.pagar_cuota_socio)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- ENCONTRAR VISTAS ---
        docSocio = findViewById(R.id.tv_documento)
        edtFechaPago = findViewById(R.id.txt_fecha_pago)
        cantCuotas = findViewById(R.id.tv_cantidad_cuotas)
        tvTotal = findViewById(R.id.tv_total)
        rbEfectivo = findViewById(R.id.rb_efectivo)
        rbTarjeta = findViewById(R.id.rb_tarjeta)

        // --- LÓGICA DE CARGA INICIAL (Intent) ---
        val documentoIntent = intent.getStringExtra(EXTRA_DOCUMENTOS)
        if (!documentoIntent.isNullOrEmpty()) {
            docSocio.setText(documentoIntent)
            buscarSocioYActualizarUI(documentoIntent) // Búsqueda automática
        } else {
            limpiarEstado()
        }

        edtFechaPago.setText(formatoFecha.format(Date()))

        // Inicializar con efectivo (1 cuota, monto base)
        rbEfectivo.isChecked = true
        cantCuotas.setText("1")
        cantCuotas.isEnabled = false // Deshabilitado para Efectivo
        calcularTotal()


        // --- LÓGICA DE RADIOBUTTONS Y DESCUENTO ---

        rbEfectivo.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                rbTarjeta.isChecked = false
                cantCuotas.setText("1")
                cantCuotas.isEnabled = false
                calcularTotal()
            }
        }

        rbTarjeta.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                rbEfectivo.isChecked = false
                cantCuotas.isEnabled = true
                if (cantCuotas.text.toString().toIntOrNull() ?: 0 < 1) {
                    cantCuotas.setText("1")
                }
                calcularTotal()
            } else if (!rbEfectivo.isChecked) {
                rbEfectivo.isChecked = true
            }
        }

        // IMPLEMENTACIÓN DEL TEXTWATCHER EN EL CAMPO DE CUOTAS
        cantCuotas.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                calcularTotal()
            }
        })


        // --- EVENTOS ---
        val btnBuscar = findViewById<Button>(R.id.btn_buscar)
        btnBuscar.setOnClickListener {
            val documento = docSocio.text.toString().trim()
            if (documento.isNotEmpty()) {
                buscarSocioYActualizarUI(documento)
            } else {
                Toast.makeText(this, "Por favor, ingrese un número de documento.", Toast.LENGTH_SHORT).show()
            }
        }

        val btnVolver = findViewById<Button>(R.id.btn_volver)
        btnVolver.setOnClickListener {
            val intent = Intent(this, MenuPrincipalActivity::class.java)
            startActivity(intent)
            finish()
        }

        val btnLimpiar = findViewById<Button>(R.id.btn_limpiar)
        btnLimpiar.setOnClickListener { limpiarCampos() }

        val btnPagar = findViewById<Button>(R.id.btn_confirmar)
        btnPagar.setOnClickListener { registrarPago() }

        // Asignar DatePicker EditText
        edtFechaPago.setOnClickListener { mostrarDatePicker(edtFechaPago) }
    }


    /**
     * Busca el tipo de cliente, los datos del socio y actualiza el estado interno.
     */
    private fun buscarSocioYActualizarUI(documento: String) {
        isSocioCargado = false

        lifecycleScope.launch(Dispatchers.IO) {
            val tipoCliente = repository.buscarClientePorDocumento(documento)

            withContext(Dispatchers.Main) {
                when (tipoCliente) {
                    "Socio" -> {
                        // 1. Es Socio: Procedemos a buscar el resto de datos
                        val datosSocio = repository.obtenerDatosSocio(documento)
                        val ultimoVencimiento = repository.obtenerUltimoVencimientoSocio(documento)

                        if (datosSocio != null) {
                            isSocioCargado = true
                            ultimoVencimientoGuardado = ultimoVencimiento

                            val nombreCompleto = "${datosSocio.nombre} ${datosSocio.apellido}"
                            val vencimientoTexto = ultimoVencimiento ?: "N/A (Primer Pago)"

                            Toast.makeText(this@PagarCuotaSocioActivity,
                                "Socio encontrado: $nombreCompleto | Vencimiento: $vencimientoTexto",
                                Toast.LENGTH_LONG).show()
                        } else {
                            // Este caso no debería ocurrir si el tipo es "Socio"
                            limpiarEstado()
                            mostrarAlerta("Error Interno", "Datos de Socio incompletos.")
                        }
                    }
                    "No Socio" -> {
                        // 2. Es No Socio: Bloqueamos
                        limpiarEstado()
                        docSocio.setText(documento) // Mantenemos el DNI
                        mostrarAlerta(
                            "Error de Acceso",
                            "El DNI ingresado corresponde a un NO SOCIO. Solo los Socios pueden pagar cuotas de membresía."
                        )
                    }
                    else -> {
                        // 3. No existe
                        limpiarEstado()
                        docSocio.setText(documento)
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
     * Calcula el total con descuentos y actualiza la vista tvTotal.
     */
    private fun calcularTotal() {
        val cuotas = cantCuotas.text.toString().toIntOrNull() ?: 1
        var total = PRECIO_BASE_CUOTA

        if (rbTarjeta.isChecked) {
            when (cuotas) {
                3 -> {
                    total *= 0.95 // 5% de descuento
                }
                6 -> {
                    total *= 0.90 // 10% de descuento
                }
            }
        }

        tvTotal.setText(formatoMonto.format(total))
    }

    // --- LÓGICA DE REGISTRO Y CÁLCULO DE VENCIMIENTO ---

    private fun registrarPago() {
        // 1. Recolección y validación de datos
        val documento = docSocio.text.toString().trim()
        val fechaPagoStr = edtFechaPago.text.toString().trim()
        val cantidadCuotas = cantCuotas.text.toString().toIntOrNull() ?: 0

        // 🚨 Manejo de parseo de monto
        val monto = try {
            val usParser = NumberFormat.getNumberInstance(Locale.US)
            val symbols = DecimalFormatSymbols(Locale.US)
            (usParser as DecimalFormat).decimalFormatSymbols = symbols

            // Limpiamos el símbolo de moneda local antes de parsear con el parser US
            val textoLimpio = tvTotal.text.toString().replace(formatoMonto.currency.symbol, "").replace(",", "")

            usParser.parse(textoLimpio)?.toDouble() ?: 0.0
        } catch (e: Exception) {
            0.0 // Si falla, es cero
        }

        val medioPago = when {
            rbEfectivo.isChecked -> "Efectivo"
            rbTarjeta.isChecked -> "Tarjeta de Crédito"
            else -> {
                Toast.makeText(this, "Debe seleccionar un método de pago.", Toast.LENGTH_LONG).show()
                return
            }
        }

        // 🚨 VALIDACIÓN CLAVE
        if (documento.isEmpty()) {
            Toast.makeText(this, "ERROR: El número de documento no puede estar vacío.", Toast.LENGTH_LONG).show()
            return
        }

        if (!isSocioCargado || fechaPagoStr.isEmpty() || monto <= 0 || cantidadCuotas <= 0) {

            val mensajeError = when {
                !isSocioCargado -> "ERROR: Debe buscar y cargar un Socio válido (no encontrado en BD)."
                fechaPagoStr.isEmpty() -> "ERROR: La fecha de pago está vacía."
                cantidadCuotas <= 0 -> "ERROR: La cantidad de cuotas debe ser mayor a 0."
                monto <= 0 -> "ERROR: El monto total no puede ser $0."
                else -> "Error de validación desconocido."
            }

            Toast.makeText(this, mensajeError, Toast.LENGTH_LONG).show()
            return
        }
        // --- FIN VALIDACIÓN ---

        // 2. Cálculo de Vencimiento
        val vencimientoCalculado = calcularVencimiento(fechaPagoStr, cantidadCuotas, ultimoVencimientoGuardado)

        // 3. Registro en BD (en Coroutine)
        lifecycleScope.launch(Dispatchers.IO) {
            val idInsertado = repository.registrarPagoSocio(
                codSocio = documento,
                fechaPago = fechaPagoStr,
                fechaVencimiento = formatoFecha.format(vencimientoCalculado),
                monto = monto,
                estadoPago = "Pagado",
                medioPago = medioPago,
                cantidadCuotas = cantidadCuotas
            )

            withContext(Dispatchers.Main) {
                if (idInsertado > 0) {
                    Toast.makeText(this@PagarCuotaSocioActivity, "Pago registrado. Nuevo Vencimiento: ${formatoFecha.format(vencimientoCalculado)}", Toast.LENGTH_LONG).show()
                    limpiarCampos()
                    limpiarEstado()
                } else {
                    Toast.makeText(this@PagarCuotaSocioActivity, "Error al registrar el pago. Verifique el DNI.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // --- OTRAS FUNCIONES AUXILIARES ---

    private fun calcularVencimiento(fechaPago: String, meses: Int, ultimoVencimiento: String?): Date {
        val calendario = Calendar.getInstance()
        var fechaBase: Date? = null

        if (!ultimoVencimiento.isNullOrEmpty()) {
            try {
                val vencimientoAnterior = formatoFecha.parse(ultimoVencimiento)
                val fechaPagoActual = formatoFecha.parse(fechaPago)

                if (vencimientoAnterior != null && (vencimientoAnterior.after(fechaPagoActual) || vencimientoAnterior == fechaPagoActual)) {
                    fechaBase = vencimientoAnterior
                }
            } catch (e: Exception) {
                // Si hay error en el formato, se usará la fecha de pago.
            }
        }

        if (fechaBase == null) {
            try {
                fechaBase = formatoFecha.parse(fechaPago)
            } catch (e: Exception) {
                fechaBase = Date() // Último recurso: fecha actual
            }
        }

        calendario.time = fechaBase!!
        calendario.add(Calendar.MONTH, meses)

        return calendario.time
    }

    private fun mostrarDatePicker(editText: EditText) {
        val calendario = Calendar.getInstance()
        val año = calendario.get(Calendar.YEAR)
        val mes = calendario.get(Calendar.MONTH)
        val dia = calendario.get(Calendar.DAY_OF_MONTH)

        val dpd = DatePickerDialog(this, { _, year, month, dayOfMonth ->
            calendario.set(year, month, dayOfMonth)
            editText.setText(formatoFecha.format(calendario.time))
        }, año, mes, dia)
        dpd.show()
    }

    private fun limpiarCampos() {
        docSocio.setText("")
        edtFechaPago.setText(formatoFecha.format(Date()))
        cantCuotas.setText("1")
        cantCuotas.isEnabled = false
        tvTotal.setText(formatoMonto.format(PRECIO_BASE_CUOTA))
        rbEfectivo.isChecked = true
        rbTarjeta.isChecked = false
        limpiarEstado()
    }

    private fun limpiarEstado() {
        isSocioCargado = false
        ultimoVencimientoGuardado = null
    }

    /**
     * Muestra un diálogo de alerta.
     */
    private fun mostrarAlerta(title: String, message: String) {
        android.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Aceptar", null)
            .show()
    }
}