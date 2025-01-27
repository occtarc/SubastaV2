import java.io.Serializable;

public abstract class Usuario implements Serializable {
    private String nombre;
    private String email;
    private Rol rol;

    public Usuario(String nombre, String email, Rol rol){
        this.nombre = nombre;
        this.email = email;
        this.rol = rol;
    }

    public String getNombre(){return nombre;}
    public String getEmail(){return email;}
    public Rol getRol(){return rol;}
    public String verResultadoSubasta(){return "";}

    @Override
    public String toString() {
        return String.format(
                        "Nombre: %s \n" +
                        "Email: %s \n" +
                        "Rol: %s"
                ,nombre,email,rol);
    }
}
