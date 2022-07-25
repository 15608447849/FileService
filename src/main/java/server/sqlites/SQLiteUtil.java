package server.sqlites;

import bottle.util.Log4j;
import bottle.util.StringUtil;
import bottle.util.TimeTool;
import server.sqlites.tables.SQLiteFileTable;
import server.undertow.WebServer;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.Date;

import static server.sqlites.tables.SQLiteFileTable.initFileTable;
import static server.sqlites.tables.SQLiteListTable.initListTable;
import static server.sqlites.tables.SQLiteMapTable.initMapTable;


/**
 * Created by user on 2017/6/30.
 */



public class SQLiteUtil {

    private SQLiteUtil(){ }

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
    public synchronized static int executeWriteSQL(String sql, Object... params){
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

    private static void initLoad(String storePath){
            SQLiteConnect.setStoreLocationPath(storePath);
            initMapTable();
            initListTable();
            initFileTable();
            Log4j.info("SQLiter 启动, 本地路径: " + storePath);
    }

    static {
        try{
            // 设置数据库
            String dbStorePath = WebServer.rootFolder.getParentFile().getCanonicalPath() + File.separator + "file_server.db";
            SQLiteUtil.initLoad(dbStorePath);
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}
