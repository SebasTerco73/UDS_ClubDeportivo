package com.example.udsclubdeportivo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MenuPrincipalActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu_principal)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.menu_principal)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 2. Extraer el valor del nombre de usuario usando la clave "usuario_logueado"
        val usuarioLogueado = intent.getStringExtra("user")

        // 3. Mostrar el nombre de usuario en un TextView
        // Primero, necesitas un TextView en tu layout. Asegúrate de que tenga el id `tv_nombre_usuario`
        val name_user = findViewById<TextView>(R.id.txt_usu)
        if (usuarioLogueado != null) {
            name_user.text = "¡Usuario: $usuarioLogueado!"
        }else{
            name_user.text = "¡Usuario: Anonimo!"
        }

        //Eventos de click

        val btnRegistrar = findViewById<Button>(R.id.btn_RegistrarCliente)
        btnRegistrar.setOnClickListener {
            val intent = Intent(this, RegistrarClienteActivity::class.java)
            startActivity(intent)
         finish()
        }

        val btnPagar = findViewById<Button>(R.id.btn_Pagos)
        btnPagar.setOnClickListener {
            val intent = Intent(this, SeleccionCobroActivity::class.java)
            startActivity(intent)
        }

        val btnVencimientos = findViewById<Button>(R.id.btn_Vencimientos)
        btnVencimientos.setOnClickListener {
            val intent = Intent(this, ListarVencimientoActivity::class.java)
            startActivity(intent)
        }

        val btnCerrarSesion = findViewById<Button>(R.id.btn_CerrarSesion)
        btnCerrarSesion.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}