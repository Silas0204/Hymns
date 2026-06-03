package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.AppDatabase
import com.example.data.repository.SongRepository
import com.example.ui.screens.AddEditSongScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SearchImportScreen
import com.example.ui.screens.SignInScreen
import com.example.ui.screens.SongViewerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.SongViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Room Local Storage
        val database = AppDatabase.getDatabase(this)
        val songDao = database.songDao()
        val songRepository = SongRepository(songDao)

        // Initialize Shared Viewmodel with Application Context
        val viewModel: SongViewModel by viewModels {
            SongViewModel.Factory(songRepository, this)
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    AppNavigation(viewModel)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(viewModel: SongViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "signin"
    ) {
        // Sign In / Splash Route
        composable("signin") {
            SignInScreen(
                viewModel = viewModel,
                onNavigateToLibrary = {
                    navController.navigate("library") {
                        popUpTo("signin") { inclusive = true }
                    }
                }
            )
        }

        // Library Screen Route (Shows user's catalog)
        composable("library") {
            LibraryScreen(
                viewModel = viewModel,
                onNavigateToView = { songId ->
                    navController.navigate("view_song/$songId")
                },
                onNavigateToAdd = {
                    navController.navigate("add_song")
                },
                onNavigateToSearch = {
                    navController.navigate("search_import")
                },
                onNavigateToSignIn = {
                    navController.navigate("signin")
                }
            )
        }

        // Manual Add Song Route
        composable("add_song") {
            AddEditSongScreen(
                viewModel = viewModel,
                songId = null,
                onBack = { navController.popBackStack() }
            )
        }

        // Manual Edit Song Route
        composable(
            route = "edit_song/{songId}",
            arguments = listOf(navArgument("songId") { type = NavType.IntType })
        ) { backStackEntry ->
            val songId = backStackEntry.arguments?.getInt("songId")
            AddEditSongScreen(
                viewModel = viewModel,
                songId = songId,
                onBack = { navController.popBackStack() }
            )
        }

        // Song Viewer Screen Route (Stage sheet with pitch shifts & font zooms)
        composable(
            route = "view_song/{songId}",
            arguments = listOf(navArgument("songId") { type = NavType.IntType })
        ) { backStackEntry ->
            val songId = backStackEntry.arguments?.getInt("songId") ?: 0
            SongViewerScreen(
                viewModel = viewModel,
                songId = songId,
                onBack = { navController.popBackStack() },
                onEditSong = { id ->
                    navController.navigate("edit_song/$id")
                }
            )
        }

        // Smart Search and Extraction screen route (with URL crawler)
        composable("search_import") {
            SearchImportScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
