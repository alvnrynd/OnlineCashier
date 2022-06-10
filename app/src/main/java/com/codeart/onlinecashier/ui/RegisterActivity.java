package com.codeart.onlinecashier.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.codeart.onlinecashier.R;
import com.codeart.onlinecashier.databinding.ActivityRegisterBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityRegisterBinding binding;

    private FirebaseAuth auth;
    private DatabaseReference reference;
    private ProgressDialog progressDialog;

    private String dateString, monthString, yearString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawableResource(R.drawable.bg_register);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        auth = FirebaseAuth.getInstance();
        reference = FirebaseDatabase.getInstance().getReference("Users");

        Date date = new Date();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            int year = localDate.getYear();
            int month = localDate.getMonthValue();
            int day = localDate.getDayOfMonth();

            // convert to string
            dateString = String.valueOf(day);
            monthString = String.valueOf(month);
            yearString = String.valueOf(year);
        }

        progressDialog = new ProgressDialog(this);

        binding.btnRegister.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_register) {
            String firstName = binding.etFirstNameRegister.getText().toString();
            String lastName = binding.etLastNameRegister.getText().toString();
            String email = binding.etEmailRegister.getText().toString();
            String password = binding.etPasswordRegister.getText().toString();

            if (!TextUtils.isEmpty(firstName) && !TextUtils.isEmpty(lastName) &&
                    !TextUtils.isEmpty(email) && !TextUtils.isEmpty(password)) {
                if (password.length() < 6) {
                    Toast.makeText(this, "Password harus lebih dari 6 digit!", Toast.LENGTH_SHORT).show();
                } else {
                    Registry(firstName, lastName, email, password);
                }
            } else {
                Toast.makeText(this, "Tolong isi semua field!", Toast.LENGTH_SHORT).show();
            }

            hideSoftKeyboard();
        }
    }

    public static boolean isEmailValid(String email) {
        String expression = "^[\\w.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$";
        Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    private void hideSoftKeyboard() {
        InputMethodManager imm = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            imm = (InputMethodManager) this.getSystemService(Activity.INPUT_METHOD_SERVICE);
        }
        View view = this.getCurrentFocus();
        if (view == null) {
            view = new View(this);
        }
        assert imm != null;
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void Registry(String firstName, String lastName, String email, String password) {
        progressDialog.setMessage("Tunggu sebentar ya..");
        progressDialog.show();
        progressDialog.setCancelable(false);

        String currentDate = dateString + "-" + monthString + "-" + yearString;

        if (isEmailValid(email)) {
            auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser firebaseUser = auth.getCurrentUser();
                    assert firebaseUser != null;
                    String userId = firebaseUser.getUid();

                    HashMap<String, String> hashMap = new HashMap<>();
                    hashMap.put("date", currentDate);
                    hashMap.put("firstName", firstName);
                    hashMap.put("lastName", lastName);
                    hashMap.put("email", email);
                    hashMap.put("storeName", "");
                    hashMap.put("storeId", "null");
                    hashMap.put("productId", "null");
                    hashMap.put("historyId", "null");
                    hashMap.put("salesId", "null");
                    hashMap.put("invoiceNumber", "null");
                    hashMap.put("noHpOwner", "");
                    hashMap.put("noRekOwner", "");
                    hashMap.put("address", "");
                    hashMap.put("bankName", "");

                    reference.child(userId).setValue(hashMap).addOnCompleteListener(task1 ->
                            Toast.makeText(RegisterActivity.this,
                                    "Registrasi berhasil, silahkan login", Toast.LENGTH_SHORT).show());

                    FirebaseAuth.getInstance().signOut();
                    progressDialog.dismiss();

                    Intent toLogin = new Intent(RegisterActivity.this, LoginActivity.class);
                    startActivity(toLogin);
                    finish();
                }
            }).addOnFailureListener(e -> hideProgressDialog(e.getMessage()));
        } else {
            hideProgressDialog("Format Email Salah!");
        }
    }

    private void hideProgressDialog(String msg) {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
        Toast.makeText(RegisterActivity.this, msg, Toast.LENGTH_SHORT).show();
    }
}