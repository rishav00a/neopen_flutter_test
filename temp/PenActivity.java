package kr.neolab.temp;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.*;
import android.util.Log;
import android.view.*;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import hackathon.co.kr.neopen.DefaultFunctionKt;
import hackathon.co.kr.neopen.R;
import kr.neolab.sdk.ink.structure.Dot;
import kr.neolab.sdk.ink.structure.Stroke;
import kr.neolab.sdk.pen.bluetooth.lib.ProtocolNotSupportedException;
import kr.neolab.sdk.pen.offline.OfflineFileParser;
import kr.neolab.sdk.pen.penmsg.JsonTag;
import kr.neolab.sdk.pen.penmsg.PenMsgType;
import kr.neolab.sdk.util.NLog;
import hackathon.co.kr.ui.activity.SubmitCompleteActivity;
import hackathon.co.kr.ui.dialog.SubmitDialog;
import hackathon.co.kr.util.SharedPreferenceUtilKt;
import hackathon.co.kr.util.network.NetworkUtil;
import hackathon.co.kr.util.DateUtils;
import hackathon.co.kr.util.network.model.ResponseVO;
import hackathon.co.kr.util.network.model2.BaseResponse;
import kotlin.Unit;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;

import static hackathon.co.kr.util.PhotoUtilKt.getImageFile;


