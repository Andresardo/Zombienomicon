package pt.ipleiria.zombienomicon;

import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

import pt.ipleiria.zombienomicon.Model.Gender;
import pt.ipleiria.zombienomicon.Model.State;
import pt.ipleiria.zombienomicon.Model.Zombie;

public class AddActivity extends AppCompatActivity {

    public static final String PT_IPLEIRIA_ZOMBIENOMICON_NEW_ZOMBIE = "pt.ipleiria.zombienomicon.new_zombie";
    public static final String PT_IPLEIRIA_ZOMBIENOMICOM_OLD_ZOMBIE = "pt.ipleiria.zombienomicom.old.zombie";
    private Zombie old_zombie;
    private boolean edit = false;
    private GregorianCalendar dateDetection = new GregorianCalendar(10, 1, 1);
    private GregorianCalendar dateTermination = new GregorianCalendar(10, 1, 1);
    private State state = null;
    private Gender gender = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);
        final LinearLayout TerminationDate_Layout = (LinearLayout) findViewById(R.id.TerminationDate_Layout);
        final Button add_button = (Button) findViewById(R.id.button_Add);
        /**
         * Caso a atividade receba um Zombie significa que o objetivo é editar
         * Desta forma os campos são preenchidos com os dados do Zombie recebido
         * O botão ADD passa a ser EDIT
         */
        Intent i = getIntent();
        Serializable extra = i.getSerializableExtra(MainActivity.PT_IPLEIRIA_ZOMBIENOMICON_EDIT_ZOMBIE);
        if (extra != null) {
            /**
             * Coloca os parâmetros do Zombie recebido nos respetivos locais (EditText, RadioButton e TextView)
             */
            old_zombie = (Zombie) extra;
            EditText editText_id = (EditText) findViewById(R.id.editText_addId);

            /**
             * Coloca zeros à esquerda na editText
             */
            String idAsString = "" + old_zombie.getId();
            int length = idAsString.length();
            idAsString = "";
            for (int j = 0; j < 9 - length; j++) {
                idAsString += '0';
            }
            idAsString += old_zombie.getId();

            editText_id.setText(idAsString);
            EditText editText_name = (EditText) findViewById(R.id.editText_addName);
            editText_name.setText(old_zombie.getName());
            EditText editText_DetectionLocation_ = (EditText) findViewById(R.id.editText_DetectionLocation);
            editText_DetectionLocation_.setText(old_zombie.getDetection_location());
            /**
             * Caso o estado do Zombie recebido seja morto, coloca o Layout da data de Terminação visivel
             * Caso o estado do Zombie recebido seja morto-vivo, coloca o Layout da data de Terminação invisivel
             */
            if (old_zombie.getState_dead() == State.DEAD) {
                state = State.DEAD;
                RadioButton button_state = (RadioButton) findViewById(R.id.radioButton_Dead);
                button_state.setChecked(true);
                TerminationDate_Layout.setVisibility(View.VISIBLE);
            } else {
                state = State.UNDEAD;
                RadioButton button_state = (RadioButton) findViewById(R.id.radioButton_Undead);
                button_state.setChecked(true);
                TerminationDate_Layout.setVisibility(View.INVISIBLE);
            }
            if (old_zombie.getGender() == Gender.MALE) {
                gender = Gender.MALE;
                RadioButton button_gender = (RadioButton) findViewById(R.id.radioButton_male);
                button_gender.setChecked(true);
            } else if (old_zombie.getGender() == Gender.FEMALE) {
                gender = Gender.FEMALE;
                RadioButton button_gender = (RadioButton) findViewById(R.id.radioButton_female);
                button_gender.setChecked(true);
            } else {
                gender = Gender.UNDEFINED;
                RadioButton button_gender = (RadioButton) findViewById(R.id.radioButton_undefined);
                button_gender.setChecked(true);
            }
            /**
             * De forma a poder mostrar as datas (do tipo GregorianCalendar) numa TextView, é
             * necessário formatá-las
             */
            SimpleDateFormat formatD = new SimpleDateFormat("dd-MMM-yyyy");
            formatD.setCalendar(old_zombie.getDetection_date());
            String dateFormattedD = formatD.format(old_zombie.getDetection_date().getTime());
            dateDetection = old_zombie.getDetection_date();
            TextView detectionDate_Text = (TextView) findViewById(R.id.textView_DetectionDate);
            detectionDate_Text.setText(dateFormattedD);

            GregorianCalendar dateVerifier = new GregorianCalendar(10, 1, 1);
            if (!old_zombie.getTermination_date().equals(dateVerifier)) {
                SimpleDateFormat formatT = new SimpleDateFormat("dd-MMM-yyyy");
                formatT.setCalendar(old_zombie.getTermination_date());
                String dateFormattedT = formatT.format(old_zombie.getTermination_date().getTime());
                dateTermination = old_zombie.getTermination_date();
                TextView terminationDate_Text = (TextView) findViewById(R.id.textView_TerminationDate);
                terminationDate_Text.setText(dateFormattedT);
            }

            /**
             * Coloca a variavel edit a true, para posteriormente saber se é necessário enviar um
             * Zombie (caso se tenha iniciado a atividade para adicionar) ou dois (caso seja editar)
             * Muda-se também o texto do botão
             */
            edit = true;
            add_button.setText(R.string.edit);
        } else {
            /**
             * Se não se recebeu nenhum Zombie, nas TextView das datas aparece que ainda não foram adicionadas
             */
            TextView detectionDate_Text = (TextView) findViewById(R.id.textView_DetectionDate);
            detectionDate_Text.setText(R.string.no_date_yet);
            TextView terminationDate_Text = (TextView) findViewById(R.id.textView_TerminationDate);
            terminationDate_Text.setText(R.string.no_date_yet);
        }

        /**
         * Este método é chamado sempre que se altera qual o botão selecionado no radio_group do estado
         * Dependendo de qual o novo estado, o layout da data de Terminação fica visivel ou invisivel
         */
        RadioGroup radioGroup_state = (RadioGroup) findViewById(R.id.radioGroup_state);
        radioGroup_state.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.radioButton_Dead:
                        TerminationDate_Layout.setVisibility(View.VISIBLE);
                        state = State.DEAD;
                        break;
                    case R.id.radioButton_Undead:
                        TerminationDate_Layout.setVisibility(View.INVISIBLE);
                        state = State.UNDEAD;
                        dateTermination = new GregorianCalendar(10, 1, 1);
                        TextView terminationDate_Text = (TextView) findViewById(R.id.textView_TerminationDate);
                        terminationDate_Text.setText(R.string.no_date_yet);
                        break;
                    default:
                        break;
                }
            }
        });

        /**
         * Este método é chamado sempre que se altera qual o botão selecionado no radio_group do género
         */
        RadioGroup radioGroup_gender = (RadioGroup) findViewById(R.id.radioGroup_gender);
        radioGroup_gender.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.radioButton_male:
                        gender = Gender.MALE;
                        break;
                    case R.id.radioButton_female:
                        gender = Gender.FEMALE;
                        break;
                    case R.id.radioButton_undefined:
                        gender = Gender.UNDEFINED;
                        break;
                    default:
                        break;
                }
            }
        });
    }

    /**
     * Método de callback chamado quando se pressiona o botão de adicionar (ou editar)
     */
    public void button_AddOnClick(View view) {
        /**
         * Cria referências para as diferentes EditText
         */
        EditText editText_AddName = (EditText) findViewById(R.id.editText_addName);
        EditText editText_AddId = (EditText) findViewById(R.id.editText_addId);
        EditText editText_AddDetectionLocation = (EditText) findViewById(R.id.editText_DetectionLocation);

        /**
         * Inicialmente as datas foram inicialzadas com a data 1-Fev-0010 (10,1,1)
         * A GregorianCalendar aqui criada serve para verificar se as datas foram alteradas
         */
        GregorianCalendar dateVerifier = new GregorianCalendar(10, 1, 1);
        /**
         * Neste if é verificado se as EditText não estão vazias, assim como se o estado e o género são
         * diferentes de "" (pois foram assim que foram inicializados.
         * Para além disso verifica se a data de Deteção foi alterada
         */
        if (!editText_AddName.getText().toString().isEmpty()
                && !editText_AddId.getText().toString().isEmpty()
                && !editText_AddDetectionLocation.getText().toString().isEmpty()
                && state!=null && gender != null
                && !dateDetection.equals(dateVerifier)) {
            /**
             * Caso se tenham verificado as condições anteriores, e se o estado for morto ("Dead"), é
             * necessário verificar se a data de Terminação foi alterada
             */
            if (state == State.DEAD && dateTermination.equals(dateVerifier)) {
                /**
                 * Caso a data não tenha sido alterada, o utilizador é notificado de tal e continua
                 * nesta atividade
                 */
                Toast.makeText(this, R.string.empty_fields, Toast.LENGTH_SHORT).show();
            } else {
                /**
                 * Verifica se o ID introduzido tem 9 dígitos. Caso contrário mostra essa informação
                 * ao utilizador
                 */
                if (editText_AddId.getText().length() == 9) {
                    /**
                     * Se todos os campos estão preenchidos, prossegue-se à criação do novo Zombie recolhendo
                     * os dados das editText
                     */
                    String name = editText_AddName.getText().toString();
                    String id_string = editText_AddId.getText().toString();
                    int id = Integer.parseInt(id_string);
                    String detectionLocation = editText_AddDetectionLocation.getText().toString();

                    final Zombie new_zombie = new Zombie(id, dateDetection, dateTermination, name, gender, detectionLocation, state);

                    /**
                     * Aqui é criado um DialogAlert que pergunta ao utilizador se pretende mesmo adicionar este Zombie
                     * A mensagem é diferente caso a atividade tenha sido criada com o intuito de adicionar
                     * ou de editar
                     */
                    final AlertDialog.Builder editConfirmation = new AlertDialog.Builder(AddActivity.this);
                    if (edit) {
                        editConfirmation.setTitle(R.string.edit_zombie);
                        editConfirmation.setMessage(R.string.confirm_edit_zombie);
                    } else {
                        editConfirmation.setTitle(R.string.add_zombie);
                        editConfirmation.setMessage(R.string.confirm_add_zombie);
                    }
                    editConfirmation.setPositiveButton(
                            R.string.yes,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    /**
                                     * Se o utilizador pretender adicionar o novo Zombie, cria-se um novo
                                     * Intent para enviar um Zombie (Adicionar) ou dois (Editar) para a MainActivity
                                     */
                                    Intent i = new Intent();
                                    i.putExtra(PT_IPLEIRIA_ZOMBIENOMICON_NEW_ZOMBIE, new_zombie);
                                    if (edit) {
                                        i.putExtra(PT_IPLEIRIA_ZOMBIENOMICOM_OLD_ZOMBIE, old_zombie);
                                    }
                                    MediaPlayer sound = MediaPlayer.create(AddActivity.this, R.raw.zombie_add);
                                    sound.start();
                                    setResult(RESULT_OK, i);
                                    finish();
                                }
                            });

                    editConfirmation.setNegativeButton(
                            R.string.no,
                            new DialogInterface.OnClickListener() {
                                /**
                                 * Se o utiliozador pressionar "Não", o Zombie não é adicionado e continua
                                 * na mesma atividade
                                 */
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
                    editConfirmation.show();

                } else {
                    Toast.makeText(this, R.string.id_9_digit, Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            Toast.makeText(this, R.string.empty_fields, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Quando se pressiona o botão da data de Deteção, cria-se um DialogFragment que permite ao
     * utilizador selecionar a data pretendida
     */
    public void DateDetectionOnClick(View view) {
        DialogFragment newFragment = new DatePickerFragment();
        Bundle args = new Bundle();
        args.putInt("detection", 1);
        args.putSerializable("date", dateDetection);
        newFragment.setArguments(args);
        newFragment.show(getFragmentManager(), "Date Picker");
    }

    /**
     * Quando se pressiona o botão da data de Terminação, cria-se um DialogFragment que permite ao
     * utilizador selecionar a data pretendida
     */
    public void DateTerminationOnClick(View view) {
        DialogFragment newFragment = new DatePickerFragment();
        Bundle args = new Bundle();
        args.putInt("detection", 2);
        args.putSerializable("date", dateTermination);
        newFragment.setArguments(args);
        newFragment.show(getFragmentManager(), "Date Picker");
    }

    /**
     * Método chamado a partir da classe DatePickerFragment que permite atualizar a data de Deteção
     */
    public void setDateDetection(GregorianCalendar dateDetection) {
        GregorianCalendar now = new GregorianCalendar();

        /**
         * Caso a data introduzida for válida (no passado ou presente), atualiza a data de Deteção
         * e coloca-a de forma formatada na TextView
         */
        if (dateDetection.compareTo(now) <= 0) {
            this.dateDetection = dateDetection;

            SimpleDateFormat format = new SimpleDateFormat("dd-MMM-yyyy");
            format.setCalendar(dateDetection);
            String dateFormatted = format.format(dateDetection.getTime());

            TextView detectionDate_Text = (TextView) findViewById(R.id.textView_DetectionDate);
            detectionDate_Text.setText(dateFormatted);

        }
        /**
         * Caso contrário alerta o utilizador
         */
        else {
            Toast.makeText(this, R.string.invalid_date, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Método chamado a partir da classe DatePickerFragment que permite atualizar a data de Terminação
     */
    public void setDateTermination(GregorianCalendar dateTermination) {
        GregorianCalendar now = new GregorianCalendar();

        /**
         * Caso a data introduzida for válida (no passado ou presente), atualiza a data de Terminação
         * e coloca-a de forma formatada na TextView
         */
        if (dateTermination.compareTo(now) <= 0) {
            this.dateTermination = dateTermination;

            SimpleDateFormat format = new SimpleDateFormat("dd-MMM-yyyy");
            format.setCalendar(dateTermination);
            String dateFormatted = format.format(dateTermination.getTime());

            TextView terminationDate_Text = (TextView) findViewById(R.id.textView_TerminationDate);
            terminationDate_Text.setText(dateFormatted);
        }
        /**
         * Caso contrário alerta o utilizador
         */
        else {
            Toast.makeText(this, getString(R.string.invalid_date), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Guarda o estado dos campos quando a orientação do ecrã é alterada
     */
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current game state
        savedInstanceState.putSerializable("dateDetection", dateDetection);
        savedInstanceState.putSerializable("dateTermination", dateTermination);
        savedInstanceState.putSerializable("gender", gender);
        savedInstanceState.putSerializable("state", state);
        savedInstanceState.putBoolean("edit", edit);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * Carrega o estado dos campos quando a orientação do ecrã é alterada
     */
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // Always call the superclass so it can restore the view hierarchy
        super.onRestoreInstanceState(savedInstanceState);

        // Restore state members from saved instance
        dateDetection = (GregorianCalendar) savedInstanceState.getSerializable("dateDetection");
        dateTermination = (GregorianCalendar) savedInstanceState.getSerializable("dateTermination");
        gender = (Gender) savedInstanceState.getSerializable("gender");
        state = (State) savedInstanceState.getSerializable("state");
        edit = savedInstanceState.getBoolean("edit");

        GregorianCalendar dateVerifier = new GregorianCalendar(10, 1, 1);
        SimpleDateFormat format = new SimpleDateFormat("dd-MMM-yyyy");
        format.setCalendar(dateDetection);
        String dateFormatted = format.format(dateDetection.getTime());
        TextView detectionDate_Text = (TextView) findViewById(R.id.textView_DetectionDate);
        if (!dateDetection.equals(dateVerifier)) {
            detectionDate_Text.setText(dateFormatted);
        } else {
            detectionDate_Text.setText(R.string.no_date_yet);
        }

        format.setCalendar(dateTermination);
        dateFormatted = format.format(dateTermination.getTime());
        TextView terminationDate_Text = (TextView) findViewById(R.id.textView_TerminationDate);
        if (!dateTermination.equals(dateVerifier)) {
            terminationDate_Text.setText(dateFormatted);
        } else {
            terminationDate_Text.setText(R.string.no_date_yet);
        }
    }
}
