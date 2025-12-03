package com.tecsup.authfirebaseapp

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val email = currentUser?.email ?: "Usuario"
    val uid = currentUser?.uid

    val context = LocalContext.current
    val db = Firebase.firestore

    var courses by remember { mutableStateOf(listOf<Course>()) }
    var isLoading by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var editingCourse by remember { mutableStateOf<Course?>(null) }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var creditsText by remember { mutableStateOf("") }

    fun loadCourses() {
        if (uid == null) return
        isLoading = true
        db.collection("courses")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { snapshot ->
                courses = snapshot.documents.map { doc ->
                    Course(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        description = doc.getString("description") ?: "",
                        credits = (doc.getLong("credits") ?: 0L).toInt(),
                        userId = doc.getString("userId") ?: ""
                    )
                }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
                Toast.makeText(context, "Error al cargar cursos", Toast.LENGTH_SHORT).show()
            }
    }

    LaunchedEffect(uid) {
        loadCourses()
    }

    fun openCreateDialog() {
        editingCourse = null
        title = ""
        description = ""
        creditsText = ""
        showDialog = true
    }

    fun openEditDialog(course: Course) {
        editingCourse = course
        title = course.title
        description = course.description
        creditsText = course.credits.toString()
        showDialog = true
    }

    fun saveCourse() {
        val credits = creditsText.toIntOrNull() ?: 0
        if (uid == null) return

        val data = hashMapOf(
            "title" to title,
            "description" to description,
            "credits" to credits,
            "userId" to uid
        )

        if (editingCourse == null) {
            db.collection("courses")
                .add(data)
                .addOnSuccessListener {
                    Toast.makeText(context, "Curso registrado", Toast.LENGTH_SHORT).show()
                    showDialog = false
                    loadCourses()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Error al registrar", Toast.LENGTH_SHORT).show()
                }
        } else {
            db.collection("courses")
                .document(editingCourse!!.id)
                .set(data)
                .addOnSuccessListener {
                    Toast.makeText(context, "Curso actualizado", Toast.LENGTH_SHORT).show()
                    showDialog = false
                    loadCourses()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Error al actualizar", Toast.LENGTH_SHORT).show()
                }
        }
    }

    fun deleteCourse(course: Course) {
        db.collection("courses")
            .document(course.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(context, "Curso eliminado", Toast.LENGTH_SHORT).show()
                loadCourses()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error al eliminar", Toast.LENGTH_SHORT).show()
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Cursos") },
                actions = {
                    TextButton(onClick = {
                        auth.signOut()
                        onLogout()
                    }) {
                        Text("Cerrar sesión")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { openCreateDialog() }) {
                Text("+")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text("Sesión iniciada como: $email")
            Spacer(Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (courses.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No tienes cursos registrados")
                }
            } else {
                LazyColumn {
                    items(courses) { course ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { openEditDialog(course) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(course.title, style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(4.dp))
                                    Text(course.description, style = MaterialTheme.typography.bodyMedium)
                                    Spacer(Modifier.height(4.dp))
                                    Text("Créditos: ${course.credits}", style = MaterialTheme.typography.bodySmall)
                                }
                                TextButton(onClick = { deleteCourse(course) }) {
                                    Text("Eliminar")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = { saveCourse() }) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancelar")
                }
            },
            title = {
                Text(if (editingCourse == null) "Nuevo curso" else "Editar curso")
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Nombre del curso") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descripción") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = creditsText,
                        onValueChange = { creditsText = it },
                        label = { Text("Créditos (número)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }
}
