package com.example.app;


import android.content.Context;
import android.util.Log;

import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;

public class TestConnectionBroker {

    private static String BrokerUrl;
    private static String Username;
    private static String Password;
    private static Context Context;

    public TestConnectionBroker(String broker, String username, String password, Context context){
        BrokerUrl = broker;
        Username = username;
        Password = password;
        Context = context;
    }

    public boolean run() {

        try {
            // Cria Cliente
            MqttClient Client = new MqttClient(BrokerUrl,Username,new MemoryPersistence());
            MqttConnectionOptions connOpts = new MqttConnectionOptions();

            // Cria Sessão
            connOpts.setCleanStart(true);
            connOpts.setUserName(Username);
            connOpts.setPassword(Password.getBytes());

            // Conecta
            Client.connect(connOpts);

            // Mensagem na Tela
            Thread Message = new Thread(new ToastShow(Context,"Teste de conexão feita com sucesso!"));
            Message.start();
            Message.interrupt();

            // Logcat
            Log.d("Input", "Broker:" +BrokerUrl + " Username: " + Username + " Password: " + Password );

            // Disconecta Cliente
            Client.disconnect();

            // Salva Dados do usuário
            SaveServerData();

            return true; // Retorna positivo para trocar cor botao

        } catch (MqttException e) {
            Thread Message = new Thread(new ToastShow(Context,"Error no Teste de Conexão: "+e.getMessage()));
            Message.start();
            Message.interrupt();

            return false; // Retorna negativo para trocar cor botao
        }
    }


    private void SaveServerData(){
        MyDataBase myDB = new MyDataBase(Context);
        myDB.AddServerData(BrokerUrl, Username, Password);
    }
}
