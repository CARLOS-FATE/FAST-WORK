package como.firebase.hackaton

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Duración del splash screen
        val splashScreenDuration = 2000L // 2 segundos

        Handler(Looper.getMainLooper()).postDelayed({
            // Iniciar LoginActivity después de la duración del splash screen
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, splashScreenDuration)
    }
}