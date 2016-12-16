package pt.ipleiria.zombienomicon.Model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Objects;

/**
 * Class Zombienomicon que contem uma lista de todos os zombies.
 */
public class Zombienomicon implements Serializable {
    private ArrayList<Zombie> zombies;

    /**
     * Instanciar lista de zombies
     */
    Zombienomicon() {
        this.zombies = new ArrayList<>();
    }

    /**
     * Método que procura todos os zombies cujo nome contenha a string inserida
     */
    public ArrayList<Zombie> searchZombieByName(String string) {
        ArrayList<Zombie> list = new ArrayList<>();
        for (Zombie z : zombies) {
            if (z.getName().contains(string)) {
                list.add(z);
            }
        }
        return list;
    }

    /**
     * Método que procura e devolve o zombie com um determinado ID
     */
    public Zombie searchZombieByID(int id) {
        for (Zombie z : zombies) {
            if (z.getId() == id) {
                return z;
            }
        }
        return null;
    }

    /**
     * Método que procura todos os zombies cujo id contenha a string inserida
     */
    public ArrayList<Zombie> searchZombieContainingId(String received) {
        ArrayList<Zombie> list = new ArrayList<>();
        String idAsString;

        for (Zombie z : zombies) {
            idAsString = "" + z.getId();
            int length = idAsString.length();
            idAsString = "";
            for (int i = 0; i < 9 - length; i++) {
                idAsString += '0';
            }
            idAsString += z.getId();
            if (idAsString.contains(received)) {
                list.add(z);
            }
        }
        return list;
    }

    /**
     * Método que procura e devolve a posição do zombie com um determinado ID
     */
    public int searchPositionByID(int id) {
        for (int i = 0; i < zombies.size(); i++) {
            if (zombies.get(i).getId() == id) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Método que devolve uma lista de Zombies de um determinado estado
     */
    public ArrayList<Zombie> searchZombieByState(String string) {
        ArrayList<Zombie> list = new ArrayList<>();
        for (Zombie z : zombies) {
            if (z.getState_dead().equals(string)) {
                list.add(z);
            }
        }
        return list;
    }

    /**
     * Método que devolve uma lista de Zombies de um determinado género
     */
    public ArrayList<Zombie> searchZombieByGender(String string) {
        ArrayList<Zombie> list = new ArrayList<>();
        for (Zombie z : zombies) {
            if (Objects.equals(z.getGender(), string)) {
                list.add(z);
            }
        }
        return list;
    }

    /**
     * Método que devolve a lista de Zombies cuja data de deteção é posterior a um determinada data
     */
    public ArrayList<Zombie> searchZombieByDetectionDate(GregorianCalendar date) {
        ArrayList<Zombie> list = new ArrayList<>();
        for (Zombie z : zombies) {
            if (z.getDetection_date().compareTo(date) >= 0) {
                list.add(z);
            }
        }
        return list;
    }

    /**
     * Método que devolve a lista de todos os zombies
     */
    public ArrayList<Zombie> getZombies() {
        return zombies;
    }

    @Override
    public String toString() {
        String res = "";
        for (Zombie z : zombies) {
            res += z + "\n";
        }
        return res;
    }

    /**
     * Método que edita os campos de um Zombie
     */
    public void editZombie(Zombie new_zombie, Zombie old_zombie) {
        if (searchZombieByID(new_zombie.getId()) != null && old_zombie.getId() != new_zombie.getId()) {
            throw new IllegalArgumentException();
        } else {
            Zombie originalZombie = searchZombieByID(old_zombie.getId());
            originalZombie.setId(new_zombie.getId());
            originalZombie.setName(new_zombie.getName());
            originalZombie.setDetection_date(new_zombie.getDetection_date());
            originalZombie.setDetection_location(new_zombie.getDetection_location());
            originalZombie.setGender(new_zombie.getGender());
            originalZombie.setState_dead(new_zombie.getState_dead());
            originalZombie.setTermination_date(new_zombie.getTermination_date());
        }
    }

    /**
     * Método que devolve a lista de Zombies de um determinado género
     */
    public void addZombie(Zombie z) {
        if (searchZombieByID(z.getId()) != null) {
            throw new IllegalArgumentException("ID já existe!");
        } else {
            zombies.add(z            );
        }
    }

    public void deleteZombie(int position) {
        zombies.remove(position);
    }


    /**
     * Verifica se a String recebida está contida nos dados de cada Zombie
     */
    public ArrayList<Zombie> searchZombieByAll(String string) {
        ArrayList<Zombie> list = new ArrayList<>();
        for (Zombie z : zombies) {
            if (z.zombieSimpleToString().contains(string)) {
                list.add(z);
            }
        }
        return list;
    }
    /**
     * Método que procura e devolve a posição do zombie com um determinado ID
     */
    public int searchAvailableID() {
        int i;

        for(int id=1; id < 999999999; id++) {
            for ( i = 0; i < zombies.size(); i++) {
                if (zombies.get(i).getId() == id) {
                    break;
                }
            }
            if(i==zombies.size()) {
                return id;
            }
        }
        return -1;
    }
}
