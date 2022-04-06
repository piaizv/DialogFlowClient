package org.izv.dialogflowclient;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.cloud.dialogflow.v2.DetectIntentRequest;
import com.google.cloud.dialogflow.v2.DetectIntentResponse;
import com.google.cloud.dialogflow.v2.QueryInput;
import com.google.cloud.dialogflow.v2.SessionName;
import com.google.cloud.dialogflow.v2.SessionsClient;
import com.google.cloud.dialogflow.v2.SessionsSettings;
import com.google.cloud.dialogflow.v2.TextInput;
import com.google.common.collect.Lists;

import java.io.InputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    //https://github.com/veeyaarVR/dialogflowdemo

    private SessionsClient sessionClient;
    private SessionName sessionName;

    private TextView tvText;

    private final String uuid = UUID.randomUUID().toString();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialize();
    }

    private void initialize() {
        Button btDialogFlow = findViewById(R.id.btDialogFlow);
        EditText etText = findViewById(R.id.etText);
        tvText = findViewById(R.id.tvText);

        btDialogFlow.setOnClickListener(view -> {
            if(!etText.getText().toString().isEmpty()) {
                sendMessageToBot(etText.getText().toString());
            }
        });

        setupBot();
    }

    private void setupBot() {
        try {
            InputStream stream = this.getResources().openRawResource(R.raw.client);
            GoogleCredentials credentials = GoogleCredentials.fromStream(stream)
                    .createScoped(Lists.newArrayList("https://www.googleapis.com/auth/cloud-platform"));
            String projectId = ((ServiceAccountCredentials) credentials).getProjectId();
            SessionsSettings.Builder settingsBuilder = SessionsSettings.newBuilder();
            SessionsSettings sessionsSettings = settingsBuilder.setCredentialsProvider(
                    FixedCredentialsProvider.create(credentials)).build();
            sessionClient = SessionsClient.create(sessionsSettings);
            sessionName = SessionName.of(projectId, uuid);
        } catch (Exception e) {
            showMessage("\nexception in setupBot: " + e.getMessage() + "\n");
        }
    }

    private void sendMessageToBot(String message) {
        QueryInput input = QueryInput.newBuilder().setText(
                        TextInput.newBuilder().setText(message).setLanguageCode("es-ES")).build();
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    DetectIntentRequest detectIntentRequest =
                            DetectIntentRequest.newBuilder()
                                    .setSession(sessionName.toString())
                                    .setQueryInput(input)
                                    .build();
                    DetectIntentResponse detectIntentResponse = sessionClient.detectIntent(detectIntentRequest);
                    //intent, action, sentiment
                    String action = detectIntentResponse.getQueryResult().getAction();
                    String intent = detectIntentResponse.getQueryResult().getIntent().toString();
                    String sentiment = detectIntentResponse.getQueryResult().getSentimentAnalysisResult().toString();
                    if(detectIntentResponse != null) {
                        String botReply = detectIntentResponse.getQueryResult().getFulfillmentText();
                        if(!botReply.isEmpty()) {
                            showMessage(botReply + "\n");
                        } else {
                            showMessage("something went wrong\n");
                        }
                    } else {
                        showMessage("connection failed\n");
                    }
                } catch (Exception e) {
                    showMessage("\nexception in thread: " + e.getMessage() + "\n");
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }

    private void showMessage(String message) {
        runOnUiThread(() -> {
            tvText.setText(message + tvText.getText().toString());
        });
    }
}