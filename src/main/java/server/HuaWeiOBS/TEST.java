package server.HuaWeiOBS;

import bottle.util.EncryptUtil;
import com.obs.services.ObsClient;
import com.obs.services.internal.utils.ServiceUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Set;



/**
 * @Author: leeping
 * @Date: 2020/10/20 17:03
 */
public class TEST {
    public static void main(String[] args) throws Exception {


//        Set<String> set = HWOBSServer.ergodicDirectory("/",true);
//        System.out.println(set);

//        List<String> list = new ArrayList<>(set);
//        HWOBSServer.deleteFile(list);
        HWOBSServer.uploadLocalFile("C:/Users/user/Downloads/tenpaycert_c47.exe","/tenpaycert_c47.exe");
//        String url = HWOBSServer.convertLocalFileToObsUrl("/tenpaycert_c47.exe");
//        System.out.println( url );
        HWOBSServer.stop();
    }
}
