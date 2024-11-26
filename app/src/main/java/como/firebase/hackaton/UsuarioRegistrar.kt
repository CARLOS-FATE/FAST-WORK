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
import net.bytebuddy.asm.Advice.This

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
        binding.Nomempresa.setTextColor(ContextCompat.getColor(this,R.color.black))
        binding.phone1.setTextColor(ContextCompat.getColor(this,R.color.black))
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
                    val userId = auth.currentUser?.uid
                    val token = auth.currentUser?.getIdToken(false)?.result?.token

                    val user = hashMapOf(
                        "nombre" to nombre,
                        "telefono" to telefono,
                        "email" to email,
                        "token" to token,
                    )

                    // Guardar datos adicionales en Firestore
                    if (userId != null) {
                        db.collection("usuarios").document(userId).set(user)
                            .addOnSuccessListener {
                                saveUserType(2)
                                saveUserData(nombre, email, token)
                                Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error al registrar en Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    val exception = task.exception
                    handleRegistrationError(exception)
                }
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

    private fun saveUserType(userType: Int) {
        val sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("UserType", userType)
        editor.apply()
    }

    private fun saveUserData(username: String, email: String, token: String?) {
        val sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("UserName", username)
        editor.putString("UserEmail", email)
        editor.putString("Token", token)
        editor.apply()
    }

}
