package ml.karmaconfigs.remote.messaging.listener;

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

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remote message event listener handler
 */
public final class RemoteListener {

    private final static Map<UUID, RemoteMessagingListener> listeners = new ConcurrentHashMap<>();

    /**
     * Register a new listener
     *
     * @param listener the listener
     * @return the listener ID
     */
    public static UUID register(final RemoteMessagingListener listener) {
        UUID random = UUID.randomUUID();

        listeners.put(random, listener);

        return random;
    }

    /**
     * Un register a listener
     *
     * @param listenerId the listener ID
     */
    public static void unRegister(final UUID listenerId) {
        listeners.remove(listenerId);
    }

    /**
     * Call an event on the server
     *
     * @param event the event to call
     */
    public static void callServerEvent(final ServerEvent event) {
        try {
            for (RemoteMessagingListener listener : listeners.values()) {
                Class<?> clazz = listener.getClass();

                Method[] methods = clazz.getDeclaredMethods();
                for (Method method : methods) {
                    if (method.getParameterTypes().length == 1) {
                        Class<?> type = method.getParameterTypes()[0];

                        if (event.getClass().isAssignableFrom(type)) {
                            method.invoke(listener, event);
                        }
                    }
                }
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Call an event on the client
     *
     * @param event the event to call
     */
    public static void callClientEvent(final ClientEvent event) {
        try {
            for (RemoteMessagingListener listener : listeners.values()) {
                Class<?> clazz = listener.getClass();

                Method[] methods = clazz.getDeclaredMethods();
                for (Method method : methods) {
                    if (method.getParameterTypes().length == 1) {
                        Class<?> type = method.getParameterTypes()[0];

                        if (event.getClass().isAssignableFrom(type)) {
                            method.invoke(listener, event);
                        }
                    }
                }
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }
}
