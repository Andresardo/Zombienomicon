package pt.ipleiria.zombienomicon.Model;

public enum State {
    DEAD,UNDEAD;


    @Override
    public String toString() {
        String string = "";
        switch(this){
            case DEAD:
                string = "Dead";
                break;
            case UNDEAD:
                string = "Undead";
            break;
            default:
                break;
        }
        return string;
    }

    public static State BooleanState(boolean isDead){
        if(isDead){
            return State.DEAD;
        } else {
            return State.UNDEAD;
        }
    }

    public static State StringState(String string){
        switch (string.toUpperCase()){
            case "DEAD":
                return State.DEAD;
            case "UNDEAD":
                return State.UNDEAD;
            default:
                return State.UNDEAD;
        }
    }

    public static String StateString(State state){
        switch (state){
            case DEAD:
                return "Dead";
            case UNDEAD:
                return "Undead";
            default:
                return "Undead";
        }
    }

}


