package com.soulmate.app.ui.setting

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soulmate.app.domain.model.PremiumOffer
import com.soulmate.app.domain.model.PremiumPaymentOrder
import com.soulmate.app.domain.repository.IPaymentRepository
import com.soulmate.app.utils.BackendErrorParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PremiumUpgradeUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val offer: PremiumOffer? = null,
    val order: PremiumPaymentOrder? = null
)

@HiltViewModel
class PremiumUpgradeViewModel @Inject constructor(
    private val paymentRepository: IPaymentRepository
) : ViewModel() {

    private val _uiState = mutableStateOf(PremiumUpgradeUiState())
    val uiState: State<PremiumUpgradeUiState> = _uiState

    private val _messages = MutableSharedFlow<String>()
    val messages = _messages.asSharedFlow()

    fun loadCheckout() {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val offer = paymentRepository.getPremiumOffer()
                .onFailure { emitMessage(it) }
                .getOrNull()

            if (offer == null) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                return@launch
            }

            paymentRepository.createOrResumePremiumOrder()
                .onSuccess { createdOrder ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        offer = offer,
                        order = mergeOrder(_uiState.value.order, createdOrder)
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isLoading = false, offer = offer)
                    emitMessage(it)
                }
        }
    }

    fun refreshOrderStatus() {
        val currentOrder = _uiState.value.order ?: return
        if (_uiState.value.isRefreshing) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            paymentRepository.getPaymentOrderStatus(currentOrder.orderId)
                .onSuccess { latestOrder ->
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        order = mergeOrder(currentOrder, latestOrder)
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isRefreshing = false)
                    emitMessage(it)
                }
        }
    }

    fun reconcileOrder() {
        val currentOrder = _uiState.value.order ?: return
        if (_uiState.value.isRefreshing) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            paymentRepository.reconcilePaymentOrder(currentOrder.orderId)
                .onSuccess { latestOrder ->
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        order = mergeOrder(currentOrder, latestOrder)
                    )
                    if (!latestOrder.isPaid()) {
                        _messages.emit("Backend chua xac nhan giao dich. Ban vui long thu lai sau.")
                    }
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isRefreshing = false)
                    emitMessage(it)
                }
        }
    }

    private suspend fun emitMessage(throwable: Throwable) {
        _messages.emit(BackendErrorParser.toUserMessage(throwable))
    }

    private fun mergeOrder(existing: PremiumPaymentOrder?, incoming: PremiumPaymentOrder): PremiumPaymentOrder {
        return incoming.copy(
            amountVnd = if (incoming.amountVnd != 0L) incoming.amountVnd else existing?.amountVnd ?: 0L,
            durationDays = if (incoming.durationDays != 0) incoming.durationDays else existing?.durationDays ?: 0,
            createdAt = if (incoming.createdAt != 0L) incoming.createdAt else existing?.createdAt ?: 0L,
            qrImageUrl = incoming.qrImageUrl ?: existing?.qrImageUrl,
            bankCode = incoming.bankCode.ifBlank { existing?.bankCode.orEmpty() },
            bankAccount = incoming.bankAccount.ifBlank { existing?.bankAccount.orEmpty() },
            accountHolder = incoming.accountHolder.ifBlank { existing?.accountHolder.orEmpty() }
        )
    }
}
