package top.syshub.relayrace.common;

/**
 * Milestone types tracked by the relay race milestone board.
 *
 * <p>Each milestone has:
 * <ul>
 *   <li>{@code labelKey} — translation key for the display line on the
 *       sidebar board. When {@code null} the milestone adds no display
 *       line (only updates the status).</li>
 *   <li>{@code statusKey} — translation key for the "current status" line.
 *       When {@code null} the milestone does not change the status.</li>
 *   <li>{@code tier} — ordering tier for status progression. A milestone
 *       only advances the status if its tier is greater than or equal to
 *       the current tier. Milestones with the same tier (BASTION /
 *       FORTRESS) can be achieved in any order, the latest one overriding
 *       the status label.</li>
 * </ul>
 */
public enum MilestoneType {

    START_GAME("milestone.start_game", "milestone.status.started", 0),
    ENTER_NETHER("milestone.enter_nether", "milestone.status.entered_nether", 1),
    REACH_BASTION("milestone.reach_bastion", "milestone.status.entered_bastion", 2),
    REACH_FORTRESS("milestone.reach_fortress", "milestone.status.entered_fortress", 2),
    GOING_TO_STRONGHOLD(null, "milestone.status.going_to_stronghold", 3),
    REACH_STRONGHOLD("milestone.reach_stronghold", "milestone.status.entered_stronghold", 4),
    ENTER_END("milestone.enter_end", "milestone.status.entered_end", 5),
    DEFEAT_DRAGON("milestone.defeat_dragon", null, -1),
    CLEAR_GAME("milestone.clear_game", null, -1);

    private final String labelKey;
    private final String statusKey;
    private final int tier;

    MilestoneType(String labelKey, String statusKey, int tier) {
        this.labelKey = labelKey;
        this.statusKey = statusKey;
        this.tier = tier;
    }

    public String getLabelKey() {
        return labelKey;
    }

    public String getStatusKey() {
        return statusKey;
    }

    public int getTier() {
        return tier;
    }

    /**
     * Whether this milestone adds a display line on the sidebar board.
     */
    public boolean hasDisplayLine() {
        return labelKey != null;
    }

    /**
     * Whether this milestone updates the "current status" line.
     */
    public boolean hasStatus() {
        return statusKey != null;
    }
}
