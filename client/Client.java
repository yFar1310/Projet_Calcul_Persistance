import java.util.*;
import java.io.*;
import java.net.*;


public class Client{

    static int port = 8010;

    public static void main(String [] args) throws Exception{
        //conneciton vers le serveur
        System.out.println("____________________________________________________________________________");
        System.out.println("Creation du socket \n");
        Socket soc = new Socket("kali", port);
        BufferedReader reader = new BufferedReader(new InputStreamReader(soc.getInputStream()));
        PrintWriter writer = new PrintWriter( 
            new BufferedWriter( new OutputStreamWriter( soc.getOutputStream() ) ), true);

        System.out.println("_________________________");
        System.out.println("Informer le serveur que je suis un client\n");
        writer.println("client");
        writer.println(Runtime.getRuntime().availableProcessors());
        
        //GETTING STATISTICS
        System.out.println("_________________________");
        System.out.println("Getting statistics from server ...\n");
        int size = Integer.parseInt(reader.readLine()) - 2;
        System.out.println(reader.readLine());
        System.out.println(reader.readLine());
        System.out.println("\n\n");
        for(int i = 0;i<size;i++){
            System.out.println(reader.readLine());
        }
        reader.readLine();
        //Fin d'obtention des statistiques

        System.out.println("____________________________________________________________________________");
        System.out.println("Finished getting statistics\n");
        System.out.println("____________________________________________________________________________");
        System.out.println("Vous pouvez maintenant saisir l'une des commandes suivantes:\n");
        System.out.println("PERSISTANCE DE ;");
        System.out.println("NB OCC DE ;");
        System.out.println("MEDIANE");
        System.out.println("STATISTIQUES");
        System.out.println("NOUVEAU INTERVALLE ;");
        System.out.print("\n");

        try (Scanner sc = new Scanner(System.in)) {
            String str;
            do{

                boolean stop = false;
                String n="";
                String command = "commande par défaut;0";
                

                //Lire la commande de l'utilisateur 


                System.out.println("____________________________________________________________________________");
                System.out.println("Écrivez une nouvelle commande: \n");
                str = sc.nextLine();


                while(!stop){
                    if(!str.contains("FIN") && !str.contains("PERSISTANCE DE") && 
                    !str.contains("NB OCC DE") && !str.contains("MEDIANE") && !str.contains("STATISTIQUES") && !str.contains("NOUVEAU INTERVALLE")){
                        System.out.println("____________________________________________________________________________");
                        System.out.println("commande incorrecte, veuillez réessayer à nouveau merci : \n");
                        str = sc.nextLine();
                    } else if ( (str.contains("PERSISTANCE DE") || str.contains("NB OCC DE") || str.contains("NOUVEAU INTERVALLE"))
                    && !str.contains(";") ){
                        System.out.println("_________________________");
                        System.out.println("Syntax incorrecte, réessayer avec : 'commande ; number', réassayer encore merci : \n");
                        str = sc.nextLine();
                    } else { 
                        command = str.split(";")[0].trim();
                        if(str.contains(";")){
                            n = str.split(";")[1].trim();
                        }
                        stop = true;
                    }
                }
                
                // Envoyer la commande au serveur et traiter la réponse

                System.out.println("____________________________________________________________________________");
                System.out.println("envoie de la commande: " + command + "\n");
                writer.println(command + ";" + n);
                
                
                if(command.equals("STATISTIQUES")){
                    // Récupérer les statistiques
                    System.out.println("_________________________");
                    System.out.println("Obtention des statistiques du serveur...\n");

                    size = Integer.parseInt(reader.readLine()) - 2;
                    System.out.println(reader.readLine());
                    System.out.println(reader.readLine());
                    System.out.println("\n\n");
                    for(int i = 0;i<size;i++){
                        System.out.println(reader.readLine());
                    }
                    reader.readLine();

                } else if(command.equals("FIN")){
                    // Arrêter le client
                    System.out.println("_________________________");
                    System.out.println("Vous avez saisi la commande'FIN'\n");
                } else {
                    // Traiter les autres commandes
                    System.out.println("_________________________");
                    System.out.println("Valeur retourné par le serveur: " + reader.readLine() + "\n");
                }
            } while(!str.equals("FIN"));
        }
        writer.close();
        reader.close();
        soc.close();
        


        Thread.sleep(1000);
    }

}