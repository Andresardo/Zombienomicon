package pt.ipleiria.zombienomicon;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.widget.DatePicker;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Classe usada para inserir datas
 */
public class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {
    public GregorianCalendar date;

    /**
     * Construtor da classe
     */
    public DatePickerFragment() {
    }

    /**
     * Método que define a data inicial no Dialog
     * Quando se cria o Dialog a data inicial é a data atual
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        int year, month, day;
        GregorianCalendar dateVerifier = new GregorianCalendar(10, 1, 1);

        /**
         * Caso a data recebida seja não nula e diferente da data inicial, o DatePicker é inicializado com essa data
         */
        if (getArguments().getSerializable("date") != null && (!(getArguments().getSerializable("date")).equals(dateVerifier))) {
            year = ((GregorianCalendar) getArguments().getSerializable("date")).get(Calendar.YEAR);
            month = ((GregorianCalendar) getArguments().getSerializable("date")).get(Calendar.MONTH);
            day = ((GregorianCalendar) getArguments().getSerializable("date")).get(Calendar.DAY_OF_MONTH);
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        /**
         * Caso contrário inicia com a data atual
         */
        final Calendar c = Calendar.getInstance();
        year = c.get(Calendar.YEAR);
        month = c.get(Calendar.MONTH);
        day = c.get(Calendar.DAY_OF_MONTH);

        return new DatePickerDialog(getActivity(), this, year, month, day);
    }

    /**
     * Método que permite aceder a métodos noutras classes dependendo da classe (e método) que criou
     * este DatePickerFragment
     */
    @Override
    public void onDateSet(DatePicker view, int year, int month, int day) {
        int var = getArguments().getInt("detection");
        date = new GregorianCalendar(year, month, day);

        switch (var) {
            case 1:
                /**
                 * Caso o DatePicker tenha sido chamado na AddActivity com o objetivo de escolher a
                 * data de deteção, chama-se o método setDateDetection
                 */
                ((AddActivity) getActivity()).setDateDetection(date);
                break;
            case 2:
                /**
                 * Caso o DatePicker tenha sido chamado na AddActivity com o objetivo de escolher a
                 * data de terminação, chama-se o método setDateDetection
                 */
                ((AddActivity) getActivity()).setDateTermination(date);
                break;
            case 3:
                /**
                 * Caso o DatePicker tenha sido chamado na Search com o objetivo de escolher a
                 * data a pesquisar, chama-se o método seacrhZombiesDate
                 */
                ((SearchActivity) getActivity()).searchZombiesDate(date);
                break;
            default:
                break;
        }
    }
}