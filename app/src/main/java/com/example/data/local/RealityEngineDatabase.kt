package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.CallRecordEntity
import com.example.data.model.ClaimEntity
import com.example.data.model.MemoryEntity
import com.example.data.model.PersonEntity
import com.example.data.model.SignalEventEntity
import kotlinx.coroutines.CoroutineScope

@Database(
    entities = [
        PersonEntity::class,
        CallRecordEntity::class,
        MemoryEntity::class,
        ClaimEntity::class,
        SignalEventEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class RealityEngineDatabase : RoomDatabase() {

    abstract fun personDao(): PersonDao
    abstract fun callRecordDao(): CallRecordDao
    abstract fun memoryDao(): MemoryDao
    abstract fun claimDao(): ClaimDao
    abstract fun signalEventDao(): SignalEventDao

    companion object {
        @Volatile
        private var INSTANCE: RealityEngineDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): RealityEngineDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RealityEngineDatabase::class.java,
                    "reality_engine_v3.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
