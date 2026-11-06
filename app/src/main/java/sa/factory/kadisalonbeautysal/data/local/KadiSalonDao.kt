package sa.factory.kadisalonbeautysal.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface KadiSalonDao {
    @Query("SELECT * FROM beauty_salon_items_cache ORDER BY id ASC")
    fun observeAllItems(): Flow<List<KadiSalonEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<KadiSalonEntity>)

    @Query("DELETE FROM beauty_salon_items_cache")
    suspend fun clearAll()
}
