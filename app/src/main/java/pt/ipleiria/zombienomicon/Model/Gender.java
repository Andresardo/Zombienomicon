package pt.ipleiria.zombienomicon.Model;

import pt.ipleiria.zombienomicon.R;

public enum Gender {
    MALE, FEMALE, UNDEFINED;

    public static Gender StringGender(String string) {
        switch (string.toUpperCase()) {
            case "MALE":
            case "MACHO":
                return Gender.MALE;
            case "FEMALE":
            case "FÊMEA":
                return Gender.FEMALE;
            case "UNDEFINED":
            case "INDEFINIDO":
                return Gender.UNDEFINED;
            default:
                return Gender.UNDEFINED;
        }
    }

    @Override
    public String toString() {
        String string = "";
        switch (this) {
            case MALE:
                string = Singleton.getInstance().getContext().getResources().getString(R.string.male);
                break;
            case FEMALE:
                string = Singleton.getInstance().getContext().getResources().getString(R.string.female);
                break;
            case UNDEFINED:
                string = Singleton.getInstance().getContext().getResources().getString(R.string.undefined);
                break;
            default:
                break;
        }
        return string;
    }
}
