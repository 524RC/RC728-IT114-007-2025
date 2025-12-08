package Project.Server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import Project.Common.ChoicePayload;
import Project.Common.ChoicePayload.Choice;
import Project.Common.Constants;
import Project.Common.LoggerUtil;
import Project.Common.Phase;
import Project.Common.PointsPayload;
import Project.Common.TimedEvent;
import Project.Common.TimerType;
import Project.Exceptions.MissingCurrentPlayerException;
import Project.Exceptions.NotPlayersTurnException;
import Project.Exceptions.NotReadyException;
import Project.Exceptions.PhaseMismatchException;
import Project.Exceptions.PlayerNotFoundException;

public class GameRoom extends BaseGameRoom {

    private final Map<Long, ChoicePayload.Choice> playerChoices = new HashMap<>();
    private final Map<Long, Boolean> eliminated = new HashMap<>();
    private final Map<Long, Integer> points = new HashMap<>();


    // used for general rounds (usually phase-based turns)
    private TimedEvent roundTimer = null;

    // used for granular turn handling (usually turn-order turns)
    private TimedEvent turnTimer = null;
    private List<ServerThread> turnOrder = new ArrayList<>();
    private long currentTurnClientId = Constants.DEFAULT_CLIENT_ID;
    private int round = 0;

    public GameRoom(String name) {
        super(name);
    }

    /** {@inheritDoc} */
    @Override
    protected void onClientAdded(ServerThread sp) {
        // sync GameRoom state to new client

        syncCurrentPhase(sp);
        // sync only what's necessary for the specific phase
        // if you blindly sync everything, you'll get visual artifacts/discrepancies
        syncReadyStatus(sp);
        if (currentPhase != Phase.READY) {
            syncTurnStatus(sp); // turn/ready use the same visual process so ensure turn status is only called
            syncTurnStatus(sp);
            // outside of ready phase
            syncPlayerPoints(sp);
        }

    }

    /** {@inheritDoc} */
    @Override
    protected void onClientRemoved(ServerThread sp) {
        // added after Summer 2024 Demo
        // Stops the timers so room can clean up
        LoggerUtil.INSTANCE.info("Player Removed, remaining: " + clientsInRoom.size());
        long removedClient = sp.getClientId();
        turnOrder.removeIf(player -> player.getClientId() == sp.getClientId());
        if (clientsInRoom.isEmpty()) {
            resetReadyTimer();
            resetTurnTimer();
            resetRoundTimer();
            onSessionEnd();
        } else if (removedClient == currentTurnClientId) {
            onTurnStart();
        }
    }

    // timer handlers
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

    private void startTurnTimer() {
        turnTimer = new TimedEvent(30, () -> onTurnEnd());
        turnTimer.setTickCallback((time) -> {
            System.out.println("Turn Time: " + time);
            sendCurrentTime(TimerType.TURN, time);
        });
    }

    private void resetTurnTimer() {
        if (turnTimer != null) {
            turnTimer.cancel();
            turnTimer = null;
            sendCurrentTime(TimerType.TURN, -1);
        }
    }
    // end timer handlers
//rc728 11/26/25
     private void syncPointsToAll() {
    for (Map.Entry<Long, Integer> entry : points.entrySet()) {
        long clientId = entry.getKey();
        int pts = entry.getValue();

        String message = String.format("POINTS_UPDATE:%d:%d", clientId, pts);
        relay(null, message);
        }
    }

    public enum BattleResult { ATTACKER_WINS, DEFENDER_WINS, TIE }

    private BattleResult resolveBattle(ChoicePayload.Choice aChoice, ChoicePayload.Choice dChoice){
        if (aChoice.equals(dChoice)) return BattleResult.TIE;

        if (aChoice == Choice.SCISSORS && dChoice == Choice.PAPER){
             return BattleResult.ATTACKER_WINS;
        }
        if (aChoice == Choice.PAPER && dChoice == Choice.ROCK){
            return BattleResult.ATTACKER_WINS;
        } 
        if (aChoice == Choice.ROCK && dChoice == Choice.SCISSORS){
            return BattleResult.ATTACKER_WINS;
        }
        return BattleResult.DEFENDER_WINS;
    }
    private String getClientNameById(long id) {
        ServerThread p = clientsInRoom.get(id);
        if (p == null){
            return "Unknown(" + id + ")";
        } 
        return p.getClientName();
    }

