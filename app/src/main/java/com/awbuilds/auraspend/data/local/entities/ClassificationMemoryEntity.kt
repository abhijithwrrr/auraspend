package com.awbuilds.auraspend.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Learned classification memory: maps a normalized merchant / note key to the category (and
 * optionally type) it was last saved with.
 *
 * This is the fast path of the AI pipeline - when an incoming bank message matches a key the user
 * (or a confirmed auto-save) has seen before, the category is applied instantly and the LLM call is
 * skipped entirely. Corrections always win: every manual save updates the entry for its key.
 */
@Entity(
    tableName = "classification_memory",
    indices = [Index(value = ["memoryKey"], unique = true)]
)
data class ClassificationMemoryEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    /** Normalized merchant or note key (see [com.awbuilds.auraspend.data.classification.MemoryKeys]). */
    val memoryKey: String,
    val categoryId: String,
    /** Optional remembered polarity ("INCOME" / "EXPENSE"), null when unknown. */
    val type: String? = null,
    /** Where the mapping came from: [SOURCE_USER_SAVE] or [SOURCE_AUTO_SAVE]. */
    val source: String,
    /** How many times this mapping has been learned/confirmed. Higher = more trustworthy. */
    val hits: Int = 1,
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val SOURCE_USER_SAVE = "user_save"
        const val SOURCE_AUTO_SAVE = "auto_save"
    }
}
