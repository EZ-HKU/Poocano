package com.ezteam.tripleuni

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.ezteam.tripleuni.MyAppGlobals.client


@Composable
fun EditPostScreen(navController: NavController) {
    var postText by remember { mutableStateOf(TextFieldValue()) }
    var selectedTopic by remember { mutableStateOf("") }
    var selectedVisibility by remember { mutableStateOf("") }
    var selectedAuthenticity by remember { mutableStateOf("") }
    var selectedSchool by remember { mutableStateOf("") }
    val context = LocalContext.current

    Scaffold(floatingActionButton = {
        FloatingActionButton(onClick = {
            CoroutineScope(Dispatchers.IO).launch {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "正在发送", Toast.LENGTH_SHORT).show()
                }
                val isSuccess = client.postPoster(
                    postText.text,
                    selectedTopic,
                    selectedAuthenticity,
                    selectedVisibility,
                    selectedSchool
                )
                if (isSuccess) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "发送成功", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "发送失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
        }
    }, content = { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val screenHeight = maxHeight

            Column {
                Text(
                    text = "New Poo~",
                    fontSize = 24.sp,
                    modifier = Modifier.padding(16.dp, 16.dp, 0.dp, 16.dp)
                )

                Column(modifier = Modifier.padding(16.dp, 0.dp, 16.dp, 0.dp)) {
                    // Post Text Field
                    TextField(value = postText,
                        onValueChange = { postText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(screenHeight / 6, screenHeight / 2),
                        placeholder = { Text("输入内容") })

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dropdowns Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DropdownSelector(
                            "话题",
                            listOf(
                                DropdownOption("Poocano", "Poocano"),
                                DropdownOption("情感", "情感"),
                                DropdownOption("随写", "随写"),
                                DropdownOption("学业", "学业"),
                                DropdownOption("求职", "求职"),
                                DropdownOption("美食", "美食"),
                                DropdownOption("跳蚤", "跳蚤")
                            ),
                            selectedTopic
                        ) { selectedTopic = it }
                        DropdownSelector(
                            "可见", listOf(
                                DropdownOption("公开", "1"),
                                DropdownOption("私密", "2")
                            ), selectedVisibility
                        ) { selectedVisibility = it }
                        DropdownSelector(
                            "实名", listOf(
                                DropdownOption("匿名", "false"),
                                DropdownOption("实名", "true")
                            ), selectedAuthenticity
                        ) { selectedAuthenticity = it }
                        DropdownSelector("范围", listOf(
                            DropdownOption("本校", "false"),
                            DropdownOption("Uni", "true")
                        ), selectedSchool) {
                            selectedSchool = it
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    })
}

data class DropdownOption(val label: String, val value: String)

@Composable
fun DropdownSelector(
    label: String,
    options: List<DropdownOption>,
    selectedValue: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.value == selectedValue }?.label ?: label


    Box(modifier = Modifier.wrapContentSize()) {
        Text(text = selectedLabel,
            modifier = Modifier.clickable { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(onClick = {
                    onOptionSelected(option.value)
                    expanded = false
                }, text = { Text(option.label) })
            }
        }
    }
}