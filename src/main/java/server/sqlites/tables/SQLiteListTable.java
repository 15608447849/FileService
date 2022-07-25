package server.sqlites.tables;

import bottle.util.StringUtil;
import bottle.util.TimeTool;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static server.sqlites.SQLiteUtil.executeQuerySQL;
import static server.sqlites.SQLiteUtil.executeWriteSQL;

/** 队列表 **/
public class SQLiteListTable {
    private static final String LIST_TABLE = "tb_list";
    private static final String LIST_LOCAL_TABLE_TYPE= "list_type";
    private static final String LIST_LOCAL_TABLE_VALUE = "list_value";
    private static final String LIST_LOCAL_TABLE_ATTACH = "list_attach";
    private static final String LIST_LOCAL_TABLE_CREATE = "list_upt";
    /**
     * 创建表 键值对映射表
     */
    public static void initListTable(){
        String stringBuffer = "CREATE TABLE IF NOT EXISTS " +
                LIST_TABLE + " (" +
                String.format("%s TEXT NOT NULL,%s TEXT NOT NULL,%s TEXT DEFAULT '',%s TEXT NOT NULL ",
                        LIST_LOCAL_TABLE_TYPE, LIST_LOCAL_TABLE_VALUE, LIST_LOCAL_TABLE_ATTACH, LIST_LOCAL_TABLE_CREATE) +
                ");" +
                String.format("CREATE UNIQUE INDEX idx ON %s (%s,%s);", LIST_TABLE, LIST_LOCAL_TABLE_TYPE, LIST_LOCAL_TABLE_VALUE);
        executeWriteSQL(stringBuffer);
    }

    // 加入队列
    public static boolean addListValue(String listType,String value,String identification){
        if (StringUtil.isEmpty(listType,value)){
            return false;
        }
        if (identification == null) identification="";
        // 直接插入
        final String SQL_INSERT = String.format("REPLACE INTO %s (%s,%s,%s,%s) VALUES (?,?,?,?);",LIST_TABLE,LIST_LOCAL_TABLE_TYPE,LIST_LOCAL_TABLE_VALUE, LIST_LOCAL_TABLE_ATTACH, LIST_LOCAL_TABLE_CREATE);
        int res = executeWriteSQL(SQL_INSERT,listType,value,identification,TimeTool.date_yMd_Hms_2Str(new Date()));
        return res>0;
    }



    // 移除队列
    public static boolean removeListValue(String listType,String value){
        if (StringUtil.isEmpty(listType,value)){
            return false;
        }
        final String SQL = String.format( "DELETE FROM %s WHERE %s=? AND %s=? ;",LIST_TABLE,LIST_LOCAL_TABLE_TYPE,LIST_LOCAL_TABLE_VALUE);
        int res = executeWriteSQL(SQL,listType,value);
        return res>0;
    }

    //获取列表 根据时间排序
    public static List<ListStorageItem> getListByType(String listType, int max){
        List<ListStorageItem> list = new ArrayList<>();
        final String SQL_SELECT = String.format("SELECT * FROM %s WHERE %s=? ORDER BY %s %s;",LIST_TABLE,LIST_LOCAL_TABLE_TYPE, LIST_LOCAL_TABLE_CREATE,(max>0?"LIMIT "+max : ""));
        List<Object[]> lines = executeQuerySQL(SQL_SELECT,listType);
        for (Object[] rows : lines) list.add(new ListStorageItem(rows));
        return list;
    }

    //根据标识查询是否存在
    public static boolean existIdentity(String listType,String identification){
        final String SQL_SELECT = String.format("SELECT * FROM %s WHERE %s=? AND %s=? LIMIT 1;",LIST_TABLE,LIST_LOCAL_TABLE_TYPE, LIST_LOCAL_TABLE_ATTACH);
        List<Object[]> lines = executeQuerySQL(SQL_SELECT,listType,identification);
        return lines.size()>0;
    }

    public static final class ListStorageItem {
        public final String type;
        public final String value;
        public final String identity;
        public final String time;

        private ListStorageItem(Object[] rows) {
            this.type = String.valueOf(rows[0]);
            this.value =  String.valueOf(rows[1]);
            this.identity =  String.valueOf(rows[2]);
            this.time =  String.valueOf(rows[3]);
        }
    }

}
