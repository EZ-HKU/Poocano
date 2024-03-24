package com.ezteam.tripleuni

import android.os.Parcelable
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ezteam.tripleuni.MyAppGlobals.client
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize


fun extractPostMessages(postListItem: MutableList<PostItem>): MutableList<PostItem> {

    // 将字符串解析为JSONObject
    val jsonObject = client.getTemp() ?: return postListItem

    // 获取"one_list"的JSONArray
    val oneList = jsonObject.getJSONArray("one_list")

    // 遍历"one_list"数组
    for (i in 0 until oneList.length()) {
        // 获取每个元素（即每个帖子）作为JSONObject
        val post = oneList.getJSONObject(i)

        try {
            // 从每个帖子的"data"对象中提取"post_msg"
            val data = post.getJSONObject("data")
            val postMsg = data.getString("post_msg_short")
            val postID = data.getInt("post_id")
            val longMsg = data.getString("post_msg")
            val isComplete = data.getBoolean("post_msg_short_is_complete")
            val uniPostID = data.getInt("uni_post_id")

            postListItem.add(PostItem(postID, postMsg, longMsg, isComplete, uniPostID))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return postListItem
}

@Parcelize
data class PostItem(
    val id: Int,
    val shortMsg: String,
    val longMsg: String,
    var isComplete: Boolean,
    val uniPostID: Int,
    var showMsg: String = shortMsg
) : Parcelable

@Composable
fun MainScreen(navigateToPostScreen: (Int, Int, String) -> Unit) {
    var postListItem by rememberSaveable { mutableStateOf(listOf<PostItem>()) }
    val context = LocalContext.current // 获取当前 Composable 的 Context
    val listState = rememberLazyListState()
    val isRefreshState = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // 默认值设为true，表示首次进入页面时执行
    val shouldExecuteEffect = rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        if (shouldExecuteEffect.value) {
            shouldExecuteEffect.value = false
            Toast.makeText(context, "获取帖子列表", Toast.LENGTH_SHORT).show()
            CoroutineScope(Dispatchers.IO).launch {
                postListItem = extractPostMessages(postListItem.toMutableList())
                client.updateTemp()
            }
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }.collect { lastIndex ->
            if (lastIndex != null && lastIndex >= postListItem.size - 1) {
                CoroutineScope(Dispatchers.IO).launch {
                    postListItem = extractPostMessages(postListItem.toMutableList())
                    client.updateTemp()
                }
            }
        }
    }

    Scaffold(floatingActionButton = {
        FloatingActionButton(onClick = {
            CoroutineScope(Dispatchers.IO).launch {
                withContext(Dispatchers.Main) {
                    listState.scrollToItem(0)
                }
            }
        }) {
            // FloatingActionButton的内容
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Top")
        }
    }, content = { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // 应用从Scaffold获得的innerPadding
        ) {
            Text(
                text = "Poocano",
                fontSize = 24.sp,
                modifier = Modifier.padding(16.dp, 16.dp, 0.dp, 16.dp)
            )

            SwipeRefresh(state = rememberSwipeRefreshState(isRefreshing = isRefreshState.value),
                onRefresh = {
                    isRefreshState.value = true
                    client.resetPageNum()
                    client.resetTempPostList()
                    Toast.makeText(context, "正在刷新", Toast.LENGTH_SHORT).show()
                    scope.launch {
                        postListItem = extractPostMessages(mutableListOf())
                    }
                    CoroutineScope(Dispatchers.IO).launch {
                        postListItem = extractPostMessages(mutableListOf())
                        isRefreshState.value = false
                        client.updateTemp()
                    }


                }) {
                LazyColumn(
                    state = listState, modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp, 0.dp)
                ) {
                    items(postListItem) { postItem ->

                        var postItemOrigin by remember { mutableStateOf(postItem) }

                        Card(modifier = Modifier
                            .padding(vertical = 8.dp)
                            .fillMaxWidth()
                            .clickable {

                                if (postItemOrigin.isComplete) navigateToPostScreen(
                                    postItemOrigin.uniPostID,
                                    postItemOrigin.id,
                                    postItemOrigin.longMsg
                                )
                                postItemOrigin = postItemOrigin.copy(
                                    isComplete = true, showMsg = postItemOrigin.longMsg
                                )
                            }) {

                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth()
                            ) {
                                // LazyColumn中每个item的布局
                                Text(
                                    text = postItemOrigin.id.toString(),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(text = postItemOrigin.showMsg)

                                if (!postItemOrigin.isComplete) {
                                    Text(
                                        "...",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.align(
                                            Alignment.End
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    })
}


@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MainScreen(navigateToPostScreen = { _, _, _ -> })
}