package com.ezteam.tripleuni.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Post(
    val id: Int,
    val shortMsg: String,
    val longMsg: String,
    var isComplete: Boolean,
    val uniPostID: Int,
    val commentNum: Int,
    val followNum: Int,
    val postTimestamp: Long,
    var showMsg: String = shortMsg,
    var isFollowing: Boolean
) : Parcelable
