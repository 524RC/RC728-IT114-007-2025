package Project.Client.Views;

import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JPanel;

import Project.Client.Client;
import Project.Client.Interfaces.IExtraModeEvent;
import Project.Common.Phase;

//rc728 12/11/25
public class PlayView extends JPanel implements IExtraModeEvent {

    private final JPanel buttonPanel = new JPanel();

    private JButton electricButton;
    private JButton rockButton;

    public PlayView(String name) {
        this.setName(name);

        JButton fire = makeButton("Fire", "fire");
        JButton grass = makeButton("Grass", "grass");
        JButton water = makeButton("Water", "water");

        electricButton = makeButton("Electric", "electric");
        rockButton = makeButton("Rock", "rock");

        buttonPanel.add(fire);
        buttonPanel.add(grass);
        buttonPanel.add(water);
        buttonPanel.add(electricButton);
        buttonPanel.add(rockButton);

        this.add(buttonPanel);

        // Hide extras until server enables them
        enableExtraButtons(false);
    }

    //rc728 12/11/25
    private JButton makeButton(String label, String msg) {
        JButton b = new JButton(label);
        b.addActionListener(_ -> {
            try {
                Client.INSTANCE.sendChoice(msg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return b;
    }

    public void enableExtraButtons(boolean enabled) {
        electricButton.setVisible(enabled);
        rockButton.setVisible(enabled);
    }

    @Override
    public void onExtraModeChanged(boolean enabled) {
        enableExtraButtons(enabled);
    }

    @Override
    public void onHostIdentified(boolean isHost) {
        // PlayView doesn't care who the host is (only ReadyView uses this),
        // so we can safely leave this empty.
    }
    
    public void changePhase(Phase phase) {
        boolean show = (phase == Phase.IN_PROGRESS || phase == Phase.CHOOSING);
        buttonPanel.setVisible(show);
    }
    

}