    private void processBattles() {
    //rc728 11/26/25
    // get all active players
    List<Long> active = clientsInRoom.values().stream()
            .map(ServerThread::getClientId)
            .filter(id -> !eliminated.getOrDefault(id, false))
            .collect(Collectors.toList());

    int n = active.size();


    if (n <= 1) {
        if (n == 1) {
            relay(null, "Winner is: " + getClientNameById(active.get(0)));
        } else {
            relay(null, "All players eliminated. It's a tie!");
        }
        onSessionEnd();
        LoggerUtil.INSTANCE.info("processBattles() end: early termination");
        return;
    }

    for (Long id : active) {
        points.putIfAbsent(id, 0);

    for (int i = 0; i < n; i++) {

        long attacker = active.get(i);
        long defender = active.get((i + 1) % n);      // next player
        long prevAttacker = active.get((i - 1 + n) % n);

        Choice aChoice = playerChoices.get(attacker);
        Choice dChoice = playerChoices.get(defender);
        Choice prevChoice = playerChoices.get(prevAttacker);

        //  Battle 1: attacker -> defender 
        BattleResult attackResult = resolveBattle(aChoice, dChoice);

        relay(null, String.format(
            "Battle: %s(%s) attacks %s(%s) → %s",
            getClientNameById(attacker), aChoice,
            getClientNameById(defender), dChoice,
            attackResult
        ));

        if (attackResult == BattleResult.ATTACKER_WINS) {
            eliminated.put(defender, true);
            points.put(attacker, points.get(attacker) + 1);
        } else if (attackResult == BattleResult.DEFENDER_WINS) {
            eliminated.put(attacker, true);
            points.put(defender, points.get(defender) + 1);
        }

        // Skip second battle if someone was eliminated in battle 1
        if (eliminated.getOrDefault(attacker, false)
         || eliminated.getOrDefault(prevAttacker, false)) {
            continue;
        }

        // Battle 2: previous player attacks current player 
        BattleResult defendResult = resolveBattle(prevChoice, aChoice);

        relay(null, String.format(
            "Battle: %s(%s) attacks %s(%s) → %s",
            getClientNameById(prevAttacker), prevChoice,
            getClientNameById(attacker), aChoice,
            defendResult
            ));

        if (defendResult == BattleResult.ATTACKER_WINS) {
                eliminated.put(attacker, true);
                points.put(prevAttacker, points.get(prevAttacker) + 1);
        }else if (defendResult == BattleResult.DEFENDER_WINS) {
                eliminated.put(prevAttacker, true);
                points.put(attacker, points.get(attacker) + 1);
        }
    }

        syncPointsToAll();
    }
}
    // lifecycle methods

    /** {@inheritDoc} */
    @Override
    protected void onSessionStart() {
        LoggerUtil.INSTANCE.info("onSessionStart() start");
        round = 0;
        changePhase(Phase.IN_PROGRESS);
        currentTurnClientId = Constants.DEFAULT_CLIENT_ID;
        setTurnOrder();
        round = 0;
        LoggerUtil.INSTANCE.info("onSessionStart() end");
        onRoundStart();
    }

    /** {@inheritDoc} */
    //Rc728 11/24/25
    @Override
    protected void onRoundStart() {
        LoggerUtil.INSTANCE.info("onRoundStart() start");
        resetRoundTimer();
        resetTurnStatus();
        round++;
        // relay(null, String.format("Round %d has started", round));
        sendGameEvent(String.format("Round %d has started", round));
        // startRoundTimer(); Round timers aren't needed for turns
        // if you do decide to use it, ensure it's reasonable and based on the number of
        // players
        for (ServerThread p : clientsInRoom.values()) {
            long id = p.getClientId();
            if (!eliminated.getOrDefault(id, false)) {
                playerChoices.put(id, null);
            }                  
        }
        changePhase(Phase.CHOOSING);
        relay(null, "You're on round " + round);
        startRoundTimer();
        LoggerUtil.INSTANCE.info("onRoundStart() end");
        onTurnStart();
    }

    /** {@inheritDoc} */
    @Override
    protected void onTurnStart() {
        LoggerUtil.INSTANCE.info("onTurnStart() start");
        resetTurnTimer();
        try {
            ServerThread currentPlayer = getNextPlayer();
            // relay(null, String.format("It's %s's turn", currentPlayer.getDisplayName()));
            sendGameEvent(String.format("It's %s's turn", currentPlayer.getDisplayName()));
        } catch (MissingCurrentPlayerException | PlayerNotFoundException e) {

            e.printStackTrace();
        }
        startTurnTimer();
        LoggerUtil.INSTANCE.info("onTurnStart() end");
    }

