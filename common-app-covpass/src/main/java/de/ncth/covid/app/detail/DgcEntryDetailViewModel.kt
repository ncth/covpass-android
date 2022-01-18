/*
 * (C) Copyright IBM Deutschland GmbH 2021
 * (C) Copyright IBM Corp. 2021
 */

package de.ncth.covid.app.detail

import com.ensody.reactivestate.BaseReactiveState
import com.ensody.reactivestate.DependencyAccessor
import com.ibm.health.common.android.utils.BaseEvents
import de.ncth.covid.app.dependencies.covpassDeps
import de.ncth.covid.app.validitycheck.countries.CountryResolver.defaultCountry
import de.ncth.covid.sdk.storage.CertRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Interface to communicate events from [DgcEntryDetailViewModel] to [DgcEntryDetailFragment].
 */
internal interface DgcEntryDetailEvents : BaseEvents {
    fun onDeleteDone(isGroupedCertDeleted: Boolean)
}

/**
 * ViewModel to handle business logic related to [DgcEntryDetailFragment].
 */
internal class DgcEntryDetailViewModel @OptIn(DependencyAccessor::class) constructor(
    scope: CoroutineScope,
    private val certRepository: CertRepository = covpassDeps.certRepository,
) : BaseReactiveState<DgcEntryDetailEvents>(scope) {

    val isPdfExportEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)

    fun onDelete(certId: String) {
        launch {
            var isGroupedCertDeleted = false
            certRepository.certs.update {
                isGroupedCertDeleted = it.deleteCovCertificate(certId)
            }
            eventNotifier {
                onDeleteDone(isGroupedCertDeleted)
            }
        }
    }

    fun checkPdfExport(certId: String) {
        val combinedCovCertificate = certRepository.certs.value.getCombinedCertificate(certId) ?: return
        isPdfExportEnabled.value =
            combinedCovCertificate.covCertificate.issuer.equals(defaultCountry.countryCode, ignoreCase = true)
    }
}
