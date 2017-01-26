package com.example.maurizio.teoria_dei_segnali;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.Layout;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;


public class MainActivity extends Activity {
    private static final int RECORDER_BPP = 16;
    private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
    private static final String AUDIO_RECORDER_FOLDER = "TDS/AudioRecorder";
    private static final String AUDIO_RECORDER_FOLDER_OUT = "TDS/AudioRecorderOut";
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
    private static final int RECORDER_SAMPLERATE = 8000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord recorder = null;
    private int bufferSize = 0;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    private String NomeFileOutCript = "";
    private String NomeFileOutDecri = "";
    private String File_namefinale = "";
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setButtonHandlers();
        enableButtons(false);

        bufferSize = AudioRecord.getMinBufferSize(8000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
    }

    private void setButtonHandlers() {
        ((Button)findViewById(R.id.btnStart)).setOnClickListener(btnClick);
        ((Button)findViewById(R.id.btnStop)).setOnClickListener(btnClick);
    }

    private void enableButton(int id,boolean isEnable){
        ((Button)findViewById(id)).setEnabled(isEnable);
    }

    private void enableButtons(boolean isRecording) {
        enableButton(R.id.btnStart,!isRecording);
        enableButton(R.id.btnStop, isRecording);
    }

    private String getFilename(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,AUDIO_RECORDER_FOLDER);

        if(!file.exists()){
            file.mkdirs();
        }

