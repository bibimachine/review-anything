package com.reviewanything.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.reviewanything.app.data.model.Note
import com.reviewanything.app.viewmodel.NotesViewModel
import com.reviewanything.app.viewmodel.UploadViewModel

@Composable
fun NotesScreen(
    viewModel: NotesViewModel,
    uploadViewModel: UploadViewModel
) {
    val allNotes by viewModel.allNotes.collectAsState()
    val sections by viewModel.sections.collectAsState()
    val selectedNote by viewModel.selectedNote.collectAsState()
    val context = LocalContext.current

    var expandedSections by remember { mutableStateOf<Set<String>>(emptySet()) }
    var newSectionName by remember { mutableStateOf("") }
    var showAddSection by remember { mutableStateOf(false) }

    // 上传单个文件的 launcher
    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val section = selectedNote?.section ?: expandedSections.firstOrNull() ?: return@let
            uploadViewModel.uploadSingleFile(context, it, section)
        }
    }

    // 上传状态
    val uploadState by uploadViewModel.state.collectAsState()
    LaunchedEffect(uploadState) {
        if (uploadState is UploadViewModel.UploadState.Success) {
            viewModel.loadAll()
            uploadViewModel.reset()
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        // 左侧边栏
        Surface(
            modifier = Modifier
                .width(260.dp)
                .fillMaxHeight(),
            tonalElevation = 1.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 标题 + 新建板块按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("笔记管理", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { showAddSection = !showAddSection }) {
                        Icon(Icons.Default.Add, contentDescription = "新建板块")
                    }
                }

                // 新建板块输入框
                AnimatedVisibility(visible = showAddSection) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newSectionName,
                            onValueChange = { newSectionName = it },
                            placeholder = { Text("板块名称") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Button(
                            onClick = {
                                if (viewModel.createSection(newSectionName)) {
                                    newSectionName = ""
                                    showAddSection = false
                                }
                            }
                        ) {
                            Text("创建", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                HorizontalDivider()

                if (sections.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "暂无笔记\n请创建板块或上传",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        sections.forEach { section ->
                            val notes = allNotes[section] ?: emptyList()
                            val isExpanded = expandedSections.contains(section)

                            item(key = "section_$section") {
                                Column {
                                    // 板块标题行
                                    Surface(
                                        onClick = {
                                            expandedSections = if (isExpanded) {
                                                expandedSections - section
                                            } else {
                                                expandedSections + section
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                section,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.weight(1f)
                                            )
                                            // + 号上传按钮
                                            if (isExpanded) {
                                                IconButton(
                                                    onClick = { fileLauncher.launch("*/*") },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Add,
                                                        contentDescription = "上传文件到 $section",
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                "${notes.size}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // 展开的笔记列表
                                    AnimatedVisibility(visible = isExpanded) {
                                        Column {
                                            notes.forEach { note ->
                                                val isSelected = selectedNote?.id == note.id
                                                Surface(
                                                    onClick = { viewModel.selectNote(note) },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    color = if (isSelected) {
                                                        MaterialTheme.colorScheme.primaryContainer
                                                    } else {
                                                        MaterialTheme.colorScheme.surface
                                                    }
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(start = 36.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            note.fileName,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            modifier = Modifier.weight(1f),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        IconButton(
                                                            onClick = { viewModel.deleteNote(note.id) },
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Delete,
                                                                contentDescription = "删除",
                                                                modifier = Modifier.size(16.dp),
                                                                tint = MaterialTheme.colorScheme.error
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    HorizontalDivider(modifier = Modifier.padding(start = 12.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        VerticalDivider()

        // 右侧预览区
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            tonalElevation = 0.dp
        ) {
            selectedNote?.let { note ->
                NotePreview(note = note)
            } ?: run {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "选择左侧笔记进行预览",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun NotePreview(note: Note) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 标题栏
        Text(
            note.fileName,
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            "板块: ${note.section}  ·  ${note.content.length} 字符",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        HorizontalDivider()

        Spacer(modifier = Modifier.height(12.dp))

        // 内容预览
        Text(
            note.content,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
