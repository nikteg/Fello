package se.sodapop.fello.ui.main

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.usage_fragment.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.support.v4.runOnUiThread
import se.sodapop.fello.HTTPClient
import se.sodapop.fello.MainActivity
import se.sodapop.fello.R

class UsageFragment : Fragment() {

    companion object {
        fun newInstance() = UsageFragment()
    }

    private lateinit var viewModel: MainViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.usage_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)

        doAsync {
            HTTPClient.allSubscriptions().execute()
            val usage = HTTPClient.usage()
            val data = HTTPClient.data()
            val (dataLeft, dataTotal) = parseMonthlyUsage(data.monthly)

            runOnUiThread {
                usageText.text =
                    """Samtal: ${usage.voicecount} st
Samtalsminuter: ${usage.voiceusage} min
SMS: ${usage.smsusage} st
MMS: ${usage.mmscount} st
Data: ${dataLeft} GB kvar av ${dataTotal} GB"""
            }
        }

        logoutButton.setOnClickListener {
            HTTPClient.logout()
            (activity as MainActivity?)?.loadMainFragment()
        }
    }

}

fun parseMonthlyUsage(monthlyHtml: String): Pair<String, String> {
    val regex = """(\d+,\d+) GB.+(\d+,\d+) GB""".toRegex(RegexOption.MULTILINE)
    val result = regex.find(monthlyHtml)

    val dataLeft = result?.groupValues?.get(1)
    val dataTotal = result?.groupValues?.get(2)

    return Pair(dataLeft ?: "N/A", dataTotal ?: "N/A")
}