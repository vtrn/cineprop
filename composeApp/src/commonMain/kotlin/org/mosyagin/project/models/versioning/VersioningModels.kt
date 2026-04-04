package org.mosyagin.project.models.versioning

enum class RevisionColor(val hexCode: String, val displayName: String) {
    WHITE("#FFFFFF", "Белые страницы"),
    BLUE("#E3F2FD", "Синие поправки"),
    PINK("#FCE4EC", "Розовые поправки"),
    YELLOW("#FFFDE7", "Желтые поправки"),
    GREEN("#E8F5E9", "Зеленые поправки");

    companion object {
        fun fromString(value: String?): RevisionColor =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: WHITE
    }
}

enum class PropStatus(val displayName: String) {
    PLANNED("Найти"),
    BOUGHT("Куплено"),
    READY("Готово"),
    LOST("Утеряно");

    companion object {
        fun fromString(value: String): PropStatus =
            when (value) {
                "Найти" -> PLANNED
                "Куплено" -> BOUGHT
                "Готово" -> READY
                "Утеряно" -> LOST
                else -> PLANNED
            }
        
        fun PropStatus.toDbString(): String =
            when (this) {
                PLANNED -> "Найти"
                BOUGHT -> "Куплено"
                READY -> "Готово"
                LOST -> "Утеряно"
            }
    }
}

data class ScriptFile(
    val id: Long,
    val projectId: Long,
    val seriesNumber: Long,
    val title: String,
    val filePath: String,
    val createdAt: Long,
    val previousVersionId: Long?,
    val revisionColor: RevisionColor,
    val uploadedBy: String?
)

data class SceneUserData(
    val id: Long,
    val projectId: Long,
    val seriesNumber: Long,
    val sceneNumber: String,
    val location: String,
    val isInterior: Boolean,
    val timeOfDay: String,
    val notes: String?,
    val needsReview: Boolean
)

data class SceneVersion(
    val id: Long,
    val scriptFileId: Long,
    val sceneUserDataId: Long,
    val content: String,
    val contentHash: String,
    val positionIndex: Long
)

data class Prop(
    val id: Long,
    val sceneUserDataId: Long,
    val name: String,
    val anchor: String,
    val status: PropStatus,
    val category: String = "Прочее",
    val propType: String = "Обстановочный",
    val note: String? = null,
    val photoPath: String? = null,
    val isCrossCutting: Boolean = false,
    val quantity: Int = 1,
    val actorId: Long? = null,
    val startOffset: Long,
    val endOffset: Long,
    val isOrphaned: Boolean,
    val groupId: Long? = null // ДОБАВЛЕНО
)

data class SceneCharacter(
    val id: Long,
    val projectId: Long,
    val name: String
)

data class SceneWithUserData(
    val userData: SceneUserData,
    val version: SceneVersion,
    val props: List<Prop>,
    val characters: List<SceneCharacter>
)
