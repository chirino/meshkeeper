package org.fusesource.cloudlaunch.zk;

import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.data.ACL;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.DisposableBean;
import org.fusesource.rmiviajms.JMSRemoteObject;

import javax.jms.Destination;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.Remote;
import java.util.ArrayList;
import java.io.*;

/**
 * @author chirino
 */
public class ZooKeeperExporter implements InitializingBean, DisposableBean {

    private Remote source;
    private String path;
    private ZooKeeper zooKeeper;
    private Destination destination;

    private Remote exportedStub;
    private String actualPath;

    public static <T extends Remote> byte[] marshall(T stub) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(baos);
        os.writeObject(stub);
        os.close();
        return baos.toByteArray();
    }

    public static <T extends Remote> T unmarshall(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(bais);
        return (T) is.readObject();
    }

    public void afterPropertiesSet() throws Exception {

        export();
    }

    public void export() throws Exception {
        if (exportedStub == null) {
            exportedStub = JMSRemoteObject.exportObject(source, destination);
            byte[] update = marshall(exportedStub);
            ArrayList<ACL> acl = ZooDefs.Ids.OPEN_ACL_UNSAFE;
            CreateMode createMode = CreateMode.EPHEMERAL_SEQUENTIAL;
            try
            {
                actualPath = zooKeeper.create(path, update, acl, createMode);
            }
            catch (NoNodeException nne)
            {
                zooKeeper.create(path.substring(0, path.lastIndexOf("/")), new byte [0], acl, CreateMode.PERSISTENT);
                actualPath = zooKeeper.create(path, update, acl, createMode);
            }
            System.out.println("Registered as: " + actualPath);
        }
    }

    public void destroy() throws Exception {
        if (exportedStub != null) {
            JMSRemoteObject.unexportObject(exportedStub, true);
            exportedStub = null;
        }
        if (actualPath != null) {
            zooKeeper.delete(actualPath, -1);
            actualPath = null;
        }
    }

    public Remote getSource() {
        return source;
    }

    public void setSource(Remote source) {
        this.source = source;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public ZooKeeper getZooKeeper() {
        return zooKeeper;
    }

    public void setZooKeeper(ZooKeeper zooKeeper) {
        this.zooKeeper = zooKeeper;
    }

    public Destination getDestination() {
        return destination;
    }

    public void setDestination(Destination destination) {
        this.destination = destination;
    }
}
