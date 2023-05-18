package com.example.sariskascreensharingdemo;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.RelativeLayout;
import com.facebook.react.bridge.ReactContext;
import com.oney.WebRTCModule.GetUserMediaImpl;
import java.util.List;
import io.sariska.sdk.Conference;
import io.sariska.sdk.Connection;
import io.sariska.sdk.JitsiLocalTrack;
import io.sariska.sdk.JitsiRemoteTrack;
import io.sariska.sdk.Params;
import io.sariska.sdk.SariskaMediaTransport;

@RequiresApi(api = Build.VERSION_CODES.P)
public class MainActivity extends AppCompatActivity {
    private static Intent dataPermissionIntent;
    int PERMISSION_ALL = 1;
    String[] PERMISSIONS = {
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO,
            Manifest.permission.FOREGROUND_SERVICE
    };
    private Connection connection;

    List<JitsiLocalTrack> localTracks;
    private Conference conference;
    private ReactContext reactContext;
    private RelativeLayout mLocalContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }

        mLocalContainer = findViewById(R.id.local_video_view_container);

        // Need activity to pass it down to Get User Media Impl
        Activity activity = this;

        SariskaMediaTransport.initializeSdk(getApplication());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(this, ScreenCaptureService.class));
        }

        // Get the projection manager
        new Handler().postDelayed(() -> {
            // code to be executed after 10 seconds
            reactContext = SariskaMediaTransport.getReactContext();
            reactContext.onHostResume(activity);
            Bundle bundle = new Bundle();
            bundle.putBoolean("desktop", true);
            bundle.putInt("resolution", 360);
            SariskaMediaTransport.createLocalTracks(bundle, tracks -> {
                localTracks = tracks;
                setupLocalStream();
            });
        }, 10000);
    }

    // Invoked from startActivityResult inside React-Native-Webrtc
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        dataPermissionIntent = data;
        GetUserMediaImpl.setMediaData(data);
    }

    // Important for Screen Sharing
    public static Intent getMediaProjectionPermissionDetails(){
        return dataPermissionIntent;
    }

    private boolean hasPermissions(MainActivity context, String[] permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private void setupLocalStream() {
        String token = "eyJhbGciOiJSUzI1NiIsImtpZCI6ImVjN2ZmZWM4NGZkOGI3MzNiNzhmNDllMDJhNDBiZGRmNzU1NjM0ZDMyMGE3NGZlMzBlN2MxNGExMzZlNDExYTgiLCJ0eXAiOiJKV1QifQ.eyJjb250ZXh0Ijp7InVzZXIiOnsiaWQiOiI0ZW9qZnFyeSIsIm5hbWUiOiJzY3JlZWNoaW5nX3F1b2trYSJ9LCJncm91cCI6Ijg2In0sInN1YiI6Im55MzQ3a3VjY3JyMTVyNnhmbHpxY2giLCJyb29tIjoiKiIsImlhdCI6MTY4NDMzNTcwNywibmJmIjoxNjg0MzM1NzA3LCJpc3MiOiJzYXJpc2thIiwiYXVkIjoibWVkaWFfbWVzc2FnaW5nX2NvLWJyb3dzaW5nIiwiZXhwIjoxNjg0NDIyMTA3fQ.rJD4Z4y5v37sPla_Q5glbAiMLtERukkz9Q5I9A2Zs9RgDca00X-rwQ4MGaPYBcSPE1aIAq8xcQJ6Zs_4kfnQ4ASzc4EE143mgNWkG5gLUcx0NjWzOOCXDIuMKxU_rtGFOEsFygeegwdRGwfT6FcDceN8v2EhY6hhL6RCuRa2LsECv450gOdtCXbL_OFf6YUS3ehejkRJC3TFFJPWJLXsrFC0rLBgFKi_WCbOTs3WmakuSEJG2UE1SNgVSdQSOu_tnnfPOipxkDc1Yndsv5tmKVM4XQT8p42Roa823jfaZaLTT_dKNKgGt1k2CqBGDqgPJyCDxqeQ8athxXg4GYVZ_w";
        connection = SariskaMediaTransport.JitsiConnection(token, "test12", false);
        connection.addEventListener("CONNECTION_ESTABLISHED", this::createConference);
        connection.addEventListener("CONNECTION_FAILED", () -> {
        });
        connection.addEventListener("CONNECTION_DISCONNECTED", () -> {
        });
        connection.connect();
    }

    private void createConference() {
        conference = connection.initJitsiConference();

        conference.addEventListener("CONFERENCE_JOINED", () -> {
            System.out.println("conference joined");

            JitsiLocalTrack track = localTracks.get(0);
            SariskaMediaTransport.sendEvent("CONFERENCE_ACTION", Params.createParams("addTrack", track.getId()));
            System.out.println("past conference");
        });


        conference.addEventListener("CONFERENCE_LEFT", () -> {
        });

        conference.addEventListener("TRACK_ADDED", p -> {
            JitsiRemoteTrack track = (JitsiRemoteTrack) p;
        });

        conference.addEventListener("TRACK_REMOVED", p -> {
            JitsiRemoteTrack track = (JitsiRemoteTrack) p;
        });

        conference.join();
    }
}