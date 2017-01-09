package pt.ipleiria.zombienomicon.Model;

import android.content.Context;

/**
 * Classe que permite que todas as atividades acessem à mesma Zombienomicon
 */
public class Singleton {
    /**
     * Cria um Singleton que armazena a lista de Zombies (Zombienomicon) para que esta possa ser usada
     * em todas as atividades
     */
    private static Singleton ourInstance = new Singleton();
    private Zombienomicon zombienomicon;
    private Context c;

    /**
     * Método que permite criar a instância para este Singleton
     */
    private Singleton() {
        this.zombienomicon = new Zombienomicon();
    }

    /**
     * Método que devolve a instância do Singleton
     */
    public static Singleton getInstance() {
        return ourInstance;
    }

    /**
     * Método que devolve toda a lista de Zombies
     */
    public Zombienomicon getZombienomicon() {
        return zombienomicon;
    }

    /**
     * Método que permite atualizar a Zombienomicon do Singleton com a Zombienomicon recebida
     */
    public void setZombienomicon(Zombienomicon zombienomicon) {
        this.zombienomicon = zombienomicon;
    }

    /**
     * Método que devolve o contexto
     */
    Context getContext() {
        return c;
    }

    /**
     * Método que permite atualizar o contexto do Singleton com o contexto recebido
     */
    public void setContext(Context c) {
        this.c = c;
    }
}