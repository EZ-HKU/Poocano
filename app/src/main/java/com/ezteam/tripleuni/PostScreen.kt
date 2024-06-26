package com.ezteam.tripleuni

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.ezteam.tripleuni.MyAppGlobals.client
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URLDecoder
import java.nio.charset.StandardCharsets


data class CommentItem(
    val userAlias: String,
    val commentMsg: String,
    val commentId: Int,
    val commentFatherId: Int = -1,
    var commentFather: CommentItem? = null,
    val commentChildList: MutableList<CommentItem> = mutableListOf(),
)

class CommentListItem(
    private var commentList: MutableList<CommentItem> = mutableListOf()
) {
    companion object {
        fun fromJson(
            jsonObject: JSONObject
        ): CommentListItem {
            val commentList = jsonObject.getJSONArray("comment_list")
            val commentListItem = CommentListItem()
            val tempListItem = mutableListOf<CommentItem>()

            for (i in 0 until commentList.length()) {
                val comment = commentList.getJSONObject(i)

                try {
                    val userAlias = comment.getString("user_alias")
                    val commentMsg = comment.getString("comment_msg")
                    val commentId = comment.getInt("comment_id")
                    val commentFatherId = comment.optInt("comment_father_id", -1)

                    tempListItem.add(CommentItem(userAlias, commentMsg, commentId, commentFatherId))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            commentListItem.setCommentList(getCommentListByFatherId(tempListItem))
            return commentListItem
        }
    }

    fun getCommentList(): MutableList<CommentItem> {
        return commentList
    }

    fun setCommentList(commentList: MutableList<CommentItem>) {
        this.commentList = commentList
    }
}


fun getCommentListByFatherId(commentListItem: MutableList<CommentItem>): MutableList<CommentItem> {
    // 通过commentFatherId将评论分组
    val commentListMap = mutableMapOf<Int, MutableList<CommentItem>>()

    for (commentItem in commentListItem) {
        commentListMap[commentItem.commentId] = mutableListOf()
    }

    for (commentItem in commentListItem) {
        if (commentListMap.containsKey(commentItem.commentFatherId)) {
            commentListMap[commentItem.commentFatherId]?.add(commentItem)
        }
    }

    // 将子评论添加到父评论的commentChildList中
    for (commentItem in commentListItem) {
        if (commentListMap.containsKey(commentItem.commentId)) {
            commentItem.commentChildList.addAll(commentListMap[commentItem.commentId]!!)
        }
    }

    // set commentFather
    for (commentItem in commentListItem) {
        if (commentItem.commentFatherId != -1) {
            commentItem.commentFather =
                commentListItem.find { it.commentId == commentItem.commentFatherId }
        }
    }
    return commentListItem.filter { it.commentFatherId == -1 }.toMutableList()
}


@Composable
fun CommentItem(commentItem: CommentItem, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { },
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Red.copy(alpha = 0f) // 使用带有透明度的颜色作为背景色shape
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(8.dp))

            if (commentItem.commentFather != null) {
                Row {
                    Text(
                        (commentItem.userAlias), color = MaterialTheme.colorScheme.primary
                    )
                    Text(text = " 回复 ")
                    Text(
                        (commentItem.commentFather?.userAlias ?: ""),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Text(
                    commentItem.userAlias, color = MaterialTheme.colorScheme.primary
                ) // 评论者
            }
            // 评论内容
            Text(commentItem.commentMsg)
        }

        if (commentItem.commentChildList.isNotEmpty()) {
            Row(modifier = Modifier.height(IntrinsicSize.Max)) {
                VerticalDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(4.dp, 0.dp, 0.dp, 0.dp),
                    color = MaterialTheme.colorScheme.secondary
                )
                content()
            }
        }
    }
}


@Composable
fun NonLazyCommentList(commentListItem: List<CommentItem>) {
    Column(modifier = Modifier.padding(16.dp, 0.dp, 0.dp, 0.dp)) {
        for (commentItem in commentListItem) {
            if (commentItem.commentChildList.isNotEmpty()) {
                CommentItem(commentItem) {
                    NonLazyCommentList(commentListItem = commentItem.commentChildList)
                }
            } else {
                CommentItem(commentItem) {}
            }
        }
    }
}

