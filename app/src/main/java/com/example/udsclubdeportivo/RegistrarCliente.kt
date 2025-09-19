package com.example.udsclubdeportivo

import android.app.DatePickerDialog
import android.os.Bundle
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

        // Activar Edge-to-Edge
        enableEdgeToEdge()

        // Layout principal
        setContentView(R.layout.activity_registrar_cliente)

        // Ajuste de padding para systemBars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.registrar_cliente)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- DatePicker para EditText ---
        val edtFecha = findViewById<EditText>(R.id.txt_FechaNac)
        val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        edtFecha.setOnClickListener {
            val calendario = Calendar.getInstance()
            val año = calendario.get(Calendar.YEAR)
            val mes = calendario.get(Calendar.MONTH)
            val dia = calendario.get(Calendar.DAY_OF_MONTH)

            val dpd = DatePickerDialog(this, { _, year, month, dayOfMonth ->
                calendario.set(year, month, dayOfMonth)
                edtFecha.setText(formatoFecha.format(calendario.time))
            }, año, mes, dia)

            dpd.show()
        }
    }
}
