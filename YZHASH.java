
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *一致性hash功能实现
 */
public class YZHash {

    private static Logger logger = LoggerFactory.getLogger(YZHash.class);

    private static YZHash instance = new YZHash();

    private TreeMap<Long, String> nodes; // 虚拟节点

    private final int NODE_NUM = 100; // 每个机器节点关联的虚拟节点个数

    private YZHash(){}

    public static YZHash getInstance(){
        return instance;
    }

    public void init(List<String> ips) { // 初始化一致性hash环
        nodes = new TreeMap<Long, String>();
        for (int i = 0; i != ips.size(); ++i) { // 每个真实机器节点都需要关联虚拟节点
            final String shardInfo = ips.get(i);
            for (int n = 0; n < NODE_NUM; n++)
                // 一个真实机器节点关联NODE_NUM个虚拟节点
                nodes.put(hash("SHARD-" + i + "-NODE-" + n), shardInfo);
        }
    }

    public String getShardInfo(String key) {
        SortedMap<Long, String> tail = nodes.tailMap(hash(key)); // 沿环的顺时针找到一个虚拟节点
        if (tail.size() == 0) {
            return nodes.get(nodes.firstKey());
        }
        return tail.get(tail.firstKey()); // 返回该虚拟节点对应的真实机器节点的信息
    }

    /**
     *  MurMurHash算法，是非加密HASH算法，性能很高，
     *  比传统的CRC32,MD5，SHA-1（这两个算法都是加密HASH算法，复杂度本身就很高，带来的性能上的损害也不可避免）
     *  等HASH算法要快很多，而且据说这个算法的碰撞率很低.
     *  http://murmurhash.googlepages.com/
     */
    public Long hash(String key) {

        ByteBuffer buf = ByteBuffer.wrap(key.getBytes());
        int seed = 0x1234ABCD;

        ByteOrder byteOrder = buf.order();
        buf.order(ByteOrder.LITTLE_ENDIAN);

        long m = 0xc6a4a7935bd1e995L;
        int r = 47;

        long h = seed ^ (buf.remaining() * m);

        long k;
        while (buf.remaining() >= 8) {
            k = buf.getLong();

            k *= m;
            k ^= k >>> r;
            k *= m;

            h ^= k;
            h *= m;
        }

        if (buf.remaining() > 0) {
            ByteBuffer finish = ByteBuffer.allocate(8).order(
                    ByteOrder.LITTLE_ENDIAN);
            // for big-endian version, do this first:
            // finish.position(8-buf.remaining());
            finish.put(buf).rewind();
            h ^= finish.getLong();
            h *= m;
        }

        h ^= h >>> r;
        h *= m;
        h ^= h >>> r;

        buf.order(byteOrder);
        return h;
    }

}
