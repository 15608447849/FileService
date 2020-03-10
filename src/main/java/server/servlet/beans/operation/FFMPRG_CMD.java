package server.servlet.beans.operation;


import bottle.util.FileUtils;
import bottle.util.Log4j;

import java.io.*;
import java.net.URL;

import static server.servlet.beans.operation.OperationUtils.filePathAndStrToSuffix;

/**
 * @Author: leeping
 * @Date: 2019/8/2 15:28
 */
public class FFMPRG_CMD {

    private static final String FFMPEG_NAME = isLinux() ? "ffmpeg" : "ffmpeg.exe" ;
    private static final String CMD_IMAGE_COMPRESS_1 = "%s -i \"%s\" -vf palettegen=max_colors=256:stats_mode=single -y \"%s\"";
    private static final String CMD_IMAGE_COMPRESS_2 = "%s -i \"%s\" -i \"%s\" -lavfi \"[0][1:v] paletteuse\" -pix_fmt pal8 -y \"%s\"";

    public static boolean isLinux() {
        return System.getProperty("os.name").toLowerCase().contains("linux");
    }


    public static File ffmpegFile(){
        //查看是否有可用ffmpeg文件
        String dirPath = new File(FFMPRG_CMD.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParent();
        File file = new File(dirPath+"/resources/" + FFMPEG_NAME);
        if (file.exists()) return file;
        URL url = Thread.currentThread().getContextClassLoader().getResource(FFMPEG_NAME);
        if (url == null) return null;
        file = new File(url.getPath());
        if (file.exists()) return file;
        return null;
    }

    private static String executeCmd(String cmd) throws IOException, InterruptedException {
        Log4j.info("执行命令: " + cmd);

        StringBuffer b = new StringBuffer();

//        new String[]{"/bin/sh","-c","grep -c 'TAG' /home/zhenm/xiaoxiao/run/test"};

        Process p = Runtime.getRuntime().exec(cmd);
//        Process p = Runtime.getRuntime().exec(cmd);
        try {
//            BufferedReader br = new BufferedReader(new InputStreamReader( p.getInputStream()));
//            String line ;
//            while ((line=br.readLine())!=null) {
//                b.append(line).append("\n");
//            }
//            System.out.println(b.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        p.waitFor();
//        p.waitFor(3, TimeUnit.MINUTES);
        p.destroy();
        return b.toString();
    }

    public static boolean imageCompress_(File src, File dist){
        File ffmpeg = ffmpegFile();
        if (ffmpeg == null) return false;
//        Log4j.info("FFMPEG PATH = "+ ffmpeg +" , "+ ffmpeg.exists());
        if (!ffmpeg.exists()) return false;

        try {
            String _ffmpeg = ffmpeg.getCanonicalPath();

            String _src = src.getCanonicalPath();

            String _unit = filePathAndStrToSuffix(_src,"_unit");

            String _dist = _unit+".jpg";

            String exe_cmd_1 = String.format(CMD_IMAGE_COMPRESS_1,
                    _ffmpeg, _src, _unit);
            String exe_cmd_2 = String.format(CMD_IMAGE_COMPRESS_2,
                    _ffmpeg, _src, _unit ,_dist);
            if (isLinux()){
                executeCmd_liunx(exe_cmd_1+" && "+exe_cmd_2);
            }else{
                executeCmd(exe_cmd_1);
                executeCmd(exe_cmd_2);
            }

            FileUtils.deleteFile(_unit); //删除单元

            File target = new File(_dist);

            if (target.exists() && target.length() > 0 ){
                FileUtils.rename(target,dist);
            }

            return dist.exists() && dist.length() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    private static void executeCmd_liunx(String s) throws IOException, InterruptedException {

//        Log4j.info("执行: "+ s);

        ProcessBuilder builder = new ProcessBuilder("/bin/bash","-c",s);
        builder.redirectErrorStream(true);
        Process p = builder.start();
        //Process p = Runtime.getRuntime().exec("grep -c 'TAG' /home/cc/test");
        int code = p.waitFor();
        /*InputStream is =  p.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        StringBuilder sb = new StringBuilder();
        while(true){
            String line = br.readLine();
            if(line == null){
                break;
            }

            if(line.trim().length()>0){
                sb.append(line);
                sb.append("\n");
            }
        }
        br.close();
        isr.close();
        is.close();
        System.out.println(code);//
        System.out.println(sb.toString());
        System.out.println("=====================");

        InputStream eis = p.getErrorStream();
        InputStreamReader eisr = new InputStreamReader(eis);
        BufferedReader ebr = new BufferedReader(eisr);
        StringBuilder esb = new StringBuilder();
        while(true){
            String eline = ebr.readLine();
            if(eline==null){
                break;
            }
            if(eline.trim().length()>0){
                esb.append(eline);
                esb.append("\n");
            }
        }
        ebr.close();
        eisr.close();
        eis.close();

        System.out.println(esb.toString());*/
        p.destroy();
    }


    public static void main(String[] args) throws IOException, InterruptedException {
        File src = new File("C:\\Users\\user\\Desktop\\GOODS\\uipageimage\\图片","10.jpg");
        File dist = new File("C:\\Users\\user\\Desktop\\GOODS\\uipageimage\\图片","10-cpr.jpg");

        imageCompress_(src,dist);

    }


}
