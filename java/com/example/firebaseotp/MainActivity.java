package com.example.firebaseotp;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private EditText etPhone, etOtp;
    private Button btnSendOtp, btnVerifyOtp;
    private TextView tvStatus;

    private FirebaseAuth mAuth;
    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etPhone = findViewById(R.id.etPhone);
        etOtp = findViewById(R.id.etOtp);
        btnSendOtp = findViewById(R.id.btnSendOtp);
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp);
        tvStatus = findViewById(R.id.tvStatus);

        mAuth = FirebaseAuth.getInstance();

        btnSendOtp.setOnClickListener(v -> {
            String phone = etPhone.getText().toString().trim();

            if (TextUtils.isEmpty(phone)) {
                etPhone.setError("Enter phone number");
                return;
            }

            sendVerificationCode(phone);
        });

        btnVerifyOtp.setOnClickListener(v -> {
            String code = etOtp.getText().toString().trim();

            if (TextUtils.isEmpty(code)) {
                etOtp.setError("Enter OTP");
                return;
            }

            if (verificationId == null) {
                Toast.makeText(MainActivity.this, "Please send OTP first", Toast.LENGTH_SHORT).show();
                return;
            }

            verifyCode(code);
        });
    }

    private void sendVerificationCode(String phoneNumber) {
        tvStatus.setText("Sending OTP...");

        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(phoneNumber)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(this)
                        .setCallbacks(mCallbacks)
                        .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                @Override
                public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                    String code = credential.getSmsCode();

                    if (code != null) {
                        etOtp.setText(code);
                        verifyCode(code);
                    } else {
                        signInWithCredential(credential);
                    }
                }

                @Override
                public void onVerificationFailed(@NonNull FirebaseException e) {
                    tvStatus.setText("Verification failed");
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                }

                @Override
                public void onCodeSent(@NonNull String s,
                                       @NonNull PhoneAuthProvider.ForceResendingToken token) {
                    super.onCodeSent(s, token);
                    verificationId = s;
                    resendToken = token;
                    tvStatus.setText("OTP sent");
                    Toast.makeText(MainActivity.this, "OTP Sent Successfully", Toast.LENGTH_SHORT).show();
                }
            };

    private void verifyCode(String code) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithCredential(credential);
    }

    private void signInWithCredential(PhoneAuthCredential credential) {
        tvStatus.setText("Verifying OTP...");

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        AuthResult result = task.getResult();
                        String phone = result.getUser() != null ? result.getUser().getPhoneNumber() : "User";
                        tvStatus.setText("Login Successful: " + phone);
                        Toast.makeText(MainActivity.this, "Authentication Successful", Toast.LENGTH_LONG).show();
                    } else {
                        tvStatus.setText("Invalid OTP");
                        Toast.makeText(MainActivity.this, "Verification Failed", Toast.LENGTH_LONG).show();
                    }
                });
    }
}