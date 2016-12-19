package pt.ipleiria.zombienomicon.Model;

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
            case "FEMALE":
                return Gender.FEMALE;
            case "UNDEFINED":
                return Gender.UNDEFINED;
            default:
                return Gender.UNDEFINED;
        }
    }

}
