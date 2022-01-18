/*
 * (C) Copyright IBM Deutschland GmbH 2021
 * (C) Copyright IBM Corp. 2021
 */

package de.ncth.covid.certificateValidator.main

import android.os.Bundle
import android.view.View
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.ensody.reactivestate.android.autoRun
import com.ensody.reactivestate.get
import com.ibm.health.common.android.utils.viewBinding
import com.ibm.health.common.navigation.android.FragmentNav
import com.ibm.health.common.navigation.android.findNavigator
import de.ncth.covid.certificateValidator.R
import de.ncth.covid.certificateValidator.databinding.CovpassCheckMainBinding
import de.ncth.covid.certificateValidator.information.CovPassCheckInformationFragmentNav
import de.ncth.covid.certificateValidator.scanner.CovPassCheckCameraDisclosureFragmentNav
import de.ncth.covid.certificateValidator.scanner.CovPassCheckQRScannerFragmentNav
import de.ncth.covid.commonapp.BaseFragment
import de.ncth.covid.commonapp.dependencies.commonDeps
import de.ncth.covid.commonapp.truetime.TimeValidationState
import de.ncth.covid.commonapp.uielements.showWarning
import de.ncth.covid.commonapp.utils.isCameraPermissionGranted
import de.ncth.covid.sdk.dependencies.sdkDeps
import de.ncth.covid.sdk.storage.DscRepository
import de.ncth.covid.sdk.utils.formatDateTime
import de.ncth.covid.sdk.worker.isDscListUpToDate
import kotlinx.parcelize.Parcelize
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Parcelize
public class MainFragmentNav : FragmentNav(MainFragment::class)

/**
 * Displays the start view of the app.
 */
internal class MainFragment : BaseFragment() {

    private val binding by viewBinding(CovpassCheckMainBinding::inflate)

    private val dscRepository get() = sdkDeps.dscRepository
    private val rulesUpdateRepository get() = sdkDeps.rulesUpdateRepository

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.mainSettingsImagebutton.setOnClickListener {
            findNavigator().push(CovPassCheckInformationFragmentNav())
        }
        binding.mainCheckCertButton.setOnClickListener {
            if (isCameraPermissionGranted(requireContext())) {
                findNavigator().push(CovPassCheckQRScannerFragmentNav())
            } else {
                findNavigator().push(CovPassCheckCameraDisclosureFragmentNav())
            }
        }
        ViewCompat.setAccessibilityDelegate(
            binding.mainHeaderTextview,
            object : AccessibilityDelegateCompat() {
                override fun onInitializeAccessibilityNodeInfo(host: View?, info: AccessibilityNodeInfoCompat) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    info.isHeading = true
                }
            }
        )
        ViewCompat.setAccessibilityDelegate(
            binding.mainCheckCertHeaderTextview,
            object : AccessibilityDelegateCompat() {
                override fun onInitializeAccessibilityNodeInfo(host: View?, info: AccessibilityNodeInfoCompat) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    info.isHeading = true
                }
            }
        )
        ViewCompat.setAccessibilityDelegate(
            binding.mainAvailabilityHeaderTextview,
            object : AccessibilityDelegateCompat() {
                override fun onInitializeAccessibilityNodeInfo(host: View?, info: AccessibilityNodeInfoCompat) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    info.isHeading = true
                }
            }
        )

        autoRun {
            updateAvailabilityCard(
                get(dscRepository.lastUpdate),
                get(rulesUpdateRepository.lastRulesUpdate)
            )
        }
        autoRun {
            when (val state = get(commonDeps.timeValidationRepository.state)) {
                is TimeValidationState.Failed -> {
                    binding.mainClockOutOfSync.isVisible = true
                    binding.mainClockOutOfSync.showWarning(
                        title = getString(R.string.validation_start_screen_scan_sync_message_title),
                        subtitle = getString(
                            R.string.validation_start_screen_scan_sync_message_text,
                            LocalDateTime.ofInstant(state.realTime, ZoneId.systemDefault()).formatDateTime()
                        ),
                        iconRes = R.drawable.info_warning,
                    )
                }
                TimeValidationState.NotInitialized, TimeValidationState.Success -> {
                    binding.mainClockOutOfSync.isVisible = false
                }
            }.let { }
        }
    }

    override fun onResume() {
        super.onResume()
        commonDeps.timeValidationRepository.validate()
    }

    private fun updateAvailabilityCard(lastUpdate: Instant, lastRulesUpdate: Instant) {
        val upToDate = isDscListUpToDate(lastUpdate)

        val availabilityStatusIconId = if (upToDate) {
            R.drawable.availability_success
        } else {
            R.drawable.availability_warning
        }
        binding.mainAvailabilityStatusImageview.setImageResource(availabilityStatusIconId)

        val availabilityStatusString = if (upToDate) {
            getString(R.string.validation_start_screen_offline_modus_note_latest_version)
        } else {
            getString(R.string.validation_start_screen_offline_modus_note_old_version)
        }
        binding.mainAvailabilityStatusTextview.text = availabilityStatusString

        if (lastUpdate == DscRepository.NO_UPDATE_YET || lastRulesUpdate == DscRepository.NO_UPDATE_YET) {
            binding.mainAvailabilityLastUpdateTextview.isGone = true
            binding.mainRulesAvailabilityLastUpdateTextview.isGone = true
        } else {
            binding.mainAvailabilityLastUpdateTextview.isGone = false
            binding.mainAvailabilityLastUpdateTextview.text = getString(
                R.string.validation_start_screen_offline_modus_certificates,
                LocalDateTime.ofInstant(lastUpdate, ZoneId.systemDefault()).formatDateTime()
            )
            binding.mainRulesAvailabilityLastUpdateTextview.isGone = false
            binding.mainRulesAvailabilityLastUpdateTextview.text = getString(
                R.string.validation_start_screen_offline_modus_rules,
                LocalDateTime.ofInstant(lastRulesUpdate, ZoneId.systemDefault()).formatDateTime()
            )
        }
    }
}