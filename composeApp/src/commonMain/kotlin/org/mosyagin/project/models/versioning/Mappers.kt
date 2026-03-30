package org.mosyagin.project.models.versioning

import org.mosyagin.project.Prop as DbProp
import org.mosyagin.project.ScriptFile as DbScriptFile
import org.mosyagin.project.SceneUserData as DbSceneUserData
import org.mosyagin.project.SceneVersion as DbSceneVersion
import org.mosyagin.project.Actor as DbActor
import org.mosyagin.project.GetSceneById
import org.mosyagin.project.GetScenesByProject

fun DbScriptFile.toDomain(): ScriptFile = ScriptFile(
    id = id,
    projectId = projectId,
    seriesNumber = seriesNumber,
    title = title,
    filePath = filePath,
    createdAt = createdAt,
    previousVersionId = previousVersionId,
    revisionColor = RevisionColor.fromString(revisionColor),
    uploadedBy = uploadedBy
)

fun DbSceneUserData.toDomain(): SceneUserData = SceneUserData(
    id = id,
    projectId = projectId,
    seriesNumber = seriesNumber,
    sceneNumber = sceneNumber,
    location = location,
    isInterior = isInterior == 1L,
    timeOfDay = timeOfDay,
    notes = notes,
    needsReview = needsReview == 1L
)

fun DbSceneVersion.toDomain(): SceneVersion = SceneVersion(
    id = id,
    scriptFileId = scriptFileId,
    sceneUserDataId = sceneUserDataId,
    content = content,
    contentHash = contentHash,
    positionIndex = positionIndex
)

fun DbProp.toDomain(): Prop = Prop(
    id = id,
    sceneUserDataId = sceneUserDataId,
    name = name,
    anchor = anchor,
    status = PropStatus.fromString(status),
    startOffset = startOffset,
    endOffset = endOffset,
    isOrphaned = orphaned == 1L
)

fun DbActor.toDomain(): SceneCharacter = SceneCharacter(
    id = id,
    projectId = projectId,
    name = name
)

// Мапперы для комплексных объектов (результатов JOIN)

fun GetSceneById.toSceneWithUserData(props: List<Prop>, characters: List<SceneCharacter>): SceneWithUserData = 
    SceneWithUserData(
        userData = SceneUserData(
            id = id,
            projectId = projectId,
            seriesNumber = seriesNumber,
            sceneNumber = sceneNumber,
            location = location,
            isInterior = isInterior == 1L,
            timeOfDay = timeOfDay,
            notes = notes,
            needsReview = needsReview == 1L
        ),
        version = SceneVersion(
            id = 0, // ID версии не всегда доступен в плоских запросах, если нужно - добавим в SQL
            scriptFileId = 0, 
            sceneUserDataId = id,
            content = content,
            contentHash = contentHash,
            positionIndex = 0
        ),
        props = props,
        characters = characters
    )

fun GetScenesByProject.toUserData(): SceneUserData = SceneUserData(
    id = id,
    projectId = projectId,
    seriesNumber = seriesNumber,
    sceneNumber = sceneNumber,
    location = location,
    isInterior = isInterior == 1L,
    timeOfDay = timeOfDay,
    notes = notes,
    needsReview = needsReview == 1L
)
