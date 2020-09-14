package com.haxotech.glinic_manager;
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.os.PersistableBundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import io.flutter.Log;
import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugins.GeneratedPluginRegistrant;
import kr.neolab.sdk.ink.structure.Dot;
import kr.neolab.sdk.ink.structure.Stroke;
import kr.neolab.sdk.metadata.MetadataCtrl;
import kr.neolab.sdk.metadata.structure.Symbol;
import kr.neolab.sdk.pen.IPenCtrl;
import kr.neolab.sdk.pen.PenCtrl;
import kr.neolab.sdk.pen.bluetooth.BLENotSupportedException;
import kr.neolab.sdk.pen.bluetooth.BTLEAdt;
import kr.neolab.sdk.pen.bluetooth.lib.ProtocolNotSupportedException;
import kr.neolab.sdk.pen.offline.OfflineFileParser;
import kr.neolab.sdk.pen.penmsg.IPenDotListener;
import kr.neolab.sdk.pen.penmsg.IPenMsgListener;
import kr.neolab.sdk.pen.penmsg.JsonTag;
import kr.neolab.sdk.pen.penmsg.PenMsg;
import kr.neolab.sdk.pen.penmsg.PenMsgType;
import kr.neolab.sdk.util.NLog;
import kr.neolab.sdk.util.UuidUtil;

import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.haxotech.glinic_manager.provider.DbOpenHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Set;
import java.util.concurrent.TimeUnit;


public class MainActivity extends  FlutterActivity {
    private static final String CHANNEL = "MyNativeChannel";
    private static final String EVENT_CHANNEL = "MyEventChannel";
    public static final String TAG = "pensdk.sample";

    public static final int REQ_GPS_EXTERNAL_PERMISSION = 0x1002;

    private static final int REQUEST_CONNECT_DEVICE_SECURE = 4;

    private PenClientCtrl penClientCtrl;
    private MultiPenClientCtrl multiPenClientCtrl;

    private com.haxotech.glinic_manager.SampleView mSampleView;

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
    private String connectedAddress = "";

    private ArrayList<String> connectedList= null;
    private IPenCtrl iPenCtrl;

    public static String EXTRA_DEVICE_SPP_ADDRESS = "device_spp_address";
    public static String EXTRA_DEVICE_LE_ADDRESS = "device_le_address";
    public static String EXTRA_IS_BLUETOOTH_LE = "is_bluetooth_le";
    public static String EXTRA_DEVICE_NAME = "device_name";
    public static String EXTRA_UUID_VER = "uuid_ver";
    public static String EXTRA_COLOR_CODE = "device_color_code";

    // Member fields
    private BluetoothAdapter mBtAdapter;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 10000;
    private BluetoothLeScanner mLeScanner;
    private ScanSettings mScanSetting;
    private List<ScanFilter> mScanFilters;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
//    private ArrayAdapter<String> mNewDevicesArrayAdapter;
    Button scanButton;
    Button scanLEButton;
    boolean is_le_scan = false;
    boolean isScanning = false;
    MyCustomListener myCustomListener = new MyCustomListener();
    HashMap<String, MainActivity.DeviceInfo> deviceMap = new HashMap<>();


    HashMap<String, String> customdeviceMap = new HashMap<>();
    ArrayList<HashMap<String, String>> customdeviceMapList = new ArrayList<>();
    SensorListener sensorListener = new SensorListener();
//    MainActivity.DeviceInfo deviceInfo = deviceMap.get(sppAddress);




    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        super.configureFlutterEngine(flutterEngine);
        NLog.d("registering ev hndler");



        new EventChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), EVENT_CHANNEL).setStreamHandler(myCustomListener);



        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL)
                .setMethodCallHandler(
                        (call, result) -> {
                            if(call.method.equals("initPen")){
                                Intent oIntent = new Intent();
                                oIntent.setClass( this, NeoSampleService.class );
                                startService( oIntent );

                                mSampleView = new com.haxotech.glinic_manager.SampleView( this );

                                chkPermissions ();

                                penClientCtrl = PenClientCtrl.getInstance( getApplicationContext() );
                                penClientCtrl.iPenCtrl.setDotListener( mPenReceiveDotListener );
//                                fwUpdateDialog = new FwUpdateDialog( MainActivity.this,penClientCtrl, mNotifyManager, mBuilder);
                                result.success("o Hello");
                            }
                            else  if(call.method.equals("getDeviceList")){

                                onOptionsItemSelected(1);
                                result.success(customdeviceMapList);
                            }

                            else  if(call.method.equals("connectDevice")){

                                String id = call.argument("id");
                                penClientCtrl.connect( id, "" );
                                result.success("Device connectDevice");
                            }

                            else  if(call.method.equals("saveFile")){


                                onOptionsItemSelected(11);
                                result.success("File Saved");
                            }


                            else {
                                result.notImplemented();
                            }
                        }
                );




    }


    private IPenDotListener mPenReceiveDotListener = new IPenDotListener() {

        @Override
        public void onReceiveDot ( String macAddress, Dot dot )
        {
            NLog.d( "NeoSampleService onReceiveDot mac_address="+macAddress+"dotType=" + dot.dotType+" ,pressure="+dot.pressure+",x="+dot.getX()+",y="+dot.getY() );
            myCustomListener.eventSink.success("mac_address="+macAddress+",dotType=" + dot.dotType+" ,pressure="+dot.pressure+",x="+dot.getX()+",y="+dot.getY());
            mSampleView.addDot(macAddress, dot );

//			sendBroadcastIfPageChanged(dot.sectionId, dot.ownerId, dot.noteId, dot.pageId);
//			enqueueDot( dot );
//			enqueueDotForBroadcast(macAddress, dot);

        }
    };





    //    @RequiresApi(api = VERSION_CODES.M)
