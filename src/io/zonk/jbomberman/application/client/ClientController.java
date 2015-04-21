package io.zonk.jbomberman.application.client;

import io.zonk.jbomberman.game.Action;
import io.zonk.jbomberman.game.ActionType;
import io.zonk.jbomberman.game.Party;
import io.zonk.jbomberman.game.Player;
import io.zonk.jbomberman.game.client.ClientGame;
import io.zonk.jbomberman.network.NetworkFacade;
import io.zonk.jbomberman.network.client.ClientNetwork;
import io.zonk.jbomberman.utils.ActionSerializer;

import java.util.HashMap;
import java.util.Observable;

public class ClientController extends Observable {
	private String server;
	private int connectionState = 0; //0 => Connect; 1 => Lobby
	
	HashMap<Integer, Boolean> states;
	
	// Player this instance is associated with [PlayerName, ID, state]
	int playerId = 0;
	
	private NetworkFacade network;
	private Party party;
	
	public ClientController() {
		this.network = new ClientNetwork();
		this.party = new Party();
	}
	
	public void startGame() {
//		new ClientGame(network, party);
	}
	
	public void finishGame() {
		
	}
	
	public void addPlayer(Player player) {
		
	}
	
	public void removePlayer(Player player) {
		
	}
	
	public void connectToServer(String hostname) {
		this.server = hostname;
		network.connect(hostname);
		if(network.isOpen()) {
			Object[] prop = {"connect"};
			send(prop);

			byte[] recMsg = network.receiveMessage();
			while(recMsg.length < 1) {
				recMsg = network.receiveMessage();
			}
			Action returnAction = ActionSerializer.deserialize(recMsg);
			if(((String)returnAction.getProperty(0)).equals("lobbyList")) {
				states = (HashMap<Integer, Boolean>)returnAction.getProperty(1);
				playerId = (Integer)returnAction.getProperty(2);
				
				connectionState = 1;
				setChanged();
				notifyObservers("connChanged");
			}
			startReceiving();
		}
	}


	public void disconnect() {
		network.close();
		connectionState = 0;
		setChanged();
		notifyObservers("connChanged");
	}
	
	public void setReady(boolean b) {
		Object[] prop = {"updateState", playerId, b};
		send(prop);
	}
	
	public String getServer() {
		return server;
	}
	
	public int getConnState() {
		return connectionState;
	}
	
	public HashMap<Integer, Boolean> getStates() {
		return states;
	}

	public int getPlayerId() {
		return playerId;
	}
	
	public void msgReceived(byte[] recMsg) {
		Action returnAction = ActionSerializer.deserialize(recMsg);
		String s = ((String)returnAction.getProperty(0));
		
		if(s != null){
			if(s.equals("updateStates") || s.equals("lobbyList")) {
				states = (HashMap<Integer, Boolean>)returnAction.getProperty(1);

				setChanged();
				notifyObservers();
			}
			
			if(s.equals("startGame")) {
				String[][] sParty = (String[][])returnAction.getProperty(1);
				for(String[] p : sParty) {
					if(p[0] != null && p[1] != null) party.add(new Player(p[0], Integer.parseInt(p[1])));
				}
				startGame();
			}
		}
		startReceiving();
	}
	
	private void startReceiving() {
		new Thread(() -> {
			msgReceived(network.receiveMessage());
		}).start();
	}
	
	private void send(Object[] prop) {
		Action connAction = new Action(ActionType.LOBBY_COMMUNICATION, prop);
		
		network.sendMessage(ActionSerializer.serialize(connAction));
	}
}
