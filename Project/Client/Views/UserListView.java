package Project.Client.Views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashMap;
import java.util.List;

import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import Project.Client.Client;
import Project.Client.Interfaces.IConnectionEvents;
import Project.Client.Interfaces.IEliminationEvent;
import Project.Client.Interfaces.IPointsEvent;
import Project.Client.Interfaces.IReadyEvent;
import Project.Client.Interfaces.IRoomEvents;
import Project.Client.Interfaces.ITurnEvent;
import Project.Common.Constants;
import Project.Common.LoggerUtil;
import Project.Client.Interfaces.IMessageEvents;


/**
 * UserListView represents a UI component that displays a list of users.
 */
public class UserListView extends JPanel
        implements IConnectionEvents, IRoomEvents, IReadyEvent, IPointsEvent, ITurnEvent, IMessageEvents, IEliminationEvent {
    private final JPanel userListArea;
    private final GridBagConstraints lastConstraints; // Keep track of the last constraints for the glue
    private final HashMap<Long, UserListItem> userItemsMap; // Maintain a map of client IDs to UserListItems

    public UserListView() {
        super(new BorderLayout(10, 10));
        userItemsMap = new HashMap<>();

        JPanel content = new JPanel(new GridBagLayout());
        userListArea = content;

        JScrollPane scroll = new JScrollPane(userListArea);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        this.add(scroll, BorderLayout.CENTER);

        // Add vertical glue to push items to the top
        lastConstraints = new GridBagConstraints();
        lastConstraints.gridx = 0;
        lastConstraints.gridy = GridBagConstraints.RELATIVE;
        lastConstraints.weighty = 1.0;
        lastConstraints.fill = GridBagConstraints.VERTICAL;
        userListArea.add(Box.createVerticalGlue(), lastConstraints);
        Client.INSTANCE.registerCallback(this);
    }

    /**
     * Adds a user to the list.
     */
    private void addUserListItem(long clientId, String clientName) {
        SwingUtilities.invokeLater(() -> {
            if (userItemsMap.containsKey(clientId)) {
                LoggerUtil.INSTANCE.warning("User already in the list: " + clientName);
                return;
            }
            LoggerUtil.INSTANCE.info("Adding user to list: " + clientName);
            UserListItem userItem = new UserListItem(clientId, clientName);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = userListArea.getComponentCount() - 1;
            gbc.weightx = 1;
            gbc.anchor = GridBagConstraints.NORTH;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(0, 0, 5, 5);
            // Remove the last glue component if it exists
            if (lastConstraints != null) {
                int index = userListArea.getComponentCount() - 1;
                if (index > -1) {
                    userListArea.remove(index);
                }
            }
            userListArea.add(userItem, gbc);
            userListArea.add(Box.createVerticalGlue(), lastConstraints);
            userItemsMap.put(clientId, userItem);
            userListArea.revalidate();
            userListArea.repaint();
        });
    }


    

    /**
     * Removes a user from the list.
     */
    private void removeUserListItem(long clientId) {
        SwingUtilities.invokeLater(() -> {
            LoggerUtil.INSTANCE.info("Removing user list item for id " + clientId);
            try {
                UserListItem item = userItemsMap.remove(clientId);
                if (item != null) {
                    userListArea.remove(item);
                    userListArea.revalidate();
                    userListArea.repaint();
                }
            } catch (Exception e) {
                LoggerUtil.INSTANCE.severe("Error removing user list item", e);
            }
        });
    }

    /**
     * Clears the user list.
     */
    private void clearUserList() {
        SwingUtilities.invokeLater(() -> {
            LoggerUtil.INSTANCE.info("Clearing user list");
            try {
                userItemsMap.clear();
                userListArea.removeAll();
                userListArea.revalidate();
                userListArea.repaint();
            } catch (Exception e) {
                LoggerUtil.INSTANCE.severe("Error clearing user list", e);
            }
        });
    }

      @Override
public void onUserEliminated(long clientId, boolean isEliminated) {

    LoggerUtil.INSTANCE.info("onUserEliminated fired for clientId=" + clientId + " isEliminated=" + isEliminated);

    SwingUtilities.invokeLater(() -> {
        UserListItem item = userItemsMap.get(clientId);

        if (item != null) {
            LoggerUtil.INSTANCE.info("Setting eliminated status on UserListItem id=" + clientId);
            item.setEliminated(isEliminated);
        } else {
            LoggerUtil.INSTANCE.warning("UserListItem not found for eliminated client " + clientId);
        }
    });
}

    @Override
    public void onReceiveRoomList(List<String> rooms, String message) {
        // unused
    }

    @Override
    public void onRoomAction(long clientId, String roomName, boolean isJoin, boolean isQuiet) {
        if (clientId == Constants.DEFAULT_CLIENT_ID) {
            clearUserList();
            return;
        }
        String displayName = Client.INSTANCE.getDisplayNameFromId(clientId);
        if (isJoin) {
            addUserListItem(clientId, displayName);
        } else {
            removeUserListItem(clientId);
        }
    }

    @Override
    public void onClientDisconnect(long clientId) {
        removeUserListItem(clientId);
    }

    @Override
    public void onReceiveClientId(long id) {
        
    }

    @Override
public void onTookTurn(long clientId, boolean didTakeTurn) {
    if (clientId == Constants.DEFAULT_CLIENT_ID) {
        SwingUtilities.invokeLater(() ->
            userItemsMap.values().forEach(u -> u.setPending())
        );
        return;
    }

    SwingUtilities.invokeLater(() -> {
        if (userItemsMap.containsKey(clientId)) {
            if (didTakeTurn) userItemsMap.get(clientId).setTurn(true);
        }
    });
}


    @Override
    public void onPointsUpdate(long clientId, int points) {
        if (clientId == Constants.DEFAULT_CLIENT_ID) {
            SwingUtilities.invokeLater(() -> {
                try {
                    userItemsMap.values().forEach(u -> u.setPoints(-1));// reset all
                } catch (Exception e) {
                    LoggerUtil.INSTANCE.severe("Error resetting user items", e);
                }
            });
        } else if (userItemsMap.containsKey(clientId)) {
            SwingUtilities.invokeLater(() -> {
                try {
                    userItemsMap.get(clientId).setPoints(points);
                } catch (Exception e) {
                    LoggerUtil.INSTANCE.severe("Error setting user item", e);
                }

            });
        }
    }

    @Override
public void onReceiveReady(long clientId, boolean isReady, boolean isQuiet) {
    SwingUtilities.invokeLater(() -> {


        if (clientId == Constants.DEFAULT_CLIENT_ID) {
            userItemsMap.values().forEach(u -> u.setPending());
            return;
        }

        // Show READY in the scoreboard
        if (userItemsMap.containsKey(clientId)) {
            userItemsMap.get(clientId).setReady(isReady);
        }
    });
}


 // UserListView.java - Inside onMessageReceive()
// UserListView.java - CORRECTED onMessageReceive()


@Override
public void onMessageReceive(long clientId, String message) {
/*
    if (message != null && message.startsWith("ELIMINATED:")) {

        SwingUtilities.invokeLater(() -> {

            System.out.println("DEBUG: UserListView received elimination message: " + message);
            System.out.println("DEBUG: userItemsMap keys: " + userItemsMap.keySet());   // <--- ADD HERE

            try {
                int colonIndex = message.indexOf(":");
                String idString = message.substring(colonIndex + 1).trim();
                long id = Long.parseLong(idString);

                UserListItem item = userItemsMap.get(id);
                if (item != null) {
                    System.out.println("DEBUG: Found user " + id + ", setting eliminated = true");
                    item.setEliminated(true);
                } else {
                    System.out.println("DEBUG: Could NOT find user " + id + " in map!");
                }
            } catch (Exception e) {
                LoggerUtil.INSTANCE.severe("Failed to parse elimination message: " + message, e);
            }
        });

        return;
    }
         */
}
 
}
