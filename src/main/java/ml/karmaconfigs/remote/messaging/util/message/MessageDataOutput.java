package ml.karmaconfigs.remote.messaging.util.message;

import ml.karmaconfigs.shaded.maputil.ConcurrentLinkedHashMap;
import ml.karmaconfigs.api.common.utils.string.StringUtils;
import ml.karmaconfigs.remote.messaging.util.message.type.DataType;
import ml.karmaconfigs.remote.messaging.util.message.type.MergeType;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

public class MessageDataOutput extends MessageOutput {

    private final ConcurrentMap<String, Serializable> serials = new ConcurrentLinkedHashMap.Builder<String, Serializable>().maximumWeightedCapacity(Long.MAX_VALUE).build();
    private final ConcurrentMap<String, CharSequence> sequences = new ConcurrentLinkedHashMap.Builder<String, CharSequence>().maximumWeightedCapacity(Long.MAX_VALUE).build();
    private final ConcurrentMap<String, Boolean> booleans = new ConcurrentLinkedHashMap.Builder<String, Boolean>().maximumWeightedCapacity(Long.MAX_VALUE).build();
    private final ConcurrentMap<String, Number> numbers = new ConcurrentLinkedHashMap.Builder<String, Number>().maximumWeightedCapacity(Long.MAX_VALUE).build();
    private final ConcurrentMap<String, char[]> characters = new ConcurrentLinkedHashMap.Builder<String, char[]>().maximumWeightedCapacity(Long.MAX_VALUE).build();
    private final ConcurrentMap<String, byte[]> bytes = new ConcurrentLinkedHashMap.Builder<String, byte[]>().maximumWeightedCapacity(Long.MAX_VALUE).build();

    /**
     * Create a new message data input
     */
    public MessageDataOutput() {
        super();
    }

    /**
     * Create a new message data input
     *
     * @param affiliate the owner data
     * @param type the merge type
     */
    public MessageDataOutput(final MessageOutput affiliate, final MergeType type) {
        super(affiliate, type);
    }

    /**
     * Create a new message data input
     *
     * @param compiled the owner data
     * @param type the merge type
     */
    public MessageDataOutput(final byte[] compiled, final MergeType type) {
        super(compiled, type);
    }

    /**
     * Write data to the message input
     *
     * @param key  the data key
     * @param data the data to write
     */
    @Override
    public void writeSerializable(final String key, final Serializable data) {
        serials.put(key, data);
    }

    /**
     * Write data to the message input
     *
     * @param key  the data key
     * @param data the data ( character sequence ) to write
     */
    @Override
    public void write(final String key, final CharSequence data) {
        sequences.put(key, data);
    }

    /**
     * Write data to the message input
     *
     * @param key  the data key
     * @param data the data ( boolean ) to write
     */
    @Override
    public void write(final String key, final boolean data) {
        booleans.put(key, data);
    }

    /**
     * Write data to the message input
     *
     * @param key    the data key
     * @param number the data ( number ) to write
     */
    @Override
    public void write(final String key, final Number number) {
        numbers.put(key, number);
    }

    /**
     * Write data to the message input
     *
     * @param key   the data key
     * @param chars the data ( characters ) to write
     */
    @Override
    public void write(final String key, final char... chars) {
        characters.put(key, chars);
    }

    /**
     * Write data to the message input
     *
     * @param key  the data key
     * @param data the data ( bytes ) to write
     */
    @Override
    public void write(final String key, final byte[] data) {
        bytes.put(key, data);
    }

    /**
     * Write unsafely to the message input
     *
     * @param key  the data key
     * @param data the data to write
     * @param type the data type
     */
    @Override
    public <T> void unsafeWrite(final String key, final T data, final DataType type) {
        try {
            switch (type) {
                case SERIALIZABLE:
                    serials.put(key, (Serializable) data);
                    break;
                case SEQUENCE:
                    sequences.put(key, (CharSequence) data);
                    break;
                case BOOLEAN:
                    booleans.put(key, (Boolean) data);
                    break;
                case NUMBER:
                    numbers.put(key, (Number) data);
                    break;
                case CHARACTER:
                    characters.put(key, (char[]) data);
                    break;
                case BYTE:
                    bytes.put(key, (byte[]) data);
                    break;
            }
        } catch (ClassCastException ignored) {}
    }

