package com.jyco.smarttransfer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jyco.smarttransfer.data.transfer.TransferRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class ReceivingViewModel(private val transfers: TransferRepository) : ViewModel() {
    val progress = transfers.progress
    init {
        viewModelScope.launch {
            try {
                transfers.receive()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // Repository exposes failures through progress.
            }
        }
    }
    override fun onCleared() {
        if (progress.value.busy) transfers.cancel()
        super.onCleared()
    }
}
