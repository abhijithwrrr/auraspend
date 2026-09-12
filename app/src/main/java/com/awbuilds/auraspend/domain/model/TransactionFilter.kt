package com.awbuilds.auraspend.domain.model

/**
 * Filter for the paged Activity list. `null` type/category mean "no
 * constraint" and a blank [query] matches everything, which mirrors the SQL
 * predicate used by Room's `PagingSource`.
 *
 * [matches] keeps the same semantics for in-memory repository fakes.
 */
data class TransactionFilter(
    val query: String = "",
    val type: TransactionType? = null,
    val categoryId: String? = null
) {
    /** True when any constraint is active (drives the filtered empty state). */
    val isActive: Boolean
        get() = query.isNotBlank() || type != null || categoryId != null

    fun matches(transaction: Transaction): Boolean =
        (type == null || transaction.type == type) &&
            (categoryId == null || transaction.categoryId == categoryId) &&
            (
                query.isBlank() ||
                    transaction.note.contains(query, ignoreCase = true) ||
                    (transaction.merchant?.contains(query, ignoreCase = true) == true)
                )
}
