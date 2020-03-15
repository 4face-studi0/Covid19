package studio.forface.covid.data.local.mapper

import studio.forface.covid.data.local.model.CountryStatPlainDbModel
import studio.forface.covid.data.local.model.CountryStatPlainDbModelImpl
import studio.forface.covid.data.local.model.CountryWithProvinceDbModel
import studio.forface.covid.data.local.model.CountryWithProvincesStatPlainDbModel
import studio.forface.covid.data.local.model.ProvinceStatPlainDbModel
import studio.forface.covid.data.local.model.ProvinceStatPlainDbModelImpl
import studio.forface.covid.data.local.model.wrap
import studio.forface.covid.domain.entity.Country
import studio.forface.covid.domain.entity.CountryFullStat
import studio.forface.covid.domain.entity.CountrySmallStat
import studio.forface.covid.domain.entity.CountryStat
import studio.forface.covid.domain.entity.ProvinceFullStat
import studio.forface.covid.domain.entity.ProvinceId
import studio.forface.covid.domain.entity.ProvinceStat
import studio.forface.covid.domain.entity.Stat
import studio.forface.covid.domain.invoke
import studio.forface.covid.domain.mapper.map

/**
 * Map a [List] of [CountryWithProvinceDbModel] to a [Country]
 * NOTE: [CountryWithProvinceDbModel]s are supposed to refer all to the same County!!
 *
 * @author Davide Farella
 */
internal class SingleCountryDbModelMapper(
    private val provinceMapper: ProvinceDbModelMapper
) : DatabaseModelMapper<List<CountryWithProvinceDbModel>, Country> {

    override fun List<CountryWithProvinceDbModel>.toEntity() = Country(
        id = first().id,
        name = first().name,
        provinces = map { provinceMapper { it.toEntity() } }
    )
}

/**
 * Map a [List] of [CountryWithProvinceDbModel] to a [List] of [Country]
 *
 * @author Davide Farella
 */
internal class MultiCountryDbModelMapper(
    private val singleCountyMapper: SingleCountryDbModelMapper,
    private val provinceMapper: ProvinceDbModelMapper
) : DatabaseModelMapper<List<CountryWithProvinceDbModel>, List<Country>> {

    /**
     * @return [List] of [Country] from the receiver list of [CountryWithProvinceDbModel]
     */
    override fun List<CountryWithProvinceDbModel>.toEntity() = groupBy { it.id }.map { (_, list) ->
        val country = singleCountyMapper { list.toEntity() }
        val provinces = list.wrap.map(provinceMapper) { it.toEntity() }
        country.copy(provinces = provinces)
    }
}

/**
 * Map a [List] of [CountryStatPlainDbModel] to a [CountrySmallStat]
 * NOTE: [CountryStatPlainDbModel]s are supposed to refer all to the same County!!
 *
 * @author Davide Farella
 */
internal class CountrySmallStatDbModelMapper(
    private val countryPlainMapper: SingleCountryPlainDbModelMapper,
    private val countyStatPlainMapper: CountryStatPlainDbModelMapper
) : DatabaseModelMapper<List<CountryStatPlainDbModel>, CountrySmallStat> {

    override fun List<CountryStatPlainDbModel>.toEntity() = CountrySmallStat(
        country = countryPlainMapper { toEntity() },
        stat = countyStatPlainMapper { first().toEntity() }
    )
}

