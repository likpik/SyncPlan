package com.example.syncplan.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.round

data class BillItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val price: Double,
    val participantIds: List<String> = emptyList()
)

data class BillParticipant(
    val id: String,
    val name: String,
    val email: String = ""
)

data class BillSplitResult(
    val participantId: String,
    val participantName: String,
    val totalAmount: Double,
    val items: List<Pair<String, Double>>,
    val tipAmount: Double,
    val taxAmount: Double
)

enum class SplitMethod {
    EQUAL,
    BY_ITEMS,
    PERCENTAGE,
    CUSTOM
}

class BillSplitCalculator : ViewModel() {
    private val _billItems = MutableStateFlow<List<BillItem>>(emptyList())
    val billItems: StateFlow<List<BillItem>> = _billItems.asStateFlow()

    private val _participants = MutableStateFlow<List<BillParticipant>>(emptyList())
    val participants: StateFlow<List<BillParticipant>> = _participants.asStateFlow()

    private val _splitMethod = MutableStateFlow(SplitMethod.EQUAL)
    val splitMethod: StateFlow<SplitMethod> = _splitMethod.asStateFlow()

    private val _tipPercentage = MutableStateFlow(0.0)
    val tipPercentage: StateFlow<Double> = _tipPercentage.asStateFlow()

    private val _taxPercentage = MutableStateFlow(0.0)
    val taxPercentage: StateFlow<Double> = _taxPercentage.asStateFlow()

    private val _customPercentages = MutableStateFlow<Map<String, Double>>(emptyMap())
    val customPercentages: StateFlow<Map<String, Double>> = _customPercentages.asStateFlow()

    private val _splitResults = MutableStateFlow<List<BillSplitResult>>(emptyList())
    val splitResults: StateFlow<List<BillSplitResult>> = _splitResults.asStateFlow()

    private val _totalAmount = MutableStateFlow(0.0)
    val totalAmount: StateFlow<Double> = _totalAmount.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun addParticipant(participant: BillParticipant) {
        _participants.value = _participants.value + participant
        recalculateSplit()
    }

    fun removeParticipant(participantId: String) {
        _participants.value = _participants.value.filter { it.id != participantId }
        _billItems.value = _billItems.value.map { item ->
            item.copy(participantIds = item.participantIds.filter { it != participantId })
        }
        _customPercentages.value = _customPercentages.value.filter { it.key != participantId }
        recalculateSplit()
    }

    fun addBillItem(item: BillItem) {
        _billItems.value = _billItems.value + item
        updateTotalAmount()
        recalculateSplit()
    }

    fun removeBillItem(itemId: String) {
        _billItems.value = _billItems.value.filter { it.id != itemId }
        updateTotalAmount()
        recalculateSplit()
    }

    fun updateBillItem(itemId: String, updatedItem: BillItem) {
        _billItems.value = _billItems.value.map { item ->
            if (item.id == itemId) updatedItem else item
        }
        updateTotalAmount()
        recalculateSplit()
    }

    fun assignParticipantToItem(itemId: String, participantId: String) {
        _billItems.value = _billItems.value.map { item ->
            if (item.id == itemId) {
                val updatedParticipants = if (item.participantIds.contains(participantId)) {
                    item.participantIds.filter { it != participantId }
                } else {
                    item.participantIds + participantId
                }
                item.copy(participantIds = updatedParticipants)
            } else {
                item
            }
        }
        recalculateSplit()
    }

    fun setSplitMethod(method: SplitMethod) {
        _splitMethod.value = method
        recalculateSplit()
    }

    fun setTipPercentage(percentage: Double) {
        _tipPercentage.value = percentage
        recalculateSplit()
    }

    fun setTaxPercentage(percentage: Double) {
        _taxPercentage.value = percentage
        recalculateSplit()
    }

    fun setCustomPercentage(participantId: String, percentage: Double) {
        _customPercentages.value = _customPercentages.value.toMutableMap().apply {
            put(participantId, percentage)
        }
        recalculateSplit()
    }

