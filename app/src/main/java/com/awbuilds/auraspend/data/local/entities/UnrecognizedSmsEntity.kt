package com.awbuilds.auraspend.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A bank SMS the app could not turn into a transaction.
 *
 * Every other failure path in the pipeline discards the message silently, and
 * that is the right call for an OTP or a promotion. It is the wrong call for a
 * message from a bank we have no parser for that turns out to carry a real
 * debit: the user sees a transaction missing from their history with no way to
 * find out why, and no way to fix it.
 *
 * This table makes that visible. The user can see what was not understood, and
 * file it by hand.
 *
 * Only the raw message is stored — sender, body, timestamp. Nothing derived
 * (no amount, no merchant, no category), because nothing was reliably derived
 * and a half-guessed amount shown as fact is worse than no amount.
 */
@Entity(tableName = "unrecognized_sms")
data class UnrecognizedSmsEntity(
    @PrimaryKey val id: String,
    /** Raw SMS address, e.g. `AXISBK` or `VI-HDFCBK-S`. May be blank. */
    val sender: String,
    /** The message exactly as received. */
    val body: String,
    /** Epoch millis the SMS was received on the device. */
    val receivedAt: Long,
    /** Epoch millis the row was created, used for the retention sweep. */
    val createdAt: Long
)
