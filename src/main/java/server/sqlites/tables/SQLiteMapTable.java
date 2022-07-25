package server.sqlites.tables;

import bottle.util.TimeTool;

import java.util.*;

import static server.sqlites.SQLiteUtil.executeQuerySQL;
import static server.sqlites.SQLiteUtil.executeWriteSQL;

/** 键值对表 **/
public class SQLiteMapTable {
    private static final String MAP_LOCAL_TABLE = "tb_map";
    private static final String MAP_LOCAL_TABLE_KEY_NAME = "map_key";
    private static final String MAP_LOCAL_TABLE_VALUE_NAME = "map_value";
    private static final String MAP_LOCAL_TABLE_UPDATE= "map_upt";

    /**
     * 创建表 键值对映射表
     */
    public static void initMapTable(){
        String stringBuffer = "CREATE TABLE IF NOT EXISTS " +
                MAP_LOCAL_TABLE + " (" +
                String.format("%s TEXT PRIMARY KEY,%s TEXT NOT NULL,%s TEXT NOT NULL ",
                        MAP_LOCAL_TABLE_KEY_NAME, MAP_LOCAL_TABLE_VALUE_NAME, MAP_LOCAL_TABLE_UPDATE) +
                ");";
        executeWriteSQL(stringBuffer);
    }

    public static void putKeyValue(String key,String value){
        if (key!=null && value!=null){
            final String SQL = String.format("REPLACE INTO %s (%s,%s,%s) VALUES (?,?,?);",MAP_LOCAL_TABLE,MAP_LOCAL_TABLE_KEY_NAME,MAP_LOCAL_TABLE_VALUE_NAME,MAP_LOCAL_TABLE_UPDATE);
            executeWriteSQL(SQL,key,value, TimeTool.date_yMd_Hms_2Str(new Date()));
        }
        if (key != null && value == null){
            final String SQL = String.format("DELETE FROM %s WHERE %s=?;",MAP_LOCAL_TABLE,MAP_LOCAL_TABLE_KEY_NAME);
            executeWriteSQL(SQL,key);
        }
    }

    public static void putMaps(Map<String,String> map){
        for (String key:map.keySet()){
            String value = map.get(key);
            putKeyValue(key,value);
        }
    }

    public static String getValue(String key){
        final String SQL = String.format("SELECT %s FROM %s WHERE %s=?;",MAP_LOCAL_TABLE_VALUE_NAME,MAP_LOCAL_TABLE,MAP_LOCAL_TABLE_KEY_NAME);
        List<Object[]> list = executeQuerySQL(SQL,key);
        if (list.size()==1 && list.get(0).length==1) return String.valueOf(list.get(0)[0]);
        return null;
    }

    public static Map<String,String> getMaps(){
        final String SQL = String.format("SELECT %s,%s FROM %s ORDER BY %s;",MAP_LOCAL_TABLE_KEY_NAME,MAP_LOCAL_TABLE_VALUE_NAME,MAP_LOCAL_TABLE,MAP_LOCAL_TABLE_UPDATE);
        List<Object[]> list = executeQuerySQL(SQL);
        Map<String,String> map = new LinkedHashMap<>();
        for (Object[] o : list){
            String k = String.valueOf(o[0]);
            String v = String.valueOf(o[1]);
            map.put(k,v);
        }
        return map;
    }


}
