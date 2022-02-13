package ml.karmaconfigs.remote.messaging.util;

/*
 * GNU LESSER GENERAL PUBLIC LICENSE
 * Version 2.1, February 1999
 * <p>
 * Copyright (C) 1991, 1999 Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * Everyone is permitted to copy and distribute verbatim copies
 * of this license document, but changing it is not allowed.
 * <p>
 * [This is the first released version of the Lesser GPL.  It also counts
 * as the successor of the GNU Library Public License, version 2, hence
 * the version number 2.1.]
 */

import ml.karmaconfigs.api.common.utils.string.StringUtils;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Message data packer
 */
public final class MessagePacker implements Serializable {

    private final byte[] original;
    private final Map<String, byte[]> sub = new ConcurrentHashMap<>();

    /**
     * Initialize the message packer
     *
     * @param o the message
     */
    public MessagePacker(final byte[] o) {
        original = o;
    }

    /**
     * Pack the message
     *
     * @return the packed message
     */
    public String pack() {
        return StringUtils.serialize(this);
    }

    /**
     * Get the message data
     *
     * @return the message data
     */
    public byte[] getData() {
        return original;
    }

    /**
     * Unpack a packed message
     *
     * @param serialized the serialized packed message
     * @return the packed message
     */
    public static MessagePacker unpack(final String serialized) {
        try {
            Object loaded = StringUtils.load(serialized);
            if (loaded instanceof MessagePacker)
                return (MessagePacker) loaded;

            return null;
        } catch (Throwable ex) {
            return null;
        }
    }
}
