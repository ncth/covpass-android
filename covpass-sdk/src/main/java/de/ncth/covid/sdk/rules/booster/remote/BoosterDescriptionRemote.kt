package de.ncth.covid.sdk.rules.booster.remote

import kotlinx.serialization.Serializable

@Serializable
public data class BoosterDescriptionRemote(
    val lang: String,
    val desc: String
)
