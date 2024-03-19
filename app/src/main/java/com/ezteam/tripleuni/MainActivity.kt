package com.ezteam.tripleuni

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ezteam.tripleuni.ui.theme.TripleuniTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TripleuniTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LoginScreen()
                }
            }
        }
    }
}

@Composable
fun LoginScreen() {
    var email by remember { mutableStateOf("") }
    var vcode by remember { mutableStateOf("") }
    val client = remember { mutableStateOf(TripleClient()) }


    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("TripleUni", fontSize = 48.sp, modifier = Modifier.padding(64.dp))
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 邮箱输入框
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("邮箱") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
            val context = LocalContext.current
            // 验证码输入行
            Row(
                verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()
            ) {
                // 验证码输入框
                OutlinedTextField(
                    value = vcode,
                    onValueChange = { vcode = it },
                    label = { Text("验证码") },
                    modifier = Modifier.weight(1f) // 使输入框占据除按钮外的所有空间
                )

                Spacer(modifier = Modifier.width(8.dp))

                val context = LocalContext.current // 获取当前 Composable 的 Context

                // 获取验证码按钮
                Button(
                    onClick = {
                        CoroutineScope(Dispatchers.IO).launch {
                            val isSuccess = client.value.sendVerification(email)

                            if (isSuccess) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "验证码已发送", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "验证码发送失败", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }
                        }
                    }, modifier = Modifier.align(Alignment.Bottom)
                ) {
                    Text("获取验证码")
                }
            }

            Button(
                onClick = {
                    CoroutineScope(Dispatchers.IO).launch {
                    val isSuccess = client.value.verifyCode(vcode)
                    print(isSuccess)


                    if (isSuccess) {
                            withContext(Dispatchers.Main) {
                        Toast.makeText(context, "登录成功", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                            withContext(Dispatchers.Main) {
                        Toast.makeText(context, "登录失败", Toast.LENGTH_SHORT).show()
                            }
                    }
                    }
                }, modifier = Modifier.padding(64.dp)
            ) {
                Text("登录")
            }
        }
    }
}
