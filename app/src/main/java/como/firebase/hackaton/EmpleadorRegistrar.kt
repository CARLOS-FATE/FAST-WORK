package como.firebase.hackaton

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import como.firebase.hackaton.databinding.RegistroEmpleadorBinding

class EmpleadorRegistrar : AppCompatActivity() {
    private lateinit var binding: RegistroEmpleadorBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = RegistroEmpleadorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Configura el icono de ojo para mostrar u ocultar la contraseña
        setupPasswordVisibilityToggle()

        // Establecer el comportamiento para registrar el trabajo
        binding.Resgistrarse.setOnClickListener {
            registerBusiness()
        }
        // Establecer el color del texto en negro para todos los campos de texto
        binding.Servicio1.setTextColor(ContextCompat.getColor(this,R.color.black))
        binding.DNI1.setTextColor(ContextCompat.getColor(this,R.color.black))
        binding.phone1.setTextColor(ContextCompat.getColor(this,R.color.black))
        binding.pais1.setTextColor(ContextCompat.getColor(this,R.color.black))
        binding.departamento1.setTextColor(ContextCompat.getColor(this,R.color.black))
        binding.ciudad1.setTextColor(ContextCompat.getColor(this,R.color.black))
        binding.Correoa1.setTextColor(ContextCompat.getColor(this,R.color.black))
        binding.passwordlog.setTextColor(ContextCompat.getColor(this,R.color.black))
    }

    // Configurar el ícono de ojo para mostrar u ocultar la contraseña
    private fun setupPasswordVisibilityToggle() {
        binding.eyeregistrer.setOnClickListener {
            togglePasswordVisibility()
        }
    }

    // Alternar entre mostrar y ocultar contraseña
    private fun togglePasswordVisibility() {
        if (isPasswordVisible) {
            binding.passwordlog.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            binding.eyeregistrer.setImageResource(R.drawable.eyepass) // Cambiar a ícono de ojo cerrado
        } else {
            binding.passwordlog.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            binding.eyeregistrer.setImageResource(R.drawable.eyeclose) // Cambiar a ícono de ojo abierto
        }
        isPasswordVisible = !isPasswordVisible
        binding.passwordlog.setSelection(binding.passwordlog.text.length) // Mover el cursor al final
    }

    private fun registerBusiness() {
        val nombreservicio = binding.Servicio1.text.toString().trim()
        val dni = binding.DNI1.text.toString().trim()
        val telefono = binding.phone1.text.toString().trim()
        val nombrepais = binding.pais1.text.toString().trim()
        val departamento = binding.departamento1.text.toString().trim()
        val ciudad = binding.ciudad1.text.toString().trim()
        val correo = binding.Correoa1.text.toString().trim()
        val contraseña = binding.passwordlog.text.toString().trim()

        // Validación de los campos
        if (nombreservicio.isEmpty() || dni.isEmpty() || telefono.isEmpty() ||
            nombrepais.isEmpty() || departamento.isEmpty() || ciudad.isEmpty() || correo.isEmpty() || contraseña.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        // Validación de DNI (solo números, longitud 8)
        if (!dni.matches("\\d{8}".toRegex())) {
            Toast.makeText(this, "El DNI debe tener exactamente 8 dígitos numéricos", Toast.LENGTH_SHORT).show()
            return
        }

        // Validación de teléfono (solo números, longitud 9)
        if (!telefono.matches("\\d{9}".toRegex())) {
            Toast.makeText(this, "El teléfono debe tener exactamente 9 dígitos numéricos", Toast.LENGTH_SHORT).show()
            return
        }

        // Crear usuario en Firebase Authentication
        auth.createUserWithEmailAndPassword(correo, contraseña)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid

                    // Guardar información adicional en Firestore
                    val empresa = hashMapOf(
                        "nombreServicio" to nombreservicio,
                        "dni" to dni,
                        "telefono" to telefono,
                        "nombrepais" to nombrepais,
                        "departamento" to departamento,
                        "ciudad" to ciudad,
                        "correo" to correo
                    )

                    if (userId != null) {
                        db.collection("Empleadores").document(userId).set(empresa)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
                                // Redirigir al MainActivity después del registro exitoso
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

    // Manejar errores específicos de registro
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
