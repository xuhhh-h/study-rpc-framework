package study.rpc.provider.impl;

import lombok.extern.slf4j.Slf4j;
import study.rpc.config.RpcServiceConfig;

import study.rpc.enums.ServiceRegistryEnum;
import study.rpc.extension.ExtensionLoader;
import study.rpc.provider.ServiceProvider;
import study.rpc.registry.ServiceRegistry;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ZkServiceProviderImpl implements ServiceProvider {

    //存储服务名称和实例的映射。管理服务实例，供本地调用和查找。
    private final Map<String, Object> serviceMap;
    // 已注册服务表，防止重复注册，所以使用Set
    private final Set<String> registeredService;
    private final ServiceRegistry serviceRegistry;

    public ZkServiceProviderImpl() {
        //ConcurrentHashMap是高并发线程安全的哈希表
        serviceMap = new ConcurrentHashMap<>();
        //当你只需要存储键并且要确保线程安全时，可以使用newKeySet()
        registeredService = ConcurrentHashMap.newKeySet();
        serviceRegistry = ExtensionLoader.getExtensionLoader(ServiceRegistry.class).getExtension(ServiceRegistryEnum.ZK.getName());

    }

    @Override
    public void addService(RpcServiceConfig rpcServiceConfig) {

    }

    @Override
    public Object getService(String rpcServiceName) {
        return null;
    }

    @Override
    public void publishService(RpcServiceConfig rpcServiceConfig) {

    }

}
