import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class GestorSubasta {

    private Subasta subasta;
    private boolean subastaActiva;
    private int tiempoRestante;
    private int codigoSubasta;
    private Timer temporizador;
    private int participantesConectados;
    private ObjectOutputStream conexionSubastador;
    private ArrayList<ObjectOutputStream> clientesConectados;
    private String carpetaSubastas = "RegistroSubastas";

    public GestorSubasta(int codigoSubasta, ObjectOutputStream conexionSubastador){
        this.subasta = new Subasta();
        this.subastaActiva = false;
        this.tiempoRestante = 0;
        this.codigoSubasta = codigoSubasta;
        this.temporizador = new Timer();
        this.participantesConectados = 0;
        this.conexionSubastador = conexionSubastador;
        this.clientesConectados = new ArrayList<>();
    }

    public void enviarMensajeIndividual(String mensaje, ObjectOutputStream objOut){
        try {
            objOut.writeObject(mensaje);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void enviarActualizacionGlobal(int opcion) {
        String mensaje = null;
        switch (opcion) {
            case 1:
                mensaje = String.format("Se ha iniciado una subasta. \n" +
                        "Subastador: %s\n" +
                        "Producto a subastar: \n%s\n" +
                        "Duracion de la subasta: %d", subasta.getSubastador().getNombre(), subasta.getArticulo(), subasta.getTiempo());
                break;
            case 2:
                if (subasta.getOfertaMayor() == null) {
                    mensaje = "La subasta ha finalizado sin ofertas para el siguiente articulo: \n" + subasta.getArticulo();
                } else {
                    mensaje = String.format("La subasta ha finalizado.\n" +
                            "Ganador: %s\n" +
                            "Monto final: $%.2f", subasta.getOfertaMayor().getParticipante().getNombre(), subasta.getOfertaMayor().getMonto());
                    almacenarSubasta();
                }
                break;
            case 3:
                mensaje = String.format("Se ha registrado una nueva oferta mayor \n" +
                        "Ofertante: %s\n" +
                        "Monto: $%.2f", subasta.getOfertaMayor().getParticipante().getNombre(), subasta.getOfertaMayor().getMonto());
                break;
            case 4:
                mensaje = "Quedan 10 segundos para que finalice la subasta!";
                break;
            case 5:
                mensaje = "Subastador desconectado! Fin de la subasta.";
                break;
        }

        if (!clientesConectados.isEmpty()) {
            for (ObjectOutputStream objOut : clientesConectados) {
                try {
                    objOut.writeObject(mensaje);
                    objOut.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void almacenarSubasta(){
        LocalDateTime fecha = LocalDateTime.now();
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        String fechaFormateada = fecha.format(formato);

        // Crear carpeta si no existe
        File carpeta = new File(carpetaSubastas);
        if (!carpeta.exists()) {
            if (carpeta.mkdir()) {
                System.out.println("Carpeta creada: " + carpetaSubastas);
            } else {
                System.out.println("Error al crear la carpeta.");
                return;
            }
        }

        String nombreArchivo = fecha.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + "_" + getCodigoSubasta() + ".txt";
        File archivo = new File(carpeta, nombreArchivo);

        try (FileWriter escritor = new FileWriter(archivo)) {
            String texto = "Subasta del día: " + fechaFormateada +
                    "\nSubastador: \n" + subasta.getSubastador().toString() +
                    "Artículo: \n" + subasta.getArticulo().toString() +
                    "\nGanador de la subasta: \nParticipante:\n" + subasta.getOfertaMayor().getParticipante().toString() +
                    "\nOferta mayor: " + subasta.getOfertaMayor().getMonto() + "\n";
            escritor.write(texto);
            System.out.println("Subasta almacenada en: " + archivo.getAbsolutePath());
        } catch (IOException e) {
            System.out.println("Ocurrió un error al escribir en el archivo.");
            e.printStackTrace();
        }
    }

    public void manejarConexionSubastador(HiloSubastador hiloSubastador){
            new Thread(hiloSubastador).start();
    }

    public void iniciarTemporizador(){
        temporizador = new Timer();
        tiempoRestante = subasta.getTiempo();
        temporizador.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(tiempoRestante == 10){
                    System.out.println("Quedan 10 segundos para que finalice la subasta " + getCodigoSubasta());
                    enviarActualizacionGlobal(4);
                }
                if(tiempoRestante == 0){
                    subastaActiva = false;
                    enviarActualizacionGlobal(2);
                    temporizador.cancel();
                    System.out.println("Subasta finalizada.");
                    Servidor.eliminarSubasta(getCodigoSubasta());
                    for(ObjectOutputStream cliente : clientesConectados){
                        try {
                            cliente.close();
                            conexionSubastador.close();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }else{
                    tiempoRestante--;
                    System.out.println("Quedan " + tiempoRestante + " segundos");
                }
            }
        },0,1000);
    }

    public void finalizarTemporizador(){
        temporizador.cancel();
    }

    public boolean isSubastaActiva() {
        return subastaActiva;
    }

    public void setSubastaActiva(boolean subastaActiva) {
        this.subastaActiva = subastaActiva;
    }

    public Subasta getSubasta() {
        return this.subasta;
    }

    public void setSubasta(Subasta subasta) {
        this.subasta = subasta;
    }

    public int getCodigoSubasta(){return this.codigoSubasta;}

    public void agregarCliente(ObjectOutputStream obj){
        clientesConectados.add(obj);
    }

    public void eliminarCliente(ObjectOutputStream obj){
        clientesConectados.remove(obj);
    }

    public int getTiempoRestante(){
        return this.tiempoRestante;
    }

    public int getParticipantesConectados(){return this.participantesConectados;}

    public void sumarParticipante(){
        this.participantesConectados++;
        if(participantesConectados == 2){
            enviarMensajeIndividual("Ya hay 2 participantes conectados, puedes iniciar la subasta", conexionSubastador);
        }
    }

    public void setTiempoRestante(int tiempoRestante){
        this.tiempoRestante = tiempoRestante;
    }
    public void manejarConexionParticipante(HiloParticipante hiloParticipante) {
        new Thread(hiloParticipante).start();
    }
}
