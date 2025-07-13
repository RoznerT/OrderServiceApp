package com.example.common.utils;

import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Objects;

/**
 * מחלקה עזר לביצוע בדיקות תקינות שונות
 * מספקת מתודות סטטיות לבדיקת null, ריקות וערכים
 */
@NoArgsConstructor
public final class ValidationUtils {

    /**
     * בודק אם האוביקט אינו null
     *
     * @param obj האוביקט לבדיקה
     * @return true אם האוביקט אינו null
     */
    public static boolean isNotNull(Object obj) {
        return Objects.nonNull(obj);
    }

    /**
     * בודק אם האוביקט הוא null
     *
     * @param obj האוביקט לבדיקה
     * @return true אם האוביקט הוא null
     */
    public static boolean isNull(Object obj) {
        return Objects.isNull(obj);
    }

    /**
     * בודק אם המחרוזת לא ריקה ומכילה תוכן
     *
     * @param str המחרוזת לבדיקה
     * @return true אם המחרוזת אינה ריקה ומכילה תוכן
     */
    public static boolean hasText(String str) {
        return StringUtils.isNotBlank(str);
    }

    /**
     * בודק אם המחרוזת ריקה או null
     *
     * @param str המחרוזת לבדיקה
     * @return true אם המחרוזת ריקה או null
     */
    public static boolean isEmpty(String str) {
        return StringUtils.isBlank(str);
    }

    /**
     * בודק אם האוסף לא ריק
     *
     * @param collection האוסף לבדיקה
     * @return true אם האוסף אינו ריק
     */
    public static boolean isNotEmpty(Collection<?> collection) {
        return !CollectionUtils.isEmpty(collection);
    }

    /**
     * בודק אם האוסף ריק או null
     *
     * @param collection האוסף לבדיקה
     * @return true אם האוסף ריק או null
     */
    public static boolean isEmpty(Collection<?> collection) {
        return CollectionUtils.isEmpty(collection);
    }

    /**
     * בודק אם הערך חיובי
     *
     * @param number המספר לבדיקה
     * @return true אם המספר חיובי
     */
    public static boolean isPositive(Integer number) {
        return isNotNull(number) && number > 0;
    }
} 