package pistolpropulsion.com.safepath;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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
    private EditText name;
    private EditText number;
    private EditText pincode;

    private DatabaseReference mDatabase;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_createaccount);
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        email = findViewById(R.id.EmailInput);
        password = findViewById(R.id.PasswordInput);

        name = findViewById(R.id.NameInput);
        number = findViewById(R.id.PhoneInput);
        pincode = findViewById(R.id.PincodeInput);

        createaccount_button = findViewById(R.id.createaccount);

        createaccount_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createAccount();
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

    public void createAccount() {
        mAuth.createUserWithEmailAndPassword(email.getText().toString(), password.getText().toString()).addOnCompleteListener(this,
                new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            User userObject = new User(
                                    email.getText().toString(),
                                    password.getText().toString(),
                                    name.getText().toString(),
                                    number.getText().toString(),
                                    pincode.getText().toString());

                            mDatabase.child("users").child(
                                    (user != null) ? user.getUid() : null).
                                    setValue(userObject);
                            sendSignUpMessage();
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
    public void sendSignUpMessage() {
        final SmsManager smsManager = SmsManager.getDefault();
        String uid = mAuth.getCurrentUser().getUid(); // gets the user ID
        DatabaseReference userRef = mDatabase.child("users").child((mAuth.getCurrentUser() != null) ? uid : null);
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                UserFirebase fetchedUser = dataSnapshot.getValue(UserFirebase.class);
                String contact = "+1" + fetchedUser.getContact();
                String name = fetchedUser.getName();
                String email = fetchedUser.getEmail();
//                    status.setText(contact);
                smsManager.sendTextMessage(contact, null, name + " has added you as their emergency contact on SafePath. Their email is: " + email + ". To learn more, go to https://github.com/CallmeJoeBob/SafePath", null, null);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }
}
