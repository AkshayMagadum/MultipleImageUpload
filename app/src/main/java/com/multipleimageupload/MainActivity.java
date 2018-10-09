package com.multipleimageupload;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.SyncStateContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    LinearLayout ll_camera, ll_gallery;
    RecyclerView rv_images;
    Button btn_submit;

    private static final String TAG = "MainActivity";
    private static String imageStoragePath;
    private ArrayList<Uri> arrImages = new ArrayList();

    public static final int RequestPermissionCode = 1;
    private static final int REQUEST_CODE = 6384;
    private static final int CAMERA_CAPTURE_IMAGE_REQUEST_CODE = 100;
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    // Bitmap sampling size
    public static final int BITMAP_SAMPLE_SIZE = 8;
    // Gallery directory name to store the images or videos
    public static final String GALLERY_DIRECTORY_NAME = "Hello Camera";
    // Image and Video file extensions
    public static final String IMAGE_EXTENSION = "jpg";
    public static final String VIDEO_EXTENSION = "mp4";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EnableRuntimePermission();

        ll_camera = findViewById(R.id.ll_camera);
        ll_gallery = findViewById(R.id.ll_gallery);
        rv_images = findViewById(R.id.rv_images);
        btn_submit = findViewById(R.id.btn_submit);

        ll_camera.setOnClickListener(this);
        ll_gallery.setOnClickListener(this);
        btn_submit.setOnClickListener(this);
    }

    public void EnableRuntimePermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.CAMERA)
                && ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)) {

            Toast.makeText(MainActivity.this, "CAMERA permission allows us to Access CAMERA app", Toast.LENGTH_LONG).show();

            Intent myAppSettings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
            myAppSettings.addCategory(Intent.CATEGORY_DEFAULT);
            myAppSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(myAppSettings);

        } else {

            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE}, RequestPermissionCode);
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ll_camera:
                Log.e(TAG, "onClick: " + "Camera clicked");
