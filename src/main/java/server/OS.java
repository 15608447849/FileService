package server;// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

import bottle.util.Log4j;

import java.io.File;
import java.lang.reflect.Field;

import static bottle.properties.abs.ApplicationPropertiesBase.getRuntimeRootPath;

public class OS {
    private static enum OSType {
        OSUndefined,
        OSLinux,
        OSWindows,
        OSMacintosh,
        OSUnknown,
    }
    ;
    private static OSType osType = OSType.OSUndefined;

    public static final boolean isWindows() {
        return getOSType() == OSType.OSWindows;
    }

    public static final boolean isMacintosh() {
        return getOSType() == OSType.OSMacintosh;
    }

    public static final boolean isLinux() {
        return getOSType() == OSType.OSLinux;
    }

    private static final OSType getOSType() {
        if (osType == OSType.OSUndefined) {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.startsWith("windows"))
                osType = OSType.OSWindows;
            else if (os.startsWith("linux"))
                osType = OSType.OSLinux;
            else if (os.startsWith("mac"))
                osType = OSType.OSMacintosh;
            else
                osType = OSType.OSUnknown;
        }
        return osType;
    }

    /* 加载库文件dll到系统目录*/
    public static void dynamicDllLoad() throws Exception {
        boolean isLiunx = OS.isLinux();
        if ( isLiunx ) return;

        String ROOT_PATH =  getRuntimeRootPath(LunchServer.class);

        String dirPath = ROOT_PATH +"/resources/dll";
        File dict = new File(dirPath);
        if (!dict.exists()) return;

        dirPath += System.getProperty("os.arch").endsWith("64") ? "/win64" : "/win32";
        dict = new File(dirPath);
        if (!dict.exists()) return;

        addJavaSystemLibraryPath(dict);
    }

    private static void addJavaSystemLibraryPath(File dict) throws Exception{
        String syslib = System.getProperty("java.library.path");
        syslib = dict.getCanonicalPath() + ";" + syslib;
        System.setProperty("java.library.path", syslib);
        Field fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
        fieldSysPath.setAccessible(true);
        fieldSysPath.set(null, null);
        Log4j.info("加载 DLL目录: " + dict);
    }
}
