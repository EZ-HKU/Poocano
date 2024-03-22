package com.ezteam.tripleuni

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ezteam.tripleuni.MyAppGlobals.client
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
            .padding(0.dp, 0.dp, 16.dp, 0.dp)
            .clickable { },
        colors = CardDefaults.cardColors(
            containerColor = Color.Red.copy(alpha = 0f) // 使用带有透明度的颜色作为背景色
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
fun PostScreen(
    postID: String, id: String, longMsg: String
) { // postID: uni_post_id, id: tree_id, longMsg: postMsg
    val decodedId = URLDecoder.decode(id, StandardCharsets.UTF_8.toString())
    val decodedLongMsg = URLDecoder.decode(longMsg, StandardCharsets.UTF_8.toString())
    var commentListItem by remember { mutableStateOf(CommentListItem()) }

    LaunchedEffect(Unit) {
        delay(500)
        CoroutineScope(
            Dispatchers.IO
        ).launch {
            val jsonObject = client.getDetail(postID) ?: return@launch
            commentListItem = CommentListItem.fromJson(jsonObject)
        }
    }

    Scaffold(floatingActionButton = {
        FloatingActionButton(onClick = {
            //TODO("回复评论")
        }) {
            // FloatingActionButton的内容
            Icon(Icons.Filled.Add, contentDescription = "Reply")
        }
    }, content = { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Row {
                Text(
                    text = decodedId,
                    fontSize = 24.sp,
                    modifier = Modifier.padding(16.dp, 16.dp, 0.dp, 16.dp)
                )
            }
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
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
}


@Preview(showBackground = true)
@Composable
fun PostScreenPreview() {
    PostScreen("12345", "123", "aa")
}