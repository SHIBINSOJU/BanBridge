/*
 * Decompiled with CFR 0.152.
 */
package com.shotdevs.banbridge.database;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;

public class FileUtil {
    public static void ensureDirectoryExists(File dir) {
        if (!dir.exists()) {
            try {
                Files.createDirectories(dir.toPath(), new FileAttribute[0]);
            }
            catch (IOException iOException) {
                // empty catch block
            }
        }
    }
}

