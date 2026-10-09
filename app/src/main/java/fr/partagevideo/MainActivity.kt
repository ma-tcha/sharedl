package fr.partagevideo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import java.util.concurrent.Executors
import dev.ffmpegkit_maintained.ytdlp.YtDlp
import dev.ffmpegkit_maintained.ytdlp.YtDlpRequest

class MainActivity : Activity() {
    private lateinit var content: LinearLayout
    private lateinit var downloadStatus: TextView
    private val downloader = Executors.newSingleThreadExecutor()
    private val videosDir by lazy { File(getExternalFilesDir("Movies"), "PartageVideo").apply { mkdirs() } }
    private val prefs by lazy { getSharedPreferences("settings", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
        handleSharedLink(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSharedLink(intent)
    }

    private fun render() {
        val scroll = ScrollView(this)
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 32, 24, 24)
        }
        scroll.addView(content)
        setContentView(scroll)

        content.addView(TextView(this).apply {
            text = "Partage Vidéo"
            textSize = 28f
        })
        content.addView(TextView(this).apply {
            text = "Les vidéos reçues et enregistrées apparaîtront ici."
            textSize = 16f
            setPadding(0, 12, 0, 18)
        })
        content.addView(Switch(this).apply {
            text = "Enregistrer une copie sur le téléphone"
            isChecked = prefs.getBoolean("save_locally", true)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("save_locally", checked).apply()
            }
        })
        content.addView(TextView(this).apply {
            text = if (InstagramSession.hasCookies(this@MainActivity))
                "Instagram : session enregistrée sur cet appareil"
            else "Instagram : connexion à effectuer si le lien l’exige"
            textSize = 14f
            setPadding(0, 14, 0, 4)
        })
        content.addView(Button(this).apply {
            text = "Connecter Instagram"
            setOnClickListener {
                startActivityForResult(Intent(this@MainActivity, InstagramLoginActivity::class.java), REQUEST_INSTAGRAM_LOGIN)
            }
        })
        if (InstagramSession.hasCookies(this)) {
            content.addView(Button(this).apply {
                text = "Déconnecter Instagram"
                setOnClickListener {
                    InstagramSession.clear(this@MainActivity)
                    Toast.makeText(this@MainActivity, "Session Instagram supprimée de l’application.", Toast.LENGTH_SHORT).show()
                    render()
                }
            })
        }
        downloadStatus = TextView(this).apply {
            textSize = 14f
            setPadding(0, 14, 0, 4)
        }
        content.addView(downloadStatus)
        content.addView(TextView(this).apply {
            text = "Vidéos enregistrées"
            textSize = 20f
            setPadding(0, 28, 0, 8)
        })
        val files = videosDir.listFiles()?.filter { it.isFile && it.extension.lowercase() in setOf("mp4", "mov", "webm") }.orEmpty()
        if (files.isEmpty()) {
            content.addView(TextView(this).apply { text = "Aucune vidéo pour le moment." })
        } else files.sortedByDescending { it.lastModified() }.forEach { file ->
            content.addView(Button(this).apply {
                text = "Partager · ${file.name}"
                setOnClickListener { shareVideo(file) }
            })
            content.addView(TextView(this).apply {
                text = file.absolutePath
                textSize = 12f
                setPadding(8, 0, 8, 12)
            })
        }
    }

    private fun handleSharedLink(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        val shared = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()?.trim().orEmpty()
        val link = Regex("https?://\\S+", RegexOption.IGNORE_CASE).find(shared)?.value?.trimEnd('.', ',', ')', ']')
        if (link.isNullOrBlank()) {
            Toast.makeText(this, "Aucun lien vidéo reçu.", Toast.LENGTH_LONG).show()
            return
        }
        startDownload(link)
    }

    private fun startDownload(link: String, allowLoginPrompt: Boolean = true) {
        downloadStatus.text = "Préparation du téléchargement…"
        val saveLocally = prefs.getBoolean("save_locally", true)
        val outputDir = if (saveLocally) videosDir else File(cacheDir, "shared-videos").apply { mkdirs() }
        val startAt = System.currentTimeMillis()
        val outputTemplate = File(outputDir, "%(title).80s [%(id)s].%(ext)s").absolutePath

        downloader.execute {
            try {
                synchronized(YTDLP_LOCK) {
                    if (!ytDlpReady) {
                        YtDlp.init(applicationContext)
                        ytDlpReady = true
                    }
                }
                val request = YtDlpRequest(link)
                    .setOutputTemplate(outputTemplate)
                    .addOption("--no-playlist")
                    .addOption("-f", "best[ext=mp4]/best")
                InstagramSession.cookieFile(this)?.takeIf { it.isFile && it.length() > 0 }?.let {
                    request.addOption("--cookies", it.absolutePath)
                }
                val future = YtDlp.executeAsync(request) { progress, eta, _ ->
                    runOnUiThread {
                        downloadStatus.text = "Téléchargement… ${progress.toInt()} % (env. ${eta}s)"
                    }
                }
                val response = future.get()
                if (!response.isSuccess()) {
                    throw IllegalStateException(response.errorOutput.ifBlank { "yt-dlp a renvoyé le code ${response.exitCode}." })
                }
                val file = outputDir.listFiles()
                    ?.filter { it.isFile && it.lastModified() >= startAt && it.length() > 0 }
                    ?.maxByOrNull { it.lastModified() }
                    ?: throw IllegalStateException("Téléchargement terminé, mais le fichier vidéo est introuvable.")
                runOnUiThread {
                    downloadStatus.text = "Vidéo prête : ${file.name}"
                    if (saveLocally) render()
                    shareVideo(file)
                }
            } catch (e: Exception) {
                val details = e.cause?.message ?: e.message ?: "Erreur inconnue"
                val needsLogin = allowLoginPrompt && isInstagramLink(link) &&
                    listOf("login required", "rate-limit reached", "requested content is not available", "cookies-from-browser")
                        .any { details.contains(it, ignoreCase = true) }
                runOnUiThread {
                    if (needsLogin) {
                        downloadStatus.text = "Instagram demande une connexion. Connecte ton compte pour reprendre le téléchargement."
                        pendingLinkForLogin = link
                        startActivityForResult(Intent(this, InstagramLoginActivity::class.java), REQUEST_INSTAGRAM_LOGIN)
                    } else {
                        downloadStatus.text = "Échec du téléchargement : ${details.lineSequence().first()}"
                        Toast.makeText(this, "Impossible de récupérer cette vidéo.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private var pendingLinkForLogin: String? = null

    @Deprecated("Use Activity Result APIs")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_INSTAGRAM_LOGIN) return
        val link = pendingLinkForLogin
        pendingLinkForLogin = null
        if (resultCode == RESULT_OK && link != null) startDownload(link, allowLoginPrompt = false)
        else if (resultCode != RESULT_OK && link != null) downloadStatus.text = "Connexion annulée. Le téléchargement n’a pas repris."
        else if (resultCode == RESULT_OK) render()
    }

    private fun isInstagramLink(link: String) =
        runCatching { android.net.Uri.parse(link).host?.endsWith("instagram.com", ignoreCase = true) == true }.getOrDefault(false)

    private fun shareVideo(file: File) {
        val uri = androidx.core.content.FileProvider.getUriForFile(this, "$packageName.files", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "video/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(send, "Partager la vidéo"))
    }

    override fun onDestroy() {
        super.onDestroy()
        downloader.shutdown()
    }

    companion object {
        private const val REQUEST_INSTAGRAM_LOGIN = 4102
        private val YTDLP_LOCK = Any()
        @Volatile private var ytDlpReady = false
    }
}
