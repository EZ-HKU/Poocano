package com.ezteam.tripleuni

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class TripleClient {
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

    fun getList(): JSONObject? {
        val token = this.token ?: return null
        val listData = FormBody.Builder().add("page", pageNum.toString()).add("token", token)
            .add("language", "zh-CN").build()
        val request =
            Request.Builder().url("https://api.tripleuni.com/v4/post/list/all.php").post(listData)
                .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Unexpected code $response")
                val responseData = JSONObject(response.body?.string() ?: "{}")
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

    fun getTemp(): JSONObject? {

        if (tempPostList == null) {
            tempPostList = getList()
            isUpdate = true
        }

        while (!isUpdate) {
            Thread.sleep(100)
        }

        isUpdate = false
        return tempPostList
    }

    fun updateTemp() {
        tempPostList = getList()
        isUpdate = true
    }

    fun sendVerification(email: String): Boolean {
        this.email = email
        val emailData = FormBody.Builder().add("user_email", email).add("language", "zh-CN").build()
        val request =
            Request.Builder().url("https://api.tripleuni.com/v4/user/register/web/email.php")
                .post(emailData).build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Unexpected code $response")
                val responseData = JSONObject(response.body?.string() ?: "{}")
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

    fun verifyCode(vcode: String): Boolean {
        val storedData = this.storedData ?: return false
        val verifyData = FormBody.Builder().add("vcode_vcode", vcode)
            .add("vcode_key", storedData.getString("vcode_key"))
            .add("user_itsc", storedData.getString("user_itsc"))
            .add("user_email_suffix", storedData.getString("user_email_suffix"))
            .add("user_school_label", storedData.getString("user_school_label"))
            .add("language", "zh-CN").build()
        val request =
            Request.Builder().url("https://api.tripleuni.com/v4/user/register/web/verify.php")
                .post(verifyData).build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Unexpected code $response")
                val responseData = JSONObject(response.body?.string() ?: "{}")
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
}