public class PenActivity extends AppCompatActivity
//		implements DrawablePage.DrawablePageListener, DrawableView.DrawableViewGestureListener
{
    public static final String TAG = "pensdk.sample";
    public static boolean isInit = true;
    public static boolean isFront = true;

    public boolean mTimerRunning;
    private CountDownTimer mCountDownTimer;

    public static final int REQ_GPS_EXTERNAL_PERMISSION = 0x1002;

    private static final int REQUEST_CONNECT_DEVICE_SECURE = 4;

    private PenClientCtrl penClientCtrl;
    private MultiPenClientCtrl multiPenClientCtrl;

    private SampleView mSampleView;

    // Notification
    protected Notification.Builder mBuilder;
    protected NotificationManager mNotifyManager;
    protected Notification mNoti;

    public InputPasswordDialog inputPassDialog;

    private FwUpdateDialog fwUpdateDialog;

    private int currentSectionId = -1;
    private int currentOwnerId = -1;
    private int currentBookcodeId = -1;
    private int currentPagenumber = -1;
    private int connectionMode = 0;

    private ArrayList<String> connectedList = null;

    ConstraintLayout cl_front;
    FrameLayout fl_back;

    private ImageView ivQuiz;
    private TextView tvOriginTimerValue;
    private ImageView changeView;
    private ImageView ivTimerStart;

    private ProgressBar progressBar;

    private TextView tvCountDown;

    private long mStartTime = 0;
    private long mTimeLeftInMillis;

    private String imageUrl;

    private int pk;

    private String isOver = "false";

    private String title;
    private String subTitle;

    private LinearLayout llSubmitLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pen);

        getWindow().setStatusBarColor(getResources().getColor(R.color.color_3440ff));
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        // add back arrow to toolbar
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back_white);
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }

        pk = getIntent().getIntExtra("QUIZ_PK", -1);
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");

            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception ex) {
            // Ignore
        }

        title = getIntent().getStringExtra("TITLE");
        subTitle = getIntent().getStringExtra("SUB_TITLE");

        TextView toolbarTitle = findViewById(R.id.tv_toolbar_title);
        toolbarTitle.setText(subTitle);

        ivQuiz = findViewById(R.id.iv_quiz);
        Glide.with(getBaseContext()).load("http://ec2-15-164-171-69.ap-northeast-2.compute.amazonaws.com/api/v1/" + getIntent().getStringExtra("IMAGE_URL")).into(ivQuiz);
        mSampleView = new SampleView(this);
        FrameLayout.LayoutParams para = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        ((FrameLayout) findViewById(R.id.fl_back)).addView(mSampleView, 0, para);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, new Intent("firmware_update"), PendingIntent.FLAG_UPDATE_CURRENT);

        mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new Notification.Builder(getApplicationContext());
        mBuilder.setContentTitle("Update Pen");
        mBuilder.setSmallIcon(R.drawable.ic_launcher_n);
        mBuilder.setContentIntent(pendingIntent);


        chkPermissions();
        Intent oIntent = new Intent();
        oIntent.setClass(this, NeoSampleService.class);
        startService(oIntent);

        penClientSetting();

        findViewById(R.id.iv_ble_refresh).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (penClientCtrl.isConnected()) {
                    penClientCtrl.disconnect();
                }
                penClientSetting();
            }
        });

        cl_front = findViewById(R.id.cl_front);
        fl_back = findViewById(R.id.fl_back);
        llSubmitLayout = findViewById(R.id.ll_submit_layout);
        NetworkUtil.getInstance();

        changeView = findViewById(R.id.iv_change_screen);

        changeView.setOnClickListener(view -> {

                    // 이 화면이 current가 된지 처음일 경우
                    if (isInit) {
                        // 앞면이라면 (앞 -> 뒤)
                        if (isFront) {
                            llSubmitLayout.setVisibility(View.VISIBLE);
                            changeView.setImageResource(R.drawable.ic_quiz);
                            final ObjectAnimator oa1 = ObjectAnimator.ofFloat(cl_front, "scaleX", 1f, 0f);
                            final ObjectAnimator oa2 = ObjectAnimator.ofFloat(fl_back, "scaleX", 0f, 1f);
                            oa1.setDuration(300);
                            oa2.setDuration(300);

                            cl_front.setVisibility(View.VISIBLE);
                            fl_back.setVisibility(View.INVISIBLE);

                            oa1.setInterpolator(new DecelerateInterpolator());
                            oa2.setInterpolator(new AccelerateDecelerateInterpolator());
                            oa1.addListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    isFront = false;
                                    super.onAnimationEnd(animation);
                                    cl_front.setVisibility(View.INVISIBLE);
                                    fl_back.setVisibility(View.VISIBLE);
                                    oa2.start();
                                }
                            });

                            oa1.start();
                        }
                        // 뒤 -> 앞
                        else {
                            llSubmitLayout.setVisibility(View.INVISIBLE);
                            changeView.setImageResource(R.drawable.ic_write);
                            final ObjectAnimator oa1 = ObjectAnimator.ofFloat(fl_back, "scaleX", 1f, 0f);
                            final ObjectAnimator oa2 = ObjectAnimator.ofFloat(cl_front, "scaleX", 0f, 1f);

                            oa1.setDuration(300);
                            oa2.setDuration(300);

                            fl_back.setVisibility(View.VISIBLE);
                            cl_front.setVisibility(View.INVISIBLE);

                            oa1.setInterpolator(new DecelerateInterpolator());
                            oa2.setInterpolator(new AccelerateDecelerateInterpolator());
                            oa1.addListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    isFront = true;
                                    super.onAnimationEnd(animation);
                                    fl_back.setVisibility(View.INVISIBLE);
                                    cl_front.setVisibility(View.VISIBLE);
                                    oa2.start();
                                }
                            });

                            oa1.start();
                        }
                    } else {
                        if (isFront) {
                            fl_back.setVisibility(View.VISIBLE);
                            cl_front.setVisibility(View.INVISIBLE);

                        } else {
                            cl_front.setVisibility(View.VISIBLE);
                            fl_back.setVisibility(View.INVISIBLE);
                        }
                    }

                }
        );

