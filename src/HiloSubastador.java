import java.io.*;
import java.net.Socket;

public class HiloSubastador implements Runnable{
    private Socket socket;
    private ObjectOutputStream objectOut;
    private ObjectInputStream objectIn;
    private DataOutputStream dataOut;
    private DataInputStream dataIn;
    private GestorSubasta gestorSubasta;

    public HiloSubastador(Socket socket, ObjectOutputStream objectOut, ObjectInputStream objectIn,
                          DataOutputStream dataOut, DataInputStream dataIn, GestorSubasta gs){
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
                switch (opcion){
                    case 1:
                        if(gestorSubasta.isSubastaActiva()){
                            gestorSubasta.enviarMensajeIndividual("Ya hay una subasta en curso, debes esperar a que finalice", objectOut);
                        }else{
                            if(gestorSubasta.getParticipantesConectados() < 2){
                                gestorSubasta.enviarMensajeIndividual("Debes esperar a que se conecten al menos 2 participantes para iniciar la subasta", objectOut);
                            }else{
                            Subasta subasta =(Subasta)objectIn.readObject();
                            gestorSubasta.setSubasta(subasta);
                            gestorSubasta.setSubastaActiva(true);
                            gestorSubasta.iniciarTemporizador();
                            System.out.println("Subasta con codigo " + gestorSubasta.getCodigoSubasta() + " iniciada correctamente");
                            gestorSubasta.enviarMensajeIndividual("Subasta iniciada correctamente", objectOut);
                            gestorSubasta.enviarActualizacionGlobal(1);
                            }
                        }
                        break;
                    case 2:
                        break;
                    default:
                        gestorSubasta.enviarMensajeIndividual("Debes ingresar una opción valida", objectOut);
                }
            }catch (IOException e){
                System.out.println("El cliente se ha desconectado: " + socket.getInetAddress());
                manejarDesconexionSubastador();
                break;
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private void manejarDesconexionSubastador() {
        try {
            gestorSubasta.eliminarCliente(objectOut);
            socket.close();
            gestorSubasta.setSubastaActiva(false);
            System.out.println("El subastador se ha desconectado.");
            gestorSubasta.finalizarTemporizador();
            gestorSubasta.enviarActualizacionGlobal(5);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

