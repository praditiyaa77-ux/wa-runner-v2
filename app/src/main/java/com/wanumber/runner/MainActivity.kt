package com.wanumber.runner

import android.app.Activity
import android.os.Bundle
import android.content.*
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject

data class QueueItem(
    val number: String,
    var status: String = "Waiting",
    var note: String = ""
)

class MainActivity : Activity() {
    private val bg = Color.rgb(10, 16, 17)
    private val panel = Color.rgb(20, 30, 29)
    private val green = Color.rgb(34, 197, 140)
    private val white = Color.rgb(235, 245, 241)
    private val muted = Color.rgb(155, 174, 166)

    private val items = mutableListOf<QueueItem>()
    private lateinit var listBox: LinearLayout
    private lateinit var stats: TextView
    private lateinit var input: EditText
    private lateinit var appChoice: Spinner
    private lateinit var currentLabel: TextView
    private var currentIndex = -1
    private var running = false
    private val prefs by lazy { getSharedPreferences("wa_runner", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadQueue()
        buildUi()
        renderQueue()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 22, 18, 18)
            setBackgroundColor(bg)
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content)

        content.addView(text("WA NUMBER RUNNER", 23f, white, true))
        content.addView(text("V2 • GUIDED QUEUE", 12f, green, true).apply {
            setPadding(0, 4, 0, 18)
        })

