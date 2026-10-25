package com.spendwise.app.data.service

import com.spendwise.app.data.local.dao.RecurringTransactionDao
import com.spendwise.app.data.local.dao.TransactionDao
import com.spendwise.app.data.local.entity.RecurringTransactionEntity
import com.spendwise.app.data.local.entity.TransactionEntity
import com.spendwise.app.model.RecurrenceFrequency
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class RecurringTransactionService(
    private val transactionDao: TransactionDao,
    private val recurringTransactionDao: RecurringTransactionDao
) {

    // Processing runs on every app start/resume; the lock stops two overlapping runs
    // from generating the same occurrence twice.
    private val processingLock = Mutex()

    suspend fun processRecurringTransactions() = processingLock.withLock {
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val dueRules = recurringTransactionDao.getDueRecurringTransactions(now)

            for (rule in dueRules) {
                var currentOccurrence = rule.nextOccurrence
                var active = rule.isActive
                val generated = mutableListOf<TransactionEntity>()

                while (active && currentOccurrence <= now) {
                    if (rule.endDate != null && currentOccurrence > rule.endDate) {
                        active = false
                        break
                    }
                    generated += TransactionEntity(
                        title = rule.title,
                        amount = rule.amount,
                        type = rule.type,
                        categoryId = rule.categoryId,
                        accountId = rule.accountId,
                        dateTimestamp = currentOccurrence,
                        note = rule.note,
                        recurringRuleId = rule.id
                    )
                    currentOccurrence = occurrenceAfter(rule, currentOccurrence)
                }

                if (rule.endDate != null && currentOccurrence > rule.endDate) {
                    active = false
                }

                if (generated.isNotEmpty()) {
                    transactionDao.insertTransactions(generated)
                }
                recurringTransactionDao.updateNextOccurrence(rule.id, currentOccurrence)
                if (active != rule.isActive) {
                    recurringTransactionDao.setActive(rule.id, active)
                }
            }
        }
    }

    /**
     * The first occurrence of [rule] that falls on a later calendar day than [afterMillis].
     *
     * Occurrences are counted from the rule's start date rather than from the previous
     * occurrence, so a rule on the 31st stays on the last day of each month instead of
     * drifting to the 28th after February, and keeps the start date's time of day.
     */
    fun occurrenceAfter(rule: RecurringTransactionEntity, afterMillis: Long): Long {
        val zone = ZoneId.systemDefault()
        val anchor = Instant.ofEpochMilli(rule.startDate).atZone(zone)
        val after = Instant.ofEpochMilli(afterMillis).atZone(zone)
        val afterDate = after.toLocalDate()

        if (anchor.toLocalDate().isAfter(afterDate)) return rule.startDate

        var n = estimatePeriods(rule.frequency, anchor, after)
        while (n > 0 && occurrence(rule.frequency, anchor, n).toLocalDate().isAfter(afterDate)) n--
        while (!occurrence(rule.frequency, anchor, n).toLocalDate().isAfter(afterDate)) n++
        return occurrence(rule.frequency, anchor, n).toInstant().toEpochMilli()
    }

    /** First occurrence at or after [fromMillis]; used when a rule's schedule is edited. */
    fun firstOccurrenceFrom(rule: RecurringTransactionEntity, fromMillis: Long): Long {
        if (rule.startDate >= fromMillis) return rule.startDate
        val dayBefore = Instant.ofEpochMilli(fromMillis).atZone(ZoneId.systemDefault()).minusDays(1)
        return occurrenceAfter(rule, dayBefore.toInstant().toEpochMilli())
    }

    private fun estimatePeriods(frequency: RecurrenceFrequency, anchor: ZonedDateTime, after: ZonedDateTime): Long {
        val periods = when (frequency) {
            RecurrenceFrequency.DAILY -> ChronoUnit.DAYS.between(anchor, after)
            RecurrenceFrequency.WEEKLY -> ChronoUnit.WEEKS.between(anchor, after)
            RecurrenceFrequency.BIWEEKLY -> ChronoUnit.WEEKS.between(anchor, after) / 2
            RecurrenceFrequency.MONTHLY -> ChronoUnit.MONTHS.between(anchor, after)
            RecurrenceFrequency.YEARLY -> ChronoUnit.YEARS.between(anchor, after)
        }
        return periods.coerceAtLeast(0)
    }

    private fun occurrence(frequency: RecurrenceFrequency, anchor: ZonedDateTime, n: Long): ZonedDateTime =
        when (frequency) {
            RecurrenceFrequency.DAILY -> anchor.plusDays(n)
            RecurrenceFrequency.WEEKLY -> anchor.plusWeeks(n)
            RecurrenceFrequency.BIWEEKLY -> anchor.plusWeeks(2 * n)
            RecurrenceFrequency.MONTHLY -> anchor.plusMonths(n)
            RecurrenceFrequency.YEARLY -> anchor.plusYears(n)
        }
}
