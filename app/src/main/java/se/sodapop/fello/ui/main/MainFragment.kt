package se.sodapop.fello.ui.main

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.main_fragment.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.support.v4.runOnUiThread
import se.sodapop.fello.HTTPClient
import se.sodapop.fello.MainActivity
import se.sodapop.fello.R
import se.sodapop.fello.on
import java.io.IOException

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (savedInstanceState == null) {

            val prefs = activity?.getSharedPreferences("LoginMetadata", Context.MODE_PRIVATE)
            val previousEmail = prefs?.getString("email", "")

            emailInput.setText(previousEmail)

            loginButton.setOnClickListener {
                doAsync {
                    val email = emailInput.text.toString()
                    val password = passwordInput.text.toString()

                    val response = HTTPClient.login(email, password).execute()
                    val responseBody = response.body()?.string()!!

                    if (responseBody.contains("\"/mina-sidor/\"")) {
                        runOnUiThread {
                            val prefsEditable = prefs?.edit()

                            prefsEditable?.putLong("expiresAt", System.currentTimeMillis() / 1000 + 24 * 60) // 24 minutes
                            prefsEditable?.putString("email", email)

                            prefsEditable?.apply()

                            (activity as MainActivity?)?.loadUsageFragment()
                        }
                    } else if (responseBody.contains("login not found")) {
                        runOnUiThread { Toast.makeText(activity, "Felaktig inloggning", Toast.LENGTH_SHORT).show() }
                    }
                }
            }

            passwordInput.on(EditorInfo.IME_ACTION_DONE) {
                loginButton.performClick()

                val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
                imm?.hideSoftInputFromWindow(view?.getWindowToken(), 0)
            }
        }
    }

}
