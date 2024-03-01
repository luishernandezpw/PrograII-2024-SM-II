package com.ugb.controlesbasicos;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {
    TextView tempVal;
    Button btn;
    FloatingActionButton btnRegresar;
    String id="", accion="nuevo";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

                String[] datos = new String[]{id,nombre,direccion,tel,email,dui};
                DB db = new DB(getApplicationContext(),"", null, 1);
                String respuesta = db.administrar_amigos(accion, datos);
                if( respuesta.equals("ok") ){
                    Toast.makeText(getApplicationContext(), "Amigo registrado con exito", Toast.LENGTH_LONG).show();
                }else {
                    Toast.makeText(getApplicationContext(), "Error al intentar registrar el amigo: "+
                            respuesta, Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}