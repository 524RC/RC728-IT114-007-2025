package Project.Client.Interfaces;

public interface IEliminationEvent extends IClientEvents {
    

    void onUserEliminated(long clientId,boolean isEliminated);
}
