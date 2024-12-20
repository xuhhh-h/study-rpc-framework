package study.rpc.serialize.kryo;


import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import study.rpc.serialize.Serializer;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Slf4j是Lombok提供的一种日志注解，可以自动生成一个基于 SLF4J 的 Logger 对象。日志默认输出到控制台
 */
@Slf4j

public class KryoSerializer implements Serializer {
    /**
     * 因为Kryo不是线程安全的，因此使用ThreadLocal存储
     * final保证ThreadLocal不会被修改
     * withInitial()方法用来指定ThreadLocal变量的初始值，这里通过 Lambda 表达式创建一个新的 Kryo 实例。
     */

    private final ThreadLocal<Kryo> kryoThreadLocal = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        /*循环引用时，不会无限序列化。每个对象都有唯一的ID，只会序列化一次*/
        kryo.setReferences(true); // 支持循环引用
        /* RPC框架中需要动态序列化任意对象，无法提前确定所有类，所以不能提前确定所有 */
        kryo.setRegistrationRequired(false); // 不需要预先注册类
        return kryo;
    });

    /**
     * try-with-resources：用于自动管理资源，确保在 try 块结束后自动关闭资源（如流对象）。
     * ByteArrayOutputStream：是一个基于内存的输出流，临时存储 Kryo 写入的序列化结果。用于将序列化后的数据写入到字节数组中。
     * */

    @Override
    public byte[] serialize(Object obj) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             Output output = new Output(byteArrayOutputStream)) {
            // 获取当前线程的 Kryo 实例
            Kryo kryo = kryoThreadLocal.get();
            // 序列化对象
            kryo.writeObject(output, obj);
            //刷新输出流，确保所有序列化的数据都写入到了byteArrayOutputStream里了
            output.flush();
            //返回流中当前数据的字节数组
            return byteArrayOutputStream.toByteArray(); // 返回字节数组
        } catch (Exception e) {
            log.error("Serialization failed", e);
            throw new RuntimeException("Serialization failed", e);
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        try(ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
            Input input = new Input(byteArrayInputStream)){

            Kryo kryo = kryoThreadLocal.get();
            return kryo.readObject(input,clazz);

        }catch (Exception e) {
            log.error("Deserialization failed", e);
            throw new RuntimeException("Deserialization failed", e);
        }
    }


}
