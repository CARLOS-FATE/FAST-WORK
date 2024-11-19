package como.firebase.hackaton

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions

class SelectLocationDialogFragment : DialogFragment(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private var googleMap: GoogleMap? = null
    private var selectedLocation: LatLng? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_select_location, container, false)
        mapView = view.findViewById(R.id.mapViewDialog)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        val nombreempleo = view.findViewById<EditText>(R.id.nombreEmpleoDialog)
        val descripcionempleo = view.findViewById<EditText>(R.id.descripcionEmpleoDialog)
        val montoEmpleo = view.findViewById<EditText>(R.id.montoEmpleoDialog)

        view.findViewById<Button>(R.id.btnSelectLocationDialog).setOnClickListener {
            if (validateFields(nombreempleo, descripcionempleo, montoEmpleo)) {
                selectedLocation?.let {
                    // Pass the selected location and other values back to the activity
                    (activity as? OnLocationSelectedListener)?.onLocationSelected(
                        it,
                        nombreempleo.text.toString(),
                        descripcionempleo.text.toString(),
                        montoEmpleo.text.toString()
                    )
                    dismiss()
                } ?: run {
                    Toast.makeText(requireContext(), "Por favor seleccione una ubicación", Toast.LENGTH_SHORT).show()
                }
            }
        }

        view.findViewById<Button>(R.id.btnCancelarEmpleoDialog).setOnClickListener {
            dismiss()
        }

        return view
    }

    private fun validateFields(
        nombreempleo: EditText,
        descripcionempleo: EditText,
        montoEmpleo: EditText
    ): Boolean {
        return when {
            nombreempleo.text.isEmpty() -> {
                Toast.makeText(requireContext(), "Por favor llene el nombre del empleo", Toast.LENGTH_SHORT).show()
                false
            }
            descripcionempleo.text.isEmpty() -> {
                Toast.makeText(requireContext(), "Por favor llene la descripción del empleo", Toast.LENGTH_SHORT).show()
                false
            }
            montoEmpleo.text.isEmpty() -> {
                Toast.makeText(requireContext(), "Por favor llene el monto del empleo", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.setOnMapClickListener { latLng ->
            googleMap?.clear()
            googleMap?.addMarker(MarkerOptions().position(latLng).title("Selected Location"))
            selectedLocation = latLng
        }
        // Enable zoom controls
        googleMap?.uiSettings?.isZoomControlsEnabled = true

        val corner1 = LatLng(-8.131912, -79.096836)
        val corner2 = LatLng(-8.053898, -78.927808)
        val bounds = LatLngBounds(corner1, corner2)

        googleMap?.setMinZoomPreference(12f)
        googleMap?.setMaxZoomPreference(15f)
        googleMap?.setLatLngBoundsForCameraTarget(bounds)
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0))
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

    interface OnLocationSelectedListener {
        fun onLocationSelected(location: LatLng, nombreEmpleo: String, descripcionEmpleo: String, montoEmpleo: String)
    }
}