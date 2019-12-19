package wang.ismy.seeaw4.client;

import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import wang.ismy.seeaw4.client.message.chain.ClientCommandMessageChain;
import wang.ismy.seeaw4.client.netty.NettyClientConnection;
import wang.ismy.seeaw4.client.terminal.TerminalProxy;
import wang.ismy.seeaw4.common.client.Per;
import wang.ismy.seeaw4.common.command.CommandKey;
import wang.ismy.seeaw4.common.command.CommandType;
import wang.ismy.seeaw4.common.connection.Connection;
import wang.ismy.seeaw4.common.connection.ConnectionListener;
import wang.ismy.seeaw4.common.message.Message;
import wang.ismy.seeaw4.common.message.MessageService;
import wang.ismy.seeaw4.common.message.chain.impl.PrintMessageChain;
import wang.ismy.seeaw4.common.message.chain.impl.PromiseMessageChain;
import wang.ismy.seeaw4.common.message.impl.CommandMessage;
import wang.ismy.seeaw4.common.message.impl.ImgMessage;
import wang.ismy.seeaw4.common.message.impl.TextMessage;
import wang.ismy.seeaw4.common.promise.ConnectionPromise;
import wang.ismy.seeaw4.common.utils.JsonUtils;
import wang.ismy.seeaw4.terminal.Terminal;
import wang.ismy.seeaw4.terminal.enums.ShellType;
import wang.ismy.seeaw4.terminal.impl.CommonTerminal;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Member;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

@Slf4j
public class Client {

    private ClientService clientService;
    private Connection connection;
    private ConnectionListener connectionListener;
    private Terminal terminal;

    public Client() {
        try {
            terminal = new CommonTerminal();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setConnectionListener(ConnectionListener listener) {
        connectionListener = listener;
    }


    public void init() {
        // 注册消息处理链
        MessageService.getInstance().registerMessageChain(new PrintMessageChain()
                , PromiseMessageChain.getInstance(),new ClientCommandMessageChain(terminal));
        // 连接服务端
        NettyClientConnection connection = new NettyClientConnection("127.0.0.1", 1999);
        connection.bindConnectionListener(connectionListener);
        // 向连接设置客户自定义的连接监听器
        connection.connect();
        this.connection = connection;
        clientService = new ClientService(connection);
    }

    public static void main(String[] args) throws IOException {
        Client client = new Client();
        client.setConnectionListener(new ConnectionListener() {
            @Override
            public void establish(Connection connection) {
                // 连接成功后从服务器拉取在线客户
                try {
                    client.clientService.selectClientList((connection1, message) -> {
                        log.info("接收到查询在线列表回调");
                        if (message instanceof TextMessage) {
                            List<Per> list = JsonUtils.fromJson(((TextMessage) message).getText(), new TypeToken<List<Per>>() {
                            }.getType());
                            System.out.println("在线客户列表:" + list);
                            Per self = list.stream().filter(Per::isSelf).collect(Collectors.toList()).get(0);

                        }
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void close(Connection connection) {

            }
        });
        client.init();
        Scanner scanner = new Scanner(System.in);
        try {
            TerminalProxy terminalProxy = new TerminalProxy(client.connection,scanner.nextLine(),scanner.nextLine());
            Thread.sleep(2000);
            terminalProxy.input("uptime");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        System.in.read();

    }


    public Terminal getTerminal() {
        return terminal;
    }
}
