package com.codeart.onlinecashier.ui;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.codeart.onlinecashier.R;
import com.codeart.onlinecashier.databinding.ActivityLoginBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityLoginBinding binding;

    private FirebaseAuth auth;
    private FirebaseUser firebaseUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawableResource(R.drawable.bg_login);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        auth = FirebaseAuth.getInstance();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        binding.btnLogin.setOnClickListener(this);
        binding.btnRegisterLogin.setOnClickListener(this);

        //permission
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE}, PackageManager.PERMISSION_GRANTED);

        // auto hide keyboard
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_register_login) {
            Intent toRegister = new Intent(this, RegisterActivity.class);
            startActivity(toRegister);
        } else {
            String email = binding.etEmailLogin.getText().toString();
            String password = binding.etPasswordLogin.getText().toString();

            if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password)) {
                if (password.length() < 6) {
                    Toast.makeText(LoginActivity.this,
                            "Password harus lebih dari 6 digit!", Toast.LENGTH_SHORT).show();
                } else {
                    Login(email, password);
                }
            } else {
                Toast.makeText(this, "Tolong isi semua field!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // cek jika sudah pernah login, maka auto masuk ke home page
        if (firebaseUser != null) {
            Intent goHome = new Intent(this, HomeActivity.class);
            startActivity(goHome);
            finish();
        }
    }

    public static boolean isEmailValid(String email) {
        String expression = "^[\\w.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$";
        Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    private void Login(String email, String password) {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Tunggu sebentar ya :)");
        progressDialog.show();
        progressDialog.setCancelable(false);

        if (isEmailValid(email)) {
            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Intent toStart = new Intent(LoginActivity.this, HomeActivity.class);
                            toStart.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(toStart);
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this,
                                    "Email belum terdaftar / password Anda salah", Toast.LENGTH_SHORT).show();
                        }
                        progressDialog.dismiss();
                    });
        } else {
            Toast.makeText(this, "Maaf, Format Email salah!", Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
        }
    }
}