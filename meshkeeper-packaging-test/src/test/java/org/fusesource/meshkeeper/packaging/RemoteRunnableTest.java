package org.fusesource.meshkeeper.packaging;

import junit.framework.TestCase;
import static org.fusesource.meshkeeper.Expression.file;

import org.fusesource.meshkeeper.Distributable;
import org.fusesource.meshkeeper.MeshKeeper;
import org.fusesource.meshkeeper.MeshKeeperFactory;
import org.fusesource.meshkeeper.distribution.PluginResolver;
import org.fusesource.meshkeeper.util.DefaultProcessListener;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;
import java.io.Serializable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author chirino
 */
public class RemoteRunnableTest extends TestCase {

    ClassPathXmlApplicationContext context;
    MeshKeeper meshKeeper;

    protected void setUp() throws Exception {
        final String SLASH = File.separator;
        String testDir = System.getProperty("basedir", ".")+ SLASH +"target"+ SLASH +"test-data"+SLASH+ "RemoteRunnableTest";
        String commonRepo = new File(testDir + SLASH + "common-repo").toURI().toString();
        System.setProperty("meshkeeper.base", testDir);
        System.setProperty("meshkeeper.repository.uri", commonRepo);
        System.setProperty("mop.base", testDir+SLASH+"mop");

        context = new ClassPathXmlApplicationContext("meshkeeper-all-spring.xml");
        meshKeeper = (MeshKeeper) context.getBean("meshkeeper");

    }

    public static void recursiveDelete(File f) {
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            for (int i = 0; i < files.length; i++) {
                recursiveDelete(files[i]);
            }
        }
        f.delete();
    }

    public static interface ICallBack extends Distributable {
        public void done();
    }
    
    public static class CallBack implements ICallBack {
        CountDownLatch latch = new CountDownLatch(1);
        public void done() {
            latch.countDown();
        }
    }

    static class RemoteTask implements Serializable, Runnable {
        private final ICallBack callback;

        public RemoteTask(ICallBack callback) {
            this.callback = callback;
        }

        public void run() {
            System.out.println("Doing callback...");
            callback.done();
        }
    }

    public void testRemoteRunnable() throws Exception {
        CallBack cb = new CallBack();
        ICallBack cbp = (ICallBack) meshKeeper.remoting().export(cb);

        meshKeeper.launcher().waitForAvailableAgents(5000);
        String agent = meshKeeper.launcher().getAvailableAgents()[0].getAgentId();

        // Note: the launched JVM will use the class path of this test case.
        meshKeeper.launcher().launch(agent, new RemoteTask(cbp), new DefaultProcessListener(meshKeeper));

        assertTrue(cb.latch.await(30, TimeUnit.SECONDS));
    }
}