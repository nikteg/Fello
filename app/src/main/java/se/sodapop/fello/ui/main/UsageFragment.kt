package se.sodapop.fello.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
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

    private val list = arrayListOf<Pair<String, String>>()

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

        val adapter = UsageAdapter(context!!, list)
        usage_list.adapter = adapter

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
                    list.clear()
                    list.add(Pair("Samtal", "${usage.voicecount} st"))
                    list.add(Pair("Samtalsminuter", "${usage.voiceusage} min"))
                    list.add(Pair("SMS", "${usage.smsusage} st"))
                    list.add(Pair("MMS", "${usage.mmscount} st"))
                    list.add(Pair("Använd data", "${dataUsed} / ${dataTotal} GB"))
                    list.add(Pair("Sparad data", "${dataSaved} / ${dataSavedTotal} GB"))

                    adapter.notifyDataSetChanged();
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