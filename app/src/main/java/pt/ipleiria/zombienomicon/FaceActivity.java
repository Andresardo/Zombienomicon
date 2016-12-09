package pt.ipleiria.zombienomicon;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.UUID;

import pt.ipleiria.zombienomicon.Model.CameraSourcePreview;
import pt.ipleiria.zombienomicon.Model.GraphicOverlay;
import pt.ipleiria.zombienomicon.Model.Singleton;
import pt.ipleiria.zombienomicon.Model.Zombie;

public final class FaceActivity extends AppCompatActivity implements SensorEventListener {
    private static final String TAG = "FaceTracker";
    private static final int RC_HANDLE_GMS = 9001;
    private static final int RC_HANDLE_CAMERA_PERM = 2;
    private static final String MY_UUID = "8bed9cd2-e835-4163-af66-23ae08c9d6b1";
    private CameraSource mCameraSource = null;
    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;
    private TextView textViewTimer;
    private CountDownTimer timer;
    private int flagR;
    private int flagL;
    private int blinksR = 0;
    private int blinksL = 0;
    private int transitionR = 0;
    private int transitionL = 0;
    private BluetoothAdapter mBluetoothAdapter;
    private int receivedId = -1;
    private int zvk_state = 0;
    private Button button_bluetooth;
    private Button button_zvk;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private boolean isDead=true;

    //==============================================================================================
    // Activity Methods
    //==============================================================================================

    /**
     * Initializes the UI and initiates the creation of a face detector.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_face);

        textViewTimer = (TextView) findViewById(R.id.textViewTimer);
        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);
        button_bluetooth = (Button) findViewById(R.id.button_bluetooth);
        button_zvk = (Button) findViewById(R.id.button_ZVK);
        textViewTimer.setText(R.string.waiting_start);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);


        timer = new CountDownTimer(30000, 1000) {
            public void onTick(long millisUntilFinished) {
                textViewTimer.setText("" + millisUntilFinished / 1000);
            }

            public void onFinish() {

                if(zvk_state==1) {
                    if (receivedId == -1) {
                        createCameraSource();
                        startCameraSource();
                        zvk_state = 3;
                        textViewTimer.setText(R.string.exterminate);
                        timer.start();
                    }
                }

                if (zvk_state == 2) {
                    if (blinksL > 4 || blinksR > 4) {
                        createCameraSource();
                        startCameraSource();
                        zvk_state = 3;
                        textViewTimer.setText(R.string.exterminate);
                        timer.start();
                    } else {
                        textViewTimer.setText("The subject is human!");
                    }
                }

                if(zvk_state==3) {
                    textViewTimer.setText("You died!");
                }

                if(zvk_state==4){
                    if(!isDead){
                        textViewTimer.setText("You died!");
                    } else {
                        textViewTimer.setText("The Zombie died!");
                    }

                }
            }
        };
        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource();
        } else {
            requestCameraPermission();
        }
    }

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };

        Snackbar.make(mGraphicOverlay, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     */
    private void createCameraSource() {

        Context context = getApplicationContext();
        FaceDetector detector = new FaceDetector.Builder(context)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setProminentFaceOnly(true)
                .build();

        detector.setProcessor(
                new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory())
                        .build());

        if (!detector.isOperational()) {
            // Note: The first time that an app using face API is installed on a device, GMS will
            // download a native library to the device in order to do detection.  Usually this
            // completes before the app is run for the first time.  But if that download has not yet
            // completed, then the above call will not detect any faces.
            //
            // isOperational() can be used to check if the required native library is currently
            // available.  The detector will automatically become operational once the library
            // download completes on device.
            Log.w(TAG, "Face detector dependencies are not yet available.");
        }

