package com.example.app;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.UUID; // <-- IMPORTANTE: Importar UUID


public class MqttHandler {

    private static final String TAG = "MqttHandler";
    public static final int MSG_SENSOR_UPDATE = 1;
    // Adicione os outros códigos de mensagem
    public static final int MSG_LED_UPDATE = 2;
    public static final int MSG_LED_BUILTIN_UPDATE = 3;


    private MqttClient client;
    private String brokerUrl;
    private String username;
    private String password;
    private Handler uiHandler;
    private String topicToSubscribe; // Guardar o tópico para reinscrever
    private MqttConnectionOptions connOpts; // Opções de conexão

    public MqttHandler(Context context, String brokerUrl, String username, String password, Handler uiHandler) {
        this.brokerUrl = brokerUrl;
        this.username = username;
        this.password = password;
        this.uiHandler = uiHandler;
    }

    public void connectAndSubscribe(String topic) {
        this.topicToSubscribe = topic; // Salva o tópico
        Log.d(TAG, "Broker: " + brokerUrl + " | Username: " + username);

        // ****** ClientID ÚNICO ******
        // O ClientID NÃO PODE ser o username. Deve ser único por dispositivo.
        String clientId = username + "-" + UUID.randomUUID().toString().substring(0, 8);
        Log.d(TAG, "Usando ClientID: " + clientId);

        try {
            // Usa o clientId único
            client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
            connOpts = new MqttConnectionOptions(); // Inicializa as opções

            // Cria Sessão
            connOpts.setCleanStart(true);
            connOpts.setUserName(username);
            connOpts.setPassword(password.getBytes());
            connOpts.setAutomaticReconnect(true); // Tentar reconectar automaticamente

            // Define callback
            client.setCallback(new MqttCallback() {

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
                    Log.d(TAG, "Mensagem recebida [" + topic + "]: " + payload);

                    try {
                        JSONObject json = new JSONObject(payload);
                        // Sensor
                        if (json.has("Sensor")) {
                            int sensorValue = json.getInt("Sensor");
                            // Use obtainMessage(what, obj) para enviar Strings
                            Message msg = uiHandler.obtainMessage(MSG_SENSOR_UPDATE, String.valueOf(sensorValue));
                            uiHandler.sendMessage(msg);
                        }

                        // LED principal
                        if (json.has("Led")) {
                            int ledState = json.getInt("Led");
                            // Use obtainMessage(what, arg1, arg2) para enviar Ints
                            Message msg = uiHandler.obtainMessage(MSG_LED_UPDATE, ledState, 0);
                            uiHandler.sendMessage(msg);
                        }

                        // LED_BUILTIN
                        if (json.has("LED_BUILTIN")) {
                            int builtInState = json.getInt("LED_BUILTIN");
                            Message msg = uiHandler.obtainMessage(MSG_LED_BUILTIN_UPDATE, builtInState, 0);
                            uiHandler.sendMessage(msg);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Erro ao parsear JSON: " + payload, e);
                    }
                }

                @Override
                public void deliveryComplete(IMqttToken token) {}

                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    Log.d(TAG, (reconnect ? "Re" : "") + "Conectado a: " + serverURI);

                    // ****** MOVER INSCRIÇÃO PARA CÁ ******
                    // Devemos nos inscrever SEMPRE que conectar (ou reconectar),
                    // pois o CleanStart(true) limpa as inscrições.
                    try {
                        if (topicToSubscribe != null) {
                            client.subscribe(topicToSubscribe, 1);
                            Log.d(TAG, (reconnect ? "Reinscrito" : "Inscrito") + " no tópico: " + topicToSubscribe);
                        }
                    } catch (MqttException e) {
                        Log.e(TAG, "Erro ao se " + (reconnect ? "reinscrever" : "inscrever"), e);
                    }
                }

                @Override
                public void disconnected(MqttDisconnectResponse disconnectResponse) {
                    // Log com mais detalhes
                    String cause = (disconnectResponse.getException() != null) ? disconnectResponse.getException().getMessage() : "sem causa";
                    Log.w(TAG, "Desconectado: " + disconnectResponse.getReasonString() + " | Causa: " + cause);
                }

                @Override
                public void mqttErrorOccurred(MqttException exception) {
                    Log.e(TAG, "Erro MQTT: " + exception.getMessage(), exception);
                }

                @Override
                public void authPacketArrived(int reasonCode, MqttProperties properties) {}
            });

            // Conecta
            Log.d(TAG, "Conectando ao broker...");
            client.connect(connOpts); // Esta é uma operação de rede (bloqueante)

            // ****** REMOVER INSCRIÇÃO DAQUI ******
            // A inscrição foi movida para o 'connectComplete'

        } catch (MqttException e) {
            Log.e(TAG, "Erro ao conectar (MqttException)", e);
        } catch (Exception e) {
            Log.e(TAG, "Erro geral ao conectar", e);
        }
    }


    public void publish(String topic, String payload, int qos) {
        if (client == null || !client.isConnected()) {
            Log.e(TAG, "Cliente MQTT não está conectado. Não é possível publicar.");
            return;
        }

        // ****** EXECUTAR PUBLISH EM UMA THREAD SEPARADA ******
        // Assim como a conexão, publicar também é uma operação de rede.
        new Thread(() -> {
            try {
                MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
                message.setQos(qos);
                client.publish(topic, message);
                Log.d(TAG, "Mensagem publicada no tópico [" + topic + "]: " + payload);
            } catch (MqttException e) {
                Log.e(TAG, "Erro ao publicar mensagem no tópico: " + topic, e);
            }
        }).start();
    }


    public void disconnect() {
        if (client != null && client.isConnected()) {
            try {
                // Para garantir que não vai tentar reconectar
                if (connOpts != null) {
                    connOpts.setAutomaticReconnect(false);
                }
                client.disconnect(3000); // 3 segundos de timeout
                Log.d(TAG, "Desconectado.");
            } catch (MqttException e) {
                Log.e(TAG, "Erro ao desconectar", e);
            }
        }
    }

    public boolean isConnected() {
        return client != null && client.isConnected();
    }
}