    fun setParticipantsFromEventAttendees(eventAttendees: List<String>, eventParticipantNames: Map<String, String>) {
        val participants = eventAttendees.map { attendeeId ->
            BillParticipant(
                id = attendeeId,
                name = eventParticipantNames[attendeeId] ?: "Nieznany uczestnik"
            )
        }
        _participants.value = participants
        recalculateSplit()
    }

    private fun updateTotalAmount() {
        val itemsTotal = _billItems.value.sumOf { it.price }
        _totalAmount.value = itemsTotal
    }

    private fun recalculateSplit() {
        viewModelScope.launch {
            _isLoading.value = true
            val results = when (_splitMethod.value) {
                SplitMethod.EQUAL -> calculateEqualSplit()
                SplitMethod.BY_ITEMS -> calculateItemBasedSplit()
                SplitMethod.PERCENTAGE -> calculatePercentageSplit()
                SplitMethod.CUSTOM -> calculateCustomSplit()
            }
            _splitResults.value = results
            _isLoading.value = false
        }
    }

    private fun calculateEqualSplit(): List<BillSplitResult> {
        val participants = _participants.value
        if (participants.isEmpty()) return emptyList()

        val baseAmount = _totalAmount.value
        val tipAmount = baseAmount * (_tipPercentage.value / 100.0)
        val taxAmount = baseAmount * (_taxPercentage.value / 100.0)
        val totalWithTipAndTax = baseAmount + tipAmount + taxAmount

        val perPersonAmount = totalWithTipAndTax / participants.size
        val perPersonTip = tipAmount / participants.size
        val perPersonTax = taxAmount / participants.size

        return participants.map { participant ->
            BillSplitResult(
                participantId = participant.id,
                participantName = participant.name,
                totalAmount = roundToTwoDecimals(perPersonAmount),
                items = listOf("Podział równo" to roundToTwoDecimals(baseAmount / participants.size)),
                tipAmount = roundToTwoDecimals(perPersonTip),
                taxAmount = roundToTwoDecimals(perPersonTax)
            )
        }
    }

    private fun calculateItemBasedSplit(): List<BillSplitResult> {
        val participants = _participants.value
        if (participants.isEmpty()) return emptyList()

        val baseAmount = _totalAmount.value
        val tipAmount = baseAmount * (_tipPercentage.value / 100.0)
        val taxAmount = baseAmount * (_taxPercentage.value / 100.0)

        val participantTotals = mutableMapOf<String, Double>()
        val participantItems = mutableMapOf<String, MutableList<Pair<String, Double>>>()

        participants.forEach { participant ->
            participantTotals[participant.id] = 0.0
            participantItems[participant.id] = mutableListOf()
        }

        _billItems.value.forEach { item ->
            if (item.participantIds.isNotEmpty()) {
                val pricePerParticipant = item.price / item.participantIds.size
                item.participantIds.forEach { participantId ->
                    participantTotals[participantId] = (participantTotals[participantId] ?: 0.0) + pricePerParticipant
                    participantItems[participantId]?.add(item.name to roundToTwoDecimals(pricePerParticipant))
                }
            }
        }

        return participants.map { participant ->
            val participantBase = participantTotals[participant.id] ?: 0.0
            val proportion = if (baseAmount > 0) participantBase / baseAmount else 0.0
            val participantTip = tipAmount * proportion
            val participantTax = taxAmount * proportion
            val total = participantBase + participantTip + participantTax

            BillSplitResult(
                participantId = participant.id,
                participantName = participant.name,
                totalAmount = roundToTwoDecimals(total),
                items = participantItems[participant.id] ?: emptyList(),
                tipAmount = roundToTwoDecimals(participantTip),
                taxAmount = roundToTwoDecimals(participantTax)
            )
        }
    }

