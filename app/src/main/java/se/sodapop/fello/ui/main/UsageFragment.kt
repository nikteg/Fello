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

    private fun logoutAndGoToMain() {
        HTTPClient.logout()
        (activity as MainActivity?)?.loadMainFragment()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)

        if (savedInstanceState == null) {
            logoutButton.setOnClickListener {
                logoutAndGoToMain()
            }

            doAsync {
                HTTPClient.allSubscriptions().execute()
                val usage = HTTPClient.usage()
                val data = HTTPClient.data()
                val (dataLeft, dataTotal) = parseUsage(data.monthly)
                val (dataSaved, dataSavedTotal) = parseUsage(data.saved)
                val dataUsed = (dataTotal * 1000 - dataLeft * 1000) / 1000

                runOnUiThread {
                    usageText.text =
                            """Samtal: ${usage.voicecount} st
Samtalsminuter: ${usage.voiceusage} min
SMS: ${usage.smsusage} st
MMS: ${usage.mmscount} st
Använd data: ${dataUsed} / ${dataTotal} GB
Sparad data: ${dataSaved} / ${dataSavedTotal} GB"""
                }
            }
        }
    }
}

fun parseUsage(html: String): Pair<Double, Double> {
    val regex = """(\d+,\d+) GB.+?(\d+,\d+) GB""".toRegex(RegexOption.MULTILINE)
    val result = regex.find(html)

    val dataLeft = result?.groupValues?.get(1)
    val dataTotal = result?.groupValues?.get(2)

    if (dataLeft != null && dataTotal != null) {
        return Pair(dataLeft.replace(",", ".").toDouble(), dataTotal.replace(",", ".").toDouble())
    }

    return Pair(.0, .0)
}