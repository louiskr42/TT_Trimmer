package com.example.tttrimmer;

import static android.media.MediaRecorder.VideoSource.CAMERA;
import static androidx.constraintlayout.widget.Constraints.TAG;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tttrimmer.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import com.gowtham.library.utils.LogMessage;
import com.gowtham.library.utils.TrimType;
import com.gowtham.library.utils.TrimVideo;
import droidninja.filepicker.FilePickerBuilder;
import droidninja.filepicker.FilePickerConst;

public class MainActivity extends AppCompatActivity {

    private ArrayList<Uri> uris = new ArrayList<>();
    //private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    public RecyclerView recyclerView;
    public VideoView videoView;
    public MediaController mediaController;

    // Register the permissions callback, which handles the user's response to the
    // system permissions dialog. Save the return value, an instance of
    // ActivityResultLauncher, as an instance variable.
    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission is granted. Continue the action or workflow in your
                    // app.
                } else {
                }
            });

    ActivityResultLauncher<Intent> startForResult = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK &&
                        result.getData() != null) {
                    Uri uri = Uri.parse(TrimVideo.getTrimmedVideoPath(result.getData()));
                    Log.d(TAG, "Trimmed path:: " + uri);
                    videoView.setVideoURI(uri);
                    saveFile(uri);


                } else
                    LogMessage.v("videoTrimResultLauncher data is null");
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //setSupportActionBar(binding.toolbar);

        //NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        //appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        //NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectVideos(view);
            }
        });

        recyclerView = findViewById(R.id.recyclerView);
        mediaController = new MediaController(this);
        videoView = findViewById(R.id.videoView);
        videoView.setMediaController(mediaController);
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

    //@Override
    //public boolean onSupportNavigateUp() {
    //    NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
    //    return NavigationUI.navigateUp(navController, appBarConfiguration)
    //            || super.onSupportNavigateUp();
    //}

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //Check condition
        if (resultCode == RESULT_OK && data != null) {
            //When activity contains data
            //Check condition
            if (requestCode == FilePickerConst.REQUEST_CODE_PHOTO) {
                //When request for photo
                //Initialize array list
                uris = data.getParcelableArrayListExtra(FilePickerConst.KEY_SELECTED_MEDIA);
                //Set layout manager
                recyclerView.setLayoutManager(new LinearLayoutManager(this));
                //Set adapter
                recyclerView.setAdapter(new MainAdapter(uris));
                //videoView.setVideoURI(uris.get(0));

                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(this, uris.get(0));

                Long oDurationString = toLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));

                Long nDurationLong = oDurationString - 4;

                TrimVideo.activity(String.valueOf(uris.get(0)))
                //      .setCompressOption(new CompressOption()) //empty constructor for default compress option
                        .setHideSeekBar(false)
                        .setAccurateCut(true)
                        .setTrimType(TrimType.FIXED_DURATION)
                        .setFixedDuration(nDurationLong)
                        .start(this,startForResult);

            }
        }
    }

    private Long toLong(String str) {
        return Long.parseLong(str.substring(0, 2));
    }

    private void makeToast(int duration) {
        Toast.makeText(MainActivity.this, duration, Toast.LENGTH_LONG);
    }

    private void selectVideos(View view) {
        if (isReadPermissionGranted() && isWritePermissionGranted()) {
            Snackbar.make(view, "It worked!", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            openVideoPicker();
        } else {
            Snackbar.make(view, "It didn't work!", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }

    private void openVideoPicker() {
        //Open picker
        FilePickerBuilder.getInstance()
                .setActivityTitle("Select Videos")
                .setSpan(FilePickerConst.SPAN_TYPE.FOLDER_SPAN, 3)
                .setSpan(FilePickerConst.SPAN_TYPE.DETAIL_SPAN, 4)
                .setMaxCount(1)
                .setSelectedFiles(uris)
                .setActivityTheme(R.style.Theme_TTTrimmer_VideoPicker)
                .enableVideoPicker(true)
                .enableImagePicker(false)
                .pickPhoto(this);
    }

    private boolean isReadPermissionGranted(){
        if (ContextCompat.checkSelfPermission(
                MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isWritePermissionGranted(){
        if (ContextCompat.checkSelfPermission(
                MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    private void saveFile(Uri sourceUri)
    {
        File file = new File(Environment.getExternalStorageDirectory().getPath(), "DCIM/TikTokTrimmer");

        Log.d(TAG, "getExternalFilesDirs(): " + getExternalFilesDirs(Environment.DIRECTORY_MOVIES));

        if (!file.exists()){
            Log.d(TAG, "Folder created: " + String.valueOf(file.mkdir()));
        }

        if (new File(Environment.getExternalStorageDirectory().getPath()).exists()) {
            Log.d(TAG, "Parent folder exists!");
        }


        String sourcePath= sourceUri.getPath();
        String[] folders= sourcePath.split("/");
        String destinationFilename = folders[folders.length - 1].replace("..", ".");
        String destinationPath = Environment.getExternalStorageDirectory().getPath()+ File.separatorChar + "DCIM" + File.separatorChar + "TikTokTrimmer" + File.separatorChar + destinationFilename;

        Log.d(TAG, "New video path: " + destinationPath);

        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;

        try {
            bis = new BufferedInputStream(new FileInputStream(sourcePath));
            bos = new BufferedOutputStream(new FileOutputStream(destinationPath, false));
            byte[] buf = new byte[1024];
            bis.read(buf);
            do {
                bos.write(buf);
            } while(bis.read(buf) != -1);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bis != null) bis.close();
                if (bos != null) bos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}