package com.trazo.android.data.local

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.trazo.android.data.model.LogSessionRequest
import com.trazo.android.data.model.withoutHealthData
import com.trazo.android.data.model.PlanView
import com.trazo.android.data.model.WeekView
import com.trazo.android.data.net.ApiError
import com.trazo.android.data.net.ErrorKind
import com.trazo.android.diagnostics.Diagnostics
import java.io.File
import java.time.LocalDate

/**
 * Tiny JSON-on-disk store. Writes go through a temp file so a crash never leaves a half-written file.
 *
 * With a [cipher] every file is stored encrypted, behind a short marker so the format can be told apart. A plain JSON
 * file written by an earlier version is still read and is encrypted the next time it is written. A file that cannot be
 * decrypted (key lost, data damaged) is deleted and read as missing, so it can never block the app. With [keepUnreadable]
 * (data that exists nowhere else) it is never deleted: it is read as missing, and a later write moves it aside first.
 */
class JsonFileStore(
    private val dir: File, private val gson: Gson = Gson(), private val cipher: LocalCipher? = null,
    private val keepUnreadable: Boolean = false
) {
    @Synchronized
    fun <T> read(name: String, type: java.lang.reflect.Type): T? {
        val file = File(dir, name)
        if (!file.exists()) return null
        val decoded = runCatching { decode(file.readBytes()) }
        val json = decoded.getOrNull()
        if (json == null) {
            if (!keepUnreadable) file.delete() else decoded.exceptionOrNull()?.let { Diagnostics.report(it) }
            return null
        }
        return runCatching { gson.fromJson<T>(json, type) }.getOrNull()
    }

    /** The earlier versions of [name] that were moved aside because they could not be read. */
    @Synchronized
    fun setAside(name: String): List<String> =
        dir.list()?.filter { it.startsWith("$name$ASIDE") }?.sorted().orEmpty()

    private fun isReadable(file: File): Boolean =
        runCatching { JsonParser.parseString(decode(file.readBytes())!!) }.isSuccess

    @Synchronized
    fun write(name: String, value: Any) {
        dir.mkdirs()
        val tmp = File(dir, "$name.tmp")
        tmp.writeBytes(encode(gson.toJson(value)))
        val current = File(dir, name)
        if (keepUnreadable && current.exists() && !isReadable(current)) {
            check(current.renameTo(File(dir, "$name$ASIDE${System.nanoTime()}"))) { "Cannot keep the unreadable $name" }
        }
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
        const val ASIDE = ".unreadable-"

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
        // A sequence plan has no calendar week: its copy only expires with the time limit.
        val end = cached.week.weekEnd?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
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

/**
 * A session log the server has not accepted yet. Without a failure it waits to be sent; with one the server refused it and
 * it stays until the user decides what to do. The failure is kept as plain fields (this class is stored as JSON).
 */
class PendingLog(
    val sessionId: String, val request: LogSessionRequest, val createdAtMillis: Long,
    val failureKind: String? = null, val failureCode: String? = null, val failureMessage: String? = null
) {
    val isRejected: Boolean get() = failureKind != null

    val failure: ApiError? get() = failureKind?.let { kind ->
        ApiError(ErrorKind.entries.firstOrNull { it.name == kind } ?: ErrorKind.Unknown, failureCode, failureMessage)
    }

    fun rejected(error: ApiError) = PendingLog(sessionId, request, createdAtMillis, error.kind.name, error.code, error.serverMessage)

    fun waiting() = PendingLog(sessionId, request, createdAtMillis)

    /** The same workout without its pain, discomfort and illness marks, for someone who withdrew the consent. */
    fun withoutHealthData() = PendingLog(sessionId, request.withoutHealthData(), createdAtMillis, failureKind, failureCode, failureMessage)

    /** True for the same finished workout, whatever happened to it since. */
    fun sameAs(other: PendingLog) = sessionId == other.sessionId && createdAtMillis == other.createdAtMillis
}

class PendingLogQueue(private val store: JsonFileStore) {
    private val type = object : TypeToken<List<PendingLog>>() {}.type

    @Synchronized
    fun all(): List<PendingLog> {
        val current = read(FILE)
        // A file that could not be read before (a passing Keystore failure) is merged back as soon as it can be.
        val recovered = store.setAside(FILE).mapNotNull { name -> read(name)?.let { name to it } }
        if (recovered.isEmpty()) return current.orEmpty()
        // One log per session: what is in the current file is newer than what was set aside.
        val merged = (recovered.flatMap { it.second } + current.orEmpty()).associateBy { it.sessionId }.values.toList()
        store.write(FILE, merged)
        recovered.forEach { store.delete(it.first) }
        return merged
    }

    private fun read(name: String): List<PendingLog>? = store.read<List<PendingLog>>(name, type)

    @Synchronized
    fun add(item: PendingLog) {
        // One log per session: the latest replaces an earlier one that was never delivered.
        store.write(FILE, all().filterNot { it.sessionId == item.sessionId } + item)
    }

    /** Removes that workout only: a newer log of the same session stays. */
    @Synchronized
    fun remove(item: PendingLog) {
        val entries = all()
        if (entries.any { it.sameAs(item) }) store.write(FILE, entries.filterNot { it.sameAs(item) })
    }

    /** Stores the new state of a queued workout; nothing happens when it is no longer there. */
    @Synchronized
    fun replace(item: PendingLog) {
        val entries = all()
        if (entries.any { it.sameAs(item) }) store.write(FILE, entries.map { if (it.sameAs(item)) item else it })
    }

    @Synchronized
    fun clear() {
        store.setAside(FILE).forEach { store.delete(it) }
        store.delete(FILE)
    }

    private companion object { const val FILE = "pending_logs.json" }
}

class DraftSet(
    val exerciseId: String, val setNumber: Int, val weight: String, val reps: String, val rir: String, val done: Boolean,
    /** 0 for the main set (and for drafts written before segments existed). */
    val segment: Int = 0
)
class WorkoutDraft(val sessionId: String, val sets: List<DraftSet>, val savedAtMillis: Long)

/** The sets typed so far in a workout, so an interrupted session (app killed, call received) is not lost. */
class WorkoutDraftStore(private val store: JsonFileStore, private val now: () -> Long = System::currentTimeMillis) {
    fun save(sessionId: String, sets: List<DraftSet>) = store.write(name(sessionId), WorkoutDraft(sessionId, sets, now()))
    /** A draft written by a build whose fields were renamed (or that is damaged) is dropped instead of crashing the session. */
    fun load(sessionId: String): WorkoutDraft? {
        val draft = store.read<WorkoutDraft>(name(sessionId), WorkoutDraft::class.java) ?: return null
        val valid = runCatching {
            draft.sets.all { (it as Any?) is DraftSet && (it.exerciseId as String?) != null && (it.weight as String?) != null && (it.reps as String?) != null && (it.rir as String?) != null }
        }.getOrDefault(false)
        if (!valid) { delete(sessionId); return null }
        return draft
    }
    fun delete(sessionId: String) = store.delete(name(sessionId))
    fun clear() = store.clear()
    private fun name(sessionId: String) = "draft_${sessionId.filter { it.isLetterOrDigit() || it == '-' }}.json"
}
