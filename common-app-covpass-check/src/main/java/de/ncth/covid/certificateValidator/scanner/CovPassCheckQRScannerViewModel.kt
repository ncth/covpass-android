/*
 * (C) Copyright IBM Deutschland GmbH 2021
 * (C) Copyright IBM Corp. 2021
 */

package de.ncth.covid.certificateValidator.scanner

import com.ensody.reactivestate.BaseReactiveState
import com.ensody.reactivestate.DependencyAccessor
import com.ensody.reactivestate.ErrorEvents
import de.ncth.covid.certificateValidator.validitycheck.CovPassCheckValidationResult
import de.ncth.covid.certificateValidator.validitycheck.validate
import de.ncth.covid.logging.Lumber
import de.ncth.covid.sdk.cert.QRCoder
import de.ncth.covid.sdk.cert.RulesValidator
import de.ncth.covid.sdk.cert.models.*
import de.ncth.covid.sdk.cert.validateEntity
import de.ncth.covid.sdk.dependencies.sdkDeps
import kotlinx.coroutines.CoroutineScope
import java.time.ZonedDateTime

/**
 * Interface to communicate events from [CovPassCheckQRScannerViewModel] to [CovPassCheckQRScannerFragment].
 */
internal interface CovPassCheckQRScannerEvents : ErrorEvents {
    fun onValidationSuccess(certificate: CovCertificate)
    fun onValidationFailure(isTechnical: Boolean = false)
    fun onValidPcrTest(certificate: CovCertificate, sampleCollection: ZonedDateTime?)
    fun onValidAntigenTest(certificate: CovCertificate, sampleCollection: ZonedDateTime?)
}

/**
 * ViewModel holding the business logic for decoding and validating a [CovCertificate].
 */
internal class CovPassCheckQRScannerViewModel @OptIn(DependencyAccessor::class) constructor(
    scope: CoroutineScope,
    private val qrCoder: QRCoder = sdkDeps.qrCoder,
    private val rulesValidator: RulesValidator = sdkDeps.rulesValidator,
) : BaseReactiveState<CovPassCheckQRScannerEvents>(scope) {

    fun onQrContentReceived(qrContent: String) {
        launch {
            try {
                val covCertificate = qrCoder.decodeCovCert(qrContent)
                val dgcEntry = covCertificate.dgcEntry
                validateEntity(dgcEntry.idWithoutPrefix)
                when (validate(covCertificate, rulesValidator)) {
                    CovPassCheckValidationResult.Success -> {
                        when (dgcEntry) {
                            is Vaccination, is Recovery -> {
                                eventNotifier {
                                    onValidationSuccess(covCertificate)
                                }
                            }
                            is TestCert -> {
                                if (dgcEntry.type == TestCertType.NEGATIVE_PCR_TEST) {
                                    handleNegativePcrResult(covCertificate)
                                } else {
                                    handleNegativeAntigenResult(covCertificate)
                                }
                            }
                            // .let{} to enforce exhaustiveness
                        }.let {}
                    }
                    CovPassCheckValidationResult.TechnicalError -> eventNotifier { onValidationFailure(true) }
                    CovPassCheckValidationResult.ValidationError -> eventNotifier { onValidationFailure() }
                }
            } catch (exception: Exception) {
                Lumber.e(exception)
                eventNotifier { onValidationFailure(true) }
            }
        }
    }

    private fun handleNegativePcrResult(
        covCertificate: CovCertificate,
    ) {
        val test = covCertificate.dgcEntry as TestCert
        eventNotifier {
            onValidPcrTest(
                covCertificate,
                test.sampleCollection
            )
        }
    }

    private fun handleNegativeAntigenResult(
        covCertificate: CovCertificate,
    ) {
        val test = covCertificate.dgcEntry as TestCert
        eventNotifier {
            onValidAntigenTest(
                covCertificate,
                test.sampleCollection
            )
        }
    }
}