        return (file.getAbsolutePath() + "/" + System.currentTimeMillis() + AUDIO_RECORDER_FILE_EXT_WAV);
    }



    private String getOutFilename(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,AUDIO_RECORDER_FOLDER_OUT);

        if(!file.exists()){
            file.mkdirs();
        }

        return (file.getAbsolutePath() + "/" + System.currentTimeMillis() + AUDIO_RECORDER_FILE_EXT_WAV);
    }

    private String getOutFilename(String name){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,AUDIO_RECORDER_FOLDER_OUT);

        if(!file.exists()){
            file.mkdirs();
        }

        return (file.getAbsolutePath() + "/" + name + AUDIO_RECORDER_FILE_EXT_WAV);
    }

    private String getTempFilename(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,AUDIO_RECORDER_FOLDER);

        if(!file.exists()){
            file.mkdirs();
        }

        File tempFile = new File(filepath,AUDIO_RECORDER_TEMP_FILE);

        if(tempFile.exists())
            tempFile.delete();

        return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
    }

    private void startRecording(){
        recorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING, bufferSize);

        int i = recorder.getState();
        if(i==1)
            recorder.startRecording();

        isRecording = true;

        recordingThread = new Thread(new Runnable() {

            @Override
            public void run() {
                writeAudioDataToFile();
            }
        },"AudioRecorder Thread");

        recordingThread.start();
    }

    private void writeAudioDataToFile(){
        byte data[] = new byte[bufferSize];
        String filename = getTempFilename();
        FileOutputStream os = null;

        try {
            os = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        int read = 0;

        if(null != os){
            while(isRecording){
                read = recorder.read(data, 0, bufferSize);

                if(AudioRecord.ERROR_INVALID_OPERATION != read){
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

    private void stopRecording(){
        if(null != recorder){
            isRecording = false;

            int i = recorder.getState();
            if(i==1)
                recorder.stop();
            recorder.release();

            recorder = null;
            recordingThread = null;
        }
        String filename = getFilename();
        copyWaveFile(getTempFilename(), filename);
        deleteTempFile();


        AppLog.logString(filename);
        File_namefinale = filename;
        do_the_fft();

      // String filename = getFilename();


    }

    private void deleteTempFile() {
        File file = new File(getTempFilename());

        file.delete();
    }

    private void copyWaveFile(String inFilename,String outFilename){
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;

        long totalDataLen = totalAudioLen+36;
        long longSampleRate = RECORDER_SAMPLERATE;
        int channels = 1;
        long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels/8;

        byte[] data = new byte[bufferSize];
        AppLog.logString("###"+outFilename);
        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;

            AppLog.logString("File size: " + totalDataLen);

            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);

            while(in.read(data) != -1){
                out.write(data);
            }

            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void WriteWaveFileHeader(
            FileOutputStream out, long totalAudioLen,
            long totalDataLen, long longSampleRate, int channels,
            long byteRate) throws IOException {

        byte[] header = new byte[44];

        header[0] = 'R';  // RIFF/WAVE header
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
        header[12] = 'f';  // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;  // format = 1
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
        header[32] = (byte) (2 * 16 / 8);  // block align
        header[33] = 0;
        header[34] = RECORDER_BPP;  // bits per sample
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
    private void do_the_fft()
    {
        Spinner spinner;
        LinearLayout layout_recording;
        LinearLayout layout_chose_freq;


        layout_recording = (LinearLayout) findViewById(R.id.LayoutRecording);
        layout_recording.setVisibility(LinearLayout.INVISIBLE);

        layout_chose_freq = (LinearLayout) findViewById(R.id.layout_chose_freq);
        layout_chose_freq.setVisibility(LinearLayout.VISIBLE);


        spinner = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,R.array.frequency_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);



        ((Button)findViewById(R.id.button_do_fft)).setOnClickListener(btnClick);
        ((Button)findViewById(R.id.button_ori)).setOnClickListener(btnClick);
        ((Button)findViewById(R.id.button_cript)).setOnClickListener(btnClick);
        ((Button)findViewById(R.id.button_decrip)).setOnClickListener(btnClick);

    }
    private View.OnClickListener btnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            AppLog.logString("$$ Pressionee Tastoooooooo!!!!!!!");
            AppLog.logString("é stato premuto questo tastoooo!! ==== " + v.getId());
            AppLog.logString("||Bottone Start|| = " + R.id.btnStart);
            AppLog.logString("||Bottone Stop|| = " + R.id.btnStop);
            AppLog.logString("||Bottone Do FFt|| = " + R.id.button_do_fft);
            AppLog.logString("||Bottone play ori|| = " + R.id.button_ori);
            AppLog.logString("||Bottone play cri|| = " + R.id.button_cript);
            AppLog.logString("||Bottone play dec|| = " + R.id.button_decrip);
            switch(v.getId()){
                case R.id.btnStart:{
                    AppLog.logString("Start Recording");

                    enableButtons(true);
                    startRecording();

                    break;
                }
                case R.id.btnStop:{
                    AppLog.logString("Start Recording");

                    enableButtons(false);
                    stopRecording();

                    break;
                }

                case R.id.button_do_fft:{
                    AppLog.logString("DO FFT");
                    int get_fft_freq = Integer.parseInt(((Spinner)findViewById(R.id.spinner)).getSelectedItem().toString());
                   // AppLog.logString("################################################## x = " + get_fft_freq + "       " + File_namefinale);
                    ((LinearLayout)findViewById(R.id.layout_chose_freq)).setVisibility(LinearLayout.INVISIBLE);
                    ((LinearLayout)findViewById(R.id.LayoutFinale)).setVisibility(LinearLayout.VISIBLE);
                    DownloadWebPageTask task = new DownloadWebPageTask(File_namefinale,get_fft_freq);
                    task.execute();
                    break;
                }

                case R.id.button_ori:{
                    AppLog.logString("############# PLAY AUDIO" + File_namefinale + "###################");
                    Context appContext = getApplicationContext();
                    MediaPlayer mp = MediaPlayer.create(appContext, Uri.parse(File_namefinale));
                    mp.start();
                    break;
                }

                case R.id.button_cript:{
                    AppLog.logString("############# PLAY AUDIO" + NomeFileOutCript + "###################");
                   Context appContext = getApplicationContext();
                    MediaPlayer mp = MediaPlayer.create(appContext, Uri.parse(NomeFileOutCript));
                    mp.start();
                    break;
                }
                case R.id.button_decrip:{
                    AppLog.logString("############# PLAY AUDIO" + NomeFileOutDecri + "###################");

                    Context appContext = getApplicationContext();
                    MediaPlayer mp = MediaPlayer.create(appContext, Uri.parse(NomeFileOutDecri));
                    mp.start();
                    break;
                }

                //TODO button do fft
                    /* DownloadWebPageTask task = new DownloadWebPageTask(filename,1024);
        task.execute();*/

            }
        }
    };
    class read_wav
    {
        char riff[] = new char[4];
        int  sread, swrite;
        int  fsize;
        char wave[] = new char[4];
        char fmt[] = new char[4];
        int  nbytes;
        short  ccode;
        short  channels;
        int rate;
        int avgrate;
        short blockalign;
        short bps; // bits per sample
        char data[] = new char[4];
        int csize[] = new int[50]; // max 50 chunks
        int ncsize = 0;
        int ibyte; // byte of cound
        char more[] = new char[4];
        int smin = 0;
        int smax = 0;
        int savg;
        int nbread; // number of bytes read
        int outix;  // number of output samples
        int A[];    // output sound amplitude, bigger than outix

        public read_wav(String file_in, String file_out, boolean ifwrite)
        {
            wav_read(file_in);
            if(ifwrite) wav_write(file_out);
        }

        void wav_read(String inp)
        {
            System.out.println("read_wav running,  reading "+inp);
            outix = 0;

            try
            {
                FileInputStream ds = new FileInputStream(inp);

                sread = 4;
                for(int i=0; i<sread; i++) riff[i] = (char)ds.read();
                System.out.println("read "+sread+" bytes, should be 4");
                System.out.println("first 4 bytes should be RIFF "+
                        riff[0]+riff[1]+riff[2]+riff[3]);

                fsize = 0;
                smax = 1;
                for(int i=0; i<4; i++) { fsize += ds.read()*smax; smax *= 256;}
                System.out.println("file has "+fsize+" +8 bytes");

                for(int i=0; i<4; i++) wave[i] = (char)ds.read();
                System.out.println("4 bytes should be WAVE "+
                        wave[0]+wave[1]+wave[2]+wave[3]);

                for(int i=0; i<4; i++) fmt[i] = (char)ds.read();
                System.out.println("4 bytes should be fmt  "+
                        fmt[0]+fmt[1]+fmt[2]+fmt[3]);

                nbytes = 0;
                smax = 1;
                for(int i=0; i<4; i++) { nbytes += ds.read()*smax; smax *= 256;}
                System.out.println("block has "+nbytes+" bytes");

                ccode = 0;
                smax = 1;
                for(int i=0; i<2; i++) { ccode += ds.read()*smax; smax *= 256;}
                System.out.println("compression code = "+ccode);

                channels = 0;
                smax = 1;
                for(int i=0; i<2; i++) { channels += ds.read()*smax; smax *= 256;}
                System.out.println("channels = "+channels);

                rate = 0;
                smax = 1;
                for(int i=0; i<4; i++) { rate += ds.read()*smax; smax *= 256;}
                System.out.println("rate = "+rate);

                avgrate = 0;
                smax = 1;
                for(int i=0; i<4; i++) { avgrate += ds.read()*smax; smax *= 256;}
                System.out.println("avg rate = "+avgrate);

                blockalign = 0;
                smax = 1;
                for(int i=0; i<2; i++) { blockalign += ds.read()*smax; smax *= 256;}
                System.out.println("blockalign = "+blockalign);

                bps = 0;
                smax = 1;
                for(int i=0; i<2; i++) { bps += ds.read()*smax; smax *= 256;}
                System.out.println("bits per sample = "+bps);

                for(int i=0; i<4; i++) data[i] = (char)ds.read();
                System.out.println("4 bytes should be data "+
                        data[0]+data[1]+data[2]+data[3]);

                csize[ncsize] = 0;
                smax = 1;
                for(int i=0; i<4; i++) { csize[ncsize] += ds.read()*smax; smax *= 256;}
                System.out.println("chunk has "+csize[ncsize]+" bytes");

                nbread = 44;
                System.out.println(nbread+" bytes read so far");
                A = new int[fsize+8-nbread]; // -8 +8 below thus big enough
                outix = 0;

                savg = 0;
                for(int i=0; i<csize[ncsize]; i++)
                {
                    ibyte = ds.read(); // signed byte
                    if(ibyte>127) ibyte = ibyte-256;
                    A[outix] = ibyte;
                    outix++;
                    savg = savg + ibyte;
                    if(i==0) {smin=ibyte; smax=ibyte;}
                    smin = ibyte<smin?ibyte:smin;
                    smax = ibyte>smax?ibyte:smax;
                    if(i<10 || i>csize[ncsize]-10)
                        System.out.println("sound byte ="+ibyte);
                }
                savg = savg / csize[ncsize];
                System.out.println("smin="+smin+", smax="+smax+", savg="+savg);

                nbread = nbread+csize[ncsize];
                System.out.println(nbread+" bytes read so far");

                // read rest of chunks
                while(nbread+17<fsize)
                {
                    ncsize++;
                    for(int i=0; i<4; i++) more[i] = (char)ds.read();
                    System.out.println("4 bytes should be WAVE "+
                            more[0]+more[1]+more[2]+more[3]);

                    csize[ncsize] = 0;
                    smax = 1;
                    for(int i=0; i<4; i++) { csize[ncsize] += ds.read()*smax; smax *= 256;}
                    System.out.println("chunk has "+csize[ncsize]+" bytes");

                    for(int i=0; i<csize[ncsize]; i++)
                    {
                        ibyte = ds.read(); // signed byte
                        if(ibyte>127) ibyte = ibyte-256;
                        A[outix] = ibyte;
                        outix++;
                    }
                    nbread = nbread+8+csize[ncsize];
                    System.out.println(nbread+" bytes read so far");
                }
                ds.close();
            }
            catch(Exception e)
            {
                System.out.println("wav_read, some exception thrown");
                System.out.println(e.toString());
            }
            System.out.println("end wav_read");
        } // end wav_read


        void wav_write(String outp)
        {
            System.out.println("read_write running,  writing "+outp);
            int inix = 0;

            ncsize=0;
            try
            {
                FileOutputStream ds = new FileOutputStream(outp);

                sread = 4;
                for(int i=0; i<sread; i++) ds.write(riff[i]);
                System.out.println("wrote "+sread+" bytes, should be 4");
                System.out.println("wrote first 4 bytes should be RIFF "+
                        riff[0]+riff[1]+riff[2]+riff[3]);

                smax = fsize;
                for(int i=0; i<4; i++) { ds.write(smax%256); smax /= 256;}
                System.out.println("wrote file has "+fsize+" +8 bytes");

                for(int i=0; i<4; i++) ds.write(wave[i]);
                System.out.println("wrote 4 bytes should be WAVE "+
                        wave[0]+wave[1]+wave[2]+wave[3]);

                for(int i=0; i<4; i++) ds.write(fmt[i]);
                System.out.println("wrote 4 bytes should be fmt  "+
                        fmt[0]+fmt[1]+fmt[2]+fmt[3]);

                smax = nbytes;
                for(int i=0; i<4; i++) { ds.write(smax%256); smax /= 256;}
                System.out.println("wrote block has "+nbytes+" bytes");

                smax = ccode;
                for(int i=0; i<2; i++) { ds.write(smax%256); smax /= 256;}
                System.out.println("wrote compression code = "+ccode);

                smax = channels;
                for(int i=0; i<2; i++) { ds.write(smax%256); smax /= 256;}
                System.out.println("wrote channels = "+channels);

                smax = rate;
                for(int i=0; i<4; i++) { ds.write(smax%256); smax /= 256;}
                System.out.println("wrote rate = "+rate);

                smax = avgrate;
                for(int i=0; i<4; i++) { ds.write(smax%256); smax /= 256;}
                System.out.println("wrote avg rate = "+avgrate);


                smax = blockalign;
                for(int i=0; i<2; i++) { ds.write(smax%256); smax /= 256;}
                System.out.println("wrote blockalign = "+blockalign);


                smax = bps;
                for(int i=0; i<2; i++) { ds.write(smax%256); smax /= 256;}
                System.out.println("wrote bits per sample = "+bps);

                for(int i=0; i<4; i++) ds.write(data[i]);
                System.out.println("wrote 4 bytes should be data "+
                        data[0]+data[1]+data[2]+data[3]);

                smax = csize[ncsize];
                for(int i=0; i<4; i++) { ds.write(smax%256); smax /= 256;}
                System.out.println("wrote chunk has "+csize[ncsize]+" bytes");

                nbread = 44;
                System.out.println(nbread+" bytes written so far");

                savg = 0;
                for(int i=0; i<csize[ncsize]; i++)
                {
                    ibyte = A[inix];
                    inix++;
                    if(ibyte<0) ibyte = ibyte+256;
                    ds.write(ibyte); // signed byte
                    if(inix>=outix) break;
                }

                nbread = nbread+csize[ncsize];
                System.out.println(nbread+" bytes written so far");

                // write rest of chunks
                while(nbread+17<fsize)
                {
                    ncsize++;
                    if(inix>=outix) break;
                    for(int i=0; i<4; i++) ds.write(more[i]);
                    System.out.println("wrote 4 bytes should be WAVE "+
                            more[0]+more[1]+more[2]+more[3]);

                    smax = csize[ncsize];
                    for(int i=0; i<4; i++) { ds.write(smax%256); smax /= 256;}
                    System.out.println("wrote chunk has "+csize[ncsize]+" bytes");

                    for(int i=0; i<csize[ncsize]; i++)
                    {
                        ibyte = A[inix];
                        inix++;
                        if(ibyte<0) ibyte = ibyte+256;
                        ds.write(ibyte); // signed byte
                        if(inix>=outix) break;
                    }
                    nbread = nbread+8+csize[ncsize];
                    System.out.println(nbread+" bytes written so far");
                }
                // ds.write(0);  // on some files
                ds.close();
            }
            catch(Exception e)
            {
                System.out.println("wav_write, some exception thrown");
            }
            System.out.println("end wav_write");
        } // end wav_write

  /*public static void main (String[] args)
  {
    String file_in = "ok.wav";
    if(args.length>0)
    {
      file_in = args[0];
    }
    String file_out = "junk.wav";
    if(args.length>1)
    {
      file_out = args[1];
    }
    new read_wav(file_in, file_out, true);
  } // end main
  */
    } // end class read_wav of read_wav.java



    private class DownloadWebPageTask extends AsyncTask<Void,Void,Void> {


        String file_name;
        int fft_fq;

        public DownloadWebPageTask(String Name, int fft_fq) {

            file_name = Name;
            this.fft_fq = fft_fq;
        }

        @Override
        protected void onPreExecute() {

            super.onPreExecute();
            ((TextView) findViewById(R.id.testo_fft_during)).setText("Criptaggio in corso\nfrequenza fft = " + fft_fq);


            //bt3.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);


            ((TextView) findViewById(R.id.testo_fft_during)).setText("Criptaggio Eseguito con successo");
            ((ProgressBar) findViewById(R.id.progressBar)).setVisibility(ProgressBar.INVISIBLE);

        }


        protected void fft_fft(String file_out, int fft_ffr) {
            int[] array_fft;
            //int fft_freq = 1024;
            if (fft_ffr == 1024) {
                array_fft = new int[]{0, 219, 178, 485, 35, 313, 448, 128, 151, 420, 31, 260, 19, 110, 50, 139, 447, 88, 268, 238, 354, 453, 5, 271, 44, 154, 316, 246, 83, 325, 22, 317, 107, 127, 229, 443, 97, 34, 209, 223, 122, 284, 466, 269, 456, 295, 121, 234, 410, 43, 279, 211, 125, 425, 495, 149, 174, 145, 74, 451, 140, 73, 272, 162, 380, 21, 66, 161, 70, 76, 108, 79, 175, 158, 258, 60, 509, 85, 69, 32, 233, 194, 57, 338, 142, 180, 291, 94, 17, 198, 192, 341, 368, 382, 75, 210, 20, 179, 201, 241, 131, 312, 254, 126, 315, 441, 38, 390, 186, 372, 188, 42, 414, 479, 440, 333, 250, 299, 137, 47, 283, 355, 216, 280, 167, 480, 449, 176, 249, 397, 417, 12, 381, 489, 132, 344, 437, 389, 348, 314, 369, 266, 144, 365, 472, 48, 282, 477, 172, 89, 67, 138, 203, 240, 395, 335, 177, 29, 55, 163, 4, 460, 328, 404, 13, 171, 396, 129, 116, 225, 467, 297, 478, 465, 205, 230, 181, 376, 189, 212, 185, 277, 11, 428, 507, 264, 430, 217, 471, 1, 182, 434, 327, 72, 193, 222, 259, 400, 118, 426, 148, 41, 36, 199, 236, 294, 345, 23, 93, 95, 224, 245, 213, 195, 169, 7, 2, 458, 274, 384, 156, 405, 248, 24, 99, 498, 450, 501, 239, 488, 343, 387, 329, 392, 28, 502, 332, 64, 267, 334, 112, 141, 379, 45, 255, 374, 323, 286, 62, 457, 336, 6, 406, 218, 196, 366, 82, 311, 147, 155, 252, 403, 337, 444, 307, 393, 276, 202, 385, 135, 424, 120, 183, 468, 361, 475, 473, 357, 455, 153, 445, 63, 300, 220, 287, 100, 319, 290, 30, 367, 119, 427, 184, 298, 431, 187, 228, 104, 16, 206, 251, 102, 265, 263, 391, 257, 296, 191, 435, 364, 505, 438, 429, 415, 231, 49, 208, 503, 261, 114, 401, 324, 124, 78, 342, 166, 482, 105, 407, 33, 197, 273, 394, 302, 65, 470, 358, 339, 226, 10, 215, 84, 350, 388, 134, 491, 232, 496, 474, 46, 86, 270, 26, 96, 56, 399, 305, 227, 352, 462, 370, 285, 346, 469, 111, 157, 421, 306, 92, 418, 90, 39, 411, 309, 408, 359, 288, 117, 207, 3, 433, 242, 303, 310, 301, 101, 506, 113, 454, 377, 14, 423, 165, 98, 204, 493, 349, 483, 373, 150, 413, 464, 494, 9, 106, 481, 484, 71, 432, 278, 123, 40, 490, 81, 422, 321, 115, 275, 322, 402, 497, 37, 363, 386, 247, 130, 330, 459, 214, 340, 463, 508, 436, 152, 173, 15, 442, 452, 500, 293, 58, 304, 77, 143, 398, 54, 499, 52, 27, 289, 486, 244, 8, 360, 68, 146, 461, 326, 318, 80, 371, 51, 409, 446, 243, 375, 237, 59, 136, 18, 439, 160, 235, 412, 378, 164, 347, 168, 492, 292, 383, 170, 61, 133, 419, 262, 320, 190, 504, 159, 416, 91, 221, 331, 256, 200, 53, 362, 109, 253, 510, 353, 25, 87, 351, 476, 103, 487, 281, 308, 356, 511, 511, 356, 308, 281, 487, 103, 476, 351, 87, 25, 353, 510, 253, 109, 362, 53, 200, 256, 331, 221, 91, 416, 159, 504, 190, 320, 262, 419, 133, 61, 170, 383, 292, 492, 168, 347, 164, 378, 412, 235, 160, 439, 18, 136, 59, 237, 375, 243, 446, 409, 51, 371, 80, 318, 326, 461, 146, 68, 360, 8, 244, 486, 289, 27, 52, 499, 54, 398, 143, 77, 304, 58, 293, 500, 452, 442, 15, 173, 152, 436, 508, 463, 340, 214, 459, 330, 130, 247, 386, 363, 37, 497, 402, 322, 275, 115, 321, 422, 81, 490, 40, 123, 278, 432, 71, 484, 481, 106, 9, 494, 464, 413, 150, 373, 483, 349, 493, 204, 98, 165, 423, 14, 377, 454, 113, 506, 101, 301, 310, 303, 242, 433, 3, 207, 117, 288, 359, 408, 309, 411, 39, 90, 418, 92, 306, 421, 157, 111, 469, 346, 285, 370, 462, 352, 227, 305, 399, 56, 96, 26, 270, 86, 46, 474, 496, 232, 491, 134, 388, 350, 84, 215, 10, 226, 339, 358, 470, 65, 302, 394, 273, 197, 33, 407, 105, 482, 166, 342, 78, 124, 324, 401, 114, 261, 503, 208, 49, 231, 415, 429, 438, 505, 364, 435, 191, 296, 257, 391, 263, 265, 102, 251, 206, 16, 104, 228, 187, 431, 298, 184, 427, 119, 367, 30, 290, 319, 100, 287, 220, 300, 63, 445, 153, 455, 357, 473, 475, 361, 468, 183, 120, 424, 135, 385, 202, 276, 393, 307, 444, 337, 403, 252, 155, 147, 311, 82, 366, 196, 218, 406, 6, 336, 457, 62, 286, 323, 374, 255, 45, 379, 141, 112, 334, 267, 64, 332, 502, 28, 392, 329, 387, 343, 488, 239, 501, 450, 498, 99, 24, 248, 405, 156, 384, 274, 458, 2, 7, 169, 195, 213, 245, 224, 95, 93, 23, 345, 294, 236, 199, 36, 41, 148, 426, 118, 400, 259, 222, 193, 72, 327, 434, 182, 1, 471, 217, 430, 264, 507, 428, 11, 277, 185, 212, 189, 376, 181, 230, 205, 465, 478, 297, 467, 225, 116, 129, 396, 171, 13, 404, 328, 460, 4, 163, 55, 29, 177, 335, 395, 240, 203, 138, 67, 89, 172, 477, 282, 48, 472, 365, 144, 266, 369, 314, 348, 389, 437, 344, 132, 489, 381, 12, 417, 397, 249, 176, 449, 480, 167, 280, 216, 355, 283, 47, 137, 299, 250, 333, 440, 479, 414, 42, 188, 372, 186, 390, 38, 441, 315, 126, 254, 312, 131, 241, 201, 179, 20, 210, 75, 382, 368, 341, 192, 198, 17, 94, 291, 180, 142, 338, 57, 194, 233, 32, 69, 85, 509, 60, 258, 158, 175, 79, 108, 76, 70, 161, 66, 21, 380, 162, 272, 73, 140, 451, 74, 145, 174, 149, 495, 425, 125, 211, 279, 43, 410, 234, 121, 295, 456, 269, 466, 284, 122, 223, 209, 34, 97, 443, 229, 127, 107, 317, 22, 325, 83, 246, 316, 154, 44, 271, 5, 453, 354, 238, 268, 88, 447, 139, 50, 110, 19, 260, 31, 420, 151, 128, 448, 313, 35, 485, 178, 219, 0};
            } else if (fft_ffr == 512) {
                array_fft = new int[]{0, 135, 120, 40, 215, 201, 172, 147, 234, 112, 161, 16, 122, 80, 20, 91, 173, 177, 242, 4, 62, 196, 192, 2, 182, 54, 84, 176, 213, 204, 131, 127, 45, 220, 59, 149, 57, 155, 186, 50, 205, 187, 37, 119, 218, 36, 237, 248, 108, 128, 230, 75, 207, 136, 65, 72, 163, 78, 178, 10, 113, 104, 245, 31, 89, 92, 158, 140, 226, 166, 200, 61, 244, 190, 88, 98, 95, 105, 167, 70, 28, 29, 83, 90, 67, 165, 102, 208, 144, 17, 227, 150, 21, 238, 253, 23, 106, 159, 53, 68, 103, 32, 222, 216, 202, 146, 247, 39, 79, 235, 153, 183, 47, 197, 241, 96, 251, 42, 7, 85, 179, 26, 151, 38, 145, 76, 6, 35, 132, 117, 93, 110, 162, 87, 198, 188, 175, 139, 137, 126, 180, 212, 138, 171, 224, 217, 232, 25, 240, 211, 99, 86, 189, 225, 134, 228, 246, 64, 160, 133, 223, 100, 30, 123, 191, 164, 111, 170, 94, 250, 199, 142, 249, 130, 239, 195, 49, 125, 33, 58, 27, 3, 210, 60, 22, 55, 115, 203, 66, 74, 185, 82, 114, 174, 19, 12, 168, 107, 221, 233, 52, 11, 18, 63, 118, 8, 46, 209, 24, 71, 69, 51, 214, 129, 77, 41, 143, 14, 15, 219, 5, 236, 141, 13, 184, 44, 231, 73, 152, 124, 116, 254, 101, 243, 121, 169, 148, 34, 1, 109, 48, 181, 157, 252, 229, 43, 154, 97, 206, 194, 193, 156, 56, 9, 81, 255, 255, 81, 9, 56, 156, 193, 194, 206, 97, 154, 43, 229, 252, 157, 181, 48, 109, 1, 34, 148, 169, 121, 243, 101, 254, 116, 124, 152, 73, 231, 44, 184, 13, 141, 236, 5, 219, 15, 14, 143, 41, 77, 129, 214, 51, 69, 71, 24, 209, 46, 8, 118, 63, 18, 11, 52, 233, 221, 107, 168, 12, 19, 174, 114, 82, 185, 74, 66, 203, 115, 55, 22, 60, 210, 3, 27, 58, 33, 125, 49, 195, 239, 130, 249, 142, 199, 250, 94, 170, 111, 164, 191, 123, 30, 100, 223, 133, 160, 64, 246, 228, 134, 225, 189, 86, 99, 211, 240, 25, 232, 217, 224, 171, 138, 212, 180, 126, 137, 139, 175, 188, 198, 87, 162, 110, 93, 117, 132, 35, 6, 76, 145, 38, 151, 26, 179, 85, 7, 42, 251, 96, 241, 197, 47, 183, 153, 235, 79, 39, 247, 146, 202, 216, 222, 32, 103, 68, 53, 159, 106, 23, 253, 238, 21, 150, 227, 17, 144, 208, 102, 165, 67, 90, 83, 29, 28, 70, 167, 105, 95, 98, 88, 190, 244, 61, 200, 166, 226, 140, 158, 92, 89, 31, 245, 104, 113, 10, 178, 78, 163, 72, 65, 136, 207, 75, 230, 128, 108, 248, 237, 36, 218, 119, 37, 187, 205, 50, 186, 155, 57, 149, 59, 220, 45, 127, 131, 204, 213, 176, 84, 54, 182, 2, 192, 196, 62, 4, 242, 177, 173, 91, 20, 80, 122, 16, 161, 112, 234, 147, 172, 201, 215, 40, 120, 135, 0};
            }


            else if (fft_ffr == 256) {
                array_fft = new int[]{0,63,5,83,6,120,118,82,49,23,11,61,40,114,12,116,32,15,117,102,37,4,8,66,100,41,54,62,43,56,38,16,98,35,123,69,97,10,58,113,25,65,13,31,50,28,46,71,103,104,115,95,121,55,106,126,53,93,78,108,68,86,87,48,76,84,111,33,51,57,36,80,73,75,21,112,59,19,3,90,81,42,92,70,105,1,119,110,29,88,91,101,18,109,64,24,2,44,17,20,47,85,89,60,30,77,14,96,52,74,22,27,7,9,34,107,125,26,67,99,124,94,39,72,79,45,122,127,127,122,45,79,72,39,94,124,99,67,26,125,107,34,9,7,27,22,74,52,96,14,77,30,60,89,85,47,20,17,44,2,24,64,109,18,101,91,88,29,110,119,1,105,70,92,42,81,90,3,19,59,112,21,75,73,80,36,57,51,33,111,84,76,48,87,86,68,108,78,93,53,126,106,55,121,95,115,104,103,71,46,28,50,31,13,65,25,113,58,10,97,69,123,35,98,16,38,56,43,62,54,41,100,66,8,4,37,102,117,15,32,116,12,114,40,61,11,23,49,82,118,120,6,83,5,63,0};
            }
            else if (fft_ffr == 128) {
                array_fft = new int[]{0, 57, 34, 7, 20, 18, 55, 1, 39, 36, 29, 50, 10, 33, 46, 37, 31, 17, 5, 45, 24, 40, 25, 14, 16, 22, 27, 2, 30, 23, 28, 44, 48, 32, 43, 26, 15, 8, 9, 41, 58, 42, 47, 12, 4, 13, 11, 53, 19, 60, 49, 62, 56, 59, 6, 21, 3, 52, 51, 38, 35, 54, 61, 63, 63, 61, 54, 35, 38, 51, 52, 3, 21, 6, 59, 56, 62, 49, 60, 19, 53, 11, 13, 4, 12, 47, 42, 58, 41, 9, 8, 15, 26, 43, 32, 48, 44, 28, 23, 30, 2, 27, 22, 16, 14, 25, 40, 24, 45, 5, 17, 31, 37, 46, 33, 10, 50, 29, 36, 39, 1, 55, 18, 20, 7, 34, 57, 0};
            } else if (fft_ffr == 64) {
                array_fft = new int[]{0, 17, 16, 9, 20, 5, 12, 4, 18, 23, 8, 24, 14, 21, 26, 19, 29, 13, 2, 25, 22, 28, 1, 6, 3, 10, 30, 15, 27, 7, 11, 31, 31, 11, 7, 27, 15, 30, 10, 3, 6, 1, 28, 22, 25, 2, 13, 29, 19, 26, 21, 14, 24, 8, 23, 18, 4, 12, 5, 20, 9, 16, 17, 0};
            } else if (fft_ffr == 32) {
                array_fft = new int[]{0, 7, 4, 8, 5, 1, 13, 11, 9, 10, 3, 6, 2, 14, 12, 15, 15, 12, 14, 2, 6, 3, 10, 9, 11, 13, 1, 5, 8, 4, 7, 0};
            } else if (fft_ffr == 16) {
                array_fft = new int[]{0, 3, 6, 2, 4, 1, 5, 7, 7, 5, 1, 4, 2, 6, 3, 0};
            } else {
                array_fft = new int[]{0, 1, 2, 3, 3, 2, 1, 0};
            }
            int m = 0;
            read_wav read = new read_wav(file_name, file_out, false);
            int n = read.outix;  // number of sound samples

            System.out.println("#####");
            System.out.println(n);
            System.out.println("#####");
            for (m = 64; m < 2500000; m *= 2) if (m > 2 * n) break;
            System.out.println("power of 2 m=" + m);

            double A[] = new double[m];
            //double B[] = new double[m];
            for (int i = 0; i < 2 * n; i += 2) A[i] = (double) read.A[i / 2]; // real
            for (int i = 1; i < 2 * n; i += 2) A[i] = 0.0;              // imag
            FFT fft = new FFT(fft_ffr, 1);
            fft.transform(A);
            complex[] complessi = new complex[A.length / 2];
            for (int i = 0; i < A.length; i += 2) {
                complessi[i / 2] = new complex(A[i], A[i + 1]);
            }


            for (int i = 0; i < complessi.length; i++) {
                int pos = array_fft[i % fft_ffr];
                complex c = complessi[i];
                complessi[i] = complessi[pos];
                complessi[pos] = c;
            }


            for (int i = 0; i < A.length; i += 2) {
                A[i] = complessi[i / 2].getRe();
                A[i + 1] = complessi[i / 2].getIm();
            }
            fft = new FFT(1024, -1);
            fft.transform(A);


            for (int i = 0; i < 2 * n; i += 2) read.A[i / 2] = (int) (0.99 * A[i]);
            for (int i = 0; i < n; i++) {
                if (read.A[i] > 127) read.A[i] = 127;
                if (read.A[i] < -127) read.A[i] = -127;
            }
            read.wav_write(file_out);
            NomeFileOutCript = file_out;

//#######################################################################################TEST######################################################

            for (int i = 0; i < complessi.length; i++) {
                int pos = array_fft[i % fft_ffr];
                complex c = complessi[i];
                complessi[i] = complessi[pos];
                complessi[pos] = c;
            }


            for (int i = 0; i < A.length; i += 2) {
                A[i] = complessi[i / 2].getRe();
                A[i + 1] = complessi[i / 2].getIm();
            }


            fft = new FFT(1024, -1);
            fft.transform(A);


            for (int i = 0; i < 2 * n; i += 2) read.A[i / 2] = (int) (0.99 * A[i]);
            for (int i = 0; i < n; i++) {
                if (read.A[i] > 127) read.A[i] = 127;
                if (read.A[i] < -127) read.A[i] = -127;
            }

            //String name = "decri_ff_" + fft_ffr;

            NomeFileOutDecri = getOutFilename();
            read.wav_write(NomeFileOutDecri);
            System.out.println("Write decription");

//#######################################################################################TEST######################################################

            System.out.println("End!");
            AppLog.logString("Done FFt!");

        }


        @Override
        protected Void doInBackground(Void... params) {



            String file_out = getOutFilename();

            fft_fft(file_out  , fft_fq);

            return null;
        }
    }





}
