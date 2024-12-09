package study.rpc.serialize;


/**
 * 序列化接口，所有序列化类都要实现这个接口
 */


public interface Serializer {
    /**序列化方法(接口中的抽象方法)，将对象序列化为字节数组
     * @param obj 是要序列化的对象，可以接受任意类型的对象，所以不需要知道对象的具体类型
     */
    byte[] serialize(Object obj);

    /**反序列化方法(接口中的抽象方法)，将字节数组反序列化为指定类型的对象
     * @return 反序列化的对象，使用泛型<T>,类型由参数Class<T>决定,返回一个明确类型的对象
     */
    <T> T deserialize(byte[] bytes, Class<T> clazz);
}
