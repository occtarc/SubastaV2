import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Servidor{
    private static final int PUERTO = 5555;
    private static ServerSocket server;
    private static final Map<Integer,GestorSubasta> gestoresSubastas = new HashMap<>();
    private static final int MAX_GESTORES = 5;


    public static void main(String[] args){
        Servidor.iniciarServidor();
    }

    private static void iniciarServidor(){
        try{
            server = new ServerSocket(PUERTO);
            System.out.println("Servidor corriendo en el puerto " + PUERTO + " correctamente.");

            while(true){
                Socket socket = server.accept();
                asignarUsuarioASubasta(socket);
            }
        } catch (IOException e) {
            System.out.println("Error al iniciar el servidor");
            System.out.println(e.getMessage());
        }
    }

    private static void asignarUsuarioASubasta(Socket socket){
        try{
            ObjectOutputStream objectOut = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream objectIn = new ObjectInputStream(socket.getInputStream());
            DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());
            DataInputStream dataIn = new DataInputStream(socket.getInputStream());
            Usuario usuario = (Usuario)objectIn.readObject();
            if(usuario.getRol() == Rol.SUBASTADOR){
                if(gestoresSubastas.size() >= MAX_GESTORES){
                    System.out.println("Limite de subastas alcanzado");
                    objectOut.writeObject("Hay demasiadas subastas activas. Intenta mas tarde");
                    socket.close();
                }else{
                    int codigoSubasta = generarCodigoSubasta();
                    GestorSubasta gestorIndividual = new GestorSubasta(codigoSubasta,objectOut);
                    gestoresSubastas.put(codigoSubasta,gestorIndividual);
                    gestorIndividual.agregarCliente(objectOut);
                    System.out.println("Subastador conectado: " + usuario.getNombre());
                    gestorIndividual.enviarMensajeIndividual("Te has conectado correctamente al servidor como subastador. El codigo de tu subasta es " + gestorIndividual.getCodigoSubasta(), objectOut);
                    gestorIndividual.manejarConexionSubastador(new HiloSubastador(socket,objectOut,objectIn,dataOut,dataIn,gestorIndividual));
                }
            }else{
                int codigoSubastaParticipante;
                boolean conectado = false;

                while(!conectado){
                    codigoSubastaParticipante = dataIn.readInt();

                    if(gestoresSubastas.containsKey(codigoSubastaParticipante)){
                        GestorSubasta gestorIndividual = gestoresSubastas.get(codigoSubastaParticipante);
                        System.out.println("Participante conectado correctamente a la subasta: " + usuario.getNombre());
                        gestorIndividual.enviarMensajeIndividual("Te has conectado a la subasta " + codigoSubastaParticipante + " correctamente.", objectOut);
                        gestorIndividual.manejarConexionParticipante(new HiloParticipante(socket,objectOut,objectIn,dataOut,dataIn, gestorIndividual));
                        conectado = true;
                        gestorIndividual.sumarParticipante();
                        gestorIndividual.agregarCliente(objectOut);
                    }else{
                        objectOut.writeObject("El codigo de subasta ingresado no es valido. Intenta nuevamente.");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void eliminarSubasta(int codigoSubasta){
        gestoresSubastas.remove(codigoSubasta);
        System.out.println("Subasta con código " + codigoSubasta + " eliminada del mapa.");
    }

    private ServerSocket getServerSocket() {
        return server;
    }

    private static Integer generarCodigoSubasta(){
        int codigo;
        do{
            codigo = 1000 + new Random().nextInt(9000);
        }while(gestoresSubastas.containsKey(codigo));

        return codigo;
    }

    public GestorSubasta getGestorSubasta(int codigoSubasta) {
        return gestoresSubastas.get(codigoSubasta);
    }
}