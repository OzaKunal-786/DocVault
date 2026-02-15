// Location: app/src/main/java/com/docvault/data/database/AppDatabase.kt

package com.docvault.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [DocumentEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun documentDao(): DocumentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private const val DATABASE_NAME = "docvault_encrypted.db"

        /**
         * Get (or create) the encrypted database instance.
         *
         * @param context Application context
         * @param passphrase The encryption key — derived from user's PIN
         *                   combined with device-specific Android Keystore key.
         *                   This is NOT the raw PIN — see EncryptionManager.
         */
        fun getInstance(context: Context, passphrase: ByteArray): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context, passphrase).also {
                    INSTANCE = it
                }
            }
        }

        private fun buildDatabase(context: Context, passphrase: ByteArray): AppDatabase {
            // SQLCipher factory encrypts the entire database file
            val supportFactory = SupportFactory(passphrase)

            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .openHelperFactory(supportFactory)  // THIS is what encrypts it
                .fallbackToDestructiveMigration()    // for v1, OK to reset on schema change
                .build()
        }

        /**
         * Close database when app is being destroyed.
         * Important for SQLCipher to properly clean up.
         */
        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}