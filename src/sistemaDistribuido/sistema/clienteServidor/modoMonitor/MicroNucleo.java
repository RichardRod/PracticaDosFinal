package sistemaDistribuido.sistema.clienteServidor.modoMonitor;

import sistemaDistribuido.sistema.clienteServidor.modoMonitor.MicroNucleoBase;
import sistemaDistribuido.sistema.clienteServidor.modoUsuario.Proceso;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Hashtable;

/**
 *
 */
public final class MicroNucleo extends MicroNucleoBase {

    private static MicroNucleo nucleo = new MicroNucleo();
    private Hashtable<Integer, InfoProceso> tablaEmision;
    private Hashtable<Integer, byte[]> tablaRecepcion;
    protected static final int BYTES_IN_SHORT = 2;

    /**
     *
     */
    private MicroNucleo() {

        tablaEmision = new Hashtable<Integer, InfoProceso>();
        tablaRecepcion = new Hashtable<Integer, byte[]>();
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
    protected void sendVerdadero(int dest,byte[] message){

        ParMaquinaProceso pmp=dameDestinatarioDesdeInterfaz();
        if(tablaEmision.containsKey(dest)){
            System.out.println("Tomando datos desde tablaEmision");
			/* se toman desde tabla */
            message = fillAddress(tablaEmision.get(new Integer(dest)).getId(),
                    message);
            sendDatagamPacket(tablaEmision.get(new Integer(dest)).getIp(),
                    message);
            imprimeln("Enviando mensaje a IP="+tablaEmision.get(new Integer(dest)).getIp()+" ID="+tablaEmision.get(new Integer(dest)));
            tablaEmision.remove(dest);
        }
        else{
			/* se toman desde la interfaz */
            System.out.println("Tomando datos desde Interfaz");
            message = fillAddress(pmp.dameID(), message);
            sendDatagamPacket(pmp.dameIP(), message);
            imprimeln("Enviando mensaje a IP="+pmp.dameIP()+" ID="+pmp.dameID());
        }
        //suspenderProceso();   //esta invocacion depende de si se requiere bloquear al hilo de control invocador

    }

    /**
     *
     */
    protected void receiveVerdadero(int addr,byte[] message){
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
    public void run(){
        byte[]  message = new byte[1024],
                origin  = new byte[4],
                destiny = new byte[4],
                data;
        String ipSource;
        Proceso destinyProcess;
        DatagramPacket dp = new DatagramPacket(message, message.length);
        while(seguirEsperandoDatagramas()){
            try {
                dameSocketRecepcion().receive(dp);
                sleep(1000);
                System.arraycopy(dp.getData(), 0, origin , 0, 4);
                System.arraycopy(dp.getData(), 4, destiny, 0, 4);
                ipSource = dp.getAddress().getHostAddress();

                tablaEmision.put(new Integer(bytesToInt(origin)),
                        new InfoProceso(bytesToInt(origin), ipSource));
                destinyProcess = dameProcesoLocal(bytesToInt(destiny));
                if(destinyProcess == null){
                    byte[] error = new byte[BYTES_IN_SHORT];
					/* se manda AU */
                    data = dp.getData();
                    error = toByte((short)-1);
                    for(int i = 10, j = 0 ; j < +BYTES_IN_SHORT; i++, j++){
                        data[i] = error[j];
                    }
                    // descomentar la linea send, crea un bucle infinito enviando mensajes al proceso 0 (no existe)
                    send(bytesToInt(origin), data);
                }
                else{
                    if (tablaRecepcion.containsKey(bytesToInt(destiny))){
                        data = tablaRecepcion.get(bytesToInt(destiny));
                        System.arraycopy(dp.getData(), 0, data, 0,
                                dp.getData().length);
                        tablaRecepcion.remove(bytesToInt(destiny));
                        reanudarProceso(destinyProcess);
                    }
                    else{
                        System.out.println("ERROR: This should not appear, tablaRecepcion doesn't contain "
                                +bytesToInt(destiny));
						/* TODO: Enviar paquete con mensaje TA (Try Again) */
                    }
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendDatagamPacket(String ip, byte[] message){
        DatagramPacket dp;
        try {
            dp = new DatagramPacket(message, message.length,
                    InetAddress.getByName(ip),damePuertoRecepcion());
            dameSocketEmision().send(dp);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] fillAddress(int dest, byte[] message){
        byte [] filled = message,
                origin = intToByteArray(super.dameIdProceso()),
                destiny= intToByteArray(dest);
        System.out.println("MicroNucleo.java: Llenando cabecera de mensaje: ");
        System.out.println("   Origen: "+super.dameIdProceso());
        System.out.println("   Destino: "+dest);
        for(int i = 0; i < origin.length; i++){
            filled[i] = origin[i];
        }
        for(int i = origin.length, j = 0; i < (destiny.length + origin.length); i++ , j++){
            filled[i] = destiny[j];
        }
        return filled;
    }

    private byte[] intToByteArray(int data){
        byte[] byteArray = new byte[4];
		/* saved from most to less significant */
        for(int i = 3; i >= 0; i--){
            byteArray[i] = (byte) data;
            data >>= 8;
        }
        return byteArray;
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
}

class InfoProceso{
    private int idProceso;
    private String ipProceso;

    public InfoProceso(int id, String ip){
        setId(id);
        setIp(ip);
    }

    private void setId(int newId){
        idProceso = newId;
    }

    private void setIp(String newIp){
        ipProceso = newIp;
    }

    public int getId(){
        return idProceso;
    }

    public String getIp(){
        return ipProceso;
    }
}
