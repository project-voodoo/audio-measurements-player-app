
package org.projectvoodoo.audiomeasurementsplayer;

import android.app.Application;
import android.content.Context;

public class App extends Application {

    public static Context context;
    public static SamplePlayer player;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
    }

}
