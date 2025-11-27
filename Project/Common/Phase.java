package Project.Common;

public enum Phase {
    READY,          // Waiting for all players to ready-up
    CHOOSING,       // Players selecting R/P/S
    BATTLE,         // Battles being processed
    RESULTS,        // Showing results of the round
    IN_PROGRESS,    // General running state (optional)
    END;  
}