package como.firebase.hackaton

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FacebookAuthProvider
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import como.firebase.hackaton.MapaUsuarios.UserData
import android.content.Intent as Intent1

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
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var messaging: FirebaseMessaging

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize FirebaseAuth
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        messaging = FirebaseMessaging.getInstance()

        // Initialize views
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


    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth.currentUser
                    val uid = user?.uid ?: ""
                    val token = user?.getIdToken(false)?.result?.token ?: ""
                    checkUserType(uid, token) // Use the UID
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                }
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

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
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
                // Inflate the terms and conditions dialog layout
                val dialogView = layoutInflater.inflate(R.layout.dialog_terminos_condiciones, null)
                val dialog = AlertDialog.Builder(this)
                    .setView(dialogView)
                    .setCancelable(false)
                    .create()

                // Set up the buttons in the dialog
                dialogView.findViewById<Button>(R.id.btnAceptarT).setOnClickListener {
                    // User accepted the terms
                    dialog.dismiss()
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val token = auth.currentUser?.getIdToken(false)?.result?.token
                                showToast("Inicio de sesión exitoso")
                                val userId = auth.currentUser?.uid
                                checkUserType(userId ?: "", token)
                            } else {
                                showToast("Error de autenticación: ${task.exception?.message}")
                            }
                        }
                }

                dialogView.findViewById<Button>(R.id.btnNoAceptarT).setOnClickListener {
                    // User did not accept the terms
                    dialog.dismiss()
                    showToast("Debe aceptar los términos y condiciones para continuar")
                }

                // Show the dialog
                dialog.show()
            } else {
                showToast("No hay conexión a Internet")
            }
        } else {
            showToast("Por favor, completa todos los campos")
        }
    }

    private fun SaveUserTokenAuth(token: String, userId: String, usertype: Int) {
        if (usertype == 1) {
            db.collection("Empleadores").document(userId).update("token", token)
        }

        if (usertype == 2) {
            db.collection("usuarios").document(userId).update("token", token)
        }

        val sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("UserToken", token)
        editor.apply()
    }

    private fun saveUserType(userType: Int) {
        val sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("UserType", userType)
        editor.apply()
    }

    private fun saveUserData(username: String, email: String, telefono: String = "") {
        val sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("UserName", username)
        editor.putString("UserEmail", email)
        editor.putString("telefono", telefono)
        editor.apply()
    }


    private fun checkUserType(userId: String, token: String? = null) {
        // Check in Empleadores collection
        db.collection("Empleadores").document(userId).get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful && task.result.exists()) {
                    // User ID found in Empleadores collection
                    showToast("El usuario pertenece a un Empleador")
                    saveUserData(
                        task.result.getString("nombreServicio") ?: "",
                        task.result.getString("correo") ?: ""
                    )
                    SaveUserTokenAuth(token ?: "", userId, 1)
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
                                SaveUserTokenAuth(token ?: "", userId, 2)

                                saveUserData(
                                    userTask.result.getString("nombre") ?: "",
                                    userTask.result.getString("email") ?: "",
                                    userTask.result.getString("telefono") ?: ""
                                )
                                navigateTo(MapaUsuarios::class.java)
                            } else {
                                // User ID not found in either collection, create new user
                                val userData = getUserData()
                                val newUser = hashMapOf(
                                    "nombre" to userData.username,
                                    "email" to userData.email,
                                    "telefono" to userData.telefono
                                )
                                db.collection("usuarios").document(userId).set(newUser)
                                    .addOnSuccessListener {
                                        showToast("Nuevo usuario creado")
                                        saveUserType(2)
                                        SaveUserTokenAuth(token ?: "", userId, 2)
                                        navigateTo(MapaUsuarios::class.java)
                                    }
                                    .addOnFailureListener { exception ->
                                        showToast("Error al crear el usuario: ${exception.message}")
                                    }
                            }
                        }
                }
            }
            .addOnFailureListener { exception ->
                showToast("Error al verificar el tipo de usuario: ${exception.message}")
            }
    }

    private fun getUserData(): UserData {
        val sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        val username = sharedPreferences.getString("UserName", "")
        val email = sharedPreferences.getString("UserEmail", "")
        val userType = sharedPreferences.getInt("UserType", 0)
        val telefono = sharedPreferences.getString("telefono", "")
        return UserData(username, email, userType, telefono)
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
        if (!isNetworkAvailable(this)) {
            showToast("No hay conexión a Internet")
            return
        }

        val inflater = layoutInflater

        // Inflate the terms and conditions dialog layout
        val dialogView = inflater.inflate(R.layout.dialog_terminos_condiciones, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // Set up the buttons in the dialog
        dialogView.findViewById<Button>(R.id.btnAceptarT).setOnClickListener {
            saveUserType(2)
            singInGoogle()
            dialog.dismiss() // Dismiss the first dialog before showing the second one
        }

        dialogView.findViewById<Button>(R.id.btnNoAceptarT).setOnClickListener {
            dialog.dismiss()
            showToast("Debe aceptar los términos y condiciones para continuar")
        }

        // Show the first dialog
        dialog.show()
    }


    private fun singInGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
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


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent1?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)

                // Retrieve user details
                val email = account.email
                val displayName = account.displayName

                saveUserData(displayName ?: "", email ?: "", "")

                // Authenticate with Firebase
                firebaseAuthWithGoogle(account.idToken!!)

            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e)
            }
        }
    }


    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateTo(activityClass: Class<*>) {
        startActivity(Intent1(this, activityClass))
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

    companion object {
        private const val TAG = "GoogleActivity"
        private const val RC_SIGN_IN = 9001
    }

}