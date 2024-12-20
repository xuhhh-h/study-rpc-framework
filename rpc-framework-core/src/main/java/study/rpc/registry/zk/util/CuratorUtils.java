package study.rpc.registry.zk.util;



import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import study.rpc.enums.RpcConfigEnum;
import study.rpc.utils.PropertiesFileUtil;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

//Curator工具类，封装常用方法,避免代码复用严重
@Slf4j
public class CuratorUtils {
    // 默认 ZooKeeper 地址，127.0.0.1 是本地 IP，2181 是默认端口。
    private static final String DEFAULT_ZOOKEEPER_ADDRESS = "127.0.0.1:2181";
    //连接 ZooKeeper 时的基础重试等待时间(ms)
    private static final int BASE_SLEEP_TIME_MS = 1000;
    //最大重试次数。
    private static final int MAX_RETRIES = 3;

    // 创建的是一个ZNode节点
    private static CuratorFramework zkClient;

    public static final String ZK_REGISTER_ROOT_PATH = "/my-rpc";
    //存储服务名对应的服务实例地址列表
    private static final Map<String, List<String>> SERVICE_ADDRESS_MAP = new ConcurrentHashMap<>();
    //存储已注册的服务路径
    private static final Set<String> REGISTERED_PATH_SET = ConcurrentHashMap.newKeySet();

    private CuratorUtils() {

    }

    /**
     * 创建持久节点，存储注册信息
     */
    public static void createPersistentNode(CuratorFramework zkClient, String path){
        try{
            //如果节点不存在，创建新节点
            if(REGISTERED_PATH_SET.contains(path) || zkClient.checkExists().forPath(path)!=null){
                // {}是占位符，参数插入位置
                log.info("The node already exists. The node is:[{}]", path);
            }else{
                //creatingParentsIfNeeded() 确保父节点路径会被自动递归创建。
                //withMode(CreateMode.PERSISTENT)指定节点为持久节点。
                zkClient.create().creatingParentContainersIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path);
                log.info("The node was created successfully. The node is:[{}]",path);
            }
            REGISTERED_PATH_SET.add(path);
        }catch(Exception e){
            log.error("create persistent node for path [{}] fail", path);
        }
    }

    /**
     * 获取指定路径的所有子节点
     * 用于服务发现，获取某个服务的所有实例地址
     */
    public static List<String> getChildrenNodes(CuratorFramework zkClient, String rpcServiceName){
        if(SERVICE_ADDRESS_MAP.containsKey(rpcServiceName)){
            return SERVICE_ADDRESS_MAP.get(rpcServiceName);
        }
        List<String> result = null;
        String servicePath = ZK_REGISTER_ROOT_PATH+ "/" + rpcServiceName;
        try{
            result = zkClient.getChildren().forPath(servicePath);
            SERVICE_ADDRESS_MAP.put(rpcServiceName,result);
            //注册一个监听器，有新节点的变化
            registerWatcher(rpcServiceName, zkClient);
        }catch(Exception e){
            log.error("get children nodes for path [{}] fail", servicePath);
        }
        return result;
    }

    /**
     * 在服务端关闭时，清理与当前服务器相关的注册信息
     */
    public static void clearRegistry(CuratorFramework zkClient, InetSocketAddress inetSocketAddress) {
        REGISTERED_PATH_SET.stream().parallel().forEach(service -> {
            try {
                if (service.endsWith(inetSocketAddress.toString())) {
                    zkClient.delete().forPath(service);
                }
            }catch (Exception e) {
                log.error("clear registry for path [{}] fail", service);
            }
        });
        log.info("All registered services on the server are cleared:[{}]", REGISTERED_PATH_SET.toString());
    }

    /**
     * 获取CuratorFramework客户端
     */

    public static CuratorFramework getZkClient() {
        //返回已启动的客户端
        if(zkClient != null && zkClient.getState() == CuratorFrameworkState.STARTED){
            return zkClient;
        }
        //检查zkClient是否已经初始化
        //Properties是一个专门用于读取配置文件的类
        //通过 Properties 类，可以从外部配置文件（.properties 文件）中加载键值对数据
        //从RpcConfigEnum枚举中获取配置文件路径
        Properties properties = PropertiesFileUtil.readPropertiesFile(RpcConfigEnum.RPC_CONFIG_PATH.getPropertyValue());
        //如果成功读取到properties对象，且成功从配置文件中获取zookeeper.address，则使用该值，否则使用默认值DEFAULT_ZOOKEEPER_ADDRESS（127.0.0.1:2181）
        String zookeeperAddress = properties != null && properties.getProperty(RpcConfigEnum.ZK_ADDRESS.getPropertyValue()) != null ?
                properties.getProperty(RpcConfigEnum.ZK_ADDRESS.getPropertyValue())
                : DEFAULT_ZOOKEEPER_ADDRESS;

        RetryPolicy retryPolicy = new ExponentialBackoffRetry(BASE_SLEEP_TIME_MS,MAX_RETRIES);
        zkClient = CuratorFrameworkFactory.builder()
                .connectString(zookeeperAddress)
                .retryPolicy(retryPolicy)
                .build();
        zkClient.start();
        //检测连接超时，确保ZooKeeper客户端成功连接到ZooKeeper服务器。
        try{
            //blockUntilConnected 是 CuratorFramework 提供的一个方法
            // 阻塞当前线程，直到客户端与 ZooKeeper 服务器成功建立连接，或达到指定的超时时间。
            if(!zkClient.blockUntilConnected(30, TimeUnit.SECONDS)){
                throw new RuntimeException("Time out waiting to connect to ZK!");
            }
        }catch(InterruptedException e){
            e.printStackTrace();
        }

        return zkClient;
    }

    /**
     *  注册 Watcher 监听某服务节点，并更新本地缓存
     *  为指定服务节点注册一个 Watcher，当节点数据或子节点发生变更时，自动更新本地缓存 SERVICE_ADDRESS_MAP。
     */

    private static void registerWatcher(String rpcServiceName, CuratorFramework zkClient) throws Exception{
        String servicePath = ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName;

        //PathChildrenCache 是 Curator 提供的一个监听器，专门用于监听指定路径下的子节点变化
        //true:要缓存子节点的数据，事件回调时，可以通过 ChildData 对象直接获取子节点的名称和数据
        PathChildrenCache pathChildrenCache = new PathChildrenCache(zkClient,servicePath,true);

        //PathChildrenCacheListener 是一个监听器接口，用于定义事件回调函数，处理 PathChildrenCache 触发的子节点变化事件
        //PathChildrenCacheListener 接口只有一个回调方法:
        //void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception;
        //在这方法里定义子节点发生变化时需要执行的操作

        PathChildrenCacheListener pathChildrenCacheListener = (curatorFramework, pathChildrenCacheEvent) -> {
            List<String> serviceAddresses = curatorFramework.getChildren().forPath(servicePath);
            SERVICE_ADDRESS_MAP.put(rpcServiceName, serviceAddresses);
        };
        //注册监听器
        pathChildrenCache.getListenable().addListener(pathChildrenCacheListener);
        //启动监听
        pathChildrenCache.start();
    }

}
