package com.tecsup.authfirebaseapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializa Firebase
        FirebaseApp.initializeApp(this)

        setContent {
            AuthApp() // Llamamos a nuestra app con navegación
        }
    }
}