    private fun calculatePercentageSplit(): List<BillSplitResult> {
        val participants = _participants.value
        if (participants.isEmpty()) return emptyList()

        val baseAmount = _totalAmount.value
        val tipAmount = baseAmount * (_tipPercentage.value / 100.0)
        val taxAmount = baseAmount * (_taxPercentage.value / 100.0)
        val totalWithTipAndTax = baseAmount + tipAmount + taxAmount

        val totalPercentage = _customPercentages.value.values.sum()
        if (totalPercentage == 0.0) {
            return calculateEqualSplit()
        }

        return participants.map { participant ->
            val percentage = _customPercentages.value[participant.id] ?: 0.0
            val participantAmount = totalWithTipAndTax * (percentage / 100.0)
            val participantBase = baseAmount * (percentage / 100.0)
            val participantTip = tipAmount * (percentage / 100.0)
            val participantTax = taxAmount * (percentage / 100.0)

            BillSplitResult(
                participantId = participant.id,
                participantName = participant.name,
                totalAmount = roundToTwoDecimals(participantAmount),
                items = listOf("${percentage}% rachunku" to roundToTwoDecimals(participantBase)),
                tipAmount = roundToTwoDecimals(participantTip),
                taxAmount = roundToTwoDecimals(participantTax)
            )
        }
    }

    private fun calculateCustomSplit(): List<BillSplitResult> {
        return calculateItemBasedSplit()
    }

    private fun roundToTwoDecimals(value: Double): Double {
        return round(value * 100) / 100
    }

    fun getTotalTip(): Double {
        return roundToTwoDecimals(_totalAmount.value * (_tipPercentage.value / 100.0))
    }

    fun getTotalTax(): Double {
        return roundToTwoDecimals(_totalAmount.value * (_taxPercentage.value / 100.0))
    }

    fun getGrandTotal(): Double {
        return roundToTwoDecimals(_totalAmount.value + getTotalTip() + getTotalTax())
    }

    fun generateSummaryText(): String {
        val results = _splitResults.value
        if (results.isEmpty()) return "Brak danych do podsumowania"

        val summary = buildString {
            appendLine("🧾 PODSUMOWANIE RACHUNKU")
            appendLine("==============================")
            appendLine("Suma pozycji: ${String.format("%.2f", _totalAmount.value)} zł")
            appendLine("Napiwek (${_tipPercentage.value}%): ${String.format("%.2f", getTotalTip())} zł")
            appendLine("Podatek (${_taxPercentage.value}%): ${String.format("%.2f", getTotalTax())} zł")
            appendLine("RAZEM: ${String.format("%.2f", getGrandTotal())} zł")
            appendLine()
            appendLine("💰 PODZIAŁ:")
            appendLine("------------------------------")

            results.forEach { result ->
                appendLine("${result.participantName}: ${String.format("%.2f", result.totalAmount)} zł")
                if (result.items.isNotEmpty()) {
                    result.items.forEach { (itemName, amount) ->
                        appendLine("  • $itemName: ${String.format("%.2f", amount)} zł")
                    }
                }
                if (result.tipAmount > 0) {
                    appendLine("  • Napiwek: ${String.format("%.2f", result.tipAmount)} zł")
                }
                if (result.taxAmount > 0) {
                    appendLine("  • Podatek: ${String.format("%.2f", result.taxAmount)} zł")
                }
                appendLine()
            }
        }

        return summary
    }

    fun clearAll() {
        _billItems.value = emptyList()
        _participants.value = emptyList()
        _splitMethod.value = SplitMethod.EQUAL
        _tipPercentage.value = 0.0
        _taxPercentage.value = 0.0
        _customPercentages.value = emptyMap()
        _splitResults.value = emptyList()
        _totalAmount.value = 0.0
    }

    fun exportToText(): String {
        return generateSummaryText()
    }

    fun validateSplit(): Boolean {
        if (_participants.value.isEmpty()) return false
        if (_billItems.value.isEmpty()) return false

        when (_splitMethod.value) {
            SplitMethod.PERCENTAGE -> {
                val totalPercentage = _customPercentages.value.values.sum()
                return totalPercentage == 100.0
            }
            SplitMethod.BY_ITEMS -> {
                return _billItems.value.all { it.participantIds.isNotEmpty() }
            }
            else -> return true
        }
    }
}
