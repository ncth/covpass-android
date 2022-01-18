/*
 * (C) Copyright IBM Deutschland GmbH 2021
 * (C) Copyright IBM Corp. 2021
 */

package de.ncth.covid.sdk.cert.models

public data class EUValueSet(
    val valueSetId: String,
    val valueSetDate: String,
    val valueSetValues: Map<String, EUValueSetValue>
)
