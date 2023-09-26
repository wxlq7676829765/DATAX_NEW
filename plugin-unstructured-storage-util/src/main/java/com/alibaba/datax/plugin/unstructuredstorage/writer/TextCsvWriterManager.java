package com.alibaba.datax.plugin.unstructuredstorage.writer;

import com.csvreader.CsvWriter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;


/* update by wx 20220105 添加ftp的excel模式导出 */
public class TextCsvWriterManager {
    public static UnstructuredWriter produceUnstructuredWriter(
            String fileFormat, char fieldDelimiter, Writer writer) {
        // warn: false means plain text(old way), true means strict csv format
        if (Constant.FILE_FORMAT_TEXT.equals(fileFormat)) {
            return new TextWriterImpl(writer, fieldDelimiter);
        } else if (Constant.FILE_FORMAT_XLSX.equals(fileFormat)) {
            return new ExcelWriterImpl();
        }else if (Constant.FILE_FORMAT_FILE2FILE.equals(fileFormat)) {
            return new SelfWriterImpl();
        }else if (Constant.FILE_FORMAT_FILE2DB.equals(fileFormat)) {
            return new SelfWriterImpl();
        }else {
            return new CsvWriterImpl(writer, fieldDelimiter);
        }


    }
}

class CsvWriterImpl implements UnstructuredWriter {
    private static final Logger LOG = LoggerFactory
            .getLogger(CsvWriterImpl.class);
    // csv 严格符合csv语法, 有标准的转义等处理
    private final char fieldDelimiter;
    private final CsvWriter csvWriter;

    public CsvWriterImpl(Writer writer, char fieldDelimiter) {
        this.fieldDelimiter = fieldDelimiter;
        this.csvWriter = new CsvWriter(writer, this.fieldDelimiter);
        this.csvWriter.setTextQualifier('"');
        this.csvWriter.setUseTextQualifier(true);
        // warn: in linux is \n , in windows is \r\n
        this.csvWriter.setRecordDelimiter(IOUtils.LINE_SEPARATOR.charAt(0));
    }

    @Override
    public void writeOneRecord(List<String> splitedRows) throws IOException {
        if (splitedRows.isEmpty()) {
            LOG.info("Found one record line which is empty.");
        }
        this.csvWriter.writeRecord(splitedRows
                .toArray(new String[0]));
    }

    @Override
    public void flush() throws IOException {
        this.csvWriter.flush();
    }

    @Override
    public void close() throws IOException {
        this.csvWriter.close();
    }

}
/* update by  wx 20220105 添加ftp的excel导出 */
class ExcelWriterImpl implements UnstructuredWriter {
    private static final Logger LOG = LoggerFactory
            .getLogger(ExcelWriterImpl.class);
    SXSSFWorkbook wb = new SXSSFWorkbook(100);
    SXSSFSheet sheet = null;
    int rowIndex = 0;
    int sheetIndex =  1 ;
    List<String> headRows = new ArrayList<String>();
    public ExcelWriterImpl() {

    }

    @Override
    public void writeOneRecord(List<String> splitedRows) throws IOException {
        if (splitedRows.isEmpty()) {
            LOG.info("Found one record line which is empty.");
        }
        if(rowIndex == 0 ){
            sheet =  wb.createSheet("sheet"+sheetIndex);
            LOG.info("create sheet sheet"+sheetIndex );
            if(rowIndex == 0 && sheetIndex ==1){
                headRows = splitedRows;
            }
        }
        if (rowIndex == 0){

            SXSSFRow row = sheet.createRow(rowIndex);
            for (int i = 0; i < headRows.size(); i++) {
                row.createCell(i).setCellValue(headRows.get(i));
            }
        }else{
            SXSSFRow row = sheet.createRow(rowIndex);
            for (int i = 0; i < splitedRows.size(); i++) {
                row.createCell(i).setCellValue(splitedRows.get(i));
            }
        }
        if(rowIndex++ == 1000*1000){
            rowIndex = 0;
            sheetIndex ++;
        }
    }

    @Override
    public void flush() throws IOException {
//        this.wb.write(outputStream);
//        this.excleWriter.flush();
    }

    @Override
    public void close() throws IOException {
//        this.excleWriter.close();
    }

    public SXSSFWorkbook getWb() {
        return wb;
    }
}

/* update by  wx 20220105 添加ftp2ftp and ftp2Db 导出 */
class SelfWriterImpl implements UnstructuredWriter {
    private static final Logger LOG = LoggerFactory
            .getLogger(ExcelWriterImpl.class);
    public SelfWriterImpl() {

    }

    @Override
    public void writeOneRecord(List<String> splitedRows) throws IOException {

    }

    @Override
    public void flush() throws IOException {
//        this.wb.write(outputStream);
//        this.excleWriter.flush();
    }

    @Override
    public void close() throws IOException {
//        this.excleWriter.close();
    }

}

class TextWriterImpl implements UnstructuredWriter {
    private static final Logger LOG = LoggerFactory
            .getLogger(TextWriterImpl.class);
    // text StringUtils的join方式, 简单的字符串拼接
    private final char fieldDelimiter;
    private final Writer textWriter;

    public TextWriterImpl(Writer writer, char fieldDelimiter) {
        this.fieldDelimiter = fieldDelimiter;
        this.textWriter = writer;
    }

    @Override
    public void writeOneRecord(List<String> splitedRows) throws IOException {
        if (splitedRows.isEmpty()) {
            LOG.info("Found one record line which is empty.");
        }
        this.textWriter.write(String.format("%s%s",
                StringUtils.join(splitedRows, this.fieldDelimiter),
                IOUtils.LINE_SEPARATOR));
    }

    @Override
    public void flush() throws IOException {
        this.textWriter.flush();
    }

    @Override
    public void close() throws IOException {
        this.textWriter.close();
    }

}
