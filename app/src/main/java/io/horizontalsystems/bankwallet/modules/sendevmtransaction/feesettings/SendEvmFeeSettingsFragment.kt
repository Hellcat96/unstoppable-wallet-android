package io.horizontalsystems.bankwallet.modules.sendevmtransaction.feesettings

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.ethereum.EthereumFeeViewModel
import io.horizontalsystems.bankwallet.core.ethereum.EvmTransactionFeeService
import io.horizontalsystems.bankwallet.core.ethereum.EvmTransactionFeeService.*
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.FeeCell
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.CellSingleLineLawrenceSection
import io.horizontalsystems.seekbar.FeeSeekBar
import io.horizontalsystems.seekbar.SeekBarConfig

class SendEvmFeeSettingsFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val feeViewModel by navGraphViewModels<EthereumFeeViewModel>(requireArguments().getInt(NAV_GRAPH_ID))

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            setContent {
                ComposeAppTheme {
                    SendEvmFeeSettingsScreen(
                        feeViewModel,
                        onClickNavigation = {
                            findNavController().popBackStack()
                        }
                    )
                }
            }
        }
    }

    companion object {
        private const val NAV_GRAPH_ID = "nav_graph_id"

        fun prepareParams(@IdRes navGraphId: Int) =
            bundleOf(NAV_GRAPH_ID to navGraphId)
    }

}

@Composable
fun SendEvmFeeSettingsScreen(
    viewModel: EthereumFeeViewModel,
    onClickNavigation: () -> Unit
) {
    val fee by viewModel.feeLiveData.observeAsState()
    val gasData by viewModel.gasDataLiveData.observeAsState()
    val feeSlider by viewModel.feeSliderLiveData.observeAsState()

    Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
        AppBar(
            title = TranslatableString.ResString(R.string.FeeSettings_Title),
            navigationIcon = {
                IconButton(onClick = onClickNavigation) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_back),
                        contentDescription = "back button",
                        tint = ComposeAppTheme.colors.jacob
                    )
                }
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        FeeCell(title = stringResource(R.string.FeeSettings_Fee), fee)
        Spacer(modifier = Modifier.height(8.dp))

        val gasPrice = gasData?.gasPrice
        if (gasPrice is GasPrice.Legacy) {
            LegacyFeeSettings(
                viewModel.convert(gasPrice.value, gasPrice.unit, EvmTransactionFeeService.Unit.GWEI).toLong(),
                viewModel.convert(gasPrice.gasPrice.bounds.lower, gasPrice.unit, EvmTransactionFeeService.Unit.GWEI).toLong(),
                viewModel.convert(gasPrice.gasPrice.bounds.upper, gasPrice.unit, EvmTransactionFeeService.Unit.GWEI).toLong(),
                gasPrice.unit,
                gasData?.gasLimit?.let { viewModel.formatGasLimit(it) },
                gasData?.gasPrice?.let { viewModel.formatGasPrice(it) },
            ) {
                Log.e("AAA", "onSelectGasPrice: $it")

                viewModel.changeCustomPriority(it.toLong())
            }
        }


    }

}

@Composable
fun LegacyFeeSettings(
    gasPrice: Long,
    minValue: Long,
    maxValue: Long,
    unit: EvmTransactionFeeService.Unit,
    gasLimitFormatted: String?,
    gasPriceFormatted: String?,
    onSelectGasPrice: (value: Int) -> Unit
) {
    val settingsViewItems = mutableListOf<@Composable () -> Unit>()

    settingsViewItems.add {
        FeeInfoCell(title = stringResource(R.string.FeeSettings_GasLimit), value = gasLimitFormatted) {
            //Open Gas Limit info
        }
    }

    settingsViewItems.add {
        FeeInfoCell(title = stringResource(R.string.FeeSettings_GasPrice), value = gasPriceFormatted) {
            //Open Gas Price info
        }
    }

    settingsViewItems.add {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 16.dp),
            factory = { context ->
                val config = SeekBarConfig(context).apply {
                    bubbleBackground = R.color.claude
                    textColor = R.color.leah
                }
                FeeSeekBar(context, config)
                    .apply {
                        background = null
                        progressDrawable = null
                        thumb = context.getDrawable(R.drawable.seekbar_thumb)

                        setListener(object : FeeSeekBar.Listener {
                            override fun onSelect(value: Int) {
                               onSelectGasPrice(value)
                            }
                        })
                    }

            },
            update = { view ->
                view.post {
                    Log.e("AAA", "progress: $gasPrice, min: $minValue, max: $maxValue")
                    view.progress = gasPrice.toInt()
                    view.min = minValue.toInt()
                    view.max = maxValue.toInt()
                    view.setBubbleHint(unit.title)
                }
            }
        )
    }

    CellSingleLineLawrenceSection(settingsViewItems)
}

@Composable
fun FeeInfoCell(title: String, value: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painter = painterResource(id = R.drawable.ic_info_20), contentDescription = "")

        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = ComposeAppTheme.typography.subhead2,
            color = ComposeAppTheme.colors.grey
        )

        Text(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            text = value ?: "",
            style = ComposeAppTheme.typography.subhead1,
            color = ComposeAppTheme.colors.leah,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
