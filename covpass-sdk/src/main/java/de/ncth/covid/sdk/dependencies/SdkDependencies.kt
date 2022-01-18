/*
 * (C) Copyright IBM Deutschland GmbH 2021
 * (C) Copyright IBM Corp. 2021
 */

package de.ncth.covid.sdk.dependencies

import android.app.Application
import androidx.lifecycle.LifecycleOwner
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ensody.reactivestate.DependencyAccessor
import com.fasterxml.jackson.databind.ObjectMapper
import de.ncth.covid.http.httpConfig
import de.ncth.covid.http.pinPublicKey
import de.ncth.covid.sdk.R
import de.ncth.covid.sdk.cert.*
import de.ncth.covid.sdk.cert.models.CertificateListMapper
import de.ncth.covid.sdk.cert.models.DscList
import de.ncth.covid.sdk.crypto.readPemAsset
import de.ncth.covid.sdk.crypto.readPemKeyAsset
import de.ncth.covid.sdk.rules.*
import de.ncth.covid.sdk.rules.booster.BoosterRule
import de.ncth.covid.sdk.rules.booster.CovPassBoosterRulesRepository
import de.ncth.covid.sdk.rules.booster.local.BoosterRulesDao
import de.ncth.covid.sdk.rules.booster.local.CovPassBoosterRulesLocalDataSource
import de.ncth.covid.sdk.rules.booster.remote.BoosterRuleInitial
import de.ncth.covid.sdk.rules.booster.remote.BoosterRuleRemote
import de.ncth.covid.sdk.rules.booster.remote.toBoosterRule
import de.ncth.covid.sdk.rules.domain.rules.CovPassGetRulesUseCase
import de.ncth.covid.sdk.rules.local.CovPassDatabase
import de.ncth.covid.sdk.rules.local.countries.CountriesDao
import de.ncth.covid.sdk.rules.local.countries.CovPassCountriesLocalDataSource
import de.ncth.covid.sdk.rules.local.rules.CovPassRulesDao
import de.ncth.covid.sdk.rules.local.rules.CovPassRulesLocalDataSource
import de.ncth.covid.sdk.rules.local.valuesets.CovPassValueSetsDao
import de.ncth.covid.sdk.rules.local.valuesets.CovPassValueSetsLocalDataSource
import de.ncth.covid.sdk.rules.remote.rules.CovPassRuleInitial
import de.ncth.covid.sdk.rules.remote.rules.CovPassRuleRemote
import de.ncth.covid.sdk.rules.remote.rules.toCovPassRule
import de.ncth.covid.sdk.rules.remote.valuesets.CovPassValueSetInitial
import de.ncth.covid.sdk.rules.remote.valuesets.CovPassValueSetRemote
import de.ncth.covid.sdk.rules.remote.valuesets.toCovPassValueSet
import de.ncth.covid.sdk.storage.CborSharedPrefsStore
import de.ncth.covid.sdk.storage.DscRepository
import de.ncth.covid.sdk.storage.RulesUpdateRepository
import de.ncth.covid.sdk.utils.readTextAsset
import dgca.verifier.app.engine.*
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.security.cert.X509Certificate

/**
 * Global var for making the [SdkDependencies] accessible.
 */
private lateinit var _sdkDeps: SdkDependencies

@DependencyAccessor
public var sdkDeps: SdkDependencies
    get() = _sdkDeps
    set(value) {
        _sdkDeps = value
        value.init()
    }

@OptIn(DependencyAccessor::class)
public val LifecycleOwner.sdkDeps: SdkDependencies
    get() = de.ncth.covid.sdk.dependencies.sdkDeps

/**
 * Access to various dependencies for covpass-sdk module.
 */
public abstract class SdkDependencies {

    public abstract val application: Application

    public open val trustServiceHost: String by lazy {
        application.getString(R.string.trust_service_host).takeIf { it.isNotEmpty() }
            ?: throw IllegalStateException("You have to set @string/trust_service_host or override trustServiceHost")
    }

    private val httpClient by lazy { httpConfig.ktorClient() }

    public open val backendCa: List<X509Certificate> by lazy {
        application.readPemAsset("covpass-sdk/backend-ca.pem")
    }

