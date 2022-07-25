package server.comm;

import bottle.util.FileTool;
import bottle.util.Log4j;

import java.io.*;
import java.net.URL;

import static server.comm.OperationUtil.filePathAndStrToSuffix;

/**
 * @Author: leeping
 * @Date: 2019/8/2 15:28
 */
public class FFMPEGTool {
    private static final String FFMPEG_NAME = isLinux() ? "ffmpeg" : "ffmpeg.exe" ;
    private static final String CMD_IMAGE_COMPRESS_1 = "%s -i \"%s\" -vf palettegen=max_colors=256:stats_mode=single -y \"%s\"";
    private static final String CMD_IMAGE_COMPRESS_2 = "%s -i \"%s\" -i \"%s\" -lavfi \"[0][1:v] paletteuse\" -pix_fmt pal8 -y \"%s\"";

    private static boolean isLinux() {
        return System.getProperty("os.name").toLowerCase().contains("linux");
    }

    private static File ffmpegFile(){
        //查看是否有可用ffmpeg文件
        String dirPath = new File(FFMPEGTool.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParent();
        File file = new File(dirPath+"/resources/" + FFMPEG_NAME);
        if (file.exists()) return file;
        URL url = Thread.currentThread().getContextClassLoader().getResource(FFMPEG_NAME);
        if (url == null) return null;
        file = new File(url.getPath());
        if (file.exists()) return file;
        return null;
    }

    private static void executeCmd_windows(String cmd) throws IOException, InterruptedException {
        Process p = Runtime.getRuntime().exec(cmd);
        p.waitFor();
        p.destroy();
    }
    private static void executeCmd_liunx(String s) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder("/bin/bash","-c",s);
        builder.redirectErrorStream(true);
        Process p = builder.start();
        p.waitFor();
        p.destroy();
    }

    public static boolean imageCompress_(File src, File dist){
        File ffmpeg = ffmpegFile();
        if (ffmpeg == null) return false;
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
                executeCmd_windows(exe_cmd_1);
                executeCmd_windows(exe_cmd_2);
            }

            FileTool.deleteFile(_unit); //删除单元

            File target = new File(_dist);

            if (target.exists() && target.length() > 0 ){
                FileTool.rename(target,dist);
            }

            return dist.exists() && dist.length() > 0;
        } catch (Exception e) {
            Log4j.error("",e);
        }
        return false;
    }







}
