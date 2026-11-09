package com.missanye.desktop;
/** Plain launcher for jpackage and classpath launches. */
public final class Launcher {
    private Launcher() {}
    public static void main(String[] args) { javafx.application.Application.launch(DesktopApplication.class,args); }
}
