package com.dragosholban.androidfacedetection;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;
import com.ontbee.legacyforks.cn.pedant.SweetAlert.SweetAlertDialog;

import java.io.File;
import java.io.IOException;

public class FaceDetectionActivity extends AppCompatActivity {

    private static final String TAG = "FaceDetection";
    private String mCurrentPhotoPath;
    private SweetAlertDialog pDialog;

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void initSweetDialog() {
        pDialog = new SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#DA1884"));
        pDialog.getProgressHelper().setSpinSpeed(1);
        pDialog.getProgressHelper().setRimColor(Color.parseColor("#8FD6BD"));
        pDialog.setTitleText("Detecting face.");
        pDialog.setContentText("Please wait...");
        pDialog.setCancelable(true);
        pDialog.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_detection);
        initSweetDialog();
        //initialize the face detector
        final FaceDetector detector = new FaceDetector.Builder(this)
                .setTrackingEnabled(false)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .build();

        Intent intent = getIntent();
        final ImageView imageView = findViewById(R.id.imageView);
        mCurrentPhotoPath = intent.getStringExtra("mCurrentPhotoPath");

        // run image related code after the view was laid out
        // to have all dimensions calculated
        imageView.post(new Runnable() {
            @Override
            public void run() {
                if (mCurrentPhotoPath != null) {
                    Bitmap bitmap = getBitmapFromPathForImageView(mCurrentPhotoPath, imageView);
//                    imageView.setImageBitmap(bitmap);
                    Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                    if (frame != null) {
                        SparseArray<Face> faces = detector.detect(frame);
                        //check if faces are present in picture
                        if (faces.size() != 0) {
                            Log.d(TAG, "Faces detected: " + String.valueOf(faces.size()));
                            pDialog.show();
                            Paint paint = new Paint();
                            paint.setColor(Color.GREEN);
                            paint.setStyle(Paint.Style.STROKE);
                            paint.setStrokeWidth(10);

                            Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                            Canvas canvas = new Canvas(mutableBitmap);

                            for (int i = 0; i < faces.size(); ++i) {
                                Face face = faces.valueAt(i);
                                if (face.getId() != 0) {
                                    Toast.makeText(FaceDetectionActivity.this, "faces detected", Toast.LENGTH_SHORT).show();
                                    pDialog.show();
                                    for (Landmark landmark : face.getLandmarks()) {
                                        if (landmark.getPosition() != null) {
                                            int cx = (int) (landmark.getPosition().x);
                                            int cy = (int) (landmark.getPosition().y);
                                            canvas.drawCircle(cx, cy, 15, paint);
                                        } else {
                                            Toast.makeText(FaceDetectionActivity.this, "No landmarks detected. Ensure you capture a face", Toast.LENGTH_SHORT).show();
                                            finish();
                                        }
                                    }
                                } else {
                                    Toast.makeText(FaceDetectionActivity.this, "No face detected", Toast.LENGTH_SHORT).show();
                                    finish();
                                }

                                Path path = new Path();
                                pDialog.show();
                                if (path != null) {
                                    path.moveTo(face.getPosition().x, face.getPosition().y);
                                    path.lineTo(face.getPosition().x + face.getWidth(), face.getPosition().y);
                                    path.lineTo(face.getPosition().x + face.getWidth(), face.getPosition().y + face.getHeight());
                                    path.lineTo(face.getPosition().x, face.getPosition().y + face.getHeight());
                                    path.close();

                                    Paint redPaint = new Paint();
                                    redPaint.setColor(0XFFFF0000);
                                    redPaint.setStyle(Paint.Style.STROKE);
                                    redPaint.setStrokeWidth(15.0f);
                                    canvas.drawPath(path, redPaint);
                                } else {
                                    Toast.makeText(FaceDetectionActivity.this, "Path is null. ensure a face is present", Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                            }
                            pDialog.dismissWithAnimation();
                            if (faces.size() > 0) {
                                imageView.setImageBitmap(mutableBitmap);
                                galleryAddPic();
                            } else {
                                finish();
                            }
                        } else {
                            Toast.makeText(FaceDetectionActivity.this, "No faces detected. Please ensure a face is present", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(FaceDetectionActivity.this, "Frame is null. please take again with a face.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
            }
        });
    }

    private Bitmap getBitmapFromPathForImageView(String mCurrentPhotoPath, ImageView imageView) {
        // Get the dimensions of the View
        int targetW = imageView.getWidth();
        int targetH = imageView.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = 500;
        int photoH = 1300;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        Bitmap rotatedBitmap = bitmap;

        // rotate bitmap if needed
        try {
            ExifInterface ei = new ExifInterface(mCurrentPhotoPath);
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotatedBitmap = rotateImage(bitmap, 90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotatedBitmap = rotateImage(bitmap, 180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotatedBitmap = rotateImage(bitmap, 270);
                    break;
            }
        } catch (IOException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        return rotatedBitmap;
    }
}
