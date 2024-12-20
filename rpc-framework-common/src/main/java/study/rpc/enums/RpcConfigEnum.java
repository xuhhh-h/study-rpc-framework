package study.rpc.enums;


import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum RpcConfigEnum {
    //配置文件路径
    RPC_CONFIG_PATH("rpc.properties"),
    //Zookeeper地址键值
    ZK_ADDRESS("rpc.zookeeper.address");

    private final String propertyValue;
}
