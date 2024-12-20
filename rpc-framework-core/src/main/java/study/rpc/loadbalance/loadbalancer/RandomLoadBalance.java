package study.rpc.loadbalance.loadbalancer;

import study.rpc.loadbalance.AbstractLoadBalance;
import study.rpc.remoting.dtObject.RpcRequest;

import java.util.List;
import java.util.Random;

public class RandomLoadBalance extends AbstractLoadBalance {
    @Override
    protected String doSelect(List<String> serviceAddresses, RpcRequest rpcRequest) {
        Random random = new Random();
        return serviceAddresses.get(random.nextInt(serviceAddresses.size()));
    }
}
