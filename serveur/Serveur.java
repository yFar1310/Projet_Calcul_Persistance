
import java.util.*;
import java.util.Map.Entry;
import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Serveur
{

    static int port = 8010;
    static ConcurrentHashMap<String, WorkerInfo> workers = new ConcurrentHashMap<>();
    static final int maxConx = 20;
    static int numConx = 1;
    static int nbWorkers = 0;
    static int nbClients = 0;
    static ArrayList<Connection> conxList = new ArrayList<Connection>();

    public static void main(String[] args) throws Exception
    {
        
        ServerSocket s = new ServerSocket(port);
        Database data = new Database();

        createStatsFile("stats.txt");

        // Créer un ScheduledExecutorService
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        
        // Définir l'intervalle de temps pour sauvegarder les statistiques
        long saveInterval = 1; // Sauvegarder les statistiques 
        TimeUnit timeUnit = TimeUnit.MINUTES;

        // Exécuter saveStatsToFile périodiquement
        scheduler.scheduleAtFixedRate(() -> saveStatsToFile("stats.txt"), saveInterval, saveInterval, timeUnit);
        while (numConx <= maxConx) {
            System.out.println("____________________________________________________________________________");
            System.out.println("...................En attente d'une nouvelle connexion.....................\n");
            Socket soc = s.accept();
            System.out.println("____________________________________________________________________________");
            Connection c = new Connection(numConx, soc, data);
            System.out.println("Nouvelle connexion, id : " + c.getWorkerId() + "\n");
            conxList.add(c);
            numConx++;
            c.start();
        } 


        

        // Attente de la fin de toutes les connexions
        for (Connection c : conxList) {
            c.join();
        }
       
        scheduler.shutdown();

        s.close();
    }


    public void updateWorkerLoad(String workerId, int newLoad) {
        WorkerInfo worker = workers.get(workerId);
        if (worker != null) {
            worker.setCurrentLoad(newLoad);
        }
    }

    public int getWorkerLoad(String workerId) {
        WorkerInfo worker = workers.get(workerId);
        if (worker != null) {
            return worker.getCurrentLoad();
        }
        return -1;
    }
    public void assignTask() {
        String workerIdWithLowestLoad = null;
        int lowestLoad = Integer.MAX_VALUE;

        for (Map.Entry<String, WorkerInfo> entry : workers.entrySet()) {
            WorkerInfo worker = entry.getValue();
            if (worker.getCurrentLoad() < lowestLoad) {
                lowestLoad = worker.getCurrentLoad();
                workerIdWithLowestLoad = worker.getId();
            }
        }

        if (workerIdWithLowestLoad != null) {
            WorkerInfo worker = workers.get(workerIdWithLowestLoad);
            worker.setCurrentLoad(worker.getCurrentLoad() + 1);

        }
    }
    private static void createStatsFile(String fileName) {
        File statsFile = new File(fileName);
        try {
            if (statsFile.createNewFile()) {
                System.out.println("Fichier de statistiques créé : " + fileName);
            } else {
                System.out.println("Le fichier de statistiques existe déjà : " + fileName);
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la création du fichier de statistiques : " + e.getMessage());
        }
    }
    

    private static void saveStatsToFile(String fileName) {
        try (FileWriter writer = new FileWriter(fileName, false)) {
            writer.write("Statistiques des workers :\n");
            for (Map.Entry<String, WorkerInfo> entry : workers.entrySet()) {
                WorkerInfo worker = entry.getValue();
                writer.write("Worker ID: " + worker.getId() + "\n");
                writer.write("Adresse IP: " + worker.getAddress() + "\n");
                writer.write("Port: " + worker.getPort() + "\n");
                writer.write("Cœurs: " + worker.getCores() + "\n");
                writer.write("Charge actuelle: " + worker.getCurrentLoad() + "\n");
                writer.write("-------------------------------\n");
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de l'enregistrement des statistiques: " + e.getMessage());
        }
    }
    public static void registerWorker(WorkerInfo worker) {
        workers.put(worker.getId(), worker);
    }
    
    
}
class Database{
    
    private String nextInt;
    private int maxP;
    private String defaultRange = "25000";
    //Hashtable pour stocker les résulats ("nombre","persistance") 
    private Hashtable<String,String> nbPersTable = new Hashtable<>();
    private int maxHashSize = 10000;
    private Hashtable<String,String> extraitF = new Hashtable<>();

    public Database(){
        this.nbPersTable = new Hashtable<>();
        this.nextInt = "0";
        this.maxP = 0;
    }
    public void setIntervalle(int rg)
    {
        this.defaultRange = Integer.toString(rg);
    }

    public synchronized String[] getNextRange(int nbCores){
        BigInteger next = new BigInteger(nextInt);
        BigInteger range = new BigInteger(defaultRange);
        BigInteger nbCoresBig = BigInteger.valueOf(nbCores);
        
        next = next.add(range.multiply(nbCoresBig));
        nextInt = next.toString();
        BigInteger start = next.subtract(range.multiply(nbCoresBig));
        String[] intervalle = {start.toString(),nextInt};
        return intervalle;
        

        
    }

    public synchronized void addPair(String[] pair){
        if (nbPersTable.size() >= maxHashSize) {
            // Enlevez des éléments de la hashtable
            // Par exemple, enlevez la moitié des éléments
            int removeCount = maxHashSize / 2;
            Enumeration<String> keys = nbPersTable.keys();
            for (int i = 0; i < removeCount; i++) {
                String key = keys.nextElement();
                nbPersTable.remove(key);
            }
        }
        nbPersTable.put(pair[0],pair[1]);
        saveToFile("resultats.txt");
    }

    public synchronized String getPersistanceOf(String n) {
        String m="Persistance_non_trouvé_le_worker_n'a_pas_encore_traité_le_nombre_saisi";
        
        if(extraitF.containsKey(n))
        {
            return extraitF.get(n);
        }
        else return m;
        

    }
   
    public synchronized ArrayList<int[]> getNPList() {
        ArrayList<int[]> resultList = new ArrayList<>();
    
        for (Entry<String, String> entry : nbPersTable.entrySet()) {
            int key = Integer.parseInt(entry.getKey());
            int value = Integer.parseInt(entry.getValue());
            int[] pair = new int[] {key, value};
            resultList.add(pair);
        }
    
        return resultList;
    }
    
     public synchronized void saveToFile(String filename)
     {
         try(FileWriter writer = new FileWriter(filename))
         {
             Enumeration<String> keys = nbPersTable.keys();
             while(keys.hasMoreElements())
             {
                 String key = keys.nextElement();
                 String value = nbPersTable.get(key);
                 writer.write(key+','+value+"\n");
             }
         }catch(IOException e)
         {
             e.printStackTrace();
         }
     }


      public void loadFromFile() 
     {
         File file = new File("resultats.txt");
         if (!file.exists()) {
             try {
                 file.createNewFile();
             } catch (IOException e) {
                 e.printStackTrace();
             }
         }
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String ligne;
             while ((ligne = br.readLine()) != null) // Tant que la ligne n'est pas vide
          {
                  String[] parts = ligne.split(",");
                  String key = parts[0].trim();
                  String value = parts[1].trim();
                  extraitF.put(key ,value);
             }
        } catch (IOException e) {
            System.out.println("Erreur lors de la lecture du fichier 'resultats'");
            e.printStackTrace();
        }
     }        
    
  
    
    
    //______________METHODS FOR DOING STATISTICS___________
    
    public synchronized int calculateMaxP() {
        if (!nbPersTable.isEmpty()) {
            for (String value : nbPersTable.values()) {
                int persistance = Integer.parseInt(value);
                if (persistance > maxP) {
                    maxP = persistance;
                }
            }
            return maxP;
        } else return -1;
    }
    
    public synchronized double calculateMean() {
        int sum = 0;
        int count = 0;
        for (String value : nbPersTable.values()) {
            sum += Integer.parseInt(value);
            count++;
        }
        return count == 0 ? 0 : (double) sum / count;
    }
    
    public synchronized int calculateMedian() {
        List<Integer> persistanceList = new ArrayList<>();
        for (String value : nbPersTable.values()) {
            persistanceList.add(Integer.parseInt(value));
        }
    
        Collections.sort(persistanceList);
    
        int size = persistanceList.size();
        if (size == 0) {
            return -1;
        } else if (size % 2 == 0) {
            return (persistanceList.get(size / 2 - 1) + persistanceList.get(size / 2)) / 2;
        } else {
            return persistanceList.get(size / 2);
        }
    }
    
    public synchronized int calculateNbOcc(int p) {
        int count = 0;
        for (String value : nbPersTable.values()) {
            if (Integer.parseInt(value) == p) {
                count++;
            }   
        }
        return count;
    }
}

class Connection extends Thread{

    private String status;
    private int nbCores;
    private int id;
    private Socket s;
    private BufferedReader reader;
    private PrintWriter writer;
    private Database d;
    private boolean stop = false;
    private String[] currentRange = new String[2];
    private WorkerInfo workerInfo = null;

    

    public Connection(int id, Socket s, Database d){
        this.id = id;
        this.s = s;
        this.d = d;

        try{

            this.reader = new BufferedReader( new InputStreamReader( s.getInputStream() ) );
            this.writer = new PrintWriter( new BufferedWriter( new OutputStreamWriter( s.getOutputStream() ) ), true );

        } catch (IOException e){e.printStackTrace();}

        try{
            String str = reader.readLine();
            this.status = str;
            System.out.println("_________________________");
            System.out.println("réussir à obtenir les statistiques : " + this.status + " de " + this.id + "\n");
            this.nbCores = Integer.parseInt(reader.readLine());
        } catch (IOException e){e.printStackTrace();}
    }

    public int getWorkerId(){
        return this.id;
    }

    public int getNbCores(){
        return this.nbCores;
    }

    public String[] getCurrentRange(){
        
        return this.currentRange;
    }

    public void run(){
        if(status.equals("worker")){
            Serveur.nbWorkers++;
            System.out.println("_________________________");
            System.out.println("Nombre de coeurs de " + this.id + " : " + this.nbCores + "\n");

            workerInfo = new WorkerInfo(Integer.toString(id), s.getInetAddress(), s.getPort(), nbCores);
            Serveur.registerWorker(workerInfo);
            try{
                while(!stop){
                    //sending the range using the syntax : lowerbound;upperbound
                    String[] r = d.getNextRange(this.nbCores);

                    this.currentRange = r;
                    System.out.println("_________________________");
                    System.out.println("Tentative d'envoi d'un intervalle" + this.id+ "\n");
                    writer.println(r[0] + ";" + r[1]);
                    System.out.println("_________________________");
                    System.out.println("Intervalle: " + r[0] + ";" + r[1] + " envoyé au workers " + this.id + "\n");

                    while(true){
                        String str = reader.readLine();
                        if(str.equals("TERMINÉ"))
                        { 
                            System.out.println("_________________________");
                            System.out.println("Worker " + this.id + " a terminé avec l'intervalle : " + r[0] + ";" + r[1] + "\n");
                            break;
                        }
                        else if(str.equals("QUITTER"))
                        {
                            System.out.println("_________________________");
                            System.out.println("Worker " + this.id + " va maintenant 'QUITTER'\n");
                            stop = true;
                            break;

                        }
                        //expecting the line to follow the syntax : number,persistance
                        
                        String[] pair = str.split(",");
                        d.addPair( new String[] {pair[0],pair[1]} );
                        d.loadFromFile();
                    }

                    // System.out.println("_________________________");
                    // System.out.println("_________________________");
                    // System.out.println("_________________________");
                    // System.out.println("_________________________");

                    // double moyenne = d.calculateMean();
                    // int mediane = d.calculateMedian();
                    // int maxP = d.calculateMaxP();

                    // System.out.println("\n\n\n____________________STATISTIQUES________________\n\n\n");
                    // System.out.println("size of Database : " + d.getNPList().size());
                    // System.out.println("moyenne : " + moyenne);
                    // System.out.println("mediane : " + mediane);
                    // System.out.println("Maximum des persistances : " + maxP);
                    // System.out.println("nombre d'occurances de max " + maxP + " : " + d.calculateNbOcc(maxP));
                    // System.out.println("nombre d'occurances de la moyenne " + moyenne + " : " + d.calculateNbOcc((int) moyenne));
                    // System.out.println("nombre d'occurances de la mediane " + mediane + " : " + d.calculateNbOcc(mediane));
                }
                reader.close();
                writer.close();
                s.close();
                this.id=0;
            } catch (IOException e){e.printStackTrace();}

        } else if(status.equals("client")){
            Serveur.nbClients++;

            try{
                System.out.println("____________________________________________________________________________");
                System.out.println("Renvoi les éléments suivants en tant que statistiques : \n");
                ArrayList<String> stats = Statistics.printStatistics(d);
                writer.println(stats.get(stats.size() - 1));
                for(String s : stats){
                    writer.println(s);
                }
                while(true) {
                    System.out.println("____________________________________________________________________________");
                    System.out.println("Entrain de lire la commande saisit par le client ...\n");
                    String[] bigCommand = reader.readLine().split(";");
                    System.out.println("____________________________________________________________________________");
                    System.out.println("Commande : " + bigCommand[0] + "\n");
                    if(bigCommand[0].equals("PERSISTANCE DE")){
                        System.out.println("____________________________________________________________________________");
                        System.out.println("Un client a demandé la persistance de : " + bigCommand[1] + "\n");
                        writer.println(d.getPersistanceOf(bigCommand[1]));
                        System.out.println("____________________________________________________________________________");
                        System.out.println("Persistance de : " + bigCommand[1] + " envoyé au client\n");
                    } else if(bigCommand[0].equals("NB OCC DE")){
                        System.out.println("____________________________________________________________________________");
                        System.out.println("Un client a demandé l'occurence de : " + bigCommand[1] + "\n");
                        writer.println(d.calculateNbOcc(Integer.parseInt(bigCommand[1])));
                        System.out.println("____________________________________________________________________________");;
                        System.out.println("Nombre d'occurence de : " + bigCommand[1] + " envoyé\n");
                    } else if(bigCommand[0].equals("MEDIANE")){
                        System.out.println("____________________________________________________________________________");
                        System.out.println("Un client demande le nombre total de la médiane\n");
                        writer.println(d.calculateMedian());
                        System.out.println("____________________________________________________________________________");
                        System.out.println("Total médiane envoyé\n");
                    } else if(bigCommand[0].equalsIgnoreCase("STATISTIQUES")){
                        System.out.println("____________________________________________________________________________");
                        System.out.println("Un client demande de consulter les statistiques\n");
                        
                        stats = Statistics.printStatistics(d);
                        writer.println(stats.get(stats.size() - 1));
                        for(String s : stats){
                            writer.println(s);
                        }

                        System.out.println("____________________________________________________________________________");
                        System.out.println("Les statistiques sont envoyés\n");
                    } else if(bigCommand[0].equals("NOUVEAU INTERVALLE")){
                        System.out.println("____________________________________________________________________________");
                        System.out.println("Un client a demandé de saisir un nouveau intervalle : " + bigCommand[1] + "\n");
                    
                        // Splitting the input to get the lower and upper bounds of the interval
                        String[] intervalBounds = bigCommand[1].split(",");
                        int lowerBound = Integer.parseInt(intervalBounds[0]);
                        // Setting the new interval
                        d.setIntervalle(lowerBound);
                        System.out.println("____________________________________________________________________________");
                        System.out.println("Nouvel intervalle mis à jour : [" + lowerBound + ", " + lowerBound*nbCores + "]\n");
                        writer.println("Nouvel intervalle mis à jour : [" + lowerBound + ", " + lowerBound*nbCores + "]");
                    }
                    
                    else if(bigCommand[0].equals("FIN")){
                        System.out.println("____________________________________________________________________________");
                        System.out.println("FIN \n");
                        break;
                    }
                }
                reader.close();
                writer.close();
                s.close();
            } catch (IOException e){e.printStackTrace();}
        }
    }
}

class WorkerInfo {
    private String id;
    private InetAddress address;
    private int port;
    private int cores;
    private int currentLoad;

    public WorkerInfo(String id, InetAddress address, int port, int cores) {
        this.id = id;
        this.address = address;
        this.port = port;
        this.cores = cores;
        this.currentLoad = 0;
    }

    public String getId()
    {
        return this.id;
    }
    public int getPort()
    {
        return this.port;
    }
    public InetAddress getAddress()
    {
        return this.address;
    }
    public int getCores()
    {
        return this.cores;
    }
    public int getCurrentLoad()
    {
        return this.currentLoad;
    }
    public void setCurrentLoad(int currentLoad) {
        this.currentLoad = currentLoad;
    }
}

class Statistics{

    static ArrayList<String> printStatistics(Database d){
        ArrayList<String> text = new ArrayList<String>();
        text.add("Nb de workers\tMédiane\tMoyenne\ttotales des nb calculés\t Persistance max");
        String str = Serveur.nbWorkers + "\t\t";
        str += d.calculateMedian() + "\t";
        str += d.calculateMean() + "\t";
        str += d.getNPList().size() + "\t\t\t";
        str += d.calculateMaxP();
        text.add(str);

        text.add("Worker\tCoeurs\tIntervalle courant\tNouveau intervalle");
        
        for(Connection w : Serveur.conxList){
            str = "";
            str += w.getWorkerId() + "\t";
            str += w.getNbCores() + "\t";
            str += Arrays.toString(w.getCurrentRange()) + "\t";
            text.add(str);
        }
        int size = text.size();
        text.add(String.valueOf(size));
        return text;
    
    }
}
