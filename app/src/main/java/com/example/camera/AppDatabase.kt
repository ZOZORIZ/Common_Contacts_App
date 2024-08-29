package com.example.camera

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.firebase.firestore.FirebaseFirestore

// Migration from version 1 to version 2
// Define migration from version 1 to 2
val MIGRATION_1_2 = object : Migration(8,1) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Example SQL command to add a new column
        // Adjust this command based on your schema changes





    }


}






@Database(entities = [Contact::class], version = 8, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "contact_database"
                )
                    .fallbackToDestructiveMigration() // Use this to avoid migration issues
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
    fun migrateIds() {
        val firestore = FirebaseFirestore.getInstance()
        val collectionRef = firestore.collection("contacts") // Replace with your collection name

        collectionRef.get().addOnSuccessListener { result ->
            for (document in result) {
                val idLong = document.getLong("id") // Fetch the id as Long
                if (idLong != null) {
                    val idString = idLong.toString() // Convert Long to String

                    // Create a new map with the updated id
                    val updatedData = document.data.toMutableMap()
                    updatedData["id"] = idString

                    // Update the document with the new data
                    document.reference.set(updatedData)
                        .addOnSuccessListener {
                            println("Successfully updated document: ${document.id}")
                        }
                        .addOnFailureListener { e ->
                            println("Error updating document: ${document.id}, $e")
                        }
                }
            }
        }.addOnFailureListener { e ->
            println("Error fetching documents: $e")
        }
    }

    fun main() {
        migrateIds()
    }
}
