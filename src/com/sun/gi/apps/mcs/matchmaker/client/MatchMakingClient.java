/*
 * Copyright © 2006 Sun Microsystems, Inc., 4150 Network Circle, Santa
 * Clara, California 95054, U.S.A. All rights reserved.
 * 
 * Sun Microsystems, Inc. has intellectual property rights relating to
 * technology embodied in the product that is described in this
 * document. In particular, and without limitation, these intellectual
 * property rights may include one or more of the U.S. patents listed at
 * http://www.sun.com/patents and one or more additional patents or
 * pending patent applications in the U.S. and in other countries.
 * 
 * U.S. Government Rights - Commercial software. Government users are
 * subject to the Sun Microsystems, Inc. standard license agreement and
 * applicable provisions of the FAR and its supplements.
 * 
 * Use is subject to license terms.
 * 
 * This distribution may include materials developed by third parties.
 * 
 * Sun, Sun Microsystems, the Sun logo and Java are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other
 * countries.
 * 
 * This product is covered and controlled by U.S. Export Control laws
 * and may be subject to the export or import laws in other countries.
 * Nuclear, missile, chemical biological weapons or nuclear maritime end
 * uses or end users, whether direct or indirect, are strictly
 * prohibited. Export or reexport to countries subject to U.S. embargo
 * or to entities identified on U.S. export exclusion lists, including,
 * but not limited to, the denied persons and specially designated
 * nationals lists is strictly prohibited.
 * 
 * Copyright © 2006 Sun Microsystems, Inc., 4150 Network Circle, Santa
 * Clara, California 95054, Etats-Unis. Tous droits réservés.
 * 
 * Sun Microsystems, Inc. détient les droits de propriété intellectuels
 * relatifs à la technologie incorporée dans le produit qui est décrit
 * dans ce document. En particulier, et ce sans limitation, ces droits
 * de propriété intellectuelle peuvent inclure un ou plus des brevets
 * américains listés à l'adresse http://www.sun.com/patents et un ou les
 * brevets supplémentaires ou les applications de brevet en attente aux
 * Etats - Unis et dans les autres pays.
 * 
 * L'utilisation est soumise aux termes de la Licence.
 * 
 * Cette distribution peut comprendre des composants développés par des
 * tierces parties.
 * 
 * Sun, Sun Microsystems, le logo Sun et Java sont des marques de
 * fabrique ou des marques déposées de Sun Microsystems, Inc. aux
 * Etats-Unis et dans d'autres pays.
 * 
 * Ce produit est soumis à la législation américaine en matière de
 * contrôle des exportations et peut être soumis à la règlementation en
 * vigueur dans d'autres pays dans le domaine des exportations et
 * importations. Les utilisations, ou utilisateurs finaux, pour des
 * armes nucléaires,des missiles, des armes biologiques et chimiques ou
 * du nucléaire maritime, directement ou indirectement, sont strictement
 * interdites. Les exportations ou réexportations vers les pays sous
 * embargo américain, ou vers des entités figurant sur les listes
 * d'exclusion d'exportation américaines, y compris, mais de manière non
 * exhaustive, la liste de personnes qui font objet d'un ordre de ne pas
 * participer, d'une façon directe ou indirecte, aux exportations des
 * produits ou des services qui sont régis par la législation américaine
 * en matière de contrôle des exportations et la liste de ressortissants
 * spécifiquement désignés, sont rigoureusement interdites.
 */

package com.sun.gi.apps.mcs.matchmaker.client;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.security.auth.callback.Callback;

import static com.sun.gi.apps.mcs.matchmaker.server.CommandProtocol.*;

import com.sun.gi.apps.mcs.matchmaker.server.CommandProtocol;
import com.sun.gi.comm.users.client.ClientChannel;
import com.sun.gi.comm.users.client.ClientChannelListener;
import com.sun.gi.comm.users.client.ClientConnectionManager;
import com.sun.gi.comm.users.client.ClientConnectionManagerListener;
import com.sun.gi.utils.SGSUUID;
import com.sun.gi.utils.StatisticalUUID;

/**
 * <p>
 * Title: MatchMakingClient
 * </p>
 * 
 * <p>
 * Description: This class is a concrete implementation of the
 * IMatchMakingClient. It communicates with the match making server
 * application via the UserManagerClient for sending commands, and
 * listening on the well known lobby control channel for receiving
 * responses.
 * </p>
 * 
 * @author Sten Anderson
 * @version 1.0
 */
