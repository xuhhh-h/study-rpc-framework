package study.rpc.utils;

import java.util.Collection;

public class CollectionUtil {

    //Collection<?> collection表示任意类型的集合
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }
}
