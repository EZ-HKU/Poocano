package com.ezteam.tripleuni

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ezteam.tripleuni.MyAppGlobals.client
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

fun extractCommentList(
    id: String, commentListItem: MutableList<CommentItem>
): MutableList<CommentItem> {
    val jsonObject = client.getDetail(id) ?: return commentListItem
    val commentList = jsonObject.getJSONArray("comment_list")

    for (i in 0 until commentList.length()) {
        val comment = commentList.getJSONObject(i)

        try {
            val userAlias = comment.getString("user_alias")
            val commentMsg = comment.getString("comment_msg")
            val commentId = comment.getInt("comment_id")
            val commentFatherId = comment.optInt("comment_father_id", -1)

            commentListItem.add(CommentItem(userAlias, commentMsg, commentId, commentFatherId))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return getCommentListByFatherId(commentListItem)
}

data class CommentItem(
    val userAlias: String,
    val commentMsg: String,
    val commentId: Int,
    val commentFatherId: Int,
    val commentChildList: MutableList<CommentItem> = mutableListOf()
)

fun getCommentListByFatherId(commentListItem: MutableList<CommentItem>): MutableList<CommentItem> {
    // 通过commentFatherId将评论分组
    val commentListMap = mutableMapOf<Int, MutableList<CommentItem>>()
    for (commentItem in commentListItem) {
        if (commentListMap.containsKey(commentItem.commentFatherId)) {
            commentListMap[commentItem.commentFatherId]?.add(commentItem)
        } else {
            commentListMap[commentItem.commentFatherId] = mutableListOf(commentItem)
        }
    }

    // 将子评论添加到父评论的commentChildList中
    for (commentItem in commentListItem) {
        if (commentListMap.containsKey(commentItem.commentId)) {
            commentItem.commentChildList.addAll(commentListMap[commentItem.commentId]!!)
        }
    }

    return commentListMap[-1] ?: mutableListOf()

}


@Composable
fun CommentItem(commentItem: CommentItem, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(commentItem.commentMsg)
            Text(
                commentItem.userAlias,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(
                    Alignment.End
                )
            )
        }
        content()
    }
}


@Composable
fun NonLazyCommentList(commentListItem: List<CommentItem>) {
    Column {
        for (commentItem in commentListItem) {
            CommentItem(commentItem) {
                NonLazyCommentList(commentListItem = commentItem.commentChildList)
            }
        }
    }
}

@Composable
fun CommentList(commentListItem: List<CommentItem>, decodedLongMsg: String = "") {
    LazyColumn {
        item {
            // 帖子全文
            Text(decodedLongMsg)
        }
        items(commentListItem) { commentItem ->
            if (commentItem.commentFatherId == -1) {
                CommentItem(commentItem) {
                    NonLazyCommentList(commentListItem = commentItem.commentChildList)
                }
            }
        }
    }
}

@Composable
fun PostScreen(postID: String, id: String, longMsg: String) {
    val decodedPostId = URLDecoder.decode(postID, StandardCharsets.UTF_8.toString())
    val decodedId = URLDecoder.decode(id, StandardCharsets.UTF_8.toString())
    val decodedLongMsg = URLDecoder.decode(longMsg, StandardCharsets.UTF_8.toString())
    var commentListItem by remember { mutableStateOf(listOf<CommentItem>()) }

    LaunchedEffect(Unit) {
        CoroutineScope(
            Dispatchers.IO
        ).launch {
            commentListItem = extractCommentList(decodedPostId, commentListItem.toMutableList())
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row {
                Text(
                    text = decodedId,
                    fontSize = 24.sp,
                    modifier = Modifier.padding(16.dp, 16.dp, 0.dp, 16.dp)
                )
            }

            Spacer(modifier = Modifier.width(40.dp))

            CommentList(commentListItem, decodedLongMsg)
        }
    })
}


@Preview(showBackground = true)
@Composable
fun PostScreenPreview() {
    PostScreen("12345", "123", "aa")
}