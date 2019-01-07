package se.sodapop.fello

import org.junit.Test

import org.junit.Assert.*
import se.sodapop.fello.ui.main.parseUsage

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class Tests {
    @Test
    fun parsesMonthlyDataCorrectly() {
        val html = "<div class=\"surf_content\">\n" +
                "                <p>Månadssurf kvar</p>\n" +
                "                <b class=\"surf_bold\">1,95 GB </b> kvar av <b class=\"surf_bold\">2,00 GB</b>\n" +
                "            </div>\n" +
                "            <div class=\"surf_bar_container monthly\">\n" +
                "                <div class=\"surf_bar\" style=\"width:97%\";><p>97%</p></div>\n" +
                "            </div>"
        assertEquals(parseUsage(html), Pair(1.95, 2.00))
    }

    @Test
    fun parsesSavedDataCorrectly() {
        val html = "<div class=\"surf_content\">\n" +
                "                <p>Sparad surf</p>\n" +
                "                <b class=\"surf_bold\">0,93 GB</b> av <b class=\"surf_bold\"> <!--0,93--> 25,00 GB</b>\n" +
                "            </div>\n" +
                "            <div class=\"surf_bar_container saved\">\n" +
                "                <div class=\"surf_bar\" style=\"width:3%\";><p>3%</p></div>\n" +
                "            </div>"
        assertEquals(parseUsage(html), Pair(0.93, 25.00))
    }


}
