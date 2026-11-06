package sa.factory.kadisalonbeautysal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import sa.factory.kadisalonbeautysal.model.KadiSalonItem

@Entity(tableName = "beauty_salon_items_cache")
data class KadiSalonEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val priceSar: Double,
    val badge: String,
    val category: String,
    val status: String
) {
    fun toDomain(): KadiSalonItem = KadiSalonItem(
        id = id,
        title = title,
        description = description,
        priceSar = priceSar,
        badge = badge,
        category = category,
        status = status
    )
}
