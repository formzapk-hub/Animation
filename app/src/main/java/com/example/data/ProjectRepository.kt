package com.example.data

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TeamMessage(
    val id: String = "",
    val senderName: String = "",
    val senderEmail: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class TeamNotification(
    val id: String = "",
    val actorName: String = "",
    val actionText: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

class ProjectRepository(context: Context) {
    private val db: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "kinetic_text_studio.db"
    ).fallbackToDestructiveMigration().build()

    private val projectDao = db.projectDao()

    // Firebase state flags
    private var isFirebaseAvailable = false
    private var firestore: FirebaseFirestore? = null
    private var firebaseAuth: FirebaseAuth? = null

    // Fallback/Simulated database for when Firebase is not connected or fails
    private val _simulatedMessages = MutableStateFlow<Map<String, List<TeamMessage>>>(emptyMap())
    private val _simulatedNotifications = MutableStateFlow<Map<String, List<TeamNotification>>>(emptyMap())

    init {
        try {
            // Check if Firebase is initialized
            val app = FirebaseApp.getInstance()
            if (app != null) {
                firestore = FirebaseFirestore.getInstance()
                firebaseAuth = FirebaseAuth.getInstance()
                isFirebaseAvailable = true
                Log.d("ProjectRepository", "Firebase is successfully initialized and available.")
            }
        } catch (e: Exception) {
            Log.w("ProjectRepository", "Firebase not initialized. Running in simulated cloud mode.", e)
            isFirebaseAvailable = false
        }
    }

    fun isCloudConfigured(): Boolean = isFirebaseAvailable

    // --- Local Room DB Functions ---
    fun getAllLocalProjects(): Flow<List<AnimationProject>> = projectDao.getAllProjects()

    suspend fun getProjectById(id: Long): AnimationProject? = withContext(Dispatchers.IO) {
        projectDao.getProjectById(id)
    }

    suspend fun saveProject(project: AnimationProject): Long = withContext(Dispatchers.IO) {
        val updatedProject = project.copy(lastUpdated = System.currentTimeMillis())
        val newId = projectDao.insertProject(updatedProject)
        
        // Sync to cloud if enabled
        if (updatedProject.isCloudSynced) {
            val projectIdToSync = if (updatedProject.id == 0L) newId else updatedProject.id
            syncProjectToCloud(updatedProject.copy(id = projectIdToSync))
        }
        newId
    }

    suspend fun deleteProject(id: Long) = withContext(Dispatchers.IO) {
        projectDao.deleteProjectById(id)
    }

    // --- Cloud Syncing Functions ---
    private fun syncProjectToCloud(project: AnimationProject) {
        if (!isFirebaseAvailable) {
            triggerSimulatedNotification(
                project.cloudProjectId ?: "project_${project.id}",
                "Sistem",
                "Progres proyek '${project.name}' disimpan otomatis (Mode Simulasi)"
            )
            return
        }

        val cloudId = project.cloudProjectId ?: "project_${project.id}"
        val data = hashMapOf(
            "id" to project.id,
            "name" to project.name,
            "text" to project.text,
            "animType" to project.animType,
            "speed" to project.speed,
            "textColor" to project.textColor,
            "backgroundColor" to project.backgroundColor,
            "fontSize" to project.fontSize,
            "resolutionWidth" to project.resolutionWidth,
            "resolutionHeight" to project.resolutionHeight,
            "fontName" to project.fontName,
            "isShadowEnabled" to project.isShadowEnabled,
            "shadowColor" to project.shadowColor,
            "shadowRadius" to project.shadowRadius,
            "shadowDx" to project.shadowDx,
            "shadowDy" to project.shadowDy,
            "isOutlineEnabled" to project.isOutlineEnabled,
            "outlineColor" to project.outlineColor,
            "outlineWidth" to project.outlineWidth,
            "lastUpdated" to project.lastUpdated
        )

        firestore?.collection("projects")?.document(cloudId)?.set(data)
            ?.addOnSuccessListener {
                Log.d("ProjectRepository", "Successfully synced project $cloudId to Firestore")
                sendCloudNotification(cloudId, "Anggota", "mengupdate setelan proyek '${project.name}'")
            }
            ?.addOnFailureListener { e ->
                Log.e("ProjectRepository", "Failed to sync project to Firestore", e)
            }
    }

    // --- Real-time Collaborative Project Sync ---
    fun observeCloudProject(cloudId: String): Flow<AnimationProject?> = callbackFlow {
        if (!isFirebaseAvailable) {
            close()
            return@callbackFlow
        }

        val listener = firestore?.collection("projects")?.document(cloudId)
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val project = AnimationProject(
                        id = snapshot.getLong("id") ?: 0L,
                        name = snapshot.getString("name") ?: "",
                        text = snapshot.getString("text") ?: "",
                        animType = snapshot.getString("animType") ?: "Teks Melingkar",
                        speed = snapshot.getDouble("speed")?.toFloat() ?: 1.0f,
                        textColor = snapshot.getString("textColor") ?: "#00E5FF",
                        backgroundColor = snapshot.getString("backgroundColor") ?: "#121212",
                        fontSize = snapshot.getDouble("fontSize")?.toFloat() ?: 48f,
                        resolutionWidth = snapshot.getLong("resolutionWidth")?.toInt() ?: 720,
                        resolutionHeight = snapshot.getLong("resolutionHeight")?.toInt() ?: 1280,
                        isCloudSynced = true,
                        cloudProjectId = cloudId,
                        fontName = snapshot.getString("fontName") ?: "Montserrat",
                        isShadowEnabled = snapshot.getBoolean("isShadowEnabled") ?: false,
                        shadowColor = snapshot.getString("shadowColor") ?: "#000000",
                        shadowRadius = snapshot.getDouble("shadowRadius")?.toFloat() ?: 8.0f,
                        shadowDx = snapshot.getDouble("shadowDx")?.toFloat() ?: 4.0f,
                        shadowDy = snapshot.getDouble("shadowDy")?.toFloat() ?: 4.0f,
                        isOutlineEnabled = snapshot.getBoolean("isOutlineEnabled") ?: false,
                        outlineColor = snapshot.getString("outlineColor") ?: "#000000",
                        outlineWidth = snapshot.getDouble("outlineWidth")?.toFloat() ?: 4.0f,
                        lastUpdated = snapshot.getLong("lastUpdated") ?: System.currentTimeMillis()
                    )
                    trySend(project)
                } else {
                    trySend(null)
                }
            }

        awaitClose { listener?.remove() }
    }

    // --- Real-time Discussion Board Functions ---
    fun observeDiscussionMessages(cloudId: String): Flow<List<TeamMessage>> = callbackFlow {
        if (!isFirebaseAvailable) {
            // Emulate local flow for simulated discussions
            val collector = _simulatedMessages.collect { map ->
                trySend(map[cloudId] ?: emptyList())
            }
            awaitClose { }
            return@callbackFlow
        }

        val listener = firestore?.collection("projects")?.document(cloudId)
            ?.collection("discussions")
            ?.orderBy("timestamp", Query.Direction.ASCENDING)
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ProjectRepository", "Error observing messages", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        TeamMessage(
                            id = doc.id,
                            senderName = doc.getString("senderName") ?: "Anonim",
                            senderEmail = doc.getString("senderEmail") ?: "",
                            content = doc.getString("content") ?: "",
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        )
                    }
                    trySend(messages)
                }
            }

        awaitClose { listener?.remove() }
    }

    fun sendMessage(cloudId: String, senderName: String, senderEmail: String, content: String) {
        if (!isFirebaseAvailable) {
            val currentList = _simulatedMessages.value[cloudId] ?: emptyList()
            val newMessage = TeamMessage(
                id = "msg_${System.currentTimeMillis()}",
                senderName = senderName,
                senderEmail = senderEmail,
                content = content,
                timestamp = System.currentTimeMillis()
            )
            val updatedMap = _simulatedMessages.value.toMutableMap()
            updatedMap[cloudId] = currentList + newMessage
            _simulatedMessages.value = updatedMap

            // Post action notification
            triggerSimulatedNotification(cloudId, senderName, "mengirim pesan: \"$content\"")
            return
        }

        val data = hashMapOf(
            "senderName" to senderName,
            "senderEmail" to senderEmail,
            "content" to content,
            "timestamp" to System.currentTimeMillis()
        )

        firestore?.collection("projects")?.document(cloudId)
            ?.collection("discussions")?.add(data)
            ?.addOnSuccessListener {
                sendCloudNotification(cloudId, senderName, "mengirim pesan di papan diskusi")
            }
    }

    // --- Real-time Task/Progress Notifications ---
    fun observeNotifications(cloudId: String): Flow<List<TeamNotification>> = callbackFlow {
        if (!isFirebaseAvailable) {
            val collector = _simulatedNotifications.collect { map ->
                trySend(map[cloudId] ?: listOf(
                    TeamNotification(
                        id = "notif_init",
                        actorName = "Sistem",
                        actionText = "Proyek kolaboratif dimulai dalam Mode Offline/Simulasi.",
                        timestamp = System.currentTimeMillis()
                    )
                ))
            }
            awaitClose { }
            return@callbackFlow
        }

        val listener = firestore?.collection("projects")?.document(cloudId)
            ?.collection("notifications")
            ?.orderBy("timestamp", Query.Direction.DESCENDING)
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ProjectRepository", "Error observing notifications", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val notifications = snapshot.documents.mapNotNull { doc ->
                        TeamNotification(
                            id = doc.id,
                            actorName = doc.getString("actorName") ?: "Anonim",
                            actionText = doc.getString("actionText") ?: "",
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        )
                    }
                    trySend(notifications)
                }
            }

        awaitClose { listener?.remove() }
    }

    private fun sendCloudNotification(cloudId: String, actorName: String, actionText: String) {
        val data = hashMapOf(
            "actorName" to actorName,
            "actionText" to actionText,
            "timestamp" to System.currentTimeMillis()
        )
        firestore?.collection("projects")?.document(cloudId)
            ?.collection("notifications")?.add(data)
    }

    private fun triggerSimulatedNotification(cloudId: String, actorName: String, actionText: String) {
        val currentList = _simulatedNotifications.value[cloudId] ?: emptyList()
        val newNotif = TeamNotification(
            id = "notif_${System.currentTimeMillis()}",
            actorName = actorName,
            actionText = actionText,
            timestamp = System.currentTimeMillis()
        )
        val updatedMap = _simulatedNotifications.value.toMutableMap()
        updatedMap[cloudId] = listOf(newNotif) + currentList
        _simulatedNotifications.value = updatedMap
    }
}
