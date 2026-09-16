package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "animation_projects")
data class AnimationProject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val text: String = "ANIMATION TEXT HZdotCOM",
    val animType: String = "Teks Melingkar",
    val speed: Float = 1.0f,
    val textColor: String = "#00E5FF", // Cyan
    val backgroundColor: String = "#121212", // Dark Charcoal
    val fontSize: Float = 48f,
    val resolutionWidth: Int = 720,
    val resolutionHeight: Int = 1280,
    val isCloudSynced: Boolean = false,
    val cloudProjectId: String? = null,
    val fontName: String = "Montserrat",
    val isShadowEnabled: Boolean = false,
    val shadowColor: String = "#000000",
    val shadowRadius: Float = 8.0f,
    val shadowDx: Float = 4.0f,
    val shadowDy: Float = 4.0f,
    val isOutlineEnabled: Boolean = false,
    val outlineColor: String = "#000000",
    val outlineWidth: Float = 4.0f,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Dao
interface ProjectDao {
    @Query("SELECT * FROM animation_projects ORDER BY lastUpdated DESC")
    fun getAllProjects(): Flow<List<AnimationProject>>

    @Query("SELECT * FROM animation_projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: Long): AnimationProject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: AnimationProject): Long

    @Query("DELETE FROM animation_projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)
}

@Database(entities = [AnimationProject::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
}
