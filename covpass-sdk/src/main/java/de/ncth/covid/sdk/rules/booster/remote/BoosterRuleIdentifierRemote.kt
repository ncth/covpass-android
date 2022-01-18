/*
 * (C) Copyright IBM Deutschland GmbH 2021
 * (C) Copyright IBM Corp. 2021
 */

package de.ncth.covid.sdk.rules.booster.remote

import kotlinx.serialization.Serializable

@Serializable
public data class BoosterRuleIdentifierRemote(
    val identifier: String,
    val version: String,
    val hash: String
)