//        findViewById(R.id.iv_capture).setOnClickListener(view ->
//                DefaultFunctionKt.getBitmapFromView(mSampleView, PenActivity.this, bitmap -> {
//                            Log.d("", bitmap.toString());
//
//                            return Unit.INSTANCE;
//                        }
//                )
//        );

        progressBar = findViewById(R.id.progress_timer);
        mStartTime = getIntent().getIntExtra("TIME_VALUE", 0);
        mTimeLeftInMillis = mStartTime * 1000;
        progressBar.setMax((int) mTimeLeftInMillis / 1000);
        progressBar.setProgress((int) (mTimeLeftInMillis / 1000));

        tvOriginTimerValue = findViewById(R.id.tv_origin_timer_value);
        DateUtils.updateCountDownText(tvOriginTimerValue, mStartTime);

        tvCountDown = findViewById(R.id.tv_countdown);
        DateUtils.updateCountDownText(tvCountDown, mStartTime);
        ivTimerStart = findViewById(R.id.iv_timer_play);

        ivTimerStart.setOnClickListener(v -> {
            if (mTimerRunning) { // pause
                pauseTimer();
            }
            else { // play
                startTimer();
            }

        });

        llSubmitLayout.setOnClickListener(v -> {
            SubmitDialog submitDialog = new SubmitDialog(PenActivity.this, new SubmitDialog.PositiveListener() {
                @Override
                public void onPositive() {
                    sendDataToServer();
                }
            }, new SubmitDialog.NegativeListener() {
                @Override
                public void onNegative() {
                }
            });
            submitDialog.init();
        });
    }

    private File file;
    private void sendDataToServer() {

        DefaultFunctionKt.getBitmapFromView(mSampleView, PenActivity.this, bitmap -> {
            Log.d("", bitmap.toString());
            file = getImageFile(this, bitmap);
            RequestBody requestBody = RequestBody.create(MediaType.parse("image/*"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestBody);

            RequestBody isOverRequest = RequestBody.create(MediaType.parse("text/pain"), isOver);
            RequestBody pkRequest = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(pk));
            RequestBody usingTimerRequest = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(mTimeLeftInMillis));

            NetworkUtil.getInstance().postAnswerPost(SharedPreferenceUtilKt.getSpStringData(SharedPreferenceUtilKt.token), body, pkRequest, isOverRequest, usingTimerRequest).enqueue(new Callback<BaseResponse>() {
                @Override
                public void onResponse(Call<BaseResponse> call, Response<BaseResponse> response) {
                    if (response.body() != null) {
                        if (response.body().code.equals("00")) {
                            Toast.makeText(getBaseContext(), "성공", Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(PenActivity.this, SubmitCompleteActivity.class);
                            intent.putExtra("TITLE", title);
                            intent.putExtra("SUB_TITLE", subTitle);
                            intent.putExtra("USING_TIMER", String.valueOf(mTimeLeftInMillis / 1000));
                            intent.putExtra("IMAGE_URL", response.body().result.getImageUrl());
                            startActivity(intent);
                            finish();
                        }

                    }
                    else {
                        Toast.makeText(getBaseContext(), "잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                    }

                }

                @Override
                public void onFailure(Call<BaseResponse> call, Throwable t) {
                    t.printStackTrace();

                }
            });
            return Unit.INSTANCE;
        });
    }

    private void startTimer() {

        mCountDownTimer = new CountDownTimer(mTimeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                mTimeLeftInMillis = millisUntilFinished;
                DateUtils.updateCountDownText(tvCountDown, mTimeLeftInMillis / 1000);
                progressBar.setProgress((int) (mTimeLeftInMillis / 1000));
            }

            @Override
            public void onFinish() {
                tvCountDown.setText("시간이 초과되었습니다.");
                tvCountDown.setTextColor(Color.parseColor("#FE3D04"));
                ivTimerStart.setVisibility(View.GONE);
                mTimerRunning = false;
                isOver = "true";
            }
        }.start();

        mTimerRunning = true;
        ivTimerStart.setImageResource(R.drawable.ic_pause);
    }

    private void pauseTimer() {

        mCountDownTimer.cancel();
        mTimerRunning = false;
        ivTimerStart.setImageResource(R.drawable.ic_play);

    }

    private void penClientSetting() {
        penClientCtrl = PenClientCtrl.getInstance(getApplicationContext());
        fwUpdateDialog = new FwUpdateDialog(PenActivity.this, penClientCtrl, mNotifyManager, mBuilder);
        penClientCtrl.connect("9C:7B:D2:05:6E:3F", null);
    }

    private void chkPermissions() {
        if (Build.VERSION.SDK_INT >= 23) {
            int gpsPermissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
            final int writeExternalPermissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

            if (gpsPermissionCheck == PackageManager.PERMISSION_DENIED || writeExternalPermissionCheck == PackageManager.PERMISSION_DENIED) {
                ArrayList<String> permissions = new ArrayList<String>();
                if (gpsPermissionCheck == PackageManager.PERMISSION_DENIED)
                    permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
                if (writeExternalPermissionCheck == PackageManager.PERMISSION_DENIED)
                    permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                requestPermissions(permissions.toArray(new String[permissions.size()]), REQ_GPS_EXTERNAL_PERMISSION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQ_GPS_EXTERNAL_PERMISSION) {
            boolean bGrantedExternal = false;
            boolean bGrantedGPS = false;
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    bGrantedExternal = true;
                } else if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION) && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    bGrantedGPS = true;
                }
            }

            if ((permissions.length == 1) && (bGrantedExternal || bGrantedGPS)) {
                bGrantedExternal = true;
                bGrantedGPS = true;
            }

            if (!bGrantedExternal || !bGrantedGPS) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Permission Check");
                builder.setMessage("PERMISSION_DENIED");
                builder.setPositiveButton("OK", (dialog, which) -> finish());
                builder.setCancelable(false);
                builder.create().show();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(mBroadcastReceiver);

    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter(Const.Broadcast.ACTION_PEN_MESSAGE);
        filter.addAction(Const.Broadcast.ACTION_PEN_DOT);
        filter.addAction(Const.Broadcast.ACTION_OFFLINE_STROKES);
        filter.addAction(Const.Broadcast.ACTION_WRITE_PAGE_CHANGED);
        filter.addAction("firmware_update");

        registerReceiver(mBroadcastReceiver, filter);
    }


        @Override
        protected void onDestroy () {
            // TODO Auto-generated method stub
            super.onDestroy();
//        unregisterReceiver( mBTDuplicateRemoveBroadcasterReceiver );
            Intent oIntent = new Intent();
            oIntent.setClass(this, NeoSampleService.class);
            stopService(oIntent);

            if (penClientCtrl != null)
                penClientCtrl.disconnect();
            if (multiPenClientCtrl != null) {
                ArrayList<String> penAddressList = multiPenClientCtrl.getConnectDevice();
                for (String address : penAddressList)
                    multiPenClientCtrl.disconnect(address);
            }

        }

        private void handleDot (String penAddress, Dot dot){
            NLog.d("penAddress=" + penAddress + ",handleDot type =" + dot.dotType);
            NLog.d("penAddress=" + penAddress + ",handleDot x =" + dot.x + ",handleDot y =" + dot.y);
            mSampleView.addDot(penAddress, dot);
            // mail (x : 87 ~ 91 | y : 9 ~ 12)
            if (dot.x > 87 && dot.x < 91 && dot.y > 9 && dot.y < 12) {
                Toast.makeText(this, "메일 보내라 당장", Toast.LENGTH_SHORT).show();
            }
        }

        private void handleMsg (String penAddress,int penMsgType, String content){
            Log.d(TAG, "penAddress=" + penAddress + ",handleMsg : " + penMsgType);

            switch (penMsgType) {
                // Message of the attempt to connect a pen
                case PenMsgType.PEN_CONNECTION_TRY:

                    Util.showToast(this, "연결중....");

                    break;

                // Pens when the connection is completed (state certification process is not yet in progress)
                case PenMsgType.PEN_CONNECTION_SUCCESS:

                    Util.showToast(this, "연결이 완료되었습니다.");
                    break;


                case PenMsgType.PEN_AUTHORIZED:
                    // OffLine Data set use
                    if (connectionMode == 0)
                        penClientCtrl.setAllowOfflineData(true);
                    else
                        multiPenClientCtrl.setAllowOfflineData(penAddress, true);
//                    Util.showToast(this, "connection is AUTHORIZED.");
                    break;
                // Message when a connection attempt is unsuccessful pen
                case PenMsgType.PEN_CONNECTION_FAILURE:

                    Util.showToast(this, "connection has failed.");

                    break;


                case PenMsgType.PEN_CONNECTION_FAILURE_BTDUPLICATE:
                    String connected_Appname = "";
                    try {
                        JSONObject job = new JSONObject(content);

                        connected_Appname = job.getString("packageName");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    Util.showToast(this, String.format("The pen is currently connected to %s app. If you want to proceed, please disconnect the pen from %s app.", connected_Appname, connected_Appname));
                    break;

                // When you are connected and disconnected from the state pen
                case PenMsgType.PEN_DISCONNECTED:

                    Util.showToast(this, "connection has been terminated.");
                    // Pen transmits the state when the firmware update is processed.
                case PenMsgType.PEN_FW_UPGRADE_STATUS:
                case PenMsgType.PEN_FW_UPGRADE_SUCCESS:
                case PenMsgType.PEN_FW_UPGRADE_FAILURE:
                case PenMsgType.PEN_FW_UPGRADE_SUSPEND: {
                    if (fwUpdateDialog != null)
                        fwUpdateDialog.setMsg(penAddress, penMsgType, content);
                }
                break;


                // Offline Data List response of the pen
                case PenMsgType.OFFLINE_DATA_NOTE_LIST:

                    try {
                        JSONArray list = new JSONArray(content);

                        for (int i = 0; i < list.length(); i++) {
                            JSONObject jobj = list.getJSONObject(i);

                            int sectionId = jobj.getInt(JsonTag.INT_SECTION_ID);
                            int ownerId = jobj.getInt(JsonTag.INT_OWNER_ID);
                            int noteId = jobj.getInt(JsonTag.INT_NOTE_ID);
                            NLog.d(TAG, "offline(" + (i + 1) + ") note => sectionId : " + sectionId + ", ownerId : " + ownerId + ", noteId : " + noteId);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    // if you want to get offline data of pen, use this function.
                    // you can call this function, after complete download.
                    //
                    break;

                // Messages for offline data transfer begins
                case PenMsgType.OFFLINE_DATA_SEND_START:

                    break;

                // Offline data transfer completion
                case PenMsgType.OFFLINE_DATA_SEND_SUCCESS:
                    if (connectionMode == 0) {
                        if (penClientCtrl.getProtocolVersion() == 1)
                            parseOfflineData(penAddress);
                    } else {
                        if (multiPenClientCtrl.getProtocolVersion(penAddress) == 1)
                            parseOfflineData(penAddress);
                    }

                    break;

                // Offline data transfer failure
                case PenMsgType.OFFLINE_DATA_SEND_FAILURE:

                    break;

                // Progress of the data transfer process offline
                // 오프라인 데이타를 전송 받을 때, 얼만큼 받았는지 확인 가능
                case PenMsgType.OFFLINE_DATA_SEND_STATUS: {
                    try {
                        JSONObject job = new JSONObject(content);

                        int total = job.getInt(JsonTag.INT_TOTAL_SIZE);
                        int received = job.getInt(JsonTag.INT_RECEIVED_SIZE);

                        Log.d(TAG, "offline data send status => total : " + total + ", progress : " + received);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                break;

                // When the file transfer process of the download offline
                case PenMsgType.OFFLINE_DATA_FILE_CREATED: {
                    try {
                        JSONObject job = new JSONObject(content);

                        int sectionId = job.getInt(JsonTag.INT_SECTION_ID);
                        int ownerId = job.getInt(JsonTag.INT_OWNER_ID);
                        int noteId = job.getInt(JsonTag.INT_NOTE_ID);
                        int pageId = job.getInt(JsonTag.INT_PAGE_ID);

                        String filePath = job.getString(JsonTag.STRING_FILE_PATH);

                        Log.d(TAG, "offline data file created => sectionId : " + sectionId + ", ownerId : " + ownerId + ", noteId : " + noteId + ", pageId : " + pageId + " filePath : " + filePath);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                break;

                // Ask for your password in a message comes when the pen
                case PenMsgType.PASSWORD_REQUEST: {
                    int retryCount = -1, resetCount = -1;

                    try {
                        JSONObject job = new JSONObject(content);

                        retryCount = job.getInt(JsonTag.INT_PASSWORD_RETRY_COUNT);
                        resetCount = job.getInt(JsonTag.INT_PASSWORD_RESET_COUNT);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if (inputPassDialog == null)
                        inputPassDialog = new InputPasswordDialog(this, this);
                    inputPassDialog.show(penAddress);
                }
                break;
                case PenMsgType.PEN_ILLEGAL_PASSWORD_0000: {
                    if (inputPassDialog == null)
                        inputPassDialog = new InputPasswordDialog(this, this);
                    inputPassDialog.show(penAddress);
                }
                break;

            }
        }

        public void inputPassword (String penAddress, String password){
            if (connectionMode == 0) {
                penClientCtrl.inputPassword(password);
            } else {
                multiPenClientCtrl.inputPassword(penAddress, password);
            }
        }

        private void parseOfflineData (String penAddress){
            // obtain saved offline data file list
            String[] files = OfflineFileParser.getOfflineFiles(penAddress);

            if (files == null || files.length == 0) {
                return;
            }

            for (String file : files) {
                try {
                    // create offline file parser instance
                    OfflineFileParser parser = new OfflineFileParser(file);

                    // parser return array of strokes
                    Stroke[] strokes = parser.parse();

                    if (strokes != null) {
                        // check offline symbol
//					ArrayList<Stroke> strokeList = new ArrayList( Arrays.asList( strokes ));
                        mSampleView.addStrokes(penAddress, strokes);
                    }

                    // delete data file
                    parser.delete();
                    parser = null;
                } catch (Exception e) {
                    Log.e(TAG, "parse file exeption occured.", e);
                }
            }
        }

        private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (Const.Broadcast.ACTION_PEN_MESSAGE.equals(action)) {
                    String penAddress = intent.getStringExtra(Const.Broadcast.PEN_ADDRESS);
                    int penMsgType = intent.getIntExtra(Const.Broadcast.MESSAGE_TYPE, 0);
                    String content = intent.getStringExtra(Const.Broadcast.CONTENT);

                    handleMsg(penAddress, penMsgType, content);
                } else if (Const.Broadcast.ACTION_PEN_DOT.equals(action)) {
                    String penAddress = intent.getStringExtra(Const.Broadcast.PEN_ADDRESS);
                    Dot dot = intent.getParcelableExtra(Const.Broadcast.EXTRA_DOT);
                    dot.color = Color.BLACK;
                    handleDot(penAddress, dot);
                } else if (Const.Broadcast.ACTION_OFFLINE_STROKES.equals(action)) {
                    String penAddress = intent.getStringExtra(Const.Broadcast.PEN_ADDRESS);
                    Parcelable[] array = intent.getParcelableArrayExtra(Const.Broadcast.EXTRA_OFFLINE_STROKES);
                    int sectionId = intent.getIntExtra(Const.Broadcast.EXTRA_SECTION_ID, -1);
                    int ownerId = intent.getIntExtra(Const.Broadcast.EXTRA_OWNER_ID, -1);
                    int noteId = intent.getIntExtra(Const.Broadcast.EXTRA_BOOKCODE_ID, -1);

                    if (array != null) {
                        Stroke[] strokes = new Stroke[array.length];
                        for (int i = 0; i < array.length; i++) {
                            strokes[i] = ((Stroke) array[i]);
                        }
                        mSampleView.addStrokes(penAddress, strokes);
                    }

                    // DB에 저장 후, 오프라인 데이터를 삭제합니다.
                    // 오프라인 데이터 요청 시, deleteOnFinished 를 true 로 요청했었다면, 아래의 과정은 필요없습니다.
                    // (오프라인 데이터 요청은 PenClientCtrl, MutiPenClientCtrl 에서 확인할 수 있습니다)
                    if (sectionId != -1 && ownerId != -1 && noteId != -1)
                        deleteOfflineData(penAddress, sectionId, ownerId, noteId);
                } else if (Const.Broadcast.ACTION_WRITE_PAGE_CHANGED.equals(action)) {
                    int sectionId = intent.getIntExtra(Const.Broadcast.EXTRA_SECTION_ID, -1);
                    int ownerId = intent.getIntExtra(Const.Broadcast.EXTRA_OWNER_ID, -1);
                    int noteId = intent.getIntExtra(Const.Broadcast.EXTRA_BOOKCODE_ID, -1);
                    int pageNum = intent.getIntExtra(Const.Broadcast.EXTRA_PAGE_NUMBER, -1);
                    currentSectionId = sectionId;
                    currentOwnerId = ownerId;
                    currentBookcodeId = noteId;
                    currentPagenumber = pageNum;
                    mSampleView.changePage(sectionId, ownerId, noteId, pageNum);
                }
            }
        };

        private void deleteOfflineData (String address,int section, int owner, int note){
            int[] noteArray = {note};
            if (connectionMode == 0) {
                try {
                    penClientCtrl.removeOfflineData(section, owner, noteArray);
                } catch (ProtocolNotSupportedException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    multiPenClientCtrl.removeOfflineData(address, section, owner, noteArray);
                } catch (ProtocolNotSupportedException e) {
                    e.printStackTrace();
                }
            }
        }

        public String getExternalStoragePath () {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                return Environment.getExternalStorageDirectory().getAbsolutePath();
            } else {
                return Environment.MEDIA_UNMOUNTED;
            }
        }
    }
