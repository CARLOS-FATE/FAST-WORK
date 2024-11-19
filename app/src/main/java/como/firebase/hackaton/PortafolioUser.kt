package como.firebase.hackaton

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class PortafolioUser : AppCompatActivity() {
    private lateinit var empleoContainer: LinearLayout
    private var empleoCount = 1  // Contador para las secciones de empleo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_portafolio)

        // Referencias a los botones de la barra superior
        val btnMas: ImageButton = findViewById(R.id.btnMas)
        val btnOpciones: ImageButton = findViewById(R.id.btnOpciones)

        // Referencia al contenedor donde se agregarán las secciones de empleo
        empleoContainer = findViewById(R.id.empleoContainer)

        // Manejo de eventos para los botones superiores
        btnMas.setOnClickListener {
            // Llamada para agregar una nueva sección de empleo
            addEmpleoSection()
        }

        btnOpciones.setOnClickListener {
            Toast.makeText(this, "Botón Opciones presionado", Toast.LENGTH_SHORT).show()
        }
    }

    // Función para agregar dinámicamente una nueva sección de empleo
    private fun addEmpleoSection() {
        // Crear un nuevo LinearLayout para la sección de empleo
        val newEmpleoSection = LinearLayout(this)
        newEmpleoSection.orientation = LinearLayout.VERTICAL
        newEmpleoSection.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        newEmpleoSection.setPadding(16, 16, 16, 16)

        // Crear un TextView para el título de la sección
        val empleoTitle = TextView(this)
        empleoTitle.text = "Empleo $empleoCount: Desarrollador Android"
        empleoTitle.textSize = 18f
        empleoTitle.setTextColor(ContextCompat.getColor(this,R.color.black))
        empleoTitle.setPadding(0, 0, 0, 8)

        // Crear un TextView para la descripción
        val empleoDesc = TextView(this)
        empleoDesc.text = "Descripción: Desarrollo de aplicaciones móviles en Kotlin."
        empleoDesc.textSize = 14f
        empleoDesc.setTextColor(ContextCompat.getColor(this,R.color.black))
        empleoDesc.setPadding(0, 0, 0, 8)

        // Crear un TextView para el subtítulo
        val empleoSubtitle = TextView(this)
        empleoSubtitle.text = "Subtítulo: Trabajo remoto"
        empleoSubtitle.textSize = 14f
        empleoSubtitle.setTextColor(ContextCompat.getColor(this, R.color.black))

        // Agregar los TextViews al LinearLayout de la nueva sección
        newEmpleoSection.addView(empleoTitle)
        newEmpleoSection.addView(empleoSubtitle)
        newEmpleoSection.addView(empleoDesc)

        // Incrementar el contador de las secciones de empleo
        empleoCount++

        // Agregar la nueva sección al contenedor
        empleoContainer.addView(newEmpleoSection)

        // Mostrar un mensaje indicando que se ha agregado una nueva sección
        Toast.makeText(this, "Sección de Empleo $empleoCount agregada", Toast.LENGTH_SHORT).show()
    }
}
