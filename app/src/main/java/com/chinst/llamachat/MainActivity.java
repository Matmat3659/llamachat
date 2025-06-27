package com.chinst.llamachat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.io.*;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_CODE = 123;
    private TextView outputText;
    private EditText inputText;
    private String binaryName = "llama-cli";
    private String modelPath = "/storage/emulated/0/llama/model.gguf";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        outputText = findViewById(R.id.outputText);
        inputText = findViewById(R.id.inputText);
        Button sendButton = findViewById(R.id.sendButton);

        ActivityCompat.requestPermissions(this,
            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_CODE);

        extractBinary(binaryName);

        sendButton.setOnClickListener(v -> {
            String prompt = inputText.getText().toString();
            runLlama(prompt);
        });
    }

    private void extractBinary(String name) {
        try {
            File outFile = new File(getFilesDir(), name);
            if (!outFile.exists()) {
                InputStream in = getAssets().open(name);
                FileOutputStream out = new FileOutputStream(outFile);
                byte[] buf = new byte[4096];
                int len;
                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                in.close(); out.close();
                outFile.setExecutable(true);
            }
        } catch (Exception e) {
            outputText.setText("Error copying binary: " + e.getMessage());
        }
    }

    private void runLlama(String prompt) {
        try {
            File bin = new File(getFilesDir(), binaryName);
            ProcessBuilder pb = new ProcessBuilder(
                bin.getAbsolutePath(), "-m", modelPath, "-p", prompt
            );
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) result.append(line).append("\n");
            proc.waitFor();
            runOnUiThread(() -> outputText.setText(result.toString()));
        } catch (Exception e) {
            runOnUiThread(() -> outputText.setText("Error: " + e.getMessage()));
        }
    }
}
