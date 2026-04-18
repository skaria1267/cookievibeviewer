package com.cookievibe.viewer.web

object UserScriptInjector {

    fun wrap(script: UserScript): String {
        val info = buildString {
            append("{script:{")
            append("name:").append(js(script.name)).append(',')
            append("namespace:").append(js(script.namespace)).append(',')
            append("version:").append(js(script.version)).append(',')
            append("description:").append(js(script.description)).append(',')
            append("author:").append(js(script.author))
            append("}}")
        }
        val tag = js("[UserScript:${script.name}]")
        return """
            ;(function(){
              try {
                var GM_info = $info;
                var GM = { info: GM_info };
                ${script.code}
              } catch (__uerr) {
                try { console.error($tag, __uerr && __uerr.stack || __uerr); } catch (_) {}
              }
            })();
        """.trimIndent()
    }

    private fun js(s: String): String {
        val sb = StringBuilder("\"")
        for (c in s) when (c) {
            '\\' -> sb.append("\\\\")
            '"' -> sb.append("\\\"")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            '\u2028' -> sb.append("\\u2028")
            '\u2029' -> sb.append("\\u2029")
            '<' -> sb.append("\\u003c")
            '>' -> sb.append("\\u003e")
            else -> sb.append(c)
        }
        sb.append('"')
        return sb.toString()
    }
}
