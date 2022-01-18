/*
 * (C) Copyright IBM Deutschland GmbH 2021
 * (C) Copyright IBM Corp. 2021
 */

package de.ncth.covid.sdk.rules

import de.ncth.covid.sdk.cert.CovPassCountriesRemoteDataSource
import de.ncth.covid.sdk.rules.local.countries.CovPassCountriesLocalDataSource

public class CovPassCountriesRepository(
    private val remoteDataSource: CovPassCountriesRemoteDataSource,
    private val localDataSource: CovPassCountriesLocalDataSource,
) {

    public suspend fun getAllCovPassCountries(): List<String> {
        return localDataSource.getAllCountries()
    }

    public suspend fun prepopulate(countries: List<String>) {
        localDataSource.insertAll(countries)
    }

    public suspend fun loadCountries() {
        localDataSource.insertAll(remoteDataSource.getCountries())
    }
}
