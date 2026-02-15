package network.twink.smpcleanroom.bypass;

public abstract class AbstractBypass implements IBypass {

    private final boolean immune;

    public AbstractBypass() {
        this(false);
    }

    public AbstractBypass(boolean immune) {
        this.immune = immune;
    }

    /**
     * Immunity is whether this bypass observed the rules set by the mode configuration value
     * By default, this will only be used by the permission and the spawn radius bypass.
     * @return Whether this bypass is immune.
     */
    public boolean isImmune() {
        return immune;
    }
}
