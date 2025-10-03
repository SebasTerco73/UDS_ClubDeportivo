package com.example.udsclubdeportivo

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PagarCuotaSocioActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pagar_cuota_socio)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.pagar_cuota_socio)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val docSocio = findViewById<EditText>(R.id.tv_documento)
        val edtFechaPago = findViewById<EditText>(R.id.txt_fecha_pago)
        val cantCuotas = findViewById<TextView>(R.id.tv_cantidad_cuotas)

        // evento botones

        var btnBuscar = findViewById<Button>(R.id.btn_buscar)
        btnBuscar.setOnClickListener {
            TODO()
        //Falta implementar
        }

        val btnVolver = findViewById<Button>(R.id.btn_volver) // Asegúrate de que el ID en tu layout sea 'btn_volver'
        btnVolver.setOnClickListener {
            finish()
        }

        val btnLimpiar = findViewById<Button>(R.id.btn_limpiar)
        btnLimpiar.setOnClickListener {
            limpiarCampos(docSocio,edtFechaPago,cantCuotas)
        }

        val btnPagar = findViewById<Button>(R.id.btn_confirmar)
        btnPagar.setOnClickListener {
            //mostrar confirmacion
            limpiarCampos(docSocio,edtFechaPago,cantCuotas)
        }

        // Asignar DatePicker EditText
        edtFechaPago.setOnClickListener { mostrarDatePicker(edtFechaPago)}
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
        }, año, mes, dia)
        dpd.show()
    }

    private fun limpiarCampos(docSocio: EditText, edtFechaNac: EditText, cantCuotas: TextView){
        docSocio.setText("")
        edtFechaNac.setText("")
        cantCuotas.setText("1")
    }

}