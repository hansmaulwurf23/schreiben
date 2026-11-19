package de.noisebleed.schreiben;

import android.app.Application;

/** Setzt das gewählte Design, bevor die erste Activity startet. */
public class SchreibenApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Settings.applyTheme(this);
    }
}
