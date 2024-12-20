package study.rpc.remoting.dtObject;


import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString

public class RpcMessage {
    // 消息类型（请求、响应、心跳等）
    private byte messageType;
    //序列化类型
    private byte codec;
    // 请求的唯一标识，request和response一一对应
    // 异步通信时，客户端可能会发送多个请求并等待多个响应
    private long requestId;
    // 消息体（具体数据，如 RpcRequest 或 RpcResponse）
    private Object data;
    //消息体的压缩方式
    private byte compress;
}
