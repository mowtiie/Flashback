package com.mowtiie.flashback.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds the per-note answer type and its own wrong options. Both stay null, so existing notes behave as before. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `notes` ADD COLUMN `answerMode` INTEGER")
        db.execSQL("ALTER TABLE `notes` ADD COLUMN `choices` TEXT")
    }
}
