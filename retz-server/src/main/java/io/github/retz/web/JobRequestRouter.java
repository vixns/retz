/**
 *    Retz
 *    Copyright (C) 2016 Nautilus Technologies, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package io.github.retz.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import io.github.retz.protocol.GetFileResponse;
import io.github.retz.protocol.GetJobResponse;
import io.github.retz.protocol.ListFilesResponse;
import io.github.retz.protocol.Response;
import io.github.retz.protocol.data.DirEntry;
import io.github.retz.protocol.data.FileContent;
import io.github.retz.protocol.data.Job;
import io.github.retz.scheduler.JobQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class JobRequestRouter {
    private static final Logger LOG = LoggerFactory.getLogger(JobRequestRouter.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.registerModule(new Jdk8Module());
    }

    public static String getJob(spark.Request req, spark.Response res) throws JsonProcessingException, IOException {
        int id = Integer.parseInt(req.params(":id"));

        LOG.debug("get job id={}, path={}, file={}", id);
        res.type("application/json");

        Optional<Job> job = JobQueue.getJob(id);

        Response response;
        // Search job from JobQueue with matching id
        GetJobResponse getJobResponse = new GetJobResponse(job);
        getJobResponse.ok();
        res.status(200);

        response = getJobResponse;

        return MAPPER.writeValueAsString(response);

    }

    public static String getFile(spark.Request req, spark.Response res) throws IOException {
        int id = Integer.parseInt(req.params(":id"));

        String file = req.params("file");
        int offset = Integer.parseInt(req.queryParams("offset"));
        int length = Integer.parseInt(req.queryParams("length"));
        Optional<Job> job = JobQueue.getJob(id);

        LOG.info("get-file: id={}, path={}, offset={}, length={}", id, file, offset, length);
        res.type("application/json");

        Optional<FileContent> fileContent;
        if (job.isPresent() && statHTTPFile(job.get().url(), file)) {
            String payload = fetchHTTPFile(job.get().url(), file, offset, length);
            LOG.info("Payload length={}, offset={}", payload.length(), offset);
            // TODO: what the heck happens when a file is not UTF-8 encodable???? How Mesos works?
            fileContent = Optional.of(new FileContent(payload, offset));
        } else {
            fileContent = Optional.empty();
        }
        GetFileResponse getFileResponse = new GetFileResponse(job, fileContent);
        getFileResponse.ok();
        res.status(200);

        return MAPPER.writeValueAsString(getFileResponse);

    }

    public static String getPath(spark.Request req, spark.Response res) throws IOException {
        int id = Integer.parseInt(req.params(":id"));

        String path = req.params("path");
        Optional<Job> job = JobQueue.getJob(id);

        LOG.info("id={}, path={}", id, path);

        List ret;
        if (job.isPresent()) {
            String json = fetchHTTPDir(job.get().url(), path);
            ret = MAPPER.readValue(json, new TypeReference<List<DirEntry>>() {});
        } else {
            ret = Arrays.asList();
        }

        ListFilesResponse listFilesResponse = new ListFilesResponse(job, ret);

        return MAPPER.writeValueAsString(listFilesResponse);
    }


    public static boolean statHTTPFile(String url, String name) {
        String addr = url.replace("files/browse", "files/download") + "%2F" + name;

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(addr).openConnection();
            conn.setRequestMethod("HEAD");
            conn.setDoOutput(false);
            LOG.debug(conn.getResponseMessage());
            return conn.getResponseCode() == 200 ||
                    conn.getResponseCode() == 204;
        } catch (IOException e) {
            LOG.debug("Failed to fetch {}: {}", addr, e.toString());
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String fetchHTTP(String addr) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(addr).openConnection();
            conn.setRequestMethod("GET");
            conn.setDoOutput(true);
            LOG.debug(conn.getResponseMessage());

        } catch (MalformedURLException e) {
            LOG.error(e.toString());
        } catch (IOException e) {
            LOG.error(e.toString());
        }

        try (InputStream input = conn.getInputStream()) {
            StringBuilder builder = new StringBuilder();

            byte[] buffer = new byte[65536];
            int bytesRead = 0;
            while ((bytesRead = input.read(buffer)) != -1) {
                builder.append(buffer); //, 0, bytesRead);
            }
            return buffer.toString();
        } catch (IOException e) {
            // Somehow this happens even HTTP was correct
            LOG.debug("Cannot fetch file {}: {}", addr, e.toString());
            // Just retry until your stack get stuck; thanks to SO:33340848
            // and to that crappy HttpURLConnection
            return fetchHTTP(addr);
        } finally {
            conn.disconnect();
        }

    }

    public static String fetchHTTPFile(String url, String name, int offset, int length) {
        String addr = url.replace("files/browse", "files/read") + "%2F" + name
                + "&offset=" + offset + "&length=" + length;
        return fetchHTTP(addr);
    }

    public static String fetchHTTPDir(String url, String path) {
        // Just do 'files/browse and get JSON
        String addr = url + "%2F" + path;
        return fetchHTTP(addr);
    }
}
