package pt.ipleiria.zombienomicon;

import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;

import pt.ipleiria.zombienomicon.Model.Gender;
import pt.ipleiria.zombienomicon.Model.Singleton;
import pt.ipleiria.zombienomicon.Model.Zombie;

import static pt.ipleiria.zombienomicon.AddActivity.PT_IPLEIRIA_ZOMBIENOMICOM_OLD_ZOMBIE;
import static pt.ipleiria.zombienomicon.AddActivity.PT_IPLEIRIA_ZOMBIENOMICON_NEW_ZOMBIE;

/**
 * Class da atividade activity_search que permite procurar todos os zombies com um determinado nome
 */

public class SearchActivity extends AppCompatActivity {
    private ListView list_search;
    private ArrayList<Zombie> searchedZombies;
    private ArrayAdapter<Zombie> adapter;
    private EditText editText_search;
    private Button button_date;
    private int spinner_search_position;
    private int spinner_gender_position;
    private GregorianCalendar searched_date = new GregorianCalendar(10, 1, 1);
    private Gender searched_gender;

    /**
     * Método de callback chamado quando se cria a atividade
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        list_search = (ListView) findViewById(R.id.listView_search);
        searchedZombies = new ArrayList<>();


        /**
         * Quando se clica num Item da ListView inicia-se AddActivity para Editar o Zombie
         */

