package com.example.app;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.TextView;

import java.io.File;

public class AccessAccount extends AppCompatActivity {

    public static String URLDatabase = "";
    public static String UsernameDatabase = "";
    public static String PasswordDataBase = "";

    private static TextView User;
    private static EditText Password;
    private static Button CriarUsuario;
    private static Button Entrar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.access_account_layout);
        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        User = findViewById(R.id.User);
        Password = findViewById(R.id.SenhaAcessoInput);
        CriarUsuario = findViewById(R.id.CriarUsuario);
        Entrar = findViewById(R.id.Entrar);

        CheckDataBase();

        Entrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Password.getText().toString().equals(PasswordDataBase)){
                    Intent intent = new Intent(AccessAccount.this, MainProgram.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(AccessAccount.this,"Senha errada",Toast.LENGTH_LONG).show();
                }
            }
        });

        CriarUsuario.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MyDataBase myDB = new MyDataBase(AccessAccount.this);
                myDB.DelData();
                Intent intent = new Intent(AccessAccount.this, CreateAccount.class);
                startActivity(intent);
                finish();

            }
        });

    }

    private void CheckDataBase() {
        //Implementar uma query, se der errado vai para a outra tela
        ///data/data/com.example.app/databases/MyDataBase.db
        File MyDB = new File("/data/user/0/com.example.app/databases/MyDataBase.db");
        MyDataBase myDB = new MyDataBase(AccessAccount.this);
        Log.d("pwd", getFilesDir().getAbsolutePath());

        if(MyDB.exists()){
            Log.d("DB no tcc exists: ", "simmm");

            myDB.GetData();

            User.setText(UsernameDatabase);
        } else {
            Log.d("DB no tcc exists: ", "naoo");
            Intent intent = new Intent(AccessAccount.this, CreateAccount.class);
            startActivity(intent);
            finish();
        }

        /////////////
        Log.d("Variavel Local", URLDatabase + " " + UsernameDatabase + " " + PasswordDataBase);
        /////////////
    }
}