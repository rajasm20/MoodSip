package com.example.moodsip.ui.components
import android.graphics.Color as AndroidColor
import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.github.mikephil.charting.data.Entry
import com.example.moodsip.R

class ChartMarkerView(
    context: Context,
    private val labels: List<String>,
    private val chartType: String,
    private val backgroundColorHex: String
) : MarkerView(context, R.layout.marker_view) {

    private val markerText: TextView = findViewById(R.id.marker_text)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let {
            val index = e.x.toInt()
            val label = labels.getOrNull(index) ?: "N/A"

            val icon = when (chartType) {
                "Hydration" -> "\uD83D\uDCA7"
                "Mood" -> "\uD83D\uDE0A"
                "Energy" -> "\u26A1"
                else -> ""
            }

            val valueText = when (chartType) {
                "Hydration" -> e.y.toInt().toString()
                else -> String.format("%.1f", e.y)
            }

            markerText.text = "$label\n$icon $valueText"

            this.setBackgroundColor(AndroidColor.parseColor(backgroundColorHex))
        }
        super.refreshContent(e, highlight)
    }


    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2f), -height.toFloat())
    }
}
