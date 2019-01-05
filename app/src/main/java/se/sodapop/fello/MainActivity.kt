package se.sodapop.fello

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import se.sodapop.fello.ui.main.MainFragment
import se.sodapop.fello.ui.main.UsageFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        HTTPClient.init(this)

        if (savedInstanceState == null) {
            if (HTTPClient.hasCookieForDomain("www.fello.se")) {
                loadUsageFragment()
            } else {
                loadMainFragment()
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