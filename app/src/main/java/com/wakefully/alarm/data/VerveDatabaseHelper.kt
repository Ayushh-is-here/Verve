package com.wakefully.alarm.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Production SQLite database helper for Verve.
 * Enforces relational integrity, foreign keys, and persistent disk storage
 * so alarms and barcodes survive app swiping, process death, and device reboots.
 */
class VerveDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    companion object {
        const val DATABASE_NAME = "verve_alarm.db"
        const val DATABASE_VERSION = 1

        const val TABLE_BARCODES = "barcodes"
        const val TABLE_ALARMS = "alarms"
        const val TABLE_STREAKS = "streak_stats"
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        // 1. Barcodes Table
        db.execSQL("""
            CREATE TABLE $TABLE_BARCODES (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                value TEXT NOT NULL,
                format INTEGER NOT NULL DEFAULT 32
            );
        """.trimIndent())

        // 2. Alarms Table with Foreign Key to Barcodes
        db.execSQL("""
            CREATE TABLE $TABLE_ALARMS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                hour_of_day INTEGER NOT NULL,
                minute INTEGER NOT NULL,
                repeat_days TEXT NOT NULL DEFAULT '',
                mission_type TEXT NOT NULL DEFAULT 'MATH',
                barcode_id INTEGER,
                enabled INTEGER NOT NULL DEFAULT 1,
                label TEXT NOT NULL DEFAULT '',
                tone_name TEXT NOT NULL DEFAULT 'Radiant Dawn',
                tone_uri TEXT,
                FOREIGN KEY (barcode_id) REFERENCES $TABLE_BARCODES(id) ON DELETE RESTRICT
            );
        """.trimIndent())

        // 3. Streak Stats Table
        db.execSQL("""
            CREATE TABLE $TABLE_STREAKS (
                id INTEGER PRIMARY KEY CHECK (id = 1),
                current_streak INTEGER NOT NULL DEFAULT 3,
                longest_streak INTEGER NOT NULL DEFAULT 7,
                total_wakeups INTEGER NOT NULL DEFAULT 14,
                is_today_verified INTEGER NOT NULL DEFAULT 0
            );
        """.trimIndent())

        // Seed default sample data once on initial database creation
        db.execSQL("""
            INSERT INTO $TABLE_BARCODES (id, name, value, format)
            VALUES (1, 'Ensuite C-09 (Ceramic Cleanser)', '4006381333931', 32);
        """.trimIndent())

        db.execSQL("""
            INSERT INTO $TABLE_ALARMS (id, hour_of_day, minute, repeat_days, mission_type, barcode_id, enabled, label, tone_name, tone_uri)
            VALUES (1, 6, 30, '1,2,3,4,5', 'BARCODE', 1, 1, 'Morning Routine', 'Radiant Dawn', NULL);
        """.trimIndent())

        db.execSQL("""
            INSERT INTO $TABLE_ALARMS (id, hour_of_day, minute, repeat_days, mission_type, barcode_id, enabled, label, tone_name, tone_uri)
            VALUES (2, 7, 15, '6,7', 'MATH', NULL, 0, 'Weekend Awakening', 'Radiant Dawn', NULL);
        """.trimIndent())

        db.execSQL("""
            INSERT INTO $TABLE_STREAKS (id, current_streak, longest_streak, total_wakeups, is_today_verified)
            VALUES (1, 3, 7, 14, 0);
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Migration logic for future database versions
    }
}
