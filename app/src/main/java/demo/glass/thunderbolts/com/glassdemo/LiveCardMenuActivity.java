package demo.glass.thunderbolts.com.glassdemo;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.FileObserver;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.glass.content.Intents;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import javax.net.ssl.HttpsURLConnection;

/**
 * A transparent {@link Activity} displaying a "Stop" options menu to remove the {@link LiveCard}.
 */
public class LiveCardMenuActivity extends Activity {

    private static final int TAKE_PICTURE_REQUEST = 1;
//    LocationManager locationManager;
//    Location userLocation;

//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        authenticateUser();
//    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        // Open the options menu right away.
        openOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.hello, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_capture:
                // Capture the image here..
                takePicture();
                //identifyLocation();
                return true;
            case R.id.action_authenticate:
                authenticateUser();
                return true;
            case R.id.action_stop:
                // Stop the service which will unpublish the live card.
                stopService(new Intent(this, HelloService.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        super.onOptionsMenuClosed(menu);
        // Nothing else to do, finish the Activity.
        //finish();
    }

    private void authenticateUser(){
        Log.i("Test", "Authentication");
        Uri webpage = Uri.parse("https://accounts.google.com/o/oauth2/auth?response_type=code&client_id=252473431161-4l5l4qn97p86k80o8cghhoar9om5b8ut.apps.googleusercontent.com&redirect_uri=http://ec2-52-64-0-154.ap-southeast-2.compute.amazonaws.com:8080/oauth2callback&scope=https://www.googleapis.com/auth/glass.timeline");
        Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
        if(intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }

    }

    private void takePicture() {
        Log.i("Test", "Take Picture function");
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, TAKE_PICTURE_REQUEST);
    }

    private void makeToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private String getEmailId() {
        Account[] glassAccount = AccountManager.get(this).getAccountsByType("com.google");
        if(glassAccount.length > 0) {
            Log.i("Glass Email: ", glassAccount[0].name);
            return glassAccount[0].name;
        }
        return null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i("Test", "onActivityResult");
        if (requestCode == TAKE_PICTURE_REQUEST && resultCode == RESULT_OK) {
            String thumbnailPath = data.getStringExtra(Intents.EXTRA_THUMBNAIL_FILE_PATH);
            String picturePath = data.getStringExtra(Intents.EXTRA_PICTURE_FILE_PATH);

            processPictureWhenReady(picturePath);
        }

        super.onActivityResult(requestCode, resultCode, data);
        finish();
    }

