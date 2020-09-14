package hackathon.co.kr.neopen.temp;

import android.app.Application;
import android.util.Log;

public class SampleApplication extends Application{
	
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		PenClientCtrl.getInstance( getApdiscoverDevicesplicationContext() );
		PenClientCtrl.getInstance( getApplicationContext() ).setContext(getApplicationContext());
		PenClientCtrl.getInstance( getApplicationContext() ).registerBroadcastBTDuplicate();

	}
	
	public void onTerminate() {
		super.onTerminate();
		PenClientCtrl.getInstance( getApplicationContext() ).unregisterBroadcastBTDuplicate();
	};

}
