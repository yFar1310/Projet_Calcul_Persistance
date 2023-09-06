import java.util.*;
import java.io.*;
import java.math.BigInteger;
import java.net.*;

public class Worker{

    static int port = 8010;
    public static void main(String[] args) throws Exception{
        Socket soc = new Socket("kali", port);
        BufferedReader reader = new BufferedReader(new InputStreamReader(soc.getInputStream()));
        PrintWriter writer = new PrintWriter(new BufferedWriter( new OutputStreamWriter( soc.getOutputStream() ) ), true );

        writer.println("worker");
        int nbCoeurs = Runtime.getRuntime().availableProcessors();
        writer.println(nbCoeurs);
        for(int i = 0; i<10000; i++)
        {
            System.out.println("_________________________");
            System.out.println("Trying to get range ...\n");
            String[] rangeStr = reader.readLine().split(";");
            BigInteger[] range = {new BigInteger(rangeStr[0]), new BigInteger(rangeStr[1])};
            System.out.println("_________________________");
            System.out.println("Range recieved : " + Arrays.toString(range) + "\n");

            // System.out.println("_________________________");
            Moniteur m = new Moniteur(range);
            Tache[] taches = new Tache[nbCoeurs];
            for(int j=0;j<nbCoeurs;j++)
            {
                taches[j] = new Tache(m,reader,writer);
                taches[j].start();
            }
            try
            {
                 for(int j=0;j<nbCoeurs;j++)
                 {
                      taches[j].join();
                 }
            }catch(InterruptedException e){ e.printStackTrace();}
            System.out.println("_________________________");
            
            System.out.println("Informer le serveur que les workers ont fini leurs propres travail 'Terminé' ...\n");
            writer.println("TERMINÉ");
            
        }
        
        System.out.println("_________________________");
        System.out.println("Requesting the command 'QUITTER' ...\n");
        writer.println("QUITTER");
        reader.close();
        writer.close();

        soc.close();
    }
}

class Computer
{
    private String getProductOf(String m){
        int product = 1;
        for (int i = 0; i < m.length(); i++)
        {
            int digit = Character.getNumericValue(m.charAt(i));
            if (digit == 0) return "0";
            product *= digit;
        }
        return Integer.toString(product);
    }
    public int calculatePersistanceOf(String n)
    {
        int p = 0;
        while (n.length() > 1){
            n = getProductOf(n);
            p++;
        }
        return p;
    }   
}


class Moniteur{

    private ArrayList<BigInteger[]> numberPersistanceList;
    private BigInteger nextNum;
    private BigInteger[] range;

    public Moniteur(BigInteger[] range){
        this.range = range;
        this.nextNum = range[0];
        this.numberPersistanceList  = new ArrayList<BigInteger[]>();
    }
    public BigInteger getnextNum()
    {
        return this.nextNum;
    }
    public BigInteger[] getRange()
    {
        return this.range;
    }
    public ArrayList<BigInteger[]> getNumberPersistanceList()
    {
        return this.numberPersistanceList;
    }

    public synchronized void addPair(BigInteger[] pair)
{
    BigInteger[] bigPair = { (pair[0]), (pair[1]) };
    numberPersistanceList.add(bigPair);
}

    //À détaillé plus dans le rapport technique
    public synchronized BigInteger incrementNextNum() {
        if (nextNum.compareTo(range[1]) <= 0) {
            nextNum = nextNum.add(BigInteger.ONE);
            return nextNum.subtract(BigInteger.ONE);
        } else {
            return BigInteger.valueOf(-1);
        }
    }
    

}

//La classe pour implémenter le Thread (Worker).
class Tache extends Thread{

    private Moniteur m;
    private PrintWriter writer; 
    private Computer c = new Computer();
    private boolean ok=true;

    public Tache(Moniteur m, BufferedReader reader, PrintWriter writer){
        this.m = m;
        this.writer = writer;
    }

    public void run(){

        while(ok){// boucle infinie tant que ok = true.
            BigInteger number = m.incrementNextNum();// En premier tant on affecte le premier nombre à l'entier number
            if(!number.equals(BigInteger.valueOf(-1)))
            {
                int persistance = c.calculatePersistanceOf(number.toString());
                System.out.println("\n" + getName()+" : " + number + " - " + persistance);
                BigInteger[] pair = {number, BigInteger.valueOf(persistance)};
                m.addPair(pair);
                writer.println(number + "," +persistance);
            } 
            else ok=false;
        }

    }

}
