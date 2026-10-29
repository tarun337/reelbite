package com.reelbite.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.reelbite.app.ui.group.GroupScreen
import com.reelbite.app.ui.group.GroupViewModel
import com.reelbite.app.ui.home.HomeScreen
import com.reelbite.app.ui.theme.ReelBiteTheme

class MainActivity : ComponentActivity() {

    /** Text shared into the app (e.g. Instagram → Share → ReelBite). */
    private var sharedText by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) sharedText = intent.extractSharedText()

        val repository = (application as ReelBiteApp).container.repository

        setContent {
            ReelBiteTheme {
                val nav = rememberNavController()
                val snackbar = remember { SnackbarHostState() }

                LaunchedEffect(Unit) {
                    repository.events.collect { snackbar.showSnackbar(it) }
                }

                NavHost(
                    navController = nav,
                    startDestination = "home",
                    enterTransition = { slideInHorizontally { it / 4 } + fadeIn() },
                    exitTransition = { fadeOut() },
                    popEnterTransition = { fadeIn() },
                    popExitTransition = { slideOutHorizontally { it / 4 } + fadeOut() },
                ) {
                    composable("home") {
                        HomeScreen(
                            sharedText = sharedText,
                            onSharedTextConsumed = { sharedText = null },
                            onOpenGroup = { id -> nav.navigate("group/$id") },
                            snackbarHostState = snackbar,
                        )
                    }
                    composable(
                        "group/{${GroupViewModel.ARG_GROUP_ID}}",
                        arguments = listOf(navArgument(GroupViewModel.ARG_GROUP_ID) { type = NavType.LongType }),
                    ) {
                        GroupScreen(
                            onBack = { if (nav.currentDestination?.route != "home") nav.popBackStack("home", false) },
                            snackbarHostState = snackbar,
                        )
                    }
                }

                // A share arriving while a group is open brings the user back home to the sheet.
                LaunchedEffect(sharedText) {
                    if (sharedText != null && nav.currentDestination?.route != "home") {
                        nav.popBackStack("home", false)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.extractSharedText()?.let { sharedText = it }
    }

    private fun Intent.extractSharedText(): String? = when (action) {
        Intent.ACTION_SEND -> getStringExtra(Intent.EXTRA_TEXT)
        Intent.ACTION_VIEW -> dataString
        else -> null
    }?.takeIf { it.isNotBlank() }
}
