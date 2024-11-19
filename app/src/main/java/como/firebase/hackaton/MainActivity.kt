package como.firebase.hackaton

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FacebookAuthProvider
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    // Declare variables
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var facebookLoginButton: Button
    private lateinit var googleLoginButton: Button
    private lateinit var forgotPasswordText: TextView
    private lateinit var createAccountText: TextView
    private lateinit var eyeIcon: ImageView
    private lateinit var callbackManager: CallbackManager
    private lateinit var auth: FirebaseAuth
    private var isPasswordVisible = false
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize FirebaseAuth
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        initializeViews()

        // Initialize Facebook CallbackManager
        callbackManager = CallbackManager.Factory.create()

        // Configure button actions
        configureButtonActions()

        // Set text color in EditTexts
        setTextColor()

        // Check if user is already logged in
        if (auth.currentUser != null) {
            // User is logged in, navigate to MapaUsuarios activity
            navigateTo(MapaUsuarios::class.java)
        }
    }


    private fun initializeViews() {
        emailEditText = findViewById(R.id.userLogin)
        passwordEditText = findViewById(R.id.passwordLogin)
        loginButton = findViewById(R.id.iniciarSesion)
        facebookLoginButton = findViewById(R.id.iniciarSesionfacebook)
        googleLoginButton = findViewById(R.id.iniciarSesiongoogle)
        forgotPasswordText = findViewById(R.id.forgotPassword)
        createAccountText = findViewById(R.id.createAccount)
        eyeIcon = findViewById(R.id.eyeicon)
    }

    private fun configureButtonActions() {
        loginButton.setOnClickListener { loginUser() }
        facebookLoginButton.setOnClickListener { loginWithFacebook() }
        googleLoginButton.setOnClickListener { loginWithGoogle() }
        forgotPasswordText.setOnClickListener { navigateToForgotPassword() }
        createAccountText.setOnClickListener { navigateToCreateAccount() }
        eyeIcon.setOnClickListener { togglePasswordVisibility() }
    }

    private fun setTextColor() {
        emailEditText.setTextColor(ContextCompat.getColor(this, R.color.black))
        passwordEditText.setTextColor(ContextCompat.getColor(this, R.color.black))
    }

    private fun loginUser() {
        val email = emailEditText.text.toString()
        val password = passwordEditText.text.toString()

        if (email.isNotEmpty() && password.isNotEmpty()) {

            if (isNetworkAvailable(this)) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            showToast("Inicio de sesión exitoso")
                            val userId = auth.currentUser?.uid
                            checkUserType(userId ?: "")
                        } else {
                            showToast("Error de autenticación: ${task.exception?.message}")
                        }
                    }
            } else {
                showToast("Por favor, completa todos los campos")
            }
        } else {
            showToast("No hay conexión a Internet")
        }
    }

    private fun saveUserType(userType: Int) {
        val sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("UserType", userType)
        editor.apply()
    }

    private fun saveUserData(username: String, email: String, ) {
        val sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("UserName", username)
        editor.putString("UserEmail", email)
        editor.apply()
    }


    private fun checkUserType(userId: String) {
        // Check in Empleadores collection
        db.collection("Empleadores").document(userId).get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful && task.result.exists()) {
                    // User ID found in Empleadores collection
                    showToast("El usuario pertenece a un Empleador")
                    saveUserData(task.result.getString("nombreServicio") ?: "", task.result.getString("correo") ?: "")
                    saveUserType(1)
                    navigateTo(MapaUsuarios::class.java)
                } else {
                    // Check in Usuarios collection
                    db.collection("usuarios").document(userId).get()
                        .addOnCompleteListener { userTask ->
                            if (userTask.isSuccessful && userTask.result.exists()) {
                                // User ID found in Usuarios collection
                                showToast("El usuario pertenece a un Usuario")
                                saveUserType(2)
                                saveUserData(userTask.result.getString("nombre") ?: "", userTask.result.getString("email") ?: "")
                                navigateTo(MapaUsuarios::class.java)
                            } else {
                                // User ID not found in either collection
                                showToast("El usuario no se encuentra registrado")
                                saveUserType(0)
                            }
                        }
                }
            }
            .addOnFailureListener { exception ->
                showToast("Error al verificar el tipo de usuario: ${exception.message}")
            }
    }

    private fun loginWithFacebook() {
        showToast("Iniciar sesión con facebook no implementado")

//        LoginManager.getInstance().logInWithReadPermissions(this, listOf("email", "public_profile"))
//        LoginManager.getInstance().registerCallback(callbackManager,
//            object : FacebookCallback<LoginResult> {
//                override fun onSuccess(result: LoginResult) {
//                    handleFacebookAccessToken(result.accessToken)
//                }
//
//                override fun onCancel() {
//                    showToast("Login cancelado")
//                }
//
//                override fun onError(error: FacebookException) {
//                    showToast("Error en el login de Facebook: ${error.message}")
//                }
//            })
    }

    private fun handleFacebookAccessToken(token: AccessToken) {
        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showToast("Autenticación con Firebase exitosa")
                    navigateTo(MapaUsuarios::class.java)
                } else {
                    showToast("Error al autenticar con Firebase: ${task.exception?.message}")
                }
            }
    }

    private fun loginWithGoogle() {
        showToast("Iniciar sesión con Google no implementado")
    }


    private fun navigateToForgotPassword() {
        navigateTo(Trabajo::class.java)
    }

    private fun navigateToCreateAccount() {
        navigateTo(EleccionRegis::class.java)
        showToast("Elección")
    }

    private fun togglePasswordVisibility() {
        if (isPasswordVisible) {
            passwordEditText.inputType =
                android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            eyeIcon.setImageResource(R.drawable.eyepass)
        } else {
            passwordEditText.inputType = android.text.InputType.TYPE_CLASS_TEXT
            eyeIcon.setImageResource(R.drawable.eyeclose)
        }
        isPasswordVisible = !isPasswordVisible
        passwordEditText.setSelection(passwordEditText.text.length)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateTo(activityClass: Class<*>) {
        startActivity(Intent(this, activityClass))
        finish()
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities =
            connectivityManager.getNetworkCapabilities(network) ?: return false
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}