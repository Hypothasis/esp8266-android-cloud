package com.example.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

/* Essa Classe é o DataBase do Usuário, que ficam as informações do Servidor HiveMQ Cloud
*  MyDataBase() inicializa a nossa classe
*  onCreate(), vamos criar nossa DataBase.db
*  onUpgrade(), mudamos o valor, o Update
*/

public class MyDataBase extends SQLiteOpenHelper {
    private static Context Context;
    private static final String DATABASE_NAME = "MyDataBase.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "MyDataHiveMQ";
    private static final String COLUMN_id = "id";
    private static final String COLUMN_TLS_URL = "URL";
    private static final String COLUMN_Username = "Usernames";
    private static final String COLUMN_Password = "Passwords";
    private static ToastShow Message;

    public MyDataBase(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        Context = context;
    }


    /* * * * * * * * * * * * * * *

        CREATE TABLE DataBase (
         id INTEGER PRIMARY KEY AUTO_INCREMENT,
         TLS_URL TEXT NOT NULL,
         Username TEXT NOT NULL,
         Password TEXT NOT NULL
        );

    * * * * * * * * * * * * * * * */
    @Override
    public void onCreate(SQLiteDatabase db) {
        String query = "CREATE TABLE " + TABLE_NAME + "(" +
                            COLUMN_id +" INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            COLUMN_TLS_URL + " TEXT NOT NULL, " +
                            COLUMN_Username + " TEXT NOT NULL, " +
                            COLUMN_Password + " TEXT NOT NULL);";
        db.execSQL(query);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String query = "DROP TABLE IF EXISTS " + TABLE_NAME;
        db.execSQL(query);
        onCreate(db);
    }

    void AddServerData(String TLS_URL,String Username, String Password){

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put(COLUMN_TLS_URL, TLS_URL);
        cv.put(COLUMN_Username,Username);
        cv.put(COLUMN_Password, Password);

        long result = db.insert(TABLE_NAME, null, cv);
        if(result == -1){
            Thread Message = new Thread(new ToastShow(Context,"Erros ao salvar os dados"));
            Message.start();
            Message.interrupt();
        } else{
            Thread Message = new Thread(new ToastShow(Context,"Dados salvos!"));
            Message.start();
            Message.interrupt();
        }
    }

    void GetData(){
        SQLiteDatabase db = this.getReadableDatabase();

        // Obtém todos os dados do servidor
        String query = "SELECT * FROM " + TABLE_NAME;
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            // Suponha que sua tabela tenha uma coluna chamada "id", "URL", "Username" e "Password"
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_id));
            String tlsUrl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TLS_URL));
            String username = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_Username));
            String password = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_Password));

            AccessAccount.URLDatabase = tlsUrl;
            AccessAccount.UsernameDatabase = username;
            AccessAccount.PasswordDataBase = password;

            // Faça o que for necessário com os dados, por exemplo, exibir no log
            Log.d("DB", "ID: " + id + ", URL: " + tlsUrl + ", Username: " + username + ", Password: " + password);
        }

        // Não se esqueça de fechar o cursor para liberar recursos
        cursor.close();
    }

    void DelData(){
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "DELETE FROM " + TABLE_NAME;
        db.execSQL(query);

    }

}
