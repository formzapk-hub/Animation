package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AnimationProject
import com.example.data.ProjectRepository
import com.example.data.TeamMessage
import com.example.data.TeamNotification
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class ProjectViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProjectRepository(application)
    private val videoRenderer = VideoRenderer(application)

    // User Profile for Collaboration
    val currentUserEmail = MutableStateFlow("formzapk@gmail.com")
    val currentUserName = MutableStateFlow("Creative Developer")

    // Local project list
    val localProjects: StateFlow<List<AnimationProject>> = repository.getAllLocalProjects()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Active project state
    private val _activeProject = MutableStateFlow<AnimationProject?>(null)
    val activeProject: StateFlow<AnimationProject?> = _activeProject.asStateFlow()

    // Undo / Redo Stacks and States
    private val undoStack = mutableListOf<AnimationProject>()
    private val redoStack = mutableListOf<AnimationProject>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private var lastUndoSaveTime: Long = 0

    // Render engine state
    val renderState: StateFlow<RenderState> = videoRenderer.renderState

    // Live sync and collaboration states
    private val _discussionMessages = MutableStateFlow<List<TeamMessage>>(emptyMap<String, List<TeamMessage>>()[""] ?: emptyList())
    val discussionMessages: StateFlow<List<TeamMessage>> = _discussionMessages.asStateFlow()

    private val _teamNotifications = MutableStateFlow<List<TeamNotification>>(emptyList())
    val teamNotifications: StateFlow<List<TeamNotification>> = _teamNotifications.asStateFlow()

    private var activeProjectJob: Job? = null
    private var discussionJob: Job? = null
    private var notificationsJob: Job? = null
    private var autoSaveJob: Job? = null

    init {
        // Create an initial default project if list is empty, or select the first one
        viewModelScope.launch {
            localProjects.collectLatest { list ->
                if (list.isEmpty() && _activeProject.value == null) {
                    createNewProject("Proyek Teks Saya")
                } else if (_activeProject.value == null && list.isNotEmpty()) {
                    selectProject(list.first())
                }
            }
        }
    }

    fun deleteProject(id: Long) {
        viewModelScope.launch {
            repository.deleteProject(id)
            if (_activeProject.value?.id == id) {
                _activeProject.value = localProjects.value.firstOrNull { it.id != id }
            }
        }
    }

    fun isCloudConfigured(): Boolean = repository.isCloudConfigured()

    // --- Create / Load / Update Project ---
    fun createNewProject(name: String) {
        viewModelScope.launch {
            val newProject = AnimationProject(name = name)
            val newId = repository.saveProject(newProject)
            val savedProject = newProject.copy(id = newId)
            selectProject(savedProject)
        }
    }

    fun selectProject(project: AnimationProject) {
        _activeProject.value = project
        observeProjectCollaboration(project)
    }

    fun updateActiveProject(updateBlock: (AnimationProject) -> AnimationProject) {
        val current = _activeProject.value ?: return
        val updated = updateBlock(current)
        
        // Push current state to undo stack before applying change (with smart debounce)
        saveToUndoStack(current, updated)
        
        _activeProject.value = updated

        // Auto-save: debounce to avoid writing to DB too frequently on slider drags
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(500) // 500ms debounce
            repository.saveProject(updated)
        }
    }

    private fun saveToUndoStack(current: AnimationProject, updated: AnimationProject) {
        if (current == updated) return
        
        // Check if only text changed
        val onlyTextChanged = current.copy(text = updated.text) == updated
        
        var shouldPush = false
        if (onlyTextChanged) {
            val now = System.currentTimeMillis()
            val textLengthDiff = Math.abs(current.text.length - updated.text.length)
            val endsWithSpace = updated.text.endsWith(" ") || updated.text.endsWith("\n")
            
            if (now - lastUndoSaveTime > 1500 || textLengthDiff >= 5 || endsWithSpace || current.text.isEmpty()) {
                shouldPush = true
                lastUndoSaveTime = now
            }
        } else {
            // Any color, speed, font, or style change is pushed immediately
            shouldPush = true
        }
        
        if (shouldPush) {
            // Avoid duplicates at the top of the stack
            if (undoStack.isEmpty() || undoStack.last() != current) {
                undoStack.add(current)
                if (undoStack.size > 50) {
                    undoStack.removeAt(0) // Cap stack at 50 to protect memory
                }
                _canUndo.value = true
                
                // Clear redo stack on new user action
                redoStack.clear()
                _canRedo.value = false
            }
        }
    }

    fun undo() {
        val current = _activeProject.value ?: return
        if (undoStack.isNotEmpty()) {
            val previousState = undoStack.removeAt(undoStack.size - 1)
            _canUndo.value = undoStack.isNotEmpty()
            
            // Push current state to redo stack
            redoStack.add(current)
            _canRedo.value = true
            
            _activeProject.value = previousState
            
            // Save to DB
            viewModelScope.launch {
                repository.saveProject(previousState)
            }
        }
    }

    fun redo() {
        val current = _activeProject.value ?: return
        if (redoStack.isNotEmpty()) {
            val nextState = redoStack.removeAt(redoStack.size - 1)
            _canRedo.value = redoStack.isNotEmpty()
            
            // Push current state back to undo stack
            undoStack.add(current)
            _canUndo.value = true
            
            _activeProject.value = nextState
            
            // Save to DB
            viewModelScope.launch {
                repository.saveProject(nextState)
            }
        }
    }

    // --- Collaboration & Real-time Listeners ---
    private fun observeProjectCollaboration(project: AnimationProject) {
        activeProjectJob?.cancel()
        discussionJob?.cancel()
        notificationsJob?.cancel()

        val cloudId = project.cloudProjectId ?: "project_${project.id}"

        // Observe collaborative sync from firestore if synced
        if (project.isCloudSynced) {
            activeProjectJob = viewModelScope.launch {
                repository.observeCloudProject(cloudId).collectLatest { cloudProj ->
                    if (cloudProj != null && cloudProj.lastUpdated > (_activeProject.value?.lastUpdated ?: 0L)) {
                        _activeProject.value = cloudProj
                    }
                }
            }
        }

        // Observe discussions and notifications regardless (it falls back to simulated offline state if Firebase unavailable)
        discussionJob = viewModelScope.launch {
            repository.observeDiscussionMessages(cloudId).collectLatest { msgs ->
                _discussionMessages.value = msgs
            }
        }

        notificationsJob = viewModelScope.launch {
            repository.observeNotifications(cloudId).collectLatest { notifs ->
                _teamNotifications.value = notifs
            }
        }
    }

    fun enableCloudCollaboration(cloudId: String, teamName: String) {
        val current = _activeProject.value ?: return
        val updated = current.copy(
            isCloudSynced = true,
            cloudProjectId = cloudId,
            name = if (current.name.startsWith("Proyek Teks")) "Kolaborasi - $teamName" else current.name
        )
        updateActiveProject { updated }
        selectProject(updated)
    }

    fun postDiscussionMessage(content: String) {
        val proj = _activeProject.value ?: return
        val cloudId = proj.cloudProjectId ?: "project_${proj.id}"
        repository.sendMessage(cloudId, currentUserName.value, currentUserEmail.value, content)
    }

    // --- Render Actions ---
    fun startVideoExport(
        width: Int,
        height: Int,
        customFileName: String? = null,
        isWatermarkMode: Boolean = false,
        bgVideoPath: String? = null
    ) {
        val proj = _activeProject.value ?: return
        val fName = customFileName ?: "kinetic_${proj.animType.replace(" ", "_")}_${width}x${height}_${System.currentTimeMillis()}.mp4"
        viewModelScope.launch {
            videoRenderer.renderVideo(
                text = proj.text,
                animType = proj.animType,
                speedMult = proj.speed,
                textColorHex = proj.textColor,
                bgColorHex = proj.backgroundColor,
                baseFontSize = proj.fontSize,
                width = width,
                height = height,
                fileName = fName,
                isWatermarkMode = isWatermarkMode,
                bgVideoPath = bgVideoPath,
                fontName = proj.fontName,
                isShadowEnabled = proj.isShadowEnabled,
                shadowColorHex = proj.shadowColor,
                shadowRadius = proj.shadowRadius,
                shadowDx = proj.shadowDx,
                shadowDy = proj.shadowDy,
                isOutlineEnabled = proj.isOutlineEnabled,
                outlineColorHex = proj.outlineColor,
                outlineWidth = proj.outlineWidth
            )
        }
    }

    fun resetRenderState() {
        videoRenderer.resetState()
    }
}
