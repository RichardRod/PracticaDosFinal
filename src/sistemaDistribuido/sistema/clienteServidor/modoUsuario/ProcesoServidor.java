package sistemaDistribuido.sistema.clienteServidor.modoUsuario;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

import sistemaDistribuido.sistema.clienteServidor.modoMonitor.Nucleo;
import sistemaDistribuido.sistema.clienteServidor.modoUsuario.Proceso;
import sistemaDistribuido.util.Escribano;
import sistemaDistribuido.util.Pausador;


/**
 * @Nombre: Rodriguez Haro Ricardo
 * @seccion: D04
 * @No: Practica 1
 * Modificado para Practica 1
 */

public class ProcesoServidor extends Proceso {

    int OFFSET = 8;
    int BYTES_IN_SHORT = 2;

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
        byte[] respServidor; //1024
        int origin;
        while(continuar()){
            imprimeln("Invocando a Receive.");
            Nucleo.receive(dameID(),solServidor);
            imprimeln("Procesando petición recibida del cliente");
            respServidor = procesaLlamada(solServidor);
            imprimeln("Generando mensaje a ser enviado, llenando los campos necesarios");
            respServidor = packageData(respServidor);
            Pausador.pausa(1000);  //sin esta línea es posible que Servidor solicite send antes que Cliente solicite receive
            imprimeln("Señalamiento al núcleo para envío de mensaje");
            origin = getOrigin(solServidor);
            System.out.println("El origen del paquete es: "+origin);
            Nucleo.send(origin,respServidor);
        }
    }

    private boolean createFile(String fileName){
        boolean success = false;
        File newFile = new File(fileName);
        if(!newFile.exists()){
            try {
                success = newFile.createNewFile();
            } catch (IOException e) {}
        }
        return success;
    }

    private boolean deleteFile(String fileName){
        boolean success = false;
        File newFile = new File(fileName);
        try{
            success = newFile.delete();
        } catch (SecurityException e) {}
        return success;
    }

    private boolean writeFile(String fileName, String lineToWrite){
        boolean success = false;
        BufferedWriter br;
        File newFile = new File(fileName);
        try {
            if(newFile.exists()){
                br = new BufferedWriter(new FileWriter(newFile));
                br.write(lineToWrite);
                br.close();
                success = true;
            }
        } catch (IOException e) {}
        return success;
    }

    private String readFile(String fileName){
        String success = "Error al leer desde archivo '"+fileName+"'";
        BufferedReader br;
        File newFile = new File(fileName);
        try {
            if(newFile.exists()){
                br = new BufferedReader(new FileReader(newFile));
                success = br.readLine();
                br.close();
            }
        } catch (IOException e) {}
        return success;
    }

    private String HacerOperacion(short operacion, String[] args){

        String fileName = "default.txt",
                toWrite = "default line";
        String log = "";
        int length = (args.length > 2) ? 2 : args.length;
        switch(length){
            case 2:
                toWrite = args[1];
            case 1:
                fileName = args[0];
                break;
            default: break;
        }
        switch(operacion){
            case 0:
                log+=(createFile(fileName)) ?  (fileName+" creado") :
                        (fileName +" no creado");
                imprimeln("Se solicitó servicio 'Crear' con el nombre de archivo: "+fileName);
                break;
            case 1:
                log+= (deleteFile(fileName)) ? (fileName +" eliminado") :
                        (fileName +" no eliminado");
                imprimeln("Se solicitó servicio 'Eliminar' con el nombre de archivo: "+fileName);
                break;
            case 2:
                log+= readFile(fileName);
                imprimeln("Se solicitó servicio 'Leer' con el nombre de archivo: "+fileName);
                break;
            case 3:
                log+= (writeFile(fileName, toWrite)) ? (fileName +" Escrito: "+toWrite) :
                        (fileName +" Error: No se pudo escribir");
                imprimeln("Se solicitó servicio 'Escribir' con el nombre de archivo: "+fileName+
                        " y el texto: '"+toWrite+"'");
                break;
            default: break;
        }
        return log;
    }

    private byte[] procesaLlamada(byte[] arrayBytes){
        short codop, dataLength;
        String message = "";
        byte[] data,
                byteCodop 	= new byte[BYTES_IN_SHORT],
                byteDataTam	= new byte[BYTES_IN_SHORT];

		/* extract codop */
        for(int i = OFFSET, j = 0; j < byteCodop.length; j++, i++){
            byteCodop[j] = arrayBytes[i];
        }
		/* extract data length (short)*/
        for(int i = OFFSET+BYTES_IN_SHORT, j = 0 ; j < BYTES_IN_SHORT; i++, j++){
            byteDataTam[j] = arrayBytes[i];
        }
        codop 		= ToShort(byteCodop);
        dataLength	= ToShort(byteDataTam);
        data = new byte[dataLength];
		/* get data */
        for(int i = OFFSET+(BYTES_IN_SHORT*2), j = 0; j < dataLength; j++, i++){
            data[j] = arrayBytes[i];
        }
        message = (HacerOperacion(codop, (new String(data)).split(",")));
        return message.getBytes();
    }

    private byte[] packageData(byte[] data){
        short dataTam = (short) data.length;
        byte[] byteDataTam = toByte(dataTam);
        byte[] newPackage = new byte[OFFSET+(BYTES_IN_SHORT*2)+dataTam];

        for(int i = 0; i < OFFSET; i++){
            newPackage[i] = 0;
        }
		/* insert dataTam (short) */
        for(int i = OFFSET+BYTES_IN_SHORT, j = 0; j < byteDataTam.length; i++,j++){
            newPackage[i] = byteDataTam[j];
        }
		/* insert data (data.length bytes) */
        for(int i = OFFSET+(BYTES_IN_SHORT*2), j = 0; j < data.length; i++, j++){
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
        byte[] byteArray = new byte[BYTES_IN_SHORT];
		/* saved from most to less significant */
        byteArray[0] = (byte) (value >> 8);
        byteArray[1] = (byte) value;
        return byteArray;
    }



}//fin de la clase ProcesoServidor
