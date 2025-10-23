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
import com.example.udsclubdeportivo.PagarCuotaSocioActivity
import com.example.udsclubdeportivo.PagarActividadNoSocioActivity

class RegistrarClienteActivity : AppCompatActivity() {

    private lateinit var clienteRepository: ClienteRepository

    // Declaramos las vistas aquí para que sean accesibles en manejarCambioSwitch()
    private lateinit var txt_documento: EditText
    private lateinit var stcSocio: Switch
    private lateinit var stcNoSocio: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registrar_cliente)

        // Inicializar el Repository
        clienteRepository = ClienteRepository(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.registrar_cliente)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- ENCONTRAR VISTAS ---
        val edtFechaNac = findViewById<EditText>(R.id.txt_FechaNac)
        val edtFechaIns = findViewById<EditText>(R.id.txt_FechaInscripcion)

        val btnVolver = findViewById<Button>(R.id.btn_Volver)
        val btnRegistrar = findViewById<Button>(R.id.btn_Confirmar)
        val btnLimpiar = findViewById<Button>(R.id.btn_Limpiar)

        // Asignación a variables de clase
        txt_documento = findViewById<EditText>(R.id.txt_documento)
        stcSocio = findViewById<Switch>(R.id.stc_Socio)
        stcNoSocio = findViewById<Switch>(R.id.stc_NoSocio)

        val txt_nombre = findViewById<EditText>(R.id.txt_nombre)
        val txt_apellido = findViewById<EditText>(R.id.txt_apellido)
        val txt_telefono = findViewById<EditText>(R.id.txt_telefono)
        val chekFicha = findViewById<CheckBox>(R.id.chbFichaMedica)
        val chekApto = findViewById<CheckBox>(R.id.chbAptoFisico)

        val campos = listOf<EditText>(
            edtFechaNac, edtFechaIns, txt_documento, txt_nombre, txt_apellido, txt_telefono
        )

        // Asignar DatePicker a ambos EditText
        edtFechaNac.setOnClickListener { mostrarDatePicker(edtFechaNac) }
        edtFechaIns.setOnClickListener { mostrarDatePicker(edtFechaIns) }

        // ----------------------------------------------------------------------------------
        // ⭐ IMPLEMENTACIÓN DE SWITCH CON LÓGICA DE VALIDACIÓN Y UPGRADE ⭐
        // La función maneja la verificación en BD de forma asíncrona.
        // ----------------------------------------------------------------------------------
        stcSocio.setOnCheckedChangeListener { _, isChecked ->
            manejarCambioSwitch(stcSocio, stcNoSocio, "Socio", isChecked)
        }
        stcNoSocio.setOnCheckedChangeListener { _, isChecked ->
            manejarCambioSwitch(stcNoSocio, stcSocio, "No Socio", isChecked)
        }
        // ----------------------------------------------------------------------------------

        // implementa los chechbox
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
            // También limpiamos el estado de los CheckBox y Switches
            chekFicha.isChecked = false
            chekApto.isChecked = false
            stcSocio.isChecked = false
            stcNoSocio.isChecked = false
        }

        // 3. IMPLEMENTACIÓN PARA GUARDAR DATOS USANDO SQLITEOPENHELPER Y COROUTINES
        btnRegistrar.setOnClickListener {

            val todosLlenos = campos.all { it.text.toString().trim().isNotEmpty() }
            val tipoSeleccionado = stcSocio.isChecked || stcNoSocio.isChecked

            if (todosLlenos && tipoSeleccionado) {

                val tipo = if (stcSocio.isChecked) "Socio" else "No Socio"

                // Lanzar Coroutine en el hilo de IO para la operación de BD
                lifecycleScope.launch(Dispatchers.IO) {

                    // Insertar cliente
                    val idInsertado = clienteRepository.insertarCliente(
                        tipoCliente = tipo,
                        documento = txt_documento.text.toString(),
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
                            Toast.makeText(this@RegistrarClienteActivity, "Cliente registrado con éxito (ID: $idInsertado)", Toast.LENGTH_LONG).show()

                            val destinoActivity = if (tipo == "Socio") {
                                PagarCuotaSocioActivity::class.java
                            } else {
                                PagarActividadNoSocioActivity::class.java
                            }

                            val intent = Intent(this@RegistrarClienteActivity, destinoActivity)
                            startActivity(intent)
                            finish()

                        } else {
                            Toast.makeText(this@RegistrarClienteActivity, "Error al registrar cliente.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Debe completar todos los campos y seleccionar el tipo de cliente.", Toast.LENGTH_LONG).show()
            }
        }
    } // Fin de onCreate

    // ----------------------------------------------------
    // ⭐ FUNCIÓN AUXILIAR PARA VALIDAR, BUSCAR DUPLICADOS Y OFRECER UPGRADE ⭐
    // ----------------------------------------------------
    private fun manejarCambioSwitch(
        targetSwitch: Switch,
        otherSwitch: Switch,
        tipo: String, // El tipo que se intenta seleccionar (Socio o No Socio)
        isChecked: Boolean
    ) {
        if (!isChecked) return // Solo actuamos al activar el switch

        val documento = txt_documento.text.toString().trim()

        // 1. Validar que el campo Documento esté lleno
        if (documento.isEmpty()) {
            Toast.makeText(this, "Por favor, ingrese primero el número de documento.", Toast.LENGTH_LONG).show()
            targetSwitch.isChecked = false
            return
        }

        // 2. Lanzar búsqueda de duplicados en Coroutine (Hilo de BD)
        lifecycleScope.launch(Dispatchers.IO) {
            // Llama a la función del Repository que ahora devuelve el tipo de cliente existente
            val tipoClienteExistente = clienteRepository.buscarClientePorDocumento(documento)

            withContext(Dispatchers.Main) {
                when (tipoClienteExistente) {
                    null -> {
                        // 3. Documento NO existe: Proceder con el registro normal
                        mostrarConfirmacion(
                            "Confirmación de $tipo",
                            "El documento $documento NO está registrado. ¿Desea continuar con el registro como $tipo?",
                            {
                                // Acción SÍ: Desactiva el otro Switch
                                otherSwitch.isChecked = false
                            },
                            {
                                // Acción NO: Deshace el cambio del Switch actual
                                targetSwitch.isChecked = false
                            }
                        )
                    }
                    "Socio" -> {
                        // 4. Documento ya es SOCIO: Error de duplicado
                        Toast.makeText(this@RegistrarClienteActivity, "ERROR: El documento $documento ya es un SOCIO registrado.", Toast.LENGTH_LONG).show()
                        targetSwitch.isChecked = false
                    }
                    "No Socio" -> {
                        // 5. Documento ya es NO SOCIO: Oportunidad de UPGRADE
                        if (tipo == "Socio") {
                            mostrarConfirmacion(
                                "¡Cliente Existente como No Socio!",
                                "El documento $documento ya está registrado como NO SOCIO. ¿Desea cambiar su estado a SOCIO y continuar con el registro de sus datos?",
                                {
                                    // Acción SÍ: Permite el cambio (manteniendo targetSwitch.isChecked = true)
                                    otherSwitch.isChecked = false
                                },
                                {
                                    // Acción NO: Deshace el cambio del Switch actual
                                    targetSwitch.isChecked = false
                                }
                            )
                        } else {
                            // 6. Si es NO SOCIO y se intenta registrar como NO SOCIO: Error de duplicado
                            Toast.makeText(this@RegistrarClienteActivity, "ERROR: El documento $documento ya es un NO SOCIO registrado.", Toast.LENGTH_LONG).show()
                            targetSwitch.isChecked = false
                        }
                    }
                }
            }
        }
    }
    // ----------------------------------------------------


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
        //cancelación si se toca fuera del diálogo
        builder.setOnCancelListener {
            onCancel()
        }
        val dialog: AlertDialog = builder.create()
        dialog.show()

    }
}