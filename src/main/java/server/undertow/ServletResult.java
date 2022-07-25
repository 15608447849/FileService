package server.undertow;

import java.util.HashMap;
import java.util.Map;

import static server.undertow.ServletResult.RESULT_CODE.*;

/**
 * Created by user on 2017/11/29.
 */
public class ServletResult {
    private final static Map<Integer,String> resultValMap = new HashMap<>();

    /**
     * @Author: leeping
     * @Date: 2019/4/2 17:40
     */
    public interface RESULT_CODE {
        int UNKNOWN = -1;
        int SUCCESS = 200;
        int EXCEPTION = -400;
        int PARAM_ERROR = -401;
        int FILE_NOT_FOUNT = -402;
    }

    static {
        resultValMap.put(UNKNOWN,"未知错误");
        resultValMap.put(SUCCESS,"操作成功");
        resultValMap.put(EXCEPTION,"异常捕获");
        resultValMap.put(PARAM_ERROR,"参数错误");
        resultValMap.put(FILE_NOT_FOUNT,"找不到指定文件或目录");
    }

    private int code = UNKNOWN;

    private String message = resultValMap.get(code);

    private Object data = null;

    private ServletResult value(int code, String message, Object data){
        this.code = code;
        this.message = message;
        this.data = data;
        return this;
    }

    public ServletResult value(int code, Object data){
        return value(code,resultValMap.get(code),data);
    }

    public ServletResult value(int code, String message){
        return value(code,message,data);
    }

    public ServletResult value(int code){
        return value(code, resultValMap.get(code));
    }

    public ServletResult setData(Object data){
        return value(SUCCESS, resultValMap.get(SUCCESS),data);
    }

    public ServletResult setError(String errorMessage){
        return value(EXCEPTION, errorMessage);
    }

}
