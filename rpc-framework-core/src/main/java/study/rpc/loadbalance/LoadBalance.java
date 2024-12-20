package study.rpc.loadbalance;

import study.rpc.extension.SPI;
import study.rpc.remoting.dtObject.RpcRequest;

import java.util.List;

@SPI
public interface LoadBalance {
    /**
     * 从服务地址列表选一个一个服务地址
     */
    String selectServiceAddress(List<String> serviceUrList, RpcRequest rpcRequest);
}
