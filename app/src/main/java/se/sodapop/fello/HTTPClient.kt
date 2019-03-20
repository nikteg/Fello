package se.sodapop.fello

import android.content.Context
import com.franmontiel.persistentcookiejar.ClearableCookieJar
import com.franmontiel.persistentcookiejar.cache.CookieCache
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.CookiePersistor
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import com.google.gson.Gson
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

// {"invdate":"2019-01-01","voiceusage":8,"voicecount":1,"smsusage":0,"mmscount":0,"datapackage":"2 GB","datausage":"0,00"}
data class Usage(
    val invdate: String,
    val voiceusage: Int,
    val voicecount: Int,
    val smsusage: Int,
    val mmscount: Int,
    val datapackage: String,
    val datausage: String
)

// {"monthly":"<div class=\"surf_content\">\n                <p>M\u00e5nadssurf kvar<\/p>\n                <b class=\"surf_bold\">1,95 GB <\/b> kvar av <b class=\"surf_bold\">2,00 GB<\/b>\n            <\/div>\n            <div class=\"surf_bar_container monthly\">\n                <div class=\"surf_bar\" style=\"width:97%\";><p>97%<\/p><\/div>\n            <\/div>","saved":"<div class=\"surf_content\">\n                <p>Sparad surf<\/p>\n                <b class=\"surf_bold\">0,93 GB<\/b> av <b class=\"surf_bold\"> <!--0,93--> 25,00 GB<\/b>\n            <\/div>\n            <div class=\"surf_bar_container saved\">\n                <div class=\"surf_bar\" style=\"width:3%\";><p>3%<\/p><\/div>\n            <\/div>","topUp":{"status":1,"text":"<div class=\"surf_content\">\n            <p>Extra surf<\/p>        \n            <b class=\"surf_bold\"><span id=\"spn_remaning\">0,00<\/span> GB<\/b> kvar av <b class=\"surf_bold\"><span id=\"spn_valid_topup_amount\">0,00<\/span> GB<\/b>\n        <\/div>"},"enddate_topups":""}
data class Data(
    val monthly: String,
    val saved: String
)

object HTTPClient : AnkoLogger {
    lateinit var client: OkHttpClient
    val cookieDomain = "https://www.fello.se"

    fun login(email: String, password: String): Call {
        val formBody = FormBody.Builder()
            .add("username", email)
            .add("password", password)
            .build()

        val request = Request.Builder()
            .url("https://www.fello.se/wp-admin/admin-post.php?action=login")
            .post(formBody)
            .build()

        return client.newCall(request)
    }

    fun allSubscriptions(): Call {
        val request = Request.Builder()
            .url("https://www.fello.se/wp-admin/admin-ajax.php?action=all_subscriptions")
            .build()

        return client.newCall(request)
    }

    fun usage(): Usage {
        val request = Request.Builder()
            .url("https://www.fello.se/wp-admin/admin-ajax.php?action=min_forbrukning")
            .build()

        val response = client.newCall(request).execute()

        val gson = Gson()
        return gson.fromJson(response.body()?.string(), Usage::class.java)
    }

    fun data(): Data {
        val request = Request.Builder()
            .url("https://www.fello.se/wp-admin/admin-ajax.php?action=din_surf")
            .build()

        val response = client.newCall(request).execute()

        val gson = Gson()
        return gson.fromJson(response.body()?.string(), Data::class.java)
    }

    fun hasCookieSet(): Boolean {
        return client.cookieJar().loadForRequest(HttpUrl.get(cookieDomain)).isNotEmpty()
    }

    fun logout() {
        (client.cookieJar() as ClearableCookieJar).clear()
    }

    fun init(context: Context) {
        client = OkHttpClient().newBuilder()
            .cookieJar(PersistentCookieJar(SetCookieCache(), SharedPrefsCookiePersistor(context)))
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
            })
            .build()

        if (BuildConfig.DEBUG) {
            val cookiesWithExpirationDates = client.cookieJar().loadForRequest(HttpUrl.get(cookieDomain)).map {
                val time = java.util.Date(it.expiresAt())

                "${it} {$time}"
            }

            info("Cookies for cookieDomain: ${cookiesWithExpirationDates}")
        }
    }

    /**
     * NOTE: Cookie jar that removes expiration checking and persistence filtering
     */
    private class PersistentCookieJar(private val cache: CookieCache, private val persistor: CookiePersistor) :
        ClearableCookieJar {
        init {
            this.cache.addAll(persistor.loadAll())
        }

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cache.addAll(cookies)
            persistor.saveAll(cookies)
        }

        @Synchronized
        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cache.iterator().asSequence().toList()
        }

        @Synchronized
        override fun clearSession() {
            cache.clear()
            cache.addAll(persistor.loadAll())
        }

        @Synchronized
        override fun clear() {
            cache.clear()
            persistor.clear()
        }
    }
}




