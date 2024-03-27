package com.ezteam.tripleuni

import android.os.Parcelable
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Notifications
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ezteam.tripleuni.MyAppGlobals.client
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import java.time.Instant


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
            val commentNum = data.getInt("post_comment_num")
            val followNum = data.getInt("post_follower_num")
            val postTimestamp = data.getLong("post_create_time")


            postListItem.add(
                PostItem(
                    postID,
                    postMsg,
                    longMsg,
                    isComplete,
                    uniPostID,
                    commentNum,
                    followNum,
                    postTimestamp
                )
            )
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
    val commentNum: Int,
    val followNum: Int,
    val postTimestamp: Long,
    var showMsg: String = shortMsg
) : Parcelable

@Composable
fun MainScreen(
    navigateToPostScreen: (Int, Int, String) -> Unit, navigateToEditPostScreen: () -> Unit
) {
    var postListItem by rememberSaveable { mutableStateOf(listOf<PostItem>()) }
    val context = LocalContext.current // 获取当前 Composable 的 Context
    val listState = rememberLazyListState()
    val isRefreshState = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val viewConfiguration = LocalViewConfiguration.current
    val currentTimestamp = Instant.now().epochSecond

    // 默认值设为true，表示首次进入页面时执行
    val shouldExecuteEffect = rememberSaveable { mutableStateOf(true) }

    // 首次进入页面时获取帖子列表
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

    // 滚动到底部时加载更多帖子
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

    // 用于判断是否发生了长按事件
    LaunchedEffect(interactionSource) {
        var isLongClick = false

        interactionSource.interactions.collectLatest { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    isLongClick = false
                    delay(viewConfiguration.longPressTimeoutMillis)
                    isLongClick = true
                    Toast.makeText(context, "WoW，原来可以发Poo了～", Toast.LENGTH_SHORT).show()
                    navigateToEditPostScreen()
                }

                is PressInteraction.Release -> {
                    if (isLongClick.not()) {
                        CoroutineScope(Dispatchers.IO).launch {
                            withContext(Dispatchers.Main) {
                                listState.scrollToItem(0)
                            }
                        }
                    }
                }

                is PressInteraction.Cancel -> {
                    isLongClick = false
                }
            }
        }
    }

    Scaffold(floatingActionButton = {
        FloatingActionButton(interactionSource = interactionSource, onClick = { }) {
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
                                Row {
                                    Row {
                                        Text(
                                            text = postItemOrigin.id.toString(),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (currentTimestamp - postItemOrigin.postTimestamp > 86400) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "你可能错过", fontSize = 12.sp,
                                                modifier = Modifier.alpha(0.6f)
                                            )
                                        }
                                    }

                                    Spacer(Modifier.weight(1f))

                                    Row {
                                        Icon(
                                            Icons.Outlined.Notifications,
                                            contentDescription = "Comment",
                                            modifier = Modifier.align(Alignment.CenterVertically).size(20.dp)
                                        )
                                        Text(
                                            text = postItemOrigin.commentNum.toString(),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Normal
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            Icons.Outlined.FavoriteBorder,
                                            contentDescription = "Follow",
                                            modifier = Modifier.align(Alignment.CenterVertically).size(20.dp)
                                        )
                                        Text(
                                            text = postItemOrigin.followNum.toString(),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Normal
                                        )
                                    }
                                }

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
    MainScreen(navigateToPostScreen = { _, _, _ -> }, navigateToEditPostScreen = {})
}