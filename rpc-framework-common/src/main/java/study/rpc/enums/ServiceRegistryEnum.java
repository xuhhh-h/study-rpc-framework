package study.rpc.enums;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ServiceRegistryEnum {
    ZK("zk");
    private final String name;
}
