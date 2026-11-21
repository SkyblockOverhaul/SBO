package net.sbo.mod.utils.version

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ModrinthVersion(
    @SerialName("version_number")
    val versionNumber: String,
    @SerialName("project_id")
    val projectId: String,
    val id: String
)