    public val dscList: DscList by lazy {
        decoder.decodeDscList(
            application.readTextAsset("covpass-sdk/dsc-list.json")
        )
    }

    public val dscListService: DscListService by lazy { DscListService(httpClient, trustServiceHost) }

    public val dscRepository: DscRepository by lazy {
        DscRepository(CborSharedPrefsStore("dsc_cert_prefs", cbor), dscList)
    }

    public val rulesUpdateRepository: RulesUpdateRepository by lazy {
        RulesUpdateRepository(CborSharedPrefsStore("rules_update_prefs", cbor))
    }

    public val validator: CertValidator by lazy { CertValidator(dscList.toTrustedCerts(), cbor) }

    public val decoder: DscListDecoder by lazy { DscListDecoder(publicKey.first()) }

    private val publicKey by lazy { application.readPemKeyAsset("covpass-sdk/dsc-list-signing-key.pem") }

    /**
     * The [QRCoder].
     */
    public val qrCoder: QRCoder by lazy { QRCoder(validator) }

    public val cbor: Cbor = defaultCbor

    public val json: Json = defaultJson

    internal fun init() {
        httpConfig.pinPublicKey(backendCa)
    }

    public val certificateListMapper: CertificateListMapper by lazy {
        CertificateListMapper(qrCoder)
    }

    private val certLogicDeps: CertLogicDeps by lazy {
        CertLogicDeps(application)
    }

    private val dccRulesHost: String by lazy { "distribution.dcc-rules.de" }

    private val covPassRulesRemoteDataSource: CovPassRulesRemoteDataSource by lazy {
        CovPassRulesRemoteDataSource(httpClient, dccRulesHost)
    }

    private val covPassValueSetsRemoteDataSource: CovPassValueSetsRemoteDataSource by lazy {
        CovPassValueSetsRemoteDataSource(httpClient, dccRulesHost)
    }

    private val boosterRulesRemoteDataSource: BoosterRulesRemoteDataSource by lazy {
        BoosterRulesRemoteDataSource(httpClient, "distribution-cff4f7147260.dcc-rules.de")
    }

    private val countriesRemoteDataSource: CovPassCountriesRemoteDataSource by lazy {
        CovPassCountriesRemoteDataSource(httpClient, dccRulesHost)
    }

    private val covPassDatabase: CovPassDatabase by lazy { createDb("covpass-database") }

    private inline fun <reified T : RoomDatabase> createDb(name: String): T =
        Room.databaseBuilder(application, T::class.java, name)
            .fallbackToDestructiveMigration()
            .build()

    private val covPassRulesLocalDataSource: CovPassRulesLocalDataSource by lazy {
        CovPassRulesLocalDataSource(covPassRulesDao)
    }

    private val covPassValueSetsLocalDataSource: CovPassValueSetsLocalDataSource by lazy {
        CovPassValueSetsLocalDataSource(covPassValueSetsDao)
    }

    private val covPassBoosterRulesLocalDataSource: CovPassBoosterRulesLocalDataSource by lazy {
        CovPassBoosterRulesLocalDataSource(boosterRuleDao)
    }

    private val covPassCountriesLocalDataSource: CovPassCountriesLocalDataSource by lazy {
        CovPassCountriesLocalDataSource(countriesDao)
    }

    private val covPassRulesDao: CovPassRulesDao by lazy { covPassDatabase.covPassRulesDao() }

    private val covPassValueSetsDao: CovPassValueSetsDao by lazy { covPassDatabase.covPassValueSetsDao() }

    private val boosterRuleDao: BoosterRulesDao by lazy { covPassDatabase.boosterRulesDao() }

    private val countriesDao: CountriesDao by lazy { covPassDatabase.countriesDao() }

    private val euRulePath: String by lazy { "covpass-sdk/eu-rules.json" }
    private val covPassRulesInitial: List<CovPassRuleInitial> by lazy {
        defaultJson.decodeFromString(
            application.readTextAsset(euRulePath)
        )
    }

    private val covPassRulesRemote: List<CovPassRuleRemote> by lazy {
        defaultJson.decodeFromString(
            application.readTextAsset(euRulePath)
        )
    }

