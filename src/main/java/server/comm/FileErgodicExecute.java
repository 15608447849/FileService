package server.comm;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件遍历
 */
public class FileErgodicExecute extends SimpleFileVisitor<Path> {

    private final Path root;
    private List<Path> pathList = new ArrayList<>();
    private final boolean flag; //是否遍历子目录
    private Callback callback;

    public FileErgodicExecute setCallback(Callback callback) {
        this.callback = callback;
        return this;
    }

    public FileErgodicExecute(String path, boolean isSub) {
        root = Paths.get(path);
        flag = isSub;
    }

    public List<String> start(boolean replaceRoot) {
        try {
            Files.walkFileTree(root , this);
        } catch (IOException ignored) {
        }
       //排序创建时间排序 - 最新最后
        try {
            pathList.sort((p1, p2) -> {
                long diff = p1.toFile().lastModified() - p2.toFile().lastModified();
                if (diff > 0)
                    return -1;//倒序正序控制
                else if (diff == 0)
                    return 0;
                else
                    return 1;//倒序正序控制
            });
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        if (replaceRoot){
            return pathList.stream().map( path -> path.toUri().toString().replace(root.toUri().toString(),"/") ).collect(Collectors.toList());
        }else {
            return pathList.stream().map( path -> path.toUri().toString() ).collect(Collectors.toList());
        }
    }

    public List<String> start() {
        return start(false);
    }

    //失败
    public FileVisitResult visitFileFailed(Path path, IOException e){
        return FileVisitResult.SKIP_SUBTREE;
    }

    //访问子目录之前触发该方法
    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        return super.preVisitDirectory(dir, attrs);
    }

    //访问目录之后触发该方法
    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        return super.postVisitDirectory(dir, exc);
    }

    public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) {
        if (!flag && !filePath.getParent().equals(root)){
            return FileVisitResult.CONTINUE;
        }
        boolean isFilter = false;
        if (callback != null) {
            isFilter = callback.filterFile(filePath.toFile());
        }
        if(!isFilter) pathList.add(filePath);
        return FileVisitResult.CONTINUE;
    }



    public interface Callback{
        boolean filterFile(File file);
    }

}
