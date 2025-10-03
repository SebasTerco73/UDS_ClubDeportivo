package com.example.udsclubdeportivo

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RegistrarCliente : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registrar_cliente)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.registrar_cliente)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val edtFechaNac = findViewById<EditText>(R.id.txt_FechaNac)
        val edtFechaIns = findViewById<EditText>(R.id.txt_FechaInscripcion)

        // 1. Encontrar el botón de volver
        val btnVolver = findViewById<Button>(R.id.btn_Volver) // Asegúrate de que el ID en tu layout sea 'btn_volver'

        // Asignar DatePicker a ambos EditText
        edtFechaNac.setOnClickListener { mostrarDatePicker(edtFechaNac) }
        edtFechaIns.setOnClickListener { mostrarDatePicker(edtFechaIns) }

        // 2. Asignar el Listener al botón de volver
        btnVolver.setOnClickListener {
            // Crear un Intent para navegar a MenuPrincipalActivity
            val intent = Intent(this, MenuPrincipalActivity::class.java)
            startActivity(intent)
            finish()
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
        }, año, mes, dia)

        dpd.show()
    }
}
