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
    public void run() {

        imprimeln("Inicio de Proceso servidor.");
        byte[] solServidor = new byte[1024];
        byte[] respServidor;
        int origen;

        while (continuar()) {
            Nucleo.receive(dameID(), solServidor);
            imprimeln("Invocando a Receive.");
            imprimeln("Procesando petición recibida del cliente");
            respServidor = ejecutarComando(solServidor);
            imprimeln("Generando mensaje a ser enviado, llenando los campos necesarios");
            respServidor = empacarDatos(respServidor);
            Pausador.pausa(1000);  //sin esta línea es posible que Servidor solicite send antes que Cliente solicite receive
            imprimeln("Señalamiento al núcleo para envío de mensaje");
            origen = obtenerOrigen(solServidor);
            Nucleo.send(origen, respServidor);
            imprimeln("Fin del proceso");
        }
    }

    private boolean crearArchivo(String nombreArchivo) {
        boolean archivoCreado = false;
        File archivo = new File(nombreArchivo);
        if (!archivo.exists()) {
            try {
                archivoCreado = archivo.createNewFile();
            } catch (IOException e) {
            }
        }
        return archivoCreado;
    }

    private boolean eliminarArchivo(String nombreArchivo) {
        boolean archivoEliminado = false;
        File archivo = new File(nombreArchivo);
        try {
            archivoEliminado = archivo.delete();
        } catch (SecurityException e) {
        }
        return archivoEliminado;
    }

    private boolean escribirArchivo(String nombreArchivo, String escribir) {
        boolean archivoEscrito = false;
        BufferedWriter writer;
        File archivo = new File(nombreArchivo);
        try {
            if (archivo.exists()) {
                writer = new BufferedWriter(new FileWriter(archivo));
                writer.write(escribir);
                writer.close();
                archivoEscrito = true;
            }
        } catch (IOException e) {
        }
        return archivoEscrito;
    }

    private String leerArchivo(String nombreArchivo) {
        String archivoLeido = "Error al leer archivo: " + nombreArchivo;
        BufferedReader reader;
        File archivo = new File(nombreArchivo);
        try {
            if (archivo.exists()) {
                reader = new BufferedReader(new FileReader(archivo));
                archivoLeido = reader.readLine();
                reader.close();
            }
        } catch (IOException e) {
        }
        return archivoLeido;
    }

    private String ejecutarComando(short comando, String[] instrucciones) {

        String nombreArchivo = "nombre.txt";
        String lineaEscribir = "lineaEscribir";
        String respuesta = "";

        int longitud = (instrucciones.length > 2) ? 2 : instrucciones.length;

        switch (longitud) {
            case 2:
                lineaEscribir = instrucciones[1];
            case 1:
                nombreArchivo = instrucciones[0];
                break;
            default:
                break;
        }//fin de switch

        switch (comando) {
            case 0:
                respuesta += (crearArchivo(nombreArchivo)) ? (nombreArchivo + " creado") : ("Error al crear: " + nombreArchivo);
                imprimeln("Crear archivo: " + nombreArchivo);
                break;
            case 1:
                respuesta += (eliminarArchivo(nombreArchivo)) ? (nombreArchivo + " eliminado") : ("Error al eliminar: " + nombreArchivo);
                imprimeln("Eliminar archivo: " + nombreArchivo);
                break;
            case 2:
                respuesta += leerArchivo(nombreArchivo);
                imprimeln("Leer archivo: " + nombreArchivo);
                break;
            case 3:
                respuesta += (escribirArchivo(nombreArchivo, lineaEscribir)) ? (nombreArchivo + " Escrito: " + lineaEscribir) : ("Error al escribir: " + nombreArchivo);
                imprimeln("Escribir en archivo: " + nombreArchivo + " Linea: " + lineaEscribir);
                break;
            default:
                break;
        }//fin de switch
        return respuesta;
    }

    private byte[] ejecutarComando(byte[] instrucciones) {
        short codop, longitudDatos;
        String mensajeEmpacado = "";
        byte[] datos;
        byte[] codigoOperacion = new byte[2];
        byte[] longitudDatosByte = new byte[2];

        for (int i = 8, j = 0; j < codigoOperacion.length; j++, i++) {
            codigoOperacion[j] = instrucciones[i];
        }

        for (int i = 10, j = 0; j < 2; i++, j++) {
            longitudDatosByte[j] = instrucciones[i];
        }
        codop = desempacarShort(codigoOperacion);
        longitudDatos = desempacarShort(longitudDatosByte);
        datos = new byte[longitudDatos];

        for (int i = 12, j = 0; j < longitudDatos; j++, i++) {
            datos[j] = instrucciones[i];
        }
        mensajeEmpacado = (ejecutarComando(codop, (new String(datos)).split("-")));
        return mensajeEmpacado.getBytes();
    }

    private byte[] empacarDatos(byte[] datos) {
        short longitudDatos = (short) datos.length;
        byte[] datosByte = empacar(longitudDatos);
        byte[] datosEmpacados = new byte[12 + longitudDatos];

        for (int i = 0; i < 8; i++) {
            datosEmpacados[i] = 0;
        }

        for (int i = 10, j = 0; j < datosByte.length; i++, j++) {
            datosEmpacados[i] = datosByte[j];
        }

        for (int i = 12, j = 0; j < datos.length; i++, j++) {
            datosEmpacados[i] = datos[j];
        }
        return datosEmpacados;
    }

    private int obtenerOrigen(byte[] solServidor) {
        byte[] origen = new byte[4];
        System.arraycopy(solServidor, 0, origen, 0, 4);
        return desempacarEntero(origen);
    }

    private int desempacarEntero(byte[] arreglo) {
        int valor = 0x0;
        valor = (int) ((arreglo[3] & 0x000000FF) | (arreglo[2] << 8 & 0x0000FF00) | (arreglo[1] << 16 & 0x00FF0000) | (arreglo[0] << 24 & 0xFF000000));
        return valor;
    }

    private short desempacarShort(byte[] arreglo) {
        short valor = 0x0;
        valor = (short) ((arreglo[1] & 0x00FF) | (arreglo[0] << 8 & 0xFF00));
        return valor;
    }

    private byte[] empacar(short valor) {
        byte[] arreglo = new byte[2];

        arreglo[0] = (byte) (valor >> 8);
        arreglo[1] = (byte) valor;
        return arreglo;
    }

}//fin de la clase ProcesoServidor