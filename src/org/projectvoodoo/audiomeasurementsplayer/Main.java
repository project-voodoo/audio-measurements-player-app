
package org.projectvoodoo.audiomeasurementsplayer;

import java.lang.reflect.Field;

import org.projectvoodoo.audiomeasurementsplayer.SamplePlayer.Sample;

import android.app.Activity;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ProgressBar;
import android.widget.TextView;

public class Main extends Activity implements OnClickListener {

    private static final String TAG = "Voodoo AudioMeasurementsPlayer";

    private static final int[] CLICKABLE_BUTTONS = {
            R.id.button_rmaa_calibration,
            R.id.button_rmaa_test,
            R.id.button_udial,
            R.id.button_stop,
    };

    private ProgressBar mProgressBar;
    private TextView mTextTime;
    private TextView mSampleName;

    private int progressDelayMillisec = 250;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        for (int buttonId : CLICKABLE_BUTTONS)
            findViewById(buttonId).setOnClickListener(this);

        mProgressBar = (ProgressBar) findViewById(R.id.progress);
        mTextTime = (TextView) findViewById(R.id.text_time);
        mSampleName = (TextView) findViewById(R.id.text_sample_name);

        if (App.player == null) {
            Log.i(TAG, "New SamplePlayer");
            App.player = new SamplePlayer();
        }

        if (getPlatformVersion() >= 9)
            progressDelayMillisec = 15;

        setProgress();

        ((TextView) findViewById(R.id.text_native_sample_rate))
                .setText("AudioFlinger sample rate: " +
                        AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC) +
                        " Hz");
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.button_rmaa_calibration:
                App.player.play(Sample.RMAA_CALIBRATION, new PlayCallback());
                break;

            case R.id.button_rmaa_test:
                App.player.play(Sample.RMAA_TEST, new PlayCallback());
                break;

            case R.id.button_udial:
                App.player.play(Sample.UDIAL, new PlayCallback());
                break;

            case R.id.button_stop:
                App.player.stop();
                break;
        }

    }

    private void setProgress() {

        final String format = "%.2f / %.2f";

        if (App.player.isPlaying())
            mProgressBar.postDelayed(new Runnable() {

                @Override
                public void run() {

                    int progressMillisec = (int) App.player.getProgressMillisec();
                    int duration = (int) App.player.getDurationMillisec();
                    mProgressBar.setMax(duration);
                    mProgressBar.setProgress(progressMillisec);

                    mTextTime.setText(String.format(format,
                            (float) progressMillisec / 1000, (float) duration / 1000));

                    mSampleName.setText(App.player.getLastPlayedSampleName());

                    setProgress();
                }
            }, progressDelayMillisec);
        else {
            mTextTime.setText(String.format(format, 0f, 0f));
            mSampleName.setText(R.string.stopped);
        }
    }

    private class PlayCallback implements Runnable {
        @Override
        public void run() {
            setProgress();
        }
    }

    private static int getPlatformVersion() {
        try {
            Field verField = Class.forName("android.os.Build$VERSION").getField("SDK_INT");
            int ver = verField.getInt(verField);
            return ver;
        } catch (Exception e) {
            return 3;
        }
    }

}
