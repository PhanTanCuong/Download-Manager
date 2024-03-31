package hcmute.edu.vn.downloadmanager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import android.Manifest; // correct
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;

public class MainActivity extends AppCompatActivity {



    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private static final int PERMISSION_REQUEST_CODE = 101;
    DownloadAdapter downloadAdapter;
    List<DownloadModel> downloadModels=new ArrayList<>();
    Realm realm;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Register the Broadcast Receiver
        registerReceiver(onComplete,new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        Button add_download_list=findViewById(R.id.add_download_list);
        RecyclerView data_list=findViewById(R.id.data_list);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission, request it
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE);
        } else {
            // We already have permission, proceed with your operations
            // For example, start your download operation


            add_download_list.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    showInputDialog();
                }
            });

            //Initialize DownloadAdapter Object ,MainActivity.
            downloadAdapter = new DownloadAdapter(MainActivity.this, downloadModels);
            data_list.setLayoutManager(new LinearLayoutManager(MainActivity.this));
            data_list.setAdapter(downloadAdapter);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showInputDialog();
            } else {
                Toast.makeText(this,"Permission denied",Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(onComplete);
    }

    //showInputDialog
    private void    showInputDialog(){
        AlertDialog.Builder al=new AlertDialog.Builder(MainActivity.this);
        View view=getLayoutInflater().inflate(R.layout.input_dilaog,null);
        al.setView(view);


        final EditText editText=view.findViewById(R.id.input);
        Button paste=view.findViewById(R.id.paste);

        paste.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboardManager= (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                try{
                    CharSequence charSequence=clipboardManager.getPrimaryClip().getItemAt(0).getText();
                    editText.setText(charSequence);

                }
                catch (Exception e){
                    e.printStackTrace();
                }
            }
        });

        al.setPositiveButton("Download", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                downloadFile(editText.getText().toString());

            }
        });

        al.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        al.show();

    }



    private void downloadFile(String url) {
        String filename= URLUtil.guessFileName(url,null,null);
        String downloadPath= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();

        File file=new File(downloadPath,filename);

        DownloadManager.Request request=null;
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N){
            request=new DownloadManager.Request(Uri.parse(url))
                    .setTitle(filename)
                    .setDescription("Downloading")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                    .setDestinationUri(Uri.fromFile(file))
                    .setRequiresCharging(false)
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true);
        }
        else{
            request=new DownloadManager.Request(Uri.parse(url))
                    .setTitle(filename)
                    .setDescription("Downloading")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                    .setDestinationUri(Uri.fromFile(file))
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true);
        }

        DownloadManager downloadManager= (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        long downloadId=downloadManager.enqueue(request);

//        Number currentnum=realm.where(DownloadModel.class).max("id");
//        int nextId;
//
//        if(currentnum==null){
//            nextId=1;
//        }
//        else{
//            nextId=currentnum.intValue()+1;
//        }
        final DownloadModel downloadModel=new DownloadModel();
        downloadModel.setId(11);
        downloadModel.setStatus("Downloading");
        downloadModel.setTitle(filename);
        downloadModel.setFile_size("0");
        downloadModel.setProgress("0");
        downloadModel.setIs_paused(false);
        downloadModel.setDownloadId(downloadId);
        downloadModel.setFile_path("");

        downloadModels.add(downloadModel);
        downloadAdapter.notifyItemInserted(downloadModels.size()-1);




        DownloadStatusTask downloadStatusTask=new DownloadStatusTask(downloadModel);
        runTask(downloadStatusTask,""+downloadId);

    }

    private void runTask(DownloadStatusTask downloadStatusTask,String id) {
        try{
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.HONEYCOMB){
                downloadStatusTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,new String[]{id});
            }
            else{
                downloadStatusTask.execute(new String[]{id});
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }


    //Creating DownloadTask which Extend AsyncTask
    //This Task Will Update The Progress of Downloading File
    public class DownloadStatusTask extends AsyncTask<String,String,String> {

        DownloadModel downloadModel;
        //Constructor
        public DownloadStatusTask(DownloadModel downloadModel) {
            this.downloadModel = downloadModel;
        }
        @Override
        protected String doInBackground(String... strings) {
            downloadFileProcess(strings[0]);
            return null;
        }

        private void downloadFileProcess(String downloadId) {
            DownloadManager downloadManager= (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            boolean downloading=true;
            while (downloading){
                DownloadManager.Query query=new DownloadManager.Query();
                query.setFilterById(Long.parseLong(downloadId));
                Cursor cursor=downloadManager.query(query);
                cursor.moveToFirst();

                if (cursor != null && cursor.moveToFirst()) {
                    int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    int bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                    int totalSizeIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);

                    if (statusIndex != -1 && bytesDownloadedIndex != -1 && totalSizeIndex != -1) {
                        int status = cursor.getInt(statusIndex);
                        int bytes_downloaded = cursor.getInt(bytesDownloadedIndex);
                        int total_size = cursor.getInt(totalSizeIndex);

                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            downloading = false;
                        }

                        int progress = (int) ((bytes_downloaded * 100L) / total_size);
                        String statusMessage = getStatusMessage(cursor);
                        publishProgress(new String[]{String.valueOf(progress), String.valueOf(bytes_downloaded), statusMessage});
                    } else {
                        // Handle missing columns
                        Log.e("DownloadManager", "One or more columns not found in cursor");
                    }
                } else {
                    // Handle empty cursor or no data found
                    Log.e("DownloadManager", "Empty cursor or no data found");
                    downloading = false;
                }
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);



                    this.downloadModel.setFile_size(values[1]);
                    this.downloadModel.setProgress(values[0]);
                    if (!downloadModel.getStatus().equalsIgnoreCase("PAUSE") && !downloadModel.getStatus().equalsIgnoreCase("RESUME")) {
                        downloadModel.setStatus(values[2]);
                    }
                    downloadAdapter.changeItem(downloadModel.getDownloadId());



        }
    }

    private String bytesIntoHumanReadable(long bytes) {
        long kilobyte = 1024;
        long megabyte = kilobyte * 1024;
        long gigabyte = megabyte * 1024;
        long terabyte = gigabyte * 1024;

        if ((bytes >= 0) && (bytes < kilobyte)) {
            return bytes + " B";

        } else if ((bytes >= kilobyte) && (bytes < megabyte)) {
            return (bytes / kilobyte) + " KB";

        } else if ((bytes >= megabyte) && (bytes < gigabyte)) {
            return (bytes / megabyte) + " MB";

        } else if ((bytes >= gigabyte) && (bytes < terabyte)) {
            return (bytes / gigabyte) + " GB";

        } else if (bytes >= terabyte) {
            return (bytes / terabyte) + " TB";

        } else {
            return bytes + " Bytes";
        }
    }

    private String getStatusMessage(Cursor cursor) {
        String msg = "-";
        int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
        if (statusIndex != -1) {
            int status = cursor.getInt(statusIndex);
            switch (status) {
                case DownloadManager.STATUS_FAILED:
                    msg = "Failed";
                    break;
                case DownloadManager.STATUS_PAUSED:
                    msg = "Paused";
                    break;
                case DownloadManager.STATUS_RUNNING:
                    msg = "Running";
                    break;
                case DownloadManager.STATUS_SUCCESSFUL:
                    msg = "Completed";
                    break;
                case DownloadManager.STATUS_PENDING:
                    msg = "Pending";
                    break;
                default:
                    msg = "Unknown";
                    break;
            }
        } else {
            // Handle missing column
            Log.e("getStatusMessage", "COLUMN_STATUS not found in cursor");
        }
        return msg;
    }

//    Creating Broadcast Receiver Object
//    This Will Work on Download Completed
BroadcastReceiver onComplete=new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
        long id=intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID,-1);
        boolean comp=downloadAdapter.ChangeItemWithStatus("Completed",id);

        if (comp) {
            DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            Cursor cursor = downloadManager.query(new DownloadManager.Query().setFilterById(id));
            if (cursor != null && cursor.moveToFirst()) {
                int localUriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                if (localUriIndex != -1) {
                    String downloadedPath = cursor.getString(localUriIndex);
                    downloadAdapter.setChangeItemFilePath(downloadedPath, id);
                } else {
                    Log.e("onComplete", "COLUMN_LOCAL_URI not found in cursor");
                }
                cursor.close();
            } else {
                Log.e("onComplete", "Cursor is null or empty");
            }
        }
    }
};
}