internal abstract class AbsCountryStatDbModelMapper<DbModel, CountryStatType, ProvinceStatType>(
    private val countryPlainMapper: SingleCountryPlainDbModelMapper,
    private val countyStatPlainMapper: CountryStatFromCountryWithProvinceStatPlainDbModelMapper,
    protected val provinceStatPlainMapper: DatabaseModelMapper<DbModel, ProvinceStatType>,
    private val buildProvinceStats: AbsCountryStatDbModelMapper<DbModel, CountryStatType, ProvinceStatType>.(
        List<CountryWithProvincesStatPlainDbModel>
    ) -> Map<ProvinceId, ProvinceStatType>,
    private val toCountryStatType: Params<ProvinceStatType>.() -> CountryStatType
) : DatabaseModelMapper<List<CountryWithProvincesStatPlainDbModel>, CountryStatType> {

    override fun List<CountryWithProvincesStatPlainDbModel>.toEntity(): CountryStatType {
        // Every group is relative to a single Province
        val groupByProvinces = groupBy { it.provinceId }

        // Take a value for each Province, ignoring different Stats with same Province
        // Each Stat will have different Province but they could have same Timestamp
        val provinceStats = groupByProvinces.map { it.value.first() }

        // Take a value for each Country Timestamp, ignoring different Stats with same Timestamp
        // Each Stat will have different Timestamp
        val countryTimestampStats = groupBy { it.countryTimestamp }.map { it.value.first() }

        return Params(
            country = countryPlainMapper { provinceStats.toCountryStatsPlain().toEntity() },
            countryStat = countryTimestampStats.map(countyStatPlainMapper) { it.toEntity() },
            provinceStats = buildProvinceStats(this)
        ).toCountryStatType()
    }

    private fun List<CountryWithProvincesStatPlainDbModel>.toCountryStatsPlain() = map {
        CountryStatPlainDbModelImpl(
            countryId = it.countryId,
            countryName = it.countryName,
            provinceId = it.provinceId,
            provinceName = it.provinceName,
            provinceLat = it.provinceLat,
            provinceLng = it.provinceLng,
            confirmed = it.countryConfirmed,
            deaths = it.countryDeaths,
            recovered = it.countryRecovered,
            timestamp = it.countryTimestamp
        )
    }

    fun CountryWithProvincesStatPlainDbModel.toProvinceStatsPlain(): ProvinceStatPlainDbModelImpl {
        return ProvinceStatPlainDbModelImpl(
            id = provinceId,
            name = provinceName,
            lat = provinceLat,
            lng = provinceLng,
            confirmed = provinceConfirmed,
            deaths = provinceDeaths,
            recovered = provinceRecovered,
            timestamp = provinceTimestamp
        )
    }

    data class Params<ProvinceStatType>(
        val country: Country,
        val countryStat: List<Stat>,
        val provinceStats: Map<ProvinceId, ProvinceStatType>
    )
}

/**
 * Map a [List] of [CountryWithProvincesStatPlainDbModel] to a [CountryStat]
 * NOTE: [CountryWithProvincesStatPlainDbModel]s are supposed to refer all to the same County!!
 *
 * @author Davide Farella
 */
internal class CountryStatDbModelMapper(
    countryPlainMapper: SingleCountryPlainDbModelMapper,
    countyStatPlainMapper: CountryStatFromCountryWithProvinceStatPlainDbModelMapper,
    provinceStatMapper: ProvinceStatDbModelMapper
) : AbsCountryStatDbModelMapper<ProvinceStatPlainDbModel, CountryStat, ProvinceStat>(
    countryPlainMapper,
    countyStatPlainMapper,
    provinceStatMapper,
    buildProvinceStats = { list ->
        list.groupBy { it.provinceId }.mapValues { e ->
            provinceStatMapper {
                e.value.first().toProvinceStatsPlain().toEntity()
            }
        }
    },
    toCountryStatType = { CountryStat(country, countryStat, provinceStats) }
)


/**
 * Map a [List] of [CountryWithProvincesStatPlainDbModel] to a [CountryFullStat]
 * NOTE: [CountryWithProvincesStatPlainDbModel]s are supposed to refer all to the same County!!
 *
 * @author Davide Farella
 */
internal class CountryFullStatDbModelMapper(
    countryPlainMapper: SingleCountryPlainDbModelMapper,
    countyStatPlainMapper: CountryStatFromCountryWithProvinceStatPlainDbModelMapper,
    provinceStatMapper: ProvinceFullStatDbModelMapper
) : AbsCountryStatDbModelMapper<List<ProvinceStatPlainDbModel>, CountryFullStat, ProvinceFullStat>(
    countryPlainMapper,
    countyStatPlainMapper,
    provinceStatMapper,
    buildProvinceStats = { list ->
        list.groupBy { it.provinceTimestamp }.map { (_, list) ->
            list.first().provinceId to provinceStatMapper { list.map { it.toProvinceStatsPlain() }.toEntity() }
        }.toMap()
    },
    toCountryStatType = { CountryFullStat(country, countryStat, provinceStats) }
)

// region Plain mappers
/**
 * Map a [List] of [CountryStatPlainDbModel] to a [Country]
 * NOTE: [CountryStatPlainDbModel]s are supposed to refer all to the same County!!
 *
 * @author Davide Farella
 */
internal class SingleCountryPlainDbModelMapper(
    private val provincePlainMapper: ProvincePlainDbModelMapper
) : DatabaseModelMapper<List<CountryStatPlainDbModel>, Country> {

    override fun List<CountryStatPlainDbModel>.toEntity(): Country {
        val (id, name) = with(first()) { countryId to countryName }
        return Country(id, name, toProvinces().map(provincePlainMapper) { it.toEntity() })
    }

    private fun List<CountryStatPlainDbModel>.toProvinces() = map {
        ProvinceStatPlainDbModelImpl(
            id = it.provinceId,
            name = it.provinceName,
            lat = it.provinceLat,
            lng = it.provinceLng,
            confirmed = it.confirmed,
            deaths = it.deaths,
            recovered = it.recovered,
            timestamp = it.timestamp
        )
    }
}
// endregion
