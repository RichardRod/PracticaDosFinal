package sistemaDistribuido.sistema.clienteServidor.modoMonitor;

import sistemaDistribuido.sistema.clienteServidor.modoMonitor.MicroNucleoBase;
import sistemaDistribuido.sistema.clienteServidor.modoUsuario.Proceso;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Hashtable;

/**
 * @Nombre: Rodriguez Haro Ricardo
 * @seccion: D04
 * @No: Practica 2
 * Modificado para Practica 2
 */

public final class MicroNucleo extends MicroNucleoBase {

    private static MicroNucleo nucleo = new MicroNucleo();
    private Hashtable<Integer, TransmisionProceso> tablaEmision = new Hashtable<>();
    private Hashtable<Integer, byte[]> tablaRecepcion = new Hashtable<>();

    /**
     *
     */
    private MicroNucleo() {
    }

    /**
     *
     */
    public final static MicroNucleo obtenerMicroNucleo() {
        return nucleo;
    }

    /*---Metodos para probar el paso de mensajes entre los procesos cliente y servidor en ausencia de datagramas.
    Esta es una forma incorrecta de programacion "por uso de variables globales" (en este caso atributos de clase)
    ya que, para empezar, no se usan ambos parametros en los metodos y fallaria si dos procesos invocaran
    simultaneamente a receiveFalso() al reescriir el atributo mensaje---*/
    byte[] mensaje;

    public void sendFalso(int dest, byte[] message) {
        System.arraycopy(message, 0, mensaje, 0, message.length);
        notificarHilos();  //Reanuda la ejecucion del proceso que haya invocado a receiveFalso()
    }

    public void receiveFalso(int addr, byte[] message) {
        mensaje = message;
        suspenderProceso();
    }
    /*---------------------------------------------------------*/

    /**
     *
     */
    protected boolean iniciarModulos() {
        return true;
    }

    /**
     *
     */
    protected void sendVerdadero(int dest, byte[] message) {

        ParMaquinaProceso pmp = dameDestinatarioDesdeInterfaz();
        if (tablaEmision.containsKey(dest)) {
            message = empacarDatos(tablaEmision.get(new Integer(dest)).getId(), message);
            enviarMensaje(tablaEmision.get(new Integer(dest)).getIp(), message);
            imprimeln("Enviando mensaje a IP=" + tablaEmision.get(new Integer(dest)).getIp() + " ID=" + tablaEmision.get(new Integer(dest)));
            tablaEmision.remove(dest);
        } else {
            message = empacarDatos(pmp.dameID(), message);
            enviarMensaje(pmp.dameIP(), message);
            imprimeln("Enviando mensaje a IP=" + pmp.dameIP() + " ID=" + pmp.dameID());
        }
        //suspenderProceso();   //esta invocacion depende de si se requiere bloquear al hilo de control invocador

    }

    /**
     *
     */
    protected void receiveVerdadero(int addr, byte[] message) {
        tablaRecepcion.put(new Integer(addr), message);
        suspenderProceso();
    }

    /**
     * Para el(la) encargad@ de direccionamiento por servidor de nombres en practica 5
     */
    protected void sendVerdadero(String dest, byte[] message) {
    }

    /**
     * Para el(la) encargad@ de primitivas sin bloqueo en practica 5
     */
    protected void sendNBVerdadero(int dest, byte[] message) {
    }

    /**
     * Para el(la) encargad@ de primitivas sin bloqueo en practica 5
     */
    protected void receiveNBVerdadero(int addr, byte[] message) {
    }

    /**
     *
     */
    public void run() {
        byte[] mensaje = new byte[1024];
        byte[] origen = new byte[4];
        byte[] destino = new byte[4];
        byte[] datos;
        String ip;
        Proceso procesoDestino;
        DatagramPacket recepcion = new DatagramPacket(mensaje, mensaje.length);

        while (seguirEsperandoDatagramas()) {

            try {
                dameSocketRecepcion().receive(recepcion);
                System.arraycopy(recepcion.getData(), 0, origen, 0, 4);
                System.arraycopy(recepcion.getData(), 4, destino, 0, 4);
                ip = recepcion.getAddress().getHostAddress();

                tablaEmision.put(new Integer(desempacarEntero(origen)), new TransmisionProceso(desempacarEntero(origen), ip));
                procesoDestino = dameProcesoLocal(desempacarEntero(destino));

                if (procesoDestino == null) {
                    byte[] error = new byte[2];

                    datos = recepcion.getData();
                    error = empacar((short) -1);
                    for (int i = 10, j = 0; j < +2; i++, j++) {
                        datos[i] = error[j];
                    }

                    send(desempacarEntero(origen), datos);
                } else {
                    if (tablaRecepcion.containsKey(desempacarEntero(destino))) {
                        datos = tablaRecepcion.get(desempacarEntero(destino));
                        System.arraycopy(recepcion.getData(), 0, datos, 0, recepcion.getData().length);
                        tablaRecepcion.remove(desempacarEntero(destino));
                        reanudarProceso(procesoDestino);
                    } else {
                        imprime("No esta en la tabla recepcion");

                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void enviarMensaje(String ip, byte[] mensaje) {
        DatagramPacket packet;
        try {
            packet = new DatagramPacket(mensaje, mensaje.length, InetAddress.getByName(ip), damePuertoRecepcion());
            dameSocketEmision().send(packet);
        }//fin de try
        catch (IOException e) {
            e.printStackTrace();
        }//fin de catch
    }

    private byte[] empacarDatos(int destino, byte[] mensaje) {
        byte[] mensajeEmpacado = mensaje;
        byte[] origen = empacar(super.dameIdProceso());
        byte[] destinoAux = empacar(destino);

        for (int i = 0; i < origen.length; i++) {
            mensajeEmpacado[i] = origen[i];
        }
        for (int i = origen.length, j = 0; i < (destinoAux.length + origen.length); i++, j++) {
            mensajeEmpacado[i] = destinoAux[j];
        }
        return mensajeEmpacado;
    }


    private int desempacarEntero(byte[] arreglo) {
        int valor = 0x0;
        valor = (int) ((arreglo[3] & 0x000000FF) | (arreglo[2] << 8 & 0x0000FF00) | (arreglo[1] << 16 & 0x00FF0000) | (arreglo[0] << 24 & 0xFF000000));
        return valor;
    }

    private byte[] empacar(int valor) {
        byte[] arreglo = new byte[4];

        for (int i = 3; i >= 0; i--) {
            arreglo[i] = (byte) valor;
            valor >>= 8;
        }
        return arreglo;
    }

    private byte[] empacar(short valor) {
        byte[] arreglo = new byte[2];

        arreglo[0] = (byte) (valor >> 8);
        arreglo[1] = (byte) valor;
        return arreglo;
    }
}

class TransmisionProceso {
    private int idProceso;
    private String ipProceso;

    public TransmisionProceso(int idProceso, String ipProceso) {
        this.idProceso = idProceso;
        this.ipProceso = ipProceso;
    }

    public int getId() {
        return idProceso;
    }

    public String getIp() {
        return ipProceso;
    }
}