    /**
     * Remove data from the specified type
     *
     * @param key  the data key
     * @param type the data type
     */
    @Override
    public void remove(final String key, final DataType type) {
        switch (type) {
            case SERIALIZABLE:
                serials.remove(key);
                break;
            case SEQUENCE:
                sequences.remove(key);
                break;
            case BOOLEAN:
                booleans.remove(key);
            case NUMBER:
                numbers.remove(key);
                break;
            case CHARACTER:
                characters.remove(key);
                break;
            case BYTE:
                bytes.remove(key);
                break;
        }
    }

    /**
     * Get all the data keys for the specified data type
     *
     * @param type the data type
     * @return all the keys
     */
    @Override
    public Set<String> getKeys(final DataType type) {
        Set<String> keys = new LinkedHashSet<>();

        switch (type) {
            case SERIALIZABLE:
                return serials.keySet();
            case SEQUENCE:
                return sequences.keySet();
            case BOOLEAN:
                return booleans.keySet();
            case NUMBER:
                return numbers.keySet();
            case CHARACTER:
                return characters.keySet();
            case BYTE:
                return bytes.keySet();
        }

        return keys;
    }

    /**
     * Get if the message input contains the specified key
     * in the specified data type
     *
     * @param key  the data key
     * @param type the data type
     * @return if the message input contains that data
     */
    @Override
    public boolean contains(final String key, final DataType type) {
        Object obj = get(key, type);
        return obj != null;
    }

    /**
     * Get the data
     *
     * @param key  the data key
     * @param type the data key
     * @return the data
     * <p>
     * THIS SHOULD BE ALWAYS PACKAGE PRIVATE AS THE VALUES IN MESSAGE INPUT MAY CHANGE, IT'S UNSAFE
     * TO RETRIEVE THEM AS IF YOU WERE USING {@link MessageInput}, IN WERE THE DATA IS FIXED TO
     * KEY => VALUE ALWAYS
     */
    @Override
    @SuppressWarnings("unchecked")
    <T> T get(final String key, final DataType type) {
        switch (type) {
            case SERIALIZABLE:
                Serializable serializable = serials.getOrDefault(key, null);
                if (serializable != null) {
                    try {
                        return (T) serializable;
                    } catch (ClassCastException ignored) {}
                }

                break;
            case SEQUENCE:
                CharSequence sequence = sequences.getOrDefault(key, null);
                if (sequence != null) {
                    try {
                        return (T) sequence;
                    } catch (ClassCastException ignored) {}
                }

                break;
            case BOOLEAN:
                Boolean bool = booleans.getOrDefault(key, null);
                if (bool != null) {
                    try {
                        return (T) bool;
                    } catch (ClassCastException ignored) {}
                }

                break;
            case NUMBER:
                Number number = numbers.getOrDefault(key, null);
                if (number != null) {
                    try {
                        return (T) number;
                    } catch (ClassCastException ignored) {}
                }

                break;
            case CHARACTER:
                char[] chars = characters.getOrDefault(key, null);
                if (chars != null) {
                    try {
                        return (T) chars;
                    } catch (ClassCastException ignored) {}
                }

                break;
            case BYTE:
                byte[] data = bytes.getOrDefault(key, null);
                if (data != null) {
                    try {
                        return (T) data;
                    } catch (ClassCastException ignored) {}
                }

                break;
        }

        return null;
    }

    /**
     * Compile the message input
     *
     * @return the message input
     */
    @Override
    public byte[] compile() {
        MessageOutput affiliate = affiliate();
        if (affiliate != null) {
            MergeType merge = merge();
            DataType[] types = DataType.values();

            switch (merge) {
                case REPLACE:
                    for (DataType type : types) {
                        Set<String> keys = affiliate.getKeys(type);
                        keys.forEach((key) -> {
                            if (contains(key, type)) {
                                unsafeWrite(key, affiliate.get(key, type), type);
                            }
                        });
                    }

                    break;
                case DIFFERENCE:
                    for (DataType type : types) {
                        Set<String> keys = affiliate.getKeys(type);
                        keys.forEach((key) -> {
                            if (!contains(key, type)) {
                                unsafeWrite(key, affiliate.get(key, type), type);
                            }
                        });
                    }

                    break;
                case NONE:
                default:
                    break;
            }
        }

        return StringUtils.serialize(this).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Get the merge type
     *
     * @return the merge type
     */
    @Override
    public MergeType merge() {
        return super.merge();
    }

    /**
     * Get the affiliated message input
     *
     * @return the affiliated message input
     */
    @Nullable
    @Override
    public MessageOutput affiliate() {
        return super.affiliate();
    }
}
