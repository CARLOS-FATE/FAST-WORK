package como.firebase.hackaton

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.Toast
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import java.util.concurrent.TimeUnit
import como.firebase.hackaton.MapaUsuarios.UserData

class PortafolioUser : AppCompatActivity(),
    SelectLocationDialogFragment.OnLocationSelectedListener {
    private lateinit var empleoContainer: LinearLayout
    private var empleoCount = 1  // Contador para las secciones de empleo
    private var userData: UserData? = null
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_portafolio)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        userData = getUserData()

        val btnMas: ImageButton = findViewById(R.id.btnMas)
        val btnOpciones: ImageButton = findViewById(R.id.btnOpciones)
        val dialog = SelectLocationDialogFragment()

        // Initialize empleoContainer before calling addEmpleoSection
        empleoContainer = findViewById(R.id.empleoContainer)

        if (userData?.userType == 1) {
            btnMas.visibility = View.VISIBLE
            btnOpciones.visibility = View.VISIBLE

            // Referencia al contenedor donde se agregarán las secciones de empleo
            empleoContainer = findViewById(R.id.empleoContainer)

            // Manejo de eventos para los botones superiores
            btnMas.setOnClickListener {
                // Llamada para agregar una nueva sección de empleo
                dialog.show(supportFragmentManager, "SelectLocationDialog")
            }

            btnOpciones.setOnClickListener {
                Toast.makeText(this, "Botón Opciones presionado", Toast.LENGTH_SHORT).show()
            }
        } else {
            btnMas.visibility = View.GONE
            btnOpciones.visibility = View.GONE
        }

        addEmpleoSection()
    }


    private fun getUserData(): UserData {
        val sharedPreferences = getSharedPreferences("UserPreferences", MODE_PRIVATE)
        val username = sharedPreferences.getString("UserName", "")
        val email = sharedPreferences.getString("UserEmail", "")
        val userType = sharedPreferences.getInt("UserType", 0)
        val telefono = sharedPreferences.getString("UserPhoneNumber", "")
        return UserData(username, email, userType, telefono)
    }

    private fun getEmpleosData(
        onComplete: (List<Map<String, Any>>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val userId = auth.currentUser?.uid
        empleoContainer.removeAllViews() // Clear the container before fetching new data

        if (getUserData().userType == 1) {
            db.collection("empleos")
                .whereEqualTo("idUsuarioRegistro", userId)
                .get()
                .addOnSuccessListener { result: QuerySnapshot ->
                    val empleosList = result.documents.map { it.data ?: emptyMap<String, Any>() }
                    onComplete(empleosList)
                    Toast.makeText(this, "Empleos obtenidos con éxito", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { exception ->
                    onError(exception)
                }
        } else {
            db.collection("empleos")
                .get()
                .addOnSuccessListener { result: QuerySnapshot ->
                    val empleosList = result.documents.map { it.data ?: emptyMap<String, Any>() }
                    onComplete(empleosList)
                    Toast.makeText(this, "Empleos obtenidos con éxito", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { exception ->
                    onError(exception)
                }
        }
    }


    private fun addEmpleoSection() {
        empleoContainer.removeAllViews()
        getEmpleosData(
            onComplete = { empleosList ->
                empleosList.forEach { empleo ->
                    val newEmpleoSection = createEmpleoSection(empleo)
                    empleoContainer.addView(newEmpleoSection)
                    empleoCount++
                }
                showToast("Secciones de Empleo agregadas")
            },
            onError = { exception ->
                showToast("Error al obtener los empleos: ${exception.message}")
            }
        )
    }

    private fun createEmpleoSection(empleo: Map<String, Any>): LinearLayout {
        val newEmpleoSection = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 16, 16, 16)
            }
            setPadding(16, 16, 16, 16)
            setBackgroundResource(R.drawable.empleo_section_background)
        }

        val empleoTitle = createTextView(
            text = "Empleo $empleoCount: ${empleo["nombre"]}",
            textSize = 18f,
            textColor = R.color.black,
            isBold = true
        )
        val empleoDesc = createTextView(
            text = "Descripción: ${empleo["descripcion"]}",
            textSize = 14f,
            textColor = R.color.black
        )
        val empleoEstado = createEstadoTextView(empleo)
        val empleoTime = createTimeTextView(empleo)

        newEmpleoSection.addView(empleoTitle)
        newEmpleoSection.addView(empleoTime)
        newEmpleoSection.addView(empleoDesc)
        newEmpleoSection.addView(empleoEstado)

        val estado = (empleo["estado"] as? Long)?.toInt() ?: 0
        if (estado == 1) {
            val actionButton = if (getUserData().userType == 2) {
                createApplyButton(empleo)
            } else {
                createVerPostulantesButton(empleo)
            }
            newEmpleoSection.addView(actionButton)

            if (getUserData().userType == 1) {
                val terminarButton = createTerminarButton(empleo)
                newEmpleoSection.addView(terminarButton)
            }
        }

        return newEmpleoSection
    }

    private fun createTextView(
        text: String,
        textSize: Float,
        textColor: Int,
        isBold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            this.text = text
            this.textSize = textSize
            setTextColor(ContextCompat.getColor(this@PortafolioUser, textColor))
            setPadding(0, 0, 0, 8)
            if (isBold) setTypeface(null, Typeface.BOLD)
        }
    }

    private fun createEstadoTextView(empleo: Map<String, Any>): TextView {
        val estado = (empleo["estado"] as? Long)?.toInt() ?: 0
        val backgroundColor = if (estado == 1) R.color.green else R.color.red
        val text = if (estado == 1) "Pendiente" else "Terminado"

        return TextView(this).apply {
            this.text = "Estado: $text"
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@PortafolioUser, R.color.white))
            setPadding(16, 8, 16, 8)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 16f
                setColor(ContextCompat.getColor(this@PortafolioUser, backgroundColor))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                setMargins(0, 8, 0, 8)
            }
        }
    }

    private fun createTimeTextView(empleo: Map<String, Any>): TextView {
        val fechaRegistro = empleo["fechaRegistro"] as Long
        val currentTime = System.currentTimeMillis()
        val timeDifference = currentTime - fechaRegistro

        val days = TimeUnit.MILLISECONDS.toDays(timeDifference)
        val hours = TimeUnit.MILLISECONDS.toHours(timeDifference) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeDifference) % 60

        val dateFormat = java.text.SimpleDateFormat(
            "dd/MM/yyyy HH:mm:ss",
            java.util.Locale.getDefault()
        )
        val formattedDate = dateFormat.format(java.util.Date(fechaRegistro))

        val timeText = when {
            timeDifference < TimeUnit.HOURS.toMillis(1) -> "Tiempo Registrado: Hace $minutes minutos \nFecha: $formattedDate"
            timeDifference < TimeUnit.DAYS.toMillis(1) -> "Tiempo Registrado: Hace $hours horas y $minutes minutos\nFecha: $formattedDate"
            else -> "Tiempo Registrado: Hace $days días y $hours horas\nFecha: $formattedDate"
        }

        return createTextView(timeText, 14f, R.color.black)
    }

    private fun createApplyButton(empleo: Map<String, Any>): Button {
        val applyButton = Button(this).apply {
            text = "Postular"
            setTextColor(ContextCompat.getColor(this@PortafolioUser, R.color.white))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 16f
                setColor(ContextCompat.getColor(this@PortafolioUser, R.color.green))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8) // Small vertical margin
                gravity = Gravity.CENTER_HORIZONTAL // Center the button horizontally
            }
        }

        val empleoId = "${empleo["lat"]}${empleo["lng"]}".replace(".", "")
        isAlreadyApplied(empleoId) { isApplied ->
            if (isApplied) {
                applyButton.text = "Postulado"
                applyButton.background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 16f
                    setColor(ContextCompat.getColor(this@PortafolioUser, R.color.primary))
                }
                applyButton.isEnabled = false
            } else {
                applyButton.setOnClickListener {
                    BtnPostular(empleo, applyButton)
                }
            }
        }

        return applyButton
    }

    private fun createVerPostulantesButton(empleo: Map<String, Any>): Button {
        val empleoId = "${empleo["lat"]}${empleo["lng"]}".replace(".", "")
        return Button(this).apply {
            text = "Ver Postulantes"
            setTextColor(ContextCompat.getColor(this@PortafolioUser, R.color.white))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 16f
                setColor(ContextCompat.getColor(this@PortafolioUser, R.color.blue))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8) // Small vertical margin
                gravity = Gravity.CENTER_HORIZONTAL // Center the button horizontally
            }
            isEnabled = true
            setOnClickListener {
                BtnVerPostulantes(empleoId)
            }
        }
    }

    private fun createTerminarButton(empleo: Map<String, Any>): Button {
        return Button(this).apply {
            text = "Terminar Empleo"
            setTextColor(ContextCompat.getColor(this@PortafolioUser, R.color.white))
            setBackgroundColor(ContextCompat.getColor(this@PortafolioUser, R.color.red))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 16f
                setColor(ContextCompat.getColor(this@PortafolioUser, R.color.red))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8) // Small vertical margin
                gravity = Gravity.CENTER_HORIZONTAL // Center the button horizontally
            }
            setOnClickListener {
                terminarEmpleo(empleo)
            }
        }
    }

    private fun terminarEmpleo(empleo: Map<String, Any>) {
        val empleoId = "${empleo["lat"]}${empleo["lng"]}".replace(".", "")
        db.collection("empleos").document(empleoId)
            .update("estado", 0) // Cambiar el estado a 2 (Terminado)
            .addOnSuccessListener {
                showToast("Empleo terminado con éxito")
                empleoContainer.removeAllViews()
                addEmpleoSection()
            }
            .addOnFailureListener { e ->
                showToast("Error al terminar el empleo: ${e.message}")
            }
    }


    private fun BtnVerPostulantes(idEmpleo: String) {
        db.collection("empleosusuarios")
            .whereEqualTo("empleoId", idEmpleo)
            .get()
            .addOnSuccessListener { result ->
                val postulantesList = result.documents.map { it.data ?: emptyMap<String, Any>() }
                val usersList = mutableListOf<Map<String, Any>>()
                if (postulantesList.isNotEmpty()) {
                    for (postulante in postulantesList) {
                        val userId = postulante["userId"] as? String

                        if (userId != null) {
                            db.collection("usuarios").document(userId).get()
                                .addOnSuccessListener { userResult ->
                                    val userData = userResult.data ?: emptyMap<String, Any>()
                                    usersList.add(userData)
                                    if (usersList.size == postulantesList.size) {
                                        showPostulantesDialog(usersList)
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    showToast("Error al obtener los detalles del usuario: ${exception.message}")
                                }
                        }
                    }
                } else {
                    showToast("No hay postulantes")
                }
            }
            .addOnFailureListener { exception ->
                showToast("Error al obtener los postulantes: ${exception.message}")
            }
    }


    private fun showPostulantesDialog(usersList: List<Map<String, Any>>) {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater

        val dialogView = inflater.inflate(R.layout.dialog_postulantes, null)
        builder.setView(dialogView)

        val postulantesContainer: LinearLayout = dialogView.findViewById(R.id.postulantesListView)

        for (user in usersList) {
            val userView = inflater.inflate(R.layout.item_postulante, null)
            val userNameTextView = userView.findViewById<TextView>(R.id.userName)
            val userEmailTextView = userView.findViewById<TextView>(R.id.userEmail)
            val qrButton = userView.findViewById<Button>(R.id.qrButton)

            userNameTextView.text = "Nombre: ${user["nombre"]}"
            userEmailTextView.text = "Email: ${user["email"]}"

            Toast.makeText(this, "telefono: ${user["telefono"]}", Toast.LENGTH_SHORT).show()


            qrButton.setOnClickListener {
                val phoneNumber = user["telefono"] as? String
                if (phoneNumber != null) {
                    val qrBitmap = generateQRCode(phoneNumber)
                    if (qrBitmap != null) {
                        val qrDialogView = inflater.inflate(R.layout.dialog_qr_code, null)
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
                    showToast("Número de teléfono no disponible")
                }
            }

            postulantesContainer.addView(userView)
        }

        builder.setPositiveButton("Cerrar") { dialog, _ -> dialog.dismiss() }

        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun isAlreadyApplied(empleoId: String, onComplete: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            showToast("Error: Usuario no autenticado")
            onComplete(false)
            return
        }

        db.collection("empleosusuarios")
            .whereEqualTo("userId", userId)
            .whereEqualTo("empleoId", empleoId)
            .get()
            .addOnSuccessListener { result ->
                onComplete(!result.isEmpty)
            }
            .addOnFailureListener { exception ->
                showToast("Error al verificar la postulación: ${exception.message}")
                onComplete(false)
            }
    }


    private fun BtnPostular(empleo: Map<String, Any>, applyButton: Button) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirmación")
        builder.setMessage("¿Estás seguro de que deseas postular al empleo: ${empleo["nombre"]}?")

        builder.setPositiveButton("Sí") { dialog, _ ->
            // Lógica para guardar la postulación en la colección empleosusuarios
            val userId = auth.currentUser?.uid
            val empleoId = "${empleo["lat"]}${empleo["lng"]}".replace(".", "")
            val postulation = hashMapOf(
                "userId" to userId,
                "empleoId" to empleoId,
                "fechaPostulacion" to System.currentTimeMillis()
            )

            db.collection("empleosusuarios").add(postulation)
                .addOnSuccessListener {
                    showToast("Postulación exitosa al empleo: ${empleo["nombre"]}")
                    applyButton.text = "Postulado"
                    applyButton.setBackgroundColor(ContextCompat.getColor(this, R.color.primary))
                    applyButton.isEnabled = false
                    empleoContainer.removeAllViews()
                    addEmpleoSection()
                }
                .addOnFailureListener { e ->
                    showToast("Error al postular al empleo: ${e.message}")
                }

            dialog.dismiss()
        }

        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog: AlertDialog = builder.create()
        dialog.show()
    }


    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onLocationSelected(
        location: com.google.android.gms.maps.model.LatLng,
        nombreEmpleo: String,
        descripcionEmpleo: String,
        montoEmpleo: String
    ) {
        val id = "${location.latitude}${location.longitude}".replace(".", "")

        val empleo = hashMapOf(
            "nombre" to nombreEmpleo,
            "descripcion" to descripcionEmpleo,
            "monto" to montoEmpleo,
            "lat" to location.latitude,
            "lng" to location.longitude,
            "fechaRegistro" to System.currentTimeMillis(),
            "idUsuarioRegistro" to auth.currentUser?.uid,
            "estado" to 1
        )

        db.collection("empleos").document(id).set(empleo)
            .addOnSuccessListener {
                showToast("Empleo guardado con éxito")
                empleoContainer.removeAllViews()
                addEmpleoSection()
            }
            .addOnFailureListener { e ->
                showToast("Error al guardar el empleo: ${e.message}")
            }

    }
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



