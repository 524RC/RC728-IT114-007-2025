package Project.Server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import Project.Common.ChoicePayload;
import Project.Common.ChoicePayload.Choice;
import Project.Common.Constants;
import Project.Common.EliminationPayload;
import Project.Common.LoggerUtil;
import Project.Common.Phase;
import Project.Common.PointsPayload;
import Project.Common.TimedEvent;
import Project.Common.TimerType;
import Project.Common.User;
import Project.Common.Payload;
import Project.Common.PayloadType;
import Project.Exceptions.MissingCurrentPlayerException;
import Project.Exceptions.NotPlayersTurnException;
import Project.Exceptions.NotReadyException;
import Project.Exceptions.PhaseMismatchException;
import Project.Exceptions.PlayerNotFoundException;


public class GameRoom extends BaseGameRoom {

    private final Map<Long, ChoicePayload.Choice> playerChoices = new HashMap<>();
    private final Map<Long, Integer> points = new HashMap<>();


    // used for general rounds (usually phase-based turns)
    private TimedEvent roundTimer = null;

    // used for granular turn handling (usually turn-order turns)

    private int round = 0;
    private boolean extrasEnabled = false;   // Toggles Electric + Rock
    private long creatorId = -1;

    public GameRoom(String name) {
        super(name);
    }


    @Override
    protected void onTurnStart() {
        // Turn-based system removed — no action needed.
    }
    @Override
    protected void onTurnEnd() {
    // Turn-based system removed — no action needed.
    }
    
    private void broadcastHost() {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.HOST_INFO);
        p.setMessage(String.valueOf(creatorId));
        relayPayload(p);
    }
    public void toggleExtras() {
        extrasEnabled = !extrasEnabled;
        relay(null, "Extra Options (Electric + Rock): " +
                (extrasEnabled ? "ENABLED" : "DISABLED"));
        broadcastChoiceMode();  
    }
    /** {@inheritDoc} */
    @Override
    protected void onClientAdded(ServerThread sp) {
        // sync GameRoom state to new client
        if (creatorId == -1) {
            creatorId = sp.getClientId();
            relay(null, sp.getClientName() + " is the session creator.");
        }
        broadcastHost();

        syncCurrentPhase(sp);

        syncReadyStatus(sp);
        if (currentPhase != Phase.READY) {
            syncTurnStatus(sp); 
            syncTurnStatus(sp);
            syncPlayerPoints(sp);
            // outside of ready phase

        }

    }

    /** {@inheritDoc} */
    @Override
    protected void onClientRemoved(ServerThread sp) {
        // added after Summer 2024 Demo
        // Stops the timers so room can clean up
        LoggerUtil.INSTANCE.info("Player Removed, remaining: " + clientsInRoom.size());
        long removedClient = sp.getClientId();
        if (clientsInRoom.isEmpty()) {
            resetReadyTimer();
            resetRoundTimer();
            onSessionEnd();
        } 
    }

    // timer handlers
    //rc728 12/11/25
    private void startRoundTimer() {
        roundTimer = new TimedEvent(30, () -> onRoundEnd());
        roundTimer.setTickCallback((time) -> {
            System.out.println("Round Time: " + time);
            sendCurrentTime(TimerType.ROUND, time);
        });
    }

    private void resetRoundTimer() {
        if (roundTimer != null) {
            roundTimer.cancel();
            roundTimer = null;
            sendCurrentTime(TimerType.ROUND, -1);
        }
    }

    // end timer handlers
    //rc728 12/11/25
    private void syncPointsToAll() {
        for (Map.Entry<Long, Integer> entry : points.entrySet()) {
            long playerId = entry.getKey();
            int pts       = entry.getValue();

            ServerThread sp = clientsInRoom.get(playerId);
            if (sp != null) {
                // store the total on the ServerThread's User
                sp.setPoints(pts);
                // broadcast a PointsPayload to everyone
                sendPlayerPoints(sp);
            }
        }
    }

    private void sendPlayerPoints(ServerThread sp) {
        clientsInRoom.values().removeIf(spInRoom -> {
            boolean failedToSend = !spInRoom.sendPlayerPoints(sp.getClientId(), sp.getPoints());
            if (failedToSend) {
                removeClient(spInRoom);
            }
            return failedToSend;
        });
    }

    public enum BattleResult { ATTACKER_WINS, DEFENDER_WINS, TIE }
    
    //rc728 12/11/25
    private BattleResult resolveBattle(Choice a, Choice b) {
        if (a == b) return BattleResult.TIE;
        switch (a) {
            case FIRE:
            if (b == Choice.GRASS) return BattleResult.ATTACKER_WINS;
            if (b == Choice.ELECTRIC) return BattleResult.TIE;  // NEW
            return BattleResult.DEFENDER_WINS;

            case WATER:
                if (b == Choice.FIRE || b == Choice.ROCK) return BattleResult.ATTACKER_WINS;
                return BattleResult.DEFENDER_WINS;

            case GRASS:
                if (b == Choice.WATER || b == Choice.ROCK) return BattleResult.ATTACKER_WINS;
                if (b == Choice.ELECTRIC) return BattleResult.TIE;  // NEW
                return BattleResult.DEFENDER_WINS;

            case ELECTRIC:
                if (b == Choice.WATER) return BattleResult.ATTACKER_WINS;
                if (b == Choice.FIRE || b == Choice.GRASS) return BattleResult.TIE; // NEW
                return BattleResult.DEFENDER_WINS;

            case ROCK:
                if (b == Choice.FIRE || b == Choice.ELECTRIC) return BattleResult.ATTACKER_WINS;
                return BattleResult.DEFENDER_WINS;
        }
        return BattleResult.TIE;
    }


    private String getClientNameById(long id) {
        ServerThread p = clientsInRoom.get(id);
        if (p == null){
            return "Unknown(" + id + ")";
        } 
        return p.getClientName();
    }

