package se.sodapop.fello

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import se.sodapop.fello.ui.main.MainFragment
import se.sodapop.fello.ui.main.UsageFragment

class MainActivity : AppCompatActivity(), AnkoLogger {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        HTTPClient.init(this)

        val prefs = this.getSharedPreferences("LoginMetadata", Context.MODE_PRIVATE)

        val expiresAt = prefs.getLong("expiresAt", 0)
        val now = System.currentTimeMillis() / 1000

        if (BuildConfig.DEBUG) {
            info("Expires at: ${expiresAt}. Now: ${now}. Diff: ${expiresAt - now}")
        }

        if (savedInstanceState == null) {
            if (now > expiresAt) {
                HTTPClient.logout()
                loadMainFragment()
            } else {
                info("Will load usage fragment")
                loadUsageFragment()
            }
        }
    }

    fun loadMainFragment() {
        supportFragmentManager.beginTransaction().replace(R.id.container, MainFragment.newInstance()).commit()
    }

    fun loadUsageFragment() {
        supportFragmentManager.beginTransaction().replace(R.id.container, UsageFragment.newInstance()).commit()
    }

}

fun android.support.design.widget.TextInputEditText.on(actionId: Int, func: () -> Unit) {
    setOnEditorActionListener { _, receivedActionId, _ ->

        if (actionId == receivedActionId) {
            func()
        }

        true
    }
}