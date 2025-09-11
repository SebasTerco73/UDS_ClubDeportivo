package com.example.udsclubdeportivo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.ImageButton
import android.widget.TextView

class LoginActivity : AppCompatActivity() {

// Lista de pares - it.first/ it.second acceder a los valores
    private val users = listOf(
        "sebas" to "pass1",
        "user2" to "pass2"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // BOTÓN DE SALIR (ImageButton)
        val btnSalir = findViewById<ImageButton>(R.id.btn_salir)
        btnSalir.setOnClickListener {
            finishAffinity() // Sale de la app
        }
        // Login
        val txtUser = findViewById<EditText>(R.id.txt_user)
        val txtPass = findViewById<EditText>(R.id.txt_pass)
        val btnLogin = findViewById<Button>(R.id.btn_iniciarSesion)

        btnLogin.setOnClickListener {
            val username = txtUser.text.toString()
            val password = txtPass.text.toString()

            val loginValido = users.any { it.first == username && it.second == password }

            if (loginValido) {
                // Abrir MenuPrincipalActivity
                val intent = Intent(this, MenuPrincipalActivity::class.java)
                // Clave para identificar el dato: "usuario_logueado"
                // Valor: la variable 'username' que contiene el nombre de usuario
                intent.putExtra("user", username)

                startActivity(intent)

                // Opcional: cerrar Login para que no vuelva con el botón Atrás del cel
                finish()
            } else {
                val error = findViewById<TextView>(R.id.txt_login_error)
                error.visibility = View.VISIBLE
            }
        }
    }
}