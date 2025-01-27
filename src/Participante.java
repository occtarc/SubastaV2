import java.io.Serializable;
import java.util.Scanner;

public class Participante extends Usuario implements Serializable {
    public Participante(){super("","", Rol.PARTICIPANTE);}
    public Participante(String nombre, String email){
        super(nombre,email,Rol.PARTICIPANTE);
    }

    public Oferta realizarOferta(){
        Scanner scanner = new Scanner(System.in);
        float montoOferta = 0;
        boolean ofertaValida = false;
        System.out.println("Ingrese el monto de la oferta (Debe ser mayor a 0): ");
        do{
            try{
                montoOferta = Float.parseFloat(scanner.nextLine());
                if(montoOferta > 0){
                    ofertaValida = true;
                }else{
                    System.out.println("Debes ingresar un monto mayor a 0. Intente nuevamente");
                }
            }catch(NumberFormatException e){
                System.out.println("Debes ingresar un numero válido. Intente nuevamente");
            }
        }while(!ofertaValida);
        return new Oferta(montoOferta,this);
    }

    @Override
    public String toString() {
        return String.format("Nombre: %s \nEmail: %s",getNombre(),getEmail());
    }
}
