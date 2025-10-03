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
        val intent = intent

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
        // Dentro de tu Activity actual (por ejemplo MainActivity)
        val btnRegistrar = findViewById<Button>(R.id.btn_RegistrarCliente)

        btnRegistrar.setOnClickListener {
            val intent = Intent(this, RegistrarCliente::class.java)
            startActivity(intent)
            finish()
        }

        // btn_Vencimientos
        val btnVencimientos = findViewById<Button>(R.id.btn_Vencimientos)

        btnVencimientos.setOnClickListener {
            val intent = Intent(this, ListarVencimientoActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}