package pistolpropulsion.com.safepath;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupWindow;

public class AlertActivity extends AppCompatActivity {

    private Button imok;
    private EditText password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert);
        imok = findViewById(R.id.Confirmbutton);
        password = findViewById(R.id.Password);


        showPopup();
    }

    private PopupWindow pw;
    private void showPopup() {
        try {
// We need to get the instance of the LayoutInflater
            LayoutInflater inflater = (LayoutInflater) AlertActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View layout = inflater.inflate(R.layout.activity_alert,
                    (ViewGroup) findViewById(R.id.Alertpopup));
            pw = new PopupWindow(layout, 300, 370, true);
            pw.showAtLocation(layout, Gravity.CENTER, 0, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
