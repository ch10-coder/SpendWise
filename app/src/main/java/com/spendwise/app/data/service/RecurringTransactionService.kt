package com.spendwise.app.data.service

import com.spendwise.app.data.local.dao.RecurringTransactionDao
import com.spendwise.app.data.local.dao.TransactionDao
import com.spendwise.app.data.local.entity.TransactionEntity
import com.spendwise.app.model.RecurrenceFrequency
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

class RecurringTransactionService(
    private val transactionDao: TransactionDao,
    private val recurringTransactionDao: RecurringTransactionDao
) {

    suspend fun processRecurringTransactions() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val dueRules = recurringTransactionDao.getDueRecurringTransactions(now)

        for (rule in dueRules) {
            var currentOccurrence = rule.nextOccurrence
            var active = rule.isActive

            while (currentOccurrence <= now && active) {
                val transaction = TransactionEntity(
                    title = rule.title,
                    amount = rule.amount,
                    type = rule.type,
                    categoryId = rule.categoryId,
                    accountId = rule.accountId,
                    dateTimestamp = currentOccurrence,
                    note = rule.note,
                    recurringRuleId = rule.id
                )
                transactionDao.insertTransaction(transaction)

                currentOccurrence = computeNextOccurrence(currentOccurrence, rule.frequency)

                if (rule.endDate != null && currentOccurrence > rule.endDate) {
                    active = false
                }
            }

            val updatedRule = rule.copy(
                nextOccurrence = currentOccurrence,
                isActive = active
            )
            recurringTransactionDao.updateRecurringTransaction(updatedRule)
        }
    }

    private fun computeNextOccurrence(current: Long, frequency: RecurrenceFrequency): Long {
        val zoneId = ZoneId.systemDefault()
        val date = Instant.ofEpochMilli(current).atZone(zoneId).toLocalDate()
        
        val nextDate = when (frequency) {
            RecurrenceFrequency.DAILY -> date.plusDays(1)
            RecurrenceFrequency.WEEKLY -> date.plusWeeks(1)
            RecurrenceFrequency.BIWEEKLY -> date.plusWeeks(2)
            RecurrenceFrequency.MONTHLY -> date.plusMonths(1)
            RecurrenceFrequency.YEARLY -> date.plusYears(1)
        }

        return nextDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }
}