@Composable
fun ReplyDialog(onDismissRequest: () -> Unit, postID: String, commentListItem: CommentListItem) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismissRequest) {
        // 使用Surface来给对话框添加圆角和背景色
        Surface(
            shape = MaterialTheme.shapes.medium, // 圆角
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                var textInput by remember { mutableStateOf("") }

                // 文本输入框
                TextField(value = textInput,
                    onValueChange = { textInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("输入评论...") })

                Spacer(modifier = Modifier.height(8.dp))

                // 发送按钮
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        CoroutineScope(Dispatchers.IO).launch {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "正在发送", Toast.LENGTH_SHORT).show()
                            }
                            val isReply = client.sendComment(textInput, postID)
                            commentListItem.setCommentList(
                                client.getDetail(postID)?.let { CommentListItem.fromJson(it) }
                                    ?.getCommentList() ?: mutableListOf()
                            )
                            if (isReply) {
                                onDismissRequest()
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "发送成功", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }) {
                        Text("发送")
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostScreen(
    postID: String, id: String, longMsg: String, navController: NavController
) { // postID: uni_post_id, id: tree_id, longMsg: postMsg
    val decodedId = URLDecoder.decode(id, StandardCharsets.UTF_8.toString())
    val decodedLongMsg = URLDecoder.decode(longMsg, StandardCharsets.UTF_8.toString())
    var commentListItem by remember { mutableStateOf(CommentListItem()) }
    var showDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current



    LaunchedEffect(Unit) {
        delay(200)
        CoroutineScope(
            Dispatchers.IO
        ).launch {
            val jsonObject = client.getDetail(postID) ?: return@launch
            commentListItem = CommentListItem.fromJson(jsonObject)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(decodedId) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(text = { Text(text = "围观") }, onClick = {
                                CoroutineScope(Dispatchers.IO).launch {
                                    val responseCode = client.followPost(postID)
                                    when (responseCode) {
                                        200 -> {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "开始围观", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        201 -> {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "取消围观", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        else -> {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "围观失败", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                                showMenu = false
                            })
                            DropdownMenuItem(text = { Text(text = "举报") }, onClick = {
                                showReportDialog = true
                                showMenu = false
                            })
                        }
                        Icon(Icons.Filled.MoreVert, contentDescription = "More")
                    }
                }
            )
        },floatingActionButton = {
        FloatingActionButton(onClick = {
            showDialog = true
        }) {
            // FloatingActionButton的内容
            Icon(Icons.Filled.Add, contentDescription = "Reply")
        }
    }, content = { innerPadding ->
        if (showDialog) {
            ReplyDialog(onDismissRequest = { showDialog = false }, postID, commentListItem)
        }
        Column(modifier = Modifier.padding(innerPadding)) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(0.dp, 0.dp, 16.dp, 0.dp)
            ) {
                Text(text = decodedLongMsg, modifier = Modifier.padding(16.dp, 0.dp))

                HorizontalDivider(
                    modifier = Modifier
                        .padding(0.dp, 16.dp, 0.dp, 8.dp)
                        .fillMaxWidth()
                )
                NonLazyCommentList(commentListItem.getCommentList())
            }
        }
    })

    if (showReportDialog) {
        AlertDialog(
            title = { Text("举报该树洞") },
            text = { Text("是否对$decodedId 进行举报操作") },
            onDismissRequest = { },
            confirmButton = {
                Button(onClick = {
                    CoroutineScope(Dispatchers.IO).launch {
                        val isSuccess = client.reportPost(postID, -1, decodedLongMsg, "来自Poocano用户的举报")
                        if (isSuccess) {
                            withContext(Dispatchers.Main) {
                            Toast.makeText(context, "举报成功", Toast.LENGTH_SHORT).show()
                            }
                            showReportDialog = false
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "举报失败", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                Button(onClick = { showReportDialog = false }) {
                    Text("取消")
                }
            },
        )
    }
}


@Preview(showBackground = true)
@Composable
fun PostScreenPreview() {
    PostScreen("12345", "123", "aa", navController = NavController(LocalContext.current))
}