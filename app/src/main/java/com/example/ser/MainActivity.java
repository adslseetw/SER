package com.example.ser;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity  implements View.OnClickListener {

    private String path; // 儲存wav的路徑
    private static final int RECORDER_BPP = 16;
    private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
    private static int frequency = 44100;//44100
    private static int channelConfiguration = AudioFormat.CHANNEL_IN_STEREO;
    private static int EncodingBitRate = AudioFormat.ENCODING_PCM_16BIT;//AudioFormat.ENCODING_PCM_16BIT

    private AudioRecord audioRecord = null;
    private AudioTrack audioTrack = null;
    private int recBufSize = 0;//錄音陣列大小
    private int playBufSize = 0;//播放陣列大小
    private Thread recordingThread = null;//錄音執行續
    private boolean isRecording = false;//是否在錄音之布林值
    private boolean m_keep_running;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


    //按鈕事件
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnRecord: {
                System.out.println("Record start");
                enableToRecording(true);
                startRecording();
                System.out.println("Record end");
                break;
            }
            case R.id.btnStop: {
                System.out.println("stopRecord start");
                enableToRecording(false);
                stopRecording();
                System.out.println("stopRecord end");
                break;
            }
        }
    }


    //以下為錄音範本---------------------------------------------------------------------------------------

    private void enableButton(int id, boolean isEnable) {
        ((Button) findViewById(id)).setEnabled(isEnable);
    }

    private void enableToRecording(boolean isRecording) {
        enableButton(R.id.btnRecord, !isRecording);
        enableButton(R.id.btnStop, isRecording);
    }

    private String getFilename() {
        String filepath = Environment.getExternalStorageDirectory()
                .getAbsolutePath();
        File file = new File(filepath, AUDIO_RECORDER_FOLDER);

        if (file.exists()) {
            file.delete();
        }

        return (file.getAbsolutePath() + "/Test.wav");
    }

    private String getTempFilename() {
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath, AUDIO_RECORDER_FOLDER);

        if (!file.exists()) {
            file.mkdirs();
        }

        File tempFile = new File(filepath, AUDIO_RECORDER_TEMP_FILE);

        if (tempFile.exists())
            tempFile.delete();

        return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
    }

    private void startRecording() {
        createAudioRecord();
        audioRecord.startRecording();
        isRecording = true;//flag

        recordingThread = new Thread(new Runnable() {

            @Override
            public void run() {
                writeAudioDataToFile();
            }
        });

        recordingThread.start();//啟動執行續
    }

    //將音檔持續寫入到檔案中
    private void writeAudioDataToFile() {
        byte data[] = new byte[recBufSize];
        String filename = getTempFilename();//取得音檔位置
        //String filename ="/storage/media/temp.raw";
        System.out.println(filename);
        FileOutputStream os = null;

        try {
            os = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        int read = 0;

        if (null != os) {
            while (isRecording) {
                read = audioRecord.read(data, 0, recBufSize);

                if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                    try {
                        os.write(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //停止寫入音檔
    private void stopRecording() {
        if (null != audioRecord) {
            System.out.println("audioRecord != null");
            isRecording = false;

            audioRecord.stop();
            audioRecord.release();

            audioRecord = null;
            recordingThread = null;
        }

        copyWaveFile(getTempFilename(), getFilename());
        //deleteTempFile();
    }

    //刪除暫存檔案
    private void deleteTempFile() {
        File file = new File(getTempFilename());

        file.delete();
    }

    private void copyWaveFile(String inFilename, String outFilename) {
        System.out.println("copyWaveFile start");
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = frequency;
        int channels = 2;
        long byteRate = RECORDER_BPP * frequency * channels / 8;//16*44100*2/ 8

        byte[] data = new byte[recBufSize];
        double[] dataArray = new double[recBufSize];
        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            System.out.println("outFilename = "+outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;

            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);

//            int i = 0;
            while (in.read(data) != -1) {
                out.write(data);
//                dataArray[i] = toDouble(data);
//                System.out.println("dataArray["+i+"] = "+dataArray[i]);
//                i++;
            }

            in.close();
            out.close();
            System.out.println("copyWaveFile success!");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("copyWaveFile end");
        //---從此開始預強調與漢明窗


    }

    //音檔標頭
    private void WriteWaveFileHeader(FileOutputStream out, long totalAudioLen,
                                     long totalDataLen, long longSampleRate, int channels, long byteRate)
            throws IOException {

        byte[] header = new byte[44];

        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8); // block align
        header[33] = 0;
        header[34] = RECORDER_BPP; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 44);
    }

    public void createAudioRecord() {


        recBufSize = AudioRecord.getMinBufferSize(frequency,
                channelConfiguration, EncodingBitRate);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, frequency,
                channelConfiguration, EncodingBitRate, recBufSize);
        System.out.println(audioRecord.getRecordingState());
        System.out.println("000");
    }

    public void createAudioTrack() {
        playBufSize = AudioTrack.getMinBufferSize(frequency,
                channelConfiguration, EncodingBitRate);

        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, frequency,
                channelConfiguration, EncodingBitRate, playBufSize,
                AudioTrack.MODE_STREAM);
    }






    class PCMAudioTrack extends Thread {

        protected byte[] m_out_bytes;

        final String FILE_PATH = "/storage/emulated/0/AudioRecorder";
        final String FILE_NAME = "Test.wav";

        File file;
        FileInputStream in;

        public void init() {
            try {
                file = new File(FILE_PATH, FILE_NAME);
                file.createNewFile();
                in = new FileInputStream(file);

                // in.read(temp, 0, length);

                m_keep_running = true;

                createAudioTrack();

                m_out_bytes = new byte[playBufSize];

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void free() {
            m_keep_running = false;
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                Log.d("sleep exceptions...\n", "");
            }
        }

        public void run() {
            byte[] bytes_pkg = null;
            audioTrack.play();
            while (m_keep_running) {
                try {
                    in.read(m_out_bytes);
                    bytes_pkg = m_out_bytes.clone();
                    audioTrack.write(bytes_pkg, 0, bytes_pkg.length);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            audioTrack.stop();
            audioTrack = null;
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }





    // preEmphasis | s'N = sN - asN-1 | emph = 0.97
    public static byte[] preEmphasis(byte[] input) {
        byte[] output = new byte[input.length];
        float emph = 0.97f;

        output[0] = input[0];
        for (int i = 1; i < input.length; i++) {
            output[i] = (byte) (input[i] - emph * input[i - 1]);
        }

        return output;
    }

//    public static double[] calculateFFT(byte[] signal) {
//        final int mNumberOfFFTPoints = 1024;
//        double mMaxFFTSample;
//
//        @SuppressWarnings("unused")
//        int mPeakPos = 0;
//
//        double temp;
//        Complex[] y;
//        Complex[] complexSignal = new Complex[mNumberOfFFTPoints];
//        double[] absSignal = new double[mNumberOfFFTPoints / 2];
//
//        for (int i = 0; i < mNumberOfFFTPoints; i++) {
//            temp = (double) ((signal[2 * i] & 0xFF) | (signal[2 * i + 1] << 8)) / 32768.0F;
//            complexSignal[i] = new Complex(temp, 0.0);
//        }
//
//        y = FFT.fft(complexSignal); // --> Here I use FFT class
//
//        mMaxFFTSample = 0.0;
//        mPeakPos = 0;
//        for (int i = 0; i < (mNumberOfFFTPoints / 2); i++) {
//            absSignal[i] = Math.sqrt(Math.pow(y[i].re(), 2) + Math.pow(y[i].im(), 2));
//            if (absSignal[i] > mMaxFFTSample) {
//                mMaxFFTSample = absSignal[i];
//                mPeakPos = i;
//            }
//        }
//
//        return absSignal;
//
//    }

    public static float melToFreq(float input) {
        return (float) (700 * (Math.pow(10, input / 2595) - 1));
    }

    public static float freqToMel(float input) {
        return (float) (2595 * Math.log10(1 + (input / 700)));
    }

//    public static byte[] loadFile(String name) throws Exception {
//        @SuppressWarnings("unused")
//        int totalFramesRead = 0;
//        ByteArrayOutputStream outputStream=new ByteArrayOutputStream();
//        File fileIn = new File(name);
//        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(fileIn);
//        int bytesPerFrame = audioInputStream.getFormat().getFrameSize();
//        if (bytesPerFrame == AudioSystem.NOT_SPECIFIED) {
//            bytesPerFrame = 1;
//        }
//        int numBytes = 1024 * bytesPerFrame;
//        byte[] audioBytes = new byte[numBytes];
//        int numBytesRead = 0;
//        int numFramesRead = 0;
//        while ((numBytesRead = audioInputStream.read(audioBytes, 0, audioBytes.length)) != -1) {
//            outputSteam.wite(audioBytes, 0, numBytesRead);
//        }
//        byte[] bytesOut=outputStream.toByteArray();
//        return bytesOut;
//    }
}
