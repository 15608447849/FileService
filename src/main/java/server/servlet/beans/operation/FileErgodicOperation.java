package server.servlet.beans.operation;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件遍历
 */
public class FileErgodicOperation extends SimpleFileVisitor<Path> {

    private final Path root;
    private List<Path> pathList = new ArrayList<>();
    private final boolean flag; //是否遍历子目录
    private Callback callback;

    public FileErgodicOperation setCallback(Callback callback) {
        this.callback = callback;
        return this;
    }

    public FileErgodicOperation(String path, boolean isSub) {
        root = Paths.get(path);
        flag = isSub;
    }

    public List<String> start() {
        try {
            Files.walkFileTree(root , this);
        } catch (IOException ignored) {
        }
       //排序创建时间排序 - 最新最后
        try {
            pathList.sort(Comparator.comparingLong(p -> p.toFile().lastModified()));
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return pathList.stream().map(path -> path.toUri().toString().replace(root.toUri().toString(),"/") ).collect(Collectors.toList());
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
