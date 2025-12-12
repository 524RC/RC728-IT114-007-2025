package Project.Client.Views;

import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JPanel;
import Project.Client.Client;
import Project.Client.Interfaces.IExtraModeEvent;

public class ReadyView extends JPanel implements IExtraModeEvent {
    private JButton toggleExtrasButton;

    public ReadyView() {
        JButton readyButton = new JButton("Ready");
        readyButton.addActionListener(_ -> {
            try {
                Client.INSTANCE.sendReady();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        });
        this.add(readyButton);

        toggleExtrasButton = new JButton("Toggle Extras");
        toggleExtrasButton.setVisible(false); 
        toggleExtrasButton.addActionListener(e -> {
            try {
                Client.INSTANCE.sendMessage("/toggleExtras");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        this.add(toggleExtrasButton);
    }

    @Override
    public void onHostIdentified(boolean isHost) {
        toggleExtrasButton.setVisible(isHost);
    }

    @Override
    public void onExtraModeChanged(boolean enabled) {}
}
