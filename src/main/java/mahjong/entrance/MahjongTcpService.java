package mahjong.entrance;

import mahjong.redis.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 斗地主
 * Created by pengyi on 2016/3/9.
 */
public class MahjongTcpService implements Runnable {

    public final static Map<Integer, MessageReceive> userClients = new HashMap<>();
    private ServerSocket serverSocket;
    private boolean started = false;
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
    private RedisService redisService;

    public MahjongTcpService(RedisService redisService) {
        this.redisService = redisService;
    }

    @Override
    public void run() {

        int port = 10401;
        try {
            serverSocket = new ServerSocket(port);
            started = true;
            logger.info("麻将tcp开启成功，端口[" + port + "]");
        } catch (IOException e) {
            logger.error("socket.open.fail.message");
        }

        try {
            while (started) {
                Socket s = serverSocket.accept();
                cachedThreadPool.execute(new MessageReceive(s, redisService));
            }
        } catch (IOException e) {
            logger.error("socket.server.dirty.shutdown.message");
        } finally {
            try {
                serverSocket.close();
            } catch (IOException e) {
                logger.error(e.toString(), e);
            }
        }
    }
}
