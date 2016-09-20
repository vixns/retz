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
import io.github.retz.protocol.data.DirEntry;
import io.github.retz.protocol.data.FileContent;
import io.github.retz.protocol.data.Job;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ListFilesResponse extends Response {
    private Optional<Job> job;
    private List<DirEntry> entries;

    @JsonCreator
    public ListFilesResponse(@JsonProperty("job") Optional<Job> job,
                             @JsonProperty("entries") List<DirEntry> entries) {
        this.job = Objects.requireNonNull(job);
        this.entries = (entries == null)? new LinkedList<>() : entries;
    }

    @JsonGetter("job")
    public Optional<Job> job() {
        return job;
    }

    @JsonGetter("entries")
    public List<DirEntry> entries() {
        return entries;
    }
}
