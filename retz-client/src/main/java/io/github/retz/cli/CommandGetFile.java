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
package io.github.retz.cli;

import com.beust.jcommander.Parameter;
import io.github.retz.protocol.ErrorResponse;
import io.github.retz.protocol.GetFileResponse;
import io.github.retz.protocol.GetJobResponse;
import io.github.retz.protocol.Response;
import io.github.retz.protocol.data.DirEntry;
import io.github.retz.protocol.data.Job;
import io.github.retz.web.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

public class CommandGetFile implements SubCommand {
    static final Logger LOG = LoggerFactory.getLogger(CommandGetFile.class);

    @Parameter(names = "-id", description = "Job ID whose state and details you want", required = true)
    private int id;

    @Parameter(names = {"-R", "--resultdir"}, description = "Local directory to save the file ('-' to print)")
    private String resultDir;

    @Parameter(names = "--fetch", description = "Remote file to fetch")
    private String filename;

    @Parameter(names = "--poll", description = "Keep polling the file until a job finishes")
    private boolean poll;

    @Parameter(names = "--offset", description = "Offset")
    private int offset = 0;

    @Parameter(names = "--length", description = "Length")
    private int length = -1; // -1 means get all file

    @Override
    public String description() {
        return "Get file from sandbox of a job";
    }

    @Override
    public String getName() {
        return "get-file";
    }

    @Override
    public int handle(FileConfiguration fileConfig) {
        LOG.debug("Configuration: {}", fileConfig.toString());

        try (Client webClient = Client.newBuilder(fileConfig.getUri())
                .enableAuthentication(fileConfig.authenticationEnabled())
                .setAuthenticator(fileConfig.getAuthenticator())
                .checkCert(fileConfig.checkCert())
                .build()) {

            LOG.info("Getting file {} (offset={}, length={}) of a job(id={})", filename, offset, length, id);

            OutputStream out;

            if ("-".equals(resultDir)) {
                out = System.out;
            } else {
                String path = resultDir + "/" + filename;
                out = new FileOutputStream(path);
            }

            if (length < 0) {
                webClient.getWholeFile(id, filename, poll, out);

            } else {
                Response res = webClient.getFile(id, filename, offset, length);
                if (res instanceof GetFileResponse) {
                    GetFileResponse getFileResponse = (GetFileResponse) res;

                    if (getFileResponse.job().isPresent()) {
                        Job job = getFileResponse.job().get();

                        LOG.info("Job: appid={}, id={}, scheduled={}, cmd='{}'", job.appid(), job.id(), job.scheduled(), job.cmd());
                        LOG.info("\tstarted={}, finished={}, state={}, result={}", job.started(), job.finished(), job.state(), job.result());

                        if (getFileResponse.file().isPresent()) {
                            LOG.info("offset={}", getFileResponse.file().get().offset());
                            out.write(getFileResponse.file().get().data().getBytes(UTF_8));
                        }

                        return 0;

                    } else {
                        LOG.error("No such job: id={}", id);
                    }
                } else {
                    ErrorResponse errorResponse = (ErrorResponse) res;
                    LOG.error("Error: {}", errorResponse.status());
                }
            }

        } catch (ConnectException e) {
            LOG.error("Cannot connect to server {}", fileConfig.getUri());
        } catch (IOException e) {
            LOG.error(e.toString(), e);
        }
        return -1;
    }

}