    // Note: logic between Turn Start and Turn End is typically handled via timers
    // and user interaction
    /** {@inheritDoc} */
    @Override
    protected void onTurnEnd() {
        LoggerUtil.INSTANCE.info("onTurnEnd() start");
        resetTurnTimer(); // reset timer if turn ended without the time expiring
        try {
            // optionally can use checkAllTookTurn();
            if (isLastPlayer()) {
                // if the current player is the last player in the turn order, end the round
                onRoundEnd();
            } else {
                onTurnStart();
            }
        } catch (MissingCurrentPlayerException | PlayerNotFoundException e) {

            e.printStackTrace();
        }
        LoggerUtil.INSTANCE.info("onTurnEnd() end");
    }
        private void checkAllChoicesMade() {
        boolean allChosen = clientsInRoom.values().stream()
        .map(ServerThread::getClientId)
        .filter(id -> !eliminated.getOrDefault(id, false))
        .allMatch(id -> playerChoices.get(id) != null);
        if (allChosen) {
            onRoundEnd();
        }
    }

    // Note: logic between Round Start and Round End is typically handled via timers
    // and user interaction
    /** {@inheritDoc} */
    @Override
    protected void onRoundEnd() {
        LoggerUtil.INSTANCE.info("onRoundEnd() start");
        resetRoundTimer(); // reset timer if round ended without the time expiring
        
    // Round timer should not continue into next round
        resetRoundTimer();

    // Condition from spec:
    // Mark players who did NOT submit a choice as eliminated.
        for (ServerThread p : clientsInRoom.values()) {
            long id = p.getClientId();
            boolean isEliminated = eliminated.getOrDefault(id, false);
            boolean madeChoice = playerChoices.get(id) != null;

            if (!isEliminated && !madeChoice) {
                eliminated.put(id, true);
                relay(null, p.getClientName() + " was eliminated (no choice submitted).");
            }
        }
        processBattles();

        LoggerUtil.INSTANCE.info("onRoundEnd() end");
         long survivors = eliminated.values().stream().filter(e -> !e).count();

        if (survivors <= 1 || round >= 3) {
            onSessionEnd();
            return;
        }

        onRoundStart();
    }


