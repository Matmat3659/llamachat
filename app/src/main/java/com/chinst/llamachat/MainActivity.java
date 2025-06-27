package com.chinst.llamachat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.*;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_CODE = 123;
    private static final String binaryName = "llama-cli";

    private TextView outputText;
    private EditText inputText;
    private EditText modelPathInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        outputText = findViewById(R.id.outputText);
        inputText = findViewById(R.id.inputText);
        modelPathInput = findViewById(R.id.modelPathInput); // NEW: input for model path
        Button sendButton = findViewById(R.id.sendButton);

        // Request permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_CODE);
        }

        // Copy llama-cli to internal storage
        extractBinary(binaryName);

        sendButton.setOnClickListener(v -> {
            String prompt = inputText.getText().toString();
            String modelPath = modelPathInput.getText().toString();
            if (!new File(modelPath).exists()) {
                outputText.setText("Model not found at:\n" + modelPath);
                return;
            }
            runLlama(prompt, modelPath);
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
                if (!outFile.setExecutable(true)) {
                    outputText.setText("Warning: binary not executable.");
                }
            }
        } catch (Exception e) {
            outputText.setText("Error copying binary: " + e.getMessage());
        }
    }

    private void runLlama(String prompt, String modelPath) {
        try {
            File bin = new File(getFilesDir(), binaryName);

            // Set LD_LIBRARY_PATH to jniLibs dir
            String libPath = getApplicationInfo().nativeLibraryDir;

            ProcessBuilder pb = new ProcessBuilder(
                    bin.getAbsolutePath(), "-m", modelPath, "-p", prompt
            );
            pb.redirectErrorStream(true);
            pb.environment().put("LD_LIBRARY_PATH", libPath);

            Process proc = pb.start();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(proc.getInputStream())
            );
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }
            proc.waitFor();

            runOnUiThread(() -> outputText.setText(result.toString()));

        } catch (Exception e) {
            runOnUiThread(() -> outputText.setText("Error: " + e.getMessage()));
        }
    }
}
