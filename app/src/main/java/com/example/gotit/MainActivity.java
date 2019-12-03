package com.example.gotit;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.googlecode.tesseract.android.TessBaseAPI;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {
    private TextView textResult;
    private TessBaseAPI mTess;
    private ImageView imgInput;
    private Button btnRecognize;
    private Button btnCamera;
    private Bitmap bitmap;
    static final int REQUEST_IMAGE_CAPTURE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prepareTessDataCopy();
        mTess = new TessBaseAPI();
        mTess.init(getFilesDir() + "", "vie");
        textResult = (TextView) findViewById(R.id.txtResult);
        imgInput = (ImageView) findViewById(R.id.imgInput);
        btnRecognize = (Button) findViewById(R.id.btnRecognize);

        btnCamera = (Button) findViewById(R.id.btnCamera);
        btnCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto();
            }
        });

        btnRecognize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTess.setImage(bitmap);
                String result = mTess.getUTF8Text();
                textResult.setText(result);
            }
        });
    }


    private void prepareTessDataCopy() {
        try {
            File dir = new File(getFilesDir() + "/tessdata");
            if (!dir.exists()) {
                dir.mkdir();
            }
            File traineddata = new File(getFilesDir() + "/tessdata/vie.traineddata");
            if (!traineddata.exists()) {
                AssetManager asset = getAssets();
                InputStream in = asset.open("tessdata/vie.traineddata");
                OutputStream out = new FileOutputStream(getFilesDir() + "/tessdata/vie.traineddata");
                byte[] buffer = new byte[1024];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                in.close();
                out.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void takePhoto() {
        Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePhotoIntent.resolveActivity((getPackageManager())) != null) {
            startActivityForResult(takePhotoIntent, REQUEST_IMAGE_CAPTURE);
        }
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            bitmap = imageBitmap;
            imgInput.setImageBitmap(imageBitmap);
        }
    }

}




