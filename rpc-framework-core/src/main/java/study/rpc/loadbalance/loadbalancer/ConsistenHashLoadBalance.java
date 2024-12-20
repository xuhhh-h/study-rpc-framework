package study.rpc.loadbalance.loadbalancer;

import study.rpc.loadbalance.AbstractLoadBalance;
import study.rpc.remoting.dtObject.RpcRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 一致性哈希负载均衡器类：保证相同请求特征会始终映射到同一个节点
 * 一致性哈希的逻辑是：根据请求特征（如 rpcRequest 的 serviceName 或 requestId）
 * 计算一个哈希值，通过一致性哈希算法选择固定的服务节点。
 */
public class ConsistenHashLoadBalance extends AbstractLoadBalance {
    private final ConcurrentHashMap<String, ConsistentHashSelector> selectors =new ConcurrentHashMap<>();


    @Override
    protected String doSelect(List<String> serviceAddresses, RpcRequest rpcRequest) {
        //是服务列表在JVM中的唯一标识符，用于检测列表是否发生变化
        int identityHashCode = System.identityHashCode(serviceAddresses);
        String rpcServiceName = rpcRequest.getRpcServiceName();

        //根据服务名提取对应的ConsistentHashSelector对象
        ConsistentHashSelector selector = selectors.get(rpcServiceName);
        // 如果 selector 不存在，或者服务地址列表发生了变化，重新创建 selector
        if(selector==null || selector.identityHashCode != identityHashCode){
            selectors.put(rpcServiceName, new ConsistentHashSelector(serviceAddresses,160,identityHashCode));
            selector = selectors.get(rpcServiceName);
        }

        //将rpc服务名和参数组合，通过selector 选择目标服务节点
        return selector.select(rpcServiceName + Arrays.stream(rpcRequest.getParameters()));
    }

    static class ConsistentHashSelector{
        //final:初始化后不能重新赋值，但是可以修改内容
        //TreeMap按照键排序，也就是按哈希值排序
        //存储虚拟节点的哈希环，键为哈希值，值为服务节点地址
        private final TreeMap<Long, String> virtualInvokers;
        //当前服务地址列表的唯一标识哈希码
        private final int identityHashCode;

        ConsistentHashSelector(List<String> invokers, int replicaNumber, int identityHashCode){
            this.virtualInvokers = new TreeMap<>();
            this.identityHashCode = identityHashCode;

            //构建一致性哈希环，讲服务节点映射到虚拟节点上
            //通过引入虚拟节点，将每个物理节点映射到哈希换上的多个位置，解决节点分布不均的问题
            //invokers是传入的服务节点列表，每个invoker表示一个真实的节点

            for(String invoker: invokers){
                //replicaNumber表示每个真实节点在哈希环上映射的虚拟节点的总数量
                //replicaNumber/4是因为 MD5哈希值生成的是一个128位的哈希值，存在长为16字节的字节数组中
                //每4字节(4字节是一个标准的整数长度)可以作为一个独立的哈希值表示一个虚拟节点
                //所以要通过replicaNumber/4次循环，生成replicaNumber个哈希值，映射到replicaNumber个虚拟节点上去
                for(int i=0; i<replicaNumber/4; i++){
                    byte[] digest = md5(invoker+i);
                    //将md5的结果分成四个哈希值
                    for(int h=0; h<4; h++){
                        long m = hash(digest, h);
                        //将虚拟节点放入哈希环中
                        //m是虚拟节点的哈希值，invoker是真实节点的地址，通过 TreeMap<Long, String> 将哈希值 m 和对应的真实节点进行映射，形成一致性哈希环
                        virtualInvokers.put(m,invoker);
                    }
                }
            }
        }

        //利用MessageDigest类获取Java内置的MD5哈希算法实例
        //MessageDigest还实现的哈希算法:MD5、SHA-1、SHA-256、SHA-512
        //为什么选择MD5: 1、RPC一致性哈希的主要目标是分布均匀性和快速计算，MD5计算快，且输出长度适中
        //2、Java 提供了内置的 MD5 支持（通过 MessageDigest），无需引入额外的依赖库，使用方便。
        static byte[] md5(String key){
            MessageDigest md;
            try{
                //获取MD5实例
                md = MessageDigest.getInstance("MD5");
                //讲字符串转化为字节数组
                byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
                //把数据块输入到MessageDigest中，准备计算哈希值
                md.update(bytes);
            }catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
            //执行哈希值计算，返回哈希结果
            return md.digest();
        }

        //将MD5转换为32位整数
        static long hash(byte[] digest, int index){
            return ((long) (digest[3 + index * 4] & 255) << 24 |
                    (long) (digest[2 + index * 4] & 255) << 16 |
                    (long) (digest[1 + index * 4] & 255) << 8 |
                    (long) (digest[index * 4] & 255)) & 4294967295L;
        }

        //根据rpc服务键选择对应的目标服务器
        //rpc服务键中有参数的变化保证是动态的服务键，其中包括唯一标识符requestId
        public String select(String rpcServiceKey){
            //rpc
            byte[] digest = md5(rpcServiceKey);
            return selectForKey(hash(digest,0));
        }

        //根据哈希值在哈希环上查找最近的服务节点
        //在哈希环中找到大于等于给定哈希值的第一个节点
        public String selectForKey(long hashCode){
            //tailMap:获取大于等于hashCode的键值对，true标识包含等于
            Map.Entry<Long,String> entry = virtualInvokers.tailMap(hashCode,true).firstEntry();

            //如果没有找到，返回第一个节点
            if(entry == null){
                entry = virtualInvokers.firstEntry();
            }

            return entry.getValue();
        }

    }
}
