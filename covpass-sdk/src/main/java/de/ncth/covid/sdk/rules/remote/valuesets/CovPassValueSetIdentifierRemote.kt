/*
 * (C) Copyright IBM Deutschland GmbH 2021
 * (C) Copyright IBM Corp. 2021
 */

package de.ncth.covid.sdk.rules.remote.valuesets

import kotlinx.serialization.Serializable

@Serializable
public data class CovPassValueSetIdentifierRemote(
    val id: String,
    val hash: String
)
