package server.servlet.beans.operation;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件遍历
 */
public class FileErgodicOperation extends SimpleFileVisitor<Path> {

    private final Path root;
    private List<Path> pathList = new ArrayList<>();
    private final boolean flag;
    private Exception e;
    public FileErgodicOperation(String path,boolean isSub) {
        root = Paths.get(path);
        flag = isSub;
    }


    public List<String> start() {
        try {
            Files.walkFileTree(root , this);
        } catch (IOException ignored) {
        }
       //排序创建时间排序 - 最新最后
        pathList.sort((p1, p2) -> (int) (p1.toFile().lastModified() - p2.toFile().lastModified()));
        return pathList.stream().map(path -> path.toUri().toString().replace(root.toUri().toString(),"/") ).collect(Collectors.toList());
    }

    //失败
    public FileVisitResult visitFileFailed(Path path, IOException e) throws IOException {
        this.e = e;
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
        this.e = exc;
        return super.postVisitDirectory(dir, exc);
    }

    public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) {
            if (!flag && !filePath.getParent().equals(root)){
                return FileVisitResult.CONTINUE;
            }
        pathList.add(filePath);
        return FileVisitResult.CONTINUE;
    }




}
