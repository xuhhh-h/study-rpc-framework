package study.rpc.remoting.dtObject;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@ToString
public class RpcRequest{

    private String requestId;
    //客户端要调用的服务接口名
    private String interfaceName;
    //要调用的具体方法
    private String methodName;
    //调用方法时传递的实际参数值
    private Object[] parameters;
    //调用方法参数的类型
    private Class<?>[] paramTypes;

    private String group;
    //多个服务可能具有相同的接口名和分组，但版本不同
    private String version;

    public String getRpcServiceName() {
        return this.getInterfaceName() + this.getGroup() + this.getVersion();
    }
}
