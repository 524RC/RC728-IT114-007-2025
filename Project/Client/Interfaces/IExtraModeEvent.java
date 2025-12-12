package Project.Client.Interfaces;

public interface IExtraModeEvent extends IClientEvents {

    // already had this:
    void onExtraModeChanged(boolean enabled);

    // ⬇️ add THIS:
    void onHostIdentified(boolean isHost);
}