public class MatchMakingClient
        implements IMatchMakingClient, ClientConnectionManagerListener,
                    ClientChannelListener
{
    private IMatchMakingClientListener listener;
    private ClientConnectionManager manager;
    private HashMap<String, LobbyChannel> lobbyMap;
    private HashMap<String, GameChannel> gameMap;

    private CommandProtocol protocol;
    private byte[] myID;

    /**
     * Constructs a new MatchMakingClient.
     * 
     * @param manager the user manager used for server
     * commmunication
     */
    public MatchMakingClient(ClientConnectionManager manager) {
        this.manager = manager;
        manager.setListener(this);
        protocol = new CommandProtocol();
        lobbyMap = new HashMap<String, LobbyChannel>();
        gameMap = new HashMap<String, GameChannel>();
    }

    void sendCommand(List list) {
        manager.sendToServer(protocol.assembleCommand(list), true);
    }

    private SGSUUID createUUID(byte[] bytes) {
        SGSUUID id = null;
        try {
            id = new StatisticalUUID(bytes);
        } catch (InstantiationException ie) {
            // ignore
        }

        return id;
    }

    public void setListener(IMatchMakingClientListener listener) {
        this.listener = listener;
    }

    public void listFolder(byte[] folderID) {
        List list = new LinkedList();
        list.add(LIST_FOLDER_REQUEST);
        if (folderID != null) {
            list.add(createUUID(folderID));
        }

        sendCommand(list);
    }

    public void joinLobby(byte[] lobbyID, String password) {
        List list = new LinkedList();
        list.add(JOIN_LOBBY);
        list.add(createUUID(lobbyID));
        if (password != null) {
            list.add(password);
        }

        sendCommand(list);
    }

    public void joinGame(byte[] gameID) {
        joinGame(gameID, null);
    }

    public void joinGame(byte[] gameID, String password) {
        List list = new LinkedList();
        list.add(JOIN_GAME);
        list.add(createUUID(gameID));
        if (password != null) {
            list.add(password);
        }

        sendCommand(list);
    }

    /**
     * Attempts to find the user name of a currently connected user with
     * the given ID.
     * 
     * @param userID
     */
    public void lookupUserName(byte[] userID) {
        List list = new LinkedList();
        list.add(LOOKUP_USER_NAME_REQUEST);
        list.add(createUUID(userID));

        sendCommand(list);
    }

    /**
     * Attempts to find the user ID of a currently connected user with
     * the given user name.
     * 
     * @param username
     */
    public void lookupUserID(String username) {
        List list = protocol.createCommandList(LOOKUP_USER_ID_REQUEST);
        list.add(username);

        sendCommand(list);
    }

    public void sendValidationResponse(Callback[] cb) {
        manager.sendValidationResponse(cb);
    }

    // implemented methods from ClientConnectionManagerListener

    public void validationRequest(Callback[] callbacks) {
        listener.validationRequest(callbacks);
    }

    public void connected(byte[] myID) {
        System.out.println("connected");
        this.myID = myID;
    }

    public void connectionRefused(String message) {
        System.out.println("Connection Refused:  shouldn't have happened.");
    }

    public void failOverInProgress() {

    }

    public void reconnected() {

    }

    public void disconnected() {
        listener.disconnected();
    }

    /**
     * This event is fired when a user joins the game. When a client
     * initially connects to the game it will receive a userJoined
     * callback for every other user rpesent. Aftre that every time a
     * new user joins, another callback will be issued.
     * 
     * @param userID The ID of the joining user.
     */
    public void userJoined(byte[] userID) {

    }

    /**
     * This event is fired whenever a user leaves the game. This occurs
     * either when a user purposefully disconnects or when they drop and
     * do not re-connect within the timeout specified for the
     * reconnection key in the Darkstar backend.
     * <p>
     * <b>NOTE: In certain rare cases (such as the death of a slice),
     * notification may be delayed. (In the slice-death case it is
     * delayed until a watchdog notices the dead slice.)</b>
     * 
     * @param userID The ID of the user leaving the system.
     */
    public void userLeft(byte[] userID) {

    }

    /**
     * This event is fired to notify the listener of sucessful
     * completion of a channel open operation.
     * 
     * @param channel the channel object used to communicate on the
     * opened channel.
     */
    public void joinedChannel(ClientChannel channel) {
        System.out.println("Connected to channel " + channel.getName());
        if (channel.getName().equals(LOBBY_MANAGER_CONTROL_CHANNEL)) {
            channel.setListener(this);
        } else if (channel.getName().indexOf(":") == -1) { // lobby
            LobbyChannel lobby = new LobbyChannel(channel, this);
            lobbyMap.put(channel.getName(), lobby);
            channel.setListener(lobby);
            listener.joinedLobby(lobby);
        } else { // game
            GameChannel game = new GameChannel(channel, this);
            gameMap.put(channel.getName(), game);
            channel.setListener(game);
            listener.joinedGame(game);
        }

    }

    /**
     * Called whenever an attempted join/leave fails due to the target
     * channel being locked.
     * 
     * @param channelName the name of the channel
     * @param userID the ID of the user attemping to join/leave
     */
    public void channelLocked(String channelName, byte[] userID) {

    }

    // implemented methods from ClientChannelListener
    // these call backs are on the LobbyManager control channel

    /**
     * A new player has joined the channel this listener is registered
     * on.
     * 
     * @param playerID The ID of the joining player.
     */
    public void playerJoined(byte[] playerID) {

    }

    /**
     * A player has left the channel this listener is registered on.
     * 
     * @param playerID The ID of the leaving player.
     */
    public void playerLeft(byte[] playerID) {

    }

    /**
     * A packet has arrived for this listener on this channel. Command
     * responses from the server come in on this channel.
     * 
     * @param from the ID of the sending player.
     * @param data the packet data
     * @param reliable true if this packet was sent reliably
     */
    public void dataArrived(byte[] from, ByteBuffer data, boolean reliable) {
        // ignore if not from server
        // hmm, this doesn't seem to work
        /*
         * if (!manager.isServerID(from)) { System.out.println("Not from
         * server"); return; }
         */
        int command = protocol.readUnsignedByte(data);
        if (command == SERVER_LISTENING) {
            listener.connected(myID);
        } else if (command == LIST_FOLDER_RESPONSE) {
            listFolderResponse(data);
        } else if (command == LOOKUP_USER_NAME_RESPONSE) {
            lookupUserNameResponse(data);
        } else if (command == LOOKUP_USER_ID_RESPONSE) {
            lookupUserIDResponse(data);
        } else if (command == GAME_PARAMETERS_RESPONSE) {
            gameParametersResponse(data);
        } else if (command == CREATE_GAME_FAILED) {
            createGameFailed(data);
        }
    }

    /**
     * Notifies this listener that the channel has been closed.
     */
    public void channelClosed() {

    }

    /**
     * Called to parse out the ListFolderResponse response from the
     * server.
     * 
     * @param data the data buffer containing the requested folder and
     * lobby detail.
     */
    private void listFolderResponse(ByteBuffer data) {
        SGSUUID folderID = protocol.readUUID(data);
        int numFolders = data.getInt();
        FolderDescriptor[] subfolders = new FolderDescriptor[numFolders];
        for (int i = 0; i < numFolders; i++) {
            String curFolderName = protocol.readString(data);
            String curFolderDescription = protocol.readString(data);
            SGSUUID curFolderID = protocol.readUUID(data);
            subfolders[i] = new FolderDescriptor(curFolderID, curFolderName,
                    curFolderDescription);
        }
        int numLobbies = data.getInt();
        LobbyDescriptor[] lobbies = new LobbyDescriptor[numLobbies];
        for (int i = 0; i < numLobbies; i++) {
            String curLobbyName = protocol.readString(data);
            String curLobbyChannelName = protocol.readString(data);
            String curLobbyDescription = protocol.readString(data);
            SGSUUID curLobbyID = protocol.readUUID(data);
            int numUsers = data.getInt();
            int maxUsers = data.getInt();
            boolean isPasswordProtected = protocol.readBoolean(data);

            lobbies[i] = new LobbyDescriptor(curLobbyID, curLobbyName,
                    curLobbyChannelName, curLobbyDescription, numUsers,
                    maxUsers, isPasswordProtected);
        }
        listener.listedFolder(folderID, subfolders, lobbies);
    }

    private void lookupUserNameResponse(ByteBuffer data) {
        String userName = protocol.readString(data);
        SGSUUID userID = protocol.readUUID(data);

        listener.foundUserName(userName, userID.toByteArray());
    }

    private void lookupUserIDResponse(ByteBuffer data) {
        String userName = protocol.readString(data);
        SGSUUID userID = protocol.readUUID(data);

        listener.foundUserID(userName, userID != null ? userID.toByteArray()
                : null);
    }

    private void gameParametersResponse(ByteBuffer data) {
        String lobbyName = protocol.readString(data);
        int numParams = data.getInt();
        HashMap<String, Object> paramMap = new HashMap<String, Object>();
        for (int i = 0; i < numParams; i++) {
            String param = protocol.readString(data);
            Object value = protocol.readParamValue(data);
            paramMap.put(param, value);
        }
        LobbyChannel lobby = lobbyMap.get(lobbyName);
        if (lobby != null) {
            lobby.receiveGameParameters(paramMap);
        }
    }

    private void createGameFailed(ByteBuffer data) {
        String game = protocol.readString(data);
        String desc = protocol.readString(data);
        String lobbyName = protocol.readString(data);
        if (lobbyName == null) {
            return;
        }
        LobbyChannel lobby = lobbyMap.get(lobbyName);
        if (lobby != null) {
            lobby.createGameFailed(game, desc);
        }
    }
}
