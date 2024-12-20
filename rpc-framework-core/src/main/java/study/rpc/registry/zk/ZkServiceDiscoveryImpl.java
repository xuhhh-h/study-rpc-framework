package study.rpc.registry.zk;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import study.rpc.enums.LoadBalanceEnum;
import study.rpc.extension.ExtensionLoader;
import study.rpc.loadbalance.LoadBalance;
import study.rpc.registry.ServiceDiscovery;
import study.rpc.registry.zk.util.CuratorUtils;
import study.rpc.remoting.dtObject.RpcRequest;
import study.rpc.utils.CollectionUtil;
import java.net.InetSocketAddress;
import java.util.List;
import study.rpc.exception.RpcException;
import study.rpc.enums.RpcErrorMessageEnum;


@Slf4j
public class ZkServiceDiscoveryImpl implements ServiceDiscovery {
    private final LoadBalance loadBalance;

    public ZkServiceDiscoveryImpl(){
        //通过ExtensionLoader获取负载均衡实例
        //getExtensionLoader：根据扩展(接口)获取加载器实例
        //getExtension：根据指定扩展名称获取扩展点实例（实现类实例）
        this.loadBalance = ExtensionLoader.getExtensionLoader(LoadBalance.class)
                .getExtension(LoadBalanceEnum.LOADBALANCE.getName());
        //getExtension(loadBalance) -> createExtension(loadBalance) -> getExtensionClasses()
        //getExtensionClasses()就会从配置文件META/extensions中读出loadBalance由哪个实现类来实现
    }

    /**
     * 查找并返回服务地址
     * @param rpcRequest RPC 请求，包含服务名称等信息
     * @return 服务地址的 InetSocketAddress
     */
    @Override
    public InetSocketAddress lookupService(RpcRequest rpcRequest) {
        //获取RPC请求中的服务名称
        String rpcServiceName = rpcRequest.getRpcServiceName();
        //获取Curator客户端，连接Zookeeper
        CuratorFramework zkClient = CuratorUtils.getZkClient();
        //从Zookeeper获取指定服务的所有服务实例地址
        List<String> serviceUrlList = CuratorUtils.getChildrenNodes(zkClient,rpcServiceName);

        if (CollectionUtil.isEmpty(serviceUrlList)) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_CAN_NOT_BE_FOUND, rpcServiceName);
        }

        //使用负载均衡策略从服务实例列表中选择一个服务地址
        String targetServiceUrl = loadBalance.selectServiceAddress(serviceUrlList, rpcRequest);
        log.info("Successfully found the service address:[{}]", targetServiceUrl);

        //解析目标服务地址字符串(host:port)，组成InetSocketAddress
        //通过:将 targetServiceUrl拆分成两个部分：host 和 port。
        String[] socketAddressArray = targetServiceUrl.split(":");
        String host = socketAddressArray[0];
        int port = Integer.parseInt(socketAddressArray[1]);

        return new InetSocketAddress(host,port);
    }
}