        mCameraSource = new CameraSource.Builder(context, detector)
                .setRequestedPreviewSize(640, 480)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedFps(30.0f)
                .build();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPreview.stop();
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mSensorManager != null) {
            mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (zvk_state == 3) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            if (x > 15 || x < -15 || y > 15 || y < -15 || z > 15 || z < -15) {
                blinksR = 0;
                blinksL = 0;
                transitionR = 0;
                transitionL = 0;
                zvk_state = 4;
                textViewTimer.setText("Movement detection!");
                timer.cancel();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Toast.makeText(this, sensor.getName() + "accuracy changed to " + accuracy, Toast.LENGTH_SHORT).show();
    }

    /**
     * Releases the resources associated with the camera source, the associated detector, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraSource != null) {
            mCameraSource.release();
        }
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source");
            createCameraSource();
            return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Face Tracker sample")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show();
    }

    //==============================================================================================
    // Camera Source Preview
    //==============================================================================================

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() {

        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg = GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    public void buttonZVKOnClick(View view) {
        zvk_state = 2;
        createCameraSource();
        startCameraSource();
        button_zvk.setEnabled(false);
    }

    public void buttonBluetoothOnClick(View view) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available.", Toast.LENGTH_SHORT).show();
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth is not enabled.", Toast.LENGTH_SHORT).show();

            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);

        } else {
            zvk_state = 1;
            button_bluetooth.setEnabled(false);
            Toast.makeText(this, "Waiting to receive ID...", Toast.LENGTH_SHORT).show();
            timer.start();
            ServerTask serverTask = new ServerTask();
            serverTask.execute();
        }
    }

    private class ServerTask extends AsyncTask<String, Void, String> {
        private BluetoothServerSocket mmServerSocket;
        private InputStream inputStream;

        ServerTask() {
            try {
                mmServerSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("My Bluetooth App", UUID.fromString(MY_UUID));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected String doInBackground(String... params) {
            BluetoothSocket socket;
            String res = null;

            while (true) { // keep listening until exception occurs or a socket is returned
                try {
                    socket = mmServerSocket.accept();
                    Log.i("ServerTask", "Connection established.");
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }

                if (socket != null) { // if a connection was accepted
                    try {
                        String data = receiveData(socket);
                        Log.i("ServerTask", "Data received: " + data);
                        res = data;
                        timer.cancel();
                        if (!Objects.equals(data, "")) {
                            receivedId = Integer.parseInt(data);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    try {
                        mmServerSocket.close();
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        mmServerSocket = null;
                        break; // stop listening
                    }
                }
            }

            return res;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            if (mmServerSocket != null) {
                try {
                    mmServerSocket.close();
                    inputStream.close();
                } catch (IOException closeException) {
                    closeException.printStackTrace();
                }
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(FaceActivity.this);
            builder.setMessage(s);
            builder.create().show();
            if (receivedId != -1) {
                button_bluetooth.setVisibility(View.INVISIBLE);
                Zombie zombie= Singleton.getInstance().getZombienomicon().searchZombieByID(receivedId);
                if(zombie == null) {
                    button_zvk.setVisibility(View.VISIBLE);
                    textViewTimer.setText(R.string.id_received);
                }else{
                    createCameraSource();
                    startCameraSource();
                    zvk_state = 3;
                    textViewTimer.setText(R.string.exterminate);
                    timer.start();
                }
            } else {
                button_bluetooth.setEnabled(true);

            }
        }

        private String receiveData(BluetoothSocket socket) throws IOException {
            inputStream = socket.getInputStream();
            java.util.Scanner s = new java.util.Scanner(inputStream).useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        }
    }


    //==============================================================================================
    // Graphic Face Tracker
    //==============================================================================================

    /**
     * Factory for creating a face tracker to be associated with a new face.  The multiprocessor
     * uses this factory to create face trackers as needed -- one for each individual.
     */
    private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {
        @Override
        public Tracker<Face> create(Face face) {
            return new GraphicFaceTracker(mGraphicOverlay);
        }
    }

    /**
     * Face tracker for each detected individual. This maintains a face graphic within the app's
     * associated face overlay.
     */
    private class GraphicFaceTracker extends Tracker<Face> {
        private GraphicOverlay mOverlay;
        private FaceGraphic mFaceGraphic;

        GraphicFaceTracker(GraphicOverlay overlay) {
            mOverlay = overlay;
            mFaceGraphic = new FaceGraphic(overlay);
        }

        /**
         * Start tracking the detected face instance within the face overlay.
         */
        @Override
        public void onNewItem(int faceId, Face item) {
            if (zvk_state == 2 || zvk_state == 4) {
                mFaceGraphic.setId(faceId);
                timer.start();
                if (item.getIsRightEyeOpenProbability() > 0.7) {
                    flagR = 0;
                } else {
                    flagR = 1;
                }
                if (item.getIsRightEyeOpenProbability() > 0.7) {
                    flagL = 0;
                } else {
                    flagL = 1;
                }
            }
        }

        /**
         * Update the position/characteristics of the face within the overlay.
         */
        @Override
        public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
            mOverlay.add(mFaceGraphic);
            mFaceGraphic.updateFace(face);
            if (flagR == 0 && face.getIsRightEyeOpenProbability() < 0.7) {
                flagR = 1;
                transitionR++;
            }
            if (flagR == 1 && face.getIsRightEyeOpenProbability() > 0.7) {
                flagR = 0;
                transitionR++;
            }

            if (flagL == 0 && face.getIsRightEyeOpenProbability() < 0.7) {
                flagL = 1;
                transitionL++;
            }
            if (flagL == 1 && face.getIsRightEyeOpenProbability() > 0.7) {
                flagL = 0;
                transitionL++;
            }

            if (face.getIsSmilingProbability() > 0.5) {
                timer.cancel();
                finish();
            }
            if(zvk_state==2) {
                if (transitionR == 2) {
                    blinksR++;
                    transitionR = 0;
                }

                if (transitionL == 2) {
                    blinksR++;
                    transitionL = 0;
                }
            }
            if(zvk_state==4){
                if(transitionL!=0 || transitionR!=0){
                    isDead=false;
                }
            }
        }

        /**
         * Hide the graphic when the corresponding face was not detected.  This can happen for
         * intermediate frames temporarily (e.g., if the face was momentarily blocked from
         * view).
         */
        @Override
        public void onMissing(FaceDetector.Detections<Face> detectionResults) {
            mOverlay.remove(mFaceGraphic);
        }

        /**
         * Called when the face is assumed to be gone for good. Remove the graphic annotation from
         * the overlay.
         */
        @Override
        public void onDone() {
            mOverlay.remove(mFaceGraphic);
            blinksR = 0;
            blinksL = 0;
            transitionR = 0;
            transitionL = 0;
            timer.cancel();
        }
    }
}
