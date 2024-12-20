package study.rpc.registry.zk;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import study.rpc.registry.ServiceRegistry;
import study.rpc.registry.zk.util.CuratorUtils;

import java.net.InetSocketAddress;


@Slf4j
public class ZkServiceRegistryImpl implements ServiceRegistry {

    @Override
    public void registerService(String rpcServiceName, InetSocketAddress inetSocketAddress) {
        // 获取客户端
        CuratorFramework zkClient = CuratorUtils.getZkClient();
        // 构建服务节点路径
        String servicePath = CuratorUtils.ZK_REGISTER_ROOT_PATH + '/'+rpcServiceName + inetSocketAddress.toString();
        //创建节点
        CuratorUtils.createPersistentNode(zkClient,servicePath);
    }

}
