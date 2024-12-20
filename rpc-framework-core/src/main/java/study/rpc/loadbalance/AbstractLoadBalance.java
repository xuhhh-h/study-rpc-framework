package study.rpc.loadbalance;


import study.rpc.remoting.dtObject.RpcRequest;
import study.rpc.utils.CollectionUtil;

import java.util.List;

/**
 * 在负载均衡策略中，大部分逻辑是相同的，使用抽象类封装抽象逻辑
 */
public abstract class AbstractLoadBalance implements LoadBalance{
    @Override
    public String selectServiceAddress(List<String> serviceAddresses, RpcRequest rpcRequest){
        //如果服务地址列表为空，返回 null。
        //如果只有一个服务地址，直接返回该地址。
        if(CollectionUtil.isEmpty(serviceAddresses)){
            return null;
        }
        if(serviceAddresses.size() == 1){
            return serviceAddresses.get(0);
        }
        return doSelect(serviceAddresses,rpcRequest);
    }

    //只需要实现做选择的抽象方法
    protected abstract String doSelect(List<String> serviceAddresses, RpcRequest rpcRequest);
}
