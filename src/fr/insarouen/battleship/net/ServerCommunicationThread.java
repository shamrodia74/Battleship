package fr.insarouen.battleship.net;

import java.net.*;
import java.io.*;
import java.util.*;

import fr.insarouen.battleship.controler.*;
import fr.insarouen.battleship.observer.Observer;

/**
 * Management of socket connections (server side).
 *
 * @author David ALBERT
 * @version 0
 * 
 */

public class ServerCommunicationThread extends CommunicationThread implements Observer {

    private static final String SEP = ":"; 
    private AbstractControler controler;
    private boolean closeConnexion = true;

    /**
     * Constructs a new Thread of server communication with the client thanks the specific Socket connection and the adequate client controller
     * @param Socket, {@link AbstractControler}
     * @throws IOException, UnknownHostException
     * 
     */
    public ServerCommunicationThread(Socket socket, AbstractControler controler) throws IOException {
		super(socket);
    	this.controler = controler;		
    }
   
    @Override
    public void run() {
			 
		System.err.println("Lancement du traitement de la communication serveur");
		InetSocketAddress remote = (InetSocketAddress)socket.getRemoteSocketAddress();
		
		closeConnexion = false;
			
		//tant que la connexion est active, on traite les demandes
		while(!this.socket.isClosed()){
		    try {
				
				String message = receive();
				
				// Transformation de la chaîne de caractère reçue pour facilité le traitement
				ArrayList<String> commande = new ArrayList<String>();
				commande = decode(message);
						
				//On affiche quelques infos
				String debug = "";
				debug = "Thread : " + Thread.currentThread().getName() + ". ";
				debug += "Demande de l'adresse : " + remote.getAddress().getHostAddress() +".\n";
				//debug += " Sur le port : " + remote.getPort() + ".\n";
				debug += "-> Commande reçue : " + message + "\n";
				System.err.println("\n" + debug);
				
				//On traite la demande du client en fonction de la commande envoyée
				process(commande);
			
				//On ferme la connexion
				if(closeConnexion){
				    System.err.println("Fermeture connexion avec :"+remote.getAddress().getHostAddress() +".");
				    writer = null;
				    reader = null;
				    socket.close();
				    break;
				}
			
			// Gestion des erreurs
		    } catch (SocketException e) {
		    	System.err.println("Connexion interrompue");
		    } catch (IOException e) {
		    	e.printStackTrace();
		    }
			 
		}
    }
    

	@Override
    public String receive(){  
    	String response = "";
    	int stream;
    	byte[] b = new byte[4096];
    	try {
			stream = reader.read(b);
	    	response = new String(b, 0, stream);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (StringIndexOutOfBoundsException e) {
			e.printStackTrace();
		}
    	return response;
    }


	@Override
	public void send(String msg) {
		writer.write(msg);
		writer.flush();
	}
	
	@Override
	protected ArrayList<String> decode(String str) {
		ArrayList<String> res = new ArrayList<String>();
		int l1 =0;
		try {
		    int l2=str.indexOf(SEP,l1);
		    res.add(new String(str.substring(l1, l2)));
		    while (l2<str.length()){
			l1=l2+1;
			l2=str.indexOf(SEP, l1);
			res.add(new String(str.substring(l1, l2)));
		    }
		} catch (StringIndexOutOfBoundsException e){
		    // System.err.println("Erreur");
		} catch (IndexOutOfBoundsException e){
		    // System.err.println("Erreur");
		}
		res.add(new String(str.substring(l1, str.length())));
			
		return res;
	}
	
    public void process(ArrayList<String> commande) {
    	String toSend = "";
		switch(commande.get(0).toUpperCase()){
		
		case "LIST":
		    toSend = "LIST:"+controler.getPlayers();
		    break;

		case "DEMANDEPARTIE":
			controler.askNewGame(commande.get(1));
			break;

		case "REPONSEPARTIE":
			controler.answerNewGame(commande.get(1),commande.get(2));
			try {
				Thread.sleep(1000);
			} catch (Exception e){}
			if (commande.get(2).equals("oui")){
				controler.newGame(commande.get(1));
			}
			break;

		case "NEW":
		    if (commande.size()>1) {
		    	controler.newPlayer(commande.get(1));
		    }
		    else {
		    	controler.newPlayer();
		    }
		    break;

		case "IDPARTIE":
				controler.setIdGame(Integer.parseInt(commande.get(1)));
			break;
			
		case "AVAILABLENAME":
			if (controler.isAvailableName(commande.get(1))){
				toSend = "AVAILABLENAME:"+commande.get(1)+":yes";
				controler.newPlayer(commande.get(1));
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
			}
			else {
				toSend = "AVAILABLENAME:"+commande.get(1)+":no";
			}
			break;
			
		case "CLOSE":
		    toSend = "CLOSE:Communication terminée"; 
		    controler.remove(this);
		    closeConnexion = true;
		    break;
        case "DECOUVRIR":
        	controler.discover(Integer.parseInt(commande.get(1)), Integer.parseInt(commande.get(2)));
       		break;
                    
		default : 
		    toSend = "Commande inconnu !";                     
		    break;
		}

		//On envoie la réponse au client
		send(toSend);
		
	}

	@Override
	public void update(String str) {
		send(str);
	}

	@Override
	public String toString(){
		return "Thread de communication coté server associé à : \n" + controler.toString(); 
	}
}