//                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//                startActivityForResult(cameraIntent, CAMERA_CODE);

                Intent intent1 = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                File file = CameraUtils.getOutputMediaFile(MEDIA_TYPE_IMAGE);
                if (file != null) {
                    imageStoragePath = file.getAbsolutePath();
                }

                Uri fileUri = CameraUtils.getOutputMediaFileUri(getApplicationContext(), file);

                intent1.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);

                // start the image capture Intent
                startActivityForResult(intent1, CAMERA_CAPTURE_IMAGE_REQUEST_CODE);
                break;
            case R.id.ll_gallery:
                // Use the GET_CONTENT intent from the utility class
                Intent target = FileUtils.createGetContentIntent();
                // Create the chooser Intent
                Intent intent = Intent.createChooser(
                        target, getString(R.string.chooser_title));
                try {
                    startActivityForResult(intent, REQUEST_CODE);
                } catch (ActivityNotFoundException e) {
                    // The reason for the existence of aFileChooser
                }
                break;
            case R.id.btn_submit:
                if (String.valueOf(arrImages.size()).equalsIgnoreCase("0")) {
                    Toast.makeText(this, "Please Select Bill Images!!!", Toast.LENGTH_SHORT).show();
                } else {
                    submitData();
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case CAMERA_CAPTURE_IMAGE_REQUEST_CODE:
                Log.e(TAG, "onActivityResult: " + "coming");
                Bitmap bitmap = CameraUtils.optimizeBitmap(BITMAP_SAMPLE_SIZE, imageStoragePath);

                String imagepath = MediaStore.Images.Media.insertImage(this.getContentResolver(), bitmap, "Title", null);
                arrImages.add(Uri.parse(imagepath));

                Log.e(TAG, "onActivityResult: " + arrImages.size());
                initRecyclerView();
                break;
            case REQUEST_CODE:
                // If the file selection was successful
                if (resultCode == RESULT_OK) {
                    if (data.getClipData() != null) {
                        int count = data.getClipData().getItemCount();
                        int currentItem = 0;
                        while (currentItem < count) {
                            Uri imageUri = data.getClipData().getItemAt(currentItem).getUri();
                            //do something with the image (save it to some directory or whatever you need to do with it here)
                            currentItem = currentItem + 1;
                            Log.e(TAG, "Uri Selected" + imageUri.toString());
                            try {
                                // Get the file path from the URI
                                String path = FileUtils.getPath(this, imageUri);
                                Log.e(TAG, "Multiple File Selected" + path);

                                arrImages.add(imageUri);

//                                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
//                                arrImages.add(bitmap);
                                initRecyclerView();

                            } catch (Exception e) {
                                Log.e(TAG, "File select error", e);
                            }
                        }
                    } else if (data.getData() != null) {
                        //do something with the image (save it to some directory or whatever you need to do with it here)
                        final Uri uri = data.getData();
                        Log.e(TAG, "Uri = " + uri.toString());
                        try {
                            // Get the file path from the URI
                            final String path = FileUtils.getPath(this, uri);
                            Log.e("Single File Selected", path);

                            arrImages.add(uri);
//                            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
//                            arrImages.add(bitmap);
                            initRecyclerView();
                        } catch (Exception e) {
                            Log.e(TAG, "File select error", e);
                        }
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void submitData() {
        if (InternetConnection.checkConnection(MainActivity.this)) {
            Retrofit retrofit = new Retrofit.Builder()
//                    .baseUrl("http://192.168.43.166/~snow/upload-files/")
                    .baseUrl("http://192.168.0.151/UploadImage/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            //showProgress();

            // create list of file parts (photo, video, ...)
            List<MultipartBody.Part> parts = new ArrayList<>();

            // create upload service client
            ApiService service = retrofit.create(ApiService.class);

            if (arrImages != null) {
                // create part for file (photo, video, ...)
                for (int i = 0; i < arrImages.size(); i++) {
                    parts.add(prepareFilePart("image" + i, arrImages.get(i)));
                }
            }

            // create a map of data to pass along
            RequestBody description = createPartFromString("Uploading Images");
            RequestBody size = createPartFromString("" + parts.size());
            RequestBody id = createPartFromString("1008");

            // finally, execute the request
            Call<ResponseBody> call = service.uploadMultiple(description, size, id, parts);

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {

                    ResponseBody responseBody = (ResponseBody) response.body();
                    Log.e(TAG, "onResponse: " + responseBody);
                    String responseBodyString = null;
                    try {
                        responseBodyString = responseBody.string();
                    } catch (IOException e) {
                        Log.e(TAG, "onResponse: " + e);
                        e.printStackTrace();
                    }
                    Log.e(TAG, "Response body" + responseBodyString);

                    // Log.e(TAG, "onResponse: "+ response.body() +","+ call );

//                    hideProgress();
                    if (response.isSuccessful()) {
                        Toast.makeText(MainActivity.this,
                                "Images successfully uploaded!", Toast.LENGTH_SHORT).show();
                        arrImages.clear();
                        initRecyclerView();
                    } else {
                        Toast.makeText(MainActivity.this, "Something went wrong.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
//                    hideProgress();
                    Toast.makeText(MainActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } else {
//            hideProgress();
            Toast.makeText(MainActivity.this, "Internet Connection Not Available", Toast.LENGTH_SHORT).show();
        }
    }

    @NonNull
    private RequestBody createPartFromString(String descriptionString) {
        return RequestBody.create(
                okhttp3.MultipartBody.FORM, descriptionString);
    }

    @NonNull
    private MultipartBody.Part prepareFilePart(String partName, Uri fileUri) {
        // https://github.com/iPaulPro/aFileChooser/blob/master/aFileChooser/src/com/ipaulpro/afilechooser/utils/FileUtils.java
        // use the FileUtils to get the actual file by uri
        File file = FileUtils.getFile(this, fileUri);

        // create RequestBody instance from file
        RequestBody requestFile =
                RequestBody.create(
                        MediaType.parse(Objects.requireNonNull(getContentResolver().getType(fileUri))),
                        file
                );

        // MultipartBody.Part is used to send also the actual file name
        return MultipartBody.Part.createFormData(partName, file.getName(), requestFile);
    }

    private void initRecyclerView() {
        int spacing = 7; // 50px
        rv_images.addItemDecoration(new ItemOffsetDecoration(spacing));

        rv_images.setLayoutManager(new GridLayoutManager(this, 2));
        ImageAdapter adapter = new ImageAdapter(arrImages);
        rv_images.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }
}
