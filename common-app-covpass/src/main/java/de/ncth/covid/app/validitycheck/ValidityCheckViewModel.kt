/*
 * (C) Copyright IBM Deutschland GmbH 2021
 * (C) Copyright IBM Corp. 2021
 */

package de.ncth.covid.app.validitycheck

import com.ensody.reactivestate.BaseReactiveState
import com.ensody.reactivestate.DependencyAccessor
import com.ensody.reactivestate.MutableValueFlow
import com.ibm.health.common.android.utils.BaseEvents
import de.ncth.covid.app.dependencies.covpassDeps
import de.ncth.covid.app.validitycheck.countries.Country
import de.ncth.covid.app.validitycheck.countries.CountryResolver.defaultCountry
import de.ncth.covid.sdk.cert.RulesValidator
import de.ncth.covid.sdk.dependencies.sdkDeps
import de.ncth.covid.sdk.storage.CertRepository
import kotlinx.coroutines.CoroutineScope
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

internal class ValidityCheckViewModel @OptIn(DependencyAccessor::class) constructor(
    scope: CoroutineScope,
    private val certRepository: CertRepository = covpassDeps.certRepository,
    private val rulesValidator: RulesValidator = sdkDeps.rulesValidator,
) : BaseReactiveState<BaseEvents>(scope) {

    val validationResults: MutableValueFlow<List<CertsValidationResults>> = MutableValueFlow(emptyList())
    val country: MutableValueFlow<Country> = MutableValueFlow(defaultCountry)
    val date: MutableValueFlow<LocalDateTime> = MutableValueFlow(LocalDateTime.now())

    init {
        launch {
            validateCertificates()
        }
    }

    private suspend fun validateCertificates() {
        validationResults.value = certRepository.certs.value.certificates.map {
            val covCertificate = it.getMainCertificate().covCertificate
            CertsValidationResults(
                covCertificate,
                rulesValidator.validate(
                    covCertificate,
                    country.value.countryCode.lowercase(),
                    ZonedDateTime.of(date.value, ZoneId.systemDefault())
                )
            )
        }
    }

    fun updateCountry(updatedCountry: Country) {
        launch {
            country.value = updatedCountry
            validateCertificates()
        }
    }

    fun updateDate(updatedDate: LocalDateTime) {
        launch {
            date.value = updatedDate
            validateCertificates()
        }
    }
}
