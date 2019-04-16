package server.servlet.beans.operation;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * excel 内容读取
 */
public class ExcelReaderOperation {
    private static final String EXCEL_XLS = "xls";
    private static final String EXCEL_XLSX = "xlsx";
    private String filePath;
    private Workbook wb =null;

    public ExcelReaderOperation(String filePath) {
        this.filePath = filePath;
    }

    /**
     * 判断文件是否是excel
     * @throws Exception
     */
    public List<Map<String,String>> start() throws Exception{
        File file = new File(filePath);
        if (!file.exists()) throw new FileNotFoundException(filePath);

        if(!(file.isFile() && (file.getName().endsWith(EXCEL_XLS) || file.getName().endsWith(EXCEL_XLSX)))){
            throw new Exception(file.getName()+"不是excel文件");
        }
        String suffix = file.getName().substring(file.getName().lastIndexOf(".") + 1);
        try (InputStream is = new FileInputStream(file)) {
            if (EXCEL_XLS.equals(suffix)) {
                wb = new HSSFWorkbook(is);
            } else if (EXCEL_XLSX.equals(suffix)) {
                wb = new XSSFWorkbook(is);
            }
        }

        List<Map<String, String>> list = new ArrayList<>();
        //获取第一个sheet
        Sheet sheet = wb.getSheetAt(0);
        //获取最大行数
        int rownum = sheet.getPhysicalNumberOfRows();
        //获取第一行
        Row row = sheet.getRow(0);
        //获取最大列数
        int colnum = row.getPhysicalNumberOfCells();
        for (int i = 1; i<rownum; i++) {
            Map<String,String> map = new LinkedHashMap<>();
            row = sheet.getRow(i);
            if(row !=null){
                for (int j = 0;j<colnum;j++){
                    String cellData = (String) getCellFormatValue(row.getCell(j));
                    map.put(sheet.getRow(0).getCell(j).getStringCellValue(), cellData);
                }
            }else{
                break;
            }
            list.add(map);
        }
        
        return list;
    }

    private Object getCellFormatValue(Cell cell){
        Object cellValue;
        if(cell!=null){
            //判断cell类型
            switch(cell.getCellType()){
                case Cell.CELL_TYPE_NUMERIC:{
                    cellValue = String.valueOf(cell.getNumericCellValue());
                    break;
                }
                case Cell.CELL_TYPE_FORMULA:{
                    //判断cell是否为日期格式
                    if(DateUtil.isCellDateFormatted(cell)){
                        //转换为日期格式YYYY-mm-dd
                        cellValue = cell.getDateCellValue();
                    }else{
                        //数字
                        cellValue = String.valueOf(cell.getNumericCellValue());
                    }
                    break;
                }
                case Cell.CELL_TYPE_STRING:{
                    cellValue = cell.getRichStringCellValue().getString();
                    break;
                }
                default:
                    cellValue = "";
            }
        }else{
            cellValue = "";
        }
        return cellValue;
    }

}
