package como.firebase.hackaton

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
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
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth

class MapaUsuarios : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var googleMap: GoogleMap? = null

    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
    }

    private fun initViews() {
        mapView = findViewById(R.id.mapView)
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        setupDrawer()
        configureIconActions()
    }

    private fun setupDrawer() {
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, R.string.drawer_open, R.string.drawer_close
        ).apply {
            drawerLayout.addDrawerListener(this)
            syncState()
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            handleNavigationMenuSelection(menuItem.itemId)
            drawerLayout.closeDrawers()
            true
        }
    }

    private fun handleNavigationMenuSelection(itemId: Int) {
        when (itemId) {
            R.id.nav_home -> showToast("Inicio seleccionado")
            R.id.nav_settings -> showToast("Configuración seleccionada")
            else -> logout()
        }
    }

    private fun configureIconActions() {
        findViewById<ImageView>(R.id.mapa).setOnClickListener { handleMapIconClick() }
        findViewById<ImageView>(R.id.portafolio).setOnClickListener { navigateToPortafolioUser() }
        findViewById<ImageView>(R.id.servicio).setOnClickListener { showToast("Servicio seleccionado") }
        findViewById<ImageView>(R.id.sanguichito).setOnClickListener { toggleDrawer() }
    }

    private fun handleMapIconClick() {
        if (checkLocationPermission()) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    googleMap?.apply {
                        animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                        addMarker(MarkerOptions().position(currentLatLng).title("Tu ubicación actual"))
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
        return if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE
            )
            false
        } else {
            true
        }
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        if (checkLocationPermission()) {
            googleMap?.isMyLocationEnabled = true
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