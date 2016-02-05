package org.tjj.starsector.ssme;
public class OperatingSystem {

    private static String os = System.getProperty("os.name").toLowerCase();

    public static final boolean isWindows = os.indexOf("win") >= 0; 
    public static final boolean isMac = os.indexOf("mac") >= 0;
    public static final boolean isUnix = os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0 || os.indexOf("aix") > 0;
    public static final boolean isSolaris = os.indexOf("sunos") >= 0;

}