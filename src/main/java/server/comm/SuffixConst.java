package server.comm;


import bottle.properties.abs.ApplicationPropertiesBase;
import bottle.properties.annotations.PropertiesFilePath;
import bottle.properties.annotations.PropertiesName;
import bottle.util.Log4j;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@PropertiesFilePath("/web.properties")
public class SuffixConst {

    /* 白名单优先级大于黑名单 全部注释不限制 */
    @PropertiesName("upload.suffix.black.list")
    private static String upload_suffix_black_list;
    @PropertiesName("upload.suffix.white.list")
    private static String upload_suffix_white_list;
    // 不可清理的文件后缀,逗号分隔
    @PropertiesName("file.clear.filter.file.suffix")
    private static String clearProhibitSuffixArrayStr = "";
    // 系统默认不处理的文件后缀
    @PropertiesName("sys.filter.suffix")
    private static String sysFilterSuffixArrayStr = "";

    // 图片处理
    public static final String[] IMG_SUFFIX_ARRAY = new String[]{"-min","-ing","-org"};

    // 服务需使用的默认后缀
    private static final Set<String> systemDefaultSuffixSet = new HashSet<>(Arrays.asList("js","html","css","bak","back","apk","version","url","json","dev","log"));

    // 禁止删除的文件后缀
    private static final Set<String> clearForbidSuffixSet = new HashSet<>();

    // 文件上传白名单
    private static final Set<String> uploadWhiteSuffixSet = new HashSet<>();
    // 文件上传黑名单
    private static final Set<String> uploadBlackSuffixSet = new HashSet<>();




    static {
        ApplicationPropertiesBase.initStaticFields(SuffixConst.class);

        addUploadSuffix(upload_suffix_white_list,upload_suffix_black_list);
        addClearForbidSuffix(clearProhibitSuffixArrayStr);
        addSystemDefaultSuffix(sysFilterSuffixArrayStr);
    }






    /* 添加系统不处理的后缀列表 */
    private static void addSystemDefaultSuffix(String suffixStr){
        try {
            String[] suffixArr = suffixStr.split(",");
            clearForbidSuffixSet.addAll(Arrays.asList(suffixArr));
            Log4j.info("不可删除文件后缀:\t"+Arrays.toString(suffixArr));
        } catch (Exception ignored) { }
    }

    /* 添加禁止清理的后缀列表 */
    private static void addClearForbidSuffix(String suffixStr){
        try {
            String[] suffixArr = suffixStr.split(",");
            clearForbidSuffixSet.addAll(Arrays.asList(suffixArr));
            Log4j.info("不处理文件后缀:\t"+Arrays.toString(suffixArr));
        } catch (Exception ignored) { }
    }

    /* 添加文件上传白名单,黑名单后缀列表 */
    private static void addUploadSuffix(String whiteSuffixStr,String blackSuffixStr){
        try {
            String[] suffixArr = whiteSuffixStr.split(",");
            uploadWhiteSuffixSet.addAll(Arrays.asList(suffixArr));
            Log4j.info("文件后缀白名单:\t"+Arrays.toString(suffixArr));
        } catch (Exception ignored) { }
        try {
            String[] suffixArr = blackSuffixStr.split(",");
            uploadBlackSuffixSet.addAll(Arrays.asList(suffixArr));
            Log4j.info("文件后缀黑名单:\t"+Arrays.toString(suffixArr));
        } catch (Exception ignored) { }
    }

    private static boolean setEqualStr(Set<String> set,String target){
        if (set.size() > 0){
            for (String str : set) {
                if (str.equalsIgnoreCase(target)){
                    return true;
                }
            }
        }
        return false;
    }


    /* 是否系统默认文件后缀 */
    public static boolean isSystemDefaultFileSuffix(String fileName){
        int index = fileName.lastIndexOf(".");
        if (index>0){
            String suffix = fileName.substring(index + 1);
            return setEqualStr(systemDefaultSuffixSet,suffix);
        }
        return false;
    }

    /* 是否存在于禁止清理的后缀列表内 */
    public static boolean isClearForbidSuffix(String fileName){
        int index = fileName.lastIndexOf(".");
        if (index>0){
            String suffix = fileName.substring(index + 1);
            return setEqualStr(clearForbidSuffixSet,suffix);
        }
        return false;
    }

    /* 文件遍历时需要过滤的后缀 */
    public static boolean isErgodicNeedFilterSuffix(String filePath,String filterSuffixStr){
        int index = filePath.lastIndexOf("/");
        if (index>0){
            filePath = filePath.substring(index+1);
        }
        String[] suffixArray = IMG_SUFFIX_ARRAY;
        if (filterSuffixStr != null){
            String[] temp = filterSuffixStr.split(",");
            if (temp.length>0){
                suffixArray = temp;
            }
        }

        for (String suffix: suffixArray){
            if (filePath.contains(suffix)) return true;
        }
        return false;
    }


    /* 判断上传的文件是否允许保存到本地 */
    public static boolean isEnableSaveToLocal(String suffix){
        if (uploadWhiteSuffixSet.size() > 0 ){
            if (setEqualStr(uploadWhiteSuffixSet,suffix)){
                // 存在于白名单,允许保存
                return true;
            }
        }

        if (uploadBlackSuffixSet.size()>0){
            if (setEqualStr(uploadBlackSuffixSet,suffix)){
                // 存在于黑名单,禁止保存
                return false;
            }
        }
        // 无限制
        return true;
    }


    public static void main(String[] args) {

        System.out.println( isSystemDefaultFileSuffix(" /ykyy/fs/file/media/drug/1654673675001001026/7.jpg"));
    }
}