private void syncPlayerEliminationStatus(ServerThread incomingClient) {
    clientsInRoom.values().forEach(serverUser -> {
        if (serverUser.getClientId() != incomingClient.getClientId()) {
            boolean failedToSync = !incomingClient.sendPlayerEliminationStatus(
                serverUser.getClientId(),
                serverUser.getUser().isEliminated()
            );
            if (failedToSync) {
                LoggerUtil.INSTANCE.warning(
                    String.format("Removing disconnected %s from list", serverUser.getDisplayName()));
                disconnect(serverUser);
            }
        }
    });
}
private void sendPlayerEliminationStatus(ServerThread sp) {
    clientsInRoom.values().removeIf(spInRoom -> {
        boolean failedToSend = !spInRoom.sendPlayerEliminationStatus(
            sp.getClientId(),
            sp.getUser().isEliminated()
        );
        if (failedToSend) {
            removeClient(spInRoom);
        }
        return failedToSend;
    });
}

//rc728 12/11/25
private void processBattles() {

    List<Long> active = clientsInRoom.values().stream()
            .map(ServerThread::getClientId)
            .filter(id -> !clientsInRoom.get(id).getUser().isEliminated())
            .collect(Collectors.toList());

    int n = active.size();
    if (n <= 1) return;

    for (Long id : active) {
        points.putIfAbsent(id, 0);
    }

    Map<Long, Integer> roundPoints = new HashMap<>();
    Map<Long, Boolean> lost = new HashMap<>();

    for (Long id : active) {
        roundPoints.put(id, 0);
        lost.put(id, false);
    }

    for (int i = 0; i < n; i++) {
        long p1 = active.get(i);
        Choice c1 = playerChoices.get(p1);

        for (int j = i + 1; j < n; j++) {

            long p2 = active.get(j);
            Choice c2 = playerChoices.get(p2);

            BattleResult r = resolveBattle(c1, c2);

            if (r == BattleResult.ATTACKER_WINS) {
                roundPoints.put(p1, roundPoints.get(p1) + 1);
                lost.put(p2, true);
            } else if (r == BattleResult.DEFENDER_WINS) {
                roundPoints.put(p2, roundPoints.get(p2) + 1);
                lost.put(p1, true);
            }
        }
    }

    for (Long id : active) {
        points.put(id, points.get(id) + roundPoints.get(id));
    }

    for (Long id : active) {

        boolean lostThisRound = lost.get(id);
        boolean wonThisRound = roundPoints.get(id) > 0;

        if (!lostThisRound && !wonThisRound) {
            continue;
        }

        if (lostThisRound && !wonThisRound) {
            ServerThread sp = clientsInRoom.get(id);
            sp.getUser().setEliminated(true);
            sendPlayerEliminationStatus(sp);
            relay(null, sp.getClientName() + " was eliminated!");
        }
    }

    syncPointsToAll();
}


    private void eliminatePlayer(long id) {
        ServerThread player = clientsInRoom.get(id);
        if (player == null) {
            return; 
        }

        if (!player.getUser().isEliminated()) {
            player.getUser().setEliminated(true);   
            sendPlayerEliminationStatus(player);  
            relay(null, player.getClientName() + " was eliminated."); 
        }
    }

   // lifecycle methods

    /** {@inheritDoc} */
    @Override
    protected void onSessionStart() {
        LoggerUtil.INSTANCE.info("onSessionStart() start");

        round = 0;
        points.clear();

        // everyone starts at 0
        clientsInRoom.values().forEach(sp -> {
           sp.setPoints(0);
           points.put(sp.getClientId(), 0);
           sendPlayerPoints(sp);
        });

        changePhase(Phase.IN_PROGRESS);
        LoggerUtil.INSTANCE.info("onSessionStart() end");
        onRoundStart();
    }


    /** {@inheritDoc} */
    //Rc728 12/11/25
    @Override
    protected void onRoundStart() {
        LoggerUtil.INSTANCE.info("onRoundStart() start");
        clientsInRoom.values().forEach(p -> p.setTookTurn(false));
        sendResetTurnStatus();
        resetRoundTimer();
        round++;

        sendGameEvent("Round " + round + " has started");

        // Reset choices only for surviving players
        for (ServerThread p : clientsInRoom.values()) {
            long id = p.getClientId();

            if (!p.getUser().isEliminated()) {
                playerChoices.put(id, null);
            }
        }

        changePhase(Phase.CHOOSING);
        startRoundTimer();

        LoggerUtil.INSTANCE.info("onRoundStart() end");
    }

    private void broadcastChoiceMode() {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.CHOICE_MODE);
        p.setMessage(extrasEnabled ? "5" : "3"); // 5 moves OR 3 moves
        relayPayload(p);  
    }

    public void toggleExtraOptions(ServerThread sender) {
        if (sender.getClientId() != creatorId) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                "Only the session creator can toggle extra options.");
            return;
        }

        if (currentPhase != Phase.READY) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                "You may only toggle during READY phase.");
            return;
        }

        extrasEnabled = !extrasEnabled;

        relay(null, "Extra Options (Electric + Rock): " + 
                    (extrasEnabled ? "ENABLED" : "DISABLED"));

        broadcastChoiceMode();
    }
    /** {@inheritDoc} */
    
    // Note: logic between Turn Start and Turn End is typically handled via timers
    // and user interaction
    /** {@inheritDoc} */

    // Note: logic between Round Start and Round End is typically handled via timers
    // and user interaction
    /** {@inheritDoc} */

    //Rc728 12/11/25
    @Override
    protected void onRoundEnd() {
        LoggerUtil.INSTANCE.info("onRoundEnd() start");
        resetRoundTimer();

        for (ServerThread p : clientsInRoom.values()) {
            long id = p.getClientId();
            boolean isEliminated = p.getUser().isEliminated();
            boolean madeChoice = playerChoices.get(id) != null;

            if (!isEliminated && !madeChoice) {
                ServerThread eliminatedPlayer = clientsInRoom.get(id); 
                if (eliminatedPlayer != null && !eliminatedPlayer.getUser().isEliminated()) {
                    eliminatedPlayer.getUser().setEliminated(true);
                    sendPlayerEliminationStatus(eliminatedPlayer); 
                    relay(null, p.getClientName() + " was eliminated (no choice submitted).");
                }
            }
        }
        processBattles();
        LoggerUtil.INSTANCE.info("onRoundEnd() end");
        long survivors = clientsInRoom.values().stream()
            .map(ServerThread::getClientId)
            .filter(id -> !clientsInRoom.get(id).getUser().isEliminated())
            .count();
        if (survivors <= 1 || round >= 3) {
            onSessionEnd();
            return;
        }
        onRoundStart();
    }

