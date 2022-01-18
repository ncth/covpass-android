/*
 * (C) Copyright IBM Deutschland GmbH 2021
 * (C) Copyright IBM Corp. 2021
 */

package de.ncth.covid.app.main

import com.ensody.reactivestate.BaseReactiveState
import com.ensody.reactivestate.DependencyAccessor
import com.ensody.reactivestate.dispatchers
import de.ncth.covid.app.checkerremark.CheckerRemarkRepository
import de.ncth.covid.app.dependencies.CovpassDependencies
import de.ncth.covid.app.dependencies.covpassDeps
import de.ncth.covid.commonapp.dependencies.CommonDependencies
import de.ncth.covid.commonapp.dependencies.commonDeps
import de.ncth.covid.commonapp.updateinfo.UpdateInfoRepository
import de.ncth.covid.sdk.cert.BoosterRulesValidator
import de.ncth.covid.sdk.cert.models.BoosterNotification
import de.ncth.covid.sdk.cert.models.BoosterResult
import de.ncth.covid.sdk.cert.models.CovCertificate
import de.ncth.covid.sdk.cert.models.GroupedCertificatesId
import de.ncth.covid.sdk.dependencies.sdkDeps
import de.ncth.covid.sdk.storage.CertRepository
import de.ncth.covid.sdk.utils.DescriptionLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay

/**
 * ViewModel providing the [onPageSelected] function and holding the [selectedCertId].
 */
internal class MainViewModel @OptIn(DependencyAccessor::class) constructor(
    scope: CoroutineScope,
    private val certRepository: CertRepository = covpassDeps.certRepository,
    private val boosterRulesValidator: BoosterRulesValidator = sdkDeps.boosterRulesValidator,
    private val covpassDependencies: CovpassDependencies = covpassDeps,
    private val commonDependencies: CommonDependencies = commonDeps,
) : BaseReactiveState<NotificationEvents>(scope) {

    init {
        runValidations()
    }

    internal var showingNotification = false

    internal fun validateNotifications() {
        when {
            showingNotification -> return
            certRepository.certs.value.certificates.any { it.hasSeenExpiryNotification } -> {
                showingNotification = true
                eventNotifier {
                    showExpiryNotification()
                }
            }
            commonDependencies.updateInfoRepository.updateInfoVersionShown.value
                != UpdateInfoRepository.CURRENT_UPDATE_VERSION -> {
                showingNotification = true
                eventNotifier {
                    showNewUpdateInfo()
                }
            }
            covpassDependencies.checkerRemarkRepository.checkerRemarkShown.value
                != CheckerRemarkRepository.CURRENT_CHECKER_REMARK_VERSION -> {
                showingNotification = true
                eventNotifier {
                    showCheckerRemark()
                }
            }
            covpassDependencies.certRepository.certs.value.certificates.any {
                it.boosterNotification.result == BoosterResult.Passed && !it.hasSeenBoosterNotification
            } -> {
                showingNotification = true
                eventNotifier {
                    showBoosterNotification()
                }
            }
        }
    }

    var selectedCertId: GroupedCertificatesId? = null

    fun onPageSelected(position: Int) {
        selectedCertId = certRepository.certs.value.getSortedCertificates()[position].id
    }

    private fun runValidations() {
        launch(dispatchers.default) {
            while (true) {
                checkBoosterNotification()
                validateNotifications()
                delay(BOOSTER_RULE_VALIDATION_INTERVAL_MS)
            }
        }
    }

    private suspend fun checkBoosterNotification() {
        val groupedCertificatesList = certRepository.certs.value
        for (groupedCert in groupedCertificatesList.certificates) {
            val latestVaccination = groupedCert.getLatestVaccination()?.covCertificate
            val latestRecovery = groupedCert.getLatestRecovery()?.covCertificate
            val recovery = latestRecovery?.recovery
            val boosterNotification = when {
                latestVaccination != null && recovery != null -> {
                    val mergedCertificate = latestVaccination.copy(
                        recoveries = listOf(recovery)
                    )
                    validateBoosterRules(boosterRulesValidator, mergedCertificate)
                }
                latestVaccination != null -> {
                    validateBoosterRules(boosterRulesValidator, latestVaccination)
                }
                else -> BoosterNotification(BoosterResult.Failed)
            }
            groupedCert.boosterNotification = boosterNotification
            if (boosterNotification.result == BoosterResult.Passed &&
                !groupedCert.boosterNotificationRuleIds.contains(boosterNotification.ruleId)
            ) {
                groupedCert.boosterNotificationRuleIds =
                    groupedCert.boosterNotificationRuleIds + boosterNotification.ruleId
                groupedCert.hasSeenBoosterNotification = false
                groupedCert.hasSeenBoosterDetailNotification = false
            }
        }

        if (groupedCertificatesList.certificates.isNotEmpty()) {
            certRepository.certs.update {
                it.certificates = groupedCertificatesList.certificates
            }
        }
    }

    private suspend fun validateBoosterRules(
        boosterRulesValidator: BoosterRulesValidator,
        covCertificate: CovCertificate,
    ): BoosterNotification {
        val boosterResult = boosterRulesValidator.validate(covCertificate).firstOrNull {
            it.result == de.ncth.covid.sdk.cert.BoosterResult.PASSED
        }
        return if (boosterResult != null) {
            BoosterNotification(
                BoosterResult.Passed,
                boosterResult.rule.getDescriptionFor(DescriptionLanguage.ENGLISH.languageCode),
                boosterResult.rule.getDescriptionFor(DescriptionLanguage.GERMAN.languageCode),
                boosterResult.rule.identifier
            )
        } else {
            BoosterNotification(BoosterResult.Failed)
        }
    }

    private companion object {
        private const val BOOSTER_RULE_VALIDATION_INTERVAL_MS: Long = 60L * 60L * 1000L
    }
}
