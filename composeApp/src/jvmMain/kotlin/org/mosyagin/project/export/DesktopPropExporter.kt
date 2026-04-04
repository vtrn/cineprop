package org.mosyagin.project.export

import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.mosyagin.project.repository.PropWithScene
import org.mosyagin.project.ui.components.props.ExportFormat
import org.mosyagin.project.ui.components.props.ExportGrouping
import java.io.ByteArrayOutputStream

/**
 * Реализация экспортера для Desktop (JVM).
 * Использует ту же логику Apache POI, что и Android.
 */
class DesktopPropExporter : PropExporter {

    override fun export(
        projectName: String,
        grouping: ExportGrouping,
        format: ExportFormat,
        props: List<PropWithScene>
    ): ByteArray {
        return when (format) {
            ExportFormat.EXCEL -> generateExcel(projectName, grouping, props)
            ExportFormat.PDF -> ByteArray(0) // PDF в разработке
        }
    }

    private fun generateExcel(
        projectName: String,
        groupingType: ExportGrouping,
        props: List<PropWithScene>
    ): ByteArray {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Реквизит")

        // Стили
        val titleStyle = workbook.createCellStyle().apply {
            alignment = HorizontalAlignment.CENTER
            val font = workbook.createFont().apply {
                bold = true
                fontHeightInPoints = 16.toShort()
            }
            setFont(font)
        }

        val headerStyle = workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.GREY_25_PERCENT.getIndex()
            fillPattern = FillPatternType.SOLID_FOREGROUND
            borderBottom = BorderStyle.THIN
            borderTop = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
            alignment = HorizontalAlignment.CENTER
            val font = workbook.createFont().apply { bold = true }
            setFont(font)
        }

        val dataStyle = workbook.createCellStyle().apply {
            borderBottom = BorderStyle.THIN
            borderTop = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
            wrapText = true
            verticalAlignment = VerticalAlignment.TOP
        }

        val shiftHeaderStyle = workbook.createCellStyle().apply {
            alignment = HorizontalAlignment.CENTER
            val font = workbook.createFont().apply {
                bold = true
                fontHeightInPoints = 12.toShort()
                color = IndexedColors.BLUE.getIndex()
            }
            setFont(font)
        }

        var currentRow = 0

        // Название проекта
        val titleRow = sheet.createRow(currentRow++)
        titleRow.createCell(0).apply {
            setCellValue(projectName.uppercase())
            setCellStyle(titleStyle)
        }
        sheet.addMergedRegion(org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 5))

        // Версия
        val versionRow = sheet.createRow(currentRow++)
        versionRow.createCell(0).apply {
            setCellValue(if (groupingType == ExportGrouping.BY_KPP) "ВЕРСИЯ КПП" else "ВЕРСИЯ СЦЕНАРИЯ")
            setCellStyle(dataStyle)
        }

        currentRow++

        // Группировка
        val groupedData = if (groupingType == ExportGrouping.BY_KPP) {
            props.groupBy { it.shiftNumber ?: 0L }
        } else {
            props.groupBy { it.seriesNumber }
        }

        groupedData.toSortedMap().forEach { (key, items) ->
            // Заголовок группы
            val groupRow = sheet.createRow(currentRow++)
            groupRow.createCell(0).apply {
                setCellValue(if (groupingType == ExportGrouping.BY_KPP) "СМЕНА $key ${items.firstOrNull()?.shiftDate ?: ""}" else "СЕРИЯ $key")
                setCellStyle(shiftHeaderStyle)
            }
            sheet.addMergedRegion(org.apache.poi.ss.util.CellRangeAddress(currentRow - 1, currentRow - 1, 0, 5))

            // Шапка таблицы
            val hRow = sheet.createRow(currentRow++)
            val headers = listOf("№ Сцены", "Наименование", "Кол-во", "Заметки", "Якорь", "Фото")
            headers.forEachIndexed { i, h ->
                hRow.createCell(i).apply {
                    setCellValue(h)
                    setCellStyle(headerStyle)
                }
            }

            // Данные
            items.forEach { p ->
                val row = sheet.createRow(currentRow++)
                row.createCell(0).apply { setCellValue("${p.seriesNumber}-${p.sceneNumber}"); setCellStyle(dataStyle) }
                row.createCell(1).apply { setCellValue(p.name); setCellStyle(dataStyle) }
                row.createCell(2).apply { setCellValue(p.quantity.toDouble()); setCellStyle(dataStyle) }
                row.createCell(3).apply { setCellValue(p.note ?: ""); setCellStyle(dataStyle) }
                row.createCell(4).apply { setCellValue(p.anchor); setCellStyle(dataStyle) }
                row.createCell(5).apply { setCellValue(""); setCellStyle(dataStyle) }
            }
            currentRow++
        }

        // Авто-ширина (примерная)
        sheet.setColumnWidth(0, 12 * 256)
        sheet.setColumnWidth(1, 25 * 256)
        sheet.setColumnWidth(4, 45 * 256)

        val out = ByteArrayOutputStream()
        workbook.write(out)
        workbook.close()
        return out.toByteArray()
    }
}
