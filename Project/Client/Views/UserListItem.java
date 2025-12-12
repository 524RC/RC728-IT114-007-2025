package Project.Client.Views;

import java.awt.Color;
import javax.swing.BoxLayout;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
//Rc728 12/11/25
public class UserListItem extends JPanel {

    private final JEditorPane nameLabel;
    private final JEditorPane statusLabel;
    private final JEditorPane pointsLabel;
    private final JEditorPane pointsLineLabel;

    private final String displayName;
    private boolean eliminated = false;

    //Rc728 12/11/25
    public UserListItem(long clientId, String displayName) {
        this.displayName = displayName;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);


        nameLabel = new JEditorPane("text/html", displayName);
        nameLabel.setEditable(false);
        nameLabel.setOpaque(false);
        nameLabel.setBorder(null);
        add(nameLabel);

        pointsLineLabel = new JEditorPane("text/html", "Points: 0");
        pointsLineLabel.setEditable(false);
        pointsLineLabel.setOpaque(false);
        pointsLineLabel.setBorder(null);
        add(pointsLineLabel);

        statusLabel = new JEditorPane("text/plain", "PENDING");
        statusLabel.setEditable(false);
        statusLabel.setOpaque(false);
        statusLabel.setBorder(null);
        statusLabel.setForeground(Color.GRAY);
        add(statusLabel);

        pointsLabel = new JEditorPane("text/plain", "");
        pointsLabel.setEditable(false);
        pointsLabel.setOpaque(false);
        pointsLabel.setBorder(null);
        add(pointsLabel);

        setPoints(-1);
    }

    public void setPending() {
        if (eliminated) return;
        statusLabel.setText("PENDING");
        statusLabel.setForeground(Color.GRAY);
    }

    //rc728 12/11/25
    public void setTurn(boolean didTakeTurn) {
        if (eliminated) return;

        if (didTakeTurn) {
            statusLabel.setText("PICKED");
            statusLabel.setForeground(Color.GREEN.darker());
        } else {
            setPending();
        }
    }

    public void setReady(boolean isReady) {
        if (eliminated) return;

        if (isReady) {
            statusLabel.setText("READY");
            statusLabel.setForeground(new Color(0, 128, 255)); // blue
        } else {
            setPending();
        }
    }

    //Rc728 12/11/25
    public void setEliminated(boolean isEliminated) {
        eliminated = isEliminated;

        if (isEliminated) {
            statusLabel.setText("ELIMINATED");
            statusLabel.setForeground(Color.RED);
        } else {
            setPending(); 
        }
    }

    public boolean isEliminated() {
        return eliminated;
    }

    public void setPoints(int pts) {
        if (pts < 0) {
            pointsLineLabel.setText("Points: 0");
        } else {
            pointsLineLabel.setText("Points: " + pts);
        }
        pointsLineLabel.repaint();
    }
}
