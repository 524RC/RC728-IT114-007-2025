package Project.Client.Views;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

import Project.Client.CardViewName;
import Project.Client.Client;
import Project.Client.Interfaces.ICardControls;
import Project.Client.Interfaces.IPhaseEvent;
import Project.Client.Interfaces.IExtraModeEvent;
import Project.Common.Phase;

public class GameView extends JPanel implements IPhaseEvent {
    private PlayView playView;
    private CardLayout cardLayout;
    private static final String READY_PANEL = "READY";
    private static final String PLAY_PANEL = "PLAY";

    public GameView(ICardControls controls) {
        super(new BorderLayout());

        JPanel gameContainer = new JPanel(new CardLayout());
        cardLayout = (CardLayout) gameContainer.getLayout();
        this.setName(CardViewName.GAME_SCREEN.name());
        Client.INSTANCE.registerCallback(this);

        ReadyView readyView = new ReadyView();
        Client.INSTANCE.registerCallback(readyView);
        gameContainer.add(READY_PANEL, readyView);

        playView = new PlayView(PLAY_PANEL);
        Client.INSTANCE.registerCallback(playView);

        gameContainer.add(READY_PANEL, readyView);
        gameContainer.add(PLAY_PANEL, playView);


        GameEventsView gameEventsView = new GameEventsView();
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, gameContainer, gameEventsView);
        splitPane.setResizeWeight(0.7);

        playView.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                splitPane.setDividerLocation(0.7);
            }
        });
        playView.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                playView.revalidate();
                playView.repaint();
            }
        });

        this.add(splitPane, BorderLayout.CENTER);
        controls.registerView(CardViewName.GAME_SCREEN.name(), this);
        setVisible(false);
    }
    @Override
    public void onReceivePhase(Phase phase) {
        System.out.println("Received phase: " + phase.name());

        switch (phase) {
            case READY:
                cardLayout.show(playView.getParent(), READY_PANEL);
                break;

            case IN_PROGRESS:
            case CHOOSING:
                cardLayout.show(playView.getParent(), PLAY_PANEL);
                break;
        }

        playView.changePhase(phase);
    }

}
