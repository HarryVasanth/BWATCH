/*
 * Written by Harry Vasanth (harry.vasanth@m-iti.org)
 * Copyright (c) 2017.
 * Please acknowledge the creator by giving him/her credit for the work
 */

package com.harry.bwatch;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.harry.bwatch.services.WearListenerService;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class PhoneActivity extends Activity implements View.OnClickListener {
    TextView txtHRate, txtGyro, txtAcc, txtWearStatus, txtDIP;
    ImageView imgWear;
    WearListenerService wearListenerService = new WearListenerService();
    Timer udpTimer;
    ParseData parseData;
    int frequency = Integer.parseInt(getString(R.string.bwatch_default));
    String filename = "bWatch_" + new SimpleDateFormat("yyyyMMdd_HHmm'.csv'").format(new Date());

    public PhoneActivity() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone);

        txtWearStatus = (TextView) findViewById(R.id.txtWearStatus);
        imgWear = (ImageView) findViewById(R.id.imgWear);

        txtHRate = (TextView) findViewById(R.id.txtHRate);
        txtGyro = (TextView) findViewById(R.id.txtGyro);
        txtAcc = (TextView) findViewById(R.id.txtAcc);
        txtDIP = (TextView) findViewById(R.id.txtDIP);

        udpTimer = new Timer();
        parseData = new ParseData();
        udpTimer.scheduleAtFixedRate(parseData, 0, 1000 / frequency);

    }

    @Override
    public void onClick(View v) {
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    public void btnExit(View view) {
        this.finish();
        System.exit(0);
    }

    public void btnInfo(View view) {
        Toast.makeText(getApplicationContext(), getString(R.string.bwatch_system), Toast.LENGTH_LONG).show();
    }

    public void btnVerbose(View view) {

        if (txtHRate.getVisibility() != txtWearStatus.getVisibility()) {
            txtAcc.setVisibility(View.VISIBLE);
            txtGyro.setVisibility(View.VISIBLE);
            txtHRate.setVisibility(View.VISIBLE);
        } else {
            txtAcc.setVisibility(View.INVISIBLE);
            txtGyro.setVisibility(View.INVISIBLE);
            txtHRate.setVisibility(View.INVISIBLE);
        }

    }

    public void btnAudio(View view) {

        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(getString(R.string.bwatch_audio));
        if (launchIntent != null) {
            startActivity(launchIntent);
        }
    }

    public void writeData(String wearData) {

        File sdcard = Environment.getExternalStorageDirectory();
        File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/BWATCH_data");

        dir.mkdir();
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd,HH:mm:ss.SSS").format(new Date());

        File sessionFile = new File(dir, filename);
        if (!sessionFile.exists()) {
            try {
                sessionFile.createNewFile();
                BufferedWriter buf = new BufferedWriter(new FileWriter(sessionFile, true));
                buf.append("date,time,hr,gyro_x,gyro_y,gyro_z,acc_x,acc_y,acc_z");
                buf.newLine();
                buf.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
            }
        }
        try {
            BufferedWriter buf = new BufferedWriter(new FileWriter(sessionFile, true));
            buf.append(timeStamp + "," + wearData);
            buf.newLine();
            buf.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

        }
    }

    class ParseData extends TimerTask {
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    txtHRate.setText(wearListenerService.gethRate());
                    txtGyro.setText(wearListenerService.getGyroXYZ());
                    txtAcc.setText(wearListenerService.getAccXYZ());

                    if (wearListenerService.gethRate().equals("00")) {
                        imgWear.setImageResource(R.drawable.hr_off);
                        txtWearStatus.setText("Disconnected");
                        txtWearStatus.setTextColor(Color.parseColor("#f33030"));
                    } else {
                        imgWear.setImageResource(R.drawable.hr_on);
                        txtWearStatus.setText("Connected");
                        txtWearStatus.setTextColor(Color.parseColor("#82dd46"));

                    }
                }
            });
            if (txtWearStatus.getText().toString().equals("Connected")) {

                writeData(wearListenerService.gethRate() + "," + wearListenerService.getGyroXYZ() + "," + wearListenerService.getAccXYZ());
            }
        }
    }
}



