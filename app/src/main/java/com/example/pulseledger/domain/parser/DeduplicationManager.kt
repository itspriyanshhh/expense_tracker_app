package com.example.pulseledger.domain.parser

import com.example.pulseledger.data.local.dao.MerchantMemoryDao
import com.example.pulseledger.data.local.dao.TransactionDao
import com.example.pulseledger.data.local.entity.TransactionEntity
import com.example.pulseledger.data.local.entity.VerificationState
import com.example.pulseledger.data.local.tier2.DeterministicRuleEngine
import com.example.pulseledger.ui.model.PulseCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

sealed interface DeduplicationResult {
    data class NewTransaction(val transaction: TransactionEntity) : DeduplicationResult
    data class MergedAndEnriched(val updatedTransaction: TransactionEntity, val mergedWithId: String) : DeduplicationResult
    data class DuplicateIgnored(val existingTransaction: TransactionEntity) : DeduplicationResult
}

/**
 * 3-Minute Sliding Window Deduplication & Transaction Enrichment Engine.
 *
 * When a user transacts via UPI, both the UPI application (GPay, PhonePe, Paytm)
 * and the banking SMS gateway fire notifications within 5 to 120 seconds of each other.
 *
 * This manager:
 * 1. Prevents duplicate debit/credit entries.
 * 2. Cross-enriches records (e.g. marrying GPay clean payee with Bank SMS 12-digit UTR ref).
 * 3. Enforces Tier 1 & Tier 2 categorization on new records.
 */
