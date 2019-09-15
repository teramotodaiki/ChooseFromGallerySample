package xyz.hackforplay.choosefromgallerysample;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import com.google.gson.Gson;
import com.microsoft.projectoxford.vision.VisionServiceClient;
import com.microsoft.projectoxford.vision.contract.LanguageCodes;
import com.microsoft.projectoxford.vision.contract.Line;
import com.microsoft.projectoxford.vision.contract.OCR;
import com.microsoft.projectoxford.vision.contract.Region;
import com.microsoft.projectoxford.vision.contract.Word;
import com.microsoft.projectoxford.vision.rest.VisionServiceException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class RecognizeTextRequest extends AsyncTask<String, String, String> {
    // Store error message
    private Exception e = null;

    private Bitmap bitmap = null;
    private VisionServiceClient client;
    private TextView textView;

    public RecognizeTextRequest(Bitmap bmp, VisionServiceClient cli, TextView view) {
        bitmap = bmp;
        client = cli;
        textView = view;
    }

    private String process(Bitmap bmp, VisionServiceClient cli) throws VisionServiceException, IOException {
        Gson gson = new Gson();

        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        OCR ocr;
        ocr = cli.recognizeText(inputStream, LanguageCodes.AutoDetect, true);


        String result = gson.toJson(ocr);
        Log.d("result", result);
        return result;
    }

    @Override
    protected String doInBackground(String... args) {
        try {
            return process(bitmap, client);
        } catch (Exception e) {
            this.e = e;    // Store error
        }

        return null;
    }

    @Override
    protected void onPostExecute(String data) {
        super.onPostExecute(data);
        // Display based on error existence

        if (e != null) {
            e.printStackTrace();
            Log.d("debug", "onPostExecute(Error): " + e.getMessage());
            this.e = null;
        } else {
            Gson gson = new Gson();
            OCR r = gson.fromJson(data, OCR.class);

            String result = "";
            for (Region reg : r.regions) {
                for (Line line : reg.lines) {
                    for (Word word : line.words) {
                        result += word.text + " ";
                    }
                    result += "\n";
                }
                result += "\n\n";
            }

            Log.d("debug", "onPostExecute: " + result);
            textView.setText(result);
        }
    }
}
