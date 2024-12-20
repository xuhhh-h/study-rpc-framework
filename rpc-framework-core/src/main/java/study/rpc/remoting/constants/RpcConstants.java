package study.rpc.remoting.constants;


import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class RpcConstants {

    //魔数用于验证数据流是否符合 RPC 协议
    public static final byte[] MAGIC_NUMBER = {(byte) 'g',(byte) 'r', (byte) 'p', (byte) 'c'};
    //指定rpc框架中使用的默认字符编码，确保客户端和服务端在处理字符串时使用相同的编码
    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    //当前rpc协议的版本号
    public static final byte VERSION = 1;
    //rpc消息的最小长度，需要达到这个长度才可以完成消息头的解析
    public static final byte TOTAL_LENGTH = 16;
    //定义消息的类型，便于客户端和服务端解析和处理不同类型的消息
    public static final byte REQUEST_TYPE = 1;
    public static final byte RESPONSE_TYPE = 2;
    //ping消息
    public static final byte HEARTBEAT_REQUEST_TYPE = 3;
    //pong消息
    public static final byte HEARTBEAT_RESPONSE_TYPE = 4;
    //rpc消息头部的固定长度
    public static final int HEAD_LENGTH = 16;
    //定义了心跳检测的具体内容。
    //客户端发送 "ping" 消息，服务端返回 "pong" 消息，用于保持连接的活跃状态。
    public static final String PING = "ping";
    public static final String PONG = "pong";

    //定义单条消息的最大长度:8MB
    public static final int MAX_FRAME_LENGTH = 8 * 1024 * 1024;
}
