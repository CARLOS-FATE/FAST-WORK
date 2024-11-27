package como.firebase.hackaton

import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import android.text.InputType
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import como.firebase.hackaton.databinding.RegistroUsersBinding

class UsuarioRegistrar : AppCompatActivity() {
    private lateinit var binding: RegistroUsersBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = RegistroUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Configurar visibilidad de contraseña
        setupPasswordVisibilityToggle()

        // Configurar botón de registro
        binding.registraru.setOnClickListener {
            registerUser()
        }

        // Configurar colores del texto
        binding.Nomempresa.setTextColor(ContextCompat.getColor(this, R.color.black))
        binding.phone1.setTextColor(ContextCompat.getColor(this, R.color.black))
        binding.Correoa1.setTextColor(ContextCompat.getColor(this, R.color.black))
        binding.passwordlog.setTextColor(ContextCompat.getColor(this, R.color.black))
    }

    private fun setupPasswordVisibilityToggle() {
        binding.eyeIcon.setOnClickListener {
            togglePasswordVisibility()
        }
    }

    private fun togglePasswordVisibility() {
        // Alternar entre mostrar y ocultar contraseña
        if (isPasswordVisible) {
            binding.passwordlog.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            binding.eyeIcon.setImageResource(R.drawable.eyepass) // Cambia al ícono de ojo cerrado
        } else {
            binding.passwordlog.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            binding.eyeIcon.setImageResource(R.drawable.eyeclose) // Cambia al ícono de ojo abierto
        }
        isPasswordVisible = !isPasswordVisible
        binding.passwordlog.setSelection(binding.passwordlog.text.length) // Mueve el cursor al final
    }

    private fun registerUser() {
        val nombre = binding.Nomempresa.text.toString().trim()
        val telefono = binding.phone1.text.toString().trim()
        val email = binding.Correoa1.text.toString().trim()
        val contrasena = binding.passwordlog.text.toString().trim()

        // Validación de campos vacíos
        if (nombre.isEmpty() || telefono.isEmpty() || email.isEmpty() || contrasena.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        // Validación de teléfono (solo números, longitud 9)
        if (!telefono.matches("\\d{9}".toRegex())) {
            Toast.makeText(this, "El teléfono debe tener exactamente 9 dígitos numéricos", Toast.LENGTH_SHORT).show()
            return
        }

        // Registro en Firebase Authentication
        auth.createUserWithEmailAndPassword(email, contrasena)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val currentUser = auth.currentUser

                    // Enviar correo de verificación
                    currentUser?.sendEmailVerification()?.addOnCompleteListener { verificationTask ->
                        if (verificationTask.isSuccessful) {
                            Toast.makeText(
                                this,
                                "Registro exitoso. Verifica tu correo para activar la cuenta.",
                                Toast.LENGTH_LONG
                            ).show()

                            // Guardar datos en Firestore
                            saveUserToFirestore(currentUser.uid, nombre, telefono, email)

                            // Redirigir a la pantalla de inicio de sesión
                            startActivity(Intent(this, MainActivity::class.java)) // MainActivity es la pantalla de inicio de sesión
                            finish()
                        } else {
                            Toast.makeText(
                                this,
                                "No se pudo enviar el correo de verificación. Intenta nuevamente.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    val exception = task.exception
                    handleRegistrationError(exception)
                }
            }
    }

    private fun saveUserToFirestore(userId: String, nombre: String, telefono: String, email: String) {
        val user = hashMapOf(
            "nombre" to nombre,
            "telefono" to telefono,
            "email" to email,
            "verificado" to false // Indicamos que no está verificado aún
        )

        db.collection("usuarios").document(userId).set(user)
            .addOnSuccessListener {
                Toast.makeText(this, "Datos guardados en Firestore", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar en Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun handleRegistrationError(exception: Exception?) {
        when (exception) {
            is com.google.firebase.auth.FirebaseAuthUserCollisionException -> {
                Toast.makeText(this, "El correo ya está registrado. Intenta iniciar sesión.", Toast.LENGTH_SHORT).show()
            }
            is com.google.firebase.auth.FirebaseAuthWeakPasswordException -> {
                Toast.makeText(this, "La contraseña es demasiado débil.", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(this, "Error en el registro: ${exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
