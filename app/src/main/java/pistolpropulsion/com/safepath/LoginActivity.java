package pistolpropulsion.com.safepath;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import static android.content.ContentValues.TAG;
/**
 * Created by Abby on 10/20/18.
 */
public class LoginActivity extends AppCompatActivity {

        private FirebaseAuth siAuth;
        private Button login_button;
        private EditText email;
        private EditText password;
        private Button create_acct_button;
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_login);
            siAuth = FirebaseAuth.getInstance();
            email = findViewById(R.id.EmailInput);
            password = findViewById(R.id.PasswordInput);
            login_button = findViewById(R.id.LoginButton);
            create_acct_button = findViewById(R.id.CreateAcct);

            login_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    signIn(email.getText().toString(), password.getText().toString());
                }
            });

            create_acct_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    createAcct();
                }
            });
        }

        public void signIn(String email, String password) {
            siAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithEmail:success");
                        FirebaseUser user = siAuth.getCurrentUser();
                        updateSignIn(user);
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        Toast.makeText(LoginActivity.this, "Authentication failed.",
                                Toast.LENGTH_SHORT).show();
                        updateSignIn(null);
                    }
                }
            });
        }

        public void createAcct() {
            Intent createintent = new Intent(LoginActivity.this, CreateAccountActivity.class);
            startActivity(createintent);
        }

        @Override
        public void onStart() {
            super.onStart();
            // Check if user is signed in (non-null) and update UI accordingly.
            FirebaseUser currentUser = siAuth.getCurrentUser();
            updateSignIn(currentUser);
        }
        public void updateSignIn(FirebaseUser user) {
            if (user != null) {
                Intent signintent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(signintent);
            }
        }
}
