package com.example.udsclubdeportivo

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.appcompat.app.AlertDialog

// Constante para la clave del documento
const val EXTRA_DOCUMENTO = "com.example.udsclubdeportivo.DOCUMENTO"

class RegistrarClienteActivity : AppCompatActivity() {

    private lateinit var clienteRepository: ClienteRepository

    private lateinit var txt_documento: EditText
    private lateinit var stcSocio: Switch
    private lateinit var stcNoSocio: Switch

    // Agregamos las vistas que nos faltan para el registro completo
    private lateinit var edtFechaNac: EditText
    private lateinit var edtFechaIns: EditText
    private lateinit var txt_nombre: EditText
    private lateinit var txt_apellido: EditText
    private lateinit var txt_telefono: EditText
    private lateinit var chekFicha: CheckBox
    private lateinit var chekApto: CheckBox


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registrar_cliente)

        clienteRepository = ClienteRepository(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.registrar_cliente)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- ENCONTRAR VISTAS ---
        edtFechaNac = findViewById(R.id.txt_FechaNac)
        edtFechaIns = findViewById(R.id.txt_FechaInscripcion)

        val btnVolver = findViewById<Button>(R.id.btn_Volver)
        val btnRegistrar = findViewById<Button>(R.id.btn_Confirmar)
        val btnLimpiar = findViewById<Button>(R.id.btn_Limpiar)

        txt_documento = findViewById(R.id.txt_documento)
        stcSocio = findViewById(R.id.stc_Socio)
        stcNoSocio = findViewById(R.id.stc_NoSocio)

        txt_nombre = findViewById(R.id.txt_nombre)
        txt_apellido = findViewById(R.id.txt_apellido)
        txt_telefono = findViewById(R.id.txt_telefono)
        chekFicha = findViewById(R.id.chbFichaMedica)
        chekApto = findViewById(R.id.chbAptoFisico)

        val campos = listOf<EditText>(
            edtFechaNac, edtFechaIns, txt_documento, txt_nombre, txt_apellido, txt_telefono
        )

        // Asignar DatePicker a ambos EditText
        edtFechaNac.setOnClickListener { mostrarDatePicker(edtFechaNac) }
        edtFechaIns.setOnClickListener { mostrarDatePicker(edtFechaIns) }

        // Lógica de Switches (se mantiene)
        stcSocio.setOnCheckedChangeListener { _, isChecked ->
            manejarCambioSwitch(stcSocio, stcNoSocio, "Socio", isChecked)
        }
        stcNoSocio.setOnCheckedChangeListener { _, isChecked ->
            manejarCambioSwitch(stcNoSocio, stcSocio, "No Socio", isChecked)
        }

        // Lógica de Checkbox (se mantiene)
        chekFicha.setOnCheckedChangeListener { _, isChecked ->
            val mensaje = if (isChecked) "Seleccionó que posee ficha médica" else "Deseleccionó que posee ficha médica"
            Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
        }

        chekApto.setOnCheckedChangeListener { _, isChecked ->
            val mensaje = if (isChecked) "Seleccionó que posee apto físico" else "Deseleccionó que posee apto físico"
            Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
        }

        btnVolver.setOnClickListener {
            val intent = Intent(this, MenuPrincipalActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnLimpiar.setOnClickListener {
            limpiarCampos(campos)
            chekFicha.isChecked = false
            chekApto.isChecked = false
            stcSocio.isChecked = false
            stcNoSocio.isChecked = false
        }

        // 3. IMPLEMENTACIÓN PARA GUARDAR DATOS Y NAVEGAR
        btnRegistrar.setOnClickListener { registrarClienteYNavegar(campos) }
    } // Fin de onCreate


    // --- NUEVO MÉTODO PARA REGISTRAR Y NAVEGAR ---
    private fun registrarClienteYNavegar(campos: List<EditText>) {

        val documento = txt_documento.text.toString().trim()
        val todosLlenos = campos.all { it.text.toString().trim().isNotEmpty() }
        val tipoSeleccionado = stcSocio.isChecked || stcNoSocio.isChecked

        if (todosLlenos && tipoSeleccionado) {

            val tipo = if (stcSocio.isChecked) "Socio" else "No Socio"
            val documentoFinal = txt_documento.text.toString()

            // Lanzar Coroutine en el hilo de IO para la operación de BD
            lifecycleScope.launch(Dispatchers.IO) {

                // Insertar cliente
                val idInsertado = clienteRepository.insertarCliente(
                    tipoCliente = tipo,
                    documento = documentoFinal,
                    nombre = txt_nombre.text.toString(),
                    apellido = txt_apellido.text.toString(),
                    telefono = txt_telefono.text.toString(),
                    fechaNacimiento = edtFechaNac.text.toString(),
                    fechaInscripcion = edtFechaIns.text.toString(),
                    tieneFichaMedica = chekFicha.isChecked,
                    tieneAptoFisico = chekApto.isChecked
                )

                // Volver al hilo principal (Main) para actualizar la UI y navegar
                withContext(Dispatchers.Main) {
                    if (idInsertado > 0) {
                        Toast.makeText(this@RegistrarClienteActivity, "Cliente registrado con éxito.", Toast.LENGTH_LONG).show()

                        val destinoActivity = if (tipo == "Socio") {
                            PagarCuotaSocioActivity::class.java
                        } else {
                            // Asumimos que la Activity del No Socio se llama PagarActividadNoSocioActivity
                            PagarActividadNoSocioActivity::class.java
                        }

                        val intent = Intent(this@RegistrarClienteActivity, destinoActivity).apply {
                            // ✅ PASAR DOCUMENTO CON INTENT.PUTEXTRA()
                            putExtra(EXTRA_DOCUMENTO, documentoFinal)
                        }
                        startActivity(intent)
                        finish()

                    } else {
                        Toast.makeText(this@RegistrarClienteActivity, "Error al registrar cliente. (Documento duplicado?)", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } else {
            Toast.makeText(this, "Debe completar todos los campos y seleccionar el tipo de cliente.", Toast.LENGTH_LONG).show()
        }
    }


    // ----------------------------------------------------
    // ⭐ FUNCIÓN AUXILIAR DE VALIDACIÓN (se mantiene) ⭐
    // ----------------------------------------------------
    private fun manejarCambioSwitch(
        targetSwitch: Switch,
        otherSwitch: Switch,
        tipo: String, // El tipo que se intenta seleccionar (Socio o No Socio)
        isChecked: Boolean
    ) {
        if (!isChecked) return // Solo actuamos al activar el switch

        val documento = txt_documento.text.toString().trim()

        if (documento.isEmpty()) {
            Toast.makeText(this, "Por favor, ingrese primero el número de documento.", Toast.LENGTH_LONG).show()
            targetSwitch.isChecked = false
            return
        }

        // 2. Lanzar búsqueda de duplicados en Coroutine (Hilo de BD)
        lifecycleScope.launch(Dispatchers.IO) {
            val tipoClienteExistente = clienteRepository.buscarClientePorDocumento(documento)

            withContext(Dispatchers.Main) {
                when (tipoClienteExistente) {
                    null -> {
                        mostrarConfirmacion(
                            "Confirmación de $tipo",
                            "El documento $documento NO está registrado. ¿Desea continuar con el registro como $tipo?",
                            { otherSwitch.isChecked = false },
                            { targetSwitch.isChecked = false }
                        )
                    }
                    "Socio" -> {
                        Toast.makeText(this@RegistrarClienteActivity, "ERROR: El documento $documento ya es un SOCIO registrado.", Toast.LENGTH_LONG).show()
                        targetSwitch.isChecked = false
                    }
                    "No Socio" -> {
                        if (tipo == "Socio") {
                            mostrarConfirmacion(
                                "¡Cliente Existente como No Socio!",
                                "El documento $documento ya está registrado como NO SOCIO. ¿Desea cambiar su estado a SOCIO y continuar con el registro de sus datos?",
                                { otherSwitch.isChecked = false },
                                { targetSwitch.isChecked = false }
                            )
                        } else {
                            Toast.makeText(this@RegistrarClienteActivity, "ERROR: El documento $documento ya es un NO SOCIO registrado.", Toast.LENGTH_LONG).show()
                            targetSwitch.isChecked = false
                        }
                    }
                }
            }
        }
    }
    // ----------------------------------------------------


    // Resto de funciones (mostrarDatePicker, limpiarCampos, mostrarConfirmacion) se mantienen
    // ...
    // Función que abre un DatePicker y setea el resultado en el EditText
    private fun mostrarDatePicker(editText: EditText) {
        val calendario = Calendar.getInstance()
        val año = calendario.get(Calendar.YEAR)
        val mes = calendario.get(Calendar.MONTH)
        val dia = calendario.get(Calendar.DAY_OF_MONTH)
        val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val dpd = DatePickerDialog(this, { _, year, month, dayOfMonth ->
            calendario.set(year, month, dayOfMonth)
            editText.setText(formatoFecha.format(calendario.time))
        }, año, mes, dia)

        dpd.show()
    }

    private fun limpiarCampos(campos: List<EditText>){
        for (campo in campos){
            campo.setText("")
        }
    }

    private fun mostrarConfirmacion(
        title: String,
        message: String,
        onConfirm: () -> Unit,
        onCancel: () -> Unit
    ) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)

        builder.setPositiveButton("Sí") { dialog, which ->
            onConfirm()
        }
        builder.setNegativeButton("No") { dialog, which ->
            onCancel()
        }
        builder.setOnCancelListener {
            onCancel()
        }
        val dialog: AlertDialog = builder.create()
        dialog.show()

    }
}