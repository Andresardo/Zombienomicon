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
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.MediaPlayer;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.IOException;
import java.io.InputStream;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import pt.ipleiria.zombienomicon.Model.CameraSourcePreview;
import pt.ipleiria.zombienomicon.Model.Gender;
import pt.ipleiria.zombienomicon.Model.GraphicOverlay;
import pt.ipleiria.zombienomicon.Model.Singleton;
import pt.ipleiria.zombienomicon.Model.State;
import pt.ipleiria.zombienomicon.Model.Weapon;
import pt.ipleiria.zombienomicon.Model.Zombie;

import static pt.ipleiria.zombienomicon.Model.Weapon.REVOLVER;

public final class FaceActivity extends AppCompatActivity implements SensorEventListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener , LocationListener {
    public static final int STATE_BLUETOOTH = 1;
    public static final int STATE_TEST = 2;
    public static final int STATE_WEAPON = 3;
    public static final int STATE_KILL = 4;
    public static final int STATE_VERIFY = 5;
    private static final String TAG = "FaceTracker";
    private static final int RC_HANDLE_GMS = 9001;
    private static final int RC_HANDLE_CAMERA_PERM = 2;
    private static final String MY_UUID = "8bed9cd2-e835-4163-af66-23ae08c9d6b1";
    private static final String FEEDBACK = "feedback";
    private static final int STATE_FINAL = 6;
    private CameraSource mCameraSource = null;
    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;
    private TextView textView_Timer;
    private CountDownTimer timer;
    private int flagR;
    private int flagL;
    private int flagW = 0;
    private int blinksR = 0;
    private int blinksL = 0;
    private int transitionR = 0;
    private int transitionL = 0;
    private int transitionW = 0;
    private BluetoothAdapter mBluetoothAdapter;
    private int receivedId = -1;
    private int zvk_state = 0;
    private Button button_bluetooth;
    private Button button_zvk;
    private Button button_start;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private boolean isDead = true;
    private boolean isZombie = false;
    private TextView textView_Info;
    private Handler mHandler;
    private BluetoothServerSocket mmServerSocket;
    private String receivedName;
    private Gender receivedGender;
    private String location;
    private boolean testing = false;
    private FaceDetector detector;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private ImageButton button_fist;
    private ImageButton button_sword;
    private ImageButton button_lightsaber;
    private ImageButton button_whip;
    private ImageButton button_revolver;
    private ImageButton button_rollingPin;
    private Weapon selected_weapon = Weapon.NONE;
    private GraphicOverlay mOverlay;
    private FaceGraphic mFaceGraphic;
    private TextView bluetooth_gif;
    private ImageView weaponImage;

    //==============================================================================================
    // Activity Methods
    //==============================================================================================

    /**
     * Inicializa as variaveis das Views, dos sensores, do timere, da camara e da localização
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
        button_fist = (ImageButton) findViewById(R.id.Button_Fist);
        button_sword = (ImageButton) findViewById(R.id.Button_Sword);
        button_lightsaber = (ImageButton) findViewById(R.id.Button_Lightsaber);
        button_whip = (ImageButton) findViewById(R.id.Button_Whip);
        button_revolver = (ImageButton) findViewById(R.id.Button_Revolver);
        button_rollingPin = (ImageButton) findViewById(R.id.Button_RollingPin);
        bluetooth_gif = (TextView) findViewById(R.id.bluetooth_gif);
        weaponImage = (ImageView) findViewById(R.id.weaponImage);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        /*
        ProgressBar mProgress = (ProgressBar) findViewById(R.id.progressBar);
        mProgress.setIndeterminate(false);
        mProgress.setMax(100);
        mProgress.setProgress(50);*/

        buildGoogleApiClient();
        mGoogleApiClient.connect();

