package com.arxyt.colonypathingedition.core.easycolony.manager;

import com.arxyt.colonypathingedition.ColonyPathingEdition;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class LinkageManager {

    public static boolean useJEAMatch = false;

    public static Method match;

    public static void setup() {
        useJEAMatch = true;
        try {
            Class<?> clazz = Class.forName("me.towdium.jecharacters.utils.Match");
            match = clazz.getMethod("contains", String.class, CharSequence.class);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            useJEAMatch = false;
        }
    }

    public static boolean invokeMatch(String s, CharSequence cs) {
        try {
            return (boolean) match.invoke(null, s, cs);
        } catch (IllegalAccessException | InvocationTargetException e) {
            ColonyPathingEdition.LOGGER.error("Cannot invoke Match.");
        }
        return false;
    }

    public static boolean match(String instance, CharSequence s) {
        if (LinkageManager.useJEAMatch) {
            return LinkageManager.invokeMatch(instance, s);
        }
        return instance.contains(s);
    }

}
