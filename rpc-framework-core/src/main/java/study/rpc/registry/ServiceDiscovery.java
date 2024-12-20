package study.rpc.registry;

import java.net.InetSocketAddress;
import study.rpc.remoting.dtObject.RpcRequest;
/**
 * 服务发现：根据服务名查询服务，用于客户端调用。
 */
public interface ServiceDiscovery {
    InetSocketAddress lookupService(RpcRequest rpcRequest);
}
