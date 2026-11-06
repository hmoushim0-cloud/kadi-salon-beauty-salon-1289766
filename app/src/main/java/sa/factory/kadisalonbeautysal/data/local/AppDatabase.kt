package sa.factory.kadisalonbeautysal.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [KadiSalonEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun beauty_salonDao(): KadiSalonDao
}
