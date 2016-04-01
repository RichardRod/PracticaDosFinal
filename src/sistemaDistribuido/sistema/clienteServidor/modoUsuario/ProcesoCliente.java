package sistemaDistribuido.sistema.clienteServidor.modoUsuario;

import sistemaDistribuido.sistema.clienteServidor.modoMonitor.Nucleo;
import sistemaDistribuido.sistema.clienteServidor.modoUsuario.Proceso;
import sistemaDistribuido.util.Escribano;

/**
 * @Nombre: Rodriguez Haro Ricardo
 * @seccion: D04
 * @No: Practica 1
 * Modificado para Practica 1
 */

public class ProcesoCliente extends Proceso {

    private short codigoOperacion;
    private String datos;

    public ProcesoCliente(Escribano esc) {
        super(esc);
        start();
    }

    public void run() {
        imprimeln("Inicio de Proceso Cliente");
        Nucleo.suspenderProceso();
        imprimeln("Generando mensaje a ser enviado, llenando los campos necesarios");
        byte[] solCliente = empacarDatos();
        byte[] respCliente = new byte[1024];
        String dato;
        imprimeln("Señalamiento al núcleo para envío de mensaje");
        Nucleo.send(248, solCliente);
        imprimeln("Invocando a Receive.");
        Nucleo.receive(dameID(), respCliente);
        imprimeln("Procesando respuesta recibida del servidor");
        dato = desempacarDatos(respCliente);
        imprimeln("Respuesta del servidor: " + dato);
        imprime("Fin del proceso");
    }//fin del metodo run

    private byte[] empacarDatos() {
        short longitudDatos = (short) datos.length();
        byte[] codop, datosByte, longitudBytes;
        codop = empacar(codigoOperacion);
        datosByte = datos.getBytes();
        longitudBytes = empacar(longitudDatos);
        byte[] solCliente = new byte[8 + codop.length + longitudBytes.length + datosByte.length];

        for (int i = 8, j = 0; j < codop.length; j++, i++) {
            solCliente[i] = codop[j];
        }

        for (int i = 8 + codop.length, j = 0; j < longitudBytes.length; j++, i++) {
            solCliente[i] = longitudBytes[j];
        }

        for (int i = 8 + codop.length + longitudBytes.length, j = 0; j < datosByte.length; j++, i++) {
            solCliente[i] = datosByte[j];
        }
        return solCliente;
    }

    private String desempacarDatos(byte[] datos) {
        String respuesta = "";
        short longitudDatos;
        byte[] longitudDatosByte = new byte[2];
        byte[] datosByte = new byte[datos.length - (2 - 8)];

        for (int i = 10, j = 0; j < 2; i++, j++) {
            longitudDatosByte[j] = datos[i];
        }

        longitudDatos = desempacarCorto(longitudDatosByte);

        if (longitudDatos > 0) {
            for (int i = 12, j = 0; j < longitudDatos; i++, j++) {
                datosByte[j] = datos[i];
            }
            respuesta = new String(datosByte);
        } else {

            if (longitudDatos == -1) {
                respuesta = "Error: Direccion Desconocida AU";
            } else {

                respuesta = "Error";
            }
        }
        return respuesta;
    }

    public void establecerDatos(short codigoOperacion, String datos) {
        this.codigoOperacion = codigoOperacion;
        this.datos = datos;
    }

    protected byte[] empacar(short valor) {
        byte[] areglo = new byte[2];

        areglo[0] = (byte) (valor >> 8);
        areglo[1] = (byte) valor;
        return areglo;
    }

    protected short desempacarCorto(byte[] arreglo) {
        short valor = 0x0;
        valor = (short) ((arreglo[1] & 0x00FF) | (arreglo[0] << 8 & 0xFF00));
        return valor;
    }
}//fin de la clase ProcesoCliene