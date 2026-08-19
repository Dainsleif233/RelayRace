package top.syshub.relayrace.common.api;

/**
 * Version-specific player state that is not available on every supported
 * server API. Latest implements it fully; classic leaves it empty.
 */
public final class PlayerExtras {

    private int arrowsInBody;
    private int freezeTicks;

    public int getArrowsInBody() {
        return arrowsInBody;
    }

    public void setArrowsInBody(int arrowsInBody) {
        this.arrowsInBody = arrowsInBody;
    }

    public int getFreezeTicks() {
        return freezeTicks;
    }

    public void setFreezeTicks(int freezeTicks) {
        this.freezeTicks = freezeTicks;
    }
}