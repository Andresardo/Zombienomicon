package pt.ipleiria.zombienomicon;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import pt.ipleiria.zombienomicon.Model.Gender;
import pt.ipleiria.zombienomicon.Model.Singleton;
import pt.ipleiria.zombienomicon.Model.State;
import pt.ipleiria.zombienomicon.Model.Zombie;
import pt.ipleiria.zombienomicon.Model.Zombienomicon;

import static pt.ipleiria.zombienomicon.AddActivity.PT_IPLEIRIA_ZOMBIENOMICOM_OLD_ZOMBIE;

/**
 * Atividade principal principal
 */
public class MainActivity extends AppCompatActivity {
    public static final String PT_IPLEIRIA_ZOMBIENOMICON_EDIT_ZOMBIE = "pt.ipleiria.zombienomicon.edit.zombie";
    public static final int REQUEST_CODE_SEARCH = 3;
    public static String URL = "http://m.uploadedit.com/ba3s/1481125680307.txt";
    private static final int REQUEST_CODE_ADD = 1;
    private static final int REQUEST_CODE_EDIT = 2;
    private Zombienomicon zombienomicon;
    private ArrayList<Zombie> zombies;
    private String saveFile = "zombienomicon.bin";
    private ListView zombie_list;
    private SimpleAdapter simpleadapter;
    private MenuItem item_all;
    private MenuItem item_dead;
    private MenuItem item_undead;

