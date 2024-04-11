package com.ugb.controlesbasicos;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    TextView tempVal;
    Button btn;
    FloatingActionButton btnRegresar;
    String id="", rev="", idAmigo="", accion="nuevo";
    ImageView img;
    String urlCompletaFoto;
    Intent tomarFotoIntent;
    utilidades utls;
    DB db;
    detectarInternet di;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        utls = new utilidades();
        db = new DB(getApplicationContext(), "", null, 1);
        di = new detectarInternet(getApplicationContext());

        btnRegresar = findViewById(R.id.fabListaAmigos);
        btnRegresar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent regresarLista = new Intent(getApplicationContext(), lista_amigos.class);
                startActivity(regresarLista);
            }
        });
        btn = findViewById(R.id.btnGuardarAmigo);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    tempVal = findViewById(R.id.txtnombre);
                    String nombre = tempVal.getText().toString();

                    tempVal = findViewById(R.id.txtdireccion);
                    String direccion = tempVal.getText().toString();

                    tempVal = findViewById(R.id.txtTelefono);
                    String tel = tempVal.getText().toString();

                    tempVal = findViewById(R.id.txtEmail);
                    String email = tempVal.getText().toString();

                    tempVal = findViewById(R.id.txtDui);
                    String dui = tempVal.getText().toString();

                    String respuesta = "";
                    if( di.hayConexionInternet() ) {
                        //obtener datos a enviar al servidor
                        JSONObject datosAmigos = new JSONObject();
                        if (accion.equals("modificar")) {
                            datosAmigos.put("_id", id);
                            datosAmigos.put("_rev", rev);
                        }
                        datosAmigos.put("idAmigo", idAmigo);
                        datosAmigos.put("nombre", nombre);
                        datosAmigos.put("direccion", direccion);
                        datosAmigos.put("telefono", tel);
                        datosAmigos.put("email", email);
                        datosAmigos.put("dui", dui);
                        datosAmigos.put("urlCompletaFoto", urlCompletaFoto);
                        //enviamos los datos
                        enviarDatosServidor objGuardarDatosServidor = new enviarDatosServidor(getApplicationContext());
                        respuesta = objGuardarDatosServidor.execute(datosAmigos.toString()).get();
                        //comprobacion de la respuesta
                        JSONObject respuestaJSONObject = new JSONObject(respuesta);
                        if (respuestaJSONObject.getBoolean("ok")) {
                            id = respuestaJSONObject.getString("id");
                            rev = respuestaJSONObject.getString("rev");
                        } else {
                            respuesta = "Error al guardar en servidor: " + respuesta;
                        }
                    }
                    String[] datos = new String[]{id, rev, idAmigo, nombre, direccion, tel, email, dui, urlCompletaFoto};
                    respuesta = db.administrar_amigos(accion, datos);
                    if (respuesta.equals("ok")) {
                        mostrarMsg("Amigos registrado con exito.");
                        listarAmigos();
                    } else {
                        mostrarMsg("Error al intentar registrar el amigo: " + respuesta);
                    }
                }catch (Exception e){
                    mostrarMsg("Error al guadar datos en el servidor o en SQLite: "+ e.getMessage());
                }
            }
        });
        img = findViewById(R.id.btnImgAmigo);
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tomarFotoAmigo();
            }
        });
        mostrarDatosAmigos();
    }
    private void tomarFotoAmigo(){
        tomarFotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File fotoAmigo = null;
        try{
            fotoAmigo = crearImagenamigo();
            if( fotoAmigo!=null ){
                Uri urifotoAmigo = FileProvider.getUriForFile(MainActivity.this,
                        "com.ugb.controlesbasicos.fileprovider", fotoAmigo);
                tomarFotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, urifotoAmigo);
                startActivityForResult(tomarFotoIntent, 1);
            }else{
                mostrarMsg("No se pudo tomar la foto");
            }
        }catch (Exception e){
            mostrarMsg("Error al abrir la camara"+ e.getMessage());
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try{
            if( requestCode==1 && resultCode==RESULT_OK ){
                Bitmap imagenBitmap = BitmapFactory.decodeFile(urlCompletaFoto);
                img.setImageBitmap(imagenBitmap);
            }else{
                mostrarMsg("Se cancelo la toma de la foto");
            }
        }catch (Exception e){
            mostrarMsg("Error al seleccionar la foto"+ e.getMessage());
        }
    }
    private File crearImagenamigo() throws Exception{
        String fechaHoraMs = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()),
                fileName = "imagen_"+fechaHoraMs+"_";
        File dirAlmacenamiento = getExternalFilesDir(Environment.DIRECTORY_DCIM);
        if( dirAlmacenamiento.exists()==false ){
            dirAlmacenamiento.mkdirs();
        }
        File image = File.createTempFile(fileName, ".jpg", dirAlmacenamiento);
        urlCompletaFoto = image.getAbsolutePath();
        return image;
    }
    private void mostrarDatosAmigos(){
        try{
            Bundle parametros = getIntent().getExtras();
            accion = parametros.getString("accion");

            if(accion.equals("modificar")){
                JSONObject jsonObject = new JSONObject(parametros.getString("amigos")).getJSONObject("value");
                id = jsonObject.getString("_id");
                rev = jsonObject.getString("_rev");
                idAmigo = jsonObject.getString("idAmigo");

                tempVal = findViewById(R.id.txtnombre);
                tempVal.setText(jsonObject.getString("nombre"));

                tempVal = findViewById(R.id.txtdireccion);
                tempVal.setText(jsonObject.getString("direccion"));

                tempVal = findViewById(R.id.txtTelefono);
                tempVal.setText(jsonObject.getString("telefono"));

                tempVal = findViewById(R.id.txtEmail);
                tempVal.setText(jsonObject.getString("email"));

                tempVal = findViewById(R.id.txtDui);
                tempVal.setText(jsonObject.getString("dui"));

                urlCompletaFoto = jsonObject.getString("urlCompletaFoto");
                Bitmap imagenBitmap = BitmapFactory.decodeFile(urlCompletaFoto);
                img.setImageBitmap(imagenBitmap);
            }else{//nuevos registros
                idAmigo = utls.generarIdUnico();
            }
        }catch (Exception e){
            mostrarMsg("Error al mostrar los datos amigos");
        }
    }
    private void mostrarMsg(String msg){
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
    }
    private void listarAmigos(){
        Intent intent = new Intent(getApplicationContext(), lista_amigos.class);
        startActivity(intent);
    }
}