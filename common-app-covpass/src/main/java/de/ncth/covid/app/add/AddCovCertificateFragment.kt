/*
 * (C) Copyright IBM Deutschland GmbH 2021
 * (C) Copyright IBM Corp. 2021
 */

package de.ncth.covid.app.add

import android.app.KeyguardManager
import android.content.Context
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.core.view.isGone
import com.ibm.health.common.android.utils.getSpanned
import com.ibm.health.common.android.utils.viewBinding
import com.ibm.health.common.navigation.android.FragmentNav
import com.ibm.health.common.navigation.android.findNavigator
import de.ncth.covid.app.R
import de.ncth.covid.app.databinding.AddCovCertPopupContentBinding
import de.ncth.covid.app.scanner.CovPassCameraDisclosureFragmentNav
import de.ncth.covid.app.scanner.CovPassQRScannerFragmentNav
import de.ncth.covid.commonapp.BaseBottomSheet
import de.ncth.covid.commonapp.uielements.showWarning
import de.ncth.covid.commonapp.utils.isCameraPermissionGranted
import de.ncth.covid.commonapp.utils.stripUnderlines
import kotlinx.parcelize.Parcelize

@Parcelize
internal class AddCovCertificateFragmentNav : FragmentNav(AddCovCertificateFragment::class)

/**
 * Fragment which shows the instructions for QR code scan
 */
internal class AddCovCertificateFragment : BaseBottomSheet() {

    override val buttonTextRes = R.string.certificate_add_popup_scan_button_title
    private val binding by viewBinding(AddCovCertPopupContentBinding::inflate)
    override val announcementAccessibilityRes: Int = R.string.accessibility_certificate_add_popup_announce

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bottomSheetBinding.bottomSheetTitle.text = getString(R.string.certificate_add_popup_title)
        binding.addCovCertFaq.apply {
            text = getSpanned(
                R.string.certificate_add_popup_action_title_linked,
                getString(R.string.cert_add_popup_link)
            )
            movementMethod = LinkMovementMethod.getInstance()
            stripUnderlines()
        }
        binding.warningElement.apply {
            showWarning(
                title = getString(R.string.certificate_add_popup_note_title),
                description = getString(R.string.certificate_add_popup_note_message),
                descriptionTopMarginDimenRes = R.dimen.grid_one
            )
            binding.warningElement.isGone = requireContext().isDeviceSecure()
        }
    }

    private fun Context.isDeviceSecure(): Boolean {
        return (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isDeviceSecure
    }

    override fun onActionButtonClicked() {
        if (isCameraPermissionGranted(requireContext())) {
            findNavigator().pop()
            findNavigator().push(CovPassQRScannerFragmentNav())
        } else {
            findNavigator().push(CovPassCameraDisclosureFragmentNav())
        }
    }
}
