package com.example.data.contacts

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import android.util.Log
import com.example.data.local.PersonDao
import com.example.data.model.PersonEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DeviceContact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val email: String? = null,
    val organization: String? = null
)

class DeviceContactsManager(
    private val context: Context,
    private val personDao: PersonDao
) {
    companion object {
        private const val TAG = "DeviceContactsManager"
    }

    suspend fun fetchDeviceContacts(query: String = ""): List<DeviceContact> = withContext(Dispatchers.IO) {
        val contactsList = mutableListOf<DeviceContact>()
        val contentResolver: ContentResolver = context.contentResolver

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        val selection = if (query.isBlank()) {
            "${ContactsContract.CommonDataKinds.Phone.NUMBER} IS NOT NULL"
        } else {
            "(${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ? OR ${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?) AND ${ContactsContract.CommonDataKinds.Phone.NUMBER} IS NOT NULL"
        }

        val selectionArgs = if (query.isBlank()) {
            null
        } else {
            arrayOf("%$query%", "%$query%")
        }

        try {
            val cursor: Cursor? = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.use {
                val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                val seenNumbers = mutableSetOf<String>()

                while (it.moveToNext()) {
                    val id = if (idIndex != -1) it.getString(idIndex) ?: "" else ""
                    val name = if (nameIndex != -1) it.getString(nameIndex) ?: "Unknown" else "Unknown"
                    val rawNumber = if (numberIndex != -1) it.getString(numberIndex) ?: "" else ""
                    val cleanNumber = rawNumber.replace("[^0-9+]".toRegex(), "")

                    if (cleanNumber.isNotEmpty() && !seenNumbers.contains(cleanNumber)) {
                        seenNumbers.add(cleanNumber)
                        contactsList.add(
                            DeviceContact(
                                id = id,
                                name = name,
                                phoneNumber = rawNumber
                            )
                        )
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for reading contacts: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching device contacts: ${e.message}", e)
        }

        contactsList
    }

    suspend fun syncDeviceContactsToRoom(): Int = withContext(Dispatchers.IO) {
        val deviceContacts = fetchDeviceContacts()
        var importedCount = 0

        for (contact in deviceContacts) {
            val existing = personDao.getPersonByPhone(contact.phoneNumber)
            if (existing == null) {
                val cleanNumber = contact.phoneNumber.replace("[^0-9+]".toRegex(), "")
                val colors = listOf("#00E5FF", "#F59E0B", "#10B981", "#8B5CF6", "#EC4899", "#3B82F6")
                val avatarColor = colors[(contact.name.hashCode().coerceAtLeast(0)) % colors.size]

                personDao.insertPerson(
                    PersonEntity(
                        name = contact.name,
                        phoneNumber = contact.phoneNumber,
                        email = contact.email ?: "",
                        organization = contact.organization ?: "",
                        relationship = "Contact",
                        avatarColorHex = avatarColor,
                        lastContactTimestamp = 0L,
                        currentTopics = "",
                        openQuestions = "",
                        recentCommitment = "",
                        notes = "Imported from device contacts."
                    )
                )
                importedCount++
            }
        }
        importedCount
    }
}
