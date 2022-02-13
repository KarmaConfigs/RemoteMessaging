package ml.karmaconfigs.remote.messaging.util.message;

import ml.karmaconfigs.api.common.utils.string.StringUtils;
import ml.karmaconfigs.remote.messaging.util.message.type.DataType;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Base64;

public class MessageDataInput extends MessageInput {

    private final MessageOutput input;

    /**
     * Initialize the message input
     *
     * @param data the serialized base64 message output
     */
    public MessageDataInput(final byte[] data) {
        super(data);
        String serialized = new String(data);
        input = StringUtils.loadUnsafe(serialized);
    }

    /**
     * Initialize the message input
     *
     * @param out the message output
     */
    public MessageDataInput(final MessageOutput out) {
        super(out);
        input = out;
    }

    /**
     * Get the data object
     *
     * @param key the data key
     * @return the object
     */
    @Override
    public @Nullable Serializable getSerialized(final String key) {
        return input.get(key, DataType.SERIALIZABLE);
    }

    /**
     * Get the data object
     *
     * @param key the data key
     * @return the object
     */
    @Override
    public @Nullable CharSequence getSequence(final String key) {
        return input.get(key, DataType.SEQUENCE);
    }

    /**
     * Get the data object
     *
     * @param key the data key
     * @return the object
     */
    @Override
    public boolean getBoolean(final String key) {
        return Boolean.TRUE.equals(input.get(key, DataType.BOOLEAN));
    }

    /**
     * Get the data object
     *
     * @param key the data key
     * @return the object
     */
    @Override
    public @Nullable Number getNumber(final String key) {
        return input.get(key, DataType.NUMBER);
    }

    /**
     * Get the data object
     *
     * @param key the data key
     * @return the object
     */
    @Override
    public char[] getCharacters(final String key) {
        return input.get(key, DataType.CHARACTER);
    }

    /**
     * Get the data object
     *
     * @param key the data key
     * @return the object
     */
    @Override
    public byte[] getBytes(final String key) {
        return input.get(key, DataType.BYTE);
    }

    /**
     * Returns a string representation of the object. In general, the
     * {@code toString} method returns a string that
     * "textually represents" this object. The result should
     * be a concise but informative representation that is easy for a
     * person to read.
     * It is recommended that all subclasses override this method.
     * <p>
     * The {@code toString} method for class {@code Object}
     * returns a string consisting of the name of the class of which the
     * object is an instance, the at-sign character `{@code @}', and
     * the unsigned hexadecimal representation of the hash code of the
     * object. In other words, this method returns a string equal to the
     * value of:
     * <blockquote>
     * <pre>
     * getClass().getName() + '@' + Integer.toHexString(hashCode())
     * </pre></blockquote>
     *
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (DataType tp : DataType.values()) {
            builder.append(tp.name()).append("=");
            input.getKeys(tp).forEach((value) -> builder.append(value).append(";"));
        }

        return getClass().getName() + "@" + Integer.toHexString(hashCode()) + "[" + builder + "]";
    }
}
