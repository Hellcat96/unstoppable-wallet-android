package io.horizontalsystems.bankwallet.modules.swap.approve

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.FeeRatePriority
import io.horizontalsystems.bankwallet.core.adapters.Erc20Adapter
import io.horizontalsystems.bankwallet.core.factories.FeeRateProviderFactory
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.guides.DataState
import io.reactivex.Observable
import java.math.BigDecimal

object SwapApproveModule {


    class Factory(private val coin: Coin, private val amount: BigDecimal, private val spenderAddress: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            val wallet = App.walletManager.wallet(coin)
            val erc20Adapter = wallet?.let { App.adapterManager.getAdapterForWallet(it) as? Erc20Adapter}

            val feeRateProvider = FeeRateProviderFactory.provider(coin)
            val feeCoinData = App.feeCoinProvider.feeCoinData(coin)
            val feeCoin = feeCoinData?.first ?: coin

            val feeWallet = App.walletManager.wallet(feeCoin)!!
            val feeBalanceAdapter = App.adapterManager.getBalanceAdapterForWallet(feeWallet)!!

            val baseCurrency = App.currencyManager.baseCurrency

            val feeService = FeeService(amount, spenderAddress, feeCoin, baseCurrency, erc20Adapter!!, feeRateProvider!!, App.xRateManager)
            val service = SwapApproveService(coin, amount, spenderAddress, feeService, erc20Adapter, feeBalanceAdapter)

            return SwapApproveViewModel(service, FeePresenter(feeService)) as T
        }
    }

    class InsufficientBalance : Exception()

}

interface ISwapApproveService {
    val coin: Coin
    val amount: BigDecimal
    val approveState: Observable<SwapApproveState>

    fun approve()
}

interface IFeeService {
    var gasPrice: Long
    var gasLimit: Long
    val feeRatePriority: FeeRatePriority

    val feeValues: Observable<DataState<Pair<CoinValue, CurrencyValue?>>>
}

sealed class SwapApproveState {
    object ApproveAllowed : SwapApproveState()
    object ApproveNotAllowed : SwapApproveState()
    object Loading : SwapApproveState()
    object Success : SwapApproveState()
    class Error(val e: Throwable) : SwapApproveState()
}
