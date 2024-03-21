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
import java.net.URLEncoder
import java.nio.charset.StandardCharsets


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
            MainScreen {uniPostId, id, longMsg ->
                val encodedPostId = URLEncoder.encode(uniPostId.toString(), StandardCharsets.UTF_8.toString())
                val encodedId = URLEncoder.encode(id.toString(), StandardCharsets.UTF_8.toString())
                val encodedLongMsg = URLEncoder.encode(longMsg, StandardCharsets.UTF_8.toString())
                navController.navigate("post/$encodedPostId/$encodedId/$encodedLongMsg")
            }
        }
        composable("post/{encodedPostId}/{encodedId}/{encodedLongMsg}") {
            val postID = it.arguments?.getString("encodedPostId") ?: ""
            val id = it.arguments?.getString("encodedId") ?: ""
            var longMsg = it.arguments?.getString("encodedLongMsg") ?: ""
            longMsg = longMsg.replace("%", "%25")
            PostScreen(postID, id, longMsg)
        }
    }
}

fun loadUser(context: Context): String? {
    val sharedPreferences = context.getSharedPreferences("MyApp", Context.MODE_PRIVATE)
    val jsonString = sharedPreferences.getString("token", null)
    return jsonString
}