        list_search.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Zombie z = (Zombie) parent.getItemAtPosition(position);
                Intent i = new Intent(SearchActivity.this, AddActivity.class);
                i.putExtra(MainActivity.PT_IPLEIRIA_ZOMBIENOMICON_EDIT_ZOMBIE, z);
                startActivityForResult(i, MainActivity.REQUEST_CODE_SEARCH);
            }
        });

        /**
         * Quando se faz um long click  num Item da ListView cria-se um AlertDialog a perguntar ao
         * utilizador se pretende eliminar o Zombie nessa posição
         * Caso o utilizador escolha "SIM", o Zombie é eliminado; caso escolha "NÂO" contrário não
         * acontece nada
         */
        list_search.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {

                AlertDialog.Builder deleteConfirmation = new AlertDialog.Builder(SearchActivity.this);
                deleteConfirmation.setTitle(R.string.delete_zombie);
                deleteConfirmation.setMessage(R.string.confirm_delete_zombie);
                deleteConfirmation.setPositiveButton(
                        R.string.yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                /**
                                 * Visto que a posição na lista pode não corresponder à posição na
                                 * Zombienomicon, é necessário arranjar essa posiçºao através do ID
                                 * do Zombie carregado
                                 */
                                int zombie_position = Singleton.getInstance().getZombienomicon().searchPositionByID(searchedZombies.get(position).getId());
                                Singleton.getInstance().getZombienomicon().deleteZombie(zombie_position);
                                searchedZombies.remove(position);
                                adapter.notifyDataSetChanged();
                            }
                        });

                deleteConfirmation.setNegativeButton(
                        R.string.no,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                deleteConfirmation.show();
                return true;
            }
        });

        /**
         * Cria referência para o spinner
         * Associa ao spinner um array de items
         * Cria o adaptador para o spinner e associa-o ao mesmo
         */
        Spinner spinner_search = (Spinner) findViewById(R.id.spinner_search);
        ArrayAdapter<CharSequence> spinner_adapter = ArrayAdapter.createFromResource(this, R.array.search_array, android.R.layout.simple_spinner_item);
        spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_search.setAdapter(spinner_adapter);

        final Spinner spinner_gender = (Spinner) findViewById(R.id.spinner_gender);
        ArrayAdapter<CharSequence> gender_spinner_adapter = ArrayAdapter.createFromResource(this, R.array.gender_array, android.R.layout.simple_spinner_item);
        gender_spinner_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_gender.setAdapter(gender_spinner_adapter);

        /**
         * Cria um método de callback Listener para quando se carrega num dos Items do spinner
         */
        spinner_search.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position_search, long id) {
                editText_search = (EditText) findViewById(R.id.editText_search);
                button_date = (Button) findViewById(R.id.button_date);

                Button search_button = (Button) findViewById(R.id.button_search);
                /**
                 * Vai buscar qual a posição do Item selecionado no spinner
                 */
                spinner_search_position = position_search;

                switch (spinner_search_position) {
                    case 0: // Search
                        /**
                         * Caso seja Search:
                         *          o botão de procurar está desativado
                         *          a editText está desativada
                         *          o spinner de género está invisivel
                         *          o botão da data esta invisivel
                         */
                        searchedZombies.clear();
                        search_button.setEnabled(false);
                        editText_search.setEnabled(false);
                        editText_search.setText("");
                        spinner_gender.setVisibility(View.INVISIBLE);
                        editText_search.setVisibility(View.VISIBLE);
                        editText_search.setHint(R.string.select_search);
                        break;
                    case 1: // Search by Id
                        /**
                         * Caso seja Search by ID:
                         *          o botão de procurar está ativo
                         *          a editText está ativa
                         *          o spinner de género está invisivel
                         *          o botão da data esta invisivel
                         */
                        searchedZombies.clear();
                        search_button.setEnabled(true);
                        editText_search.setEnabled(true);
                        editText_search.setInputType(InputType.TYPE_CLASS_NUMBER);
                        editText_search.setText("");
                        spinner_gender.setVisibility(View.INVISIBLE);
                        editText_search.setVisibility(View.VISIBLE);
                        button_date.setVisibility(View.INVISIBLE);
                        editText_search.setHint(R.string.id_to_search);
                        /**
                         * Limita a entrada de texto a 9 digitos
                         */
                        editText_search.setFilters(new InputFilter[]{new InputFilter.LengthFilter(9)});
                        break;
                    case 2: // Search by Name
                        /**
                         * Caso seja Search by Name:
                         *          o botão de procurar está ativo
                         *          a editText está ativa
                         *          o spinner de género está invisivel
                         *          o botão da data esta invisivel
                         */
                        searchedZombies.clear();
                        search_button.setEnabled(true);
                        editText_search.setEnabled(true);
                        editText_search.setInputType(InputType.TYPE_CLASS_TEXT);
                        editText_search.setText("");
                        spinner_gender.setVisibility(View.INVISIBLE);
                        editText_search.setVisibility(View.VISIBLE);
                        button_date.setVisibility(View.INVISIBLE);
                        editText_search.setHint(R.string.name_to_search);
                        /**
                         * Remove a limitação de entrada imposta no caso anterior
                         */
                        editText_search.setFilters(new InputFilter[]{});
                        break;
                    case 3: // Search by gender
                        /**
                         * Caso seja Search by gender:
                         *          o botão de procurar está ativo
                         *          a editText está invisivel
                         *          o spinner de género está visivel
                         *          o botão da data esta invisivel
                         */
                        searchedZombies.clear();
                        search_button.setEnabled(true);
                        button_date.setVisibility(View.INVISIBLE);
                        spinner_gender.setVisibility(View.VISIBLE);
                        editText_search.setEnabled(false);
                        editText_search.setVisibility(View.INVISIBLE);
                        break;
                    case 4: // Search by Detection Date
                        /**
                         * Caso seja Search by Date:
                         *          o botão de procurar está ativo
                         *          a editText está invisivel
                         *          o spinner de género está invisivel
                         *          o botão da data esta visivel
                         */
                        searchedZombies.clear();
                        DialogFragment newFragment = new DatePickerFragment();
                        Bundle args = new Bundle();
                        args.putInt("detection", 3);
                        newFragment.setArguments(args);
                        newFragment.show(getFragmentManager(), "Date Picker");

                        search_button.setEnabled(true);
                        spinner_gender.setVisibility(View.INVISIBLE);
                        editText_search.setEnabled(false);
                        editText_search.setVisibility(View.INVISIBLE);
                        button_date.setVisibility(View.VISIBLE);
                        break;
                    case 5: //Search by All
                        /**
                         * Caso seja Search by All:
                         *          o botão de procurar está ativo
                         *          a editText está ativa
                         *          o spinner de género está invisivel
                         *          o botão da data esta invisivel
                         */
                        searchedZombies.clear();
                        search_button.setEnabled(true);
                        editText_search.setEnabled(true);
                        editText_search.setInputType(InputType.TYPE_CLASS_TEXT);
                        editText_search.setText("");
                        spinner_gender.setVisibility(View.INVISIBLE);
                        editText_search.setVisibility(View.VISIBLE);
                        button_date.setVisibility(View.INVISIBLE);
                        editText_search.setHint(R.string.name_to_search);
                        /**
                         * Remove a limitação de entrada imposta no caso do id
                         */
                        editText_search.setFilters(new InputFilter[]{});
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        /**
         * Spinner do género
         */
        spinner_gender.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position_gender, long id) {
                spinner_gender_position = position_gender;

                switch (spinner_gender_position) {
                    case 0: // Male
                        searched_gender = Gender.MALE;
                        break;
                    case 1: // Female
                        searched_gender = Gender.FEMALE;
                        break;
                    case 2: // Undefined
                        searched_gender = Gender.UNDEFINED;
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    /**
     * Método de callback quando se carrega no botão de procurar
     */
    public void button_searchOnClick(View view) {
        EditText editText_search = (EditText) findViewById(R.id.editText_search);
        String searched_text = editText_search.getText().toString();
        int var = 0;

        /**
         * Consoante a posição do spinner, procura nos campos pretendidos
         */
        switch (spinner_search_position) {
            case 1: // Search by Id
                try {
                    if (!editText_search.getText().toString().isEmpty()) {
                        searchedZombies = new ArrayList<>();
                        searchedZombies = Singleton.getInstance().getZombienomicon().searchZombieContainingId(searched_text);
                    } else {
                        var = 1;
                        Toast.makeText(this, R.string.empty_id, Toast.LENGTH_SHORT).show();
                    }
                } catch (IllegalArgumentException e) {
                    var = 1;
                    Toast.makeText(SearchActivity.this, R.string.invalid_id, Toast.LENGTH_SHORT).show();
                }
                break;
            case 2: // Search by Name
                if (!editText_search.getText().toString().isEmpty()) {
                    searchedZombies = Singleton.getInstance().getZombienomicon().searchZombieByName(searched_text);
                } else {
                    var = 1;
                    Toast.makeText(this, R.string.empty_name, Toast.LENGTH_SHORT).show();
                }
                break;
            case 3: // Search by Gender
                searchedZombies = Singleton.getInstance().getZombienomicon().searchZombieByGender(searched_gender);
                break;
            case 4: // Search by Detection Date
                GregorianCalendar dateVerifier = new GregorianCalendar(10, 1, 1);
                if (!searched_date.equals(dateVerifier)) {
                    searchedZombies = Singleton.getInstance().getZombienomicon().searchZombieByDetectionDate(searched_date);
                } else {
                    var = 1;
                    Toast.makeText(this, R.string.empty_date, Toast.LENGTH_SHORT).show();
                }
                break;
            case 5: // Search by All
                searchedZombies = Singleton.getInstance().getZombienomicon().searchZombieByAll(searched_text);
                break;
            default:
                break;
        }
        /**
         * Apresenta os resultados da pesquisa na listView
         */
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, searchedZombies);
        list_search.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        /**
         * Caso a lista esteja vazia, envia uma Toast
         */
        if (searchedZombies.isEmpty() && var == 0) {
            Toast.makeText(SearchActivity.this, R.string.list_empty, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Método de callback chamado quando uma atividade devolve algo
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        /**
         * Visto que os campos de um Zombie podem ser alterados através desta atividade, é necessário
         * colocar o método onActivityResult.
         * A edição do Zombie é feita da mesma forma que na MainActivity
         */
        if (requestCode == MainActivity.REQUEST_CODE_SEARCH) {
            if (resultCode == RESULT_OK) {
                try {
                    Zombie new_zombie = (Zombie) data.getSerializableExtra(PT_IPLEIRIA_ZOMBIENOMICON_NEW_ZOMBIE);
                    Zombie old_zombie = (Zombie) data.getSerializableExtra(PT_IPLEIRIA_ZOMBIENOMICOM_OLD_ZOMBIE);
                    Singleton.getInstance().getZombienomicon().editZombie(new_zombie, old_zombie);
                    adapter.notifyDataSetChanged();
                } catch (IllegalArgumentException e) {
                    Toast.makeText(this, R.string.id_already_exists, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /**
     * Caso a data escolhida no DatePicker seja no futuro o utilizador é notificado
     * Só é possivel carregar no botão PROCURAR se a data for aceite
     */
    public void searchZombiesDate(GregorianCalendar c) {
        GregorianCalendar now = new GregorianCalendar();
        Button searchButton = (Button) findViewById(R.id.button_search);

        if (c.compareTo(now) <= 0) {
            searched_date = c;

            SimpleDateFormat format = new SimpleDateFormat("dd-MMM-yyyy");
            format.setCalendar(searched_date);
            String dateFormatted = format.format(searched_date.getTime());
            button_date.setText(dateFormatted);
            searchButton.setEnabled(true);
        } else {
            Toast.makeText(this, getString(R.string.invalid_date), Toast.LENGTH_SHORT).show();
            button_date.setText(R.string.date);
            searchButton.setEnabled(false);
        }
    }

    /**
     * Caso se carregue no botão "back", a Atividade coloca RESULT_CANCELED, para que na MainActivity
     * se atualize a listView consoante a Zombienomicon
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResult(RESULT_CANCELED);
        finish();
    }

    /**
     * Quando se carrega no botão da data abre-se um novo datePicker que permite alterar a data
     * que se pretende procurar
     */
    public void ButtonDateOnClick(View view) {
        DialogFragment newFragment = new DatePickerFragment();
        Bundle args = new Bundle();
        args.putInt("detection", 3);
        args.putSerializable("date", searched_date);
        newFragment.setArguments(args);
        newFragment.show(getFragmentManager(), "Date Picker");
    }


}
