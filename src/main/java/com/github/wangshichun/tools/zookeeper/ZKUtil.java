package com.github.wangshichun.tools.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by wangshichun on 2016/5/16.
 */
public class ZKUtil {
    private CuratorFramework client;
    private boolean alreadyInit = false;

    public void init(String hostAndPort, String chRoot) {
//        if (null == chRoot || chRoot.isEmpty())
//            if (hostAndPort.contains("/"))
//                chRoot = hostAndPort.substring(hostAndPort.indexOf("/") + 1);
        if (alreadyInit)
            return;

        shutdown();
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                .connectString(hostAndPort)
                .retryPolicy(new RetryNTimes(Integer.MAX_VALUE, 2))
                .connectionTimeoutMs(3000);
        if (chRoot != null && chRoot.length() > 0)
            builder.namespace(chRoot);
        client = builder.build();
        client.start();
        alreadyInit = true;
    }

    public void shutdown() {
        alreadyInit = false;

        if (client != null)
            client.close();
        client = null;
    }

    public boolean isAlreadyInit() {
        return alreadyInit;
    }

    public Stat exists(String path) {
        try {
            return client.checkExists().forPath(path);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getData(String path) {
        try {
            byte[] data = client.getData().forPath(path);
            if (null == data || data.length == 0)
                return "";
            return new String(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String create(String path, byte[] data) {
        try {
            if (null == data || data.length == 0)
                data = new byte[0];
            return client.create().forPath(path, data) + " 创建成功";
        } catch (KeeperException.NodeExistsException e) {
            return "节点已存在";
        } catch (Exception e) {
            e.printStackTrace();
            return "未知错误：" + e.getMessage();
        }
    }

    public String delete(String path) {
        try {
            client.delete().forPath(path);
            return "删除成功";
        } catch (KeeperException.NoNodeException e) {
            return "删除失败：节点不存在";
        } catch (Exception e) {
            e.printStackTrace();
            return "删除失败：" + e.getMessage();
        }
    }

    public String update(String path, byte[] data) {
        try {
            if (null == data || data.length == 0)
                data = new byte[0];
            Stat stat = client.setData().forPath(path, data);
            if (stat != null && stat.getDataLength() == data.length)
                return  "数据设置成功";
            else
                return  "数据设置失败：" + stat;
        } catch (KeeperException.NoNodeException e) {
            return "节点不存在";
        } catch (Exception e) {
            e.printStackTrace();
            return "未知错误：" + e.getMessage();
        }
    }

    public List<String> children(String path) {
        try {
            List<String> children = client.getChildren().forPath(path);
            Collections.sort(children);
            return children;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new LinkedList<>();
    }
}
