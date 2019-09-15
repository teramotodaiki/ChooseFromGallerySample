package xyz.hackforplay.choosefromgallerysample;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.widget.ImageView;
import android.widget.TextView;

import com.microsoft.projectoxford.vision.VisionServiceClient;
import com.microsoft.projectoxford.vision.VisionServiceRestClient;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_CHOOSE_FILE = 1;

    private ImageView imageView;
    private TextView textView;

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

        if (client==null){
            client = new VisionServiceRestClient(key, root);
        }
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
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

