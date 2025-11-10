package com.example.app;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class CreateAccount extends AppCompatActivity {

    private static EditText BrokerUrlUI;
    private static EditText UsernameUI;
    public static EditText PasswordUI;
    private static Button ButtonTest;
    private static Button ButtonConnect;
    private static TestConnectionBroker ThreadTestConnection;

    public static Boolean isConnect = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.create_account_layout);


        //Remove layout da Notificação para com objetos
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Declaração da Variaveis
        BrokerUrlUI = findViewById(R.id.TLS_MQTT_URL);
        UsernameUI = findViewById(R.id.Username);
        PasswordUI = findViewById(R.id.PasswordInput);

        ButtonConnect = findViewById(R.id.ButtonConnect);
        ButtonConnect.setEnabled(false);
        ButtonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("Connection","APP Conectado com o Cluster mesmo");

                // Muda para o layout MainProgram
                Intent intent = new Intent(CreateAccount.this, MainProgram.class);
                startActivity(intent);
                finish();
            }
        });

        ButtonTest = findViewById(R.id.TestConectionButton);
        ButtonTest.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("ResourceAsColor")
            @Override
            public void onClick(View v) {
                String Broker = "ssl://" + BrokerUrlUI.getText().toString();
                String Username = UsernameUI.getText().toString();
                String Password = PasswordUI.getText().toString();

                if(Broker.equals("ssl://") || Username.equals("") || Password.equals("")){

                    Toast.makeText(getApplicationContext(), "Insira todos os dados", Toast.LENGTH_LONG).show();

                } else{

                    ThreadTestConnection = new TestConnectionBroker( Broker, Username, Password, CreateAccount.this);
                    isConnect = ThreadTestConnection.run();

                    if(isConnect){
                        ButtonConnect.setEnabled(true);
                        ButtonConnect.setBackgroundColor(ContextCompat.getColor(CreateAccount.this,R.color.GreenAprove));
                    }
                }
            }
        });

    }

}