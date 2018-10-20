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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import static android.content.ContentValues.TAG;

/**
 * Created by Abby on 10/20/18.
 * Written by Johanna 10/20/18
 */

public class CreateAccountActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private Button createaccount_button;
    private EditText email;
    private EditText password;
    private DatabaseReference mDatabase;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_createaccount);
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        email = findViewById(R.id.EmailInput);
        password = findViewById(R.id.PasswordInput);
        createaccount_button = findViewById(R.id.createaccount);

        createaccount_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createAccount(email.getText().toString(), password.getText().toString());
            }
        });
    }

    public void onStart() {
        super.onStart();
        // check to see if user is signed in and (non-null) and update
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);


    }

    private void updateUI(FirebaseUser currentUser) {
        if(currentUser != null) {
            Intent signup = new Intent(CreateAccountActivity.this, MainActivity.class);
            startActivity(signup);
        }
    }

    public void createAccount(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this,
                new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            Toast.makeText(CreateAccountActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                            updateUI(null);
                        }

                        // ...
                    }
                });

    }
}
