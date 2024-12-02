package com.example.proyectofirebase;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Firebase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    //Variables MQTT
    private static String mqttHost = "tcp://leadedge987.cloud.shiftr.io:1883";
    private static String IdUsuario = "AppAndroid";

    private static String Topico = "Mensaje";
    private static String User = "leadedge987";
    private static String Pass = "M3bKwGbtA5j4j6tz";

    //Datos del proyecto
    private EditText txtCodigo, txtNombre, txtAutor, txtNacionalidad;
    private ListView lista;
    private Spinner spCategoria;
    private Button btnEnvio;

    private FirebaseFirestore db;

    //Datos Spinner
    String[] Categorias = {"Terror", "Romance", "Misterio", "Educativo"};

    //Mqtt
    private MqttClient mqttClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CargarListaFirestore();
        db = FirebaseFirestore.getInstance();

        txtCodigo = findViewById(R.id.txtCodigo);
        txtNombre = findViewById(R.id.txtNombre);
        txtAutor = findViewById(R.id.txtAutor);
        txtNacionalidad = findViewById(R.id.txtNacionalidad);
        spCategoria = findViewById(R.id.spCategoria);
        lista = findViewById(R.id.lista);

        btnEnvio = findViewById(R.id.btnEnviar);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Categorias);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
        spCategoria.setAdapter(adapter);

        try {
            mqttClient = new MqttClient(mqttHost, IdUsuario, null);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(User);
            options.setPassword(Pass.toCharArray());
            mqttClient.connect(options);
            Toast.makeText(this, "Aplication conectada al Servidor MQTT", Toast.LENGTH_SHORT).show();
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.d("MQTT", "Conexión perdida");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    String payload = new String(message.getPayload());
                    runOnUiThread(() -> txtNombre.setText(payload));
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    Log.d("MQTT", "Entrega completa");
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }

        btnEnvio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String mensaje = txtNombre.getText().toString();
                try {
                    if (mqttClient != null && mqttClient.isConnected()) {
                        mqttClient.publish(Topico, mensaje.getBytes(), 0, false);
                        txtNombre.append("\n - " + mensaje);
                    } else {
                        Toast.makeText(MainActivity.this, "Mensaje enviado", Toast.LENGTH_SHORT).show();
                    }
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        });


    }

    public void enviarDatosFirestore(View view){

        String codigo = txtCodigo.getText().toString();
        String nombre = txtNombre.getText().toString();
        String autor = txtAutor.getText().toString();
        String nacionalidad = txtNacionalidad.getText().toString();
        String tipoLibro = spCategoria.getSelectedItem().toString();

        Map<String, Object> libro = new HashMap<>();
        libro.put("codigo", codigo);
        libro.put("nombre", nombre);
        libro.put("autor", autor);
        libro.put("nacionalidad", nacionalidad);
        libro.put("tipoLibro", tipoLibro);

        db.collection("libros")
                .document(codigo)
                .set(libro)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(MainActivity.this, "Datos enviados a Firestore Correctamente", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Error al enviar datos al Firestore" + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

    }

    public void CargarLista(View view){
        CargarListaFirestore();
    }

    public  void CargarListaFirestore(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("libros")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if( task.isSuccessful()) {
                            List<String> listaLibros = new ArrayList<>();

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String linea = "||" + document.getString("codigo") + "||" +
                                        document.getString("nombre") + "||" +
                                        document.getString("autor") + "||" +
                                        document.getString("nacionalidad");
                                listaLibros.add(linea);
                            }

                            ArrayAdapter<String> adaptador = new ArrayAdapter<>(MainActivity.this,
                                    android.R.layout.simple_list_item_1, listaLibros);
                            lista.setAdapter(adaptador);
                        } else {
                            Log.e("TAG", "Error al obtener datos de Firestore", task.getException());
                        }
                    }
                });
    }


}