import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.sql.SQLOutput;
import java.util.Scanner;

public class Cliente {
    private static Usuario usuario;
    private static final String DIRECCION_SERVIDOR = "localhost";
    private static final int PUERTO = 5555;
    private static Socket socket;
    private static boolean participantesMinimos = false;
    private static boolean subastaActiva;
    private static boolean participanteConectado = false;
    private static  boolean escuchando = true;

    public Cliente(){}

    public static void main (String[] args){
        Cliente.definirUsuario();

        try {
            Cliente.socket = new Socket(Cliente.DIRECCION_SERVIDOR,Cliente.PUERTO);
            ObjectOutputStream objectOut = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream objectIn = new ObjectInputStream(socket.getInputStream());
            DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());
            DataInputStream dataIn = new DataInputStream(socket.getInputStream());
            objectOut.writeObject(Cliente.usuario);
            Scanner scanner = new Scanner(System.in);
            int opcion;

            new Thread(()->{
                try{
                    while(escuchando){
                        Object mensaje = objectIn.readObject();
                        System.out.println("[Notificacion del servidor]: " + mensaje);
                        if(mensaje instanceof String){
                            String msg = (String) mensaje;
                            if(msg.contains("Ya hay una subasta activa") || msg.contains("Se ha iniciado una subasta") || msg.contains("Hay una subasta en curso")){
                                subastaActiva = true;
                            }
                            else if(msg.contains("La subasta ha finalizado") || msg.contains("Subastador desconectado! Fin de la subasta.")){
                                subastaActiva = false;
                            }else if(msg.contains("Te has conectado a la subasta")){
                               participanteConectado = true;
                            }else if(msg.contains("Ya hay 2 participantes conectados")){
                                participantesMinimos = true;
                            }else if(msg.contains("El subastador se ha desconectado")){
                                System.exit(0);
                            }
                        }
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }).start();

            if(Cliente.usuario.getRol() == Rol.SUBASTADOR){
                Subastador subastador = (Subastador)Cliente.usuario;
                while(true){
                    Thread.sleep(100);
                    System.out.println("1- Iniciar una subasta");
                    System.out.println("2- Salir del sistema");
                    System.out.print("> ");
                    opcion = scanner.nextInt();
                    dataOut.writeInt(opcion);
                    switch (opcion){
                        case 1:
                            if(!subastaActiva && participantesMinimos){
                                objectOut.writeObject(new Subasta(subastador.generarArticuloASubastar(), subastador.fijarTiempoSubasta(), (Subastador)Cliente.usuario));
                                subastaActiva = true;
                            }
                            break;
                        case 2:
                            desconexion();
                            break;
                    }
                }
            }else{
                Participante participante = (Participante) Cliente.usuario;
                int codigo;
                System.out.println("Ingresa el codigo de la subasta a la cual te deseas conectar");
                do{
                    System.out.print("> ");
                    codigo = scanner.nextInt();
                    dataOut.writeInt(codigo);
                    Thread.sleep(50);
                }while(!participanteConectado);
                while(true){
                    Thread.sleep(100);
                    System.out.println("1- Realizar una oferta");
                    System.out.println("2- Salir del sistema");
                    System.out.print("> ");
                    opcion = scanner.nextInt();
                    dataOut.writeInt(opcion);
                    switch (opcion){
                        case 1:
                            if(subastaActiva){
                                objectOut.writeObject(participante.realizarOferta());
                            }
                            break;
                        case 2:
                            desconexion();
                            break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void definirUsuario(){
        Scanner scanner = new Scanner(System.in);
        String nombreUsuario;
        String emailUsuario;
        String rolUsuario;

        System.out.println("Ingrese su nombre: ");
        nombreUsuario = scanner.nextLine();

        System.out.println("Ingrese su email: ");
        emailUsuario = scanner.nextLine();

        System.out.println("¿Desea ser Subastador o Participante? (Ingrese S o P): ");
        boolean rolValido = false;
        do{
            rolUsuario = scanner.nextLine();
            if(rolUsuario.equals("S") || rolUsuario.equals("P")){
                rolValido = true;
            }else{
                System.out.println("Debes ingresar un rol valido. Intente nuevamente");
            }
        }while(!rolValido);

        if (rolUsuario.equals("S")){
            Cliente.setUsuario(new Subastador(nombreUsuario,emailUsuario));
        }else{
            Cliente.setUsuario(new Participante(nombreUsuario,emailUsuario));
        }
    }

    public static void desconexion(){
        try {
            escuchando = false;
            if (socket != null) socket.close();
            System.out.println("Conexion finalizada correctamente");
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            System.exit(0);
        }
    }
    public static Usuario getUsuario(){return usuario;}
    public static void setUsuario(Usuario usuario){Cliente.usuario = usuario;}
}
