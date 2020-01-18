package com.qgailab.raftkv;

import com.qgailab.raftkv.common.NodeInfo;
import com.qgailab.raftkv.impl.DefaultNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * @author linxu
 */
@Component
@Slf4j
@PropertySource("classpath:server-config.properties")
@Data
public class RaftNodeBootStrap {
    static Node backUpNode;
    //@Value("${qurom}")
    private String peers;

    //@Value("${node.port}")
    private int port;

    private List<String> addressList = new LinkedList<>();

    @PostConstruct
    public void main0() throws Throwable {
        peers = "localhost:11111,localhost:11112,localhost:11113";
        port = 11111;
        String[] peerAddr = peers.split(",");
        //提供一个全局列表维护
        addressList.addAll(Arrays.asList(peerAddr));
        NodeInfo config = new NodeInfo();

        // 自身节点
        config.setSelfPort(port);

        // 其他节点地址
        config.setPeerAddress(Arrays.asList(peerAddr));
        Node node = DefaultNode.getInstance();
        ((DefaultNode) node).setConfig(config);
        node.loadNodeInfo(config);
        node.init();
        //获取回溯节点
        backUpNode = node;
    }

    @PreDestroy
    public void bye() {
        if (backUpNode != null) {
            try {
                backUpNode.destroy();
            } catch (Throwable throwable) {
                log.error("DESTROY BACKUP NODE FAIL.");
            }
        }
    }

    public static void main(String[] args) {
        try {
            new RaftNodeBootStrap().main0();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

}
