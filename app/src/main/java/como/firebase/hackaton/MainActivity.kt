package como.firebase.hackaton

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FacebookAuthProvider
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize FirebaseAuth
        auth = FirebaseAuth.getInstance()

        // Check if user is already logged in
        if (auth.currentUser != null) {
            // User is logged in, navigate to MapaUsuarios activity
            navigateTo(MapaUsuarios::class.java)
        } else {
            // Assign views
            initializeViews()

        }

        // Initialize Facebook CallbackManager
        callbackManager = CallbackManager.Factory.create()

        // Configure button actions
        configureButtonActions()

        // Set text color in EditTexts
        setTextColor()
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
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        showToast("Inicio de sesión exitoso")
                        navigateTo(MapaUsuarios::class.java)
                    } else {
                        showToast("Error de autenticación: ${task.exception?.message}")
                    }
                }
        } else {
            showToast("Por favor, completa todos los campos")
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
//        val signInRequest = BeginSignInRequest.builder()
//            .setGoogleIdTokenRequestOptions(
//                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
//                    .setSupported(true)
//                    .setServerClientId(getString(R.string.your_web_client_id))
//                    .setFilterByAuthorizedAccounts(false)
//                    .build()
//            )
//            .build()
//
//        try {
//            val signInClient = Identity.getSignInClient(this)
//            signInClient.beginSignIn(signInRequest)
//                .addOnSuccessListener { result ->
//                    showToast("Iniciando sesión con Google")
//                }
//                .addOnFailureListener { e ->
//                    showToast("Error al iniciar sesión con Google: ${e.message}")
//                }
//        } catch (e: Exception) {
//            showToast("Error inesperado: ${e.message}")
//        }
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
}