package ml.karmaconfigs.remote.messaging.util.message;

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
import ml.karmaconfigs.remote.messaging.util.message.type.DataType;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Message input interface
 */
public abstract class MessageInput {

    private final MessageOutput output;

    /**
     * Initialize the message input
     *
     * @param data the serialized base64 message output
     */
    public MessageInput(final byte[] data) {
        output = StringUtils.loadUnsafe(new String(data));
    }

    /**
     * Initialize the message output
     *
     * @param instance the message output
     */
    public MessageInput(final MessageOutput instance) {
        output = instance;
    }

    /**
     * Get the data object
     *
     * @param key the data key
     * @return the object
     */
    @Nullable
    public abstract Serializable getSerialized(final String key);

    /**
     * Get the data object
     *
     * @param key the data key
     * @return the object
     */
    @Nullable
    public abstract CharSequence getSequence(final String key);

    /**
     * Get the data object
     *
     * @param key the data key
     * @return the object
     */
    public @Nullable String getString(final String key) {
        CharSequence sequence = getSequence(key);
        if (sequence != null) {
            try {
                return sequence.toString();
            } catch (Throwable ignored) {}
        } else {
            Serializable serializable = getSerialized(key);
            if (serializable instanceof String) {
                return (String) serializable;
            }
        }

        return null;
    }

    /**
     * Get the data object
     *
     * @param key the data key
     * @return the object
     */
    public abstract boolean getBoolean(final String key);

    /**
     * Get the data object
     *
     * @param key the data key
     * @return the object
     */
    @Nullable
    public abstract Number getNumber(final String key);

    /**
     * Get the data object
     *
     * @param key the data key
     * @return the object
     */
    public abstract char[] getCharacters(final String key);

    /**
     * Get the data object
     *
     * @param key the data key
     * @return the object
     */
    public abstract byte[] getBytes(final String key);

    /**
     * Get the serialized message output
     *
     * @return the message output
     */
    public final MessageOutput getOutput() {
        return output;
    }

    /**
     * Clone the message input without the specified keys
     *
     * @param remove the keys to remove
     * @return the message input without the keys
     */
    public final MessageInput clone(final Map<DataType, Set<String>> remove) {
        MessageOutput out = new MessageDataOutput();
        for (DataType type : DataType.values()) {
            output.getKeys(type).forEach((key) -> {
                if (remove.containsKey(type)) {
                    Set<String> ignore = remove.getOrDefault(type, new HashSet<>());
                    if (!ignore.contains(key)) {
                        out.unsafeWrite(key, out.get(key, type), type);
                    }
                }
            });
        }

        return new MessageDataInput(out);
    }
}
