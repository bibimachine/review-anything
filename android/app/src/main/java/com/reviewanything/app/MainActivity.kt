package com.reviewanything.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.reviewanything.app.data.repository.ReviewRepository
import com.reviewanything.app.ui.screens.ReviewScreen
import com.reviewanything.app.ui.screens.UploadScreen
import com.reviewanything.app.ui.screens.SettingsScreen
import com.reviewanything.app.ui.screens.NotesScreen
import com.reviewanything.app.ui.theme.ReviewAnythingTheme
import com.reviewanything.app.viewmodel.ReviewViewModel
import com.reviewanything.app.viewmodel.UploadViewModel
import com.reviewanything.app.viewmodel.SettingsViewModel
import com.reviewanything.app.viewmodel.NotesViewModel
import com.reviewanything.app.viewmodel.ReviewViewModelFactory
import com.reviewanything.app.viewmodel.UploadViewModelFactory
import com.reviewanything.app.viewmodel.SettingsViewModelFactory
import com.reviewanything.app.viewmodel.NotesViewModelFactory
import com.reviewanything.app.viewmodel.CheckInViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ReviewAnythingTheme {
                MainApp()
            }
        }
    }
}

sealed class Screen(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Review : Screen("review", "复习", Icons.Default.Home)
    object Upload : Screen("upload", "上传", Icons.Default.Add)
    object Notes : Screen("notes", "笔记", Icons.Default.Menu)
    object Settings : Screen("settings", "设置", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    val navController = rememberNavController()
    val items = listOf(Screen.Review, Screen.Upload, Screen.Notes, Screen.Settings)
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as ReviewAnythingApp
    val db = app.database

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Review.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Review.route) {
                val viewModel: ReviewViewModel = viewModel(
                    factory = ReviewViewModelFactory(ReviewRepository(db.reviewItemDao()), db)
                )
                ReviewScreen(viewModel)
            }
            composable(Screen.Upload.route) {
                val viewModel: UploadViewModel = viewModel(
                    factory = UploadViewModelFactory(db)
                )
                UploadScreen(viewModel)
            }
            composable(Screen.Notes.route) {
                val notesVm: NotesViewModel = viewModel(factory = NotesViewModelFactory(db))
                val uploadVm: UploadViewModel = viewModel(factory = UploadViewModelFactory(db))
                NotesScreen(notesVm, uploadVm)
            }
            composable(Screen.Settings.route) {
                val viewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModelFactory(db)
                )
                SettingsScreen(viewModel)
            }
        }
    }
}
