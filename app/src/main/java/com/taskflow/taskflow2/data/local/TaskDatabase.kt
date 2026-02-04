package com.taskflow.taskflow2.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TaskEntity::class, TaskColor::class],  // ← 直接加進去
    version = 1,                                        // ← 保持 1，不升級
    exportSchema = false
)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: TaskDatabase? = null

        fun getInstance(context: Context): TaskDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    TaskDatabase::class.java,
                    "taskflow.db"
                )
                    .fallbackToDestructiveMigration()  // ← ⚠️ 只在開發期用！
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
