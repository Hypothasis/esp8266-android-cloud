#include <ESP8266WiFi.h>
#include <PubSubClient.h>
#include <time.h>
#include <TZ.h>
#include <FS.h>
#include <LittleFS.h>
#include <CertStoreBearSSL.h>
#include <ArduinoJson.h>

// Delcaração dee funções globais
void setup_wifi();
void setDateTime();
void callback(char* topic, byte* payload, unsigned int length);
void reconnect(const char* msg);

// Variaveis de configuração de WIFI e MQTT Broker
const char* ssid = "";
const char* password = "";
const char* mqtt_server = "";
const char* hive_mqttt_username = "";
const char* hive_mqtt_password = "";
const char* topico_atual = "";

//json para receber arquivos
StaticJsonDocument<200> json;

BearSSL::CertStore certStore;

WiFiClientSecure espClient;
PubSubClient * client;

unsigned long lastMsg = 0;
#define MSG_BUFFER_SIZE (500)
char msg[MSG_BUFFER_SIZE];

int value = 0;

const int SENSOR = A0;                                      // A0 corresponde ao pino GPIO1
const int LED = 16;                                         // D0 corresponde ao pino GPIO16
int LedState = LOW;

void setup() {
  Serial.begin(9600);

  LittleFS.begin();
  setup_wifi();
  setDateTime();

  pinMode(LED_BUILTIN, OUTPUT); // Initialize the LED_BUILTIN pin as an output
  pinMode(LED, OUTPUT);

  int numCerts = certStore.initCertStore(LittleFS, PSTR("/certs.idx"), PSTR("/certs.ar"));
  Serial.printf("Number of CA certs read: %d\n", numCerts);
  if (numCerts == 0) {
    Serial.printf("No certs found. Did you run certs-from-mozilla.py and upload the LittleFS directory before running?\n");
    return; // Can't connect to anything w/o certs!
  }

  BearSSL::WiFiClientSecure *bear = new BearSSL::WiFiClientSecure();
  // Integrate the cert store with this connection
  bear->setCertStore(&certStore);

  client = new PubSubClient(*bear);

  client->setServer(mqtt_server, 8883);
  client->setCallback(callback);
}


void loop() {
  if (!client->connected()) {
    reconnect("esp8266 ligado ao cloud Hivemq");
  }
  client->loop();

  unsigned long now = millis();
  if (now - lastMsg > 2500) { // 2500 milissegundos = 2,5 segundos
    lastMsg = now;

    // Lê o valor do pino A0 (o valor será entre 0 e 1023)
    int valorLido = analogRead(SENSOR);

    // Cria o documento JSON para enviar
    StaticJsonDocument<200> doc_envio;
    doc_envio["device"] = "ESP8266";                        // Device
    doc_envio["uptime_ms"] = now;                           // Tempo ativo em MS
    doc_envio["Sensor"] = valorLido;                        // Envia dados do Sensor
    doc_envio["Led"] = LedState;                            // Modifica Estado do LED
    // O LED_BUILTIN é ativo em LOW, então invertemos a lógica de leitura
    doc_envio["LED_BUILTIN"] = (digitalRead(LED_BUILTIN) == LOW) ? 1 : 0;

    memset(msg, 0, MSG_BUFFER_SIZE);                        // Limpa o buffer antes da Serilização
    serializeJson(doc_envio, msg, MSG_BUFFER_SIZE);         // Serializar o JSON para o buffer 'msg   
    
    // A função publish envia o conteúdo do buffer 'msg' para o tópico 'topico_atual'
    bool published = client->publish(topico_atual, msg);
    
  }

}

/* ----------- Métodos de Configuração -----------*/

void setup_wifi() {
  // Começamos a conexão do WIFI
  Serial.print("Connecting to ");
  Serial.println(ssid);

  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  randomSeed(micros());

  Serial.println("");
  Serial.println("WiFi connected");
  Serial.println("IP address: ");
  Serial.println(WiFi.localIP());
}


void setDateTime() {
  // You can use your own timezone, but the exact time is not used at all.
  // Only the date is needed for validating the certificates.
  configTime(TZ_America_Sao_Paulo, "a.ntp.br", "b.ntp.br");

  Serial.print("Waiting for NTP time sync: ");
  time_t now = time(nullptr);
  while (now < 8 * 3600 * 2) {
    delay(100);
    Serial.print(".");
    now = time(nullptr);
  }
  Serial.println();

  struct tm timeinfo;
  gmtime_r(&now, &timeinfo);
  Serial.printf("%s %s", tzname[0], asctime(&timeinfo));
}


/* --------- Método para receber Mensagem -------*/

void callback(char* topic, byte* payload, unsigned int length) {
  Serial.print("Message arrived [");
  Serial.print(topic);
  Serial.print("] ");
  for (int i = 0; i < length; i++) {
    Serial.print((char)payload[i]);
  }
  Serial.println();
  
  // Limpe o documento JSON antes de fazer o parsing
  json.clear();

  // Fazer o parsing do JSON a partir do payload
  DeserializationError error = deserializeJson(json, payload, length);
  if (error) {
    Serial.print("Erro ao fazer o parsing do JSON: ");
    Serial.println(error.c_str());
    return;
  }

  // Serializar o JSON para uma string
  String jsonString;
  serializeJson(json, jsonString);
  
  if (json.containsKey("Led")) {
    
    int INPUT_LED = json["Led"];                            // Pega o valor (que esperamos ser 0 ou 1)

    if (INPUT_LED == 1) {
      LedState = HIGH;                                      // Atualiza a variável
    } 
    else if (INPUT_LED == 0) {
      LedState = LOW;                                       // Atualiza a variável
    }

    // Aplica o estado (HIGH ou LOW) da variável no pino do LED
    digitalWrite(LED, LedState); 
  }

  if(json.containsKey("LED_BUILTIN")){
    int INPUT_LED_BUILTIN = json["LED_BUILTIN"];             // Pega o valor (que esperamos ser 0 ou 1)

    // Como o LED é ativo em LOW, invertemos a lógica
    if (INPUT_LED_BUILTIN == 1) {
      digitalWrite(LED_BUILTIN, LOW);   // Liga o LED
    } 
    else if (INPUT_LED_BUILTIN == 0) {
      digitalWrite(LED_BUILTIN, HIGH);  // Desliga o LED
    }
  }

}


/* ------------- Método de reconexão -------------*/

void reconnect(const char* msg) {
  // Permanece no loop até conectar
  while (!client->connected()) {
    Serial.print("Attempting MQTT connection…");
    String clientId = "ESP8266Client - MyClient";
    if (client->connect(clientId.c_str(), hive_mqttt_username, hive_mqtt_password)) {
      Serial.println("");
      Serial.println("connected");
      // Uma vez conectado publica no topico atual
      client->publish(topico_atual, msg);
      // … and resubscribe
      client->subscribe(topico_atual);
    } else {
      // Se falhar conexão tenta denovo em 5 segundos
      Serial.print("failed, rc = ");
      Serial.print(client->state());
      Serial.println(" try again in 5 seconds");
      delay(5000);
    }
  }
}

