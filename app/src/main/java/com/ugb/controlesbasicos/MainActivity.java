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

import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

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
    String urlCompletaFotoFirestore;
    String miToken="";
    Intent tomarFotoIntent;
    utilidades utls;
    DB db;
    detectarInternet di;
    DatabaseReference databaseReference;
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
                subirFotoFirestore();
            }
        });
        img = findViewById(R.id.btnImgAmigo);
        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tomarFotoAmigo();
            }
        });
        obtenerToken();
        mostrarDatosAmigos();
    }
    private void obtenerToken(){
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(tarea->{
            if( !tarea.isSuccessful() ) return;
            miToken = tarea.getResult();
        });
    }
    private void subirFotoFirestore(){
        mostrarMsg("Subiendo foto...");
        StorageReference storageReference = FirebaseStorage.getInstance().getReference();
        Uri file = Uri.fromFile(new File(urlCompletaFoto));
        final StorageReference reference = storageReference.child("foto/"+file.getLastPathSegment());

        final UploadTask tareaSubir = reference.putFile(file);
        tareaSubir.addOnFailureListener(e->{
            mostrarMsg("Error al subir la foto a firestore: "+ e.getMessage());
        });
        tareaSubir.addOnSuccessListener(instantanea->{
            mostrarMsg("Foto subida con exito.");
            Task<Uri> descargarUri = tareaSubir.continueWithTask(tarea->reference.getDownloadUrl()).addOnCompleteListener(tarea->{
                if( tarea.isSuccessful() ){
                    urlCompletaFotoFirestore = tarea.getResult().toString();
                    guardarAmigo();
                }else{
                    mostrarMsg("Error al obtener la ruta de la foto. ");
                }
            });
        });
    }
    private void guardarAmigo(){
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

            databaseReference = FirebaseDatabase.getInstance().getReference("amigos");
            String key = databaseReference.push().getKey();

            if( miToken.equals("") || miToken==null ){
                obtenerToken();
            }
            if( miToken!="" && miToken!=null ){
                amigos amigo = new amigos(idAmigo,nombre,direccion,tel,email,dui,urlCompletaFoto,urlCompletaFotoFirestore,miToken);
                if(key!=null){
                    databaseReference.child(key).setValue(amigo).addOnSuccessListener(aVoid->{
                        mostrarMsg("Amigo registrado con exito.");
                    });
                }else{
                    mostrarMsg("Error al intentar guardar el amigo.");
                }
            }else{
                mostrarMsg("Error el telefono no es compatible con las notificaciones");
            }
        }catch (Exception e){
            mostrarMsg("Error al guadar datos en el servidor o en SQLite: "+ e.getMessage());
        }
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