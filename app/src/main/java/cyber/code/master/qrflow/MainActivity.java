package cyber.code.master.qrflow;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_SAVE_FILE = 1;
    private Bitmap qrBitmapForSaving;
    private Spinner typeSpinner;
    private LinearLayout dynamicFields;
    private ImageView qrImage;
    private Button generateBtn, downloadBtn;

    private final String QR_API_BASE_URL = "https://public-api.qr-code-generator.com/v1/create/free";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        typeSpinner = findViewById(R.id.type_spinner);
        dynamicFields = findViewById(R.id.dynamic_fields);
        qrImage = findViewById(R.id.qr_image);
        generateBtn = findViewById(R.id.generate_btn);
        downloadBtn = findViewById(R.id.download_btn);

        setupSpinner();

        generateBtn.setOnClickListener(v -> generateQRCode());
        downloadBtn.setOnClickListener(v -> {
            String selectedType = (String) typeSpinner.getSelectedItem();
            String qrData = getQRData(selectedType); // Fetch QR data
            if (qrData != null && !qrData.isEmpty()) {
                downloadQRCode(qrData); // Pass qrData to downloadQRCode
            } else {
                Toast.makeText(this, "Invalid input. Please fill out the required fields.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addTextAreaField(String label, String tag) {
        TextView textView = new TextView(this);
        textView.setText(label);

        EditText editText = new EditText(this);
        editText.setTag(tag);
        editText.setMinLines(3);

        dynamicFields.addView(textView);
        dynamicFields.addView(editText);
    }

    private void addSpinnerField(String label, String[] options) {
        TextView textView = new TextView(this);
        textView.setText(label);

        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        dynamicFields.addView(textView);
        dynamicFields.addView(spinner);
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.qr_code_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(adapter);

        typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedType = (String) parent.getItemAtPosition(position);
                renderDynamicFields(selectedType);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void renderDynamicFields(String type) {
        dynamicFields.removeAllViews(); // Clear previous fields

        switch (type) {
            case "URL":
                addTextField("Enter URL:", "url");
                break;
            case "vCard":
                addTextField("First Name:", "first-name");
                addTextField("Last Name:", "last-name");
                addTextField("Mobile:", "mobile");
                addTextField("Email:", "email");
                addTextField("Company:", "company");
                addTextField("Website:", "website");
                break;
            case "Text":
                addTextField("Enter Text:", "text");
                break;
            case "E-mail":
                addTextField("Email:", "email");
                addTextField("Subject:", "subject");
                addTextAreaField("Message:", "message");
                break;
            case "SMS":
                addTextField("Phone Number:", "number");
                addTextAreaField("Message:", "sms-message");
                break;
            case "WiFi":
                addTextField("Network Name (SSID):", "ssid");
                addTextField("Password:", "password");
                addSpinnerField("Encryption:", new String[]{"None", "WPA", "WEP"});
                break;
            case "Bitcoin":
                addSpinnerField("Cryptocurrency:", new String[]{"Bitcoin", "Bitcoin Cash", "Ether", "Litecoin", "Dash"});
                addTextField("Receiver Address:", "bitcoin-address");
                addTextField("Amount:", "bitcoin-amount");
                addTextAreaField("Message (Optional):", "bitcoin-message");
                break;
            case "Twitter":
                addTextField("Twitter URL:", "twitter-url");
                break;
            case "Facebook":
                addTextField("Facebook URL:", "facebook-url");
                break;
            default:
                Toast.makeText(this, "Please select a valid QR Code type.", Toast.LENGTH_SHORT).show();
        }
    }

    private void addTextField(String label, String tag) {
        TextView textView = new TextView(this);
        textView.setText(label);

        EditText editText = new EditText(this);
        editText.setTag(tag);

        dynamicFields.addView(textView);
        dynamicFields.addView(editText);
    }

    private void generateQRCode() {
        String selectedType = (String) typeSpinner.getSelectedItem();
        String qrData = getQRData(selectedType);

        if (qrData == null || qrData.isEmpty()) {
            Toast.makeText(this, "Invalid input. Please fill out the required fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                String qrApiUrl = QR_API_BASE_URL + "?image_format=SVG&qr_code_text=" + qrData;
                Log.d("QR_API_URL", "QR API URL: " + qrApiUrl);

                // Fetch the SVG data
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(qrApiUrl).build();
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        InputStream svgStream = response.body().byteStream();
                        SVG svg = SVG.getFromInputStream(svgStream);
                        Log.d("response:", response.toString());

                        // Render SVG
                        runOnUiThread(() -> renderSVG(svg));
                    } else {
                        Log.e("QR_CODE_ERROR", "API request failed: " + response.message());
                        showToast("Failed to generate QR Code.");
                    }
                }
            } catch (IOException | SVGParseException e) {
                Log.e("QR_CODE_ERROR", "Error generating QR Code", e);
                showToast("Error generating QR Code.");
            }
        }).start();
    }

    private void renderSVG(SVG svg) {
        if (svg != null) {
            qrImage.post(() -> {
                int width = qrImage.getWidth();
                int height = qrImage.getHeight();

                if (width > 0 && height > 0) {
                    Log.d("RenderSVG", "Width: " + width + ", Height: " + height);
                    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    svg.renderToCanvas(canvas);

                    qrImage.setImageBitmap(bitmap);
                    showToast("QR Code generated successfully!");
                } else {
                    showToast("Unable to render QR Code. View dimensions are invalid.");
                }
            });
        } else {
            showToast("Failed to render QR Code.");
        }
    }


    private String getQRData(String type) {
        if ("URL".equals(type)) {
            EditText urlField = dynamicFields.findViewWithTag("url");
            if (urlField != null) {
                return urlField.getText().toString();
            }
        } else if ("vCard".equals(type)) {
            String firstName = getTextFromField("first-name");
            String lastName = getTextFromField("last-name");
            String mobile = getTextFromField("mobile");
            String email = getTextFromField("email");
            String company = getTextFromField("company");
            String website = getTextFromField("website");
            return String.format("BEGIN:VCARD\nFN:%s %s\nTEL:%s\nEMAIL:%s\nORG:%s\nURL:%s\nEND:VCARD",
                    firstName, lastName, mobile, email, company, website);
        } else if ("E-mail".equals(type)) {
            String email = getTextFromField("email");
            String subject = getTextFromField("subject");
            String message = getTextFromField("message");
            return String.format("mailto:%s?subject=%s&body=%s", email, subject, message);
        } else if ("SMS".equals(type)) {
            String number = getTextFromField("number");
            String smsMessage = getTextFromField("sms-message");
            return String.format("sms:%s?body=%s", number, smsMessage);
        } else if ("WiFi".equals(type)) {
            String ssid = getTextFromField("ssid");
            String password = getTextFromField("password");
            String encryption = getSelectedSpinnerValue("Encryption:");
            return String.format("WIFI:S:%s;T:%s;P:%s;;", ssid, encryption, password);
        } else if ("Bitcoin".equals(type)) {
            String crypto = getSelectedSpinnerValue("Cryptocurrency:");
            String address = getTextFromField("bitcoin-address");
            String amount = getTextFromField("bitcoin-amount");
            String message = getTextFromField("bitcoin-message");
            return String.format("%s:%s?amount=%s&message=%s", crypto.toLowerCase(), address, amount, message);
        } else if ("Phone Call".equals(type)) {
            return String.format("tel:%s", getTextFromField("call-number"));
        } else if ("Geo Location".equals(type)) {
            String latitude = getTextFromField("latitude");
            String longitude = getTextFromField("longitude");
            String description = getTextFromField("geo-description");
            return String.format("geo:%s,%s?q=%s", latitude, longitude, description);
        } else if ("Event".equals(type)) {
            String name = getTextFromField("event-name");
            String location = getTextFromField("event-location");
            String startDate = getTextFromField("event-start-date");
            String endDate = getTextFromField("event-end-date");
            String description = getTextFromField("event-description");
            return String.format("BEGIN:VEVENT\nSUMMARY:%s\nLOCATION:%s\nDTSTART:%s\nDTEND:%s\nDESCRIPTION:%s\nEND:VEVENT",
                    name, location, startDate, endDate, description);
        }
        return null;
    }

    private String getTextFromField(String tag) {
        EditText field = dynamicFields.findViewWithTag(tag);
        return field != null ? field.getText().toString() : "";
    }

    private String getSelectedSpinnerValue(String label) {
        Spinner spinner = dynamicFields.findViewWithTag(label);
        return spinner != null ? spinner.getSelectedItem().toString() : "";
    }

    private void downloadQRCode(String qrData) {
        try {
            // Step 1: Generate QR Code Bitmap
            com.google.zxing.Writer writer = new com.google.zxing.qrcode.QRCodeWriter();
            com.google.zxing.common.BitMatrix bitMatrix = writer.encode(qrData, com.google.zxing.BarcodeFormat.QR_CODE, 512, 512);

            Bitmap qrBitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888);
            for (int x = 0; x < 512; x++) {
                for (int y = 0; y < 512; y++) {
                    qrBitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            // Step 2: Open File Picker for User to Choose Location and File Name
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/png");
            intent.putExtra(Intent.EXTRA_TITLE, "QRCode_" + System.currentTimeMillis() + ".png");
            qrBitmapForSaving = qrBitmap; // Store the bitmap globally for use in onActivityResult
            startActivityForResult(intent, REQUEST_CODE_SAVE_FILE);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to generate QR Code: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SAVE_FILE && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                try {
                    OutputStream outputStream = getContentResolver().openOutputStream(uri);
                    qrBitmapForSaving.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                    if (outputStream != null) {
                        outputStream.flush();
                        outputStream.close();
                    }
                    Toast.makeText(this, "QR Code saved successfully!", Toast.LENGTH_LONG).show();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Failed to save QR Code: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }
    }


    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }
}
