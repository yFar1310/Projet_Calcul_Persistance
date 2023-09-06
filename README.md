#Calcul_Persistance

Dans ce projet, j'ai mis en œuvre les principes fondamentaux de la communication TCP/IP entre un serveur et plusieurs clients. 
Je tiens à préciser que mon serveur est capable d'interagir avec plusieurs clients simultanément. 
Dans cette mise en œuvre, nous avons pu observer la robustesse et la rapidité de la CPU grâce à l'utilisation de la classe Worker. 
Cette classe gère le calcul en utilisant un thread qui transmet chaque opération à effectuer à la CPU.
De plus, j'ai ajouté des tables de hachage (hashtables) : l'une pour stocker des données dans un fichier, et l'autre pour lire les données depuis ce fichier en fonction des demandes des clients.
