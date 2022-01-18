/*
 * (C) Copyright IBM Deutschland GmbH 2021
 * (C) Copyright IBM Corp. 2021
 */

package de.ncth.covid.certificateValidator.scanner

import com.ensody.reactivestate.android.reactiveState
import com.ibm.health.common.navigation.android.FragmentNav
import com.ibm.health.common.navigation.android.findNavigator
import de.ncth.covid.certificateValidator.validation.*
import de.ncth.covid.commonapp.R
import de.ncth.covid.commonapp.dialog.DialogAction
import de.ncth.covid.commonapp.dialog.DialogListener
import de.ncth.covid.commonapp.scanner.QRScannerFragment
import de.ncth.covid.sdk.cert.models.CovCertificate
import de.ncth.covid.sdk.utils.formatDate
import kotlinx.parcelize.Parcelize
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException

@Parcelize
internal class CovPassCheckQRScannerFragmentNav : FragmentNav(CovPassCheckQRScannerFragment::class)

/**
 * QR Scanner Fragment extending from QRScannerFragment to intercept qr code scan result.
 */
internal class CovPassCheckQRScannerFragment :
    QRScannerFragment(), DialogListener, CovPassCheckQRScannerEvents, ValidationResultListener {

    private val viewModel by reactiveState { CovPassCheckQRScannerViewModel(scope) }

    override val announcementAccessibilityRes: Int = R.string.accessibility_scan_camera_announce

    override fun onBarcodeResult(qrCode: String) {
        viewModel.onQrContentReceived(qrCode)
    }

    override fun onDialogAction(tag: String, action: DialogAction) {
        scanEnabled.value = true
    }

    override fun onValidationSuccess(certificate: CovCertificate) {
        var vaccinationDate = certificate.vaccination?.occurrence;
        var vaccinationDatePeriod = vaccinationDate?.until(LocalDate.now());
        findNavigator().push(
            ValidationResultSuccessNav(
                certificate.fullName,
                certificate.fullTransliteratedName,
                formatDate(certificate.birthDateFormatted),
                vaccinationDatePeriod?.months,
                certificate.vaccination?.isBooster ?: false,
                certificate.vaccination?.hasFullProtection?: false,
            )
        )
    }

    override fun onValidPcrTest(
        certificate: CovCertificate,
        sampleCollection: ZonedDateTime?,
    ) {
        findNavigator().push(
            ValidPcrTestFragmentNav(
                certificate.fullName,
                formatDate(certificate.birthDateFormatted),
                certificate.fullTransliteratedName,
                sampleCollection
            )
        )
    }

    override fun onValidAntigenTest(
        certificate: CovCertificate,
        sampleCollection: ZonedDateTime?,
    ) {
        findNavigator().push(
            ValidAntigenTestFragmentNav(
                certificate.fullName,
                certificate.fullTransliteratedName,
                formatDate(certificate.birthDateFormatted),
                sampleCollection
            )
        )
    }

    override fun onValidationFailure(isTechnical: Boolean) {
        if (isTechnical) {
            findNavigator().push(ValidationResultTechnicalFailureFragmentNav())
        } else {
            findNavigator().push(ValidationResultFailureFragmentNav())
        }
    }

    override fun onValidationResultClosed() {
        scanEnabled.value = true
        sendAccessibilityAnnouncementEvent(announcementAccessibilityRes)
    }

    /**
     * Formats the birth date to "12.03.1989" only in case the given date is
     * in XXXX-XX-XX format. Otherwise we show the unformatted birth date.
     */
    private fun formatDate(birthDate: String): String {
        return try {
            LocalDate.parse(birthDate).formatDate()
        } catch (e: DateTimeParseException) {
            birthDate
        }
    }
}