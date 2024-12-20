package study.rpc.extension;


import java.lang.annotation.*;

/**
 * 定义一个注解，标记一个接口或类作为“扩展点”，允许在运行时通过反射机制动态地加载和处理。
 * 被标记为扩展点，表示可以有多个实现
 * 在ExtensionLoader中，通过检查@SPI注解来验证扩展点是否合法。
 */
//标记该注解在生成文档（如 Javadoc）时会被包含在类或接口的注释中。
//当开发者查看被标记类或接口的文档时，可以看到该注解的信息。增强代码的可读性，便于使用者理解被标注的接口或类的用途。
@Documented
//指定注解的生命周期，也就是注解在程序运行时是否还存在。
//RetentionPolicy.RUNTIME 表示注解将在运行时保留，并且可以通过反射机制获取。
//支持在运行时通过反射机制读取注解。在 SPI 框架中，ExtensionLoader 使用反射来判断某个接口是否标记了 @SPI。
@Retention(RetentionPolicy.RUNTIME)
//指定注解可以应用的 Java 元素类型。
//ElementType.TYPE 表示该注解只能用于类、接口、枚举、注解等类型。
//限制 @SPI 注解只能标记在接口或类上，防止被误用在方法或字段上。
@Target(ElementType.TYPE)
public @interface SPI {
}
