package sistemaDistribuido.sistema.clienteServidor.modoMonitor;

import microKernelBasedSystem.system.clientServer.monitorMode.MicroKernelBase;
import microKernelBasedSystem.system.clientServer.userMode.threadPackage.SystemThread;
import sistemaDistribuido.util.Escribano;
import sistemaDistribuido.sistema.clienteServidor.modoUsuario.Proceso;
import java.net.DatagramSocket;

/**
 * 
 */
public abstract class MicroNucleoBase extends MicroKernelBase{
	private ParMaquinaProceso pmp;

	/**
	 * Practica 1 - Reanuda la ejecucion del proceso que le toque recibir mensaje
	 */
	protected void notificarHilos(){
		long[] procesos=super.getAllProcessIDs();
		int i,j=procesos.length;
		Proceso p;
		for(i=0;i<j;i++){
			if ((p=dameProcesoLocal((int)procesos[i]))!=null){
				super.resumeThread(p);
			}
			else{
				/* por concurrencia, procesos[i] pudo haber contenido un id de proceso conocido
				 * pero solo hasta antes de llamar a dameProcesoLocal (en distribuido se provocaria un paquete AU)
				 */
			}
		}
		imprimeln("Todos los hilos registrados han sido reanudados.");
	}

	/**
	 * Para compatibilidad con versiones previas a 2008A
	 */
	protected int atenderProceso(){
		return (int)super.getCurrentRunningProcessID();
	}

	/**
	 * Practica 1 - Inicia el sistema para maquina local
	 */
	public final synchronized void iniciarSistema(Escribano esc,int puertoEntrada){
		super.initializeSystem(esc, puertoEntrada);
	}

	/**
	 * Desde Practica 2 - Inicia el sistema usando una fuente de informacion para IP e ID
	 */
	public final synchronized void iniciarSistema(Escribano esc,int puertoEntrada,ParMaquinaProceso pmp){
		this.pmp=pmp;
		iniciarSistema(esc,puertoEntrada);
	}

	/**
	 * Desde Practica 1 - Cierra el sistema local
	 */
	public final synchronized void cerrarSistema(){
		super.shutdownSystem();
	}

	/**
	 * Devuelve el ID de proceso del Proceso p
	 */
	public final int dameIdProceso(Proceso p){
		return (int)super.getThreadID(p);
	}
	
	/**
	 * Termina un hilo t 
	 */
	public final void terminarHilo(SystemThread t){
		super.terminateThread(t);
	}
	
    /**
     * Devuelve true si el kernel no ha recibido la instruccion de cerrarse 
     */
	protected final boolean estaDisponible(){
		return super.isSystemAvailable();
	}

	/**
	 * A realizarse cuando se solicita desbloquear un hilo
	 */
	protected void unblockingThread(SystemThread t) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * A realizarse antes de que un hilo ya no pueda recibir servicios del n�cleo
	 */
	protected void finalizingThread(SystemThread p) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * Invocado desde MicroKernelBase
	 */
	protected boolean initializeModules() {
		return iniciarModulos();
	}
	
	/**
	 * Invocado desde MicroKernelBase para aplicaciones con addr long
	 */
	protected void receiveImplememted(long addr, byte[] message) {
		// TODO Auto-generated method stub
		
	}




	/*----Listado de metodos a usar y en su caso implementar en clase MicroNucleo----*/


	/**
	 * Imprime texto
	 */
	protected final void imprime(String s){
		super.print(s);
	}

	/**
	 * Imprime texto mas un salto de linea
	 */
	protected final void imprimeln(String s){
		super.println(s);
	}

	/**
	 * Imprime el nombre del thread invocante seguido del texto 
	 */
	protected final void timprime(String s){
		super.tprint(s);
	}

	/**
	 * Mismo que timprime mas un salto de linea
	 */
	protected final void timprimeln(String s){
		super.tprintln(s);
	}

	/**
	 * Practica 2 - Para obtener de la interfaz grafica IP e ID del proceso destinatario
	 */
	protected final ParMaquinaProceso dameDestinatarioDesdeInterfaz(){
		return pmp;
	}

	/**
	 * Practica 2 - Socket para enviar datagramas
	 */
	protected final DatagramSocket dameSocketEmision(){
		return super.getSendSocket();
	}

	/**
	 * Practica 2 - Socket para recibir datagramas
	 */
	protected final DatagramSocket dameSocketRecepcion(){
		return super.getReceiveSocket();
	}

