package com.haxotech.glinic_manager



import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_FINISHED
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.NonNull
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugins.GeneratedPluginRegistrant
import kr.neolab.sdk.ink.structure.Dot
import kr.neolab.sdk.pen.PenCtrl
import kr.neolab.sdk.pen.penmsg.*
import kr.neolab.sdk.util.NLog
import kr.neolab.sdk.util.UuidUtil.changeAddressFromLeToSpp
import org.json.JSONException
import org.json.JSONObject
import java.util.*


class MainActivity_ : FlutterActivity()  {
    var iPenCtrl: PenCtrl? = null
    var USING_SECTION_ID = 3
    var USING_OWNER_ID = 27
    private val mNewDevicesArrayAdapter: ArrayAdapter<String>? = null
    private val devices_list : ArrayList<BluetoothDevice> = ArrayList()
    private var mBtAdapter: BluetoothAdapter? = null
    private val mLeScanner: BluetoothLeScanner? = null
    private val mScanFilters: List<ScanFilter>? = null
    private val mScanSetting: ScanSettings? = null

    var temp = HashMap<String, String>()

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        GeneratedPluginRegistrant.registerWith(flutterEngine);

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
                .setMethodCallHandler { call , result ->
                    if (call.method == "getData") {

                        result.success("called");
                    }
                    else if (call.method == "getDevices"){

                        result.success(devices_list);
                    }
                    else {
                        result.notImplemented()
                    }

                }

    }

    private fun getData(): String? {
        return iPenCtrl?.isAvailableDevice("9c7bd251e4be").toString()
    }

    companion object {
        private const val CHANNEL = "MyNativeChannel"
    }

}
