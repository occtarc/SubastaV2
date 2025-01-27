import javax.xml.crypto.Data;
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
                manejarConexion(socket);
            }
        } catch (IOException e) {
            System.out.println("Error al iniciar el servidor: " + e.getMessage());
        }
    }

    private static void manejarConexion(Socket socket){
        try {
            ObjectOutputStream objectOut = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream objectIn = new ObjectInputStream(socket.getInputStream());
            DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());
            DataInputStream dataIn = new DataInputStream(socket.getInputStream());
            Usuario usuario = (Usuario)objectIn.readObject();
            if(usuario.getRol() == Rol.SUBASTADOR){
                gestionarSubastador(socket,usuario, objectOut,dataOut, objectIn,dataIn);
            }else{
                gestionarParticipante(socket,usuario,objectOut,dataOut,objectIn,dataIn);
            }
        } catch (Exception e) {
            System.err.println("Error al manejar la conexión: " + e.getMessage());
        }
    }

    private static void gestionarSubastador(Socket socket, Usuario usuario, ObjectOutputStream objectOut, DataOutputStream dataOut,
                                            ObjectInputStream objectIn, DataInputStream dataIn) throws IOException{

        if(gestoresSubastas.size() >= MAX_GESTORES){
            System.out.println("Limite de subastas alcanzado");
            objectOut.writeObject("Hay demasiadas subastas activas. Intenta mas tarde");
            socket.close();
            return;
        }

        int codigoSubasta = generarCodigoSubasta();
        GestorSubasta gestor = new GestorSubasta(codigoSubasta,objectOut);
        gestoresSubastas.put(codigoSubasta,gestor);
        System.out.println("Subastador conectado: " + usuario.getNombre() + ". Subasta numero " + gestor.getCodigoSubasta());
        gestor.enviarMensajeIndividual("Te has conectado correctamente al servidor como subastador. El codigo de tu subasta es " + codigoSubasta, objectOut);
        gestor.agregarCliente(objectOut);
        gestor.manejarConexionSubastador(new HiloSubastador(socket,objectOut,objectIn,dataOut,dataIn,gestor));

    }

    private static void gestionarParticipante(Socket socket, Usuario usuario, ObjectOutputStream objectOut, DataOutputStream dataOut,
                                              ObjectInputStream objectIn, DataInputStream dataIn) throws IOException{
        boolean conectado = false;

        while(!conectado){
            int codigoSubasta = dataIn.readInt();
            GestorSubasta gestor = gestoresSubastas.get(codigoSubasta);

            if(gestor != null){
                System.out.println("Participante conectado: " + usuario.getNombre() + ". Subasta numero " + gestor.getCodigoSubasta());
                gestor.enviarMensajeIndividual("Te has conectado a la subasta " + codigoSubasta + " correctamente.", objectOut);
                gestor.agregarCliente(objectOut);
                gestor.sumarParticipante();
                gestor.manejarConexionParticipante(new HiloParticipante(socket,objectOut,objectIn,dataOut,dataIn,gestor));
                conectado = true;
            }else{
                objectOut.writeObject("El codigo de subasta ingresado no es valido. Intenta nuevamente.");
            }
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