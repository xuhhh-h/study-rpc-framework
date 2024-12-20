package study.rpc.config;


import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString

public class RpcServiceConfig {

    private String version = "";
    private String group = "";

    private Object service;

    public String getRpcServiceName() {
        return this.getServiceName() + this.getGroup() + this.getVersion();
    }
    /**
     * 反射机制
     * getClass():返回service的运行时类(实现类)
     * getInterfaces()
     * 通过 getInterfaces() 获取该类实现的接口数组。
     * 通过数组访问第一个接口 interfaces[0]。
     * 使用 getCanonicalName() 获取该接口的全限定名。
     */
    public String getServiceName() {
        return this.service.getClass().getInterfaces()[0].getCanonicalName();
    }

}

