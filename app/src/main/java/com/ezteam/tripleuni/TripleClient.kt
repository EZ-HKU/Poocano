package com.ezteam.tripleuni

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class TripleClient {
    private val domain = "uuunnniii.com"

    private val client = OkHttpClient()
    private var ifLogin = false
    private var email: String? = null
    private var storedData: JSONObject? = null
    private var token: String? = null
    private var pageNum: Int = 0
    private var tempPostList: JSONObject? = null
    private var isUpdate = false

    init {
        client.newBuilder().readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        client.newBuilder().writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        client.newBuilder().connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
    }

    fun getToken(): String? {
        return token
    }

    fun setToken(token: String) {
        this.token = token
    }

    fun resetPageNum() {
        pageNum = 0
    }

    fun resetTempPostList() {
        tempPostList = null
    }

    private fun getList(topic: String): JSONObject? {
        var url = "https://api.$domain/v4/post/list/all.php"

        val token = this.token ?: return null
        val listData = FormBody.Builder().add("page", pageNum.toString()).add("token", token)
            .add("language", "zh-CN")

        if (topic != "全部") {
            url = "https://api.$domain/v4/post/list/topic.php"
            listData.add("post_topic", topic)
        }

        val request = Request.Builder().url(url).post(listData.build()).build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Unexpected code $response")
                val responseData = JSONObject(response.body.string())
                if (responseData.getInt("code") == 200) {
                    pageNum++
                    return responseData
                }
                return null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun getProfile(): JSONObject? {
        val token = this.token ?: return null
        val profileData = FormBody.Builder().add("token", token).add("language", "zh-CN").build()
        val request = Request.Builder().url("https://api.$domain/v4/user/profile/get.php")
            .post(profileData).build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Unexpected code $response")
                val responseData = JSONObject(response.body.string())
                if (responseData.getInt("code") == 200) {
                    return responseData
                }
                return null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun getDetail(id: String): JSONObject? {

        val token = this.token ?: return null

        val detailData =
            FormBody.Builder().add("uni_post_id", id).add("token", token).add("language", "zh-CN")
                .build()
        val request = Request.Builder().url("https://api.$domain/v4/post/single/get.php")
            .post(detailData).build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Unexpected code $response")
                val responseData = JSONObject(response.body.string())
                // show responseData in logcat
                print(responseData.getInt("code"))
                if (responseData.getInt("code") == 200) {
                    return responseData
                }
                return null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun getTemp(topic: String): JSONObject? {

        if (tempPostList == null) {
            tempPostList = getList(topic)
            isUpdate = true
        }

        while (!isUpdate) {
            Thread.sleep(100)
        }

        isUpdate = false
        return tempPostList
    }

    fun updateTemp(topic: String) {
        tempPostList = getList(topic)
        isUpdate = true
    }

    fun sendVerification(email: String): Boolean {
        this.email = email
        val emailData = FormBody.Builder().add("user_email", email).add("language", "zh-CN").build()
        val request =
            Request.Builder().url("https://api.$domain/v4/user/register/web/email.php")
                .post(emailData).build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Unexpected code $response")
                val responseData = JSONObject(response.body.string())
                if (responseData.getInt("code") == 200) {
                    storedData = responseData
                    return true
                }
                return false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun sendComment(content: String, postId: String): Boolean {
        val commentData = FormBody.Builder().add("uni_post_id", postId).add("comment_msg", content)
            .add("user_is_real_name", "false").add("token", this.token!!).add("language", "zh-CN")
        val request = Request.Builder().url("https://api.$domain/v4/comment/post.php")
            .post(commentData.build()).build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Unexpected code $response")
                val responseData = JSONObject(response.body.string())
                return responseData.getInt("code") == 200
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false

        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun postPoster(
        content: String, topic: String, real: String, public: String, uni: String
    ): Boolean {
        val postData = FormBody.Builder().add("post_msg", content).add("post_topic", topic)
            .add("user_is_real_name", real).add("post_public", public).add("post_is_uni", uni)
            .add("post_image", "[]").add("token", this.token!!).add("language", "zh-CN")
        val request = Request.Builder().url("https://api.$domain/v4/post/single/post.php")
            .post(postData.build()).build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Unexpected code $response")
                val responseData = JSONObject(response.body.string())
                return responseData.getInt("code") == 200
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun verifyCode(vcode: String): Boolean {
        val storedData = this.storedData ?: return false
        val verifyData = FormBody.Builder().add("vcode_vcode", vcode)
            .add("vcode_key", storedData.getString("vcode_key"))
            .add("user_itsc", storedData.getString("user_itsc"))
            .add("user_email_suffix", storedData.getString("user_email_suffix"))
            .add("user_school_label", storedData.getString("user_school_label"))
            .add("language", "zh-CN").build()
        val request =
            Request.Builder().url("https://api.$domain/v4/user/register/web/verify.php")
                .post(verifyData).build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Unexpected code $response")
                val responseData = JSONObject(response.body.string())
                print(responseData)
                if (responseData.getInt("code") != 200) {
                    return false
                }
                token = responseData.getString("token")
                ifLogin = true
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun reportPost(
        uniPostId: String, commentOrder: Int, commentMsg: String, reportMsg: String
    ): Boolean {
        val token = this.token ?: return false
        val reportData = FormBody.Builder().add("uni_post_id", uniPostId)
            .add("comment_order", commentOrder.toString()).add("comment_msg", commentMsg)
            .add("report_msg", reportMsg).add("token", token).add("language", "zh-CN").build()

        val request = Request.Builder().url("https://api.$domain/v4/post/single/report.php")
            .post(reportData).build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Unexpected code $response")
                val responseData = JSONObject(response.body.string())
                return responseData.getInt("code") == 200
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun followPost(uniPostId: String): Int {
        val token = this.token ?: return 0
        val followData = FormBody.Builder().add("uni_post_id", uniPostId)
            .add("token", token).add("language", "zh-CN")
            .build()

        val request = Request.Builder().url("https://api.$domain/v4/post/single/follow.php")
            .post(followData).build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Unexpected code $response")
                val responseData = JSONObject(response.body.string())

                return responseData.getInt("code")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return 0
        }
    }
}