    public val bundledRules: List<CovPassRule> by lazy {
        covPassRulesRemote.zip(covPassRulesInitial).map {
            it.first.toCovPassRule(it.second.hash)
        }
    }

    private val euValueSetsPath: String by lazy { "covpass-sdk/eu-value-sets.json" }
    private val covPassValueSetsRemote: List<CovPassValueSetRemote> by lazy {
        defaultJson.decodeFromString(
            application.readTextAsset(euValueSetsPath)
        )
    }

    private val covPassValueSetsInitial: List<CovPassValueSetInitial> by lazy {
        defaultJson.decodeFromString(
            application.readTextAsset(euValueSetsPath)
        )
    }

    public val bundledValueSets: List<CovPassValueSet> by lazy {
        covPassValueSetsRemote.zip(covPassValueSetsInitial).map {
            it.first.toCovPassValueSet(it.second.hash)
        }
    }

    private val boosterRulesPath: String by lazy { "covpass-sdk/eu-booster-rules.json" }
    public val boosterRulesRemote: List<BoosterRuleRemote> by lazy {
        defaultJson.decodeFromString(
            application.readTextAsset(boosterRulesPath)
        )
    }

    public val boosterRulesInitial: List<BoosterRuleInitial> by lazy {
        defaultJson.decodeFromString(
            application.readTextAsset(boosterRulesPath)
        )
    }

    public val bundledBoosterRules: List<BoosterRule> by lazy {
        boosterRulesRemote.zip(boosterRulesInitial).map {
            it.first.toBoosterRule(it.second.hash)
        }
    }

    public val bundledCountries: List<String> by lazy {
        defaultJson.decodeFromString(
            application.readTextAsset("covpass-sdk/eu-countries.json")
        )
    }

    public val covPassRulesRepository: CovPassRulesRepository by lazy {
        CovPassRulesRepository(
            covPassRulesRemoteDataSource,
            covPassRulesLocalDataSource,
            rulesUpdateRepository
        )
    }

    public val covPassValueSetsRepository: CovPassValueSetsRepository by lazy {
        CovPassValueSetsRepository(
            covPassValueSetsRemoteDataSource,
            covPassValueSetsLocalDataSource
        )
    }

    public val covPassBoosterRulesRepository: CovPassBoosterRulesRepository by lazy {
        CovPassBoosterRulesRepository(
            boosterRulesRemoteDataSource,
            covPassBoosterRulesLocalDataSource
        )
    }

    public val covPassCountriesRepository: CovPassCountriesRepository by lazy {
        CovPassCountriesRepository(
            countriesRemoteDataSource,
            covPassCountriesLocalDataSource
        )
    }

    private val getRulesUseCase: CovPassGetRulesUseCase by lazy {
        CovPassGetRulesUseCase(covPassRulesRepository)
    }

    public val rulesValidator: RulesValidator by lazy {
        RulesValidator(
            getRulesUseCase,
            certLogicDeps.certLogicEngine,
            covPassValueSetsRepository
        )
    }

    private val boosterCertLogicEngine: BoosterCertLogicEngine by lazy {
        BoosterCertLogicEngine(certLogicDeps.jsonLogicValidator)
    }

    public val boosterRulesValidator: BoosterRulesValidator by lazy {
        BoosterRulesValidator(
            boosterCertLogicEngine,
            covPassBoosterRulesRepository
        )
    }
}

public class CertLogicDeps(
    private val application: Application
) {
    private val objectMapper: ObjectMapper by lazy {
        ObjectMapper().apply {
            findAndRegisterModules()
        }
    }

    private val jsonSchema: String by lazy {
        application.readTextAsset("covpass-sdk/json-schema-v1.json")
    }

    public val jsonLogicValidator: JsonLogicValidator by lazy {
        DefaultJsonLogicValidator()
    }

    private val affectedFieldsDataRetriever: AffectedFieldsDataRetriever by lazy {
        DefaultAffectedFieldsDataRetriever(objectMapper.readTree(jsonSchema), objectMapper)
    }

    public val certLogicEngine: CertLogicEngine by lazy {
        DefaultCertLogicEngine(affectedFieldsDataRetriever, jsonLogicValidator)
    }
}