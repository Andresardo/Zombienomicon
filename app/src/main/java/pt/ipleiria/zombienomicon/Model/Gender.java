package pt.ipleiria.zombienomicon.Model;

/**
 * Created by Mário Vala on 17-12-2016.
 */

public enum Gender {
    MALE,FEMALE,UNDEFINED;

    @Override
    public String toString() {
        String string = "";
        switch(this){
            case MALE:
                string = "Male";
                break;
            case FEMALE:
                string = "Female";
                break;
            case UNDEFINED:
                string = "Undefined";
                break;
            default:
                break;
        }
        return string;
    }

    public static Gender StringGender(String string){
        switch(string.toUpperCase()){
            case "MALE":
                return Gender.MALE;
                //break;
            case "FEMALE":
                return Gender.FEMALE;
                //break;
            case "UNDEFINED":
                return Gender.UNDEFINED;
                //break;
            default:
                return Gender.UNDEFINED;
                //break;
        }
    }

}