    /** {@inheritDoc} */
    @Override
    protected void onSessionEnd() {
        LoggerUtil.INSTANCE.info("onSessionEnd() start");
        turnOrder.clear();
        currentTurnClientId = Constants.DEFAULT_CLIENT_ID;
        resetReadyStatus();
        resetTurnStatus();
        changePhase(Phase.READY); 
        List<Long> survivors = eliminated.entrySet().stream()
            .filter(e -> !e.getValue())
            .map(Map.Entry::getKey)
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

        points.clear();
        eliminated.clear();
        playerChoices.clear();

        sendResetTurnStatus();                // clears tookTurn flags client-side
        clientsInRoom.values().forEach(sp -> sp.sendMessage(
            Constants.DEFAULT_CLIENT_ID,
            "RESET_DATA"                 // tells client to reset its UI/state
        ));

        resetReadyStatus();                   // marks all players as NOT READY
        changePhase(Phase.READY);

        LoggerUtil.INSTANCE.info("onSessionEnd() end");
        
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
    //Rc728 11/26/25
    public void handleChoiceAction(ServerThread currentUser, ChoicePayload payload) {
        try {
            checkPlayerInRoom(currentUser);
           

            if (currentUser.didTakeTurn()) {
                currentUser.sendMessage(Constants.DEFAULT_CLIENT_ID, "You have already taken your turn this round");
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

    private void sendPlayerPoints(ServerThread sp) {
        clientsInRoom.values().removeIf(spInRoom -> {
            boolean failedToSend = !spInRoom.sendPlayerPoints(sp.getClientId(), sp.getPoints());
            if (failedToSend) {
                removeClient(spInRoom);
            }
            return failedToSend;
        });
    }

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
    private void setTurnOrder() {
        turnOrder.clear();
        turnOrder = clientsInRoom.values().stream().filter(ServerThread::isReady).collect(Collectors.toList());
        Collections.shuffle(turnOrder);
    }

    /**
     * Gets the current player based on the `currentTurnClientId`.
     * 
     * @return
     * @throws MissingCurrentPlayerException
     * @throws PlayerNotFoundException
     */
    private ServerThread getCurrentPlayer() throws MissingCurrentPlayerException, PlayerNotFoundException {
        // quick early exit
        if (currentTurnClientId == Constants.DEFAULT_CLIENT_ID) {
            throw new MissingCurrentPlayerException("Current Player not set");
        }
        return turnOrder.stream()
                .filter(sp -> sp.getClientId() == currentTurnClientId)
                .findFirst()
                // this shouldn't occur but is included as a "just in case"
                .orElseThrow(() -> new PlayerNotFoundException("Current player not found in turn order"));
    }

    /**
     * Gets the next player in the turn order.
     * If the current player is the last in the turn order, it wraps around
     * (round-robin).
     * 
     * @return
     * @throws MissingCurrentPlayerException
     * @throws PlayerNotFoundException
     */
    private ServerThread getNextPlayer() throws MissingCurrentPlayerException, PlayerNotFoundException {
        int index = 0;
        if (currentTurnClientId != Constants.DEFAULT_CLIENT_ID) {
            index = turnOrder.indexOf(getCurrentPlayer()) + 1;
            if (index >= turnOrder.size()) {
                index = 0;
            }
        }
        ServerThread nextPlayer = turnOrder.get(index);
        currentTurnClientId = nextPlayer.getClientId();
        return nextPlayer;
    }

    /**
     * Checks if the current player is the last player in the turn order.
     * 
     * @return
     * @throws MissingCurrentPlayerException
     * @throws PlayerNotFoundException
     */
    private boolean isLastPlayer() throws MissingCurrentPlayerException, PlayerNotFoundException {
        // check if the current player is the last player in the turn order
        return turnOrder.indexOf(getCurrentPlayer()) == (turnOrder.size() - 1);
    }

    private void checkAllTookTurn() {
        int numReady = clientsInRoom.values().stream()
                .filter(sp -> sp.isReady())
                .toList().size();
        int numTookTurn = clientsInRoom.values().stream()
                // ensure to verify the isReady part since it's against the original list
                .filter(sp -> sp.isReady() && sp.didTakeTurn())
                .toList().size();
        if (numReady == numTookTurn) {
            //relay(null,
            // String.format("All players have taken their turn (%d/%d) ending the round",
            // numTookTurn, numReady));
            sendGameEvent(
                    String.format("All players have taken their turn (%d/%d) ending the round", numTookTurn, numReady));
            onRoundEnd();
        }
    }

    // start check methods
    private void checkCurrentPlayer(long clientId) throws NotPlayersTurnException {
        if (currentTurnClientId != clientId) {
            throw new NotPlayersTurnException("You are not the current player");
        }
    }

    // end check methods

    // receive data from ServerThread (GameRoom specific)

    /**
     * Handles the turn action from the client.
     * 
     * @param currentUser
     * @param exampleText (arbitrary text from the client, can be used for
     *                    additional actions or information)
     */
    protected void handleTurnAction(ServerThread currentUser, String exampleText) {
        // check if the client is in the room
        try {
            checkPlayerInRoom(currentUser);
            checkCurrentPhase(currentUser, Phase.IN_PROGRESS);
            checkCurrentPlayer(currentUser.getClientId());
            checkIsReady(currentUser);
            if (currentUser.didTakeTurn()) {
                currentUser.sendMessage(Constants.DEFAULT_CLIENT_ID, "You have already taken your turn this round");
                return;
            }
            // example points
            int points = new Random().nextInt(4) == 3 ? 1 : 0;
            sendGameEvent(String.format("%s %s", currentUser.getDisplayName(),
                    points > 0 ? "gained a point" : "didn't gain a point"));
            if (points > 0) {
                currentUser.changePoints(points);
                sendPlayerPoints(currentUser);
            }
            currentUser.setTookTurn(true);
            sendTurnStatus(currentUser, currentUser.didTakeTurn());

            // TODO handle example text possibly or other turn related intention from client
            sendTurnStatus(currentUser, currentUser.didTakeTurn());
            // finished processing the turn
            onTurnEnd();
            checkAllTookTurn();

        } catch (NotPlayersTurnException e) {
            currentUser.sendMessage(Constants.DEFAULT_CLIENT_ID, "It's not your turn");
            LoggerUtil.INSTANCE.severe("handleTurnAction exception", e);
        } catch (NotReadyException e) {
            // The check method already informs the currentUser
            LoggerUtil.INSTANCE.severe("handleTurnAction exception", e);
        } catch (PlayerNotFoundException e) {
            currentUser.sendMessage(Constants.DEFAULT_CLIENT_ID, "You must be in a GameRoom to do the ready check");
            LoggerUtil.INSTANCE.severe("handleTurnAction exception", e);
        } catch (PhaseMismatchException e) {
            currentUser.sendMessage(Constants.DEFAULT_CLIENT_ID,
                    "You can only take a turn during the IN_PROGRESS phase");
            LoggerUtil.INSTANCE.severe("handleTurnAction exception", e);
        } catch (Exception e) {
            LoggerUtil.INSTANCE.severe("handleTurnAction exception", e);
        }
    }

    // end receive data from ServerThread (GameRoom specific)
}