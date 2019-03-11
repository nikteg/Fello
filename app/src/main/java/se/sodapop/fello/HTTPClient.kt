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
import java.io.BufferedInputStream
import java.io.InputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager


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
        /**
         * Create custom TrustManager with Fello's SSL certificate
         *
         * I'm not entirely sure, but I think it has to do that Fello's certificate is issued by a CA
         * that is not trusted by default in Android.
         *
         * See: https://developer.android.com/training/articles/security-ssl#CommonProblems
         */
        val cf: CertificateFactory = CertificateFactory.getInstance("X.509")
        val caInput: InputStream = BufferedInputStream(context.resources.openRawResource(R.raw.fello_se))
        val ca: X509Certificate = caInput.use {
            cf.generateCertificate(it) as X509Certificate
        }

        val keyStoreType = KeyStore.getDefaultType()
        val keyStore = KeyStore.getInstance(keyStoreType).apply {
            load(null, null)
            setCertificateEntry("ca", ca)
        }

        val tmfAlgorithm: String = TrustManagerFactory.getDefaultAlgorithm()
        val tmf: TrustManagerFactory = TrustManagerFactory.getInstance(tmfAlgorithm).apply {
            init(keyStore)
        }

        val sslContext: SSLContext = SSLContext.getInstance("TLS").apply {
            init(null, tmf.trustManagers, null)
        }

        client = OkHttpClient().newBuilder()
            .cookieJar(PersistentCookieJar(SetCookieCache(), SharedPrefsCookiePersistor(context)))
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
            })
            .sslSocketFactory(sslContext.socketFactory, tmf.trustManagers[0] as X509TrustManager)
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




