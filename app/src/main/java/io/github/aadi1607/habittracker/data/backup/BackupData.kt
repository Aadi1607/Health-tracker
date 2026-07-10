package io.github.aadi1607.habittracker.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val version: Int = 1,
    val habits: List<BackupHabit>,
    val completions: List<BackupCompletion>,
)

@Serializable
data class BackupHabit(
    val id: Long,
    val name: String,
    val emoji: String,
    val color: Long,
    val createdAt: Long,
    val dailyTarget: Int = 1,
    val goalPeriod: String = "daily",
    val sortOrder: Long = 0,
)

@Serializable
data class BackupCompletion(
    val habitId: Long,
    val date: String,
    val count: Int = 1,
)
