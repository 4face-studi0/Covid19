package studio.forface.covid.domain.usecase

import studio.forface.covid.domain.gateway.Api
import studio.forface.covid.domain.gateway.Repository

/**
 * Sync Countries on local cache [Repository] from remote source [Api]
 * @author Davide Farella
 */
class SyncCountries(private val api: Api, private val repository: Repository) {

    suspend operator fun invoke() {
        val countries = api.getCountries()
        repository.storeCountries(countries)
    }
}
