package com.example.udsclubdeportivo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SeleccionCobroActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_seleccion_cobro)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.seleccion_cobro)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val swcSocio = findViewById<Switch>(R.id.swc_socio)
        val swcNoSocio = findViewById<Switch>(R.id.swc_noSocio)
        controlarSwitch(swcSocio,swcNoSocio)

        val btnConfirmar = findViewById<Button>(R.id.btn_confirmar)
        val btnVolver = findViewById<Button>(R.id.btn_volver)

        btnVolver.setOnClickListener {
            finish()
        }

        btnConfirmar.setOnClickListener {
            if (swcSocio.isChecked){
                val intent = Intent(this, PagarCuotaSocioActivity::class.java)
                startActivity(intent)
                finish()
            } else if (swcNoSocio.isChecked){
                val intent = Intent(this, PagarActividadNoSocioActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun controlarSwitch(swcSocio:Switch, swcNoSocio:Switch){

        swcSocio.setOnCheckedChangeListener { _, isChecked ->
            // Si swcSocio se enciende, apaga swcNoSocio
            if (isChecked) {
                swcNoSocio.isChecked = false
            }
        }
        swcNoSocio.setOnCheckedChangeListener { _, isChecked ->
            // Si swcNoSocio se enciende, apaga swcSocio
            if (isChecked) {
                swcSocio.isChecked = false
            }
        }
    }
}