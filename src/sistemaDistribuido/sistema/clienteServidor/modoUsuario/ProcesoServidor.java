package sistemaDistribuido.sistema.clienteServidor.modoUsuario;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import sistemaDistribuido.sistema.clienteServidor.modoMonitor.Nucleo;
import sistemaDistribuido.util.Escribano;
import sistemaDistribuido.util.Pausador;


/**
 * @Nombre: Rodriguez Haro Ricardo
 * @seccion: D04
 * @No: Practica 2
 * Modificado para Practica 2
 */

public class ProcesoServidor extends Proceso {

    int POSICION_CODOP = 8;
    /**
     *
     */
    public ProcesoServidor(Escribano esc) {
        super(esc);
        start();
    }

    /**
     *
     */
    public void run(){

        imprimeln("Inicio de Proceso servidor.");
        byte[] solServidor=new byte[1024];
        byte[] respServidor;
        int origen;

        while(continuar()){
            imprimeln("Invocando a Receive.");
            Nucleo.receive(dameID(),solServidor);
            imprimeln("Procesando petición recibida del cliente");
            respServidor = procesaLlamada(solServidor);
            imprimeln("Generando mensaje a ser enviado, llenando los campos necesarios");
            respServidor = packageData(respServidor);
            Pausador.pausa(1000);  //sin esta línea es posible que Servidor solicite send antes que Cliente solicite receive
            imprimeln("Señalamiento al núcleo para envío de mensaje");
            origen = getOrigin(solServidor);
            System.out.println("El origen del paquete es: "+origen);
            Nucleo.send(origen,respServidor);
        }
    }

    private boolean crearArchivo(String nombreArchivo){
        boolean archivoCreado = false;
        File archivo = new File(nombreArchivo);
        if(!archivo.exists()){
            try {
                archivoCreado = archivo.createNewFile();
            } catch (IOException e) {}
        }
        return archivoCreado;
    }

    private boolean eliminarArchivo(String nombreArchivo){
        boolean archivoEliminado = false;
        File archivo = new File(nombreArchivo);
        try{
            archivoEliminado = archivo.delete();
        } catch (SecurityException e) {}
        return archivoEliminado;
    }

    private boolean escribirArchivo(String nombreArchivo, String escribir){
        boolean archivoEscrito = false;
        BufferedWriter writer;
        File archivo = new File(nombreArchivo);
        try {
            if(archivo.exists()){
                writer = new BufferedWriter(new FileWriter(archivo));
                writer.write(escribir);
                writer.close();
                archivoEscrito = true;
            }
        } catch (IOException e) {}
        return archivoEscrito;
    }

    private String leerArchivo(String nombreArchivo){
        String archivoLeido = "Error al leer archivo: " + nombreArchivo;
        BufferedReader reader;
        File archivo = new File(nombreArchivo);
        try {
            if(archivo.exists()){
                reader = new BufferedReader(new FileReader(archivo));
                archivoLeido = reader.readLine();
                reader.close();
            }
        } catch (IOException e) {}
        return archivoLeido;
    }

    private String ejecutarComando(short comando, String[] instrucciones){

        String nombreArchivo = "nombre.txt";
        String lineaEscribir = "lineaEscribir";
        String respuesta = "";

        int longitud = (instrucciones.length > 2) ? 2 : instrucciones.length;

        switch(longitud)
        {
            case 2:
                lineaEscribir = instrucciones[1];
            case 1:
                nombreArchivo = instrucciones[0];
                break;
            default: break;
        }//fin de switch

        switch(comando)
        {
            case 0:
                respuesta += (crearArchivo(nombreArchivo)) ?  (nombreArchivo+" creado") : ("Error al crear: " + nombreArchivo);
                imprimeln("Crear archivo: " + nombreArchivo);
                break;
            case 1:
                respuesta+= (eliminarArchivo(nombreArchivo)) ? (nombreArchivo +" eliminado") : ("Error al eliminar: " + nombreArchivo);
                imprimeln("Eliminar archivo: "+nombreArchivo);
                break;
            case 2:
                respuesta += leerArchivo(nombreArchivo);
                imprimeln("Leer archivo: " + nombreArchivo);
                break;
            case 3:
                respuesta += (escribirArchivo(nombreArchivo, lineaEscribir)) ? (nombreArchivo +" Escrito: "+lineaEscribir) : ("Error al escribir: " + nombreArchivo);
                imprimeln("Escribir en archivo: " + nombreArchivo + " Linea: "+ lineaEscribir);
                break;
            default: break;
        }//fin de switch
        return respuesta;
    }

    private byte[] procesaLlamada(byte[] arrayBytes){
        short codop, dataLength;
        String message = "";
        byte[] data,
                byteCodop 	= new byte[2],
                byteDataTam	= new byte[2];

		/* extract codop */
        for(int i = POSICION_CODOP, j = 0; j < byteCodop.length; j++, i++){
            byteCodop[j] = arrayBytes[i];
        }
		/* extract data length (short)*/
        for(int i = POSICION_CODOP +2, j = 0; j < 2; i++, j++){
            byteDataTam[j] = arrayBytes[i];
        }
        codop 		= ToShort(byteCodop);
        dataLength	= ToShort(byteDataTam);
        data = new byte[dataLength];
		/* get data */
        for(int i = POSICION_CODOP +(4), j = 0; j < dataLength; j++, i++){
            data[j] = arrayBytes[i];
        }
        message = (ejecutarComando(codop, (new String(data)).split(",")));
        return message.getBytes();
    }

    private byte[] packageData(byte[] data){
        short dataTam = (short) data.length;
        byte[] byteDataTam = toByte(dataTam);
        byte[] newPackage = new byte[POSICION_CODOP +(4)+dataTam];

        for(int i = 0; i < POSICION_CODOP; i++){
            newPackage[i] = 0;
        }
		/* insert dataTam (short) */
        for(int i = POSICION_CODOP +2, j = 0; j < byteDataTam.length; i++,j++){
            newPackage[i] = byteDataTam[j];
        }
		/* insert data (data.length bytes) */
        for(int i = POSICION_CODOP +(4), j = 0; j < data.length; i++, j++){
            newPackage[i] = data[j];
        }
        return newPackage;
    }

    private int getOrigin(byte[] solServidor){
        byte[] origin = new byte[4];
        System.arraycopy(solServidor, 0, origin, 0, 4);
        return bytesToInt(origin);
    }

    private int bytesToInt(byte[] array){
        int bytesValue = 0x0;
        bytesValue = (int)( (array[3]       & 0x000000FF) |
                (array[2] << 8  & 0x0000FF00) |
                (array[1] << 16 & 0x00FF0000) |
                (array[0] << 24 & 0xFF000000));
        return bytesValue;
    }

    protected short ToShort(byte[] array){
        short bytesValue = 0x0;
        bytesValue = (short)((array[1]      & 0x00FF) |
                (array[0] << 8 & 0xFF00));
        return bytesValue;
    }

    protected byte[] toByte(short value){
        byte[] byteArray = new byte[2];
		/* saved from most to less significant */
        byteArray[0] = (byte) (value >> 8);
        byteArray[1] = (byte) value;
        return byteArray;
    }



}//fin de la clase ProcesoServidor
