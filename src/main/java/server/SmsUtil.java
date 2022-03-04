package server;

import bottle.properties.abs.ApplicationPropertiesBase;
import bottle.properties.annotations.PropertiesFilePath;
import bottle.properties.annotations.PropertiesName;
import bottle.util.HttpUtil;
import bottle.util.Log4j;
import bottle.util.StringUtil;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;


/**
 * @description: 发送短信工具
 * @author: lzp
 */

@PropertiesFilePath("/sms.properties")
public class SmsUtil {

    private static SmsUtil INSTANCE = new SmsUtil();

    @PropertiesName("sms.ip")
    public static String ip;

    @PropertiesName("sms.port")
    public static String port;

    @PropertiesName("sms.username")
    public static String username;
    @PropertiesName("sms.password")
    public static String password;

    @PropertiesName("sms.username.market")
    public static String usernameMarket;
    @PropertiesName("sms.password.market")
    public static String passwordMarket;

    static {
        ApplicationPropertiesBase.initStaticFields(SmsUtil.class);
    }

    private static String sendMsg(String phone, String content,boolean isMarket) {
        try {
            if (StringUtil.isEmpty(phone,content)) return null;
            String username = INSTANCE.username;
            String password = INSTANCE.password;
            if (isMarket){
                username = INSTANCE.usernameMarket;
                password = INSTANCE.passwordMarket;
            }
            // 短信相关的必须参数
            String mobile = phone;
            String message = content;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            String timestamp = sdf.format(new Date());
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(username.getBytes("utf8"));
            md5.update(password.getBytes("utf8"));
            md5.update(timestamp.getBytes("utf8"));
            md5.update(content.getBytes("utf8"));

            String passwordMd5 = Base64.getEncoder().encodeToString(md5.digest());
            passwordMd5 = URLEncoder.encode(passwordMd5,"utf-8");

            String url = "http://" + INSTANCE.ip + ":" + INSTANCE.port + "/mt";
            // 装配GET所需的参数
            StringBuilder sb = new StringBuilder(url);
            sb.append("?dc=8"); // unicode编码
            sb.append("&sm=").append(URLEncoder.encode(message, "utf8"));
            sb.append("&da=").append(mobile);
            sb.append("&un=").append(username);
            sb.append("&pw=").append(passwordMd5);
            sb.append("&tf=3"); // 表示短信内容为 urlencode+utf8
            sb.append("&rf=2");//json返回
            sb.append("&ts=").append(timestamp);//加密时间戳
            String request = sb.toString();
            // 以GET方式发起请求
            return HttpUtil.formText(request, "GET", null);

        } catch (Exception e) {
            Log4j.error("发送短信失败",e);
        }
        return null;
    }


    public static void main(String[] args) throws Exception{
       String message = args[0];
       for (int i=1; i < args.length ; i++){
           String phone = args[i];
           String res = sendMsg(phone,message,false);
           System.out.println("发送: "+ phone +" 结果: "+ res );
           Thread.sleep(1000);
       }

        System.out.println("即将结束");
        Thread.sleep(5000);

    }

}