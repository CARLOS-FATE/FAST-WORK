package como.firebase.hackaton

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter

class MapaUsuarios : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var googleMap: GoogleMap? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var userData: UserData? = null

    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    data class UserData(
        val username: String?,
        val email: String?,
        val userType: Int,
        val telefono: String?
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = FirebaseFirestore.getInstance()
        userData = getUserData()

        initViews()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        auth = FirebaseAuth.getInstance()

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        addJobMarkers()
    }

    private fun initViews() {
        mapView = findViewById(R.id.mapView)
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        setupDrawer()
        configureIconActions()
    }

    private fun setupDrawer() {
        val headerView = navigationView.getHeaderView(0)
        val lblNombreUsuario = headerView.findViewById<TextView>(R.id.lblNombreUsurio)
        val lblEmailUsuario = headerView.findViewById<TextView>(R.id.lblemailusuario)

        val userData = getUserData()
        lblNombreUsuario.text = userData.username ?: "Usuario"
        lblEmailUsuario.text = userData.email ?: "Email"

        if (userData?.userType == 1) {
            findViewById<ImageView>(R.id.servicio).visibility = View.GONE
        } else {
            findViewById<ImageView>(R.id.servicio).visibility = View.VISIBLE
        }

        configureMenuItems()
        navigationView.setNavigationItemSelectedListener { menuItem ->
            handleNavigationMenuSelection(menuItem.itemId)
            drawerLayout.closeDrawers()
            true
        }
    }

    private fun configureMenuItems() {
        val menu = navigationView.menu
        val qrsettings = menu.findItem(R.id.nav_qrsettings)
        menu.findItem(R.id.nav_home).isVisible = false
        if (userData?.userType != 1) {
            qrsettings.isVisible = true
            qrsettings.setOnMenuItemClickListener {
                showQRSettings()
                true
            }
        } else {
            qrsettings.isVisible = false
        }
    }

    private fun handleNavigationMenuSelection(itemId: Int) {
        when (itemId) {
            else -> logout()
        }
    }

    private fun showQRSettings() {
        val phoneNumber = userData?.telefono ?: ""

        if (phoneNumber.isNotBlank()) {
            val qrBitmap = generateQRCode(phoneNumber)
            if (qrBitmap != null) {
                val qrDialogView = layoutInflater.inflate(R.layout.dialog_qr_code, null)
                val qrImageView = qrDialogView.findViewById<ImageView>(R.id.qrImageView)
                qrImageView.setImageBitmap(qrBitmap)

                AlertDialog.Builder(this)
                    .setView(qrDialogView)
                    .setPositiveButton("Cerrar") { dialog, _ -> dialog.dismiss() }
                    .show()
            } else {
                showToast("Error al generar el código QR")
            }
        } else {
            showToast("Error: No se ha registrado un número de teléfono")
            showAddPhoneNumberDialog()

        }
    }

    private fun showAddPhoneNumberDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_phone_number, null)
        val phoneNumberEditText = dialogView.findViewById<EditText>(R.id.phoneEditText)

        AlertDialog.Builder(this)
            .setTitle("Agregar Teléfono")
            .setView(dialogView)
            .setPositiveButton("Guardar") { dialog, _ ->
                val phoneNumber = phoneNumberEditText.text.toString()
                if (phoneNumber.isNotBlank()) {
                    savePhoneNumberToFirebase(phoneNumber)
                } else {
                    showToast("El número de teléfono no puede estar vacío")
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun savePhoneNumberToFirebase(phoneNumber: String) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("usuarios").document(userId)
                .update("telefono", phoneNumber)
                .addOnSuccessListener {
                    showToast("Número de teléfono actualizado")
                    logout()
                }
                .addOnFailureListener { exception ->
                    showToast("Error al actualizar el número de teléfono: ${exception.message}")
                }
        } else {
            showToast("Error: Usuario no autenticado")
        }
    }

    private fun configureIconActions() {
        findViewById<ImageView>(R.id.mapa).setOnClickListener { handleMapIconClick() }
        findViewById<ImageView>(R.id.portafolio).setOnClickListener { navigateToPortafolioUser() }

        val servicioImageView = findViewById<ImageView>(R.id.servicio)

        if (userData?.userType == 1) {
            servicioImageView.isEnabled = false
        } else {
            servicioImageView.setOnClickListener {
                getPostulados { postuladosList ->
                    val builder = AlertDialog.Builder(this)
                    val inflater = layoutInflater
                    val dialogView = inflater.inflate(R.layout.dialog_postulados, null)
                    builder.setView(dialogView)

                    val postuladosListTextView =
                        dialogView.findViewById<TextView>(R.id.postuladosList)

                    val dateFormat =
                        java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                    val postuladosString = postuladosList.joinToString("\n") { empleo ->
                        "Nombre: ${empleo["nombre"]}\nDescripción: ${empleo["descripcion"]}\nFecha de Postulación: ${
                            dateFormat.format(
                                empleo["fechaPostulacion"] as Long
                            )
                        }\n"
                    }

                    postuladosListTextView.text = postuladosString

                    builder.setPositiveButton("Cerrar") { dialog, _ -> dialog.dismiss() }

                    val dialog: AlertDialog = builder.create()
                    dialog.show()
                }
            }
        }


        findViewById<ImageView>(R.id.sanguichito).setOnClickListener { toggleDrawer() }
    }


    private fun getPostulados(onComplete: (List<Map<String, Any>>) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            showToast("Error: Usuario no autenticado")
            return
        }

        db.collection("empleosusuarios")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                val postuladosList = result.documents.map { it.data ?: emptyMap<String, Any>() }
                val empleosList = mutableListOf<Map<String, Any>>()

                for (postulado in postuladosList) {
                    val empleoId = postulado["empleoId"] as? String
                    val fechaPostulacion = postulado["fechaPostulacion"] as? Long

                    if (empleoId != null) {
                        db.collection("empleos").document(empleoId).get()
                            .addOnSuccessListener { empleoResult ->
                                val empleoData = empleoResult.data?.toMutableMap() ?: mutableMapOf()
                                empleoData["fechaPostulacion"] = fechaPostulacion
                                empleosList.add(empleoData)
                                if (empleosList.size == postuladosList.size) {
                                    onComplete(empleosList)
                                }
                            }
                            .addOnFailureListener { exception ->
                                showToast("Error al obtener los detalles del empleo: ${exception.message}")
                            }
                    }
                }
            }
            .addOnFailureListener { exception ->
                showToast("Error al obtener las postulaciones: ${exception.message}")
            }
    }


    private fun handleMapIconClick() {
        if (checkLocationPermission()) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    googleMap?.apply {
                        animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                        addMarker(
                            MarkerOptions().position(currentLatLng).title("Tu ubicación actual")
                        )
                    }
                } ?: showToast("No se pudo obtener la ubicación")
            }
        }
    }

    private fun navigateToPortafolioUser() {
        showToast("Portafolio seleccionado")
        startActivity(Intent(this, PortafolioUser::class.java).apply {
            putExtra("EXTRA_MESSAGE", "Mensaje desde la actividad principal")
        })
    }

    private fun toggleDrawer() {
        if (drawerLayout.isDrawerOpen(navigationView)) {
            drawerLayout.closeDrawer(navigationView)
        } else {
            drawerLayout.openDrawer(navigationView)
        }
    }

    private fun checkLocationPermission(): Boolean {
        return if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            false
        } else {
            true
        }
    }

    private fun addJobMarkers() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            showToast("Error: Usuario no autenticado")
            return
        }

        when (userData?.userType) {
            1 -> fetchJobsByIdRegistroUsuario(userId) { jobList ->
                jobList.forEach { job -> addMarker(job) }
            }

            2 -> fetchUserJobs(userId) { jobList ->
                jobList.forEach { job ->
                    val jobId = job["empleoId"] as? String
                    jobId?.let { fetchJobDetails(it) { jobData -> addMarker(jobData) } }
                }
            }

            else -> showToast("Error: Tipo de usuario no válido")
        }
    }

    private fun fetchJobsByIdRegistroUsuario(
        userId: String,
        onComplete: (List<Map<String, Any>>) -> Unit
    ) {
        db.collection("empleos")
            .whereEqualTo("idUsuarioRegistro", userId)
            .get()
            .addOnSuccessListener { result ->
                val jobList = result.documents.map { it.data ?: emptyMap<String, Any>() }
                onComplete(jobList)
            }
            .addOnFailureListener { exception ->
                showToast("Error al obtener los empleos: ${exception.message}")
            }
    }

    private fun fetchUserJobs(userId: String, onComplete: (List<Map<String, Any>>) -> Unit) {
        db.collection("empleosusuarios")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                val jobList = result.documents.map { it.data ?: emptyMap<String, Any>() }
                onComplete(jobList)
            }
            .addOnFailureListener { exception ->
                showToast("Error al obtener las postulaciones: ${exception.message}")
            }
    }

    private fun fetchJobDetails(jobId: String, onComplete: (Map<String, Any>) -> Unit) {
        db.collection("empleos").document(jobId).get()
            .addOnSuccessListener { jobResult ->
                val jobData = jobResult.data ?: return@addOnSuccessListener
                onComplete(jobData)
            }
            .addOnFailureListener { exception ->
                showToast("Error al obtener los detalles del empleo: ${exception.message}")
            }
    }

    private fun addMarker(jobData: Map<String, Any>) {
        val jobLat = jobData["lat"] as? Double
        val jobLng = jobData["lng"] as? Double

        if (jobLat != null && jobLng != null) {
            val jobLatLng = LatLng(jobLat, jobLng)
            googleMap?.addMarker(
                MarkerOptions()
                    .position(jobLatLng)
                    .title(jobData["nombre"] as? String ?: "Empleo")
                    .snippet(jobData["descripcion"] as? String)
            )
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        if (checkLocationPermission()) {
            googleMap?.isMyLocationEnabled = true
        }

        val corner1 = LatLng(-8.131912, -79.096836)
        val corner2 = LatLng(-8.053898, -78.927808)
        val bounds = LatLngBounds(corner1, corner2)

        googleMap?.setMinZoomPreference(12f)
        googleMap?.setMaxZoomPreference(15f)
        googleMap?.setLatLngBoundsForCameraTarget(bounds)
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0))
    }


    private fun saveUserData(username: String, email: String, telefono: String = "") {
        val sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("UserName", username)
        editor.putString("UserEmail", email)
        editor.putString("telefono", telefono)
        editor.putString("UserID", auth.currentUser?.uid)
        editor.apply()
    }

    private fun saveUserType(userType: Int) {
        val sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("UserType", userType)
        editor.apply()
    }

    private fun logout() {
        saveUserData("", "")
        saveUserType(0)
        FirebaseAuth.getInstance().signOut()
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })

        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun getUserData(): UserData {
        val sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        val username = sharedPreferences.getString("UserName", "")
        val email = sharedPreferences.getString("UserEmail", "")
        val userType = sharedPreferences.getInt("UserType", 0)
        val telefono = sharedPreferences.getString("telefono", "")
        return UserData(username, email, userType, telefono)
    }

    private fun generateQRCode(data: String): Bitmap? {
        val writer = QRCodeWriter()
        return try {
            val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bmp
        } catch (e: WriterException) {
            e.printStackTrace()
            null
        }
    }


    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

}