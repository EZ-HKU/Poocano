package com.ezteam.tripleuni

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ezteam.tripleuni.MyAppGlobals.client
import com.ezteam.tripleuni.ui.theme.TripleuniTheme


object MyAppGlobals {
    var client = TripleClient()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TripleuniTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MyApp()
                }
            }
        }
    }
}

@Composable
fun MyApp() {
    val context = LocalContext.current
    var des = "login"
    val temp = loadUser(context)
    if (temp != null) {
        client.setToken(temp)
        des = "main"
    }
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = des) {
        composable("login") {
            LoginScreen {
                navController.navigate("main") {
                    popUpTo("login") {
                        inclusive = true
                    }
                }
            }
        }
        composable("main") {
            MainScreen()
        }
    }
}

fun loadUser(context: Context): String? {
    val sharedPreferences = context.getSharedPreferences("MyApp", Context.MODE_PRIVATE)
    val jsonString = sharedPreferences.getString("token", null)
    return jsonString
}