        /**
         * Timer de 30 segundos
         */
        timer = new CountDownTimer(10000, 1000) {
            public void onTick(long millisUntilFinished) {
                String str = getString(R.string.time_left) + millisUntilFinished / 1000;
                textView_Timer.setText(str);
                //mProgress.setProgress((int)millisUntilFinished/100);
            }

            /**
             * Método de callback chamado quando o timer acaba a contagem
             */
            public void onFinish() {
                switch (zvk_state) {
                    /**
                     * Caso o timer chegue ao fim da contagem quando o processo se encontra no estado
                     * STATE_TEST, verifica-se se os olhos foram piscados 5 ou mais vezes
                     */
                    case STATE_TEST:
                        if (blinksL > 4 || blinksR > 4) {
                            /**
                             * Caso isto aconteça, significa que o sujeito de teste é um Zombie e o estado
                             * passa para STATE_WEAPON, onde o Zombie Runner pode escolher a arma
                             */
                            isZombie = true;
                            zvk_state = STATE_WEAPON;
                            textView_Info.setText(R.string.choose_weapon);
                            weaponButtonsVisible();
                        } else {
                            /**
                             * Caso não seja um Zombie, essa informação aparece num AlertDialog e termina
                             * a atividade (recorrendo ao lastMethod())
                             */
                            isZombie = false;
                            textView_Info.setText(R.string.subject_human);
                            lastMethod();
                        }
                        /**
                         * Independentemente de ser um Zombie ou não, a variavel testing passa a ser false
                         */
                        testing = false;
                        break;
                    case STATE_KILL:
                        /**
                         * Caso o timer chegue ao fim da contagem quando o processo se encontra no estado
                         * STATE_KILL, significa que não foi detetado um ataque ao Zombie: a atividade
                         * é terminada, ficando o estado do Zombie Undead
                         */
                        isDead = false;
                        textView_Info.setText(R.string.you_died);
                        lastMethod();
                        break;
                    case STATE_VERIFY:
                        /**
                         * Caso o timer chegue ao fim da contagem quando o processo se encontra no estado
                         * STATE_VERIFY, significa que o sujeito de teste não piscou os olhos, o que
                         * implica que este está morto.
                         */
                        isDead = true;
                        textView_Info.setText(R.string.zombie_died);
                        lastMethod();
                        break;
                    default:
                        break;
                }
            }
        };
        /**
         * Verifica se tem permissão da câmara antes de lhe aceder
         */
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource();
        } else {
            requestCameraPermission();
        }
    }

    /**
     * Este método lida com o pedido de permissão da câmara.
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
     * Cria e inicia a camâra.
     */
    private void createCameraSource() {

        Context context = getApplicationContext();
        /**
         * Cria um detetor para as caras. Este detetor apenas vê a cara mais proeminente
         */
        detector = new FaceDetector.Builder(context)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setProminentFaceOnly(true)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .build();

        detector.setProcessor(
                new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory())
                        .build());

        if (!detector.isOperational()) {
            Log.w(TAG, "Face detector dependencies are not yet available.");
        }

        mCameraSource = new CameraSource.Builder(context, detector)
                .setRequestedPreviewSize(640, 480)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedFps(30.0f)
                .build();

        /**
         * Caso se pretenda aceder à UI quando acontece algo nos métodos de callback da câmara
         * (onNewItem, onUpdate, onMissing ou onDone), é necessário estas comunicarem com a thread
         * da UI através de mensagens. Aqui cria-se o Handler para essas mensagens.
         */
        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                String feedback = msg.getData().getString(FEEDBACK);
                /**
                 * Escreve na textView o que recebe dos métodos acima referidos
                 */
                if (feedback != null) {
                    textView_Info.setText(feedback);
                }
                /**
                 * Caso se receba "Start test!" , o botão para iniciar o teste fica visivel;
                 * Caso se receba "Subject lost!", o botão para iniciar o teste fica invisivel;
                 * Caso contrário é chamado o lastMethod para terminar a atividade.
                 */
                if (!Objects.equals(feedback, "Start test!") && !Objects.equals(feedback, "Subject lost!")) {
                    lastMethod();
                } else if (Objects.equals(feedback, "Start test!")) {
                    button_start.setVisibility(View.VISIBLE);
                } else if (Objects.equals(feedback, "Subject lost!")) {
                    button_start.setVisibility(View.INVISIBLE);
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

    /**
     * Mátodo de callback chamado quando ha alterações nos sensores
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        /**
         * Os sensores apenas são relevante quando o processo se encontra no estado STATE_KILL
         */
        if (zvk_state == STATE_KILL) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            switch (selected_weapon) {
                /**
                 * Dependendo da arma escolhida, os movimentos para matar o Zombie são diferentes
                 */
                case SWORD:
                    if (x > 15 || x < -15 || y > 15 || y < -15 || z > 15 || z < -15) {
                        movementDetection();
                        MediaPlayer sound = MediaPlayer.create(FaceActivity.this, R.raw.sword);
                        sound.start();
                        timer.cancel();
                    }
                    break;
                case LIGHTSABER:
                    if (x > 15 || x < -15 || y > 15 || y < -15 || z > 15 || z < -15) {
                        movementDetection();
                        MediaPlayer sound = MediaPlayer.create(FaceActivity.this, R.raw.lightsaber);
                        sound.start();
                        timer.cancel();
                    }
                    break;
                case WHIP:
                    if (x < -15) {
                        flagW = 1;
                    } else if (x > 15 && flagW == 1) {
                        movementDetection();
                        MediaPlayer sound = MediaPlayer.create(FaceActivity.this, R.raw.whip);
                        sound.start();
                        timer.cancel();
                    }
                    break;
                case REVOLVER:
                    if (x < 15) {
                        flagW = 1;
                    } else if (x > -15 && flagW == 1) {
                        movementDetection();
                        MediaPlayer sound = MediaPlayer.create(FaceActivity.this, R.raw.revolver);
                        sound.start();
                        timer.cancel();
                    }
                    break;
                case FIST:
                    if (x > 15) {
                        movementDetection();
                        MediaPlayer sound = MediaPlayer.create(FaceActivity.this, R.raw.fist);
                        sound.start();
                        timer.cancel();
                    }
                    break;
                /**
                 * Para matar o Zombie com o rolo da massa é preciso "acertar-lhe" 3 vezes
                 */
                case ROLLINGPIN:
                    if (x < -15 || y < -15 || z < -15) {
                        flagW = 1;
                    } else if ((x > 15 || y > 15 || z > 15) && flagW == 1) {
                        MediaPlayer sound = MediaPlayer.create(FaceActivity.this, R.raw.rollingpin);
                        sound.start();
                        transitionW++;
                    }
                    if (transitionW == 3) {
                        transitionW=0;
                        movementDetection();
                        timer.cancel();
                    }
                    break;
                default:
                    Toast.makeText(this, "No weapon is selected!", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    /**
     * Mátodo que é chamado quando se verificou movimento das arma
     */
    private void movementDetection() {
        blinksR = 0;
        blinksL = 0;
        transitionR = 0;
        transitionL = 0;
        zvk_state = STATE_VERIFY;
        textView_Info.setText(R.string.movement_detected);
        weaponImage.setVisibility(View.INVISIBLE);
        timer.cancel();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Toast.makeText(this, sensor.getName() + "accuracy changed to " + accuracy, Toast.LENGTH_SHORT).show();
    }

    /**
     * Mátodo de callback chamado quando se pretende pedir permissões.
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
     * Começa ou reinicia a camera source, se exitir.
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

    /**
     * Método chamado quando se clica no botão para iniciar o teste. O estado é alterado para STATE_TEST
     * A câmara é iniciada
     */
    public void buttonZVKOnClick(View view) {
        zvk_state = STATE_TEST;
        startCameraSource();
        mPreview.setVisibility(View.VISIBLE);
        button_zvk.setVisibility(View.INVISIBLE);
        textView_Info.setText(R.string.subject_search);
        textView_Info.setVisibility(View.VISIBLE);
    }

    /**
     * Método chamado quando se clica no botão para esperar pelos dados por Bluetooth.
     * Caso o Bluetooth não estiver ativado, é pedido para o ser.
     * O estado passa para o STATE_BLUETOOTH
     * É criada uma AssyncTask que espera pelos dados.
     */
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
            bluetooth_gif.setVisibility(View.VISIBLE);
            textView_Info.setVisibility(View.VISIBLE);
            textView_Info.setText(R.string.receive_id_wait);
            timer.start();
            ServerTask serverTask = new ServerTask();
            serverTask.execute();
        }
    }

    /**
    * Método chamado quando se clica no botão para iniciar o teste
     * Este botão apenas está visivel quando o processo se encontra no estado STATE_TEST ou STATE_VERIFY
     * e quando o detetor reconhece uma cara
     */
    public void buttonStartOnClick(View view) {
        timer.start();
        button_start.setVisibility(View.INVISIBLE);
        textView_Info.setText(R.string.testing_subject);
        testing = true;
        startCameraSource();
    }

    /**
     * Método chamado quando se clica no botão da espada
     * A arma escolhida passa a ser a espada e o estado passa a ser STATE_KILL
     */
    public void buttonSwordOnClick(View view) {
        selected_weapon = Weapon.SWORD;
        weaponImage.setVisibility(View.VISIBLE);
        weaponImage.setImageResource(R.drawable.sword_fps);
        zvk_state = STATE_KILL;
        textView_Info.setText(R.string.exterminate);
        timer.start();
        startCameraSource();
        weaponButtonsInvisible();
    }

    /**
     * Método chamado quando se clica no botão do lightsaber
     * A arma escolhida passa a ser o lightsaber e o estado passa a ser STATE_KILL
     */
    public void buttonLightsaberOnClick(View view) {
        selected_weapon = Weapon.LIGHTSABER;
        weaponImage.setVisibility(View.VISIBLE);
        weaponImage.setImageResource(R.drawable.light_saber_fps);
        zvk_state = STATE_KILL;
        textView_Info.setText(R.string.exterminate);
        timer.start();
        startCameraSource();
        weaponButtonsInvisible();
    }

    /**
     * Método chamado quando se clica no botão do revolver
     * A arma escolhida passa a ser o revolver e o estado passa a ser STATE_KILL
     */
    public void buttonRevolverOnClick(View view) {
        selected_weapon = REVOLVER;
        weaponImage.setVisibility(View.VISIBLE);
        weaponImage.setImageResource(R.drawable.revolver_fps);
        zvk_state = STATE_KILL;
        textView_Info.setText(R.string.exterminate);
        timer.start();
        startCameraSource();
        weaponButtonsInvisible();
    }

    /**
     * Método chamado quando se clica no botão do punho
     * A arma escolhida passa a ser o punho e o estado passa a ser STATE_KILL
     */
    public void buttonFistOnClick(View view) {
        selected_weapon = Weapon.FIST;
        weaponImage.setVisibility(View.VISIBLE);
        weaponImage.setImageResource(R.drawable.fist_fps);
        zvk_state = STATE_KILL;
        textView_Info.setText(R.string.exterminate);
        timer.start();
        startCameraSource();
        weaponButtonsInvisible();
    }

    /**
     * Método chamado quando se clica no botão do chicote
     * A arma escolhida passa a ser o chicote e o estado passa a ser STATE_KILL
     */
    public void buttonWhipOnClick(View view) {
        selected_weapon = Weapon.WHIP;
        weaponImage.setVisibility(View.VISIBLE);
        weaponImage.setImageResource(R.drawable.whip_fps);
        zvk_state = STATE_KILL;
        textView_Info.setText(R.string.exterminate);
        timer.start();
        startCameraSource();
        weaponButtonsInvisible();
    }

    /**
     * Método chamado quando se clica no botão do rolo da massa
     * A arma escolhida passa a ser o rolo da massa e o estado passa a ser STATE_KILL
     */
    public void buttonRollingPinOnClick(View view) {
        selected_weapon = Weapon.ROLLINGPIN;
        weaponImage.setVisibility(View.VISIBLE);
        weaponImage.setImageResource(R.drawable.rolling_pin_fps);
        zvk_state = STATE_KILL;
        textView_Info.setText(R.string.exterminate);
        timer.start();
        startCameraSource();
        weaponButtonsInvisible();
    }

    /**
     * Método chamado quando se pretende que os botões das armas fiquem visiveis
     */
    private void weaponButtonsVisible() {
        mPreview.setVisibility(View.INVISIBLE);
        button_rollingPin.setVisibility(View.VISIBLE);
        button_sword.setVisibility(View.VISIBLE);
        button_lightsaber.setVisibility(View.VISIBLE);
        button_whip.setVisibility(View.VISIBLE);
        button_fist.setVisibility(View.VISIBLE);
        button_revolver.setVisibility(View.VISIBLE);
        textView_Timer.setVisibility(View.INVISIBLE);
    }

    /**
     * Método chamado quando se pretende que os botões das armas fiquem invisiveis
     */
    private void weaponButtonsInvisible() {
        mPreview.setVisibility(View.VISIBLE);
        button_rollingPin.setVisibility(View.INVISIBLE);
        button_sword.setVisibility(View.INVISIBLE);
        button_lightsaber.setVisibility(View.INVISIBLE);
        button_whip.setVisibility(View.INVISIBLE);
        button_fist.setVisibility(View.INVISIBLE);
        button_revolver.setVisibility(View.INVISIBLE);
        textView_Timer.setVisibility(View.VISIBLE);
    }

    /**
     * Método chamado quando se pressiona o botão de retrocesso
     * Caso o socket esteja aberto, este é fechado
     * Caso a camara estiver iniciada é libertada
     */
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
        if (mCameraSource != null) {
            mCameraSource.release();
        }
        setResult(RESULT_CANCELED);
    }

    //==============================================================================================
    // Graphic Face Tracker
    //==============================================================================================

    /**
     * Mátodo chamado quando se pretende terminar a atividade
     */
    public void lastMethod() {
        Zombie z;
        GregorianCalendar date;
        android.support.v7.app.AlertDialog.Builder editConfirmation = new android.support.v7.app.AlertDialog.Builder(FaceActivity.this);
        if (!isZombie) {
            /**
             * Caso o sujeito não seja Zombie, apresenta um AlertDialog com essa informação
             */
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
            /**
             * Caso contrário é verificado se o Zombie está morto ou não
             */
            if (isDead) {
                /**
                 * Caso seja um Zombie é apresentado um AlertDialog com a informação relativa ao seu estado
                 */
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
            /**
             * Caso não tenha sido recebido informação por bluetooth, o nome do Zombie passa a ser
             * "Unnamed Zombie"; o género passa a ser "Undefined" e o Id é o primeiro Id não utilizado
             */
            if (receivedId == -1) {
                receivedName = "Unnamed Zombie";
                receivedGender = Gender.UNDEFINED;
                receivedId = Singleton.getInstance().getZombienomicon().searchAvailableID();
            } else {
                /**
                 * Caso contrário, verifica se o Zombie com o Id recebido já existe
                 */
                if (Singleton.getInstance().getZombienomicon().searchZombieByID(receivedId) != null) {
                    /**
                     * Caso exista, este Zombie é removido da lista
                     */
                    Singleton.getInstance().getZombienomicon().deleteZombie(Singleton.getInstance().getZombienomicon().searchPositionByID(receivedId));
                }
            }
            /**
             * É criado um Zombie com os parâmetros corretos dependendo do estado
             */
            if (isDead) {
                z = new Zombie(receivedId, (GregorianCalendar) GregorianCalendar.getInstance(), (GregorianCalendar) GregorianCalendar.getInstance(), receivedName, receivedGender, location, State.BooleanState(isDead));
            } else {
                date = new GregorianCalendar(10, 1, 1);
                z = new Zombie(receivedId, (GregorianCalendar) GregorianCalendar.getInstance(), date, receivedName, receivedGender, location, State.BooleanState(isDead));
            }
            /**
             * O Zombie é adicionado à lista
             */
            Singleton.getInstance().getZombienomicon().addZombie(z);
        }
        /**
         * O estado passa a ser STATE_FINAL
         * É feito o release do detetor e da camara
         */
        zvk_state = STATE_FINAL;
        if(detector!=null){
            detector.release();
        }
        if(mCameraSource!=null) {
            mCameraSource.release();
        }

        editConfirmation.setCancelable(false);
        editConfirmation.show();
    }

    /**
     * É criado um novo ApiClient para aceder à localização
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
    }

    /**
     * Método chamado quando existe conexão
     */
    @Override
    public void onConnected(Bundle bundle) {
        Toast.makeText(FaceActivity.this, "Google API Client connected.", Toast.LENGTH_SHORT).show();

        /**
         * A última localização é pedida a cada segundo
         */
        LocationRequest mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_LOW_POWER)
                .setInterval(1000)
                .setFastestInterval(2000);

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        /**
         * Se a última localização for diferente de null, é chamado o método getLocation()
         */
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            getLocation();
        } else {
            Toast.makeText(FaceActivity.this, "ERROR: unable to get last location.", Toast.LENGTH_LONG).show();
        }

    }

    /**
     * Método chamado quando a conexão é suspendida
     */
    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(FaceActivity.this, "Google API Client connection suspended.", Toast.LENGTH_SHORT).show();
        mGoogleApiClient.connect();
    }

    /**
     * Método chamado quando se a conexão falhar
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(FaceActivity.this, "Google API Client connection failed.", Toast.LENGTH_SHORT).show();
    }

    /**
     * Mátodo que atualiza a variavel location para um String com o endereço e o país
     */
    private void getLocation() {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        try {
            List<Address> addresses = geocoder.getFromLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude(), 1);
            location = addresses.get(0).getAddressLine(0);
            if(addresses.get(0).getCountryName()!=null){
                location = location + addresses.get(0).getCountryName();
            }
            Toast.makeText(FaceActivity.this, "Location read.", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(FaceActivity.this, "ERROR: unable to get address from location.", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    /**
     * Metodo chamado quando existe uma atualização da localização
     */
    @Override
    public void onLocationChanged(Location location) {
        mLastLocation=location;
    }

    /**
     * Inner class que representa uma AsyncTask para quando se está à espera de receber dados por bluetooth
     */
    private class ServerTask extends AsyncTask<String, Void, String> {
        private InputStream inputStream;

        /**
         * É criado um novo socket
         */
        ServerTask() {
            try {
                mmServerSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("My Bluetooth App", UUID.fromString(MY_UUID));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Mátodo que faz algo em background
         */
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
                            /**
                             *Caso se receba algo diferente de uma String vazia, é feito um split
                             * por :, atribuindo os valores recebidos às respetivas variaveis
                             */
                            String[] split = data.split(":");
                            receivedId = Integer.parseInt(split[0]);
                            receivedName = split[1];
                            receivedGender = Gender.StringGender(split[2]);
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

        /**
         * Método chamado quando se acaba a execução
         */
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
                /**
                 * Caso não tenha existido timeout
                 */
                AlertDialog.Builder builder = new AlertDialog.Builder(FaceActivity.this);

                if (receivedId != -1) {
                    /**
                     * Caso se tenha recebido algo diferente de String vazia
                     */
                    builder.setMessage(s);
                    builder.create().show();
                    button_bluetooth.setVisibility(View.INVISIBLE);
                    bluetooth_gif.setVisibility(View.INVISIBLE);
                    mPreview.setVisibility(View.VISIBLE);
                    if (Singleton.getInstance().getZombienomicon().searchZombieByID(receivedId) == null) {
                        /**
                         * É verificado se existe um Zombie com o Id recebido na lista.
                         * Caso não exista o botão para iniciar o teste ZVK fica visivel
                         */
                        button_zvk.setVisibility(View.VISIBLE);
                        textView_Info.setVisibility(View.INVISIBLE);
                    } else {
                        /**
                         * Caso contrário, o estado passa a ser STATE_WEAPON, pois o sujeito é um Zombie
                         */
                        isZombie = true;
                        zvk_state = STATE_WEAPON;
                        textView_Info.setText(R.string.choose_weapon);
                        weaponButtonsVisible();
                    }
                } else {
                    /**
                     * Caso se receba String vazia, é apresentado ao Zombie Runner que houve um erro
                     * de comunicação e é necessário reiniciar o teste
                     */
                    builder.setMessage("Error receiving information. Please restart the test!");
                    builder.create().show();
                    timer.cancel();
                    button_bluetooth.setVisibility(View.VISIBLE);
                    textView_Info.setVisibility(View.INVISIBLE);
                }
            } else {
                /**
                 * Caso não se receba nada, ou seja, se houver um timeout, significa que o sujeito é
                 * um Zombie e o estado passa a ser STATE_WEAPON
                 */
                isZombie = true;
                zvk_state = STATE_WEAPON;
                bluetooth_gif.setVisibility(View.INVISIBLE);
                textView_Info.setText(R.string.choose_weapon);
                weaponButtonsVisible();
            }
        }

        private String receiveData(BluetoothSocket socket) throws IOException {
            inputStream = socket.getInputStream();
            java.util.Scanner s = new java.util.Scanner(inputStream).useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        }
    }

    /**
     * Criado um novo face tracker para ser associado a uma nova cara
     */
    private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {
        @Override
        public Tracker<Face> create(Face face) {
            return new GraphicFaceTracker(mGraphicOverlay);
        }
    }

    private class GraphicFaceTracker extends Tracker<Face> {
        private double blinkLimit = 0.5;

        GraphicFaceTracker(GraphicOverlay overlay) {
            mOverlay = overlay;
            mFaceGraphic = new FaceGraphic(overlay);
        }

        /**
         * Começa o seguimento da cara
         */
        @Override
        public void onNewItem(int faceId, Face item) {
            if (zvk_state == STATE_TEST || zvk_state == STATE_VERIFY) {
                /**
                 * Caso o estado seja STATE_TEST ou STATE_VERIFY verifica se os olhos do sujeito de
                 * teste estão abertos ou fechados
                 */
                mFaceGraphic.setZombie(isZombie);
                mFaceGraphic.setId(faceId);

                if (item.getIsRightEyeOpenProbability() > blinkLimit) {
                    flagR = 0;
                } else {
                    flagR = 1;
                }
                if (item.getIsRightEyeOpenProbability() > blinkLimit) {
                    flagL = 0;
                } else {
                    flagL = 1;
                }
                /**
                 * É enviada uma mensagem  de forma a que aceder-se à UI
                 */
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
            /**
             * Método chamado quando exite uma alteração na cara
             */
            if (zvk_state == STATE_TEST || zvk_state == STATE_VERIFY) {
                /**
                 * O update só é relevante quando estamos no estado STATE_TEST ou STATE_VERIFY
                 */
                mOverlay.add(mFaceGraphic);
                mFaceGraphic.updateFace(face);
                /**
                 * Dependendo do estado da flag, é visto quando existe uma transição olho aberto - olho fechado
                 * e vice-versa
                 */
                if (testing) {
                    if (flagR == 0 && face.getIsRightEyeOpenProbability() < blinkLimit) {
                        flagR = 1;
                        transitionR++;
                    } else {
                        if (flagR == 1 && face.getIsRightEyeOpenProbability() > blinkLimit) {
                            flagR = 0;
                            transitionR++;
                        } else {
                            if (flagL == 0 && face.getIsRightEyeOpenProbability() < blinkLimit) {
                                flagL = 1;
                                transitionL++;
                            } else {
                                if (flagL == 1 && face.getIsRightEyeOpenProbability() > blinkLimit) {
                                    flagL = 0;
                                    transitionL++;
                                }
                            }
                        }
                    }
                    if (zvk_state == STATE_TEST) {
                        /**
                         * Se o processo estiver no estado STATE_TEST
                         */
                        if (face.getIsSmilingProbability() > 0.5) {
                            /**
                             * Caso o sujeito de teste sorria, significa que não é Zombie
                             */
                            isZombie = false;
                            timer.cancel();
                            Message message = mHandler.obtainMessage();
                            Bundle bundle = new Bundle();
                            bundle.putString(FEEDBACK, "The subject is human!");
                            message.setData(bundle);
                            mHandler.sendMessage(message);
                            testing = false;
                        } else {
                            /**
                             * Caso existam 2 transições num olho, o número de blinks desse olho é
                             * incrementado
                             */
                            if (transitionR == 2) {
                                blinksR++;
                                transitionR = 0;
                            } else {
                                if (transitionL == 2) {
                                    blinksR++;
                                    transitionL = 0;
                                }
                            }
                        }
                    } else {
                        if (zvk_state == STATE_VERIFY) {
                            /**
                             * Caso o estado seja STATE_VERIFY, se existir uma transição, significa que
                             * o Zombie não morreu
                             */
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
        }

        /**
         * Esconde o face graphic quando a cara correspondente não for detetada.
         */
        @Override
        public void onMissing(FaceDetector.Detections<Face> detectionResults) {
            if (zvk_state == STATE_TEST || zvk_state == STATE_VERIFY) {
                mOverlay.remove(mFaceGraphic);
                timer.cancel();
            }
        }

        /**
         * Chamado quando é assumido que a cara desapareceu por completo
         */
        @Override
        public void onDone() {
            if (zvk_state == STATE_TEST || zvk_state == STATE_VERIFY) {
                mOverlay.remove(mFaceGraphic);
                blinksR = 0;
                blinksL = 0;
                transitionR = 0;
                transitionL = 0;
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
}