class DeduplicationManager(
    private val transactionDao: TransactionDao,
    private val merchantMemoryDao: MerchantMemoryDao
) {

    companion object {
        const val SLIDING_WINDOW_MILLIS = 180_000L // 3 minutes
    }

    suspend fun process(parsed: ParsedTransaction): DeduplicationResult = withContext(Dispatchers.IO) {
        // Step 1: Query Room for candidate transactions within ±180 seconds with matching amount & type
        val candidates = transactionDao.findWindowCandidates(
            amount = parsed.amount,
            type = parsed.type,
            timestamp = parsed.timestamp
        )

        // Step 2: Evaluate candidates for correlation
        val matchedCandidate = candidates.firstOrNull { candidate ->
            isSameTransaction(candidate, parsed)
        }

        if (matchedCandidate != null) {
            // Step 3: Match found within 3-minute window -> Merge metadata and enrich
            val enriched = mergeTransactions(matchedCandidate, parsed)
            transactionDao.updateTransaction(enriched)
            return@withContext DeduplicationResult.MergedAndEnriched(
                updatedTransaction = enriched,
                mergedWithId = matchedCandidate.id
            )
        }

        // Step 4: No duplicate found -> Categorize via Tier 1 & Tier 2 and insert as new
        val lookupKey = DeterministicRuleEngine.extractLookupKey(parsed.rawMerchant, parsed.upiVpa)
        val tier1Memory = merchantMemoryDao.getMerchantMemory(lookupKey)

        val newEntity = if (tier1Memory != null) {
            TransactionEntity(
                id = generateDeterministicId(parsed),
                amount = parsed.amount,
                type = parsed.type,
                rawMerchant = parsed.rawMerchant,
                displayMerchant = tier1Memory.customAlias ?: parsed.rawMerchant,
                upiVpa = parsed.upiVpa,
                upiRefNo = parsed.upiRefNo,
                bankOrAppSource = parsed.bankOrAppSource,
                category = tier1Memory.category,
                timestamp = parsed.timestamp,
                verificationState = VerificationState.VERIFIED_MEMORY
            )
        } else {
            val tier2Match = DeterministicRuleEngine.match(parsed.rawMerchant, parsed.upiVpa)
            if (tier2Match != null) {
                TransactionEntity(
                    id = generateDeterministicId(parsed),
                    amount = parsed.amount,
                    type = parsed.type,
                    rawMerchant = parsed.rawMerchant,
                    displayMerchant = tier2Match.cleanMerchantName,
                    upiVpa = parsed.upiVpa,
                    upiRefNo = parsed.upiRefNo,
                    bankOrAppSource = parsed.bankOrAppSource,
                    category = tier2Match.category.id,
                    timestamp = parsed.timestamp,
                    verificationState = VerificationState.VERIFIED_RULE
                )
            } else {
                // P2P Zero-Guessing Rule Fallback
                TransactionEntity(
                    id = generateDeterministicId(parsed),
                    amount = parsed.amount,
                    type = parsed.type,
                    rawMerchant = parsed.rawMerchant,
                    displayMerchant = parsed.rawMerchant,
                    upiVpa = parsed.upiVpa,
                    upiRefNo = parsed.upiRefNo,
                    bankOrAppSource = parsed.bankOrAppSource,
                    category = PulseCategory.UNCATEGORIZED_P2P.id,
                    timestamp = parsed.timestamp,
                    verificationState = VerificationState.PENDING_USER_TAP
                )
            }
        }

        transactionDao.insertTransaction(newEntity)

        // If Tier 1 memory was matched, update usage stats asynchronously
        if (tier1Memory != null) {
            merchantMemoryDao.rememberMerchant(
                lookupKey = lookupKey,
                category = tier1Memory.category,
                customAlias = tier1Memory.customAlias,
                timestamp = parsed.timestamp
            )
        }

        return@withContext DeduplicationResult.NewTransaction(newEntity)
    }

    private fun isSameTransaction(candidate: TransactionEntity, parsed: ParsedTransaction): Boolean {
        // 1. Definite match: Same 12-digit UPI UTR ref number
        if (!candidate.upiRefNo.isNullOrBlank() && !parsed.upiRefNo.isNullOrBlank()) {
            return candidate.upiRefNo == parsed.upiRefNo
        }

        // 2. Definite match: Same UPI VPA handle
        if (!candidate.upiVpa.isNullOrBlank() && !parsed.upiVpa.isNullOrBlank()) {
            if (candidate.upiVpa.equals(parsed.upiVpa, ignoreCase = true)) return true
        }

        // 3. Normalized merchant keyword overlap
        val cNorm = candidate.rawMerchant.trim().lowercase()
        val pNorm = parsed.rawMerchant.trim().lowercase()
        if (cNorm == pNorm || cNorm.contains(pNorm) || pNorm.contains(cNorm)) {
            return true
        }

        // 4. One record has generic bank placeholder (e.g. "Debit Transaction") and other has merchant
        if (isGenericPlaceholder(candidate.rawMerchant) || isGenericPlaceholder(parsed.rawMerchant)) {
            return true
        }

        // 5. Default window deduplication for identical amount & direction within 3 minutes
        return true
    }

    private fun mergeTransactions(
        existing: TransactionEntity,
        incoming: ParsedTransaction
    ): TransactionEntity {
        val mergedUpiRef = existing.upiRefNo ?: incoming.upiRefNo
        val mergedUpiVpa = existing.upiVpa ?: incoming.upiVpa

        // Combine sources if distinct (e.g. "GPay" + "HDFC Bank")
        val mergedSource = if (existing.bankOrAppSource.contains(incoming.bankOrAppSource, ignoreCase = true)) {
            existing.bankOrAppSource
        } else {
            "${existing.bankOrAppSource} + ${incoming.bankOrAppSource}"
        }

        // Choose the cleaner, more descriptive display name
        val betterMerchant = when {
            isGenericPlaceholder(existing.displayMerchant) && !isGenericPlaceholder(incoming.rawMerchant) ->
                incoming.rawMerchant
            !isGenericPlaceholder(existing.displayMerchant) ->
                existing.displayMerchant
            else ->
                incoming.rawMerchant
        }

        // Re-evaluate category if existing was unverified PENDING_USER_TAP and incoming has better merchant info
        var category = existing.category
        var verificationState = existing.verificationState

        if (existing.verificationState == VerificationState.PENDING_USER_TAP) {
            val tier2 = DeterministicRuleEngine.match(betterMerchant, mergedUpiVpa)
            if (tier2 != null) {
                category = tier2.category.id
                verificationState = VerificationState.VERIFIED_RULE
            }
        }

        return existing.copy(
            rawMerchant = if (!isGenericPlaceholder(incoming.rawMerchant)) incoming.rawMerchant else existing.rawMerchant,
            displayMerchant = betterMerchant,
            upiVpa = mergedUpiVpa,
            upiRefNo = mergedUpiRef,
            bankOrAppSource = mergedSource,
            category = category,
            verificationState = verificationState
        )
    }

    private fun isGenericPlaceholder(name: String): Boolean {
        val upper = name.trim().uppercase()
        return upper == "DEBIT TRANSACTION" ||
                upper == "CREDIT TRANSACTION" ||
                upper == "UPI" ||
                upper == "VPA" ||
                upper.startsWith("UPI/") ||
                upper.isBlank()
    }

    private fun generateDeterministicId(parsed: ParsedTransaction): String {
        return parsed.upiRefNo ?: "tx_${parsed.timestamp}_${(parsed.amount * 100).toLong()}_${abs(parsed.rawMerchant.hashCode())}"
    }
}