        val selectorTitle = text("Aplikasi tujuan", 14f, muted, true)
        content.addView(selectorTitle)
        appChoice = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                listOf("WhatsApp", "WhatsApp Business")
            )
        }
        content.addView(appChoice, lp(height = 48, bottom = 12))

        content.addView(text("Tambah nomor (satu nomor per baris)", 14f, muted, true))
        input = EditText(this).apply {
            hint = "+628123456789"
            setHintTextColor(muted)
            setTextColor(white)
            textSize = 14f
            gravity = Gravity.TOP
            minLines = 3
            maxLines = 6
            setPadding(12, 12, 12, 12)
            setBackgroundColor(panel)
        }
        content.addView(input, lp(height = -2, bottom = 8))

        val addRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        addRow.addView(button("TAMBAH", green) { addNumbers(input.text.toString()) }, LinearLayout.LayoutParams(0, 48, 1f))
        addRow.addView(button("IMPORT TXT", Color.rgb(48, 75, 67)) { importTxt() },
            LinearLayout.LayoutParams(0, 48, 1f).apply { marginStart = 8 })
        content.addView(addRow, lp(bottom = 16))

        stats = text("", 13f, green, true)
        content.addView(stats, lp(bottom = 12))
        currentLabel = text("Belum ada nomor aktif", 14f, white, true)
        content.addView(card(currentLabel), lp(bottom = 10))

        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        controls.addView(button("RUN", green) { runCurrent() }, LinearLayout.LayoutParams(0, 48, 1f))
        controls.addView(button("PAUSE", Color.rgb(94, 110, 102)) { pauseQueue() },
            LinearLayout.LayoutParams(0, 48, 1f).apply { marginStart = 7 })
        controls.addView(button("STOP", Color.rgb(130, 55, 55)) { stopQueue() },
            LinearLayout.LayoutParams(0, 48, 1f).apply { marginStart = 7 })
        content.addView(controls, lp(bottom = 12))

        content.addView(text("Konfirmasi hasil nomor aktif", 14f, muted, true), lp(bottom = 8))
        val resultRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        resultRow.addView(button("BERHASIL", green) { markCurrent("Success") },
            LinearLayout.LayoutParams(0, 46, 1f))
        resultRow.addView(button("GAGAL", Color.rgb(160, 86, 65)) { markCurrent("Failed") },
            LinearLayout.LayoutParams(0, 46, 1f).apply { marginStart = 6 })
        resultRow.addView(button("LIMIT", Color.rgb(178, 126, 48)) { markLimited() },
            LinearLayout.LayoutParams(0, 46, 1f).apply { marginStart = 6 })
        content.addView(resultRow, lp(bottom = 16))

        content.addView(text("ANTREAN NOMOR", 15f, white, true), lp(bottom = 8))
        listBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(listBox)

        val foot = text(
            "Guided mode: aplikasi hanya menyalin nomor dan membuka aplikasi pilihan. " +
            "Login, OTP, dan konfirmasi tetap dilakukan sendiri. Jika terdeteksi limit/blokir, antrean dijeda.",
            11f, muted, false
        )
        foot.setPadding(0, 18, 0, 8)
        content.addView(foot)

        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun text(value: String, size: Float, color: Int, bold: Boolean): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
        }

    private fun card(child: View): LinearLayout = LinearLayout(this).apply {
        setPadding(13, 13, 13, 13)
        setBackgroundColor(panel)
        addView(child)
    }

    private fun lp(height: Int = -2, bottom: Int = 0) =
        LinearLayout.LayoutParams(-1, height).apply { bottomMargin = bottom }

    private fun button(label: String, color: Int, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            textSize = 11f
            setTextColor(Color.WHITE)
            setBackgroundColor(color)
            isAllCaps = false
            setOnClickListener { action() }
        }

    private fun addNumbers(raw: String) {
        val parsed = raw.split(Regex("[\\n,;\\t ]+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
        var added = 0
        parsed.forEach { number ->
            val normalized = number.filter { it.isDigit() || it == '+' }
            if (normalized.length >= 8 && items.none { it.number == normalized }) {
                items.add(QueueItem(normalized))
                added++
            }
        }
        input.setText("")
        saveQueue()
        renderQueue()
        toast("$added nomor ditambahkan")
    }

    private fun importTxt() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
        }
        startActivityForResult(intent, 41)
    }

    @Deprecated("Deprecated Android callback retained for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 41 && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                val content = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
                addNumbers(content)
            }
        }
    }

    private fun runCurrent() {
        val next = if (currentIndex in items.indices && items[currentIndex].status == "Processing") {
            currentIndex
        } else {
            items.indexOfFirst { it.status == "Waiting" }
        }
        if (next < 0) {
            toast("Tidak ada nomor yang menunggu")
            return
        }
        currentIndex = next
        running = true
        items[next].status = "Processing"
        val number = items[next].number
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Nomor WhatsApp", number))
        saveQueue()
        renderQueue()
        val packageName = if (appChoice.selectedItemPosition == 1) "com.whatsapp.w4b" else "com.whatsapp"
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        if (launch != null) {
            startActivity(launch)
            toast("Nomor disalin. Selesaikan login secara manual.")
        } else {
            items[next].status = "Waiting"
            running = false
            saveQueue()
            renderQueue()
            toast("Aplikasi tujuan belum terpasang")
        }
    }

    private fun markCurrent(status: String) {
        if (currentIndex !in items.indices) {
            toast("Belum ada nomor aktif")
            return
        }
        items[currentIndex].status = status
        items[currentIndex].note = "Dikonfirmasi pengguna"
        currentIndex = -1
        running = false
        saveQueue()
        renderQueue()
        toast("Status disimpan: $status")
    }

    private fun markLimited() {
        if (currentIndex !in items.indices) {
            toast("Belum ada nomor aktif")
            return
        }
        items[currentIndex].status = "Limited"
        items[currentIndex].note = "Pembatasan dilaporkan pengguna"
        currentIndex = -1
        running = false
        saveQueue()
        renderQueue()
        toast("Antrean dijeda karena limit. Periksa akun secara manual.")
    }

    private fun pauseQueue() {
        running = false
        if (currentIndex in items.indices && items[currentIndex].status == "Processing") {
            items[currentIndex].status = "Waiting"
        }
        currentIndex = -1
        saveQueue()
        renderQueue()
        toast("Antrean dijeda")
    }

    private fun stopQueue() {
        running = false
        if (currentIndex in items.indices && items[currentIndex].status == "Processing") {
            items[currentIndex].status = "Waiting"
        }
        currentIndex = -1
        saveQueue()
        renderQueue()
        toast("Antrean dihentikan")
    }

    private fun renderQueue() {
        if (!::listBox.isInitialized) return
        listBox.removeAllViews()
        val success = items.count { it.status == "Success" }
        val failed = items.count { it.status == "Failed" }
        val waiting = items.count { it.status == "Waiting" }
        val limited = items.count { it.status == "Limited" }
        stats.text = "TOTAL ${items.size}   •   MENUNGGU $waiting   •   BERHASIL $success   •   GAGAL $failed   •   LIMIT $limited"
        currentLabel.text = if (currentIndex in items.indices) {
            "Nomor aktif: ${items[currentIndex].number}"
        } else "Belum ada nomor aktif"
        items.forEachIndexed { index, item ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(12, 10, 12, 10)
                setBackgroundColor(panel)
            }
            val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val title = text("${index + 1}. ${item.number}", 14f, white, true)
            line.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
            line.addView(text(item.status.uppercase(), 11f, statusColor(item.status), true))
            row.addView(line)
            if (item.note.isNotBlank()) row.addView(text(item.note, 10f, muted, false))
            listBox.addView(row, lp(bottom = 5))
        }
        if (items.isEmpty()) listBox.addView(text("Belum ada nomor. Tambahkan manual atau import TXT.", 12f, muted, false))
    }

    private fun statusColor(status: String): Int = when (status) {
        "Success" -> green
        "Failed" -> Color.rgb(245, 116, 91)
        "Limited" -> Color.rgb(242, 184, 75)
        "Processing" -> Color.rgb(95, 178, 255)
        else -> muted
    }

    private fun saveQueue() {
        val array = JSONArray()
        items.forEach {
            array.put(JSONObject().put("number", it.number).put("status", it.status).put("note", it.note))
        }
        prefs.edit().putString("queue", array.toString()).apply()
    }

    private fun loadQueue() {
        val raw = prefs.getString("queue", "[]") ?: "[]"
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                items.add(QueueItem(
                    obj.optString("number"),
                    obj.optString("status", "Waiting"),
                    obj.optString("note")
                ))
            }
        } catch (_: Exception) {
            items.clear()
        }
    }

    private fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
