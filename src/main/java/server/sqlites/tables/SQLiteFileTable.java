package server.sqlites.tables;

import bottle.util.EncryptUtil;
import bottle.util.StringUtil;
import bottle.util.TimeTool;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static server.sqlites.SQLiteUtil.executeQuerySQL;
import static server.sqlites.SQLiteUtil.executeWriteSQL;

public class SQLiteFileTable {
    private static final String FILE_TABLE = "tb_file";
    private static final String FILE_LOCAL_TABLE_PATH= "fs_path";
    private static final String FILE_LOCAL_TABLE_POS= "fs_pos";
    private static final String FILE_LOCAL_TABLE_URL= "fs_url";
    private static final String FILE_LOCAL_TABLE_UPDATE= "fs_upt";

    public static final String POS_TYPE_LOCAL = "LOCAL";
    public static final String POS_TYPE_HWOBS = "HWOBS";

    /**
     * 创建表 键值对映射表
     */
    public static void initFileTable(){
        String stringBuffer = "CREATE TABLE IF NOT EXISTS " +
                FILE_TABLE + " (" +
                String.format("%s TEXT NOT NULL PRIMARY KEY,%s TEXT NOT NULL,%s TEXT DEFAULT '',%s TEXT NOT NULL ",
                        FILE_LOCAL_TABLE_PATH, FILE_LOCAL_TABLE_POS, FILE_LOCAL_TABLE_URL, FILE_LOCAL_TABLE_UPDATE) +
                ");";
        executeWriteSQL(stringBuffer);
    }


    /**
     * 加入文件
     * */
   private static boolean addFile(String path,String pos,String url){
       if (StringUtil.isEmpty(path,pos)){
           return false;
       }
       if (url == null) url="";
       // 直接插入
       final String SQL_INSERT = String.format("REPLACE INTO %s (%s,%s,%s,%s) VALUES (?,?,?,?);",FILE_TABLE,FILE_LOCAL_TABLE_PATH,FILE_LOCAL_TABLE_POS, FILE_LOCAL_TABLE_URL, FILE_LOCAL_TABLE_UPDATE);
       int res = executeWriteSQL(SQL_INSERT,path,pos,url,TimeTool.date_yMd_Hms_2Str(new Date()));
//       System.out.println("[" +pos+ "] 添加文件记录 "+ path+" -> "+ url);
       return res>0;
    }

    public static boolean addFile_LOCAL(String path,String url){
       return addFile(path,POS_TYPE_LOCAL,url);
    }

    public static boolean addFile_HWOBS(String path,String url){
        return addFile(path,POS_TYPE_HWOBS,url);
    }

    /**
     * 查询指定路径下的文件
     * */
    public static List<FileStorageItem> queryFileList(String path){
        List<FileStorageItem> list = new ArrayList<>();
        if (path != null && path.length()>0){
            path = path+"%";
            final String SQL_SELECT = String.format("SELECT * FROM %s WHERE %s LIKE ? ORDER BY %s ;",FILE_TABLE, FILE_LOCAL_TABLE_PATH, FILE_LOCAL_TABLE_PATH);
            List<Object[]> lines = executeQuerySQL(SQL_SELECT, path);
            for (Object[] r:lines){
                list.add(new FileStorageItem(r));
            }
        }
        return list;
    }

   public static final class FileStorageItem {
        public final String path;
        public final String pos;
        public final String url;
        public final String time;

        private FileStorageItem(Object[] rows) {
            this.path = String.valueOf(rows[0]);
            this.pos =  String.valueOf(rows[1]);
            this.url =  String.valueOf(rows[2]);
            this.time =  String.valueOf(rows[3]);
        }
    }

}