	/**
	 * Practica 2 - Puerto para recibir mensajes
	 */
	protected final int damePuertoRecepcion(){
		return super.getReceivePort();
	}

	/**
	 * Practica 2 - devuelve distinto de null si el proceso des existe en esta maquina 
	 */
	protected final Proceso dameProcesoLocal(int dest){
		return (Proceso)super.getLocalThread(dest);
	}

	/**
	 * Practica 2 - Invocado desde MicroKernelBase
	 */
	protected final void sendImplemented(int dest,byte[] message){
		if (estaDisponible()){
			timprimeln("solicita envio bloqueante de mensaje.");
			sendVerdadero(dest,message);
		}
		else{
			imprimeln("PROCESO NO REGISTRADO INTENTA ENVIAR MENSAJE");
		}
	}

	/**
	 * Practica 5 - Invocado desde MicroKernelBase
	 */
	protected final void sendImplemented(String dest, byte[] message) {
		if (estaDisponible()){
			timprimeln("solicita envio bloqueante de mensaje.");
			sendVerdadero(dest,message);
		}
		else{
			imprimeln("PROCESO NO REGISTRADO INTENTA ENVIAR MENSAJE");
		}
	}

	/**
	 * Practica 2 - Invocable por un proceso para recibir un mensaje
	 */
	public final void receive(int addr,byte[] message){
		if (estaDisponible()){
			timprimeln("solicita recepci�n bloqueante de mensaje.");
			receiveVerdadero(addr,message);
		}
		else{
			imprimeln("PROCESO NO REGISTRADO INTENTA RECIBIR MENSAJE");
		}
	}

	/**
	 * Practica 2 - Devuelve true mientras el kernel no se le solicite cerrar desde la IG
	 */
	protected boolean seguirEsperandoDatagramas(){
		return super.isWaitingForDatagrams();
	}

	/**
	 * Desde Practica 1 - Detiene la ejecucion de un Proceso 
	 */
	public final void suspenderProceso(){
		super.suspendThread();
	}

	/**
	 * Desde Practica 1 - Reanuda la ejecucion de un Proceso
	 */
	public final void reanudarProceso(SystemThread t){
		super.resumeThread(t);
	}
	
	/**
	 * Desde Practica 2 - Devuelve el identificador unico del proceso invocador
	 */
	public int dameIdProceso(){
		return (int)super.getProcessID();
	}

	/**
	 * Desde Practica 6 - Devuelve el identificador unico del hilo invocador
	 */
	public int dameIdHilo(){
		return (int)super.getThreadID();
	}

	/**
	 * Practica 5 - Para envio no bloqueante de mensaje
	 */
	public final void sendNB(int dest,byte[] message){
		if (estaDisponible()){
			timprimeln("solicita envio no bloqueante de mensaje.");
			sendNBVerdadero(dest,message);
		}
		else{
			imprimeln("PROCESO NO REGISTRADO INTENTA ENVIAR MENSAJE");
		}
	}

	/**
	 * Practica 5 - Para recepcion no bloqueante de mensaje
	 */
	public final void receiveNB(int addr,byte[] message){
		if (estaDisponible()){
			timprimeln("solicita recepcion no bloqueante de mensaje.");
			receiveNBVerdadero(addr,message);
		}
		else{
			imprimeln("PROCESO NO REGISTRADO INTENTA RECIBIR MENSAJE");
		}
	}
	
	/**
	 * Practica 5 - Devuelva true si la inicializacion de modulos:
	 * Direccionamiento, Bloqueo, Almacenamiento y Confiabilidad ha sido exitosa
	 */
	protected abstract boolean iniciarModulos();

	/**
	 * Practica 2 - A implementar por alumn@s
	 */
	protected abstract void sendVerdadero(int dest,byte[] message);

	/**
	 * Practica 5 - A implementar por alumn@os
	 */
	protected abstract void sendVerdadero(String dest,byte[] message);

	/**
	 * Practica 2 - A implementar por alumn@s
	 */
	protected abstract void receiveVerdadero(int addr,byte[] message);

	/**
	 * Practica 5 - A implementar optativamente por alumn@s
	 */
	protected abstract void sendNBVerdadero(int dest,byte[] message);

	/**
	 * Practica 5 - A implementar optativamente por alumn@s
	 */
	protected abstract void receiveNBVerdadero(int addr,byte[] message);

	/**
	 * Practica 2 - A implementar por alumn@s
	 */
	public void run(){
	}

}
