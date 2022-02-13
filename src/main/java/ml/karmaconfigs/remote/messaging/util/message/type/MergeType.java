package ml.karmaconfigs.remote.messaging.util.message.type;

import ml.karmaconfigs.remote.messaging.util.message.MessageDataOutput;

/**
 * Valid merge types for {@link MessageDataOutput}
 */
public enum MergeType {
    /**
     * Do nothing, also used as default merge type
     */
    NONE(-1),
    /**
     * Append only the not in data of the new message data input object
     */
    DIFFERENCE(0),
    /**
     * Replace matching data with the merged one of the new message data input object
     */
    REPLACE(1),
    /**
     * Replace matching data or set if not exists
     */
    REPLACE_OR_ADD(2);

    /**
     * Merge id field
     */
    private final int merge_id;

    /**
     * Merge type
     *
     * @param id the merge id
     */
    MergeType(final int id) {
        merge_id = id;
    }

    /**
     * Get the merge type id
     *
     * @return the merge type id
     */
    public final int getId() {
        return merge_id;
    }

    /**
     * Get the merge type based on method name
     *
     * @param method the method name
     * @return the merge type
     */
    public static MergeType fromName(final String method) {
        switch (method.toLowerCase()) {
            case "difference":
            case "diff":
            case "d":
                return MergeType.DIFFERENCE;
            case "replace":
            case "r":
                return MergeType.REPLACE;
            case "add":
            case "a":
                return MergeType.REPLACE_OR_ADD;
            default:
                throw new IllegalArgumentException("Unknown merge type: " + method);
        }
    }
}