//    @Override
    protected void onCreateOld( Bundle savedInstanceState )
    {
//        super.onCreate( savedInstanceState );

        setContentView( R.layout.activity_main );

        try
        {
            ViewConfiguration config = ViewConfiguration.get( this );
            Field menuKeyField = ViewConfiguration.class.getDeclaredField( "sHasPermanentMenuKey" );

            if ( menuKeyField != null )
            {
                menuKeyField.setAccessible( true );
                menuKeyField.setBoolean( config, false );
            }
        }
        catch ( Exception ex )
        {
            // Ignore
        }

        mSampleView = new com.haxotech.glinic_manager.SampleView( this );
        FrameLayout.LayoutParams para = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        ( (FrameLayout) findViewById( R.id.sampleview_frame ) ).addView( mSampleView, 0, para );

        PendingIntent pendingIntent = PendingIntent.getBroadcast( this, 0, new Intent( "firmware_update" ), PendingIntent.FLAG_UPDATE_CURRENT );

        mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new Notification.Builder(getApplicationContext());
        mBuilder.setContentTitle( "Update Pen" );
        mBuilder.setSmallIcon( R.drawable.ic_launcher_n );
        mBuilder.setContentIntent( pendingIntent );


        chkPermissions ();
        Intent oIntent = new Intent();
        oIntent.setClass( this, NeoSampleService.class );
        startService( oIntent );


        AlertDialog.Builder builder;
        builder = new AlertDialog.Builder( this );
        builder.setSingleChoiceItems( new CharSequence[]{ "Single Connection Mode", "Multi Connection Mode" }, connectionMode, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick (DialogInterface dialog, int which )
            {
                connectionMode = which;
                if(connectionMode == 0)
                {
                    penClientCtrl = PenClientCtrl.getInstance( getApplicationContext() );
                    fwUpdateDialog = new FwUpdateDialog( MainActivity.this,penClientCtrl, mNotifyManager, mBuilder);
                    Log.d( TAG, "SDK Version " + penClientCtrl.getSDKVerions() );
                }else
                {
                    multiPenClientCtrl = MultiPenClientCtrl.getInstance( getApplicationContext() );
                    fwUpdateDialog = new FwUpdateDialog( MainActivity.this,multiPenClientCtrl, mNotifyManager, mBuilder);
                    Log.d( TAG, "SDK Version " + multiPenClientCtrl.getSDKVerions() );
                }
                dialog.dismiss();
            }
        });
        builder.setCancelable( false );
        builder.create().show();
    }


    private void chkPermissions ()
    {
        if( Build.VERSION.SDK_INT >= 23)
        {
            int gpsPermissionCheck = ContextCompat.checkSelfPermission( this, Manifest.permission.ACCESS_FINE_LOCATION );
            final int writeExternalPermissionCheck = ContextCompat.checkSelfPermission( this, Manifest.permission.WRITE_EXTERNAL_STORAGE );

            if(gpsPermissionCheck == PackageManager.PERMISSION_DENIED || writeExternalPermissionCheck == PackageManager.PERMISSION_DENIED)
            {
                ArrayList<String> permissions = new ArrayList<String>();
                if(gpsPermissionCheck == PackageManager.PERMISSION_DENIED)
                    permissions.add( Manifest.permission.ACCESS_FINE_LOCATION );
                if(writeExternalPermissionCheck == PackageManager.PERMISSION_DENIED)
                    permissions.add( Manifest.permission.WRITE_EXTERNAL_STORAGE );
                requestPermissions( permissions.toArray( new String[permissions.size()] ), REQ_GPS_EXTERNAL_PERMISSION );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult ( int requestCode, String[] permissions, int[] grantResults )
    {
        if (requestCode == REQ_GPS_EXTERNAL_PERMISSION )
        {
            boolean bGrantedExternal = false;
            boolean bGrantedGPS = false;
            for ( int i = 0; i < permissions.length; i++ )
            {
                if ( permissions[i].equals( Manifest.permission.WRITE_EXTERNAL_STORAGE ) && grantResults[i] == PackageManager.PERMISSION_GRANTED )
                {
                    bGrantedExternal = true;
                }
                else if ( permissions[i].equals( Manifest.permission.ACCESS_FINE_LOCATION ) && grantResults[i] == PackageManager.PERMISSION_GRANTED )
                {
                    bGrantedGPS = true;
                }
            }

            if ( ( permissions.length == 1 ) && ( bGrantedExternal || bGrantedGPS ) )
            {
                bGrantedExternal = true;
                bGrantedGPS = true;
            }

            if ( !bGrantedExternal || !bGrantedGPS )
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle( "Permission Check" );
                builder.setMessage( "PERMISSION_DENIED" );
                builder.setPositiveButton( "OK", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick ( DialogInterface dialog, int which )
                    {
                        finish();
                    }
                } );
                builder.setCancelable( false );
                builder.create().show();
            }
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        unregisterReceiver( mBroadcastReceiver );

    }

    @Override
    protected void onResume()
    {
        super.onResume();

        IntentFilter filter = new IntentFilter( Const.Broadcast.ACTION_PEN_MESSAGE );
        filter.addAction( Const.Broadcast.ACTION_PEN_DOT );
        filter.addAction( Const.Broadcast.ACTION_OFFLINE_STROKES );
        filter.addAction( Const.Broadcast.ACTION_WRITE_PAGE_CHANGED );




        filter.addAction( "firmware_update" );

        registerReceiver( mBroadcastReceiver, filter );


    }

    @Override
    protected void onActivityResult( int requestCode, int resultCode, Intent data )
    {
        switch ( requestCode )
        {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if ( resultCode == Activity.RESULT_OK )
                {
                    String sppAddress = null;
                    String deviceName = data.getStringExtra( DeviceListActivity.EXTRA_DEVICE_NAME );
                    BTLEAdt.UUID_VER uuid_ver = BTLEAdt.UUID_VER.valueOf(data.getStringExtra( DeviceListActivity.EXTRA_UUID_VER));
                    int colorCode = data.getIntExtra( DeviceListActivity.EXTRA_COLOR_CODE, 0);

                    if ( (sppAddress = data.getStringExtra( DeviceListActivity.EXTRA_DEVICE_SPP_ADDRESS )) != null )
                    {
                        boolean isLe = data.getBooleanExtra( DeviceListActivity.EXTRA_IS_BLUETOOTH_LE, false);
                        String leAddress = data.getStringExtra( DeviceListActivity.EXTRA_DEVICE_LE_ADDRESS );

                        if(connectionMode == 0)
                        {
                            boolean leResult = penClientCtrl.setLeMode( isLe );

                            if( leResult )
                            {
                                if( uuid_ver == BTLEAdt.UUID_VER.VER_5 )
                                    penClientCtrl.connect( sppAddress, leAddress, false );
                                else
                                    penClientCtrl.connect( sppAddress, leAddress );
                            }
                            else
                            {
                                try
                                {
                                    penClientCtrl.connect( sppAddress );
                                } catch ( BLENotSupportedException e )
                                {
                                    e.printStackTrace();
                                }
                            }
                        }
                        else
                        {
                            multiPenClientCtrl.connect(sppAddress, leAddress, isLe );
                        }
                    }
                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu )
    {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate( R.menu.main, menu );

        return super.onCreateOptionsMenu( menu );
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
//        unregisterReceiver( mBTDuplicateRemoveBroadcasterReceiver );
        Intent oIntent = new Intent();
        oIntent.setClass( this, NeoSampleService.class );
        stopService( oIntent );

        if(penClientCtrl != null)
            penClientCtrl.disconnect();
        if(multiPenClientCtrl != null)
        {
            ArrayList<String> penAddressList = multiPenClientCtrl.getConnectDevice();
            for(String address : penAddressList)
                multiPenClientCtrl.disconnect( address );
        }

    }

    public boolean onOptionsItemSelected( int item )
    {
        int it = item;
        NLog.d(Integer.toString(it));
        // Handle presses on the action bar items
        switch ( item)
        {


            case 0:

                if(connectionMode == 0)
                {
                    if(penClientCtrl.isAuthorized())
                    {
                        Intent intent = new Intent( MainActivity.this, SettingActivity.class );
                        startActivity( intent);
                    }
                }
                else
                {
                    connectedList = multiPenClientCtrl.getConnectDevice();
                    if(connectedList.size() > 0)
                    {
                        AlertDialog.Builder builder;
                        String[] addresses = connectedList.toArray( new String[connectedList.size()]);
                        builder = new AlertDialog.Builder( this );
                        builder.setSingleChoiceItems( addresses, 0, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick ( DialogInterface dialog, int which )
                            {
                                Intent intent = new Intent( MainActivity.this, SettingActivity.class );
                                intent.putExtra("pen_address",  connectedList.get( which ) );
                                startActivity( intent);
                                dialog.dismiss();
                            }
                        });
                        builder.create().show();
                    }

                }
                return true;

            case 1:
                if(connectionMode == 1 || (connectionMode == 0 &&!penClientCtrl.isConnected()))
                {

                    doDiscovery(false);

//                    startActivityForResult( new Intent( MainActivity.this, DeviceListActivity.class ), 4 );
                }
                return true;

            case 2:
//                R.id.action_disconnect
                if(connectionMode == 0)
                {
                    if ( penClientCtrl.isConnected() )
                    {
                        penClientCtrl.disconnect();
                    }
                }
                else
                {
                    connectedList = multiPenClientCtrl.getConnectDevice();
                    if(connectedList.size() > 0)
                    {
                        AlertDialog.Builder builder;
                        String[] addresses = connectedList.toArray( new String[connectedList.size()]);
                        builder = new AlertDialog.Builder( this );
                        builder.setSingleChoiceItems( addresses, 0, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick ( DialogInterface dialog, int which )
                            {
                                multiPenClientCtrl.disconnect( connectedList.get( which ) );
                                dialog.dismiss();
                            }
                        });
                        builder.create().show();
                    }

                }

                return true;

            case 3:
//                R.id.action_offline_list
                if(connectionMode == 0)
                {
                    if ( penClientCtrl.isAuthorized() )
                    {
                        // to process saved offline data
                        penClientCtrl.reqOfflineDataList();
                    }
                }
                else
                {
                    connectedList = multiPenClientCtrl.getConnectDevice();
                    if(connectedList.size() > 0)
                    {
                        AlertDialog.Builder builder;
                        String[] addresses = connectedList.toArray( new String[connectedList.size()]);
                        builder = new AlertDialog.Builder( this );
                        builder.setSingleChoiceItems( addresses, 0, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick ( DialogInterface dialog, int which )
                            {
                                multiPenClientCtrl.reqOfflineDataList( connectedList.get( which ) );
                                dialog.dismiss();
                            }
                        });
                        builder.create().show();
                    }
                }
                return true;

            case 4:
//                R.id.action_offline_list_page
                // 펜에있는 오프라인 데이터 리스트를 페이지단위로 받아온다.

                final int sectionId = 0, ownerId = 0, noteId = 0;
                //TODO Put section, owner , note

                if(connectionMode == 0)
                {
                    if ( penClientCtrl.isAuthorized() )
                    {
                        // to process saved offline data

                        try
                        {
                            penClientCtrl.reqOfflineDataPageList(sectionId, ownerId, noteId);
                        } catch ( ProtocolNotSupportedException e )
                        {
                            e.printStackTrace();
                        }

                    }
                }
                else
                {
                    connectedList = multiPenClientCtrl.getConnectDevice();
                    if(connectedList.size() > 0)
                    {
                        AlertDialog.Builder builder;
                        String[] addresses = connectedList.toArray( new String[connectedList.size()]);
                        builder = new AlertDialog.Builder( this );
                        builder.setSingleChoiceItems( addresses, 0, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick ( DialogInterface dialog, int which )
                            {
                                try
                                {
                                    multiPenClientCtrl.reqOfflineDataPageList( connectedList.get( which ), sectionId, ownerId, noteId );
                                } catch ( ProtocolNotSupportedException e )
                                {
                                    e.printStackTrace();
                                }

                                dialog.dismiss();
                            }
                        });
                        builder.create().show();
                    }
                }

                return true;

            case 5:
//                R.id.action_offline_note_info
                if(connectionMode == 0)
                {
                    if(penClientCtrl.isAuthorized())
                    {
                        try
                        {
                            penClientCtrl.reqOfflineNoteInfo( currentSectionId, currentOwnerId, currentBookcodeId );
                        } catch ( ProtocolNotSupportedException e )
                        {
                            e.printStackTrace();
                        }
                    }
                }
                else
                {
                    connectedList = multiPenClientCtrl.getConnectDevice();
                    if(connectedList.size() > 0)
                    {
                        AlertDialog.Builder builder;
                        String[] addresses = connectedList.toArray( new String[connectedList.size()]);
                        builder = new AlertDialog.Builder( this );
                        builder.setSingleChoiceItems( addresses, 0, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick ( DialogInterface dialog, int which )
                            {
                                try
                                {
                                    multiPenClientCtrl.reqOfflineNoteInfo( connectedList.get( which ), currentSectionId, currentOwnerId, currentBookcodeId );
                                } catch ( ProtocolNotSupportedException e )
                                {
                                    e.printStackTrace();
                                }

                                dialog.dismiss();
                            }
                        });
                        builder.create().show();
                    }
                }
                return true;

            case 6:
//                R.id.action_upgrade
                if(connectionMode == 0)
                {
                    if ( penClientCtrl.isAuthorized() )
                    {
                        fwUpdateDialog.show( penClientCtrl.getConnectDevice() );
                    }
                }
                else
                {
                    connectedList = multiPenClientCtrl.getConnectDevice();
                    if(connectedList.size() > 0)
                    {
                        AlertDialog.Builder builder;
                        String[] addresses = connectedList.toArray( new String[connectedList.size()]);
                        builder = new AlertDialog.Builder( this );
                        builder.setSingleChoiceItems( addresses, 0, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick ( DialogInterface dialog, int which )
                            {
                                fwUpdateDialog.show( connectedList.get( which ) );
                                dialog.dismiss();
                            }
                        });
                        builder.create().show();
                    }
                }

                return true;

            case 7:
