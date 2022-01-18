/*
 * (C) Copyright IBM Deutschland GmbH 2021
 * (C) Copyright IBM Corp. 2021
 */

package de.ncth.covid.sdk.rules.remote.rules

import kotlinx.serialization.Serializable

@Serializable
public data class CovPassRuleIdentifierRemote(
    val identifier: String,
    val version: String,
    val country: String,
    val hash: String
)