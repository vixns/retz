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
package io.github.retz.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.Optional;

public class GetJobRequest extends Request {
    private int id;
    private Optional<String> listDir;
    private Optional<String> fetchFile;

    @JsonCreator
    public GetJobRequest(@JsonProperty(value = "id", required = true) int id,
                         @JsonProperty(value = "listDir") Optional<String> dir,
                         @JsonProperty(value = "fetchFile") Optional<String> file) {
        this.id = id;
        this.listDir = Objects.requireNonNull(dir);
        this.fetchFile = Objects.requireNonNull(file);
    }

    @JsonGetter("id")
    public int id() {
        return id;
    }

    @JsonGetter("listDir")
    public Optional<String> listDir() {
        return listDir;
    }

    @JsonGetter("fetchFile")
    public Optional<String> fetchFile() {
        return fetchFile;
    }

    @Override
    public String resource() {
        return "/job/" + id;
    }

    @Override
    public String method() {
        return GET;
    }

    @Override
    public boolean hasPayload() {
        return false;
    }

    public static String resourcePattern() {
        return "/job/:id";
    }
}
