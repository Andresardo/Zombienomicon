package pt.ipleiria.zombienomicon.Model;

import pt.ipleiria.zombienomicon.R;

public enum State {
    DEAD, UNDEAD;


    public static State BooleanState(boolean isDead) {
        if (isDead) {
            return State.DEAD;
        } else {
            return State.UNDEAD;
        }
    }

    public static State StringState(String string) {
        switch (string.toUpperCase()) {
            case "DEAD":
            case "MORTO":
                return State.DEAD;
            case "UNDEAD":
            case "MORTO-VIVO":
                return State.UNDEAD;
            default:
                return State.UNDEAD;
        }
    }

    public static String StateString(State state) {
        switch (state) {
            case DEAD:
                return Singleton.getInstance().getContext().getResources().getString(R.string.dead);
            case UNDEAD:
                return Singleton.getInstance().getContext().getResources().getString(R.string.undead);
            default:
                return Singleton.getInstance().getContext().getResources().getString(R.string.undead);
        }
    }

    @Override
    public String toString() {
        String string = "";
        switch (this) {
            case DEAD:
                string = Singleton.getInstance().getContext().getResources().getString(R.string.dead);
                break;
            case UNDEAD:
                string = Singleton.getInstance().getContext().getResources().getString(R.string.undead);
                break;
            default:
                break;
        }
        return string;
    }
}


