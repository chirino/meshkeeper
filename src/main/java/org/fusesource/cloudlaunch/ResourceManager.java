/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.cloudlaunch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.shared.http.AbstractHttpClientWagon;
import org.apache.maven.wagon.shared.http.HttpConfiguration;
import org.apache.maven.wagon.shared.http.HttpMethodConfiguration;
import org.fusesource.cloudlaunch.util.internal.FileUtils;

/**
 * RepositoryManager
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class ResourceManager {

    //Access to our local repo:
    private Wagon localWagon;

    private static final HashMap<String, Class<? extends Wagon>> wagonProviders = new HashMap<String, Class<? extends Wagon>>();

    private HashMap<String, Wagon> connectedRepos = new HashMap<String, Wagon>();

    static {
        registerWagonClass("file", "org.apache.maven.wagon.providers.file.FileWagon");
        registerWagonClass("ftp", "org.apache.maven.wagon.providers.ftp.FtpWagon");
        registerWagonClass("http", "org.apache.maven.wagon.providers.http.HttpWagon");
        registerWagonClass("dav", "org.apache.maven.wagon.providers.webdav.WebDavWagon");
    }

    @SuppressWarnings("unchecked")
    private static void registerWagonClass(String protocol, String classname) {
        try {
            Class<? extends Wagon> clazz = (Class<? extends Wagon>) Thread.currentThread().getContextClassLoader().loadClass(classname);
            wagonProviders.put(protocol, clazz);
        } catch (Exception e) {

        }

    }

    public void setLocalRepoDir(File localRepoDir) throws Exception {
        Repository localRepo = new Repository("local", localRepoDir.toURI().toString());
        if (!localRepoDir.exists()) {
            localRepoDir.mkdir();
        }
        localWagon = connectWagon(localRepo, null);
    }

    public void setCommonRepo(String url, AuthenticationInfo authInfo) throws Exception {
        Repository remoteRepo = new Repository("common", url);
        connectWagon(remoteRepo, authInfo);
    }

    public void locateResource(Resource resource) throws Exception {
        Wagon w = null;
        long timestamp = 0;
        if (localWagon.resourceExists(resource.getRepoPath())) {
            timestamp = new File(localWagon.getRepository().getBasedir() + File.separator + resource.getRepoPath()).lastModified();
        } else {
            synchronized (this) {
                w = connectedRepos.get(resource.getRepoName());
                if (w == null) {
                    Repository remote = new Repository(resource.getRepoName(), resource.getRepoUrl());
                    w = connectWagon(remote, null);
                }
            }

            if (w != null && w.resourceExists(resource.getRepoPath())) {
                try {
                    if (resource.getType() == Resource.DIRECTORY) {
                        String path = resource.getRepoPath();
                        if (!path.endsWith("/")) {
                            path = path + "/";
                        }
                        downloadDirectory(w, new File(localWagon.getRepository().getBasedir()), path);
                    } else {
                        w.getIfNewer(resource.getRepoPath(), new File(localWagon.getRepository().getBasedir(), resource.getRepoPath()), timestamp);

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (timestamp == 0) {
                throw new Exception("Resource not found: " + resource.getRepoPath());
            }
        }

        resource.setResolvedPath(localWagon.getRepository().getBasedir() + File.separator + resource.getRepoPath());
    }

    /**
     * @param resource
     * @param data
     * @throws IOException
     */
    public void deployFile(Resource resource, byte[] data) throws Exception {
        // TODO Auto-generated method stub
        File f = File.createTempFile("tmp", "dat");
        FileOutputStream fw = new FileOutputStream(f);
        fw.write(data);
        fw.flush();
        fw.close();
        try {
            deployResource(resource, f);
        } finally {
            f.delete();
        }
    }

    public void deployDirectory(Resource resource, File d) throws Exception {
        deployResource(resource, d);
    }

    private void deployResource(Resource resource, File f) throws Exception {
        Wagon w = null;
        synchronized (this) {
            w = connectedRepos.get(resource.getRepoName());
            if (w == null) {
                Repository remote = new Repository(resource.getRepoName(), resource.getRepoUrl());
                w = connectWagon(remote, null);
            }
        }
        w.put(f, resource.getRepoPath());
    }

    private Wagon connectWagon(Repository repo, AuthenticationInfo authInfo) throws Exception {
        Class<? extends Wagon> wagonClass = wagonProviders.get(repo.getProtocol());
        Wagon w = wagonClass.newInstance();
        String protocol = repo.getProtocol();
        if (w instanceof AbstractHttpClientWagon) {
            //Override the default http configuration since it erroneously sets 
            //Accept Encoding: gzip, then barfs when it doesn't check for it.
            HttpConfiguration hc = new HttpConfiguration();
            HttpMethodConfiguration hmc = new HttpMethodConfiguration();
            hmc.setUseDefaultHeaders(false);
            hmc.addHeader("Cache-control", "no-cache");
            hmc.addHeader("Cache-store", "no-store");
            hmc.addHeader("Pragma", "no-cache");
            hmc.addHeader("Expires", "0");
            hc.setAll(hmc);
            ((AbstractHttpClientWagon) w).setHttpConfiguration(hc);
        }

        w.connect(repo, authInfo);
        connectedRepos.put(repo.getName(), w);
        return w;
    }

    private static final void downloadDirectory(Wagon source, File targetDir, String path) throws Exception {
        Iterator i = source.getFileList(path).iterator();
        if (!i.hasNext()) {
            File target = new File(targetDir, path);
            target.mkdirs();
        } else {
            while (i.hasNext()) {
                String file = (String) i.next();
                if (file.endsWith("/")) {
                    downloadDirectory(source, targetDir, path + file);
                } else {
                    downloadFile(source, targetDir, path + file);
                }
            }
        }
    }

    private static final void downloadFile(Wagon source, File targetDir, String name) throws IOException, TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
        File target = new File(targetDir, name);
        source.get(name, new File(targetDir, name));
        //Empty files may not get created, so make sure that they are created here. 
        if (!target.exists()) {
            target.createNewFile();
        }
    }

    public void purgeLocalRepo() throws IOException {
        FileUtils.recursiveDelete(localWagon.getRepository().getBasedir());
    }

    /**
     * Closes all repository connections.
     * 
     * @throws Exception
     */
    public void close() {
        for (Wagon w : connectedRepos.values()) {
            try {
                w.disconnect();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        connectedRepos.clear();
    }

}
