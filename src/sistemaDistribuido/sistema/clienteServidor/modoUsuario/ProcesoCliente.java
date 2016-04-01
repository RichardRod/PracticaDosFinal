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

    private short codop;
    private String data;
    int OFFSET = 8;
    int BYTES_IN_SHORT = 2;

    public ProcesoCliente(Escribano esc) {
        super(esc);
        start();
    }



    public void run() {
        imprimeln("Inicio de Proceso Cliente");
        Nucleo.suspenderProceso();
        imprimeln("Generando mensaje a ser enviado, llenando los campos necesarios");
        byte[] solCliente = packageData();
        byte[] respCliente=new byte[1024]; //1024
        String dato;
        imprimeln("Señalamiento al núcleo para envío de mensaje");
        Nucleo.send(248,solCliente); //esta no se mueve
        imprimeln("Invocando a Receive.");
        Nucleo.receive(dameID(),respCliente); // esta tampoco
        imprimeln("Procesando respuesta recibida del servidor");
        dato=unpackageData(respCliente);
        imprimeln("Respuesta del servidor: "+dato);
    }//fin del metodo run

    private byte[] packageData(){
        short dataTam = (short) data.length();
        byte [] byteCodop, byteData, byteDataTam;
        byteCodop 	= toByte(codop);
        byteData 	= data.getBytes();
        byteDataTam = toByte(dataTam);
        byte [] solCliente = new byte[OFFSET + byteCodop.length + byteDataTam.length +byteData.length];
		/* FIXME: insert dir1, dir2 (now OFFSET) */
		/* insert codop */
        for(int i = OFFSET, j = 0; j < byteCodop.length; j++, i++){
            solCliente[i] = byteCodop[j];
        }
		/* insert data length */
        for(int i = OFFSET+byteCodop.length,j = 0; j < byteDataTam.length; j++,i++){
            solCliente[i] = byteDataTam[j];
        }
		/* insert data*/
        for(int i = OFFSET+byteCodop.length+byteDataTam.length, j = 0; j < byteData.length; j++, i++){
            solCliente[i] = byteData[j];
        }
        return solCliente;
    }

    private String unpackageData(byte[] data){
        String dataStr = "";
        short dataTam;
        byte[] byteDataTam = new byte[BYTES_IN_SHORT],
                byteData = new byte[data.length - (BYTES_IN_SHORT - OFFSET)];
		/* extract dataTam (short) */
        for(int i = OFFSET + BYTES_IN_SHORT, j = 0; j < BYTES_IN_SHORT;  i++, j++){
            byteDataTam[j] = data[i];
        }
        dataTam = ToShort(byteDataTam);
        if(dataTam > 0){
            for(int i = OFFSET + (BYTES_IN_SHORT*2), j = 0; j < dataTam; i++, j++){
                byteData[j] = data[i];
            }
            dataStr = new String(byteData);
        }
        else{
			/* errores del servidor */
            if(dataTam == -1){
                dataStr = "Error: Adress unknown";
            }
            else{
				/* TODO: Implementar más errores */
                dataStr = "Error: Desconocido...";
            }
        }
        return dataStr;
    }

    public void setData(short codop, String data){
        this.codop = codop;
        this.data  = data;
    }

    protected byte[] toByte(short value){
        byte[] byteArray = new byte[BYTES_IN_SHORT];
		/* saved from most to less significant */
        byteArray[0] = (byte) (value >> 8);
        byteArray[1] = (byte) value;
        return byteArray;
    }

    protected short ToShort(byte[] array){
        short bytesValue = 0x0;
        bytesValue = (short)((array[1]      & 0x00FF) |
                (array[0] << 8 & 0xFF00));
        return bytesValue;
    }

}