//                R.id.action_pen_status
                if(connectionMode == 0)
                {
                    if ( penClientCtrl.isAuthorized() )
                    {
                        penClientCtrl.reqPenStatus();
                    }
                }
                else
                {
                    connectedList = multiPenClientCtrl.getConnectDevice();
                    if(connectedList.size() > 0)
                    {
                        AlertDialog.Builder builder;
                        String[] addresses = connectedList.toArray( new String[connectedList.size()]);
                        builder = new AlertDialog.Builder( this );
                        builder.setSingleChoiceItems( addresses, 0, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick ( DialogInterface dialog, int which )
                            {
                                multiPenClientCtrl.reqPenStatus( connectedList.get( which ) );
                                dialog.dismiss();
                            }
                        });
                        builder.create().show();
                    }
                }
                return true;

            case 8:
//                R.id.action_profile_test
                if(penClientCtrl.isAuthorized())
                {
                    if(penClientCtrl.isSupportPenProfile())
                    {
                        startActivity( new Intent( MainActivity.this, ProfileTestActivity.class ) );

                    }
                    else
                    {
                        Util.showToast( this, "Firmware of this pen is not support pen profile feature." );
                    }
                }

                return true;

            case 9:
//                R.id.action_pen_unpairing
                if(connectionMode == 0)
                {
                    if(penClientCtrl.isAuthorized())
                        penClientCtrl.unpairDevice( penClientCtrl.getConnectDevice() ) ;
                }
                else
                {
                    connectedList = multiPenClientCtrl.getConnectDevice();
                    if(connectedList.size() > 0)
                    {
                        AlertDialog.Builder builder;
                        String[] addresses = connectedList.toArray( new String[connectedList.size()]);
                        for(String addr  : addresses)
                        {
                            penClientCtrl.unpairDevice( addr ) ;
                        }
                    }
                }
                return true;

            case 10:
