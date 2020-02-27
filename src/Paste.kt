package main

import java.util.*

/**
 * A hastily-created wrapper for a paste's metadata. Does not include the paste's raw contents. The user is advised
 * to create a paste class and parser for their use to suit their needs.
 */
class Paste(val key: String,
            val date: Long,
            val title: String,
            val size: Int,
            val expireDate: String,
            val visibility: Int,
            val formatLong: String,
            val formatShort: String,
            val url: String,
            val hits: Int) {


    override fun toString() =
        "Paste(key='$key', date=$date, title='$title', size=$size, expireDate='$expireDate', visibility=$visibility, formatLong='$formatLong', formatShort='$formatShort', url='$url', hits=$hits)"


}



/**
 * Contains some methods for parsing pastes. Messy and insecure.
 */
object PasteParser {


    /*
    Some absolutely horridly messy and insecure methods ahead. XML parsing is a pain and I'm not going to abstract
    it away from its one application in this API, which is to parse pastes.
     */



    fun parsePastes(details: List<String>): List<Paste> {
        var pastes = ArrayList<Paste>()
        var current = ArrayList<String>()

        for (d in details) {
            when(xmlTag(d)) {
                "<paste>" -> current.clear()
                "</paste>" -> pastes.add(parsePaste(current))
                else -> current.add(d)
            }
        }

        return pastes
    }



    private fun parsePaste(details: List<String>) : Paste {
        val parameters = HashMap<String, String>()

        for (d in details) parameters[trimmedXMLPasteTag(d)] = xmlContent(d)

        with(parameters) {
            return Paste(
                get("key")!!,
                get("date")!!.toLong(),
                get("title")!!,
                get("size")!!.toInt(),
                get("expire_date")!!,
                get("private")!!.toInt(),
                get("format_long")!!,
                get("format_short")!!,
                get("url")!!,
                get("hits")!!.toInt()

            )
        }
    }



    private fun xmlTag(s: String): String {
        var result = ""

        for (c in s) {
            result += c

            if (c == '>') break
        }

        return result
    }



    private fun trimmedXMLPasteTag(s: String): String {
        var result = ""

        var recording = false

        for (c in s) {
            if (c == '>') break

            if (recording) result += c

            if (c == '_') recording = true
        }

        return result
    }



    private fun xmlContent(s: String): String {
        var result = ""

        var recording = false

        var firstLessThan = false

        for (c in s) {
            if (c == '<') {
                if(firstLessThan) break

                firstLessThan = true
            }

            if (recording) result += c

            if (c == '>') recording = true
        }

        return result
    }


}