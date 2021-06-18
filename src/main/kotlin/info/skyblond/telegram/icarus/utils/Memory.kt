package info.skyblond.telegram.icarus.utils

import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * This class acts like a memory. It will record every key's create date
 * in UTC timezone, and the key is only validate in that day.
 * After that day, the key will be unavailable.
 *
 * The remove of expired key only occurred in get.
 * */
class Memory<K, V>(
    private val todayProvider: () -> LocalDate = { ZonedDateTime.now(ZoneOffset.UTC).toLocalDate() }
) : MutableMap<K, V> {
    private val internalStorageMap = mutableMapOf<K, V>()
    private val internalDateMap = mutableMapOf<K, LocalDate>()

    private fun getUtcToday(): LocalDate {
        return todayProvider()
    }

    private fun isKeyExpired(key: K): Boolean =
        internalDateMap[key]?.let { it != getUtcToday() } ?: false

    override val size: Int
        get() {
            // this act as a cache
            val today = getUtcToday()
            return internalDateMap.count { it.value == today }
        }

    override fun containsKey(key: K): Boolean =
        if (internalDateMap.containsKey(key)) {
            // key present and not expired
            !isKeyExpired(key)
        } else {
            false
        }

    override fun containsValue(value: V): Boolean {
        throw UnsupportedOperationException("Query based on value is not supported")
    }

    override fun get(key: K): V? =
        if (isKeyExpired(key)) {
            // remove expired key and return null
            remove(key)
            null
        } else {
            // get the value, if key is not present, then null is get and returned
            internalStorageMap[key]
        }

    override fun isEmpty(): Boolean = this.size == 0

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() {
            // this act as a cache
            val today = getUtcToday()
            return mutableSetOf<MutableMap.MutableEntry<K, V>>().apply {
                this.addAll(internalStorageMap.entries.filter { internalDateMap[it.key] == today })
            }
        }

    override val keys: MutableSet<K>
        get() {
            // this act as a cache
            val today = getUtcToday()
            return mutableSetOf<K>().apply {
                this.addAll(internalStorageMap.keys.filter { internalDateMap[it] == today })
            }
        }

    override val values: MutableCollection<V>
        get() {
            // this act as a cache
            val today = getUtcToday()
            return mutableSetOf<V>().apply {
                this.addAll(internalStorageMap.mapNotNull {
                    if (internalDateMap[it.key] == today)
                        it.value
                    else
                        null
                })
            }
        }

    override fun clear() {
        internalStorageMap.clear()
        internalDateMap.clear()
    }

    @Synchronized
    override fun put(key: K, value: V): V? {
        val oldValue = if (isKeyExpired(key)) null else internalStorageMap[key]
        internalStorageMap[key] = value
        internalDateMap[key] = getUtcToday()
        return oldValue
    }

    @Synchronized
    override fun putAll(from: Map<out K, V>) {
        // cache
        val today = getUtcToday()
        from.forEach { (k, v) ->
            internalStorageMap[k] = v
            internalDateMap[k] = today
        }
    }

    @Synchronized
    override fun remove(key: K): V? {
        val isExpired = isKeyExpired(key)
        val oldValue = internalStorageMap.remove(key)
        internalDateMap.remove(key)
        return if (isExpired) null else oldValue
    }
}