    private void processPictureWhenReady(final String picturePath) {
        final File pictureFile = new File(picturePath);
        Log.i("Test", "Picture Path" + picturePath);

        if (pictureFile.exists()) {
            // The picture is ready; process it.
            makeToast("Picture Taken. Processing..");


            //Location

            // This example requests fine accuracy and requires altitude, but
            // these criteria could be whatever you want.
//            Criteria criteria = new Criteria();
//            criteria.setAccuracy(Criteria.ACCURACY_FINE);
//            criteria.setAltitudeRequired(true);
//
//            List<String> providers = locationManager.getProviders(
//                    criteria, true /* enabledOnly */);
//
//            for (String provider : providers) {
//                locationManager.requestLocationUpdates(provider, 1000, 10, locationListener);
//            }


            //Scale down the size of the image..
            File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
            Bitmap b = BitmapFactory.decodeFile(picturePath);
            Bitmap out = Bitmap.createScaledBitmap(b, 1280, 960, false);

            File file = new File(dir, "resized.png");
            FileOutputStream fOut;
            try {
                fOut = new FileOutputStream(file);
                out.compress(Bitmap.CompressFormat.PNG, 100, fOut);
                fOut.flush();
                fOut.close();
                b.recycle();
                out.recycle();

                SendToServer task = new SendToServer();
                task.execute(new String[] { file.getAbsolutePath(), file.toString() });

            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            // The file does not exist yet. Before starting the file observer, you
            // can update your UI to let the user know that the application is
            // waiting for the picture (for example, by displaying the thumbnail
            // image and a progress indicator).

            final File parentDirectory = pictureFile.getParentFile();
            FileObserver observer = new FileObserver(parentDirectory.getPath(),
                    FileObserver.CLOSE_WRITE | FileObserver.MOVED_TO) {
                // Protect against additional pending events after CLOSE_WRITE
                // or MOVED_TO is handled.
                private boolean isFileWritten;

                @Override
                public void onEvent(int event, String path) {
                    if (!isFileWritten) {
                        // For safety, make sure that the file that was created in
                        // the directory is actually the one that we're expecting.
                        File affectedFile = new File(parentDirectory, path);
                        isFileWritten = affectedFile.equals(pictureFile);

                        if (isFileWritten) {
                            stopWatching();

                            // Now that the file is ready, recursively call
                            // processPictureWhenReady again (on the UI thread).
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    processPictureWhenReady(picturePath);
                                }
                            });
                        }
                    }
                }
            };
            observer.startWatching();
        }
    }

    //Class to POST an image to the server
    private class SendToServer extends AsyncTask<String, Void, String> {

        private static final String TAG = "Test";

        private ProgressDialog processDlg = null;

        @Override
        protected String doInBackground(String... urls) {

            Log.i(TAG, "doInBackground");
            String fileLocation = urls[0];
            File pictureFile = new File(urls[1]);
            String fileName = pictureFile.getName();

            try{
                String lineEnd = "\r\n";
                String twoHyphens = "--";
                String boundary = "*****";
                int bytesRead, bytesAvailable, bufferSize;
                byte[] buffer;
                int maxBufferSize = 1 * 1024 * 1024;

                double latitude = 37.337166;
                double longitude = -121.881329;

                // open a URL connection to the Servlet
                FileInputStream fileInputStream = new FileInputStream(fileLocation);
//                URL url1 = new URL("http://ec2-52-64-0-154.ap-southeast-2.compute.amazonaws.com:8080/uploadImage");
                URL url1 = new URL("http://10.0.0.18:8080/uploadImage" + "?email=" + getEmailId() + "&latitude=" + latitude + "&longitude=" + longitude);

                // Open a HTTP  connection to  the URL
                HttpURLConnection conn = (HttpURLConnection) url1.openConnection();

                // Allow Inputs & Outputs
                conn.setDoInput(true); // Allow Inputs
                conn.setDoOutput(true); // Allow Outputs
                conn.setUseCaches(false); // Don't use a Cached Copy
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                conn.setRequestProperty("Content-Disposition", "upload");
                DataOutputStream dos = new DataOutputStream( conn.getOutputStream() );
                dos.writeBytes(twoHyphens + boundary + lineEnd);

                dos.writeBytes("Content-Disposition: form-data;name=\"file\";filename=\""
                        + fileName + "\"" + lineEnd);

                Log.i(TAG,"Content-Disposition: form-data;name=\"file\";filename=\""
                        + fileName + "\"" + lineEnd);
                dos.writeBytes(lineEnd);
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                // read file and write it into form...
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                Log.i(TAG, "Bytes Read - " + bytesRead);

                while (bytesRead > 0) {
                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                }

                // send multipart form data necesssary after file data...
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                fileInputStream.close();
                dos.flush();
                dos.close();

                // Responses from the server (code and message)
                int serverResponseCode = conn.getResponseCode();
                Log.i(TAG, fileName + " - code: " + serverResponseCode);

                // String serverResponseMessage = conn.getResponseMessage();

                //Get Response
                InputStream is = conn.getInputStream();
                BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                String line;
                StringBuffer response = new StringBuffer();
                while((line = rd.readLine()) != null) {
                    response.append(line);
                    response.append('\r');
                }
                rd.close();

                Log.i(TAG, fileLocation + " uploaded successfully for scanning!");
                JSONObject jObject  = new JSONObject(response.toString()); // json

            } catch(Exception e) {
                Log.i(TAG, fileLocation + " cannot be uploaded for scanning!");
                Log.i(TAG,e.getMessage());
                return null;
            }

            return "Success";

        }
    }

}

//LocationListener locationListener = new LocationListener() {
//    public void onLocationChanged(Location location) {
//        // Called when a new location is found by the network location provider.
//        makeUseOfNewLocation(location);
//    }
//
//    public void onStatusChanged(String provider, int status, Bundle extras) {}
//
//    public void onProviderEnabled(String provider) {}
//
//    public void onProviderDisabled(String provider) {}
//};
