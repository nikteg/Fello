package se.sodapop.fello.ui.main

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import se.sodapop.fello.R

class UsageAdapter(
    context: Context,
    private val dataSource: ArrayList<Pair<String, String>>
) : BaseAdapter() {

    private val inflater: LayoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getCount(): Int {
        return dataSource.size
    }

    //2
    override fun getItem(position: Int): Pair<String, String> {
        return dataSource[position]
    }

    //3
    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val rowView = inflater.inflate(R.layout.usage_row, parent, false)
        val labelView = rowView.findViewById<TextView>(R.id.usage_row_label)
        val valueView = rowView.findViewById<TextView>(R.id.usage_row_value)

        labelView.text = getItem(position).first
        valueView.text = getItem(position).second

        return rowView
    }

}