package com.example.gotit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.common.util.NumberUtils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextDetector;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private TextView textResult;
    private ImageView imgInput;
    private Button btnRecognize;
    private Button btnSolve;
    private Bitmap bitmap;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int IMAGE_PICK_GALLERY_CODE = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setSubtitle("Click image button to insert image");

        textResult = (TextView) findViewById(R.id.txtResult);
        imgInput = (ImageView) findViewById(R.id.imgInput);
        btnRecognize = (Button) findViewById(R.id.btnRecognize);
        btnSolve = (Button) findViewById(R.id.btnSolve);

        btnRecognize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                detectTxt();
            }
        });

        btnSolve.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String strResult=textResult.getText().toString();
                strResult=strResult.replace('O','0');
                strResult=strResult.replace(" ","");
                int degree=0;
                for(int i=0;i<strResult.length();i++) if(strResult.charAt(i)=='x') degree++;
                switch (degree){
                    case 1: textResult.append(giaiPtBac1(strResult)); break;
                    case 2: textResult.append(giaiPtBac2(strResult)); break;
                    case 3: textResult.append(giaiPtBac3(strResult)); break;
                }

            }
        });
    }


    private void takePhoto() {
        Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePhotoIntent.resolveActivity((getPackageManager())) != null) {
            startActivityForResult(takePhotoIntent, REQUEST_IMAGE_CAPTURE);
        }
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                Bundle extras = data.getExtras();
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                bitmap = imageBitmap;
                int w = bitmap.getWidth();
                int h = bitmap.getHeight();
                Matrix mtx = new Matrix();
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, false);
                bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                imgInput.setImageBitmap(imageBitmap);
            }
            if (requestCode == IMAGE_PICK_GALLERY_CODE) {
                //got image from gallery
                CropImage.activity(data.getData()).setGuidelines(CropImageView.Guidelines.ON).start(this); //enable image guidlines
            }
        }
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri(); // get image uri
                //set image to image view
                imgInput.setImageURI(resultUri);
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    //actionbar menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //inflate item
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }


    //handle actionbar item clicks
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.addImage) {
            showImageImportDialog();
        }
        if(id == R.id.setting) {
            Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show();
        }
        return super.onOptionsItemSelected(item);
    }


    private void showImageImportDialog()
    {
        String[] items = {" Camera", " Gallery"};
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        //set title
        dialog.setTitle("Select Image");
        dialog.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(which == 0) {
                    //camera option clicked
                    takePhoto();
                }
                if(which == 1) {
                    //gallery option clicked
                    pickGallery();
                }
            }
        });
        dialog.create().show(); // show dialog
    }


    private void pickGallery() {
        //intent to pick image from gallery
        Intent intent = new Intent(Intent.ACTION_PICK);
        //set intent type to image
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE);
    }


    private void detectTxt() {
        FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromBitmap(bitmap);
        FirebaseVisionTextDetector firebaseVisionTextDetector = FirebaseVision.getInstance().getVisionTextDetector();
        firebaseVisionTextDetector.detectInImage(firebaseVisionImage).addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
            @Override
            public void onSuccess(FirebaseVisionText firebaseVisionText) {
                processTxt(firebaseVisionText);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this,"Error", Toast.LENGTH_LONG).show();
            }
        });
    }


    private void processTxt(FirebaseVisionText firebaseVisionText){
        List<FirebaseVisionText.Block> blockList = firebaseVisionText.getBlocks();
        if (blockList.size() == 0){
            Toast.makeText(MainActivity.this, "No Text", Toast.LENGTH_LONG).show();
            return;
        }
        else{
            for(FirebaseVisionText.Block block: firebaseVisionText.getBlocks()){
                String txt=block.getText();
                textResult.setText(txt);
            }
        }
    }

    private static String giaiPtBac1(String strResult){
        String strSetText="";
        String str_a="", str_b="";
        int i,j;

        for(i=0;i<strResult.length();i++){
            if(strResult.charAt(i)=='x') break;
            str_a+=strResult.charAt(i);
        }

        for(j=i+1;j<strResult.length();j++){
            if(strResult.charAt(j)=='=') break;
            str_b+=strResult.charAt(j);
        }

        if(NumberUtils.isNumeric(str_a)&&NumberUtils.isNumeric(str_b)){
            double a = Double.parseDouble(str_a);
            double b=Double.parseDouble(str_b);
            if(a==0){
                if(b==0) strSetText = "\nPt co vo so nghiem";
                else strSetText = "\nPt vo nghiem";
            }
            else strSetText = "\nPt co nghiem x="+(-b)/a;
        }
        else strSetText="\nCan't solve";

        return strSetText;
    }

    private static String giaiPtBac2(String strResult){
        String strSetText="";

        String str_a="", str_b="", str_c="";
        int i,j,k;
        for(i=0;i<strResult.length();i++){
            if(strResult.charAt(i)=='x') break;
            str_a+=strResult.charAt(i);
        }

        for(j=i+2;j<strResult.length();j++){
            if(strResult.charAt(j)=='x') break;
            str_b+=strResult.charAt(j);
        }

        for(k=j+1;k<strResult.length();k++){
            if(strResult.charAt(k)=='=') break;
            str_c+=strResult.charAt(k);
        }

        if(NumberUtils.isNumeric(str_a)&&NumberUtils.isNumeric(str_b)&&NumberUtils.isNumeric(str_c)){
            double a = Double.parseDouble(str_a);
            double b = Double.parseDouble(str_b);
            double c = Double.parseDouble(str_c);
            double delta = b*b-4*a*c;
            if(delta<0) strSetText = "\nPT vo nghiem";
            else if(delta == 0) strSetText="\nPT co nghiem kep x = "+(-b/(2*a));
            else {
                double x1=(-b+Math.sqrt(delta))/(2*a);
                double x2=(-b-Math.sqrt(delta))/(2*a);
                strSetText = "\nPT co hai nghiem phan biet x1="+x1+" ; x2="+x2;
            }
        }
        else strSetText="\nCan't solve";

        return strSetText;
    }

    private static String giaiPtBac3(String strResult){
        String strSetText="";
        return strSetText;
    }
}





