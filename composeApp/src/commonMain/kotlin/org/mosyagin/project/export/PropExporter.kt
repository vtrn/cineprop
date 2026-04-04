package org.mosyagin.project.export

import org.mosyagin.project.repository.PropWithScene
import org.mosyagin.project.ui.components.props.ExportFormat
import org.mosyagin.project.ui.components.props.ExportGrouping

/**
 * Интерфейс для экспорта данных реквизита.
 * Реализуется отдельно для каждой платформы (Android/JVM).
 */
interface PropExporter {
    /**
     * Генерирует файл экспорта и возвращает его в виде массива байтов.
     */
    fun export(
        projectName: String,
        grouping: ExportGrouping,
        format: ExportFormat,
        props: List<PropWithScene>
    ): ByteArray
}
