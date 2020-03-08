package studio.forface.covid.data.remote.mapper

import studio.forface.covid.data.remote.model.ProvinceFullStatApiModel
import studio.forface.covid.data.remote.model.ProvinceStatApiModel
import studio.forface.covid.domain.entity.Province
import studio.forface.covid.domain.entity.ProvinceFullStat
import studio.forface.covid.domain.entity.ProvinceStat
import studio.forface.covid.domain.invoke
import studio.forface.covid.domain.mapper.map

internal class ProvinceStatApiModelMapper(
    private val provinceMapper: ProvinceApiModelMapper,
    private val statParamsMapper: StatParamsMapper
) : ApiModelMapper<ProvinceStatApiModel, ProvinceStat> {

    override fun ProvinceStatApiModel.toEntity() = ProvinceStat(
        province = provinceMapper { this@toEntity.toEntity() },
        stat = statParamsMapper { StatParams(confirmed, deaths, recovered, lastUpdate).toEntity() }
    )
}

internal class ProvinceFullStatApiModelMapper(
    private val provinceMapper: ProvinceApiModelMapper,
    private val statMapper: StatApiModelMapper,
    private val statParamsMapper: StatParamsMapper
) : ApiModelMapper<ProvinceFullStatApiModel, ProvinceFullStat> {

    override fun ProvinceFullStatApiModel.toEntity() = ProvinceFullStat(
        province = provinceMapper { this@toEntity.toEntity() },
        stat = statParamsMapper { StatParams(confirmed, deaths, recovered, lastUpdate).toEntity() },
        otherStats = stats.map(statMapper) { it.toEntity() }
    )
}

// TODO replace ProvinceStatApiModel with ProvinceApiModel, which does not exist ATM
internal class ProvinceApiModelMapper(
    private val idMapper: IdApiModelMapper,
    private val nameMapper: NameApiModelMapper,
    private val locationMapper: LocationApiModelMapper
) : ApiModelMapper<ProvinceStatApiModel, Province> {

    override fun ProvinceStatApiModel.toEntity() = Province(
        id = idMapper { id.toEntity() },
        name = nameMapper { name.toEntity() },
        location = locationMapper { LocationParams(lat, lng).toEntity() }
    )

    fun ProvinceFullStatApiModel.toEntity() = Province(
        id = idMapper { id.toEntity() },
        name = nameMapper { name.toEntity() },
        location = locationMapper { LocationParams(lat, lng).toEntity() }
    )
}
