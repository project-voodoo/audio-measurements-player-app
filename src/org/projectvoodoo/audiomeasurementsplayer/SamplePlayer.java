
package org.projectvoodoo.audiomeasurementsplayer;

import java.io.IOException;
import java.io.InputStream;

import org.kc7bfi.jflac.FLACDecoder;
import org.kc7bfi.jflac.PCMProcessor;
import org.kc7bfi.jflac.metadata.StreamInfo;
import org.kc7bfi.jflac.util.ByteData;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

public class SamplePlayer {

    private static final String TAG = "Voodoo AudioMeasurementsPlayer SamplePlayer";

    private static final int SAMPLE_FREQUENCY = 44100;
    private static final int BUFFER_MILLISEC = 800;

    private Object playingLock = new Object();
    private InputStream mInputStream;
    private long mTotalSamples;
    private AudioTrack mTrack;
    private Sample lastPlayedSample;

    private long mLastTimePlaying;

    enum Sample {
        RMAA_CALIBRATION("rmaa-calibration-44100-16.flac"),
        SQUARE_CALIBRATION("square-1kHz-calibration-44100-16.flac"),
        ZEROS("zeros.flac"),
        FAINT_NOISE("faint-noise.flac"),
        RMAA_TEST("rmaa-test-44100-16.flac"),
        UDIAL("udial.flac"),
        CCIFIMD("ccif-imd.flac"),

        ;
        String assetFileName;

        Sample(String fileName) {
            this.assetFileName = fileName;
        }
    }

    public synchronized void play(Sample sample, boolean loop, Runnable callback) {
        stop();

        Log.i(TAG, "Playing " + sample + ": " + sample.assetFileName);
        DecodingTask task = new DecodingTask(sample, loop, callback);
        task.start();
        lastPlayedSample = sample;
    }

    public synchronized void stop() {
        if (mInputStream != null)
            try {
                mInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        if (mTrack != null) {
            mTrack.stop();
            mTrack.flush();
        }

        synchronized (playingLock) {
        }
    }

    public long getDurationMillisec() {
        return (long) ((double) mTotalSamples * 1000 / SAMPLE_FREQUENCY);
    }

    public boolean isPlaying() {
        if (mTrack == null)
            return false;

        if (mTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
            mLastTimePlaying = System.currentTimeMillis();
            return true;
        }

        if (System.currentTimeMillis() < mLastTimePlaying + BUFFER_MILLISEC + 50)
            return true;

        mTrack.release();
        mTrack = null;
        return false;
    }

    public long getProgressMillisec() {
        if (mTrack != null)
            try {
                return (long) ((double) mTrack.getPlaybackHeadPosition() * 1000 / SAMPLE_FREQUENCY);
            } catch (Exception e) {
            }

        return 0;
    }

    public String getLastPlayedSampleName() {
        if (lastPlayedSample != null)
            return lastPlayedSample.assetFileName;

        return "";
    }

    private class DecodingTask extends Thread {
        private Sample mSample;
        private AudioTrack mDecoderTrack;
        private Runnable mCallback;
        private boolean mLoop;

        public DecodingTask(Sample sample, boolean loop, Runnable callback) {
            setPriority(MAX_PRIORITY);
            setName("Sample Decoder/Player");
            this.mSample = sample;
            mLoop = loop;
            mCallback = callback;
        }

        @Override
        public void run() {
            super.run();
            synchronized (playingLock) {

                mDecoderTrack = mTrack = getAudioTrack();
                try {
                    while (true) {
                        String realAssetFileName = mSample.assetFileName + ".ogg";
                        mInputStream = App.context.getAssets().open(realAssetFileName);
                        FLACDecoder decoder = new FLACDecoder(mInputStream);

                        decoder.addPCMProcessor(new AudioTrackOutput(mDecoderTrack));
                        mDecoderTrack.play();
                        mCallback.run();
                        decoder.decode();
                        mInputStream.close();

                        if (!mLoop)
                            break;
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    // manual stop
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (mDecoderTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING)
                    mDecoderTrack.stop();

                Log.i(TAG, "Finished");
                mInputStream = null;
            }
        }

        class AudioTrackOutput implements PCMProcessor {
            final AudioTrack track;

            public AudioTrackOutput(AudioTrack track) {
                this.track = track;
            }

            @Override
            public void processStreamInfo(StreamInfo streamInfo) {
                mTotalSamples = streamInfo.getTotalSamples();
            }

            @Override
            public void processPCM(ByteData pcm) {
                track.write(pcm.getData(), 0, pcm.getLen());
            }
        }
    }

    @SuppressWarnings("deprecation")
    private AudioTrack getAudioTrack() {
        mLastTimePlaying = 0;
        return new AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_FREQUENCY,
                AudioFormat.CHANNEL_CONFIGURATION_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                SAMPLE_FREQUENCY * BUFFER_MILLISEC / 1000 * 4,
                AudioTrack.MODE_STREAM);
    }

}
