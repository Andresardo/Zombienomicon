package pt.ipleiria.zombienomicon.Model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Objects;

import pt.ipleiria.zombienomicon.R;

/**
 * Class Zombie que contem toda a informação relativa a um zombie
 */
public class Zombie implements Serializable {
    private int id;
    private GregorianCalendar detection_date;
    private GregorianCalendar termination_date;
    private String name;
    private String gender;
    private String detection_location;
    private String state_dead;

    /**
     * Construtor da classe que recebe todos os valores necessários para criar um novo Zombie
     */
    public Zombie(int id, GregorianCalendar detection_date, GregorianCalendar termination_date, String name, String gender, String detection_location, String state_dead) {
        this.id = id;
        this.detection_date = detection_date;
        this.termination_date = termination_date;
        this.name = name;
        this.gender = gender;
        this.detection_location = detection_location;
        this.state_dead = state_dead;
    }

    /**
     * Método que devolove o ID de um determinado Zombie
     */
    public int getId() {
        return id;
    }

    /**
     * Método que permite alterar o ID de um determinado Zombie
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Método que devolove a data de deteção de um determinado Zombie
     */
    public GregorianCalendar getDetection_date() {
        return detection_date;
    }

    /**
     * Método que permite alterar a data de deteção de um determinado Zombie
     */
    void setDetection_date(GregorianCalendar detection_date) {
        this.detection_date = detection_date;
    }

    /**
     * Método que devolove a data de terminação de um determinado Zombie
     */
    public GregorianCalendar getTermination_date() {
        return termination_date;
    }

    /**
     * Método que permite alterar a data de terminação de um determinado Zombie
     */
    void setTermination_date(GregorianCalendar termination_date) {
        this.termination_date = termination_date;
    }

    /**
     * Método que devolove o nome de um determinado Zombie
     */
    public String getName() {
        return name;
    }

    /**
     * Método que permite alterar o nome de um determinado Zombie
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Método que devolove o género de um determinado Zombie
     */
    public String getGender() {
        return gender;
    }

    /**
     * Método que permite alterar o género de um determinado Zombie
     */
    void setGender(String gender) {
        this.gender = gender;
    }

    /**
     * Método que devolove a localização de deteção de um determinado Zombie
     */
    public String getDetection_location() {
        return detection_location;
    }

    /**
     * Método que permite alterar a localização de deteção de um determinado Zombie
     */
    void setDetection_location(String detection_location) {
        this.detection_location = detection_location;
    }

    /**
     * Método que devolove o estado de um determinado Zombie
     */
    public String getState_dead() {
        return state_dead;
    }

    /**
     * Método que permite alterar o estado de um determinado Zombie
     */
    void setState_dead(String state_dead) {
        this.state_dead = state_dead;
    }

    /**
     * Método que devolove a String com todos os parâmetros de um Zombie
     */
    @Override
    public String toString() {
        String res = "";

        SimpleDateFormat formatD = new SimpleDateFormat("dd-MMM-yyyy");
        formatD.setCalendar(detection_date);
        String dateFormattedD = formatD.format(detection_date.getTime());

        SimpleDateFormat formatT = new SimpleDateFormat("dd-MMM-yyyy");
        formatT.setCalendar(termination_date);
        String dateFormattedT = formatT.format(termination_date.getTime());

        /**
         * Caso o ID tenha zeros à esquerda, são acrescentados para serem apresentados
         */
        String idAsString = "" + id;
        int length = idAsString.length();
        idAsString = "";
        for (int i = 0; i < 9 - length; i++) {
            idAsString += '0';
        }
        idAsString += id;

        /**
         * Caso o Zombie esteja morto, o toString apresenta também a data de terminação
         */
        res = Singleton.getInstance().getContext().getResources().getString(R.string.id) + idAsString + '\n' +
                Singleton.getInstance().getContext().getResources().getString(R.string.name) + name + '\n' +
                Singleton.getInstance().getContext().getResources().getString(R.string.state) + state_dead + '\n' +
                Singleton.getInstance().getContext().getResources().getString(R.string.gender) + gender + '\n' +
                Singleton.getInstance().getContext().getResources().getString(R.string.detection_date) + dateFormattedD + '\n' +
                Singleton.getInstance().getContext().getResources().getString(R.string.detection_location) + detection_location + '\n';
        if (Objects.equals(state_dead, "Dead")) {
            res += Singleton.getInstance().getContext().getResources().getString(R.string.termination_date) + dateFormattedT + '\n';
        }
        return res;
    }

    /**
     * Concatena todos os dados do Zombie numa String para ser procurado no método SearchZombieByAll
     */
    String zombieSimpleToString() {
        String res = "";

        SimpleDateFormat formatD = new SimpleDateFormat("dd-MMM-yyyy");
        formatD.setCalendar(detection_date);
        String dateFormattedD = formatD.format(detection_date.getTime());

        SimpleDateFormat formatT = new SimpleDateFormat("dd-MMM-yyyy");
        formatT.setCalendar(termination_date);
        String dateFormattedT = formatT.format(termination_date.getTime());

        /**
         * Caso o ID tenha zeros à esquerda, são acrescentados para serem apresentados
         */
        String idAsString = "" + id;
        int length = idAsString.length();
        idAsString = "";
        for (int i = 0; i < 9 - length; i++) {
            idAsString += '0';
        }
        idAsString += id;

        res += idAsString + '\n' +
                name + '\n' +
                state_dead + '\n' +
                gender + '\n' +
                dateFormattedD + '\n' +
                detection_location + '\n';
        if (Objects.equals(state_dead, "Dead")) {
            res += dateFormattedT;
        }
        return res;
    }
}