package app.sharedl

import android.content.Context
import android.webkit.CookieManager
import java.io.File

/** Stores the WebView session in app-private, non-backed-up storage for yt-dlp. */
object InstagramSession {
    private const val COOKIE_FILE = "instagram-cookies.txt"

    fun cookieFile(context: Context): File = File(context.noBackupFilesDir, COOKIE_FILE)

    fun hasCookies(context: Context): Boolean {
        val file = cookieFile(context)
        return file.isFile && file.readText().lineSequence().any { it.contains("\tsessionid\t") }
    }

    fun captureCookies(context: Context): Boolean {
        val raw = CookieManager.getInstance().getCookie("https://www.instagram.com") ?: return false
        val cookies = raw.split(';').mapNotNull { item ->
            val pair = item.trim()
            val equalsAt = pair.indexOf('=')
            if (equalsAt <= 0) null else pair.substring(0, equalsAt).trim() to pair.substring(equalsAt + 1).trim()
        }
        if (cookies.none { it.first == "sessionid" } || cookies.isEmpty()) return false

        val file = cookieFile(context)
        val temp = File(context.noBackupFilesDir, "$COOKIE_FILE.tmp")
        temp.bufferedWriter().use { writer ->
            writer.appendLine("# Netscape HTTP Cookie File")
            cookies.forEach { (name, value) ->
                if ('\t' !in name && '\n' !in name && '\t' !in value && '\n' !in value) {
                    writer.append(".instagram.com\tTRUE\t/\tTRUE\t0\t")
                        .append(name).append('\t').appendLine(value)
                }
            }
        }
        if (file.exists()) file.delete()
        if (!temp.renameTo(file)) {
            temp.copyTo(file, overwrite = true)
            temp.delete()
        }
        return file.isFile && file.length() > 0
    }

    fun clear(context: Context) {
        cookieFile(context).delete()
        CookieManager.getInstance().removeAllCookies { CookieManager.getInstance().flush() }
    }
}