    /**
     * Método de callback chamado quando se cria a atividade
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Singleton.getInstance().setContext(getApplicationContext());
        /**
         * Tenta fazer o load da lista contida no ficheiro Zombienomicon.bin. Caso não consiga, pode
         * dever-se ao facto de Zombinomicon.bin não ter sido criada ou ao facto de ter ocorrido um
         * erro ao ler do ficheiro.
         */
        try {
            FileInputStream fileInputStream = openFileInput(saveFile);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            Singleton.getInstance().setZombienomicon((Zombienomicon) objectInputStream.readObject());
            objectInputStream.close();
            fileInputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, R.string.no_file_error, Toast.LENGTH_LONG).show();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, R.string.read_error, Toast.LENGTH_LONG).show();
        }

        /**
         * Atribui à variavel Zombienomicon desta atividade a Zombienomicon do Singleton
         */
        zombienomicon = Singleton.getInstance().getZombienomicon();

        /**
         * Cria a toolbar e atribui-a à Atividade
         */
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        /**
         * Cria a referencia para a listview
         * Cria um adapter para a lista de zombies
         * Associa o adapter à lista de zombies
         */
        zombies = zombienomicon.getZombies();
        createSimpleAdapter(zombies);
        zombie_list = (ListView) findViewById(R.id.listView_Zombies);
        zombie_list.setAdapter(simpleadapter);

        /**
         * Quando se clica num Item da ListView Mostra uma AlerDialog com todos os dados do zombie
         * Caso se carregue no botão edit, a AddActivity é iniciada com esses dados
         */
        zombie_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {

                HashMap<String, String> h = (HashMap<String, String>) parent.getItemAtPosition(position);
                final Zombie zombie = zombienomicon.searchZombieByID(Integer.parseInt(h.get("id")));

                AlertDialog.Builder editConfirmation = new AlertDialog.Builder(MainActivity.this);
                editConfirmation.setTitle(R.string.edit_zombie);
                editConfirmation.setMessage(zombie.toString());
                editConfirmation.setPositiveButton(
                        R.string.edit,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent i = new Intent(MainActivity.this, AddActivity.class);
                                i.putExtra(PT_IPLEIRIA_ZOMBIENOMICON_EDIT_ZOMBIE, zombie);
                                startActivityForResult(i, REQUEST_CODE_EDIT);
                            }
                        });

                editConfirmation.setNegativeButton(
                        R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                editConfirmation.show();
            }
        });

        /**
         * Quando se faz um long click  num Item da ListView cria-se um AlertDialog a perguntar ao
         * utilizador se pretende eliminar o Zombie nessa posição
         * Caso o utilizador escolha "SIM", o Zombie é eliminado; caso escolha "NÂO" não
         * acontece nada
         */
        zombie_list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {

                AlertDialog.Builder deleteConfirmation = new AlertDialog.Builder(MainActivity.this);
                deleteConfirmation.setTitle(R.string.delete_zombie);
                deleteConfirmation.setMessage(R.string.confirm_delete_zombie);
                deleteConfirmation.setPositiveButton(
                        R.string.yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                /**
                                 * Dependendo de qual o botão vísivel a lista de Zombies é diferente,
                                 * por isso é necessário separa-los.
                                 */

                                if (item_all.isVisible()) {
                                    zombies = zombienomicon.getZombies();
                                    int zombie_position = zombienomicon.searchPositionByID(zombies.get(position).getId());
                                    Singleton.getInstance().getZombienomicon().deleteZombie(zombie_position);
                                    createSimpleAdapter(zombienomicon.getZombies());
                                    zombie_list = (ListView) findViewById(R.id.listView_Zombies);
                                    zombie_list.setAdapter(simpleadapter);
                                } else if (item_dead.isVisible()) {
                                    zombies = zombienomicon.searchZombieByState(State.DEAD);
                                    int zombie_position = zombienomicon.searchPositionByID(zombies.get(position).getId());
                                    Singleton.getInstance().getZombienomicon().deleteZombie(zombie_position);
                                    createSimpleAdapter(zombienomicon.searchZombieByState(State.DEAD));
                                    zombie_list = (ListView) findViewById(R.id.listView_Zombies);
                                    zombie_list.setAdapter(simpleadapter);
                                } else {
                                    zombies = zombienomicon.searchZombieByState(State.UNDEAD);
                                    int zombie_position = zombienomicon.searchPositionByID(zombies.get(position).getId());
                                    Singleton.getInstance().getZombienomicon().deleteZombie(zombie_position);
                                    createSimpleAdapter(zombienomicon.searchZombieByState(State.UNDEAD));
                                    zombie_list = (ListView) findViewById(R.id.listView_Zombies);
                                    zombie_list.setAdapter(simpleadapter);
                                }

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
    }

    /**
     * Método de callback chamado quando se seleciona um dos botões do menu
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            /**
             * Caso o botão pressionado seja Search inicia a SearchActivity e envia a lista de Zombies
             */
            case R.id.zombie_search:
                Intent i = new Intent(this, SearchActivity.class);
                startActivityForResult(i, REQUEST_CODE_SEARCH);
                break;
            /**
             * Caso o botão pressionado seja Add inicia a AddActivity com o objetivo de receber um Zombie para adicionar à lista
             */
            case R.id.zombie_add:

                final AlertDialog.Builder editConfirmation = new AlertDialog.Builder(this);


                editConfirmation.setTitle("ZVK Test");
                editConfirmation.setMessage("Do you want to do the ZVK test?");


                editConfirmation.setPositiveButton(
                        R.string.yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent i2 = new Intent(MainActivity.this, FaceActivity.class);
                                startActivity(i2);
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
                                Intent i2 = new Intent(MainActivity.this, AddActivity.class);
                                startActivityForResult(i2, REQUEST_CODE_ADD);
                            }
                        });
                editConfirmation.setCancelable(true);
                editConfirmation.show();


                break;
            /**
             * Caso o botão visivel seja o zombie_all (mão e RIP) a listview apresenta todos os Zombies.
             * Quando se clica no botão, o botão visivel passa a ser zombie_dead (RIP) e a listview
             * passa a mostrar todos os Zombies cujo estado seja "Dead"
             */
            case R.id.zombie_all:
                item_all.setVisible(false);
                item_dead.setVisible(true);
                item_undead.setVisible(false);
                createSimpleAdapter(zombienomicon.searchZombieByState(State.DEAD));
                zombie_list.setAdapter(simpleadapter);
                break;
            /**
             * Caso o botão visivel seja o zombie_dead (RIP) a listview apresenta todos os Zombies
             * cujo estado seja "Dead".
             * Quando se clica no botão, o botão visivel passa a ser zombie_undead (mão) e a listview
             * passa a mostrar todos os Zombies cujo estado seja "Undead"
             */
            case R.id.zombie_dead:
                item_all.setVisible(false);
                item_dead.setVisible(false);
                item_undead.setVisible(true);
                createSimpleAdapter(zombienomicon.searchZombieByState(State.UNDEAD));
                zombie_list.setAdapter(simpleadapter);
                break;
            /**
             * Caso o botão visivel seja o zombie_undead (mão) a listview apresenta todos os Zombies
             * cujo estado seja "Undead".
             * Quando se clica no botão, o botão visivel passa a ser zombie_all(mão e RIP) e a listview
             * passa a mostrar todos os Zombies
             */
            case R.id.zombie_undead:
                item_all.setVisible(true);
                item_dead.setVisible(false);
                item_undead.setVisible(false);
                createSimpleAdapter(zombienomicon.getZombies());
                zombie_list.setAdapter(simpleadapter);
                break;
            /**
             * Quando se pressiona a opção read from network, é lido o ficheiro com o URL definido
             */
            case (R.id.read_network):
                ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                if (networkInfo != null && networkInfo.isConnected()) {
                    DownloadContactsTask downloadContactsTask = new DownloadContactsTask();
                    downloadContactsTask.execute(URL);
                } else {
                    Toast.makeText(MainActivity.this, "Error: no network connection.", Toast.LENGTH_SHORT).show();
                }
                break;
            /**
             * Quando se pressiona a opção configure URL, aparece um AlertDialog com uma EditText
             * que permite ao utilizador introduzir o seu próprio URL, de forma a ser lida a sua informação
             */
            case (R.id.configure_url):
                AlertDialog.Builder alert = new AlertDialog.Builder(this);

                alert.setTitle("Configure URL");
                alert.setMessage("Write URL link in the box below:");
                final EditText input = new EditText(this);
                alert.setView(input);

                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        URL = input.getText().toString();
                    }
                });

                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });

                alert.show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Método de callback chamado quando uma atividade devolve algo
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        /**
         * Caso o requestCode recebido seja da AddActivity (para adicionar), coloca o Zombie recebido na lista
         */
        if (requestCode == REQUEST_CODE_ADD) {
            if (resultCode == RESULT_OK) {
                try {
                    Zombie zombie = (Zombie) data.getSerializableExtra(AddActivity.PT_IPLEIRIA_ZOMBIENOMICON_NEW_ZOMBIE);
                    zombienomicon.addZombie(zombie);
                    Singleton.getInstance().setZombienomicon(zombienomicon);

                    /**
                     * Quando se volta de adicionar, apresenta novamente a lista completa dos Zombies
                     */
                    createSimpleAdapter(zombienomicon.getZombies());
                    zombie_list.setAdapter(simpleadapter);

                } catch (IllegalArgumentException e) {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }
        /**
         * Caso o requestCode recebido seja da AddActivity(para editar), altera os parâmetros do Zombie pretendido
         */
        if (requestCode == REQUEST_CODE_EDIT) {
            if (resultCode == RESULT_OK) {
                try {
                    Zombie new_zombie = (Zombie) data.getSerializableExtra(AddActivity.PT_IPLEIRIA_ZOMBIENOMICON_NEW_ZOMBIE);
                    Zombie old_zombie = (Zombie) data.getSerializableExtra(PT_IPLEIRIA_ZOMBIENOMICOM_OLD_ZOMBIE);
                    zombienomicon.editZombie(new_zombie, old_zombie);
                    Singleton.getInstance().setZombienomicon(zombienomicon);

                    createSimpleAdapter(zombienomicon.getZombies());
                    zombie_list.setAdapter(simpleadapter);
                } catch (IllegalArgumentException e) {
                    Toast.makeText(this, R.string.id_already_exists, Toast.LENGTH_SHORT).show();
                }
            }
        }
        /**
         * Caso o requestCode recebido seja da SearchActivity apenas atualiza a lista
         */
        if (requestCode == REQUEST_CODE_SEARCH) {
            if (resultCode == RESULT_CANCELED) {

                createSimpleAdapter(zombienomicon.getZombies());
                zombie_list.setAdapter(simpleadapter);
            }
        }
    }

    /**
     * Método de callback que permite guardar a Zombienomicon atual no ficheiro zombienomicon.bin
     */
    @Override
    protected void onPause() {
        super.onPause();
        try {
            FileOutputStream fileOutputStream = openFileOutput(saveFile, Context.MODE_PRIVATE);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(zombienomicon);
            objectOutputStream.close();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, R.string.write_error, Toast.LENGTH_LONG).show();
        }
    }


    /**
     * Método que permite criar um SimpleAdapter de forma a que em cada Item da ListView apenas apareça
     * o nome e estado do Zombie
     */
    private void createSimpleAdapter(ArrayList<Zombie> list) {
        List<HashMap<String, String>> simpleAdapterData = new ArrayList<>();

        for (Zombie z : list) {
            HashMap<String, String> hashMap = new HashMap<>();

            hashMap.put("name", z.getName());
            hashMap.put("state", State.StateString(z.getState_dead()));
            hashMap.put("id", "" + z.getId());

            simpleAdapterData.add(hashMap);
        }

        String[] from = {"name", "state"};
        int[] to = {R.id.textView_name, R.id.textView_state};
        simpleadapter = new SimpleAdapter(getBaseContext(), simpleAdapterData, R.layout.listview_item, from, to);
    }

    /**
     * Método de callback chamado quando se cria o menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        item_all = menu.findItem(R.id.zombie_all);
        item_dead = menu.findItem(R.id.zombie_dead);
        item_undead = menu.findItem(R.id.zombie_undead);

        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Quando volta de outras atividades, coloca como visível o botão de filtro "All"
     */
    @Override
    protected void onRestart() {
        super.onResume();
        item_all.setVisible(true);
        item_dead.setVisible(false);
        item_undead.setVisible(false);

        createSimpleAdapter(zombienomicon.getZombies());
        zombie_list.setAdapter(simpleadapter);
    }

    private String readStream(InputStream is) {
        StringBuilder sb = new StringBuilder(512);
        try {
            Reader r = new InputStreamReader(is, "UTF-8");
            int c;
            while ((c = r.read()) != -1) {
                sb.append((char) c);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sb.toString();
    }

    /**
     * Método que lê o ficheiro de texto no URL e adiciona os Zombies à lista
     */
    private void parseZombie(String text) {
        String[] lines = text.split("\n");
        for (String line : lines) {
            /**
             * Para cada linha, se não for nula, se não começar por "#" e se começar por "*"
             */
            if (line != null && !line.startsWith("#") && line.startsWith("*") && !line.trim().isEmpty()) {
                /**
                 * É feito um split por ":"
                 */
                String[] split = line.split(":");
                int zombieId = Integer.parseInt(split[1]);
                /**
                 * Verifica se já existe um Zombie com o Id lido
                 */
                if (zombienomicon.searchZombieByID(zombieId) == null) {
                    /**
                     * Caso não exista adiciona-o à lista com os campos lidos no ficheiro
                     */
                    String zombieName = split[2].trim();
                    Gender zombieGender = Gender.StringGender(split[3].trim());
                    State zombieState = State.StringState(split[4].trim());
                    DateFormat df = new SimpleDateFormat("dd MM yyyy");
                    Date date = null;
                    try {
                        date = df.parse(split[5]);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    GregorianCalendar detectionDate = new GregorianCalendar();
                    detectionDate.setTime(date);
                    String detectionLocation = split[6].trim();
                    GregorianCalendar terminationDate = new GregorianCalendar(10, 1, 1);
                    if (zombieState == State.DEAD) {
                        df = new SimpleDateFormat("dd MM yyyy");
                        date = null;
                        try {
                            date = df.parse(split[7]);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        terminationDate = new GregorianCalendar();
                        terminationDate.setTime(date);
                    }
                    Zombie zombie = new Zombie(zombieId, detectionDate, terminationDate, zombieName, zombieGender, detectionLocation, zombieState);
                    zombienomicon.addZombie(zombie);
                }
            }
        }
    }

    /**
     * AsyncTask que permite ir buscar o conteúdo do ficheiro
     */
    private class DownloadContactsTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            try {
                // establish the connection to the network resource
                URL url = new URL(urls[0]);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setReadTimeout(10000);
                httpURLConnection.setConnectTimeout(15000);
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.setDoInput(true);
                httpURLConnection.connect();
                int responseCode = httpURLConnection.getResponseCode();
                Log.i("Contacts App", "HTTP response code: " + responseCode);
                //retrieve the network resource's content
                InputStream inputStream = httpURLConnection.getInputStream();
                String contentAsString = readStream(inputStream);
                inputStream.close();
                return contentAsString;
            } catch (IOException e) {
                return "ERROR: unable to retrieve web page. URL may be invalid.";
            }
        }

        /**
         * Após ir buscar o ficheiro e colocar os Zombies na lista, atualiza a listView
         */
        @Override
        protected void onPostExecute(String result) {
            if (!result.startsWith("ERROR")) {
                parseZombie(result);
                createSimpleAdapter(zombienomicon.getZombies());
                zombie_list.setAdapter(simpleadapter);
                Toast.makeText(MainActivity.this, "Contacts loaded from network resource!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, result, Toast.LENGTH_LONG).show();
            }
        }
    }
}
