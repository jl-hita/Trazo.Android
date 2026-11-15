package com.trazo.android.data.local

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.trazo.android.data.model.LogSessionRequest
import com.trazo.android.data.model.PlanView
import com.trazo.android.data.model.WeekView
import java.io.File
import java.time.LocalDate

/**
 * Tiny JSON-on-disk store. Writes go through a temp file so a crash never leaves a half-written file.
 *
 * With a [cipher] every file is stored encrypted, behind a short marker so the format can be told apart. A plain JSON
 * file written by an earlier version is still read and is encrypted the next time it is written. A file that cannot be
 * decrypted (key lost, data damaged) is deleted and read as missing, so it can never block the app.
 */
class JsonFileStore(private val dir: File, private val gson: Gson = Gson(), private val cipher: LocalCipher? = null) {
    @Synchronized
    fun <T> read(name: String, type: java.lang.reflect.Type): T? {
        val file = File(dir, name)
        if (!file.exists()) return null
        val json = runCatching { decode(file.readBytes()) }.getOrNull()
        if (json == null) { file.delete(); return null }
        return runCatching { gson.fromJson<T>(json, type) }.getOrNull()
    }

    @Synchronized
    fun write(name: String, value: Any) {
        dir.mkdirs()
        val tmp = File(dir, "$name.tmp")
        tmp.writeBytes(encode(gson.toJson(value)))
        if (!tmp.renameTo(File(dir, name))) { File(dir, name).delete(); tmp.renameTo(File(dir, name)) }
    }

    private fun encode(json: String): ByteArray {
        val bytes = json.toByteArray(Charsets.UTF_8)
        return if (cipher == null) bytes else MAGIC + cipher.encrypt(bytes)
    }

    private fun decode(bytes: ByteArray): String? {
        val encrypted = bytes.size > MAGIC.size && bytes.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)
        return when {
            !encrypted -> bytes.toString(Charsets.UTF_8)
            cipher == null -> null
            else -> cipher.decrypt(bytes.copyOfRange(MAGIC.size, bytes.size)).toString(Charsets.UTF_8)
        }
    }

    private companion object {
        /** Not valid at the start of a JSON document, so it never clashes with a legacy plain file. */
        val MAGIC = byteArrayOf(0x54, 0x5A, 0x45, 0x01)
    }

    @Synchronized
    fun delete(name: String) { File(dir, name).delete() }

    @Synchronized
    fun clear() { dir.listFiles()?.forEach { it.delete() } }
}

class CachedWeek(val savedAtMillis: Long, val week: WeekView)
class CachedPlan(val savedAtMillis: Long, val plan: PlanView)

/**
 * Short-lived copy of the current week and plan summary, only used when the network fails. The backend serves the current
 * week alone on purpose; this cache never holds more than that and expires after [ttlMillis] or when the week ends.
 */
class PlanCache(
    private val store: JsonFileStore,
    private val now: () -> Long = System::currentTimeMillis,
    private val today: () -> LocalDate = LocalDate::now,
    private val ttlMillis: Long = DEFAULT_TTL_MILLIS
) {
    fun saveWeek(week: WeekView) = store.write(WEEK, CachedWeek(now(), week))
    fun savePlan(plan: PlanView) = store.write(PLAN, CachedPlan(now(), plan))

    fun week(): WeekView? {
        val cached = store.read<CachedWeek>(WEEK, CachedWeek::class.java) ?: return null
        val end = runCatching { LocalDate.parse(cached.week.weekEnd) }.getOrNull()
        val fresh = now() - cached.savedAtMillis in 0..ttlMillis && (end == null || !today().isAfter(end))
        if (!fresh) store.delete(WEEK)
        return cached.week.takeIf { fresh }
    }

    fun plan(): PlanView? {
        val cached = store.read<CachedPlan>(PLAN, CachedPlan::class.java) ?: return null
        val fresh = now() - cached.savedAtMillis in 0..ttlMillis
        if (!fresh) store.delete(PLAN)
        return cached.plan.takeIf { fresh }
    }

    fun clear() = store.clear()

    companion object {
        const val DEFAULT_TTL_MILLIS = 6L * 60 * 60 * 1000
        private const val WEEK = "week.json"
        private const val PLAN = "plan.json"
    }
}

/** A session log that could not be delivered yet. It is sent again when the connection is back. */
class PendingLog(val sessionId: String, val request: LogSessionRequest, val createdAtMillis: Long)

class PendingLogQueue(private val store: JsonFileStore) {
    private val type = object : TypeToken<List<PendingLog>>() {}.type

    @Synchronized
    fun all(): List<PendingLog> = store.read<List<PendingLog>>(FILE, type).orEmpty()

    @Synchronized
    fun add(item: PendingLog) {
        // One log per session: the latest replaces an earlier one that was never delivered.
        store.write(FILE, all().filterNot { it.sessionId == item.sessionId } + item)
    }

    @Synchronized
    fun remove(sessionId: String) { store.write(FILE, all().filterNot { it.sessionId == sessionId }) }

    @Synchronized
    fun clear() = store.delete(FILE)

    private companion object { const val FILE = "pending_logs.json" }
}

class DraftSet(val exerciseId: String, val setNumber: Int, val weight: String, val reps: String, val rir: String, val done: Boolean)
class WorkoutDraft(val sessionId: String, val sets: List<DraftSet>, val savedAtMillis: Long)

/** The sets typed so far in a workout, so an interrupted session (app killed, call received) is not lost. */
class WorkoutDraftStore(private val store: JsonFileStore, private val now: () -> Long = System::currentTimeMillis) {
    fun save(sessionId: String, sets: List<DraftSet>) = store.write(name(sessionId), WorkoutDraft(sessionId, sets, now()))
    fun load(sessionId: String): WorkoutDraft? = store.read(name(sessionId), WorkoutDraft::class.java)
    fun delete(sessionId: String) = store.delete(name(sessionId))
    fun clear() = store.clear()
    private fun name(sessionId: String) = "draft_${sessionId.filter { it.isLetterOrDigit() || it == '-' }}.json"
}
