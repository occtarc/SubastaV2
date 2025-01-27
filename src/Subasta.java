import java.io.Serializable;
import java.util.Observable;

public class Subasta implements Serializable {
    private static final long serialVersionUID = 1L;
    private Articulo articulo;
    private int tiempo;
    private Subastador subastador;
    private boolean finalizada;
    private Oferta ofertaMayor;

    public Subasta(){
        this.articulo = new Articulo();
        this.tiempo = 0;
        this.subastador = new Subastador();
        this.finalizada = false;
        this.ofertaMayor = new Oferta();
    }
    public Subasta(Articulo articulo, int tiempo, Subastador subastador){
        this.articulo = articulo;
        this.tiempo = tiempo;
        this.subastador = subastador;
        this.finalizada = false;
        this.ofertaMayor = null;
    }

    public Articulo getArticulo() {return this.articulo;}
    public int getTiempo(){return this.tiempo;}
    public Subastador getSubastador() {return this.subastador;}
    public boolean getFinalizada(){return this.finalizada;}
    public void setFinalizada(boolean finalizada){this.finalizada = finalizada;}
    public Oferta getOfertaMayor(){return this.ofertaMayor;}
    public void setOfertaMayor(Oferta ofertaMayor){this.ofertaMayor = ofertaMayor;}

    @Override
    public String toString() {
        return String.format(
                        "%s \n" +
                        "Tiempo Inicial: %d \n" +
                        "Subastador: %s \n" +
                        "Estado: %s \n" +
                        "Monto oferta mayor: $%.2f \n" +
                                "Ofertante: %s",
        this.articulo ,this.tiempo,this.subastador.getNombre(), this.finalizada ? "Finalizada" : "Activa", this.ofertaMayor == null ? 0 : this.ofertaMayor.getMonto(),
                this.ofertaMayor == null ? "-" : this.ofertaMayor.getParticipante());
    }
}
