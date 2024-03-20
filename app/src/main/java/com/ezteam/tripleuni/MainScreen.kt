package com.ezteam.tripleuni

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ezteam.tripleuni.MyAppGlobals.client
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


fun extractPostMessages(postListItem: MutableList<PostItem>): MutableList<PostItem> {
    // 将字符串解析为JSONObject
    val jsonObject = client.getList() ?: return postListItem

    // 获取"one_list"的JSONArray
    val oneList = jsonObject.getJSONArray("one_list")


    // 遍历"one_list"数组
    for (i in 0 until oneList.length()) {
        // 获取每个元素（即每个帖子）作为JSONObject
        val post = oneList.getJSONObject(i)

        // 从每个帖子的"data"对象中提取"post_msg"
        val data = post.getJSONObject("data")
        val postMsg = data.getString("post_msg_short")
        val postID = data.getInt("post_id")

        postListItem.add(PostItem(postID, postMsg))
    }
    return postListItem
}

data class PostItem(val id: Int, val shortMsg: String)

@Composable
fun MainScreen() {
    var postListItem by remember { mutableStateOf(listOf<PostItem>()) }


    val context = LocalContext.current // 获取当前 Composable 的 Context

    LaunchedEffect(Unit) {
        Toast.makeText(context, "获取帖子列表", Toast.LENGTH_SHORT).show()
        CoroutineScope(Dispatchers.IO).launch {
            postListItem = extractPostMessages(postListItem.toMutableList())
        }
    }

    Box(modifier = Modifier.systemBarsPadding()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp, 0.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .height(400.dp)
            ) {

                items(postListItem) {
                    Text(text = it.id.toString())
                    Text(text = it.shortMsg)
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MainScreen()
}