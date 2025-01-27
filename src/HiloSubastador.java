import java.io.*;
import java.net.Socket;

public class HiloSubastador implements Runnable{
    public Socket socket;
    private ObjectOutputStream objectOut;
    private ObjectInputStream objectIn;
    private DataOutputStream dataOut;
    private DataInputStream dataIn;
    private GestorSubasta gestorSubasta;

    public HiloSubastador(Socket socket, ObjectOutputStream objectOut, ObjectInputStream objectIn, DataOutputStream dataOut,
                           DataInputStream dataIn, GestorSubasta gs){
        this.socket = socket;
        this.objectOut = objectOut;
        this.objectIn = objectIn;
        this.dataOut = dataOut;
        this.dataIn = dataIn;
        this.gestorSubasta = gs;
        System.out.println("Hilo Subastador creado correctamente");
    }

    @Override
    public void run() {
        int opcion;
        while(true){
            try{
                opcion = dataIn.readInt();
                System.out.println(opcion);
                switch (opcion){
                    case 1:
                        if(gestorSubasta.isSubastaActiva()){
                            gestorSubasta.enviarMensajeIndividual("Ya hay una subasta en curso, no puedes iniciar otra.", objectOut);
                        }else{
                            if(gestorSubasta.getParticipantesConectados() < 2){
                                gestorSubasta.enviarMensajeIndividual("Debes esperar a que se conecten al menos 2 participantes para iniciar la subasta", objectOut);
                            }else{
                            Subasta subasta =(Subasta)objectIn.readObject();
                            iniciarSubasta(subasta);
                            System.out.println("Subasta con codigo " + gestorSubasta.getCodigoSubasta() + " iniciada correctamente");
                            }
                        }
                        break;
                    case 2:
                        break;
                    default:
                        gestorSubasta.enviarMensajeIndividual("Debes ingresar una opción valida", objectOut);
                }
            }catch (IOException e){
                e.getMessage();
                manejarDesconexionSubastador();
                break;
            }catch (Exception e){
                e.getMessage();
            }
        }
    }

    private void manejarDesconexionSubastador() {
        try {
            gestorSubasta.eliminarCliente(objectOut);
            socket.close();
            gestorSubasta.setSubastaActiva(false);
            System.out.println("El subastador de la subasta " + gestorSubasta.getCodigoSubasta() + " se ha desconectado.");
            gestorSubasta.finalizarTemporizador();
            gestorSubasta.enviarActualizacionGlobal(5);
        } catch (IOException e) {
            System.out.println("Error al manejar la desconexion del subastador: " + e.getMessage());
        }
    }

    private void iniciarSubasta(Subasta subasta){
        gestorSubasta.setSubasta(subasta);
        gestorSubasta.setSubastaActiva(true);
        gestorSubasta.iniciarTemporizador();
        gestorSubasta.enviarMensajeIndividual("Subasta iniciada correctamente",objectOut);
        gestorSubasta.enviarActualizacionGlobal(1);
    }
}

