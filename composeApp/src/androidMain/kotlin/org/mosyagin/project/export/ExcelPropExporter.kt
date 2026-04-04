package org.mosyagin.project.export

import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.mosyagin.project.repository.PropWithScene
import org.mosyagin.project.ui.components.props.ExportFormat
import org.mosyagin.project.ui.components.props.ExportGrouping
import java.io.ByteArrayOutputStream

/**
 * Реализация экспортера для Android.
 * На данный момент поддерживает только формат Excel.
 */
class AndroidPropExporter : PropExporter {

    override fun export(
        projectName: String,
        grouping: ExportGrouping,
        format: ExportFormat,
        props: List<PropWithScene>
    ): ByteArray {
        return when (format) {
            ExportFormat.EXCEL -> generateExcel(projectName, grouping, props)
            ExportFormat.PDF -> {
                // PDF будет реализован позже, пока возвращаем пустой массив или бросаем исключение
                ByteArray(0)
            }
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
        val titleStyle = createTitleStyle(workbook)
        val headerStyle = createHeaderStyle(workbook)
        val dataStyle = createDataStyle(workbook)
        val shiftHeaderStyle = createShiftHeaderStyle(workbook)

        var currentRow = 0

        // 1. НАЗВАНИЕ ПРОЕКТА
        val titleRow = sheet.createRow(currentRow++)
        val titleCell = titleRow.createCell(0)
        titleCell.setCellValue(projectName.uppercase())
        titleCell.setCellStyle(titleStyle)
        sheet.addMergedRegion(org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 5))

        // 2. ВЕРСИЯ
        val versionRow = sheet.createRow(currentRow++)
        val versionCell = versionRow.createCell(0)
        versionCell.setCellValue(if (groupingType == ExportGrouping.BY_KPP) "ВЕРСИЯ КПП" else "ВЕРСИЯ СЦЕНАРИЯ")
        versionCell.setCellStyle(dataStyle)

        currentRow++ // Пропуск строки

        // Группировка данных
        val groupedData = if (groupingType == ExportGrouping.BY_KPP) {
            props.groupBy { it.shiftNumber ?: 0L }
        } else {
            props.groupBy { it.seriesNumber }
        }

        groupedData.toSortedMap().forEach { (groupKey, groupProps) ->
            // 3. ЗАГОЛОВОК СМЕНЫ / СЕРИИ
            val groupHeaderRow = sheet.createRow(currentRow++)
            val groupHeaderCell = groupHeaderRow.createCell(0)
            
            if (groupingType == ExportGrouping.BY_KPP) {
                val date = groupProps.firstOrNull()?.shiftDate ?: ""
                groupHeaderCell.setCellValue("СМЕНА $groupKey $date")
            } else {
                groupHeaderCell.setCellValue("СЕРИЯ $groupKey")
            }
            groupHeaderCell.setCellStyle(shiftHeaderStyle)
            sheet.addMergedRegion(org.apache.poi.ss.util.CellRangeAddress(currentRow - 1, currentRow - 1, 0, 5))

            // 4. ШАПКА ТАБЛИЦЫ
            val headerRow = sheet.createRow(currentRow++)
            val headers = listOf("№ Сцены и заголовок", "Наименование", "Кол-во", "Заметки", "Якорь", "Сделать игровые фото")
            headers.forEachIndexed { index, title ->
                val cell = headerRow.createCell(index)
                cell.setCellValue(title)
                cell.setCellStyle(headerStyle)
            }

            // 5. ДАННЫЕ
            groupProps.forEach { prop ->
                val row = sheet.createRow(currentRow++)
                
                row.createCell(0).apply { 
                    setCellValue("${prop.seriesNumber}-${prop.sceneNumber}")
                    setCellStyle(dataStyle) 
                }
                row.createCell(1).apply { 
                    setCellValue(prop.name)
                    setCellStyle(dataStyle) 
                }
                row.createCell(2).apply { 
                    setCellValue(prop.quantity.toDouble())
                    setCellStyle(dataStyle) 
                }
                row.createCell(3).apply { 
                    setCellValue(prop.note ?: "")
                    setCellStyle(dataStyle) 
                }
                row.createCell(4).apply { 
                    setCellValue(prop.anchor)
                    setCellStyle(dataStyle) 
                }
                row.createCell(5).apply { 
                    setCellValue("") 
                    setCellStyle(dataStyle) 
                }
            }
            currentRow++ 
        }

        // Настройка ширины колонок
        sheet.setColumnWidth(0, 15 * 256) // № Сцены
        sheet.setColumnWidth(1, 25 * 256) // Наименование
        sheet.setColumnWidth(2, 8 * 256)  // Кол-во
        sheet.setColumnWidth(3, 20 * 256) // Заметки
        sheet.setColumnWidth(4, 45 * 256) // Якорь
        sheet.setColumnWidth(5, 15 * 256) // Фото

        val outputStream = ByteArrayOutputStream()
        workbook.write(outputStream)
        workbook.close()
        return outputStream.toByteArray()
    }

    private fun createTitleStyle(workbook: Workbook): CellStyle = workbook.createCellStyle().apply {
        alignment = HorizontalAlignment.CENTER
        val font = workbook.createFont().apply {
            bold = true
            fontHeightInPoints = 16.toShort()
        }
        setFont(font)
    }

    private fun createHeaderStyle(workbook: Workbook): CellStyle = workbook.createCellStyle().apply {
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

    private fun createShiftHeaderStyle(workbook: Workbook): CellStyle = workbook.createCellStyle().apply {
        alignment = HorizontalAlignment.CENTER
        val font = workbook.createFont().apply {
            bold = true
            fontHeightInPoints = 12.toShort()
            color = IndexedColors.BLUE.getIndex()
        }
        setFont(font)
    }

    private fun createDataStyle(workbook: Workbook): CellStyle = workbook.createCellStyle().apply {
        borderBottom = BorderStyle.THIN
        borderTop = BorderStyle.THIN
        borderLeft = BorderStyle.THIN
        borderRight = BorderStyle.THIN
        wrapText = true
        verticalAlignment = VerticalAlignment.TOP
    }
}
