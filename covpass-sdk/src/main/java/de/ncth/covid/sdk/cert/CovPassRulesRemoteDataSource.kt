/*
 * (C) Copyright IBM Deutschland GmbH 2021
 * (C) Copyright IBM Corp. 2021
 */

package de.ncth.covid.sdk.cert

import de.ncth.covid.sdk.dependencies.defaultJson
import de.ncth.covid.sdk.rules.remote.rules.CovPassRuleIdentifierRemote
import de.ncth.covid.sdk.rules.remote.rules.CovPassRuleRemote
import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*

public class CovPassRulesRemoteDataSource(httpClient: HttpClient, host: String) {
    private val client = httpClient.config {
        defaultRequest {
            this.host = host
        }
        install(JsonFeature) {
            serializer = KotlinxSerializer(defaultJson)
        }
    }

    public suspend fun getRuleIdentifiers(): List<CovPassRuleIdentifierRemote> =
        client.get("rules")

    public suspend fun getRule(country: String, hash: String): CovPassRuleRemote =
        client.get("rules/$country/$hash")
}
