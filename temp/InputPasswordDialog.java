package hackathon.co.kr.neopen.temp;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import hackathon.co.kr.neopen.R;

public class InputPasswordDialog extends Dialog
{
	private PenActivity parent;

	public Button btnLogin;
	
	public EditText edPass;

	private String penAddress = "";
	
	public InputPasswordDialog( Context context, PenActivity p )
	{
		super( context );
		requestWindowFeature( Window.FEATURE_NO_TITLE );
		getWindow().clearFlags( WindowManager.LayoutParams.FLAG_DIM_BEHIND );
		
		this.parent = p;
		
		setContentView( R.layout.input_password_dialog );
		
		edPass = (EditText) findViewById( R.id.inputPassword );
		
		btnLogin = (Button) findViewById( R.id.btnInputPassword );
		btnLogin.setOnClickListener( new View.OnClickListener()
		{
			@Override
			public void onClick( View v )
			{
					submit();
					cancel();
			}
		} );
	}
	public void show(String penAddress)
	{
		this.penAddress = penAddress;
		super.show();
	}
	
	public void submit()
	{
		parent.inputPassword( penAddress, edPass.getText().toString() );
	}
	
	@Override
	public void cancel()
	{
		super.cancel();
	}
}
