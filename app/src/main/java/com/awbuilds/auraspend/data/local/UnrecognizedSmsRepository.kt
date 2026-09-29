package com.awbuilds.auraspend.data.local

import com.awbuilds.auraspend.core.AuraLog
import com.awbuilds.auraspend.core.boundary
import com.awbuilds.auraspend.core.boundaryOrNull
import com.awbuilds.auraspend.data.local.dao.UnrecognizedSmsDao
import com.awbuilds.auraspend.data.local.entities.UnrecognizedSmsEntity
import kotlinx.coroutines.flow.Flow

/**
 * Bank messages the app could not turn into a transaction.
 *
 * The point of this table is that a *missing* transaction stays visible.
 * Silently dropping a message that turned out to be a real debit leaves the
 * user with a history that is quietly wrong and no way to discover why. Keeping
 * the raw message means it can be inspected and filed by hand.
 */
class UnrecognizedSmsRepository(private val dao: UnrecognizedSmsDao) {

    fun observeAll(): Flow<List<UnrecognizedSmsEntity>> = dao.observeAll()

    fun observeCount(): Flow<Int> = dao.observeCount()

    /**
     * Records [id] as unreadable. Idempotent — re-processing the same message
     * replaces the row rather than duplicating it.
     */
    suspend fun record(id: String, sender: String, body: String, receivedAt: Long) {
        boundary(TAG, Unit) {
            dao.insertAll(
                listOf(
                    UnrecognizedSmsEntity(
                        id = id,
                        sender = sender,
                        body = body,
                        receivedAt = receivedAt,
                        createdAt = System.currentTimeMillis()
                    )
                )
            )
        }
    }

    suspend fun delete(id: String) {
        boundary(TAG, Unit) { dao.delete(id) }
    }

    /**
     * Drops entries older than [days], so the list stays a recent-questions list
     * rather than an unbounded archive of SMS the user chose not to file.
     */
    suspend fun purgeOlderThan(days: Int = RETENTION_DAYS) {
        boundary(TAG, Unit) {
            dao.purgeOlderThan(System.currentTimeMillis() - days * 24L * 60 * 60 * 1000)
        }
    }

    /** For the atomic Drive restore. */
    suspend fun all(): List<UnrecognizedSmsEntity> =
        boundaryOrNull(TAG) { dao.getAll() } ?: emptyList()

    /** Used only by the atomic Drive restore: clear, then re-insert. */
    suspend fun replaceAll(entries: List<UnrecognizedSmsEntity>) {
        boundary(TAG, Unit) {
            dao.clear()
            if (entries.isNotEmpty()) dao.insertAll(entries)
        }
    }

    private companion object {
        const val TAG = "UnrecognizedSms"
        const val RETENTION_DAYS = 30
    }
}
