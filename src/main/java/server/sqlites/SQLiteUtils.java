package server.sqlites;

import bottle.util.FileTool;
import bottle.util.StringUtil;
import bottle.util.TimeTool;
import server.prop.WebServer;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.Date;

import static server.prop.WebServer.rootFolder;
import static server.prop.WebServer.rootFolderStr;

/**
 * Created by user on 2017/6/30.
 */
public class SQLiteUtils {
    private SQLiteUtils(){ };

    private static void setParameters(PreparedStatement pst, Object... params) throws SQLException {
        if (params!=null) setInputParameters(pst, params);
    }

    private static  void setInputParameters(PreparedStatement pst, Object... params) throws SQLException {
        for(int i = 0; i < params.length; ++i) {
            Object o = params[i];
            if (o != null) {
                pst.setObject(i + 1, o);
            } else {
                pst.setNull(i + 1, 0);
            }
        }

    }

    private static PreparedStatement prepareStatement(Connection conn, String sql) throws SQLException {
        return conn.prepareStatement(sql);
    }

    /**  执行新增修改删除sql */
    public static int executeWriteSQL(String sql,Object...params){
        int affectedRows = -1;
        PreparedStatement pst = null;
        SQLiteConnect connection = null;
        try {
            connection = SQLiteConnect.getConnect();
            pst = prepareStatement(connection.getConnection(), sql);
            setParameters(pst, params);
            if (!pst.execute()){
                affectedRows = pst.getUpdateCount();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            closeStatement(pst);
            SQLiteConnect.releaseConnect(connection);
        }
        return affectedRows;
    }

    /** 执行查询SQL*/
    public static List<Object[]> executeQuerySQL(String sql, Object... params){
        List<Object[]> resList = new ArrayList<>();
        PreparedStatement pst = null;
        SQLiteConnect connection = null;
        try {
            connection = SQLiteConnect.getConnect();
            pst = prepareStatement(connection.getConnection(), sql);
            setParameters(pst, params);
            if (pst.execute()){
                try(ResultSet rs = pst.getResultSet()){
                    if (rs!=null){
                        int cols = rs.getMetaData().getColumnCount(); //行数
                        while(rs.next()) {
                            Object[] arrays = new Object[cols];
                            for(int i = 0; i < cols; i++) {
                                arrays[i] = rs.getObject(i + 1);
                            }
                            resList.add(arrays);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }finally {
            closeStatement(pst);
            SQLiteConnect.releaseConnect(connection);
        }
        return resList;
    }

    //关闭声明对象
    private static void closeStatement(Statement stat){
        if (stat!=null){
            try {
                stat.close();
            } catch (SQLException ignored) {
            }
        }
    }

    private static String MAP_LOCAL_TABLE = "key_val_table";
    private static String MAP_LOCAL_TABLE_KEY_NAME = "map_key";
    private static String MAP_LOCAL_TABLE_VALUE_NAME = "map_value";

    /**
     * 创建表 键值对映射表
     */
    public static void initMapTable(){
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("CREATE TABLE IF NOT EXISTS ")
                .append(MAP_LOCAL_TABLE).append(" (")
                .append(String.format("%s TEXT UNIQUE,%s TEXT NOT NULL",MAP_LOCAL_TABLE_KEY_NAME,MAP_LOCAL_TABLE_VALUE_NAME))
                .append(");");
        executeWriteSQL(stringBuffer.toString());
    }

    private static void putKeyValue(String key,String value){
        if (key!=null && value!=null){
            final String SQL = String.format("REPLACE INTO %s (%s,%s) VALUES (?,?);",MAP_LOCAL_TABLE,MAP_LOCAL_TABLE_KEY_NAME,MAP_LOCAL_TABLE_VALUE_NAME);
            executeWriteSQL(SQL,key,value);
        }
        if (key != null && value == null){
            final String SQL = String.format("DELETE FROM %s WHERE %s=?;",MAP_LOCAL_TABLE,MAP_LOCAL_TABLE_KEY_NAME);
            executeWriteSQL(SQL,key);
        }
    }

    private static void putMaps(Map<String,String> map){
        for (String key:map.keySet()){
            String value = map.get(key);
            putKeyValue(key,value);
        }
    }

    private static String getValue(String key){
        final String SQL = String.format("SELECT %s FROM %s WHERE %s=?;",MAP_LOCAL_TABLE_VALUE_NAME,MAP_LOCAL_TABLE,MAP_LOCAL_TABLE_KEY_NAME);
        List<Object[]> list = executeQuerySQL(SQL,key);
        if (list.size()==1 && list.get(0).length==1) return String.valueOf(list.get(0)[0]);
        return null;
    }

    private static Map<String,String> getMaps(){
        final String SQL = String.format("SELECT %s,%s FROM %s;",MAP_LOCAL_TABLE_KEY_NAME,MAP_LOCAL_TABLE_VALUE_NAME,MAP_LOCAL_TABLE);
        List<Object[]> list = executeQuerySQL(SQL);
        Map<String,String> map = new HashMap<>();
        for (Object[] o : list){
            String k = String.valueOf(o[0]);
            String v = String.valueOf(o[1]);
            map.put(k,v);
        }
        return map;
    }


    private static String LIST_TABLE = "list_table";
    private static String LIST_LOCAL_TABLE_TYPE= "list_type";
    private static String LIST_LOCAL_TABLE_VALUE = "list_value";
    private static String LIST_LOCAL_TABLE_IDENTIFICATION = "list_identification";
    private static String LIST_LOCAL_TABLE_VERSION = "list_modification_time";
    /**
     * 创建表 键值对映射表
     */
    public static void initListTable(){
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("CREATE TABLE IF NOT EXISTS ")
                .append(LIST_TABLE).append(" (")
                .append(String.format("%s TEXT NOT NULL,%s TEXT NOT NULL,%s TEXT,%s TIMESTAMP"
                        ,LIST_LOCAL_TABLE_TYPE,LIST_LOCAL_TABLE_VALUE,LIST_LOCAL_TABLE_IDENTIFICATION,LIST_LOCAL_TABLE_VERSION))
                .append(");");
        executeWriteSQL(stringBuffer.toString());
    }

    // 加入队列
    public static boolean addListValue(String listType,String value,String identification,String dataTime){
        if (StringUtil.isEmpty(listType,value)){
            return false;
        }
        if (dataTime == null) dataTime = TimeTool.date_yMd_Hms_2Str(new Date());

        if (identification!=null){
            //查询是否存在, 存在更新
           final String SQL_SELECT = String.format("SELECT * FROM %s WHERE %s=? AND %s=?;",LIST_TABLE,LIST_LOCAL_TABLE_TYPE,LIST_LOCAL_TABLE_IDENTIFICATION);
            List<Object[]> lines = executeQuerySQL(SQL_SELECT,listType,identification);
            if (lines.size()>0){
                //修改
                final String SQL_UPDATE = String.format("UPDATE %s SET %s=? , %s=? WHERE %s=? AND %s=?;",LIST_TABLE,LIST_LOCAL_TABLE_VALUE,LIST_LOCAL_TABLE_VERSION,LIST_LOCAL_TABLE_TYPE,LIST_LOCAL_TABLE_IDENTIFICATION);
                int res = executeWriteSQL(SQL_UPDATE,value,dataTime,listType,identification);
                if (res>0) return true;
            }
        }
        //直接插入
        final String SQL_INSERT = String.format("INSERT INTO %s (%s,%s,%s,%s) VALUES (?,?,?,?);",LIST_TABLE,LIST_LOCAL_TABLE_TYPE,LIST_LOCAL_TABLE_VALUE,LIST_LOCAL_TABLE_IDENTIFICATION,LIST_LOCAL_TABLE_VERSION);
        int res = executeWriteSQL(SQL_INSERT,listType,value,identification,dataTime);
        return res>0;
    }

    // 移除队列
    public static boolean removeListValue(String listType,String value,String identification){
        if (StringUtil.isEmpty(listType,value)){
            return false;
        }
        final String SQL = String.format( "DELETE FROM %s WHERE %s=? AND %s=? AND %s=?;",LIST_TABLE,LIST_LOCAL_TABLE_TYPE,LIST_LOCAL_TABLE_VALUE,LIST_LOCAL_TABLE_IDENTIFICATION);

        int res = executeWriteSQL(SQL,listType,value,identification);
        return res>0;
    }

    public static class StorageItem {
        public final String type;
        public final String value;
        public final String identity;
        public final String time;

        private StorageItem(Object[] rows) {
            this.type = String.valueOf(rows[0]);
            this.value =  String.valueOf(rows[1]);
            this.identity =  String.valueOf(rows[2]);
            this.time =  String.valueOf(rows[3]);
        }

        @Override
        public String toString() {
            return "\t{" +
                    "type='" + type + '\'' +
                    ", value='" + value + '\'' +
                    ", identity='" + identity + '\'' +
                    ", time='" + time + '\'' +
                    '}'+"";
        }
    }

    //获取列表 根据时间排序
    public static List<StorageItem> getListByType(String listType){
        List<StorageItem> list = new ArrayList<>();
        final String SQL_SELECT = String.format("SELECT * FROM %s WHERE %s=?;",LIST_TABLE,LIST_LOCAL_TABLE_TYPE);
        List<Object[]> lines = executeQuerySQL(SQL_SELECT,listType);
        for (Object[] rows : lines) list.add(new StorageItem(rows));
        return list;
    }

    static {
        try {
            String storePath = rootFolder.getParentFile().getCanonicalPath() + File.separator+"local.db";
            SQLiteConnect.setStoreLocationPath(storePath);
            initMapTable();
            initListTable();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        addListValue("文件同步OBS列表","/a/b/c/1.png","md5-1",null);
        addListValue("文件同步OBS列表","/a/b/c/2.png","md5-2",null);
        addListValue("文件同步OBS列表","/a/b/c/3.png","md5-3",null);
        List<StorageItem> list = getListByType("文件同步OBS列表");
        System.out.println(list);
        for (StorageItem it : list){
            removeListValue("文件同步OBS列表",it.value,null);
        }
        list = getListByType("文件同步OBS列表");
        System.out.println(list);
    }



}
