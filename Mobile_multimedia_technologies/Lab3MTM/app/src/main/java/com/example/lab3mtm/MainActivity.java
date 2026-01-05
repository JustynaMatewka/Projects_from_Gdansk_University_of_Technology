package com.example.lab3mtm;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.audiofx.Visualizer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.jspecify.annotations.NonNull;


public class MainActivity extends AppCompatActivity {
    private MediaPlayer mediaPlayer;
    private float volumeValue = 0.5f;
    private AudioRecord audioRecord;
    private final int minBuffer = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);
    private RecordTask recordTask;
    private Visualizer visualizer;
    private WaveformView waveformViewPlayer;
    private WaveformView waveformViewRecorder;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        waveformViewPlayer = findViewById(R.id.imageView);
        waveformViewRecorder = findViewById(R.id.imageView2);

        Button buttonIncrease = findViewById(R.id.button5);
        buttonIncrease.setOnClickListener((b) -> {
            if (mediaPlayer != null) {
                if (volumeValue < 1.0f) volumeValue += 0.1f;
                mediaPlayer.setVolume(volumeValue, volumeValue);
            }
        });

        Button buttonDecrease = findViewById(R.id.button6);
        buttonDecrease.setOnClickListener((b) -> {
            if (mediaPlayer != null) {
                if (volumeValue > 0.0f) volumeValue -= 0.1f;
                mediaPlayer.setVolume(volumeValue, volumeValue);
            }
        });

        Button buttonMediaPlayerStart = findViewById(R.id.button);
        buttonMediaPlayerStart.setOnClickListener((b) -> {
//            mediaPlayer = MediaPlayer.create(this, R.raw.sample1);  //  APE
//            mediaPlayer = MediaPlayer.create(this, R.raw.sample2);  // OGG
//            mediaPlayer = MediaPlayer.create(this, R.raw.sample3);  // AAC
//            mediaPlayer = MediaPlayer.create(this, R.raw.sample4);  // WAV
//            mediaPlayer = MediaPlayer.create(this, R.raw.sample5);  // MP3
            mediaPlayer = MediaPlayer.create(this, R.raw.sample6);  // FLAC

            if (visualizer != null) {
                visualizer.release();
            }

            visualizer = new android.media.audiofx.Visualizer(mediaPlayer.getAudioSessionId());
            visualizer.setCaptureSize(android.media.audiofx.Visualizer.getCaptureSizeRange()[1]);
            visualizer.setDataCaptureListener(new android.media.audiofx.Visualizer.OnDataCaptureListener() {
                @Override
                public void onWaveFormDataCapture(android.media.audiofx.Visualizer visualizer, byte[] waveform, int samplingRate) {
                    if (waveformViewPlayer != null) {
                        waveformViewPlayer.updateVisualizer(waveform);
                    }
                }

                @Override
                public void onFftDataCapture(android.media.audiofx.Visualizer visualizer, byte[] fft, int samplingRate) {
                }
            }, android.media.audiofx.Visualizer.getMaxCaptureRate() / 2, true, false);

            visualizer.setEnabled(true);

            mediaPlayer.setVolume(volumeValue, volumeValue);
            mediaPlayer.start();
        });

        Button buttonMediaPlayerStop = findViewById(R.id.button2);
        buttonMediaPlayerStop.setOnClickListener((b) -> {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
            }

            if (visualizer != null) {
                visualizer.release();
                visualizer = null;
            }
        });


        Button buttonAudioRecordStart = findViewById(R.id.button3);
        buttonAudioRecordStart.setOnClickListener((b) -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            if (audioRecord != null) return;

            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBuffer);

            if (audioRecord.getState() == AudioRecord.STATE_UNINITIALIZED) {
                Log.e("audrec", "STATE_UNINITIALIZED");
                return;
            }

            audioRecord.startRecording();
            recordTask = new RecordTask();
            recordTask.execute();
        });

        Button buttonAudioRecordStop = findViewById(R.id.button4);
        buttonAudioRecordStop.setOnClickListener((b) -> {
            if (recordTask != null) {
                recordTask.cancel(true);
                recordTask = null;
            }

            if (audioRecord != null) {
                try {
                    audioRecord.stop();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
                audioRecord.release();
                audioRecord = null;
                Log.i("audrec", "Recording stopped");
            }

            if (visualizer != null) {
                visualizer.release();
                visualizer = null;
            }
        });
    }

    private class RecordTask extends AsyncTask<Void, byte[], Void> {
        @Override
        protected Void doInBackground(Void... params) {
            short[] buffer = new short[minBuffer];

            while (!isCancelled()) {
                int readSize = audioRecord.read(buffer, 0, minBuffer);

                if (readSize > 0) {
                    byte[] bytesForVisualizer = new byte[readSize];
                    for (int i = 0; i < readSize; i++) {
                        int amplified = buffer[i] * 5;
                        if (amplified > Short.MAX_VALUE) amplified = Short.MAX_VALUE;
                        if (amplified < Short.MIN_VALUE) amplified = Short.MIN_VALUE;
                        bytesForVisualizer[i] = (byte) ((amplified >> 8) - 128);
                    }
                    publishProgress(bytesForVisualizer);
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(byte[]... values) {
            if (waveformViewRecorder != null && values.length > 0) {
                waveformViewRecorder.updateVisualizer(values[0]);
            }
        }
    }
}
