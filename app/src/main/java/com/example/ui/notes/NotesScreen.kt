package com.example.ui.notes

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.Note
import com.example.ui.MainViewModel
import com.example.ui.tilt3D
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val notes by viewModel.allNotes.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val playingNoteId by viewModel.playingNoteId.collectAsState()
    val recordedPath by viewModel.recordedPath.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var noteTypeToAdd by remember { mutableStateOf("text") } // "text", "voice", "image"
    
    var editNote by remember { mutableStateOf<Note?>(null) }
    
    val context = LocalContext.current
    
    // Media result configurations
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var capturedImageFile by remember { mutableStateOf<File?>(null) }
    
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedImageUri = uri
    }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (!success) {
            cameraImageUri = null
            capturedImageFile = null
        }
    }

    val storagePermissionsToRequest = remember {
        if (Build.VERSION.SDK_INT >= 33) {
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            listOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    var pendingStorageAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) {
            pendingStorageAction?.invoke()
        } else {
            Toast.makeText(context, "Storage permissions are required to access and save media.", Toast.LENGTH_SHORT).show()
        }
        pendingStorageAction = null
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            try {
                val file = File(context.filesDir, "note_img_${System.currentTimeMillis()}.jpg")
                capturedImageFile = file
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                cameraImageUri = uri
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error setting up camera output: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Camera permission is required to snapshot notes.", Toast.LENGTH_SHORT).show()
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startVoiceRecording()
        } else {
            Toast.makeText(context, "Microphone permission is required to record voice notes.", Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Notes Hub",
                        color = Color(0xFF001D35),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Take voice, text or image notes",
                        color = Color(0xFF44474E),
                        fontSize = 14.sp
                    )
                }
                
                // Add Quick buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = {
                            noteTypeToAdd = "text"
                            showAddDialog = true
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFFD3E4FF)),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Filled.EditNote, contentDescription = "Add Text note", tint = Color(0xFF0061A4))
                    }
                    IconButton(
                        onClick = {
                            noteTypeToAdd = "voice"
                            showAddDialog = true
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFFFFD8E4)),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Filled.Mic, contentDescription = "Add Voice note", tint = Color(0xFFBA1A1A))
                    }
                    IconButton(
                        onClick = {
                            noteTypeToAdd = "image"
                            showAddDialog = true
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFFC2EFD3)),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Filled.Image, contentDescription = "Add Image note", tint = Color(0xFF0A6B3A))
                    }
                }
            }

            if (notes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FolderOpen,
                            contentDescription = "No Notes",
                            tint = Color(0xFF0061A4),
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No notes saved yet",
                            color = Color(0xFF001D35),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap any quick action icon above to write notes, record voice memos, or save snapshots locally.",
                            color = Color(0xFF44474E),
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(notes) { note ->
                        NoteCard(
                            note = note,
                            isPlaying = playingNoteId == note.id,
                            onPlayClick = { viewModel.playVoiceNote(note) },
                            onDeleteClick = { viewModel.deleteNote(note) },
                            onEditClick = { editNote = note }
                        )
                    }
                }
            }
        }

        // Add Note Dialog
        if (showAddDialog) {
            var noteTitle by remember { mutableStateOf("") }
            var noteContent by remember { mutableStateOf("") }
            
            Dialog(onDismissRequest = {
                showAddDialog = false
                selectedImageUri = null
                cameraImageUri = null
                capturedImageFile = null
                viewModel.stopVoiceRecording()
            }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when (noteTypeToAdd) {
                                "voice" -> "Create Voice Note"
                                "image" -> "Create Image Note"
                                else -> "New Note"
                            },
                            color = Color(0xFF001D35),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        OutlinedTextField(
                            value = noteTitle,
                            onValueChange = { noteTitle = it },
                            label = { Text("Title") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF1A1C1E),
                                unfocusedTextColor = Color(0xFF1A1C1E),
                                focusedBorderColor = Color(0xFF0061A4),
                                unfocusedBorderColor = Color(0xFFCAC4D0).copy(alpha = 0.8f),
                                focusedLabelColor = Color(0xFF0061A4),
                                unfocusedLabelColor = Color(0xFF44474E)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("note_title_input")
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (noteTypeToAdd == "text" || noteTypeToAdd == "image") {
                            OutlinedTextField(
                                value = noteContent,
                                onValueChange = { noteContent = it },
                                label = { Text("Note Content") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF1A1C1E),
                                    unfocusedTextColor = Color(0xFF1A1C1E),
                                    focusedBorderColor = Color(0xFF0061A4),
                                    unfocusedBorderColor = Color(0xFFCAC4D0).copy(alpha = 0.8f),
                                    focusedLabelColor = Color(0xFF0061A4),
                                    unfocusedLabelColor = Color(0xFF44474E)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .testTag("note_content_input")
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Special configurations per Note Type
                        if (noteTypeToAdd == "voice") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFE7E0FF).copy(alpha = 0.5f))
                                    .border(androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0061A4).copy(alpha = 0.15f)), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    if (isRecording) {
                                        Text("Recording System Audio...", color = Color(0xFFBA1A1A), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = { viewModel.stopVoiceRecording() },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A)),
                                            shape = RoundedCornerShape(24.dp)
                                        ) {
                                            Icon(Icons.Filled.Stop, contentDescription = "Stop", tint = Color.White)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Stop Recording")
                                        }
                                    } else {
                                        if (recordedPath != null) {
                                            Text("Recording captured successfully!", color = Color(0xFF0A6B3A), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        } else {
                                            Text("Tap Mic button to begin voice capture", color = Color(0xFF44474E), fontSize = 13.sp)
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        IconButton(
                                            onClick = {
                                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                                    viewModel.startVoiceRecording()
                                                } else {
                                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                }
                                            },
                                            colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFFBA1A1A)),
                                            modifier = Modifier.size(54.dp)
                                        ) {
                                            Icon(Icons.Filled.Mic, contentDescription = "Record", tint = Color.White, modifier = Modifier.size(28.dp))
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        } else if (noteTypeToAdd == "image") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFE8F5E9))
                                    .border(androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0A6B3A).copy(alpha = 0.15f)), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    cameraImageUri != null -> {
                                        AsyncImage(
                                            model = cameraImageUri,
                                            contentDescription = "Captured",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    selectedImageUri != null -> {
                                        AsyncImage(
                                            model = selectedImageUri,
                                            contentDescription = "Selected",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    else -> {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                                        try {
                                                            val file = File(context.filesDir, "note_img_${System.currentTimeMillis()}.jpg")
                                                            capturedImageFile = file
                                                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                                            cameraImageUri = uri
                                                            cameraLauncher.launch(uri)
                                                        } catch (e: Exception) {
                                                            e.printStackTrace()
                                                            Toast.makeText(context, "Error setting up camera: ${e.message}", Toast.LENGTH_SHORT).show()
                                                        }
                                                    } else {
                                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4)),
                                                shape = RoundedCornerShape(24.dp)
                                            ) {
                                                Icon(Icons.Filled.PhotoCamera, contentDescription = "Camera", tint = Color.White)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Camera", color = Color.White)
                                            }
                                            Button(
                                                onClick = {
                                                    galleryLauncher.launch("image/*")
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4)),
                                                shape = RoundedCornerShape(24.dp)
                                            ) {
                                                Icon(Icons.Filled.PhotoLibrary, contentDescription = "Gallery", tint = Color.White)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Gallery", color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    showAddDialog = false
                                    selectedImageUri = null
                                    cameraImageUri = null
                                    capturedImageFile = null
                                    viewModel.stopVoiceRecording()
                                },
                                shape = RoundedCornerShape(24.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCAC4D0)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel", color = Color(0xFF0061A4))
                            }
                            Button(
                                onClick = {
                                    var imageSavePath: String? = null
                                    if (noteTypeToAdd == "image") {
                                        if (capturedImageFile != null && capturedImageFile!!.exists()) {
                                            imageSavePath = capturedImageFile!!.absolutePath
                                        } else if (selectedImageUri != null) {
                                            imageSavePath = selectedImageUri.toString()
                                        }
                                    }
                                    viewModel.saveNote(
                                        title = noteTitle,
                                        content = noteContent,
                                        type = noteTypeToAdd,
                                        imageUri = imageSavePath
                                    )
                                    showAddDialog = false
                                    selectedImageUri = null
                                    cameraImageUri = null
                                    capturedImageFile = null
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when (noteTypeToAdd) {
                                        "voice" -> Color(0xFFBA1A1A)
                                        "image" -> Color(0xFF0A6B3A)
                                        else -> Color(0xFF0061A4)
                                    }
                                ),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.weight(1f),
                                enabled = (noteTypeToAdd != "voice" || recordedPath != null)
                            ) {
                                Text("Save Memo")
                            }
                        }
                    }
                }
            }
        }

        // Edit Note Dialog
        editNote?.let { n ->
            var editTitle by remember { mutableStateOf(n.title) }
            var editContent by remember { mutableStateOf(n.content) }
            
            Dialog(onDismissRequest = { editNote = null }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCAC4D0).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Edit Note",
                            color = Color(0xFF001D35),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        OutlinedTextField(
                            value = editTitle,
                            onValueChange = { editTitle = it },
                            label = { Text("Title") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF1A1C1E),
                                unfocusedTextColor = Color(0xFF1A1C1E),
                                focusedBorderColor = Color(0xFF0061A4),
                                unfocusedBorderColor = Color(0xFFCAC4D0).copy(alpha = 0.8f),
                                focusedLabelColor = Color(0xFF0061A4),
                                unfocusedLabelColor = Color(0xFF44474E)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (n.type == "text" || n.type == "image") {
                            OutlinedTextField(
                                value = editContent,
                                onValueChange = { editContent = it },
                                label = { Text("Note Content") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF1A1C1E),
                                    unfocusedTextColor = Color(0xFF1A1C1E),
                                    focusedBorderColor = Color(0xFF0061A4),
                                    unfocusedBorderColor = Color(0xFFCAC4D0).copy(alpha = 0.8f),
                                    focusedLabelColor = Color(0xFF0061A4),
                                    unfocusedLabelColor = Color(0xFF44474E)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { editNote = null },
                                shape = RoundedCornerShape(24.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCAC4D0)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel", color = Color(0xFF0061A4))
                            }
                            Button(
                                onClick = {
                                    viewModel.updateNote(n.copy(title = editTitle, content = editContent))
                                    editNote = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4)),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Update")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoteCard(
    note: Note,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val displayDate = remember(note.timestamp) {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        sdf.format(Date(note.timestamp))
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.2.dp,
            when (note.type) {
                "voice" -> Color(0xFFBA1A1A).copy(alpha = 0.2f)
                "image" -> Color(0xFF0A6B3A).copy(alpha = 0.2f)
                else -> Color(0xFFCAC4D0).copy(alpha = 0.5f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .tilt3D(maxRotationX = 10f, maxRotationY = 10f)
            .clickable { onEditClick() }
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = when (note.type) {
                        "voice" -> listOf(Color(0xFFE7E0FF), Color(0xFFF1EDFF))
                        "image" -> listOf(Color(0xFFE8F5E9), Color(0xFFD6F5DE))
                        else -> listOf(Color(0xFFFFFFFF), Color(0xFFF8F9FF))
                    }
                ),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (note.type) {
                            "voice" -> Icons.Filled.Mic
                            "image" -> Icons.Filled.Image
                            else -> Icons.Filled.EditNote
                        },
                        contentDescription = null,
                        tint = when (note.type) {
                            "voice" -> Color(0xFFBA1A1A)
                            "image" -> Color(0xFF0A6B3A)
                            else -> Color(0xFF0061A4)
                        },
                        modifier = Modifier
                            .size(24.dp)
                            .padding(end = 6.dp)
                    )
                    Text(
                        text = note.title,
                        color = Color(0xFF001D35),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 200.dp)
                    )
                }
                
                Row {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit Note", tint = Color(0xFF44474E), modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete Note", tint = Color(0xFFBA1A1A), modifier = Modifier.size(18.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            // Body rendering per note style
            if (note.type == "voice" && note.voicePath != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFD8E4).copy(alpha = 0.6f))
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onPlayClick,
                                colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFFBA1A1A))
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = "Play voice audio",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isPlaying) "Playing Audio..." else "Voice Recording Memo",
                                color = Color(0xFF1A1C1E),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else if (note.type == "image" && note.imageUri != null) {
                val isLocalPath = note.imageUri.startsWith("/")
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE8F5E9))
                ) {
                    AsyncImage(
                        model = if (isLocalPath) java.io.File(note.imageUri) else note.imageUri,
                        contentDescription = "Note snapshot",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                if (note.content.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = note.content,
                        color = Color(0xFF1A1C1E),
                        fontSize = 14.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Text(
                    text = note.content,
                    color = Color(0xFF1A1C1E),
                    fontSize = 14.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = displayDate,
                    color = Color(0xFF44474E).copy(alpha = 0.8f),
                    fontSize = 11.sp
                )
            }
        }
    }
}
