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
package io.github.retz.cli;

import com.beust.jcommander.Parameter;
import io.github.retz.protocol.ErrorResponse;
import io.github.retz.protocol.GetJobResponse;
import io.github.retz.protocol.Response;
import io.github.retz.protocol.data.DirEntry;
import io.github.retz.protocol.data.Job;
import io.github.retz.web.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Optional;

public class CommandGetJob implements SubCommand {
    static final Logger LOG = LoggerFactory.getLogger(CommandGetJob.class);

    @Parameter(names = "-id", description = "Job ID whose state and details you want", required = true)
    private int id;

    @Parameter(names = {"-R", "--resultdir"}, description = "Local directory to save job results ('-' to print)")
    private String resultDir;

    @Parameter(names = "--list", description = "Remote sandbox path to list")
    private String listDir;

    @Parameter(names = "--fetch", description = "Remote file to fetch")
    private String filename;

    @Override
    public String description() {
        return "Get details of a job";
    }

    @Override
    public String getName() {
        return "get-job";
    }

    @Override
    public int handle(FileConfiguration fileConfig) {
        LOG.debug("Configuration: {}", fileConfig.toString());

        try (Client webClient = Client.newBuilder(fileConfig.getUri())
                .enableAuthentication(fileConfig.authenticationEnabled())
                .setAuthenticator(fileConfig.getAuthenticator())
                .checkCert(fileConfig.checkCert())
                .build()) {

            LOG.info("Fetching job detail id={}, dir={}, file={}", id, listDir, filename);

            Response res = webClient.getJob(id, Optional.ofNullable(listDir), Optional.ofNullable(filename));
            if (res instanceof GetJobResponse) {
                GetJobResponse getJobResponse = (GetJobResponse) res;

                if (getJobResponse.job().isPresent()) {
                    Job job = getJobResponse.job().get();

                    LOG.info("Job: appid={}, id={}, scheduled={}, cmd='{}'", job.appid(), job.id(), job.scheduled(), job.cmd());
                    LOG.info("\tstarted={}, finished={}, state={}, result={}", job.started(), job.finished(), job.state(), job.result());

                    if (!getJobResponse.entries().isEmpty()) {
                        LOG.info("list of {}", listDir);
                        for (DirEntry e : getJobResponse.entries()) {
                            LOG.info(e.toString());
                        }
                    }

                    if (getJobResponse.file().isPresent()) {
                        
                    }
                    //Client.fetchJobResult(job, resultDir);

                    return 0;

                } else {
                    LOG.error("No such job: id={}", id);
                }
            } else {
                ErrorResponse errorResponse = (ErrorResponse) res;
                LOG.error("Error: {}", errorResponse.status());
            }

        } catch (ConnectException e) {
            LOG.error("Cannot connect to server {}", fileConfig.getUri());
        } catch (IOException e) {
            LOG.error(e.toString(), e);
        }
        return -1;

    }
}