//rc728 12/11/25
  @Override
protected void onSessionEnd() {
    LoggerUtil.INSTANCE.info("onSessionEnd() start");

    List<Long> survivors = clientsInRoom.values().stream()
        .filter(p -> !p.getUser().isEliminated())
        .map(ServerThread::getClientId)
        .collect(Collectors.toList());

    long survivorCount = survivors.size();

    if (survivorCount == 1) {
        long winnerId = survivors.get(0);
        relay(null, "GAME OVER: Winner is " + getClientNameById(winnerId));
    } else if (survivorCount == 0) {
        relay(null, "GAME OVER: All players eliminated — it's a tie!");
    }

    List<Map.Entry<Long, Integer>> sortedScores = points.entrySet().stream()
        .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
        .collect(Collectors.toList());

    relay(null, "=== FINAL SCOREBOARD ===");
    for (Map.Entry<Long, Integer> entry : sortedScores) {
        relay(null, getClientNameById(entry.getKey()) + ": " + entry.getValue() + " points");
    }
    relay(null, "");

    round = 0;
    points.clear();
    playerChoices.clear();

    clientsInRoom.values().forEach(sp -> sp.getUser().setEliminated(false));

    clientsInRoom.values().forEach(sp -> {
        sendPlayerEliminationStatus(sp);  
    });

    sendResetTurnStatus();

    clientsInRoom.values().forEach(sp ->
        sp.sendMessage(Constants.DEFAULT_CLIENT_ID, "RESET_DATA")
    );

    resetReadyStatus();
    changePhase(Phase.READY);

    LoggerUtil.INSTANCE.info("onSessionEnd() end");
}
    private void checkAllChoicesMade() {
    boolean allChosen = clientsInRoom.values().stream()
        .map(ServerThread::getClientId)
        .filter(id -> !clientsInRoom.get(id).getUser().isEliminated())
        .allMatch(id -> playerChoices.get(id) != null);

    if (allChosen) {
        onRoundEnd();
    }
}

    // GameRoom.java - Add this new method
    /**
     * Called by ServerThread when a client sends a ChoicePayload.
     * Records the choice and marks the client as having taken a turn.
     * * @param currentUser
     * @param payload
     */
    // GameRoom.java - Corrected handleChoiceAction(ServerThread currentUser, ChoicePayload payload)
    /**
     * Called by ServerThread when a client sends a ChoicePayload.
     * Records the choice and marks the client as having taken a turn.
     * * @param currentUser
     * @param payload
     */
    //Rc728 12/11/25
    public void handleChoiceAction(ServerThread currentUser, ChoicePayload payload) {
        try {
            checkPlayerInRoom(currentUser);
            checkCurrentPhase(currentUser, Phase.CHOOSING);
            checkIsReady(currentUser);

            if (currentUser.getUser().isEliminated()) {
            currentUser.sendMessage(Constants.DEFAULT_CLIENT_ID,
                "You have been eliminated and cannot pick.");
            return; 
            }
            if (playerChoices.get(currentUser.getClientId()) != null) {
                currentUser.sendMessage(Constants.DEFAULT_CLIENT_ID, "You already selected this round.");
                return;
            }


            playerChoices.put(currentUser.getClientId(), payload.getChoice());

            currentUser.setTookTurn(true);
            sendTurnStatus(currentUser, currentUser.didTakeTurn());
            
            currentUser.sendMessage(Constants.DEFAULT_CLIENT_ID,
                    String.format("Choice recorded: %s", payload.getChoice()));

            checkAllChoicesMade();

        } catch (NotReadyException e) {
            currentUser.sendMessage(Constants.DEFAULT_CLIENT_ID, "You must be ready to take a turn");
            LoggerUtil.INSTANCE.severe("handleChoiceAction exception", e);
        } catch (PlayerNotFoundException e) {
            currentUser.sendMessage(Constants.DEFAULT_CLIENT_ID, "You must be in a GameRoom to choose an action");
            LoggerUtil.INSTANCE.severe("handleChoiceAction exception", e);
        } catch (PhaseMismatchException e) {
            currentUser.sendMessage(Constants.DEFAULT_CLIENT_ID,
                    "You can only choose an action during the CHOOSING phase"); 
            LoggerUtil.INSTANCE.severe("handleChoiceAction exception", e);
        } catch (Exception e) {
            LoggerUtil.INSTANCE.severe("handleChoiceAction exception", e);
        }
    }
    
        // end lifecycle methods


    // send/sync data to ServerThread(s)
    private void syncPlayerPoints(ServerThread incomingClient) {
        clientsInRoom.values().forEach(serverUser -> {
            if (serverUser.getClientId() != incomingClient.getClientId()) {
                boolean failedToSync = !incomingClient.sendPlayerPoints(serverUser.getClientId(),
                        serverUser.getPoints());
                if (failedToSync) {
                    LoggerUtil.INSTANCE.warning(
                            String.format("Removing disconnected %s from list", serverUser.getDisplayName()));
                    disconnect(serverUser);
                }
            }
        });
    }

    
    //Rc728 12/11/25
    private void sendResetTurnStatus() {
        clientsInRoom.values().forEach(spInRoom -> {
            boolean failedToSend = !spInRoom.sendResetTurnStatus();
            if (failedToSend) {
                removeClient(spInRoom);
            }
        });
    }

    private void sendTurnStatus(ServerThread client, boolean tookTurn) {
        clientsInRoom.values().removeIf(spInRoom -> {
            boolean failedToSend = !spInRoom.sendTurnStatus(client.getClientId(), client.didTakeTurn());
            if (failedToSend) {
                removeClient(spInRoom);
            }
            return failedToSend;
        });
    }

    private void syncTurnStatus(ServerThread incomingClient) {
        clientsInRoom.values().forEach(serverUser -> {
            if (serverUser.getClientId() != incomingClient.getClientId()) {
                boolean failedToSync = !incomingClient.sendTurnStatus(serverUser.getClientId(),
                        serverUser.didTakeTurn(), true);
                if (failedToSync) {
                    LoggerUtil.INSTANCE.warning(
                            String.format("Removing disconnected %s from list", serverUser.getDisplayName()));
                    disconnect(serverUser);
                }
            }
        });
    }

    // end send data to ServerThread(s)

    // misc methods
    private void resetTurnStatus() {
        clientsInRoom.values().forEach(sp -> {
            sp.setTookTurn(false);
        });
        sendResetTurnStatus();
    }

    /**
     * Sets `turnOrder` to a shuffled list of players who are ready.
     */
    /**
     * Gets the current player based on the `currentTurnClientId`.
     * 
     * @return
     * @throws MissingCurrentPlayerException
     * @throws PlayerNotFoundException
     */
    /**
     * Gets the next player in the turn order.
     * If the current player is the last in the turn order, it wraps around
     * (round-robin).
     * 
     * @return
     * @throws MissingCurrentPlayerException
     * @throws PlayerNotFoundException
     */
    // start check methods
    // end check methods

    // receive data from ServerThread (GameRoom specific)

    /**
     * Handles the turn action from the client.
     * 
     * @param currentUser
     * @param exampleText (arbitrary text from the client, can be used for
     *                    additional actions or information)
     */

    // end receive data from ServerThread (GameRoom specific)
}