//                R.id.action_symbol_stroke
                // 특정 페이지의 심볼 리스트를 추출, 스트로크 데이터를 입력하여서 이미지를 추출할 수 있는 샘플

                // 특정 페이지의 심볼 리스트를 추출
                Symbol[] symbols = MetadataCtrl.getInstance().findApplicableSymbols( currentBookcodeId, currentPagenumber );

                // 해당 심볼 중, 원하는 심볼을 선택해서 이미지를 만든다.
                // 본 샘플에서는 임의로 첫번째 심볼을 선택하였음. 아래 부분을 수정하여 원하는 심볼을 선택할 수 있다.
                if( symbols != null && symbols.length > 0 )
                    mSampleView.makeSymbolImage( symbols[0] );

                return true;

            case 11:
//                R.id.action_convert_neoink
                // 현재 페이지의 stroke 를 NeoInk format 으로 변환합니다.
                // 변환된 파일은 json 형식으로 지정된 위치에 저장합니다.
                if(connectionMode == 0)
                {
                    String captureDevice = penClientCtrl.getDeviceName();
                    mSampleView.makeNeoInkFile( captureDevice );
                }
                else
                {
                    connectedList = multiPenClientCtrl.getConnectDevice();
                    if(connectedList.size() > 0)
                    {
                        AlertDialog.Builder builder;
                        String[] addresses = connectedList.toArray( new String[connectedList.size()]);
                        builder = new AlertDialog.Builder( this );
                        builder.setSingleChoiceItems( addresses, 0, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick ( DialogInterface dialog, int which )
                            {
                                String captureDevice = multiPenClientCtrl.getDeviceName( connectedList.get( which ) );
                                mSampleView.makeNeoInkFile( captureDevice );
                                dialog.dismiss();
                            }
                        });
                        builder.create().show();
                    }
                }
                return true;

            case R.id.action_db_export:

                // DB Export
                Util.spliteExport( this );

                return true;

            case R.id.action_db_delete:

                DbOpenHelper mDbOpenHelper = new DbOpenHelper( this);
                mDbOpenHelper.deleteAllColumns();

            default:
                return true;
