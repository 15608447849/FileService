package server.sqlites;


import bottle.util.Log4j;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by user on 2017/7/3.
 */
class SQLiteConnect{
    static final private int OUT_TIME = 5 * 60 * 1000;
    static final private int NOT_USED = 0;
    static final private int USED = 1;
    private static String storeLocationPath = "memory";

    static void setStoreLocationPath(String path){
        storeLocationPath = path;
    }

    private static final ArrayList<SQLiteConnect> mConnectPools = new ArrayList<>();

    static final private Runnable LOOP_OUT_TIME_CONNECTION_RUNABLE = new Runnable() {
        @Override
        public void run() {
            while (true){
                try{
                    synchronized (this){
                        this.wait(OUT_TIME);
                    }
                    checkPool();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    };

    private static void checkPool() {
        if (mConnectPools.size()>0){
            Iterator<SQLiteConnect> iterator = mConnectPools.iterator();
            SQLiteConnect connect ;
            while (iterator.hasNext()){
                connect = iterator.next();
                if (connect.state == NOT_USED){
                    if (connect.isClose() || connect.isOutTime()){
                        iterator.remove();
                        connect.close();
                    }
                }

            }
        }
    }

    private static ThreadLocal<SQLiteConnect> local = new ThreadLocal<>();

    /**
     * 获取连接池
     */
    static SQLiteConnect getConnect(){
        SQLiteConnect connect = local.get();

        if (connect==null || connect.isClose()){
            connect = null;
            local.set(null);
        }

        if (connect == null){
            connect = new SQLiteConnect();
            mConnectPools.add(connect);
            local.set(connect);
            Log4j.info(Thread.currentThread()+" 创建数据库连接, 当前总连接数: " + mConnectPools.size());
        }
        connect.state = USED;
        return connect;
    }

    static void releaseConnect(SQLiteConnect connect){
        if (connect == null) return;
        connect.updateTime();
        connect.state = NOT_USED;
    }


    private Connection connection;
    private long time;
    private int state = NOT_USED;

    private SQLiteConnect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:"+storeLocationPath);//连接数据库
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
        updateTime();
    }


    //更新时间
    private void updateTime(){
        time = System.currentTimeMillis();
    }

    //超时结束
    private boolean isOutTime(){
        return (System.currentTimeMillis() - time) > OUT_TIME;
    }

    //是否关闭
    public boolean isClose(){
        try {
            return connection==null || connection.isClosed();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    public void close(){
        if (!isClose()){
            try {
                Log4j.info("释放SQL连接: "+ connection);
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }finally {
                connection = null;
            }
        }
    }

    public Connection getConnection() {
        return connection;
    }

    static {
        Thread t = new Thread(LOOP_OUT_TIME_CONNECTION_RUNABLE);
        t.setDaemon(true);
        t.start();
    }
}
