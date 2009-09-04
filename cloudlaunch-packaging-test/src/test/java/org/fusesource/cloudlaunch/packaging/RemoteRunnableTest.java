package org.fusesource.cloudlaunch.packaging;

import junit.framework.TestCase;
import static org.fusesource.cloudlaunch.Expression.file;

import org.fusesource.cloudlaunch.Distributable;
import org.fusesource.cloudlaunch.LaunchClient;
import org.fusesource.cloudlaunch.util.DefaultProcessListener;
import org.fusesource.cloudlaunch.distribution.PluginResolver;
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
    LaunchClient client;

    protected void setUp() throws Exception {
        if( System.getProperty(PluginResolver.KEY_DEFAULT_PLUGINS_VERSION) == null ) {
            System.setProperty(PluginResolver.KEY_DEFAULT_PLUGINS_VERSION, "LATEST");
        }

        File basedir = new File(file("target/test-data").evaluate());
        // recursiveDelete(basedir);


        // This should be getting set by the junit test runner to actuall plugins being tested.
        if( System.getProperty(PluginResolver.KEY_DEFAULT_PLUGINS_VERSION) == null ) {
            System.setProperty(PluginResolver.KEY_DEFAULT_PLUGINS_VERSION, "LATEST");
        }

        System.setProperty(PluginResolver.BASE_DIR, new File(basedir, "mop").getCanonicalPath());
        System.setProperty("basedir", basedir.getCanonicalPath());
        String commonRepo = new File(basedir, "common-repo").toURI().toString();
        System.setProperty("common.repo.url", commonRepo);
        System.setProperty("local.repo.url", new File(basedir, "local-repo").getCanonicalPath() );

        context = new ClassPathXmlApplicationContext("cloudlaunch-all-spring.xml");
        client = (LaunchClient) context.getBean("launch-client");

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
        ICallBack cbp = (ICallBack) client.getDistributor().export(cb);

        client.waitForAvailableAgents(5000);
        String agent = client.getAvailableAgents()[0].getAgentId();

        // Note: the launched JVM will use the class path of this test case.
        client.launch(agent, new RemoteTask(cbp), new DefaultProcessListener(client.getDistributor()));

        assertTrue(cb.latch.await(30, TimeUnit.SECONDS));
    }
}