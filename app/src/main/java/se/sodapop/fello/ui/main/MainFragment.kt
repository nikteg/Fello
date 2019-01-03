package se.sodapop.fello.ui.main

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import kotlinx.android.synthetic.main.main_fragment.*
import okhttp3.logging.HttpLoggingInterceptor
import se.sodapop.fello.BuildConfig
import se.sodapop.fello.R
import android.R.attr.host
import android.widget.Toast
import okhttp3.*
import java.io.IOException


class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var viewModel: MainViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    private class MyCookieJar : CookieJar {
        private val store = HashMap<String, List<Cookie>>()

        override fun saveFromResponse(url: HttpUrl, cookies: MutableList<Cookie>) {
            store.put(url.host(), cookies);
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie>? {
            val cookies = store.get(url.host())
            return if (cookies != null) cookies else ArrayList()
        }

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        // TODO: Use the ViewModel

        val client = OkHttpClient().newBuilder()
            .cookieJar(MyCookieJar())
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
            })
            .build()



        loginButton.setOnClickListener {
            val formBody = FormBody.Builder()
                .add("username", emailInput.text.toString())
                .add("password", passwordInput.text.toString())
                .build()

            val request = Request.Builder()
                .url("https://www.fello.se/wp-admin/admin-post.php?action=login")
                .post(formBody)
                .build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    // TODO: java.lang.RuntimeException: Can't toast on a thread that has not called Looper.prepare()
                    Toast.makeText(activity, "Failure", Toast.LENGTH_SHORT).show()
                }

                override fun onResponse(call: Call, response: Response) {
                    // TODO: java.lang.RuntimeException: Can't toast on a thread that has not called Looper.prepare()
                    Toast.makeText(activity, "Success", Toast.LENGTH_SHORT).show()
                }

            })
        }
    }

}
