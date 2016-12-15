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
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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
import java.util.GregorianCalendar;
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
    private static final String FEEDBACK = "feedback";
    public static final int STATE_BLUETOOTH = 1;
    public static final int STATE_TEST = 2;
    public static final int STATE_KILL = 3;
    public static final int STATE_VERIFY = 4;
    private static final int STATE_FINAL = 5;
    private CameraSource mCameraSource = null;
    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;
    private TextView textView_Timer;
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
    private Button button_start;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private boolean isDead = false;
    private boolean isZombie = false;
    private TextView textView_Info;
    private Handler mHandler;
    private BluetoothServerSocket mmServerSocket;
    private String receivedName;
    private String receivedGender;
    private boolean testing = false;


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

        textView_Timer = (TextView) findViewById(R.id.textView_Timer);
        textView_Info = (TextView) findViewById(R.id.textView_Info);
        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);
        button_bluetooth = (Button) findViewById(R.id.button_bluetooth);
        button_zvk = (Button) findViewById(R.id.button_ZVK);
        button_start = (Button) findViewById(R.id.Start);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);


        timer = new CountDownTimer(10000, 1000) {
            public void onTick(long millisUntilFinished) {
                textView_Timer.setText(getString(R.string.time_left) + millisUntilFinished / 1000);
            }

            public void onFinish() {
                switch (zvk_state) {
                    case STATE_TEST:
                        if (blinksL > 4 || blinksR > 4) {
                            isZombie = true;
                            startCameraSource();
                            zvk_state = STATE_KILL;
                            textView_Info.setText(R.string.exterminate);
                            timer.start();
                        } else {
                            isZombie = false;
                            textView_Info.setText(R.string.subject_human);
                            lastMethod();
                        }
                        testing = false;
                        break;
                    case STATE_KILL:
                        isDead = false;
                        textView_Info.setText(R.string.you_died);
                        lastMethod();
                        break;
                    case STATE_VERIFY:
                        isDead = true;
                        textView_Info.setText(R.string.zombie_died);
                        lastMethod();
                        break;
                    default:
                        break;
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

        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                String feedback = msg.getData().getString(FEEDBACK);

                if (feedback != null) {
                    textView_Info.setText(feedback);
                }
                if(!Objects.equals(feedback, "Start test!") && !Objects.equals(feedback, "Subject lost!")) {
                    lastMethod();
                } else if(Objects.equals(feedback, "Start test!")){
                    button_start.setVisibility(View.VISIBLE);
                }
            }
        };
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
        if (zvk_state == STATE_KILL) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            if (x > 15 || x < -15 || y > 15 || y < -15 || z > 15 || z < -15) {
                blinksR = 0;
                blinksL = 0;
                transitionR = 0;
                transitionL = 0;
                zvk_state = STATE_VERIFY;
                textView_Info.setText(R.string.movement_detected);
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
        zvk_state = STATE_TEST;
        startCameraSource();
        button_zvk.setVisibility(View.INVISIBLE);
        textView_Info.setText(R.string.subject_search);
        textView_Info.setVisibility(View.VISIBLE);

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
            zvk_state = STATE_BLUETOOTH;
            button_bluetooth.setVisibility(View.INVISIBLE);
            textView_Info.setVisibility(View.VISIBLE);
            textView_Info.setText(R.string.receive_id_wait);
            timer.start();
            ServerTask serverTask = new ServerTask();
            serverTask.execute();
        }
    }

    public void buttonStartOnClick(View view) {
        timer.start();
        button_start.setVisibility(View.INVISIBLE);
        textView_Info.setText("Testing the subject!");
        testing = true;
    }

    private class ServerTask extends AsyncTask<String, Void, String> {
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
                    socket = mmServerSocket.accept(10000);
                    Log.i("ServerTask", "Connection established.");
                } catch (IOException e) {
                    e.printStackTrace();
                    res = "timeout";
                    break;
                }

                if (socket != null) { // if a connection was accepted
                    try {
                        String data = receiveData(socket);
                        Log.i("ServerTask", "Data received: " + data);
                        res = data;
                        timer.cancel();
                        if (!Objects.equals(data, "")) {
                            String[] split = data.split(":");
                            receivedId = Integer.parseInt(split[0]);
                            receivedName = split[1];
                            receivedGender = split[2];
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
                        inputStream = null;
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
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException closeException) {
                            closeException.printStackTrace();
                        }
                    }
                } catch (IOException closeException) {
                    closeException.printStackTrace();
                }
            }
            if (!Objects.equals(s, "timeout")) {
                AlertDialog.Builder builder = new AlertDialog.Builder(FaceActivity.this);

                if (receivedId != -1) {
                    builder.setMessage(s);
                    builder.create().show();
                    button_bluetooth.setVisibility(View.INVISIBLE);
                    if (Singleton.getInstance().getZombienomicon().searchZombieByID(receivedId) == null) {
                        button_zvk.setVisibility(View.VISIBLE);
                        textView_Info.setVisibility(View.INVISIBLE);
                    } else {
                        isZombie = true;
                        startCameraSource();
                        zvk_state = STATE_KILL;
                        textView_Info.setText(R.string.exterminate);
                        timer.start();
                    }
                } else {
                    builder.setMessage("Error receiving information. Please restart the test!");
                    builder.create().show();
                    button_bluetooth.setVisibility(View.VISIBLE);
                    textView_Info.setVisibility(View.INVISIBLE);
                }
            } else {
                startCameraSource();
                isZombie = true;
                zvk_state = STATE_KILL;
                textView_Info.setText(R.string.exterminate);
                timer.start();
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
            if (zvk_state == STATE_TEST || zvk_state == STATE_VERIFY) {
                mFaceGraphic.setZombie(isZombie);
                mFaceGraphic.setId(faceId);

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
                Message message = mHandler.obtainMessage();
                Bundle bundle = new Bundle();
                bundle.putString(FEEDBACK, "Start test!");
                message.setData(bundle);
                mHandler.sendMessage(message);
            }
        }

        /**
         * Update the position/characteristics of the face within the overlay.
         */
        @Override
        public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
            if (zvk_state == STATE_TEST || zvk_state == STATE_VERIFY) {
                mOverlay.add(mFaceGraphic);
                mFaceGraphic.updateFace(face);
                if (testing) {
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
                    if (zvk_state == STATE_TEST) {
                        if (face.getIsSmilingProbability() > 0.5) {
                            isZombie = false;
                            timer.cancel();
                            Message message = mHandler.obtainMessage();
                            Bundle bundle = new Bundle();
                            bundle.putString(FEEDBACK, "The subject is human!");
                            message.setData(bundle);
                            mHandler.sendMessage(message);
                            testing=false;
                        }

                        if (transitionR == 2) {
                            blinksR++;
                            transitionR = 0;
                        }

                        if (transitionL == 2) {
                            blinksR++;
                            transitionL = 0;
                        }
                    }
                    if (zvk_state == STATE_VERIFY) {
                        if (transitionL != 0 || transitionR != 0) {
                            isDead = false;
                            Message message = mHandler.obtainMessage();
                            Bundle bundle = new Bundle();
                            bundle.putString(FEEDBACK, "Failed to retire subject");
                            message.setData(bundle);
                            mHandler.sendMessage(message);
                            timer.cancel();
                            zvk_state = STATE_FINAL;
                        }
                    }
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
            timer.cancel();
        }

        /**
         * Called when the face is assumed to be gone for good. Remove the graphic annotation from
         * the overlay.
         */
        @Override
        public void onDone() {
            if (zvk_state == STATE_TEST || zvk_state == STATE_VERIFY) {
                mOverlay.remove(mFaceGraphic);
                blinksR = 0;
                blinksL = 0;
                transitionR = 0;
                transitionL = 0;
                button_start.setVisibility(View.INVISIBLE);
                Message message = mHandler.obtainMessage();
                Bundle bundle = new Bundle();
                bundle.putString(FEEDBACK, "Subject lost!");
                message.setData(bundle);
                mHandler.sendMessage(message);
                testing = false;
                timer.cancel();
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (mmServerSocket != null) {
            try {
                //se ainda estiver aberto fecho a socket, por prevenção
                mmServerSocket.close();
            } catch (IOException closeException) {

                closeException.printStackTrace();
            }
        }
        setResult(RESULT_CANCELED);
        finish();
    }

    public void lastMethod() {
        android.support.v7.app.AlertDialog.Builder editConfirmation = new android.support.v7.app.AlertDialog.Builder(FaceActivity.this);
        if (!isZombie) {
            editConfirmation.setTitle("The living shall rise!");
            editConfirmation.setMessage("The subject is human!");
            editConfirmation.setPositiveButton(
                    "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            finish();
                        }
                    });
        } else {
            if (isDead) {
                textView_Info.setText(R.string.zombie_died);
                editConfirmation.setTitle("The living shall rise!");
                editConfirmation.setMessage("The Zombie died!");
                editConfirmation.setPositiveButton(
                        "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                finish();
                            }
                        });
            } else {
                editConfirmation.setTitle("The dead shall rise!");
                editConfirmation.setMessage("You died!");
                editConfirmation.setPositiveButton(
                        "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                finish();
                            }
                        });
            }
            Zombie z = new Zombie(receivedId,(GregorianCalendar) GregorianCalendar.getInstance(), (GregorianCalendar) GregorianCalendar.getInstance(),receivedName,receivedGender,"leiria","dead");
            Singleton.getInstance().getZombienomicon().addZombie(z);
        }
        editConfirmation.setCancelable(false);
        editConfirmation.show();
    }
}