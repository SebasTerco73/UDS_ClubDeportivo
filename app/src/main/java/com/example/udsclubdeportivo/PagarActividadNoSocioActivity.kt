package com.example.udsclubdeportivo

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class PagarActividadNoSocioActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pagar_actividad_no_socio)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.pagar_actividad_no_socio)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //CAMPOS
        val txtDocumento = findViewById<EditText>(R.id.tv_documento_no_socio)
        val txt_fecha_pago = findViewById<EditText>(R.id.txt_fecha_pago)
        val spinnerActividades = findViewById<Spinner>(R.id.spinner_actividades)

        val campos: List<View> = listOf(
            txtDocumento,
            txt_fecha_pago,
            spinnerActividades
        )


        // BOTONES
        val btnBuscar = findViewById<Button>(R.id.btn_buscar_no_socio)
        btnBuscar.setOnClickListener {
            TODO()
        }

        val btnLimpiar = findViewById<Button>(R.id.btn_limpiar_ns)
        btnLimpiar.setOnClickListener {
            limpiarCampos(campos)
        }

        val btnVolver = findViewById<Button>(R.id.btn_volver_ns)
        btnVolver.setOnClickListener {
            finish()
        }

        val btnConfirmar = findViewById<Button>(R.id.btn_confirmar_ns)
        btnConfirmar.setOnClickListener {
            limpiarCampos(campos)
            TODO()
        }
    }

    private fun limpiarCampos(vistas: List<View>){
        for (vista in vistas) {
            if (vista is EditText) {
                vista.setText("")
            }
            else if (vista is Spinner) {
                vista.setSelection(0)
            }
        }

    }
}