//                return super.onOptionsItemSelected( item );
        }
    }

    private void handleDot(String penAddress, Dot dot )
    {
        Log.d(  TAG,"penAddress="+penAddress+",handleDot type =" + dot.dotType );
        mSampleView.addDot(penAddress, dot );
    }

    private void handleMsg(String penAddress, int penMsgType, String content )
    {
        Log.d( TAG, "penAddress="+penAddress+",handleMsg : " + penMsgType );

        switch ( penMsgType )
        {
            // Message of the attempt to connect a pen
            case PenMsgType.PEN_CONNECTION_TRY:

                Util.showToast( this, "try to connect." );
                Message message = new Message();

                myCustomListener.eventSink.success("PEN_CONNECTION_TRY");
                break;

            // Pens when the connection is completed (state certification process is not yet in progress)
            case PenMsgType.PEN_CONNECTION_SUCCESS:

                Util.showToast( this, "connection is successful." );
                myCustomListener.eventSink.success("PEN_CONNECTION_SUCCESS");
                break;


            case PenMsgType.PEN_AUTHORIZED:
                // OffLine Data set use
                if(connectionMode == 0)
                    penClientCtrl.setAllowOfflineData( true );
                else
                    multiPenClientCtrl.setAllowOfflineData(penAddress, true );
                this.connectedAddress = penAddress;
                Util.showToast( this, "connection is AUTHORIZED." );
                myCustomListener.eventSink.success("PEN_AUTHORIZED");

                break;
            // Message when a connection attempt is unsuccessful pen
            case PenMsgType.PEN_CONNECTION_FAILURE:

                Util.showToast( this, "connection has failed." );
                myCustomListener.eventSink.success("PEN_CONNECTION_FAILURE");

                break;


            case PenMsgType.PEN_CONNECTION_FAILURE_BTDUPLICATE:
                String connected_Appname = "";
                try
                {
                    JSONObject job = new JSONObject( content );

                    connected_Appname = job.getString("packageName");
                }
                catch ( JSONException e )
                {
                    e.printStackTrace();
                }

                Util.showToast( this, String.format("The pen is currently connected to %s app. If you want to proceed, please disconnect the pen from %s app.",connected_Appname,connected_Appname));
                myCustomListener.eventSink.success("PEN_CONNECTION_FAILURE_BTDUPLICATE");
                break;

            // When you are connected and disconnected from the state pen
            case PenMsgType.PEN_DISCONNECTED:

                Util.showToast( this, "connection has been terminated." );
                // Pen transmits the state when the firmware update is processed.
            case PenMsgType.PEN_FW_UPGRADE_STATUS:
            case PenMsgType.PEN_FW_UPGRADE_SUCCESS:
            case PenMsgType.PEN_FW_UPGRADE_FAILURE:
            case PenMsgType.PEN_FW_UPGRADE_SUSPEND:
            {
                if(fwUpdateDialog != null)
                    fwUpdateDialog.setMsg(penAddress, penMsgType, content);
            }
            break;


            // Offline Data List response of the pen
            case PenMsgType.OFFLINE_DATA_NOTE_LIST:

                try
                {
                    JSONArray list = new JSONArray( content );

                    for ( int i = 0; i < list.length(); i++ )
                    {
                        JSONObject jobj = list.getJSONObject( i );

                        int sectionId = jobj.getInt( JsonTag.INT_SECTION_ID );
                        int ownerId = jobj.getInt( JsonTag.INT_OWNER_ID );
                        int noteId = jobj.getInt( JsonTag.INT_NOTE_ID );
                        NLog.d( TAG, "offline(" + ( i + 1 ) + ") note => sectionId : " + sectionId + ", ownerId : " + ownerId + ", noteId : " + noteId );
                    }
                }
                catch ( JSONException e )
                {
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
                if(connectionMode == 0)
                {
                    if(penClientCtrl.getProtocolVersion() ==1)
                        parseOfflineData(penAddress);
                }
                else
                {
                    if(multiPenClientCtrl.getProtocolVersion( penAddress) == 1)
                        parseOfflineData(penAddress);
                }

                break;

            // Offline data transfer failure
            case PenMsgType.OFFLINE_DATA_SEND_FAILURE:

                break;

            // Progress of the data transfer process offline
            case PenMsgType.OFFLINE_DATA_SEND_STATUS:
            {
                try
                {
                    JSONObject job = new JSONObject( content );

                    int total = job.getInt( JsonTag.INT_TOTAL_SIZE );
                    int received = job.getInt( JsonTag.INT_RECEIVED_SIZE );

                    Log.d( TAG, "offline data send status => total : " + total + ", progress : " + received );
                }
                catch ( JSONException e )
                {
                    e.printStackTrace();
                }
            }
            break;

            // When the file transfer process of the download offline
            case PenMsgType.OFFLINE_DATA_FILE_CREATED:
            {
                try
                {
                    JSONObject job = new JSONObject( content );

                    int sectionId = job.getInt( JsonTag.INT_SECTION_ID );
                    int ownerId = job.getInt( JsonTag.INT_OWNER_ID );
                    int noteId = job.getInt( JsonTag.INT_NOTE_ID );
                    int pageId = job.getInt( JsonTag.INT_PAGE_ID );

                    String filePath = job.getString( JsonTag.STRING_FILE_PATH );

                    Log.d( TAG, "offline data file created => sectionId : " + sectionId + ", ownerId : " + ownerId + ", noteId : " + noteId + ", pageId : " + pageId + " filePath : " + filePath );
                }
                catch ( JSONException e )
                {
                    e.printStackTrace();
                }
            }
            break;

            // Ask for your password in a message comes when the pen
            case PenMsgType.PASSWORD_REQUEST:
            {
                int retryCount = -1, resetCount = -1;

                try
                {
                    JSONObject job = new JSONObject( content );

                    retryCount = job.getInt( JsonTag.INT_PASSWORD_RETRY_COUNT );
                    resetCount = job.getInt( JsonTag.INT_PASSWORD_RESET_COUNT );
                }
                catch ( JSONException e )
                {
                    e.printStackTrace();
                }

                if(inputPassDialog == null)
                    inputPassDialog = new InputPasswordDialog( this, this);
                inputPassDialog.show(penAddress);
            }
            break;
            case PenMsgType.PEN_ILLEGAL_PASSWORD_0000:
            {
                if(inputPassDialog == null)
                    inputPassDialog = new InputPasswordDialog( this, this );
                inputPassDialog.show(penAddress);
            }

            case PenMsgType.OFFLINE_NOTE_INFO:
            {
                try
                {
                    JSONObject job = new JSONObject( content );

                    int sectionId = job.getInt( JsonTag.INT_SECTION_ID );
                    int ownerId = job.getInt( JsonTag.INT_OWNER_ID );
                    int noteId = job.getInt( JsonTag.INT_NOTE_ID );
                    int noteVersion = job.getInt( kr.neolab.sdk.pen.penmsg.JsonTag.INT_NOTE_VERSION );
                    boolean isInvalidpage = job.getBoolean( kr.neolab.sdk.pen.penmsg.JsonTag.BOOL_OFFLINE_INFO_INVALID_PAGE );
                    String pageList = job.getString( kr.neolab.sdk.pen.penmsg.JsonTag.STRING_OFFLINE_INFO_PAGE_LIST );

                    Util.showToast( this, "offline note info => sectionId : " + sectionId + ", ownerId : " + ownerId + ", noteId : " + noteId + ", isInvalidPage : " + isInvalidpage + " pageList : " + pageList );
//					Log.d( TAG, "offline note info => sectionId : " + sectionId + ", ownerId : " + ownerId + ", noteId : " + noteId + ", isInvalidPage : " + isInvalidpage + " pageList : " + msg );
                }
                catch ( JSONException e )
                {
                    e.printStackTrace();
                }
            }
            break;

        }
    }

    public void inputPassword(String penAddress,  String password )
    {
        if(connectionMode == 0)
        {
            penClientCtrl.inputPassword(password);
        }
        else
        {
            multiPenClientCtrl.inputPassword(penAddress, password);
        }
    }

    private void parseOfflineData(String penAddress)
    {
        // obtain saved offline data file list
        String[] files = OfflineFileParser.getOfflineFiles(penAddress);

        if ( files == null || files.length == 0 )
        {
            return;
        }

        for ( String file : files )
        {
            try
            {
                // create offline file parser instance
                OfflineFileParser parser = new OfflineFileParser( file );

                // parser return array of strokes
                Stroke[] strokes = parser.parse();

                if ( strokes != null )
                {
                    // check offline symbol
//					ArrayList<Stroke> strokeList = new ArrayList( Arrays.asList( strokes ));
                    mSampleView.addStrokes( penAddress, strokes );
                }

                // delete data file
                parser.delete();
                parser = null;
            }
            catch ( Exception e )
            {
                Log.e( TAG, "parse file exeption occured.", e );
            }
        }
    }


    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive( Context context, Intent intent )
        {
            String action = intent.getAction();

            if ( Const.Broadcast.ACTION_PEN_MESSAGE.equals( action ) )
            {
                String penAddress = intent.getStringExtra( Const.Broadcast.PEN_ADDRESS );
                int penMsgType = intent.getIntExtra( Const.Broadcast.MESSAGE_TYPE, 0 );
                String content = intent.getStringExtra( Const.Broadcast.CONTENT );

                handleMsg( penAddress, penMsgType, content );
            }
            else if ( Const.Broadcast.ACTION_PEN_DOT.equals( action ) )
            {
                String penAddress = intent.getStringExtra( Const.Broadcast.PEN_ADDRESS );
                Dot dot = intent.getParcelableExtra(Const.Broadcast.EXTRA_DOT );
                dot.color = Color.BLACK;
                NLog.d("handleDot"+":"+penAddress);
                handleDot(penAddress, dot );
            }
            else if(Const.Broadcast.ACTION_OFFLINE_STROKES.equals( action ))
            {
                String penAddress = intent.getStringExtra( Const.Broadcast.PEN_ADDRESS );
                Parcelable[] array = intent.getParcelableArrayExtra( Const.Broadcast.EXTRA_OFFLINE_STROKES );
                int sectionId = intent.getIntExtra( Const.Broadcast.EXTRA_SECTION_ID, -1 );
                int ownerId = intent.getIntExtra( Const.Broadcast.EXTRA_OWNER_ID , -1);
                int noteId = intent.getIntExtra( Const.Broadcast.EXTRA_BOOKCODE_ID , -1);

                if(array != null)
                {
                    Stroke[] strokes  = new Stroke[array.length];
                    for (int i = 0; i < array.length; i++) {
                        strokes[i] = ((Stroke) array[i]);
                    }
                    mSampleView.addStrokes(penAddress, strokes);
                }

            }
            else if(Const.Broadcast.ACTION_WRITE_PAGE_CHANGED.equals( action ))
            {
                int sectionId = intent.getIntExtra( Const.Broadcast.EXTRA_SECTION_ID, -1);
                int ownerId = intent.getIntExtra( Const.Broadcast.EXTRA_OWNER_ID, -1);
                int noteId = intent.getIntExtra( Const.Broadcast.EXTRA_BOOKCODE_ID, -1);
                int pageNum = intent.getIntExtra( Const.Broadcast.EXTRA_PAGE_NUMBER, -1);
                currentSectionId = sectionId;
                currentOwnerId = ownerId;
                currentBookcodeId = noteId;
                currentPagenumber =pageNum;
                mSampleView.changePage(sectionId, ownerId,noteId,pageNum );
            }
        }
    };

    private void deleteOfflineData(String address, int section, int owner, int note)
    {
        int[] noteArray = {note};
        if( connectionMode == 0)
        {
            try
            {
                penClientCtrl.removeOfflineData( section, owner, noteArray );
            } catch ( ProtocolNotSupportedException e )
            {
                e.printStackTrace();
            }
        }
        else
        {
            try
            {
                multiPenClientCtrl.removeOfflineData( address, section, owner, noteArray );
            } catch ( ProtocolNotSupportedException e )
            {
                e.printStackTrace();
            }
        }
    }

    public String getExternalStoragePath()
    {
        if ( Environment.getExternalStorageState().equals( Environment.MEDIA_MOUNTED ) )
        {
            return Environment.getExternalStorageDirectory().getAbsolutePath();
        }
        else
        {
            return Environment.MEDIA_UNMOUNTED;
        }
    }



    public void doDiscovery(boolean le)
    {
        NLog.d("doDiscovery()");

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        if (pairedDevices.size() > 0)
        {
//            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);

            for (BluetoothDevice device : pairedDevices)
            {
                MainActivity.DeviceInfo info = new MainActivity.DeviceInfo();
                info.sppAddress = device.getAddress();
                info.leAddress = "";
                info.deviceName = device.getName();
                info.isLe = is_le_scan;
                info.uuidVer = BTLEAdt.UUID_VER.VER_2.toString();
                info.colorCode = -1;

                deviceMap.put( device.getAddress(), info );
                customdeviceMap.put(device.getAddress(), device.getName());
                HashMap<String, String> hashMM = new HashMap<>();
                hashMM.put(device.getAddress(), device.getName());
                customdeviceMapList.add(hashMM);

                NLog.d(device.getName() + "\n" + device.getAddress());
//                mPairedDevicesArrayAdapter.add(device.getName() +"\n M:"+device.getBluetoothClass().getMajorDeviceClass()+"D:"+device.getBluetoothClass().getDeviceClass());
            }
        }
        else
        {
            String noDevices = getResources().getText(R.string.none_paired).toString();
            NLog.d(noDevices);
        }

        // Indicate scanning in the title
//        setProgressBarIndeterminateVisibility(true);
//        setTitle(R.string.scanning);

        // Turn on sub-title for new devices
//        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

//        mNewDevicesArrayAdapter.clear();

        if (le) // scan btle
        {
            if (Build.VERSION.SDK_INT < 21) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle( "Not Supported BLE under 21" );
                builder.setMessage( "Android SDK [" + Build.VERSION.SDK_INT + "] does not support BLE in SDK" );
                builder.setPositiveButton( "OK", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick ( DialogInterface dialog, int which )
                    {
                        finish();
                    }
                } );
                builder.setCancelable( false );
                builder.create().show();
                return;
            } else {
                mLeScanner.startScan(mScanFilters, mScanSetting, mScanCallback);
            }
        }
        else    // scan bt
        {
            // If we're already discovering, stop it
            if (mBtAdapter.isDiscovering())
            {
                mBtAdapter.cancelDiscovery();
            }

//        mBtAdapter.startLeScan( callback );
            mBtAdapter.startDiscovery();
            NLog.d("mBtAdapter.startDiscovery()");
        }


    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            if ( device != null )
            {
                String sppAddress = UuidUtil.changeAddressFromLeToSpp(result.getScanRecord().getBytes());
                String msg = device.getName() + "\n" +"[RSSI : " + result.getRssi() + "dBm]" + sppAddress;
                NLog.d( "onLeScan " + msg );
                /**
                 * have to change adapter to BLE
                 */
                if( !deviceMap.containsKey( sppAddress ) )
                {
                    NLog.d( "ACTION_FOUND onLeScan : " + device.getName() + " sppAddress : " + sppAddress + ", COD:" + device.getBluetoothClass() );

                    PenClientCtrl.getInstance( MainActivity.this ).setLeMode( true );
                    if( PenClientCtrl.getInstance( MainActivity.this ).isAvailableDevice( result.getScanRecord().getBytes() ) )
                    {
                        MainActivity.DeviceInfo info = new MainActivity.DeviceInfo();
                        info.sppAddress = sppAddress;
                        info.leAddress = device.getAddress();
                        info.deviceName = device.getName();
                        info.isLe = is_le_scan;
                        info.uuidVer = BTLEAdt.UUID_VER.VER_2.toString();
                        info.colorCode = -1;

                        List<ParcelUuid> parcelUuids = result.getScanRecord().getServiceUuids();
                        for(ParcelUuid uuid:parcelUuids)
                        {
                            if( uuid.toString().equals(Const.ServiceUuidV5.toString()))
                            {
                                info.uuidVer = BTLEAdt.UUID_VER.VER_5.toString();
                                info.colorCode = UuidUtil.getColorCodeFromUUID(result.getScanRecord().getBytes());
                                info.companyCode = UuidUtil.getCompanyCodeFromUUID(result.getScanRecord().getBytes());
                                info.productCode = UuidUtil.getProductCodeFromUUID(result.getScanRecord().getBytes());
                                break;
                            }
                            else if(uuid.toString().equals(Const.ServiceUuidV2.toString()))
                            {
                                info.uuidVer = BTLEAdt.UUID_VER.VER_2.toString();
                                info.colorCode = UuidUtil.getColorCodeFromUUID(result.getScanRecord().getBytes());
                                info.companyCode = UuidUtil.getCompanyCodeFromUUID(result.getScanRecord().getBytes());
                                info.productCode = UuidUtil.getProductCodeFromUUID(result.getScanRecord().getBytes());
                                break;

                            }

                        }
                        NLog.d( "ACTION_FOUND onLeScan : " + device.getName() + " sppAddress : " + sppAddress + ", COD:" + device.getBluetoothClass()+", colorCode="+info.colorCode+", productCode="+info.productCode+", companyCode="+info.companyCode  );

                        deviceMap.put( sppAddress, info );
                        customdeviceMap.put(device.getAddress(), device.getName());
                        HashMap<String, String> hashMM = new HashMap<>();
                        hashMM.put(device.getAddress(), device.getName());
                        customdeviceMapList.add(hashMM);
//                        mNewDevicesArrayAdapter.add( msg );
                        NLog.d( msg );
                    }
                }
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            for ( ScanResult scanResult : results ) {
                NLog.d("ScanResult - Results", scanResult.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            NLog.d("Scan Failed", "Error Code : " + errorCode);
        }
    };


    class MyCustomListener implements  EventChannel.StreamHandler{
        private final Handler handler = new Handler();
        String mycustomMessage = "default";
        private final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (count > 10) {
                    eventSink.endOfStream();
                } else {
                    eventSink.success("Hello " + count + "/10");
                }
                count++;
                handler.postDelayed(this, 1000);
            }
        };

        private final Runnable myCustomRunnable = new Runnable() {
            @Override
            public void run() {
                eventSink.success("myCustomRunnable");
                handler.post(this);
            }
        };

        public void sendSomeData(String message){
            myCustomRunnable.run();
        }

        private EventChannel.EventSink eventSink;
        private int count = 1;

        @Override
        public void onListen(Object o, final EventChannel.EventSink eventSink) {
            this.eventSink = eventSink;
//            runnable.run();
//            myCustomRunnable.run();
        }

        @Override
        public void onCancel(Object o) {
            handler.removeCallbacks(runnable);
        }

    }

    class DeviceInfo
    {
        String sppAddress = "";
        String leAddress = "";
        String deviceName = "";
        boolean isLe = false;
        String uuidVer = "";
        int colorCode = 0;
        int productCode = 0;
        int companyCode = 0;

    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action))
            {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                // Get rssi value
                short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI,  Short.MIN_VALUE);

                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED)
                {
                    MainActivity.DeviceInfo info = new MainActivity.DeviceInfo();
                    info.sppAddress = device.getAddress();
                    info.leAddress = "";
                    info.deviceName = device.getName();
                    info.isLe = is_le_scan;
                    info.uuidVer = BTLEAdt.UUID_VER.VER_2.toString();
                    info.colorCode = -1;


                    NLog.d( "ACTION_FOUND SPP : " +device.getName() + " address : "+ device.getAddress()+", COD:" + device.getBluetoothClass());

                    deviceMap.put( device.getAddress(), info );
                    customdeviceMap.put(device.getAddress(), device.getName());
                    HashMap<String, String> hashMM = new HashMap<>();
                    hashMM.put(device.getAddress(), device.getName());
                    customdeviceMapList.add(hashMM);
                    NLog.d(device.getName() + "\n" + "[RSSI : "+rssi +"dBm] " + device.getAddress());
//                    mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress()+"\n Major"+device.getBluetoothClass().toString()+"\nDeviceClass()"+device.getBluetoothClass().getDeviceClass()+"device="+device.getType());

                }
                // When discovery is finished, change the Activity title
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
            {
                NLog.d("ACTION_DISCOVERY_FINISHED");
                setProgressBarIndeterminateVisibility(false);
                setTitle(R.string.select_device);


            }
        }
    };
}