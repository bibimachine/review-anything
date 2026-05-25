package com.reviewanything.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reviewanything.app.data.model.Note
import com.reviewanything.app.viewmodel.NotesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(viewModel: NotesViewModel) {
    val sections by viewModel.sections.collectAsState()
    val notes by viewModel.notes.collectAsState()
    var selectedSection by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // 板块选择
        Text("笔记管理", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp))

        if (sections.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("暂无笔记", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return
        }

        // 板块标签
        ScrollableTabRow(
            selectedTabIndex = sections.indexOf(selectedSection).coerceAtLeast(0),
            modifier = Modifier.fillMaxWidth()
        ) {
            sections.forEach { section ->
                Tab(
                    selected = selectedSection == section,
                    onClick = {
                        selectedSection = section
                        viewModel.selectSection(section)
                    },
                    text = { Text(section) }
                )
            }
        }

        // 笔记列表
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(notes) { note ->
                NoteItemCard(note = note, onDelete = { viewModel.deleteNote(note.id) })
            }
        }
    }
}

@Composable
fun NoteItemCard(note: Note, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(note.fileName, style = MaterialTheme.typography.titleMedium)
                Text(
                    note.content.take(100) + if (note.content.length > 100) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "删除")
            }
        }
    }
}
