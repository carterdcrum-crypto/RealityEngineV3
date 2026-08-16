package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CallRecordEntity
import com.example.data.model.ClaimEntity
import com.example.data.model.MemoryEntity
import com.example.data.model.PersonEntity
import com.example.data.model.SignalEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {
    @Query("SELECT * FROM people ORDER BY lastContactTimestamp DESC")
    fun getAllPeopleFlow(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM people ORDER BY lastContactTimestamp DESC")
    suspend fun getAllPeople(): List<PersonEntity>

    @Query("SELECT * FROM people WHERE id = :id LIMIT 1")
    suspend fun getPersonById(id: Long): PersonEntity?

    @Query("SELECT * FROM people WHERE phoneNumber = :phoneNumber OR phoneNumber LIKE '%' || :phoneNumber LIMIT 1")
    suspend fun getPersonByPhone(phoneNumber: String): PersonEntity?

    @Query("SELECT * FROM people WHERE name LIKE '%' || :query || '%' OR phoneNumber LIKE '%' || :query || '%'")
    fun searchPeople(query: String): Flow<List<PersonEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(person: PersonEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeople(people: List<PersonEntity>)

    @Update
    suspend fun updatePerson(person: PersonEntity)

    @Delete
    suspend fun deletePerson(person: PersonEntity)
}

@Dao
interface CallRecordDao {
    @Query("SELECT * FROM call_records ORDER BY timestamp DESC")
    fun getAllCallRecordsFlow(): Flow<List<CallRecordEntity>>

    @Query("SELECT * FROM call_records WHERE personId = :personId ORDER BY timestamp DESC")
    fun getCallsForPersonFlow(personId: Long): Flow<List<CallRecordEntity>>

    @Query("SELECT * FROM call_records WHERE id = :id LIMIT 1")
    suspend fun getCallRecordById(id: Long): CallRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallRecord(callRecord: CallRecordEntity): Long

    @Update
    suspend fun updateCallRecord(callRecord: CallRecordEntity)

    @Delete
    suspend fun deleteCallRecord(callRecord: CallRecordEntity)
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories WHERE isDismissed = 0 ORDER BY timestamp DESC")
    fun getAllActiveMemoriesFlow(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE personId = :personId AND isDismissed = 0 ORDER BY timestamp DESC")
    fun getMemoriesForPersonFlow(personId: Long): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE personName = :personName AND isDismissed = 0 ORDER BY timestamp DESC")
    suspend fun getMemoriesForPerson(personName: String): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE state = :state AND isDismissed = 0 ORDER BY timestamp DESC")
    fun getMemoriesByStateFlow(state: String): Flow<List<MemoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemories(memories: List<MemoryEntity>)

    @Update
    suspend fun updateMemory(memory: MemoryEntity)

    @Query("UPDATE memories SET state = :newState WHERE id = :id")
    suspend fun updateMemoryState(id: Long, newState: String)

    @Query("UPDATE memories SET isDismissed = 1 WHERE id = :id")
    suspend fun dismissMemory(id: Long)

    @Delete
    suspend fun deleteMemory(memory: MemoryEntity)
}

@Dao
interface ClaimDao {
    @Query("SELECT * FROM claims ORDER BY timestamp DESC")
    fun getAllClaimsFlow(): Flow<List<ClaimEntity>>

    @Query("SELECT * FROM claims WHERE personId = :personId ORDER BY timestamp DESC")
    fun getClaimsForPersonFlow(personId: Long): Flow<List<ClaimEntity>>

    @Query("SELECT * FROM claims WHERE personName = :personName ORDER BY timestamp DESC")
    suspend fun getClaimsForPerson(personName: String): List<ClaimEntity>

    @Query("SELECT * FROM claims WHERE hasInconsistency = 1 ORDER BY timestamp DESC")
    fun getInconsistentClaimsFlow(): Flow<List<ClaimEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClaim(claim: ClaimEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClaims(claims: List<ClaimEntity>)

    @Update
    suspend fun updateClaim(claim: ClaimEntity)

    @Delete
    suspend fun deleteClaim(claim: ClaimEntity)
}

@Dao
interface SignalEventDao {
    @Query("SELECT * FROM signal_events ORDER BY timestamp DESC")
    fun getAllSignalsFlow(): Flow<List<SignalEventEntity>>

    @Query("SELECT * FROM signal_events WHERE callRecordId = :callId ORDER BY timestamp ASC")
    suspend fun getSignalsForCall(callId: Long): List<SignalEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignalEvent(signal: SignalEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignalEvents(signals: List<SignalEventEntity>)
}
