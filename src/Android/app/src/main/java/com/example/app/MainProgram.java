package com.example.app;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainProgram extends AppCompatActivity {

    private Switch switchLed1;
    private View viewLed1;
    private Switch switchLed2;
    private View viewLed2;
    private TextView valorSensor;

    private MqttHandler mqttHandler;
    private String topic = "ESP8266";
    private boolean ignoreSwitchChange = false;

    private Handler uiHandler = new Handler(msg -> {
        switch (msg.what) {
            case MqttHandler.MSG_SENSOR_UPDATE:
                String sensorValue = (String) msg.obj;
                valorSensor.setText(sensorValue);
                break;

            case MqttHandler.MSG_LED_UPDATE:
                boolean led1State = (msg.arg1 == 1);
                ignoreSwitchChange = true;
                switchLed1.setChecked(led1State);
                ignoreSwitchChange = false;
                setLedState(viewLed1, led1State);
                break;

            case MqttHandler.MSG_LED_BUILTIN_UPDATE:
                boolean led2State = (msg.arg1 == 1);
                ignoreSwitchChange = true;
                switchLed2.setChecked(led2State);
                ignoreSwitchChange = false;
                setLedState(viewLed2, led2State);
                break;
        }
        return true;
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.main_program_activity);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Inicializa componentes do layout
        switchLed1 = findViewById(R.id.switch_led_1);
        viewLed1 = findViewById(R.id.view_led_1);
        switchLed2 = findViewById(R.id.switch_led_2);
        viewLed2 = findViewById(R.id.view_led_2);

        valorSensor = findViewById(R.id.ValorSensor);

        // Listener do LED 1
        switchLed1.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (ignoreSwitchChange) return;
            setLedState(viewLed1, isChecked);
            mqttHandler.publish(topic, "{\"Led\":" + (isChecked ? 1 : 0) + "}", 1);
        });

        // Listener do LED 2
        switchLed2.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (ignoreSwitchChange) return;
            setLedState(viewLed2, isChecked);
            mqttHandler.publish(topic, "{\"LED_BUILTIN\":" + (isChecked ? 1 : 0) + "}", 1);
        });

        // 5️⃣ Conecta ao MQTT usando dados do banco
        MyDataBase db = new MyDataBase(this);
        db.GetData(); // Puxa dados do banco e preenche AccessAccount

        String brokerUrl = AccessAccount.URLDatabase;
        String username = AccessAccount.UsernameDatabase;
        String password = AccessAccount.PasswordDataBase;

        Log.d("MQTT", "Conectando ao broker: " + brokerUrl + " com usuário: " + username);

        mqttHandler = new MqttHandler(this, brokerUrl, username, password, uiHandler);
        new Thread(() -> mqttHandler.connectAndSubscribe(topic)).start();
    }

    private void setLedState(View ledView, boolean isOn) {
        int colorResId = isOn ? R.color.colorLedOn : R.color.colorLedOff;
        Drawable drawable = ledView.getBackground();
        if (drawable != null) {
            drawable.setTint(ContextCompat.getColor(this, colorResId));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mqttHandler != null) mqttHandler.disconnect();
    }
}
