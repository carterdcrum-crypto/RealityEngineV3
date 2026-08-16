package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.CallRecordEntity
import com.example.data.model.ClaimEntity
import com.example.data.model.MemoryEntity
import com.example.data.model.PersonEntity
import com.example.data.model.SignalEventEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
                    "reality_engine_v2.db"
                )
                    .addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }

            private suspend fun populateInitialData(database: RealityEngineDatabase) {
                val personDao = database.personDao()
                val memoryDao = database.memoryDao()
                val claimDao = database.claimDao()
                val callDao = database.callRecordDao()

                // Initial Known Person: Sarah (matching user prompt example)
                val sarahId = personDao.insertPerson(
                    PersonEntity(
                        name = "Sarah",
                        phoneNumber = "+1 (415) 890-2134",
                        email = "sarah.j@projectx.org",
                        organization = "Project X Initiative",
                        relationship = "Co-Lead",
                        avatarColorHex = "#F59E0B",
                        lastContactTimestamp = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000L), // 3 days ago
                        currentTopics = "Project X, Moving, Schedule",
                        openQuestions = "Did Project X launch?",
                        recentCommitment = "Send document Friday.",
                        notes = "Oversees product deliverable timelines and milestone logistics."
                    )
                )

                val marcusId = personDao.insertPerson(
                    PersonEntity(
                        name = "Dr. Marcus Vance",
                        phoneNumber = "+1 (650) 412-9901",
                        email = "m.vance@bioventures.io",
                        organization = "BioVentures Capital",
                        relationship = "Series A Investor",
                        avatarColorHex = "#00E5FF",
                        lastContactTimestamp = System.currentTimeMillis() - (1 * 24 * 60 * 60 * 1000L),
                        currentTopics = "Valuation Cap, IP Rights, Board Seat",
                        openQuestions = "Has the syndicate term sheet been countersigned?",
                        recentCommitment = "Review revised valuation terms by Monday 9am.",
                        notes = "Analytical, questions operational expenditure assumptions."
                    )
                )

                val elenaId = personDao.insertPerson(
                    PersonEntity(
                        name = "Elena Rostova",
                        phoneNumber = "+1 (212) 773-8109",
                        email = "elena@rostovacounsel.com",
                        organization = "Rostova Law Partners",
                        relationship = "Legal Counsel",
                        avatarColorHex = "#8B5CF6",
                        lastContactTimestamp = System.currentTimeMillis() - (5 * 24 * 60 * 60 * 1000L),
                        currentTopics = "Master Services Agreement, Indemnity Clause",
                        openQuestions = "What is the liability cap threshold?",
                        recentCommitment = "Redline Section 8.4 on intellectual property indemnity.",
                        notes = "Direct and precise. Prefers clear confirmation on terms."
                    )
                )

                val davidId = personDao.insertPerson(
                    PersonEntity(
                        name = "David Kim",
                        phoneNumber = "+1 (206) 555-4321",
                        email = "david@apexcloud.net",
                        organization = "Apex Cloud Systems",
                        relationship = "Infrastructure Partner",
                        avatarColorHex = "#10B981",
                        lastContactTimestamp = System.currentTimeMillis() - (2 * 24 * 60 * 60 * 1000L),
                        currentTopics = "GPU Cluster Allocation, SLA guarantees",
                        openQuestions = "Can we guarantee 99.99% uptime during the Q4 spike?",
                        recentCommitment = "Provision 8x H100 nodes by Thursday evening.",
                        notes = "Solid engineering contact, responsive to technical probes."
                    )
                )

                // Sarah's previous claims and memories (for prompt scenario)
                claimDao.insertClaim(
                    ClaimEntity(
                        personId = sarahId,
                        personName = "Sarah",
                        currentStatement = "I started the project in March.",
                        previousStatement = "Project kicked off early spring.",
                        context = "Project X kickoff timeline confirmation",
                        inconsistencyConfidence = 0,
                        hasInconsistency = false,
                        category = "Timeline",
                        timestamp = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000L)
                    )
                )

                memoryDao.insertMemory(
                    MemoryEntity(
                        personId = sarahId,
                        personName = "Sarah",
                        statement = "Sarah says she is moving in October.",
                        state = "OBSERVED",
                        provenance = "Call on Aug 12, 14:22 · Audio stream",
                        category = "Personal",
                        timestamp = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000L)
                    )
                )

                memoryDao.insertMemory(
                    MemoryEntity(
                        personId = sarahId,
                        personName = "Sarah",
                        statement = "Committed to delivering final architecture review by Friday.",
                        state = "CONFIRMED",
                        provenance = "Call on Aug 12, 14:26",
                        category = "Professional",
                        timestamp = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000L)
                    )
                )

                memoryDao.insertMemory(
                    MemoryEntity(
                        personId = marcusId,
                        personName = "Dr. Marcus Vance",
                        statement = "BioVentures target deployment tranche is \$2.5M - \$4M.",
                        state = "CONFIRMED",
                        provenance = "Call on Aug 14, 11:05",
                        category = "Financial",
                        timestamp = System.currentTimeMillis() - (1 * 24 * 60 * 60 * 1000L)
                    )
                )

                // Previous Call Record for Sarah
                callDao.insertCallRecord(
                    CallRecordEntity(
                        personId = sarahId,
                        personName = "Sarah",
                        phoneNumber = "+1 (415) 890-2134",
                        callType = "INCOMING",
                        timestamp = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000L),
                        durationSeconds = 763, // 00:12:43
                        topic = "Project X Milestone Review & Kickoff",
                        summary = "Aligned on Q3 deliverable milestones. Discussed schedule adjustments and confirmed document delivery.",
                        importantStatements = "Sarah confirmed initial prototype was tested. Mentioned relocating in October.",
                        extractedClaims = "Stated initial project kickoff was in March.",
                        extractedCommitments = "Send document Friday.",
                        questionsAnswered = "Confirmed hardware requirements.",
                        questionsUnresolved = "Did Project X launch?",
                        inconsistencies = "Minor discrepancy in schedule delivery date.",
                        deceptionSummary = "Low baseline signals (18%).",
                        newMemoriesCreated = "Moving in October; Friday doc commitment.",
                        recommendedFollowUps = "Follow up on Friday if document is not received.",
                        strategiesUsed = "BONDING, COGNITIVE PROBE, MIRRORING",
                        deceptionAvgScore = 22,
                        transcriptJson = "[]"
                    )
                )
            }
        }
    }
}
