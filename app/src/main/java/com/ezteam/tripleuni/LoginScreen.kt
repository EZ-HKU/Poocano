package com.ezteam.tripleuni

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
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
import com.ezteam.tripleuni.MyAppGlobals.client
import com.ezteam.tripleuni.ui.theme.TripleuniTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LoginScreen(navigateToMainScreen: (String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var vcode by remember { mutableStateOf("") }
    Box(modifier = Modifier.systemBarsPadding()) {

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("TripleUni", fontSize = 48.sp, modifier = Modifier.padding(16.dp, 0.dp, 0.dp, 48.dp))
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
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 验证码输入框
                    OutlinedTextField(
                        value = vcode,
                        onValueChange = { vcode = it },
                        label = { Text("验证码") },
                        modifier = Modifier.weight(1f) // 使输入框占据除按钮外的所有空间
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // 获取验证码按钮
                    Button(
                        onClick = {
                            Toast.makeText(context, "尝试发送验证码", Toast.LENGTH_SHORT).show()

                            CoroutineScope(Dispatchers.IO).launch {
                                val isSuccess = client.sendVerification(email)

                                if (isSuccess) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "验证码已发送", Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                } else {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            context, "验证码发送失败", Toast.LENGTH_SHORT
                                        ).show()
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
                            val isSuccess = client.verifyCode(vcode)

                            if (isSuccess) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "登录成功", Toast.LENGTH_SHORT).show()
                                    saveUser(context, client.getToken() ?: "")
                                    navigateToMainScreen(email)
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
}

fun saveUser(context: Context, token: String) {
    val sharedPreferences = context.getSharedPreferences("MyApp", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    editor.putString("token", token)
    editor.apply()
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TripleuniTheme {
        LoginScreen {}
    }
}