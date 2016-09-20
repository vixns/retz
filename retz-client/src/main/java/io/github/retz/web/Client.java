/**
 * Retz
 * Copyright (C) 2016 Nautilus Technologies, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.retz.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import io.github.retz.auth.Authenticator;
import io.github.retz.cli.TimestampHelper;
import io.github.retz.protocol.*;
import io.github.retz.protocol.data.Application;
import io.github.retz.protocol.data.Job;
import io.github.retz.protocol.data.MesosContainer;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.eclipse.jetty.util.security.Credential.MD5.digest;


// WebSocket client to Retz service
public class Client implements AutoCloseable {
    static final Logger LOG = LoggerFactory.getLogger(Client.class);
    private final ObjectMapper mapper;
    private final String scheme;
    private final String host;
    private final int port;
    private WebSocketClient wsclient = null;
    private MySocket socket = null;
    private final Optional<io.github.retz.auth.Authenticator> authenticator;

    protected Client(URI uri, Optional<Authenticator> authenticator) {
        this.scheme = uri.getScheme();
        this.host = uri.getHost();
        this.port = uri.getPort();
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new Jdk8Module());
        this.authenticator = authenticator;
    }

    protected Client(URI uri) {
        this(uri, Optional.empty());
    }

    protected Client(URI uri, Optional<Authenticator> authenticator, boolean checkCert) {
        this(uri, authenticator);
        if (uri.getScheme().equals("https") && !checkCert) {
            try {
                WrongTrustManager.disableTLS();
            } catch (NoSuchAlgorithmException e) {
                throw new AssertionError(e.toString());
            } catch (KeyManagementException e) {
                throw new AssertionError(e.toString());
            }
        }
    }

    private boolean connect() throws IOException, ExecutionException, URISyntaxException, Exception {
        // TODO: this does not work for SSL
        URI uri = new URI(String.format("ws://%s:%d/cui", host, port));
        this.socket = new MySocket();
        this.wsclient = new WebSocketClient();
        wsclient.start();

        Future<Session> f = wsclient.connect(socket, uri, new ClientUpgradeRequest());
        while (true) {
            try {
                f.get();
                break;
            } catch (InterruptedException e) {
            }
        }
        return !f.isCancelled();
    }

    @Override
    public void close() {
        disconnect();
    }

    public void disconnect() {
        if (wsclient != null) {
            try {
                wsclient.stop();
            } catch (Exception e) {
            }
        }
    }

    public boolean ping() throws IOException {
        URL url;
        try {
            url = new URL(scheme + "://" + host + ":" + port + "/ping");
            LOG.debug("Pinging {}", url);
        } catch (MalformedURLException e) {
            LOG.error(e.toString());
            return false;
        }
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setDoOutput(false);
            byte[] buffer = {'n', 'g'};
            int s = conn.getInputStream().read(buffer, 0, 2);
            if (s < 0) {
                return false;
            }
            String msg = new String(buffer, StandardCharsets.UTF_8);
            LOG.info(msg);
            return "OK".equals(msg);
        } catch (IOException e) {
            LOG.debug(e.toString());
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    public Response list(int limit) throws IOException {
        return rpc(new ListJobRequest(limit));
    }

    public Response schedule(Job job) throws IOException {
        return rpc(new ScheduleRequest(job, false));
    }

    public Response getJob(int id) throws IOException {
        return rpc(new GetJobRequest(id));
    }

    public Response getFile(int id, String file, int offset, int length) throws IOException {
        return rpc(new GetFileRequest(id, file, offset, length));
    }

    public void getWholeFile(int id, String filename, String resultDir) throws IOException {
        String path = resultDir + "/" + filename;
        FileOutputStream out = new FileOutputStream(path);
        getWholeFile(id, filename, false, out);
    }

    // Gets whole file until the job finishes and streams out to 'out'!!!
    public void getWholeFile(int id, String filename, boolean poll, OutputStream out) throws IOException {
        int offset = 0;
        int length = 65536;
        do {
            Response res = getFile(id, filename, offset, length);
            if (res instanceof GetFileResponse) {
                GetFileResponse getFileResponse = (GetFileResponse) res;
                if (getFileResponse.file().isPresent()) {
                    if (getFileResponse.file().get().data().isEmpty()) {
                        // All contents fetched
                        if (poll) {
                            try {
                                Thread.sleep(1024);
                            } catch (InterruptedException e) {
                            }
                        }
                    } else {
                        byte[] data = getFileResponse.file().get().data().getBytes(UTF_8);
                        out.write(data);
                        offset += data.length;
                    }
                } else if (getFileResponse.job().isPresent()) {
                    // If the job is still running or not yet running ...
                    if (getFileResponse.job().get().state() == Job.JobState.FINISHED
                            || getFileResponse.job().get().state() == Job.JobState.KILLED) {
                        // Just loop until the end if it's empty
                    } else if (poll) {
                        // Wait a bit
                        try {
                            Thread.sleep(1024);
                        } catch (InterruptedException e) {
                        }
                    }
                } else {
                    // No such file
                    LOG.info("No such job or file: {}", id);
                    return;
                }
            } else {
                LOG.error(res.status());
                return;
            }
        } while (poll);
    }

    public Response listFiles(int id, String path) throws IOException {
        return rpc(new ListFilesRequest(id, path));
    }

    public Job run(Job job) throws IOException {
        Response res = rpc(new ScheduleRequest(job, true));
        // FIXME: There's a slight race condition between a job finish and watch registration
        // The latter is expected to be before the former, otherwise this function waits forever
        if (res instanceof ScheduleResponse) {
            ScheduleResponse scheduleResponse = (ScheduleResponse) res;
            LOG.info("Job scheduled: id={}", scheduleResponse.job().id());
            Job result = waitForResponse(watchResponse -> {
                if (watchResponse.job() == null) {
                    return null;
                } else if (scheduleResponse.job().id() == watchResponse.job().id()) {
                    LOG.info("{}: id={}", watchResponse.event(), watchResponse.job().id());
                    if (watchResponse.job().state() == Job.JobState.FINISHED) {
                        return watchResponse.job();
                    } else if (watchResponse.job().state() == Job.JobState.KILLED) {
                        return watchResponse.job();
                    }
                }
                LOG.debug("keep waiting");
                return null; // keep waiting
            });
            return result;
        } else {
            LOG.error(res.status());
            return null;
        }
    }

    public Response kill(int id) throws IOException {
        return rpc(new KillRequest(id));
    }

    public Response getApp(String appid) throws IOException {
        return rpc(new GetAppRequest(appid));
    }

    public Response load(Application application) throws IOException {
        return rpc(new LoadAppRequest(Objects.requireNonNull(application)));
    }

    public Response load(String appid, List<String> persistentFiles, List<String> largeFiles,
                         List<String> files, Optional<Integer> diskMB) throws IOException {
        return rpc(new LoadAppRequest(new Application(appid, persistentFiles, largeFiles, files, diskMB, new MesosContainer())));
    }

    public Response load(String appid, List<String> persistentFiles, List<String> largeFiles, List<String> files) throws IOException {
        return load(appid, persistentFiles, largeFiles, files, Optional.empty());
    }

    public Response listApp() throws IOException {
        return rpc(new ListAppRequest());
    }

    public Response unload(String appName) throws IOException {
        return rpc(new UnloadAppRequest(appName));
    }

    private <ReqType extends Request> Response rpc(ReqType req) throws IOException {
        URL url;
        try {
            url = new URL(scheme + "://" + host + ":" + port + req.resource());
            LOG.debug("Connecting {}", url);
        } catch (MalformedURLException e) {
            return new ErrorResponse(e.toString());
        }
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(req.method());
            conn.setDoOutput(req.hasPayload());

            String md5 = "";
            String payload = "";
            if (req.hasPayload()) {
                payload = mapper.writeValueAsString(req);
                LOG.info(req.method());
                LOG.info(payload);
                md5 = digest(payload);
                conn.setRequestProperty("Content-MD5", md5);
                conn.setRequestProperty("Content-Length", Integer.toString(payload.length()));
            }

            String date = TimestampHelper.now();
            conn.setRequestProperty("Date", date);

            if (authenticator.isPresent()) {
                String signature = authenticator.get().buildHeaderValue(req.method(), md5, date, req.resource());
                conn.setRequestProperty(Authenticator.AUTHORIZATION, signature);
            }

            if (req.hasPayload()) {
                //mapper.writeValue(conn.getOutputStream(), req);
                conn.getOutputStream().write(payload.getBytes(UTF_8));
            }

            return mapper.readValue(conn.getInputStream(), Response.class);
        } catch (IOException e) {
            LOG.debug(e.toString());
            return new ErrorResponse(e.toString());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    // Wait for good response until the callback gives non-null good value.
    // To keep watching, callback must keep returning null.
    public <RetType> RetType waitForResponse(Function<WatchResponse, RetType> callback) throws IOException {
        try {
            if (!connect()) {
                LOG.error("failed to connect to host");
                return null;
            } else {
                LOG.info("Connected to the host. Start waiting for response..");
            }
        } catch (Exception e) {
            LOG.error(e.toString());
            return null;
        }
        RetType ret = null;
        while (ret == null) {
            String json;
            try {
                json = socket.awaitResponse();
                LOG.debug("WaitForResponse started: {}", json);
            } catch (InterruptedException e) {
                continue;
            }
            Response res = mapper.readValue(json, Response.class);
            if (res instanceof WatchResponse) {
                WatchResponse wres = (WatchResponse) res;
                ret = callback.apply(wres);
            } else if (res instanceof ErrorResponse) {
                ErrorResponse eres = (ErrorResponse) res;
                LOG.error(eres.status());
                return null;
            }
        }
        return ret;
    }

    // This method does block while callback returns.
    // The callback must return true to keep watching.
    public void startWatch(Predicate<WatchResponse> callback) throws IOException {
        waitForResponse(watchResponse -> {
            if (callback.test(watchResponse)) {
                return null;
            } else {
                return 1;
            }
        });

    }

    public static ClientBuilder newBuilder(URI uri) {
        return new ClientBuilder(uri);
    }
}
