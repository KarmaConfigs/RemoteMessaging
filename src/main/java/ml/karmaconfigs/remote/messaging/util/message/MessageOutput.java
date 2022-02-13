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
import ml.karmaconfigs.remote.messaging.util.message.type.MergeType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Base64;
import java.util.Set;

/**
 * Message input interface
 */
public abstract class MessageOutput implements Serializable {

    private final MergeType type;
    private final MessageOutput origin;

    /**
     * Initialize the message input
     */
    public MessageOutput() {
        type = MergeType.NONE;
        origin = null;
    }

    /**
     * Initialize the message input
     *
     * @param merge the already existing message input
     * @param mt the merge type for the already existing message input
     */
    public MessageOutput(final @Nullable MessageOutput merge, final MergeType mt) {
        origin = merge;
        type = mt;
    }

    /**
     * Initialize the message input
     *
     * @param merge the already existing message input
     * @param mt the merge type for the already existing message input
     */
    public MessageOutput(final byte[] merge, final MergeType mt) {
        String serialized = new String(Base64.getDecoder().decode(merge));
        origin = StringUtils.loadUnsafe(serialized);
        type = mt;
    }

    /**
     * Write data to the message input
     *
     * @param key the data key
     * @param data the data to write
     */
    public abstract void writeSerializable(final String key, final Serializable data);

    /**
     * Write data to the message input
     *
     * @param key the data key
     * @param data the data ( character sequence ) to write
     */
    public abstract void write(final String key, final CharSequence data);

    /**
     * Write data to the message input
     *
     * @param key the data key
     * @param data the data ( boolean ) to write
     */
    public abstract void write(final String key, final boolean data);

    /**
     * Write data to the message input
     *
     * @param key the data key
     * @param number the data ( number ) to write
     */
    public abstract void write(final String key, final Number number);

    /**
     * Write data to the message input
     *
     * @param key the data key
     * @param chars the data ( characters ) to write
     */
    public abstract void write(final String key, final char... chars);

    /**
     * Write data to the message input
     *
     * @param key the data key
     * @param data the data ( bytes ) to write
     */
    public abstract void write(final String key, final byte[] data);

    /**
     * Write unsafely to the message input
     *
     * @param key the data key
     * @param data the data to write
     * @param type the data type
     * @param <T> the data type
     */
    public abstract <T> void unsafeWrite(final String key, final T data, final DataType type);

    /**
     * Remove data from the specified type
     *
     * @param key the data key
     * @param type the data type
     */
    public abstract void remove(final String key, final DataType type);

    /**
     * Get all the data keys for the specified data type
     *
     * @param type the data type
     * @return all the keys
     */
    public abstract Set<String> getKeys(final DataType type);

    /**
     * Get if the message input contains the specified key
     * in the specified data type
     *
     * @param key the data key
     * @param type the data type
     * @return if the message input contains that data
     */
    public abstract boolean contains(final String key, final DataType type);

    /**
     * Get the data
     *
     * @param key the data key
     * @param type the data key
     * @param <T> the data type
     * @return the data
     *
     * THIS SHOULD BE ALWAYS PACKAGE PRIVATE AS THE VALUES IN MESSAGE INPUT MAY CHANGE, IT'S UNSAFE
     * TO RETRIEVE THEM AS IF YOU WERE USING {@link MessageInput}, IN WERE THE DATA IS FIXED TO
     * KEY => VALUE ALWAYS
     */
    @Nullable
    abstract <T> T get(final String key, final DataType type);

    /**
     * Compile the message input
     *
     * @return the message input
     */
    public abstract byte[] compile();

    /**
     * Get the merge type
     *
     * @return the merge type
     */
    MergeType merge() {
        return type;
    }

    /**
     * Get the affiliated message input
     *
     * @return the affiliated message input
     */
    @Nullable
    MessageOutput affiliate() {
        return origin;
    }
}
