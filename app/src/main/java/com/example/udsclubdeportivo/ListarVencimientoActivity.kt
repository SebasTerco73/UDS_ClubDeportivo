package com.example.udsclubdeportivo
        import android.content.Intent
        import android.os.Bundle
        import android.widget.Button
        import android.widget.LinearLayout
                import android.widget.RadioButton
                import android.widget.TextView
                import androidx.appcompat.app.AppCompatActivity

        class ListarVencimientoActivity : AppCompatActivity() {

            data class actividadNoSocio(
                val id: String,
                val nombre: String,
                val reserva: String,
                val actividad: String
            )
            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                setContentView(R.layout.activity_listar_vencimiento)

                val rbDia = findViewById<RadioButton>(R.id.rb_dia)
                val rbSemana = findViewById<RadioButton>(R.id.rb_semana)
                val rbMes = findViewById<RadioButton>(R.id.rb_mes)
                val rbTodos = findViewById<RadioButton>(R.id.rb_todos)

                val radioButtons = listOf(rbDia, rbSemana, rbMes, rbTodos)

                radioButtons.forEach { rb ->
                    rb.setOnCheckedChangeListener { buttonView, isChecked ->
                        if (isChecked) {
                            radioButtons.forEach { if (it != buttonView) it.isChecked = false }
                        }
                    }
                }

                val tableContainer = findViewById<LinearLayout>(R.id.table_container)

                // Ejemplos para implementacion
                val actividadNoSocio = listOf(
                    actividadNoSocio("2218", "Ivan Gomez", "26/01/25", "FUTBOL"),
                    actividadNoSocio("132", "Abril Lopez", "15/06/22", "NATACION")
                )

                for (noSocio in actividadNoSocio) {
                    val row = LinearLayout(this)
                    row.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    row.orientation = LinearLayout.HORIZONTAL
                    row.setBackgroundColor(0xFFF0F0F0.toInt())
                    row.setPadding(24, 24, 30, 30)

                    val idText = TextView(this)
                    idText.layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                    idText.text = noSocio.id

                    val nombreText = TextView(this)
                    nombreText.layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        2f
                    )
                    nombreText.text = noSocio.nombre

                    val reservaText = TextView(this)
                    reservaText.layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1.5f
                    )
                    reservaText.text = noSocio.reserva

                    val actividadText = TextView(this)
                    actividadText.layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1.5f
                    )
                    actividadText.text = noSocio.actividad

                    row.addView(idText)
                    row.addView(nombreText)
                    row.addView(reservaText)
                    row.addView(actividadText)

                    tableContainer.addView(row)
                }
                val btnVolver = findViewById<Button>(R.id.btn_volver)
                btnVolver.setOnClickListener {
                    finish()
                }

            }
        }

