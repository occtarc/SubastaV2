import java.io.*;
import java.net.Socket;

public class HiloParticipante implements Runnable{
    private Socket socket;
    private ObjectOutputStream objectOut;
    private ObjectInputStream objectIn;
    private DataOutputStream dataOut;
    private DataInputStream dataIn;
    private GestorSubasta gestorSubasta;
    public HiloParticipante(Socket socket, ObjectOutputStream objectOut, ObjectInputStream objectIn, DataOutputStream dataOut,
                            DataInputStream dataIn, GestorSubasta gs){
        this.socket = socket;
        this.objectOut = objectOut;
        this.objectIn = objectIn;
        this.dataOut = dataOut;
        this.dataIn = dataIn;
        this.gestorSubasta = gs;
        System.out.println("Hilo Participante creado correctamente");
    }

    @Override
    public void run() {
        if(gestorSubasta.isSubastaActiva()){
            gestorSubasta.enviarMensajeIndividual(String.format("Hay una subasta en curso. Puedes participar \n" +
                                    "Subastador: %s\n" +
                                    "Producto: \n%s\n" +
                                    "Tiempo restante: %d segundos\n" +
                                    "%s",
                                    gestorSubasta.getSubasta().getSubastador().getNombre(),
                                    gestorSubasta.getSubasta().getArticulo(),
                                    gestorSubasta.getTiempoRestante(),
                                    gestorSubasta.getSubasta().getOfertaMayor() != null
                                    ? "Mayor oferta actual: $" + gestorSubasta.getSubasta().getOfertaMayor().getMonto()
                                    : "Aun no hay ofertas para el articulo")
                            ,objectOut);
        }
        int opcion;
        while(true){
            try {
                opcion = dataIn.readInt();
                switch (opcion) {
                    case 1:
                        if (!gestorSubasta.isSubastaActiva()) {
                            gestorSubasta.enviarMensajeIndividual("Espera a que haya una subasta activa para realizar una oferta", objectOut);
                        } else {
                            Oferta ofertaCliente = (Oferta) objectIn.readObject();
                            if ((gestorSubasta.getSubasta().getOfertaMayor() == null && ofertaCliente.getMonto() >= gestorSubasta.getSubasta().getArticulo().getPrecioBase()) ||
                                    (gestorSubasta.getSubasta().getOfertaMayor() != null && ofertaCliente.getMonto() >= gestorSubasta.getSubasta().getOfertaMayor().getMonto())) {
                                fijarOfertaMayor(ofertaCliente);
                                System.out.println("Actualizacion nueva oferta mayor en la subasta: " + gestorSubasta.getCodigoSubasta() + "\nOferta: " + gestorSubasta.getSubasta().getOfertaMayor().getMonto());
                            } else if(gestorSubasta.getSubasta().getOfertaMayor() == null && gestorSubasta.getSubasta().getArticulo().getPrecioBase() > ofertaCliente.getMonto()) {
                                gestorSubasta.enviarMensajeIndividual("Oferta rechazada. La oferta realizada no supera el precio base", objectOut);
                            }else{
                                gestorSubasta.enviarMensajeIndividual("Oferta rechazada. La oferta realizada no supera el monto de la oferta mayor", objectOut);
                            }
                        }
                        break;
                    case 2:
                        manejarDesconexionParticipante();
                        break;
                    default:
                        gestorSubasta.enviarMensajeIndividual("Debes ingresar una opción valida", objectOut);
                }
            }catch (IOException e){
                System.out.println("Se ha cerrado la conexion con el cliente: " + socket.getInetAddress() + " de la subasta " + gestorSubasta.getCodigoSubasta());
                break;
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private void manejarDesconexionParticipante() {
        try {
            gestorSubasta.eliminarCliente(objectOut);
            socket.close();
            System.out.println("Recursos liberados para el cliente desconectado.");
        } catch (IOException e) {
            System.out.println("Error al manejar la desconexion del participante: " + e.getMessage());
        }
    }

    private void fijarOfertaMayor(Oferta ofertaCliente){
        gestorSubasta.getSubasta().setOfertaMayor(ofertaCliente);
        gestorSubasta.reiniciarTemporizador();
        gestorSubasta.enviarMensajeIndividual("Oferta recibida correctamente. Actualmente tu oferta es la mayor", objectOut);
        gestorSubasta.enviarActualizacionGlobal(3);
    }
}

