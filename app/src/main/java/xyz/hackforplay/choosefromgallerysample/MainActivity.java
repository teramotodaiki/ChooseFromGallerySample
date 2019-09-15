package xyz.hackforplay.choosefromgallerysample;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.microsoft.azure.cognitiveservices.vision.computervision.*;
import com.microsoft.azure.cognitiveservices.vision.computervision.models.OcrLanguages;
import com.microsoft.azure.cognitiveservices.vision.computervision.models.OcrLine;
import com.microsoft.azure.cognitiveservices.vision.computervision.models.OcrRegion;
import com.microsoft.azure.cognitiveservices.vision.computervision.models.OcrResult;
import com.microsoft.azure.cognitiveservices.vision.computervision.models.OcrWord;
import com.microsoft.projectoxford.vision.VisionServiceClient;
import com.microsoft.projectoxford.vision.VisionServiceRestClient;
import com.microsoft.projectoxford.vision.contract.LanguageCodes;
import com.microsoft.projectoxford.vision.contract.Line;
import com.microsoft.projectoxford.vision.contract.OCR;
import com.microsoft.projectoxford.vision.contract.Region;
import com.microsoft.projectoxford.vision.contract.Word;
import com.microsoft.projectoxford.vision.rest.VisionServiceException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import rx.Observable;
import rx.Observer;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_CHOOSE_FILE = 1;

    private ImageView imageView;
    private TextView textView;
    private ComputerVisionClient compVisClient;

    // The image selected to detect.
    private Bitmap mBitmap;
    private VisionServiceClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

                chooseImageDialog();
            }
        });

        imageView = findViewById(R.id.imageView);
        textView = findViewById(R.id.textView);

        String key = getString(R.string.subscription_key);
        String root = getString(R.string.subscription_apiroot);

        compVisClient = ComputerVisionManager.authenticate(key).withEndpoint(root);

        if (client==null){
            client = new VisionServiceRestClient(getString(R.string.subscription_key), getString(R.string.subscription_apiroot));
        }

//        Uri uri = Uri.parse("content://com.android.providers.media.documents/document/image%3A26");
//        analyzeLocalImage(uri);
    }

    private void analyzeLocalImage(Uri uri) {

        if (Build.VERSION.SDK_INT < 26) {
            return;
        }

        byte[] imageByteArray;
        try {
            InputStream stream = getContentResolver().openInputStream(uri);
            imageByteArray = getBytes(stream);
        } catch (IOException e) {
            Log.d("debug", "Failed to open uri: " + uri.toString() + " " + e.getMessage());
            return;
        }

        callApiAsyncTask(imageByteArray);


    }

    protected void callApiAsyncTask(byte[] imageByteArray) {
        try {
            // メインスレッドで Network 処理を走らせると android.os.NetworkOnMainThreadException がスローされる
            AsyncTask<byte[], String, String> recognizeTextTask = new AsyncTask<byte[], String, String>() {
                @Override
                protected String doInBackground(byte[]... inputs) {

                    Log.d("debug", "doInBackground: start");

                    OcrResult result;

                    try {
                        result = compVisClient.computerVision().recognizePrintedTextInStream()
                                .withDetectOrientation(true)
                                .withImage(inputs[0])
                                .withLanguage(OcrLanguages.JA)
                                .execute();

                    } catch (Exception e) {
                        Log.d("debug", "doInBackground: catch");
                        e.printStackTrace();
                        return "";
                    }

                    Log.d("debug", result.language());
                    Log.d("debug", result.textAngle() + "");
                    Log.d("debug", result.orientation());


                    for (OcrRegion region : result.regions()) {
                        String text = "";
                        for (OcrLine line : region.lines()) {
                            for (OcrWord word : line.words()) {
                                Log.d("debug", word.text());
                                text += word.text();
                            }
                            text += "\n";
                        }
                        Log.d("debug", text);
                    }
                    return "";
                }
            };
            Log.d("debug", "start OCR");
            recognizeTextTask.execute(imageByteArray);
            Log.d("debug", "started OCR");

        } catch (Exception e) {
            Log.d("debug", "analyzeLocalImage: " + e.getMessage());
            e.printStackTrace();
            return;
        }
    }

    protected  void callApiSameThread(byte[] imageByteArray) {
        Observable<OcrResult> result$ = compVisClient.computerVision().recognizePrintedTextInStream()
                .withDetectOrientation(true)
                .withImage(imageByteArray)
                .withLanguage(OcrLanguages.JA)
                .executeAsync();

        result$.subscribe(new Observer<OcrResult>() {
            @Override
            public void onCompleted() {
                Log.d("debug", "onCompleted: ");
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
                Log.d("debug", "onError: " + e.toString());
            }

            @Override
            public void onNext(OcrResult ocrResult) {
                Log.d("debug", "onNext: " + ocrResult.toString());
            }
        });
    }

    protected void chooseImageDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Please choose image")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // ボタンをクリックしたときの動作
                        chooseFile();
                    }
                });
        builder.show();
    }


    private void chooseFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
//        intent.putExtra()
        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select a File to Upload"),
                    REQUEST_CHOOSE_FILE
            );

        } catch (Exception e) {

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.d("debug", resultCode + " resultCode");
        if (requestCode == REQUEST_CHOOSE_FILE) {
            if (data == null) {
                Log.d("debug","data is null");
                return;
            }
            Uri uri = data.getData();
            if(uri == null){
                Log.d("debug", "uri is null");
                return;
            }

            Log.d("debug", "uri:" + uri.toString());
            imageView.setImageURI(uri);

            try {
                Bitmap bitmap = loadBitmapFromUri(uri);

                if (bitmap != null) {
                    // Show the image on screen.
                    ImageView imageView = findViewById(R.id.imageView);
                    imageView.setImageBitmap(bitmap);

                    // Add detection log.
                    Log.d("AnalyzeActivity", "Image: " + uri + " resized to " + bitmap.getWidth()
                            + "x" + bitmap.getHeight());

                    doRecognize(bitmap);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Log.d("error", "onActivityResult: FileNotFound");
            }
        }
    }

    protected Bitmap loadBitmapFromUri(Uri uri) throws FileNotFoundException {
        InputStream imageInputStream = getContentResolver().openInputStream(uri);
        BitmapFactory.Options options = new BitmapFactory.Options();
        return BitmapFactory.decodeStream(imageInputStream, new Rect(), options);
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_CHOOSE_FILE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * https://stackoverflow.com/a/32647693
     * get bytes from input stream.
     *
     * @param inputStream inputStream.
     * @return byte array read from the inputStream.
     * @throws IOException
     */
    public static byte[] getBytes(InputStream inputStream) throws IOException {

        byte[] bytesResult = null;
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        try {
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            bytesResult = byteBuffer.toByteArray();
        } finally {
            // close the stream
            try{ byteBuffer.close(); } catch (IOException ignored){ /* do nothing */ }
        }
        return bytesResult;
    }

    public void doRecognize(Bitmap bitmap) {
        Log.d("debug", "doRecognize: Analyzing...");

        try {
            new RecognizeTextRequest(bitmap, client, textView).execute();
        } catch (Exception e)
        {
            Log.d("debug", "doRecognize: " + "Error encountered. Exception is: " + e.toString());
        }
    }
}

