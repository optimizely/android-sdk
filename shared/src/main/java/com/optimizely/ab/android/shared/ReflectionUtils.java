package com.optimizely.ab.android.shared;

import android.content.Context;
import android.support.annotation.NonNull;

import com.optimizely.ab.bucketing.UserProfileService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by tzurkan on 6/15/17.
 */

public class ReflectionUtils {

    static public Object[] emptyArgs = {};
    static public Class[] emptyArgTypes = {};

    static private Logger log = LoggerFactory.getLogger(ReflectionUtils.class);

    static public Class getClass(@NonNull String className, @NonNull ClassLoader classLoader) {
        try {
            Class clazz = Class.forName(className, true, classLoader);
            return clazz;
        }
        catch (ClassNotFoundException e) {
            log.warn(className, e);
        }

        return null;
    }

    static public Object getObject(@NonNull String className, @NonNull ClassLoader classLoader,
                            String methodStr, Class[] classes, Object... arguments) {
        try {
            Class clazz = Class.forName(className, true, classLoader);
            if (clazz != null) {
                Method method;

                if (methodStr == null) {
                    return clazz.getDeclaredConstructor(classes).newInstance(arguments);
                }

                method = clazz.getMethod(methodStr, classes);
                Object o = null;

                if (method != null) {
                    o = method.invoke(null, arguments);
                }

                return o;
            }
        } catch (NoSuchMethodException e) {
            log.warn(className, e);
        } catch (IllegalAccessException e) {
            log.warn(className, e);
        } catch (InvocationTargetException e) {
            log.warn(className, e);
        }
        catch (ClassNotFoundException e) {
            log.warn(className, e);
        } catch (InstantiationException e) {
            log.warn(className, e);
        }

        return null;

    }

    static public Object callMethod(@NonNull Object object,
                            String methodStr, Class[] argTypes, Object... arguments) {

        if (object == null || methodStr == null) {
            return null;
        }

        try {
            Method method;
            method = object.getClass().getMethod(methodStr, argTypes);
            Object o = null;
            if (method != null) {
                o = method.invoke(object, arguments);
            }

            return o;
        } catch (NoSuchMethodException e1) {
            log.warn(methodStr, e1);
        } catch (IllegalAccessException e1) {
            log.warn(methodStr, e1);
        } catch (InvocationTargetException e1) {
            log.warn(methodStr, e1);
        }

        return null;

    }

    static public Object callStaticMethod(@NonNull Class clazz,
                                    String methodStr, Class[] argTypes, Object... arguments) {

        if (clazz == null || methodStr == null) {
            return null;
        }

        try {
            Method method;
            method = clazz.getMethod(methodStr, argTypes);
            Object o = null;

            if (method != null) {
               o = method.invoke(null, arguments);
            }

            return o;
        } catch (NoSuchMethodException e1) {
            log.warn(methodStr, e1);
        } catch (IllegalAccessException e1) {
            log.warn(methodStr, e1);
        } catch (InvocationTargetException e1) {
            log.warn(methodStr, e1);
        }

        return null;

    }
}
