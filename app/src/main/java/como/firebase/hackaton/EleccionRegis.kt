package como.firebase.hackaton

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class EleccionRegis : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.eleccion_registro) // Asegúrate de que el nombre del layout coincida

        // Inicializar botones
        val empleadorButton: Button = findViewById(R.id.Empleador)
        val usuarioButton: Button = findViewById(R.id.Usuario)

        // Configurar listeners para los botones
        empleadorButton.setOnClickListener {
            // Acción cuando el usuario selecciona "Empleador"
            // Podrías iniciar una nueva actividad para el Empleador
            val intent = Intent(this, EmpleadorRegistrar::class.java)
            startActivity(intent)
        }

        usuarioButton.setOnClickListener {
            // Acción cuando el usuario selecciona "Usuario"
            // Podrías iniciar una nueva actividad para el Usuario
            val intent = Intent(this, UsuarioRegistrar::class.java)
            startActivity(intent)
        }
    }
}