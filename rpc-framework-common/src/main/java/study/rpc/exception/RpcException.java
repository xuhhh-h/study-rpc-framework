package study.rpc.exception;

import study.rpc.enums.RpcErrorMessageEnum;

//继承RuntimeException：运行时异常 = 表示程序运行时可能发生的错误，通常是由逻辑问题或程序缺陷引起的
// 是一个未检查异常类 = 不需要显式地捕获或声明抛出这种异常
//强制捕获：显式地在方法签名中声明 throws，或者在方法内使用 try-catch
public class RpcException extends RuntimeException{
    //通过传入 RpcErrorMessageEnum 和详细信息，将错误消息拼接成完整的异常信息。这适用于你需要提供更详细的错误描述的场景。
    public RpcException(RpcErrorMessageEnum rpcErrorMessageEnum, String detail) {
        //super用于调用父类的构造函数public RuntimeException(String message)
        super(rpcErrorMessageEnum.getMessage() + ":" + detail);
    }
    //这种构造函数可以用于异常链的传递，即当你需要包装其他异常时，可以将原始异常传递给 RpcException
    //public RuntimeException(String message, Throwable cause)
    public RpcException(String message, Throwable cause) {
        super(message, cause);
    }
    //直接使用 RpcErrorMessageEnum 枚举类中的错误信息来创建异常。
    public RpcException(RpcErrorMessageEnum rpcErrorMessageEnum) {
        super(rpcErrorMessageEnum.getMessage());
    }
}
