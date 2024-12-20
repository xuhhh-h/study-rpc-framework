package study.rpc.utils;


import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Slf4j

public class PropertiesFileUtil {
    private PropertiesFileUtil(){
    }

    /**
     * 读取指定路径的配置文件，将配置文件的内容加载到 Properties 对象中。
     */
    public static Properties readPropertiesFile(String fileName){
        //Thread.currentThread()：获取当前正在运行的线程
        //getContextClassLoader()：获取当前线程的上下文类加载器
        //getResource()：根据提供的路径名称，返回资源的url
        //传入空字符串""表示从类加载器的根目录开始查找，返回从根目录开始的路径。
        URL url = Thread.currentThread().getContextClassLoader().getResource("");
        String rpcConfigPath ="";
        if(url != null){
            //如果获取到路径，将文件名拼接到根路径后
            rpcConfigPath = url.getPath()+fileName;
        }
        //在读取文本文件时，如果不显式指定字符编码，Java 会使用默认平台编码（例如在 Windows 上是 GBK，在 Linux 上是 UTF-8）
        //使用InputStreamReader字符流类，指定字符编码为UTF-8，确保读取文件时不会出现乱码
        //文件本身是字符流，通过InputStreamReader转为字符流，load载入要求字符流
        Properties properties = null;
        try(InputStreamReader inputStreamReader = new InputStreamReader(
                new FileInputStream(rpcConfigPath), StandardCharsets.UTF_8)){
            properties = new Properties();
            properties.load(inputStreamReader);
        }catch(IOException e){
            log.error("occur exception when read properties file [{}]", fileName);
        }
        return properties;
    }
}

