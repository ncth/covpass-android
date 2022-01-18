/*
 * (C) Copyright IBM Deutschland GmbH 2021
 * (C) Copyright IBM Corp. 2021
 */

package de.ncth.covid.sdk.rules.booster.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class BoosterRuleInitial(
    @SerialName("Identifier")
    val identifier: String,
    val hash: String,
)
